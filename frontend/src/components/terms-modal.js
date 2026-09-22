"use client";

import { useState } from "react";
import { FileText, X } from "@phosphor-icons/react";

const TERMS_CONTENT = `제1조 (목적)
이 약관은 동네수리(이하 "회사")가 제공하는 지역 기반 수리 매칭 서비스(이하 "서비스")의 이용과 관련하여
회사와 이용자 간의 권리, 의무 및 책임사항을 정함을 목적으로 합니다.

제2조 (정의)
1. "서비스"란 이용자가 수리 요청을 등록하고, 다른 이용자가 이에 대해 수리 제안을 등록하여
   서로 매칭될 수 있도록 회사가 제공하는 일체의 서비스를 의미합니다.
2. "회원"이란 이 약관에 동의하고 회사와 이용계약을 체결한 자를 말합니다.
3. "수리 요청"이란 회원이 고장난 물품의 수리를 요청하기 위해 등록하는 게시물을 말합니다.
4. "수리 제안"이란 다른 회원의 수리 요청에 대해 회원이 제시하는 수리 조건을 말합니다.

제3조 (약관의 효력 및 변경)
1. 이 약관은 서비스 화면에 게시하거나 기타의 방법으로 회원에게 공지함으로써 효력을 발생합니다.
2. 회사는 필요한 경우 관련 법령을 위배하지 않는 범위에서 이 약관을 변경할 수 있으며,
   변경된 약관은 서비스 내 공지사항을 통해 사전에 고지합니다.

제4조 (회원가입)
1. 이용자는 회사가 정한 가입 양식에 따라 회원정보를 기입한 후 이 약관에 동의함으로써
   회원가입을 신청합니다.
2. 회사는 다음 각 호에 해당하는 신청에 대해서는 승낙을 하지 않거나 사후에 이용계약을
   해지할 수 있습니다.
   - 타인의 명의를 이용한 경우
   - 허위의 정보를 기재한 경우
   - 기타 회원으로 등록하는 것이 서비스 운영에 현저히 지장이 있다고 판단되는 경우

제5조 (회원의 의무)
1. 회원은 수리 요청 및 수리 제안 등록 시 사실에 근거한 정보를 제공하여야 합니다.
2. 회원은 서비스를 통해 알게 된 다른 회원의 개인정보를 서비스 이용 목적 외로
   사용해서는 안 됩니다.
3. 회원 간 수리 거래 과정에서 발생하는 분쟁은 1차적으로 당사자 간 해결을 원칙으로 하며,
   회사는 원활한 분쟁 해결을 위해 필요한 지원을 할 수 있습니다.

제6조 (서비스의 중단)
회사는 시스템 점검, 교체, 고장 등의 사유가 발생한 경우 서비스 제공을 일시적으로
중단할 수 있으며, 이 경우 사전에 공지합니다. 다만 긴급한 경우 사후에 공지할 수 있습니다.

제7조 (면책)
회사는 회원 간 수리 거래와 관련하여 발생하는 손해에 대해, 회사의 고의 또는
중대한 과실이 없는 한 책임을 지지 않습니다.`;

const PRIVACY_CONTENT = `1. 수집하는 개인정보 항목
회사는 회원가입, 서비스 이용 과정에서 아래와 같은 개인정보를 수집합니다.
- 필수 항목: 이름, 닉네임, 이메일 주소, 비밀번호
- 선택 항목: 활동 지역 정보
- 서비스 이용 과정에서 자동으로 생성되는 정보: 접속 로그, 서비스 이용 기록

2. 개인정보의 수집 및 이용 목적
- 회원 식별 및 가입 의사 확인, 본인 확인
- 수리 요청·수리 제안 매칭 서비스 제공
- 활동 지역 기반 주변 수리 요청 조회 기능 제공
- 부정 이용 방지 및 서비스 운영상 문제 해결

3. 개인정보의 보유 및 이용 기간
회사는 원칙적으로 개인정보 수집 및 이용목적이 달성된 후에는 해당 정보를
지체 없이 파기합니다. 다만 관계 법령에 따라 보존할 필요가 있는 경우
해당 법령에서 정한 기간 동안 보관합니다.

4. 개인정보의 제3자 제공
회사는 원칙적으로 회원의 개인정보를 서비스 제공 목적 외의 용도로 제3자에게
제공하지 않습니다. 다만 회원이 사전에 동의한 경우, 또는 법령의 규정에
의거한 경우는 예외로 합니다.

5. 이용자의 권리
회원은 언제든지 등록되어 있는 자신의 개인정보를 조회하거나 수정할 수 있으며,
가입 해지를 요청할 수 있습니다.

6. 개인정보 보호책임자
서비스 이용 중 개인정보 관련 문의사항은 서비스 내 고객센터를 통해 접수할 수
있습니다.`;

export default function TermsModal({ trigger, onConfirm }) {
  const [open, setOpen] = useState(false);
  const [tab, setTab] = useState("terms");

  function handleConfirm() {
    onConfirm?.();
    setOpen(false);
  }

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="font-semibold text-blue-600 underline underline-offset-2 hover:text-blue-700"
      >
        {trigger}
      </button>

      {open && (
        <div
          className="location-modal-backdrop"
          role="dialog"
          aria-modal="true"
          onClick={() => setOpen(false)}
        >
          <div
            className="location-modal !max-w-[560px] !text-left"
            onClick={(event) => event.stopPropagation()}
          >
            <button
              type="button"
              className="location-modal-close"
              onClick={() => setOpen(false)}
              aria-label="닫기"
            >
              <X size={20} />
            </button>

            <span className="location-modal-icon">
              <FileText size={30} weight="duotone" />
            </span>
            <h2 className="!text-center">이용약관 및 개인정보 처리방침</h2>

            <div className="mt-5 flex gap-2 border-b border-slate-200">
              <button
                type="button"
                onClick={() => setTab("terms")}
                className={`px-4 py-2.5 text-sm font-semibold transition-colors ${
                  tab === "terms"
                    ? "border-b-2 border-blue-600 text-blue-600"
                    : "text-slate-400 hover:text-slate-600"
                }`}
              >
                이용약관
              </button>
              <button
                type="button"
                onClick={() => setTab("privacy")}
                className={`px-4 py-2.5 text-sm font-semibold transition-colors ${
                  tab === "privacy"
                    ? "border-b-2 border-blue-600 text-blue-600"
                    : "text-slate-400 hover:text-slate-600"
                }`}
              >
                개인정보 처리방침
              </button>
            </div>

            <div className="mt-4 max-h-[360px] overflow-y-auto whitespace-pre-line rounded-xl bg-slate-50 p-5 text-left text-[13px] leading-relaxed text-slate-600">
              {tab === "terms" ? TERMS_CONTENT : PRIVACY_CONTENT}
            </div>

            <button
              type="button"
              onClick={handleConfirm}
              className="primary-button mt-5 w-full justify-center"
            >
              확인 및 동의
            </button>
          </div>
        </div>
      )}
    </>
  );
}
