import test from "node:test";
import assert from "node:assert/strict";
import { normalizeRegionScope, regionListHref } from "./region-scope.js";

test("default and invalid URL scopes fall back to my 시·군·구 (SIGUNGU)", () => {
  assert.equal(normalizeRegionScope(undefined), "SIGUNGU");
  assert.equal(normalizeRegionScope("unknown"), "SIGUNGU");
  assert.equal(normalizeRegionScope("SIGUNGU"), "SIGUNGU");
  assert.equal(normalizeRegionScope("ALL"), "ALL");
});

test("filter changes reset page while preserving category and region scope", () => {
  const href = regionListHref({ category: "PLUMBING", regionScope: "DONG" });
  const query = new URL(href, "http://test").searchParams;
  assert.equal(query.get("category"), "PLUMBING");
  assert.equal(query.get("regionScope"), "DONG");
  assert.equal(query.has("page"), false);
});

test("pagination and home links preserve selected scope", () => {
  const href = regionListHref({ category: "PLUMBING", regionScope: "SIGUNGU", page: 2 });
  assert.equal(href, "/posts?regionScope=SIGUNGU&category=PLUMBING&page=2");
  assert.equal(regionListHref({ pathname: "/", regionScope: "DONG" }), "/?regionScope=DONG#posts");
});
