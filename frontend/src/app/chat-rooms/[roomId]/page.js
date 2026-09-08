import { cookies } from "next/headers";
import { redirect, notFound } from "next/navigation";
import ChatRoom from "@/components/chat-room";

export default async function ChatRoomPage({ params }) {
  const { roomId } = await params;
  if (!/^\d+$/.test(roomId)) notFound();
  if (!(await cookies()).get("access_token")?.value) redirect("/login");
  return <ChatRoom roomId={roomId} />;
}
