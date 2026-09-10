"use client";
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import "./repair-contract.css";

const fields = [
  ["title", "계약 제목", "text", 120], ["scope", "작업 대상과 수리 범위", "area", 4000],
  ["exclusions", "작업에서 제외되는 사항", "area", 2000], ["materials", "부품·자재와 비용 부담", "area", 2000],
  ["totalAmount", "총 계약금액 (원, 부품비·출장비·세금 포함)", "number"],
  ["paymentTerms", "지급 시점과 방법", "area", 2000], ["startDate", "작업 시작일", "date"], ["endDate", "완료 예정일", "date"],
  ["workLocation", "작업 장소·물품 전달 및 반환 방법", "area", 1000],
  ["acceptanceCriteria", "완료·검수 기준", "area", 2000], ["warrantyTerms", "하자 보수 범위와 기간", "area", 2000],
  ["cancellationTerms", "취소·지연·파손·수리 불가 시 처리", "area", 2000],
  ["additionalCostTerms", "추가 작업·비용의 사전 승인 절차", "area", 2000],
];
const labels = { DRAFT: "초안", SIGNING: "서명 대기", SIGNED: "체결 완료", SUPERSEDED: "이전 버전" };
const dealLabels = { MATCHED: "작업 전", REPAIRING: "수리 진행 중", REPAIR_DONE: "완료 확인 대기", COMPLETED: "거래 완료", CANCELED: "거래 취소", PRODUCT_SENT: "물품 전달" };
const initial = Object.fromEntries(fields.map(([key]) => [key, ""]));
const dateTime = value => value ? new Date(value).toLocaleString("ko-KR") : "—";

function SignatureForm({ version, consentText, busy, onSign }) {
  const [name, setName] = useState("");
  const [consent, setConsent] = useState(false);
  return <form className="contract-sign contract-controls" onSubmit={event => {
    event.preventDefault();
    if (window.confirm(`버전 ${version.revision} 계약에 '${name.trim()}' 이름으로 서명하시겠습니까? 양측 서명 후에는 수정할 수 없습니다.`))
      onSign({ signerName: name.trim(), consent, documentHash: version.documentHash });
  }}>
    <h2>전자서명</h2>
    <p>로그인 계정에 서명 기록이 남습니다. 별도 본인인증이나 공인 전자서명 서비스는 연동되지 않았습니다.</p>
    <label>서명 성명<input required maxLength={80} value={name} onChange={e => setName(e.target.value)} autoComplete="name" /></label>
    <label className="contract-check"><input type="checkbox" required checked={consent} onChange={e => setConsent(e.target.checked)} />{consentText}</label>
    <button disabled={busy || !consent || !name.trim()}>버전 {version.revision}에 서명하기</button>
  </form>;
}

