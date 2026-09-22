import { cookies } from "next/headers";
import { redirect, notFound } from "next/navigation";
import SiteHeader from "@/components/site-header";
import ChatRoom from "@/components/chat-room";

export default async function ChatRoomPage({ params }) {
  const { roomId } = await params;
  if (!/^\d+$/.test(roomId)) notFound();
  const cookieStore = await cookies();
  if (!cookieStore.get("access_token")?.value) redirect("/login");
  const userEmail = cookieStore.get("user_email")?.value ?? null;
  return (
    <div className="flex min-h-screen flex-col bg-[#f7f9fc]">
      <SiteHeader userEmail={userEmail} />
      <ChatRoom roomId={roomId} />
    </div>
  );
}
