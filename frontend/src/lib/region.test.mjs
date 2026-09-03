import test from "node:test";
import assert from "node:assert/strict";
import { formatRegionName, getGeolocationErrorMessage } from "./region.js";

test("행정구역 이름을 상위 지역부터 조합한다", () => {
  assert.equal(
    formatRegionName({ sido: "서울특별시", sigungu: "강남구", dong: "역삼동" }),
    "서울특별시 강남구 역삼동",
  );
});

test("비어 있는 행정구역 단계는 표시 이름에서 제외한다", () => {
  assert.equal(formatRegionName({ sido: "세종특별자치시", dong: "한솔동" }), "세종특별자치시 한솔동");
});

test("브라우저 위치 권한 거부를 사용자 메시지로 변환한다", () => {
  assert.match(getGeolocationErrorMessage(1), /권한이 거부/);
});
