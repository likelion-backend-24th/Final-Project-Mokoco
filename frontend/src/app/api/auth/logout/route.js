import { NextResponse } from "next/server";

export async function POST(request) {
  const response = NextResponse.redirect(new URL("/", request.url), 303);
  
  // path를 지정해서 확실하게 쿠키 파괴
  response.cookies.set("access_token", "", { maxAge: 0, path: "/" });
  response.cookies.set("refresh_token", "", { maxAge: 0, path: "/" });
  response.cookies.set("user_email", "", { maxAge: 0, path: "/" });
  
  return response;
}