export default function RepairContract({ roomId }) {
  const [overview, setOverview] = useState(null);
  const [userId, setUserId] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const endpoint = `/api/chat-rooms/${roomId}/contract`;
  const load = useCallback(async () => {
    const response = await fetch(endpoint, { cache: "no-store" });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error);
    setOverview(data);
    setSelectedId(current => current ?? data.versions[0]?.id ?? null);
    return data;
  }, [endpoint]);
  useEffect(() => {
    let active = true;
    async function refresh() { try { if (active) await load(); } catch (failure) { if (active) setError(failure.message); } }
    refresh();
    fetch("/api/chat/session", { cache: "no-store" }).then(async response => {
      const data = await response.json(); if (!response.ok) throw new Error(data.error);
      if (active) setUserId(data.userId);
    }).catch(failure => { if (active) setError(failure.message); });
    const interval = setInterval(() => { if (document.visibilityState === "visible") refresh(); }, 15000);
    window.addEventListener("focus", refresh);
    return () => { active = false; clearInterval(interval); window.removeEventListener("focus", refresh); };
  }, [load]);
  const latest = overview?.versions[0];
  const selected = overview?.versions.find(version => version.id === selectedId) ?? latest;
  const isLatest = selected?.id === latest?.id;
  async function mutate(action, body) {
    setBusy(true); setError(""); setNotice("");
    try {
      const response = await fetch(`${endpoint}${action ? `/${action}` : ""}`, {
        method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error);
      const updated = await load(); setSelectedId(updated.versions[0]?.id ?? null);
      setEditing(null); setNotice("저장되었습니다.");
    } catch (failure) { setError(failure.message); }
    finally { setBusy(false); }
  }
  function advance(action, prompt) {
    if (window.confirm(prompt)) mutate(action, { versionId: latest.id });
  }
  function download() {
    const blob = new Blob([JSON.stringify({ roomId, requesterId: overview.requesterId, repairerId: overview.repairerId, consentText: overview.consentText, contract: selected }, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob); const anchor = document.createElement("a");
    anchor.href = url; anchor.download = `repair-contract-${roomId}-v${selected.revision}.json`; anchor.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  return <main className="contract-page">
    <header className="contract-controls"><Link href={`/chat-rooms/${roomId}`}>← 채팅으로 돌아가기</Link><h1>수리 계약서</h1>
      <p>작업 범위와 조건을 확인하고 같은 계약에 양측이 서명하세요.</p></header>
    {error && <div role="alert" className="contract-error contract-controls">{error}<button type="button" onClick={() => load().then(() => setError("")).catch(e => setError(e.message))}>다시 불러오기</button></div>}
    {notice && <p role="status" className="contract-controls">{notice}</p>}
    {!overview ? <p>계약 정보를 불러오는 중입니다.</p> : <>
      <div className="contract-toolbar contract-controls"><strong>{dealLabels[overview.dealStatus] || overview.dealStatus}</strong>
        {overview.versions.length > 0 && <select aria-label="계약 버전" value={selected?.id || ""} onChange={e => { setSelectedId(Number(e.target.value)); setEditing(null); }}>{overview.versions.map(version => <option key={version.id} value={version.id}>버전 {version.revision} · {labels[version.status]}</option>)}</select>}
        {latest?.status !== "SIGNED" && overview.dealStatus === "MATCHED" && <button disabled={busy || userId === null} onClick={() => setEditing({ baseId: latest?.id ?? null, terms: latest?.terms ? { ...latest.terms } : { ...initial } })}>{latest ? "수정본 작성" : "계약 초안 작성"}</button>}
      </div>
      {editing ? <form className="contract-editor contract-controls" onSubmit={event => {
        event.preventDefault();
        if (latest && !window.confirm("새 버전으로 저장합니다. 기존 버전의 서명은 새 버전에 적용되지 않습니다. 계속하시겠습니까?")) return;
        mutate("", editing);
      }}>
        <h2>{editing.baseId ? "계약 수정본" : "새 계약 초안"}</h2><p>모든 항목을 작성해주세요. 해당 사항이 없으면 ‘없음’을 입력하세요.</p>
        {fields.map(([key, label, type, max]) => <label key={key}>{label}
          {type === "area" ? <textarea required maxLength={max} rows={3} value={editing.terms[key]} onChange={e => setEditing({ ...editing, terms: { ...editing.terms, [key]: e.target.value } })} />
            : <input required type={type} maxLength={max} min={type === "number" ? "0.01" : undefined} max={type === "number" ? "9999999999.99" : undefined} step={type === "number" ? "0.01" : undefined} value={editing.terms[key]} onChange={e => setEditing({ ...editing, terms: { ...editing.terms, [key]: e.target.value } })} />}
        </label>)}
        <div className="contract-toolbar"><button disabled={busy}>초안 저장</button><button type="button" disabled={busy} onClick={() => setEditing(null)}>취소</button></div>
      </form> : selected ? <>
        {!isLatest && <p className="contract-error contract-controls">이전 계약 버전입니다. 최신 버전을 선택해 진행해주세요.</p>}
        <article className="contract-document">
          <div className="contract-document-heading"><span>수리 도급계약 · 버전 {selected.revision}</span><strong>{labels[selected.status]}</strong></div>
          <h2>{selected.terms.title}</h2>
          <p>의뢰인 계정 #{overview.requesterId} · 수리자 계정 #{overview.repairerId} · 채팅방 #{roomId}</p>
          <p>작성: {dateTime(selected.createdAt)} · 체결: {dateTime(selected.signedAt)}</p>
          <dl>{fields.filter(([key]) => key !== "title").map(([key, label]) => <div key={key}><dt>{label}</dt><dd>{key === "totalAmount" ? `${Number(selected.terms[key]).toLocaleString("ko-KR")}원` : selected.terms[key]}</dd></div>)}</dl>
          <h3>서명 기록</h3><p>{overview.consentText}</p>
          <div className="contract-signatures">{[[overview.requesterId, "의뢰인"], [overview.repairerId, "수리자"]].map(([id, role]) => {
            const signature = selected.signatures.find(item => item.signerId === id);
            return <section key={id}><strong>{role} · 계정 #{id}</strong><p>{signature ? signature.signerName : "미서명"}</p><small>{signature ? dateTime(signature.signedAt) : "서명을 기다리고 있습니다."}</small></section>;
          })}</div>
          <p className="contract-hash">문서 SHA-256: {selected.documentHash}</p>
          <small>로그인 계정 기반 전자서명 기록입니다. 별도 본인인증·외부 전자서명 서비스는 미연동입니다.</small>
        </article>
        <div className="contract-toolbar contract-controls"><button onClick={() => window.print()}>인쇄 / PDF 저장</button><button onClick={download}>계약·서명 기록 저장</button></div>
        {isLatest && selected.status === "DRAFT" && <button className="contract-primary contract-controls" disabled={busy || userId === null} onClick={() => mutate("request", { versionId: selected.id })}>이 버전으로 양측 서명 요청</button>}
        {isLatest && selected.status === "SIGNING" && userId !== null && !selected.signatures.some(s => s.signerId === userId) && <SignatureForm key={selected.id} version={selected} consentText={overview.consentText} busy={busy} onSign={body => mutate("sign", { versionId: selected.id, ...body })} />}
        {isLatest && selected.status === "SIGNING" && selected.signatures.some(s => s.signerId === userId) && <p className="contract-controls">내 서명이 저장되었습니다. 상대방 서명을 기다리고 있습니다.</p>}
        {isLatest && selected.status === "SIGNED" && <section className="contract-sign contract-controls"><h2>계약 체결 완료</h2><p>양측 서명이 완료되었습니다. 위 계약 내용을 기준으로 작업을 진행하세요.</p>
          {userId === overview.repairerId && overview.dealStatus === "MATCHED" && <button disabled={busy} onClick={() => advance("start", "체결된 계약에 따라 수리 작업을 시작하시겠습니까?")}>수리 작업 시작</button>}
          {userId === overview.repairerId && overview.dealStatus === "REPAIRING" && <button disabled={busy} onClick={() => advance("finish", "작업을 마치고 의뢰인에게 완료 확인을 요청하시겠습니까?")}>작업 완료 확인 요청</button>}
          {userId === overview.requesterId && overview.dealStatus === "REPAIR_DONE" && <button disabled={busy} onClick={() => advance("accept", "계약의 검수 기준을 확인하고 수리 완료를 수락하시겠습니까?")}>검수 및 수리 완료 확인</button>}
        </section>}
      </> : <p className="contract-sign">아직 계약서가 없습니다. 채팅에서 합의한 조건으로 초안을 작성해주세요.</p>}
    </>}
  </main>;
}
