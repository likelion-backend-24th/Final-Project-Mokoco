import test from "node:test";
import assert from "node:assert/strict";
import { plainTextToHtml } from "./rich-text.js";

test("plain text becomes paragraphs without interpreting HTML", () => {
  assert.equal(
    plainTextToHtml("첫 줄\n<script>alert(1)</script>"),
    "<p>첫 줄</p><p>&lt;script&gt;alert(1)&lt;/script&gt;</p>",
  );
});
