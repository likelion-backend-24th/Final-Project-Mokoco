// store/chatWidgetStore.js
import { create } from "zustand";

// 채팅 위젯(FAB+모달)의 열림 상태를 전역에서 공유한다.
// 글 상세의 "채팅방 열기" 버튼처럼 위젯 밖의 컴포넌트에서도 특정 방을
// 바로 열 수 있어야 해서(페이지 이동 없이) zustand 스토어로 뺐다.
export const useChatWidgetStore = create((set) => ({
  open: false,
  activeRoomId: null,

  openList: () => set({ open: true, activeRoomId: null }),
  openRoom: (roomId) => set({ open: true, activeRoomId: roomId }),
  backToList: () => set({ activeRoomId: null }),
  close: () => set({ open: false, activeRoomId: null }),
}));
