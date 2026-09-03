import AuthShell from "@/components/auth-shell";
import LoginForm from "@/components/login-form";

export const metadata = { title: "로그인 | Mokoco" };
export default async function LoginPage({ searchParams }) {
  const { registered } = await searchParams;
  return <AuthShell variant="login"><LoginForm registered={registered === "true"} /></AuthShell>;
}
