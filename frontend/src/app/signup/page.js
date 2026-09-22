import AuthShell from "@/components/auth-shell";
import SignupForm from "@/components/signup-form";

export const metadata = { title: "회원가입 | Mokoco" };
export default function SignupPage() { return <AuthShell variant="signup"><SignupForm /></AuthShell>; }
