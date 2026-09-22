import { cookies } from "next/headers";
import { redirect, notFound } from "next/navigation";
import RepairContract from "@/components/repair-contract";
export default async function ContractPage({ params }) {
  const { roomId } = await params;
  if (!/^\d+$/.test(roomId)) notFound();
  if (!(await cookies()).get("access_token")?.value) redirect("/login");
  return <RepairContract roomId={roomId} />;
}
