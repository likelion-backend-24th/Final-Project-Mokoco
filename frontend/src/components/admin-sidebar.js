"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Gauge, Receipt, Users, Flag, FileText } from "@phosphor-icons/react";

const ITEMS = [
  { href: "/admin/overview", label: "대시보드", icon: Gauge },
  { href: "/admin/deals", label: "거래 현황", icon: Receipt },
  { href: "/admin/posts", label: "글 관리", icon: FileText },
  { href: "/admin/users", label: "회원 관리", icon: Users },
  { href: "/admin/reports", label: "신고 접수함", icon: Flag },
];

export default function AdminSidebar() {
  const pathname = usePathname();

  return (
    <aside className="w-52 shrink-0">
      <p className="mb-3 px-3 text-xs font-bold uppercase tracking-wide text-slate-400">ADMIN</p>
      <nav className="flex flex-col gap-1">
        {ITEMS.map(({ href, label, icon: Icon }) => {
          const active = pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              className={`flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-sm font-semibold transition-colors ${
                active ? "bg-blue-50 text-blue-700" : "text-slate-500 hover:bg-slate-100 hover:text-slate-700"
              }`}
            >
              <Icon size={18} weight={active ? "fill" : "regular"} />
              {label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
