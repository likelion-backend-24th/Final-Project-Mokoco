// FixDealStatus 문구를 한 곳에서만 정의한다 — 홈 화면 "진행중인 거래" 카드와 프로필 거래
// 내역 탭이 각자 다른 단어를 쓰다 보니(예: MATCHED를 "매칭 완료"/"이웃과 연결됨"으로 다르게
// 표시) 같은 거래를 두 화면에서 다르게 보이게 만든 적이 있었다.
// 게시글 목록의 Post.status(WAITING/MATCHED/COMPLETED, 더 뭉뚱그린 상태)와 겹치는 MATCHED는
// 그쪽과 같은 문구("이웃과 연결됨")로 맞춘다.
export const DEAL_STATUS_LABEL = {
  MATCHED: "이웃과 연결됨",
  PRODUCT_SENT: "제품 전달 완료",
  REPAIRING: "수리 진행중",
  REPAIR_DONE: "수리완료 신청됨",
  COMPLETED: "거래 완료",
  CANCELED: "거래 취소됨",
};
