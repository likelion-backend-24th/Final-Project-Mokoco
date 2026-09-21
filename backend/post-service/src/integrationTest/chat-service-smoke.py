"""Run real Post/Chat HTTP processes with separate ephemeral H2 databases and a User API stub.
Run from any directory: python backend/post-service/src/integrationTest/chat-service-smoke.py
No production configuration, data, Gemini, or real user-service is used.
"""
import concurrent.futures
import json
import os
from pathlib import Path
import socket
import subprocess
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

ROOT = Path(__file__).resolve().parents[4]
RUN = ROOT / ".tmp" / f"chat-split-qa-{time.time_ns()}"
RUN.mkdir(parents=True)
KEY = "isolated-chat-qa-key"
USERS = {name: {"id": index, "email": name + "@test", "nickname": name, "region": None}
         for index, name in enumerate(("requester", "repairer", "outsider"), 1)}


class UserStub(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def do_POST(self):
        self.do_GET()

    def do_GET(self):
        if self.headers.get("X-Internal-Service-Key") != KEY:
            self.send_error(401)
            return
        parsed = urllib.parse.urlparse(self.path)
        query = urllib.parse.parse_qs(parsed.query)
        if parsed.path.endswith("verify-token"):
            name = self.rfile.read(int(self.headers.get("Content-Length", 0))).decode().strip('"')
            user = USERS.get(name)
        elif "email" in query:
            user = USERS.get(query["email"][0].split("@")[0])
        else:
            index = int(query.get("id", [parsed.path.rsplit("/", 1)[-1]])[0])
            user = next((user for user in USERS.values() if user["id"] == index), None)
        if user is None:
            self.send_error(401)
            return
        body = json.dumps(user).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def request(base, path, method="GET", token=None, body=None, headers=None, expected=200):
    actual_headers = dict(headers or {})
    if token:
        actual_headers["Authorization"] = "Bearer " + token
    data = None if body is None else json.dumps(body).encode()
    if data is not None:
        actual_headers["Content-Type"] = "application/json"
    req = urllib.request.Request(base + path, data=data, method=method, headers=actual_headers)
    try:
        response = urllib.request.urlopen(req, timeout=15)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        payload = response.read().decode()
        assert response.code == expected, (path, response.code, payload)
        return json.loads(payload) if payload else None


def main():
    stub = ThreadingHTTPServer(("127.0.0.1", 0), UserStub)
    threading.Thread(target=stub.serve_forever, daemon=True).start()
    post_port, chat_port, gateway_port = port(), port(), port()
    post, chat = f"http://127.0.0.1:{post_port}", f"http://127.0.0.1:{chat_port}"
    gateway = f"http://127.0.0.1:{gateway_port}"
    init = RUN / "classpath.gradle"
    init.write_text("""allprojects {
    afterEvaluate {
        if (plugins.hasPlugin('java')) {
            tasks.register('chatQaClasspath') {
                dependsOn tasks.named('classes')
                doLast { file('build/chat-qa-classpath.txt').text = sourceSets.main.output.asPath + File.pathSeparator + configurations.testRuntimeClasspath.asPath }
            }
        }
    }
}
""", encoding="utf-8")
    seed = RUN / "seed.sql"
    seed.write_text("""INSERT INTO posts (id,title,content,author_email,region_name,publicly_visible,category,status,created_at,updated_at) VALUES (1,'Repair','Repair','requester@test','Seoul',true,'FURNITURE_INSTALL','WAITING',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO proposals (id,post_id,repairer_email,estimated_price,content,is_adopted,attach_resume) VALUES (1,1,'repairer@test',50000,'Repair',false,false);
""", encoding="utf-8")
    processes, logs = [], []
    try:
        for service, service_port in (("post-service", post_port), ("chat-service", chat_port), ("api-gateway", gateway_port)):
            directory = ROOT / "backend" / service
            wrapper = str(directory / ("gradlew.bat" if os.name == "nt" else "gradlew"))
            subprocess.run([wrapper, "-I", str(init), ":chatQaClasspath", "--console=plain"], cwd=directory, check=True)
            classpath = (directory / "build/chat-qa-classpath.txt").read_text().strip()
            name = {"post-service": "PostServiceApplication", "chat-service": "ChatServiceApplication", "api-gateway": "ApiGatewayApplication"}[service]
            args = ["-Xmx256m", "-cp", classpath, f"com.team2.{service.replace('-', '')}.{name}",
                    "--spring.config.location=optional:classpath:/isolated-qa.yaml",
                    f"--server.port={service_port}", "--server.address=127.0.0.1",
                    f"--spring.datasource.url=jdbc:h2:mem:{service};MODE=MySQL;DB_CLOSE_DELAY=-1",
                    "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.datasource.username=sa",
                    "--spring.datasource.password=", "--spring.jpa.hibernate.ddl-auto=create-drop",
                    "--spring.flyway.enabled=false", f"--internal.service-key={KEY}", "--ai.enabled=false",
                    "--file.upload-dir=./uploads", "--file.base-url=/images",
                    f"--services.user-service.url=http://127.0.0.1:{stub.server_port}",
                    f"--services.post-service.url={post}", f"--services.chat-service.url={chat}"]
            if service == "post-service":
                args += ["--spring.jpa.defer-datasource-initialization=true", "--spring.sql.init.mode=always",
                         f"--spring.sql.init.data-locations={seed.as_uri()}"]
            if service == "api-gateway":
                args = ["-Xmx256m", "-cp", classpath, "com.team2.apigateway.ApiGatewayApplication",
                        f"--spring.config.location={(directory / 'src/main/resources/application.yaml').as_uri()}",
                        f"--server.port={service_port}", "--server.address=127.0.0.1",
                        f"--services.post-service.url={post}",
                        f"--services.chat-service.ws-url=ws://127.0.0.1:{chat_port}",
                        f"--services.chat-service.url={chat}"]
            argfile = RUN / f"{service}.args"
            argfile.write_text("\n".join('"' + arg.replace('\\', '/').replace('"', '\\"') + '"' for arg in args), encoding="utf-8")
            log = (RUN / f"{service}.log").open("w", encoding="utf-8")
            logs.append(log)
            processes.append(subprocess.Popen(["java", "@" + str(argfile)], cwd=RUN, stdout=log, stderr=subprocess.STDOUT,
                                              creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0))
        for base, process in zip((post, chat, gateway), processes):
            deadline = time.monotonic() + 90
            while True:
                assert process.poll() is None, f"Service exited; see {RUN}"
                try:
                    path = "/api/chat-rooms/proposals/1" if base == gateway else "/internal/chat-rooms/1" if base == chat else "/internal/fix-deals/1"
                    request(base, path, expected=401)
                    break
                except (OSError, AssertionError):
                    if time.monotonic() >= deadline:
                        raise AssertionError(f"Service not ready; see {RUN}")
                    time.sleep(0.25)
        request(gateway, "/api/chat-rooms/proposals/1", "POST", expected=401)
        request(gateway, "/api/chat-rooms/proposals/1", "POST", token="outsider", expected=403)
        with concurrent.futures.ThreadPoolExecutor(2) as pool:
            rooms = list(pool.map(lambda token: request(gateway, "/api/chat-rooms/proposals/1", "POST", token=token),
                                  ["requester", "repairer"]))
        room = rooms[0]
        assert room["chatRoomId"] == rooms[1]["chatRoomId"] and room["fixDealId"] is None
        room_path = f'/api/chat-rooms/{room["chatRoomId"]}'
        request(gateway, "/posts/1/proposals/1", "DELETE", token="repairer", expected=409)
        request(gateway, room_path + "/contract", token="requester", expected=409)
        request(gateway, "/posts/1/proposals/1/adopt", "PATCH", token="requester")
        adopted = request(gateway, room_path + "/detail", token="requester")
        assert adopted["chatRoomId"] == room["chatRoomId"] and adopted["fixDealId"] is not None
        request(gateway, room_path + "/contract", token="requester")
        request(gateway, room_path + "/contract", token="outsider", expected=403)
        assert request(chat, f'/internal/chat-rooms/{room["chatRoomId"]}/text-messages?userId=1',
                       headers={"X-Internal-Service-Key": KEY}) == []
        request(post, "/posts/1/proposals/1/cancel", "PATCH", token="requester")
        assert request(chat, room_path + "/detail", token="requester")["fixDealId"] is None
        request(post, room_path + "/contract", token="requester", expected=409)
        request(post, "/posts/1/proposals/1/adopt", "PATCH", token="requester")
        assert request(chat, room_path + "/detail", token="requester")["fixDealId"] != adopted["fixDealId"]
        print("PASS: real Gateway/Post/Chat HTTP; separate H2; User stub; auth, concurrent create, delete guard, adoption, contract access, cancellation, re-adoption", flush=True)
        print(f"Logs: {RUN}", flush=True)
    finally:
        for process in processes:
            process.terminate()
        for process in processes:
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
        for log in logs:
            log.close()
        stub.shutdown()
        stub.server_close()


if __name__ == "__main__":
    main()
