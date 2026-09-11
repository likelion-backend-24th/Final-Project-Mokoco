import test from "node:test";
import assert from "node:assert/strict";
import { getNearbyPosts } from "./nearby-posts.js";

test("guest requests all regions without an authorization header and keeps paging", async (t) => {
  t.mock.method(globalThis, "fetch", async (url, options) => {
    const query = new URL(url).searchParams;
    assert.equal(query.has("regionScope"), false);
    assert.equal(query.get("page"), "1");
    assert.equal(query.get("category"), "PLUMBING");
    assert.equal("Authorization" in options.headers, false);
    return Response.json({ content: [{ id: 1 }], totalElements: 25, regionFilter: null });
  });
  const result = await getNearbyPosts(null, "PLUMBING", 1, 20, "DONG");
  assert.deepEqual(result.posts, [{ id: 1 }]);
  assert.equal(result.error, null);
  assert.equal(result.pagination.totalElements, 25);
});

test("invalid logged-in token does not silently fall back to guest data", async (t) => {
  const fetch = t.mock.method(globalThis, "fetch", async () => Response.json({ message: "로그인이 필요합니다." }, { status: 401 }));
  const result = await getNearbyPosts("invalid");
  assert.match(result.error, /로그인/);
  assert.deepEqual(result.posts, []);
  assert.equal(fetch.mock.callCount(), 1);
});

test("forwards verified-token input and paging parameters, preserves total count", async (t) => {
  t.mock.method(globalThis, "fetch", async (url, options) => {
    const query = new URL(url).searchParams;
    assert.equal(query.get("page"), "2");
    assert.equal(query.get("category"), "PLUMBING");
    assert.equal(query.get("regionScope"), "SIDO");
    assert.equal(options.headers.Authorization, "Bearer test-token");
    assert.equal(options.cache, "no-store");
    return Response.json({ content: [{ id: 9 }], number: 2, totalElements: 41, last: true });
  });
  const result = await getNearbyPosts("test-token", "PLUMBING", 2);
  assert.deepEqual(result.posts, [{ id: 9 }]);
  assert.equal(result.pagination.totalElements, 41);
});

test("selected district and dong scopes are sent to the backend", async (t) => {
  const scopes = [];
  t.mock.method(globalThis, "fetch", async (url) => {
    scopes.push(new URL(url).searchParams.get("regionScope"));
    return Response.json({ content: [], totalElements: 0 });
  });
  await getNearbyPosts("token", "ALL", 0, 20, "SIGUNGU");
  await getNearbyPosts("token", "ALL", 0, 20, "DONG");
  assert.deepEqual(scopes, ["SIGUNGU", "DONG"]);
});

test("region-required and upstream errors are shown without fake data", async (t) => {
  t.mock.method(globalThis, "fetch", async () => Response.json({ message: "활동 지역을 먼저 설정해주세요." }, { status: 409 }));
  const result = await getNearbyPosts("token");
  assert.deepEqual(result.posts, []);
  assert.match(result.error, /활동 지역/);
  assert.equal(result.pagination, undefined);
});
