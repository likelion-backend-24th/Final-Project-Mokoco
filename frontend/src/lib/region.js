export function formatRegionName(region) {
  return [region?.sido, region?.sigungu, region?.dong]
    .filter((value) => typeof value === "string" && value.trim())
    .join(" ");
}

export function getGeolocationErrorMessage(code) {
  if (code === 1) return "위치 권한이 거부되었습니다. 브라우저 설정에서 위치 권한을 허용해주세요.";
  if (code === 2) return "현재 위치를 확인할 수 없습니다. 잠시 후 다시 시도해주세요.";
  if (code === 3) return "위치 확인 시간이 초과되었습니다. 다시 시도해주세요.";
  return "위치를 확인하는 중 문제가 발생했습니다.";
}
