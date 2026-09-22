import test from "node:test";
import assert from "node:assert/strict";
import { normalizeRegionScope, regionListHref } from "./region-scope.js";

test("default and invalid URL scopes use the city/province scope", () => {
  assert.equal(normalizeRegionScope(undefined), "SIDO");
  assert.equal(normalizeRegionScope("unknown"), "SIDO");
  assert.equal(normalizeRegionScope("SIGUNGU"), "SIGUNGU");
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
