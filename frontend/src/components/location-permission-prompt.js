"use client";

import { useEffect, useState } from "react";
import {
  CheckCircle,
  Crosshair,
  DeviceMobile,
  MapPin,
  WarningCircle,
  X,
} from "@phosphor-icons/react";
import LocationMapPicker from "@/components/location-map-picker";
import {
  createManualMapFallback,
  formatRegionName,
  isAutomaticLocationAccurate,
} from "@/lib/region";

export default function LocationPermissionPrompt({ userEmail }) {
  const [state, setState] = useState("checking");
  const [message, setMessage] = useState("");
  const [regionName, setRegionName] = useState("");
  const [position, setPosition] = useState(null);
  const dismissalKey = `mokoco-location-dismissed:${userEmail}`;

  useEffect(() => {
    let active = true;

    async function checkRegion() {
      try {
        const response = await fetch("/api/users/me/region", { cache: "no-store" });

        if (response.ok) {
          const region = await response.json();
          if (active) {
            setRegionName(formatRegionName(region));
            setState("saved");
          }
          return;
        }

        if (response.status === 404) {
          const dismissed = window.sessionStorage.getItem(dismissalKey) === "true";
          if (active) setState(dismissed ? "dismissed" : "prompt");
          return;
        }

        if (active) setState("prompt");
      } catch {
        if (active) setState("prompt");
      }
    }

    checkRegion();
    return () => { active = false; };
  }, [dismissalKey]);

  function dismiss() {
    window.sessionStorage.setItem(dismissalKey, "true");
    setState("dismissed");
    setPosition(null);
    setMessage("");
  }

  function requestLocation() {
    setMessage("");

    if (!("geolocation" in navigator)) {
      const fallback = createManualMapFallback(0);
      setPosition(fallback.position);
      setMessage(`이 브라우저에서는 위치 기능을 지원하지 않습니다. 지도에서 실제 위치를 직접 선택해주세요.`);
      setState("manual");
      return;
    }

    setState("requesting");
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        const nextPosition = {
          latitude: coords.latitude,
          longitude: coords.longitude,
          accuracy: coords.accuracy,
          source: "automatic",
        };
        setPosition(nextPosition);

        if (isAutomaticLocationAccurate(coords.accuracy)) {
          setState("confirm");
          return;
        }

        setMessage(`브라우저가 제공한 위치 정확도는 약 ${Math.round(coords.accuracy).toLocaleString("ko-KR")}m이며, 실제 오차는 이보다 더 클 수 있습니다. 지도에서 실제 위치를 직접 선택해주세요.`);
        setState("manual");
      },
      (error) => {
        const fallback = createManualMapFallback(error.code);
        setPosition((current) => current ?? fallback.position);
        setMessage(fallback.message);
        setState("manual");
      },
      { enableHighAccuracy: true, timeout: 20000, maximumAge: 0 },
    );
  }

  function selectPosition(nextPosition) {
    setPosition(nextPosition);
    setMessage("지도에서 선택한 위치입니다. 지점을 확인한 뒤 설정해주세요.");
    setState("manual-selected");
  }

  async function savePosition() {
    if (!position) return;
    setMessage("");
    setState("saving");

    try {
      const response = await fetch("/api/users/me/region", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ latitude: position.latitude, longitude: position.longitude }),
      });
      const payload = await response.json();

      if (!response.ok) {
        setMessage(payload.message ?? "지역 정보를 저장하지 못했습니다.");
        setState(position.source === "manual" ? "manual-selected" : "confirm");
        return;
      }

      setRegionName(formatRegionName(payload));
      window.sessionStorage.removeItem(dismissalKey);
      setState("success");
    } catch {
      setMessage("지역 정보 서버와 통신할 수 없습니다.");
      setState(position.source === "manual" ? "manual-selected" : "confirm");
    }
  }

  const mapVisible = Boolean(position) && ["confirm", "manual", "manual-selected", "saving"].includes(state);
  const succeeded = state === "success";
  const modalVisible = ["prompt", "requesting", "error", "confirm", "manual", "manual-selected", "saving", "success"].includes(state);
  const locationLabel = regionName || (state === "checking" ? "지역 확인 중" : "내 동네를 설정해주세요");
  const manuallySelected = state === "manual-selected" && position?.source === "manual";

  return (
    <>
      <section className="region-status-bar" aria-label="현재 설정 지역">
        <div><MapPin size={22} weight="fill" /><strong>{locationLabel}</strong></div>
        <button type="button" onClick={() => setState("prompt")}>{regionName ? "지역 변경" : "지역 설정"}</button>
        <a href="/requests/new">수리 요청하기</a>
      </section>

      {modalVisible && <div className="location-modal-backdrop">
        <section className={`location-modal ${mapVisible ? "location-modal-map" : ""}`} role="dialog" aria-modal="true" aria-labelledby="location-dialog-title">
          <button className="location-modal-close" type="button" onClick={dismiss} aria-label="위치 설정 닫기"><X size={21} /></button>
          <span className={`location-modal-icon ${succeeded ? "location-modal-icon-success" : ""}`}>
            {succeeded ? <CheckCircle size={34} weight="fill" /> : <MapPin size={34} weight="duotone" />}
          </span>
          <p className="section-kicker">MY NEIGHBORHOOD</p>
          <h2 id="location-dialog-title">
            {succeeded ? "내 동네가 설정되었어요" : mapVisible ? "여기가 맞나요?" : "내 동네를 알려주세요"}
          </h2>
          <p className="location-modal-description">
            {succeeded
              ? `${regionName || "현재 지역"}을 기준으로 가까운 이웃과 연결할게요.`
              : mapVisible
                ? manuallySelected ? "파란 점이 실제 위치와 맞는지 확인해주세요." : "파란 점과 원은 브라우저가 제공한 추정 위치입니다. 실제 위치가 원 밖에 있을 수도 있습니다."
                : "현재 위치를 확인하면 가까운 지역의 수리 요청과 도움 가능한 이웃을 보여드릴 수 있어요."}
          </p>

          {mapVisible && <LocationMapPicker position={position} onPositionChange={selectPosition} />}

          {message && (
            <div className={state === "manual" || state === "manual-selected" ? "location-warning" : "location-error"} role="alert">
              <WarningCircle size={20} weight="fill" /><span>{message}</span>
            </div>
          )}

          {succeeded ? (
            <button className="primary-button mt-7 w-full justify-center" type="button" onClick={() => setState("saved")}>확인</button>
          ) : mapVisible ? (
            <div className="location-map-actions">
              <button className="secondary-button justify-center" type="button" onClick={requestLocation} disabled={state === "saving"}>
                <Crosshair size={20} weight="bold" />위치 다시 찾기
              </button>
              <button className="primary-button justify-center" type="button" onClick={savePosition} disabled={state === "saving" || state === "manual"}>
                {state === "saving" ? "지역 저장 중..." : "이 위치로 설정"}
              </button>
            </div>
          ) : (
            <div className="mt-7 grid gap-3 sm:grid-cols-2">
              <button className="secondary-button justify-center" type="button" onClick={dismiss}>나중에</button>
              <button className="primary-button justify-center" type="button" onClick={requestLocation} disabled={state === "requesting"}>
                <Crosshair size={20} weight="bold" />{state === "requesting" ? "위치 확인 중..." : "현재 위치 확인"}
              </button>
            </div>
          )}

          {!succeeded && !mapVisible && <p className="mt-4 text-center text-xs leading-5 text-slate-400">위치 정보는 행정구역 설정에만 사용되며 정확한 좌표는 프로필에 저장하지 않습니다.</p>}
          {mapVisible && <p className="mobile-location-hint"><DeviceMobile size={18} />PC에서 위치가 계속 부정확하면 휴대폰에서 접속해 GPS로 다시 시도할 수 있어요.</p>}
        </section>
      </div>}
    </>
  );
}
