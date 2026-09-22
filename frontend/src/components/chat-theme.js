"use client";

import { useSyncExternalStore } from "react";

export const chatThemes = [
  ["lavender", "라벤더"], ["ocean", "오션 블루"], ["forest", "포레스트"], ["sand", "샌드"],
];
const key = "mokoco.chat.theme";
let fallback = "lavender";
function snapshot() {
  try {
    const saved = localStorage.getItem(key);
    return chatThemes.some(([id]) => id === saved) ? saved : fallback;
  } catch { return fallback; }
}
function subscribe(listener) {
  window.addEventListener("storage", listener);
  window.addEventListener("chat-theme-change", listener);
  return () => {
    window.removeEventListener("storage", listener);
    window.removeEventListener("chat-theme-change", listener);
  };
}
export function useChatTheme() {
  const theme = useSyncExternalStore(subscribe, snapshot, () => "lavender");
  function selectTheme(value) {
    if (!chatThemes.some(([id]) => id === value)) return;
    fallback = value;
    try { localStorage.setItem(key, value); } catch { /* Keep the choice for this session. */ }
    window.dispatchEvent(new Event("chat-theme-change"));
  }
  return [theme, selectTheme];
}
