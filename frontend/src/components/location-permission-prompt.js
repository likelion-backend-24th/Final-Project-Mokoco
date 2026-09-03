"use client";

import { useEffect, useState } from "react";
import { CheckCircle, Crosshair, MapPin, WarningCircle, X } from "@phosphor-icons/react";
import { formatRegionName, getGeolocationErrorMessage } from "@/lib/region";

export default function LocationPermissionPrompt({ userEmail }) {
  const [state, setState] = useState("checking");
  const [message, setMessage] = useState("");
  const [regionName, setRegionName] = useState("");
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
  }

  function requestLocation() {
    setMessage("");

    if (!("geolocation" in navigator)) {
      setMessage("이 브라우저에서는 위치 기능을 지원하지 않습니다.");
      setState("error");
      return;
    }

    setState("requesting");
    navigator.geolocation.getCurrentPosition(
      async ({ coords }) => {
        try {
          const response = await fetch("/api/users/me/region", {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ latitude: coords.latitude, longitude: coords.longitude }),
          });
          const payload = await response.json();

          if (!response.ok) {
            setMessage(payload.message ?? "지역 정보를 저장하지 못했습니다.");
            setState("error");
            return;
          }

          setRegionName(formatRegionName(payload));
          window.sessionStorage.removeItem(dismissalKey);
          setState("success");
        } catch {
          setMessage("지역 정보 서버와 통신할 수 없습니다.");
          setState("error");
        }
      },
      (error) => {
        setMessage(getGeolocationErrorMessage(error.code));
        setState("error");
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 },
    );
  }

  if (state === "checking" || state === "dismissed" || state === "saved") return null;

  const succeeded = state === "success";
  return (
    <div className="location-modal-backdrop">
      <section className="location-modal" role="dialog" aria-modal="true" aria-labelledby="location-dialog-title">
        <button className="location-modal-close" type="button" onClick={dismiss} aria-label="위치 설정 닫기"><X size={21} /></button>
        <span className={`location-modal-icon ${succeeded ? "location-modal-icon-success" : ""}`}>
          {succeeded ? <CheckCircle size={34} weight="fill" /> : <MapPin size={34} weight="duotone" />}
        </span>
        <p className="section-kicker">MY NEIGHBORHOOD</p>
        <h2 id="location-dialog-title">{succeeded ? "내 동네가 설정되었어요" : "내 동네를 알려주세요"}</h2>
        <p className="location-modal-description">
          {succeeded
            ? `${regionName || "현재 지역"}을 기준으로 가까운 이웃과 연결할게요.`
            : "현재 위치를 확인하면 가까운 지역의 수리 요청과 도움 가능한 이웃을 보여드릴 수 있어요."}
        </p>

        {state === "error" && (
          <div className="location-error" role="alert"><WarningCircle size={20} weight="fill" /><span>{message}</span></div>
        )}

        {succeeded ? (
          <button className="primary-button mt-7 w-full justify-center" type="button" onClick={() => setState("saved")}>확인</button>
        ) : (
          <div className="mt-7 grid gap-3 sm:grid-cols-2">
            <button className="secondary-button justify-center" type="button" onClick={dismiss}>나중에</button>
            <button className="primary-button justify-center" type="button" onClick={requestLocation} disabled={state === "requesting"}>
              <Crosshair size={20} weight="bold" />{state === "requesting" ? "위치 확인 중..." : "위치 허용"}
            </button>
          </div>
        )}
        {!succeeded && <p className="mt-4 text-center text-xs leading-5 text-slate-400">위치 정보는 행정구역 설정에만 사용되며 정확한 좌표는 프로필에 저장하지 않습니다.</p>}
      </section>
    </div>
  );
}
