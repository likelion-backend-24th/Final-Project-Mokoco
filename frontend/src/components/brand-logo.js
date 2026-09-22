import Link from "next/link";
import { HouseLine } from "@phosphor-icons/react/dist/ssr";

export default function BrandLogo() {
  return (
    <Link href="/" className="flex items-center gap-2 text-blue-600" aria-label="Mokoco 홈">
      <HouseLine size={33} weight="bold" />
      <span className="text-xl font-extrabold tracking-[-0.04em]">동네수리</span>
    </Link>
  );
}
