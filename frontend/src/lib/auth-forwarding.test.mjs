import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";

for (const path of ["notifications/route.js", "notifications/settings/route.js", "notifications/unread-count/route.js"]) {
  test(`${path}: sends Bearer only upstream, never in response headers`, async () => {
    const source = await readFile(new URL(`../app/api/${path}`, import.meta.url), "utf8");
    let forwarded;
    const get = vm.runInNewContext(source.replace(/^import .*;$/gm, "").replace(/^export /gm, "") + "\nGET", {
      cookies: async () => ({ get: (name) => ({ value: name === "access_token" ? "test-only-token" : "user@test" }) }),
      backendUrl: (route) => `http://backend${route}`,
      fetch: async (_url, options) => { forwarded = options.headers; return Response.json({ count: 1 }); },
      Response, AbortSignal,
    });
    const response = await get();
    assert.equal(forwarded.Authorization, "Bearer test-only-token");
    assert.equal(forwarded["X-User-Email"], undefined);
    assert.equal(response.headers.get("Authorization"), null);
    assert.equal(response.headers.get("Cache-Control"), "no-store");
    assert.deepEqual(await response.json(), { count: 1 });
  });
}
