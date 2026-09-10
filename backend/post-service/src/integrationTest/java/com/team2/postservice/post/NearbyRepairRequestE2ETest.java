package com.team2.postservice.post;

import com.fasterxml.jackson.databind.*;
import com.team2.postservice.post.repository.PostRepository;
import com.team2.postservice.post.entity.PostStatus;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.config.location=optional:classpath:/nearby-e2e-empty.properties",
    "spring.datasource.url=jdbc:h2:mem:nearby-post;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false",
    "internal.service-key=nearby-e2e-only", "file.upload-dir=build/nearby-e2e/uploads",
    "file.base-url=/images"
})
class NearbyRepairRequestE2ETest {
    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    static final ObjectMapper JSON = new ObjectMapper();
    static Process userProcess;
    static HttpServer geocoder;
    static int userPort;
    @LocalServerPort int postPort;
    @Autowired PostRepository posts;

    @DynamicPropertySource static void userService(DynamicPropertyRegistry registry) throws Exception {
        geocoder = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        geocoder.createContext("/req/address", exchange -> {
            // Only the third-party geocoder is a fixture. User/Location is the real application.
            String point = exchange.getRequestURI().getQuery();
            String code = point.contains("127.0") ? "1165053100" : point.contains("125.0") ? "1165051000"
                    : point.contains("126.0") ? "1168051000" : "2611051000";
            String sido = code.startsWith("11") ? "서울특별시" : "부산광역시";
            String sigungu = code.startsWith("11650") ? "서초구" : code.startsWith("11680") ? "강남구" : "중구";
            String dong = code.equals("1165053100") ? "서초4동" : "테스트동";
            byte[] body = JSON.writeValueAsBytes(Map.of("response", Map.of("status", "OK", "result",
                    List.of(Map.of("structure", Map.of("level1", sido, "level2", sigungu, "level4A", dong, "level4AC", code))))));
            exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
        });
        geocoder.start();
        try (var socket = new java.net.ServerSocket(0)) { userPort = socket.getLocalPort(); }
        String classpath = Files.readString(Path.of(System.getProperty("nearby.userClasspathFile")))
                + java.io.File.pathSeparator + Path.of(Class.forName("org.h2.Driver").getProtectionDomain().getCodeSource().getLocation().toURI());
        var command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx256m", "-cp", classpath, "com.team2.userservice.UserServiceApplication",
                "--spring.config.location=optional:classpath:/nearby-e2e-empty.properties",
                "--server.port=" + userPort, "--spring.datasource.url=jdbc:h2:mem:nearby-user;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.datasource.username=sa", "--spring.datasource.password=",
                "--spring.jpa.hibernate.ddl-auto=create-drop", "--internal.service-key=nearby-e2e-only",
                "--jwt.secretKey=nearby-e2e-access-key-with-at-least-32-bytes",
                "--jwt.refreshKey=nearby-e2e-refresh-key-with-at-least-32-bytes",
                "--jwt.access-expiration-ms=3600000", "--jwt.refresh-expiration-ms=3600000",
                "--spring.security.oauth2.client.registration.google.client-id=e2e",
                "--spring.security.oauth2.client.registration.google.client-secret=e2e",
                "--vworld.api-key=e2e", "--vworld.base-url=http://127.0.0.1:" + geocoder.getAddress().getPort()));
        var log = Path.of("build/nearby-e2e/user-service.log");
        Files.createDirectories(log.getParent());
        userProcess = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { if (userProcess != null) userProcess.destroy(); }));
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        boolean ready = false;
        while (System.nanoTime() < deadline && userProcess.isAlive()) {
            try {
                HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + userPort + "/api/auth/signin"))
                        .timeout(Duration.ofSeconds(1)).GET().build(), HttpResponse.BodyHandlers.discarding());
                ready = true; break;
            } catch (Exception ignored) { Thread.sleep(200); }
        }
        if (!ready) { userProcess.destroy(); geocoder.stop(0); throw new IllegalStateException("User startup failed: " + log.toAbsolutePath()); }
        registry.add("services.user-service.url", () -> "http://127.0.0.1:" + userPort);
    }

    @AfterAll static void shutdown() throws Exception {
        if (userProcess != null) {
            userProcess.destroy();
            if (!userProcess.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) userProcess.destroyForcibly();
        }
        if (geocoder != null) geocoder.stop(0);
    }

    @Test void signupRegionRegistrationNearbyPagingVisibilityAndRegionChange() throws Exception {
        String owner = signup("owner");
        String viewer = signup("viewer");
        String outsider = signup("outsider");
        String sameDistrict = signup("same-district");
        String sameCity = signup("same-city");
        assertStatus(call(postPort, "GET", "/posts", null, null), 200);
        assertStatus(call(postPort, "GET", "/posts", "forged", null), 401);
        assertStatus(call(postPort, "GET", "/posts", viewer, null), 409);
        setRegion(owner, 127); setRegion(viewer, 127); setRegion(outsider, 128);
        setRegion(sameDistrict, 125); setRegion(sameCity, 126);
        long a1 = create(owner, "A 공개 첫 번째");
        long a2 = create(owner, "A 공개 두 번째");
        long hidden = create(owner, "비공개");
        long matched = create(owner, "매칭됨");
        long completed = create(owner, "종료됨");
        long b = create(outsider, "B 공개");
        long districtPost = create(sameDistrict, "같은 구 다른 동");
        long cityPost = create(sameCity, "같은 시 다른 구");
        assertStatus(call(postPort, "PATCH", "/posts/" + hidden + "/visibility", viewer, "{\"publiclyVisible\":false}"), 403);
        assertStatus(call(postPort, "PATCH", "/posts/" + hidden + "/visibility", owner, "{\"publiclyVisible\":false}"), 204);
        // Repair-owned lifecycle fixture, without modifying User/Location repositories.
        var matchedPost = posts.findById(matched).orElseThrow(); matchedPost.updateStatusToMatched(); posts.save(matchedPost);
        var completedPost = posts.findById(completed).orElseThrow();
        org.springframework.test.util.ReflectionTestUtils.setField(completedPost, "status", PostStatus.COMPLETED); posts.save(completedPost);
        var legacy = posts.save(com.team2.postservice.post.entity.Post.builder().title("지역 코드 없는 기존 요청")
                .content("공개 요청").authorEmail("owner@nearby.test").regionName("기존 지역")
                .category(com.team2.postservice.post.entity.PostCategory.LIVING_ETC).build());
        var guest = json(call(postPort, "GET", "/posts?regionScope=DONG", null, null), 200);
        assertThat(guest.path("totalElements").asInt()).isEqualTo(6);
        assertThat(guest.path("regionFilter").isNull()).isTrue();
        assertThat(guest.path("content").findValuesAsText("id"))
                .containsExactlyInAnyOrder("" + a1, "" + a2, "" + b, "" + districtPost, "" + cityPost, "" + legacy.getId());
        var guestPage = json(call(postPort, "GET", "/posts?size=2&page=1", null, null), 200);
        assertThat(guestPage.path("content").size()).isEqualTo(2);
        assertThat(guestPage.path("totalPages").asInt()).isEqualTo(3);
        assertStatus(call(postPort, "GET", "/posts?size=0", null, null), 400);
        assertStatus(call(postPort, "PATCH", "/posts/" + hidden + "/visibility", null, "{\"publiclyVisible\":true}"), 401);
        JsonNode page0 = json(call(postPort, "GET", "/posts?size=1&regionScope=DONG", viewer, null), 200);
        JsonNode page1 = json(call(postPort, "GET", "/posts?size=1&page=1&regionScope=DONG", viewer, null), 200);
        assertThat(page0.path("totalElements").asInt()).isEqualTo(2);
        assertThat(page0.path("content").get(0).path("id").asLong()).isEqualTo(a2);
        assertThat(page1.path("content").get(0).path("id").asLong()).isEqualTo(a1);
        assertThat(page1.path("last").asBoolean()).isTrue();
        assertThat(json(call(postPort, "GET", "/posts?page=2&size=1&regionScope=DONG", viewer, null), 200).path("content").isEmpty()).isTrue();
        var defaultScope = json(call(postPort, "GET", "/posts", viewer, null), 200);
        assertThat(defaultScope.path("regionFilter").path("scope").asText()).isEqualTo("SIDO");
        assertThat(defaultScope.path("regionFilter").path("sido").asText()).isEqualTo("서울특별시");
        assertThat(defaultScope.path("content").findValuesAsText("id"))
                .containsExactlyInAnyOrder("" + a1, "" + a2, "" + districtPost, "" + cityPost);
        var districtScope = json(call(postPort, "GET", "/posts?regionScope=SIGUNGU", viewer, null), 200);
        assertThat(districtScope.path("totalElements").asInt()).isEqualTo(3);
        assertThat(districtScope.path("regionFilter").path("sigungu").asText()).isEqualTo("서초구");
        assertThat(districtScope.path("content").findValuesAsText("id"))
                .containsExactlyInAnyOrder("" + a1, "" + a2, "" + districtPost);
        var cityPage = json(call(postPort, "GET", "/posts?size=1", viewer, null), 200);
        assertThat(cityPage.path("totalElements").asInt()).isEqualTo(4);
        assertThat(cityPage.path("totalPages").asInt()).isEqualTo(4);
        // A query parameter cannot override the authenticated viewer's region.
        assertThat(json(call(postPort, "GET", "/posts?regionName=B&regionCode=B&regionScope=DONG", viewer, null), 200).path("totalElements").asInt()).isEqualTo(2);
        JsonNode detail = json(call(postPort, "GET", "/posts/" + a1, null, null), 200);
        assertThat(detail.path("title").asText()).isEqualTo("A 공개 첫 번째");
        assertThat(detail.path("content").asText()).isEqualTo("전원이 켜지지 않습니다");
        assertThat(detail.path("regionCode").asText()).isEqualTo("1165053100");
        assertStatus(call(postPort, "GET", "/posts/" + hidden, owner, null), 404);
        for (String query : List.of("page=-1", "size=0", "size=101", "page=bad", "page=2147483647", "category=INVALID", "regionScope=INVALID"))
            assertStatus(call(postPort, "GET", "/posts?" + query, viewer, null), 400);
        for (long unavailable : List.of(hidden, matched, completed))
            assertStatus(call(postPort, "POST", "/posts/" + unavailable + "/proposals", viewer,
                    "{\"estimatedPrice\":1000,\"content\":\"수리 제안\"}"), 409);
        setRegion(viewer, 128);
        var changed = json(call(postPort, "GET", "/posts", viewer, null), 200);
        assertThat(changed.path("totalElements").asInt()).isEqualTo(1);
        assertThat(changed.path("content").get(0).path("id").asLong()).isEqualTo(b);
        assertThat(posts.findById(a1).orElseThrow().getRegionCode()).isEqualTo("1165053100");
        assertStatus(call(postPort, "GET", "/posts/999999", null, null), 404);
    }

    String signup(String name) throws Exception {
        String email = name + "@nearby.test";
        assertStatus(call(userPort, "POST", "/api/auth/signup", null,
                JSON.writeValueAsString(Map.of("email", email, "password", "Password123", "name", name, "nickname", name))), 200);
        return json(call(userPort, "POST", "/api/auth/signin", null,
                JSON.writeValueAsString(Map.of("email", email, "password", "Password123"))), 200).path("accessToken").asText();
    }
    void setRegion(String token, int longitude) throws Exception {
        assertStatus(call(userPort, "PATCH", "/api/users/me/region", token,
                "{\"latitude\":37.5,\"longitude\":" + longitude + "}"), 200);
    }
    long create(String token, String title) throws Exception {
        String boundary = "nearby-test-boundary";
        String body = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"post\"\r\nContent-Type: application/json\r\n\r\n"
                + JSON.writeValueAsString(Map.of("title", title, "content", "전원이 켜지지 않습니다", "category", "LIVING_ETC")) + "\r\n--" + boundary + "--\r\n";
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + postPort + "/posts"))
                .header("Authorization", "Bearer " + token).header("X-User-Email", "forged@example.com")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return json(HTTP.send(request, HttpResponse.BodyHandlers.ofString()), 200).asLong();
    }
    static HttpResponse<String> call(int port, String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(10));
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.header("Content-Type", "application/json");
        request.header("X-User-Email", "viewer@nearby.test");
        return HTTP.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    static void assertStatus(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).as("HTTP response: %s", response.body()).isEqualTo(status);
    }
    static JsonNode json(HttpResponse<String> response, int status) throws Exception {
        assertStatus(response, status); return JSON.readTree(response.body());
    }
}
