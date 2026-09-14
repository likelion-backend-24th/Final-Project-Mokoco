// store/notificationStore.js
import { create } from "zustand";

export const useNotificationStore = create((set) => ({
  items: [],
  unreadCount: 0,
  connected: false,

  setConnected: (connected) => set({ connected }),

  setAll: (items) =>
    set({
      items,
      unreadCount: items.filter((n) => !n.isRead).length,
    }),

  setUnreadCount: (unreadCount) => set({ unreadCount }),

  // 실시간으로 새 알림 수신 (중복 id 방지)
  prepend: (notification) =>
    set((state) => {
      if (state.items.some((n) => n.id === notification.id)) return state;
      return {
        items: [notification, ...state.items],
        unreadCount: state.unreadCount + (notification.isRead ? 0 : 1),
      };
    }),

  markRead: (id) =>
    set((state) => {
      const target = state.items.find((n) => n.id === id);
      if (!target || target.isRead) return state;
      return {
        items: state.items.map((n) => (n.id === id ? { ...n, isRead: true } : n)),
        unreadCount: Math.max(0, state.unreadCount - 1),
      };
    }),

  markAllRead: () =>
    set((state) => ({
      items: state.items.map((n) => ({ ...n, isRead: true })),
      unreadCount: 0,
    })),

  reset: () => set({ items: [], unreadCount: 0, connected: false }),
}));
