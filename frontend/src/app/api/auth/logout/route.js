import { NextResponse } from "next/server";

export async function POST() {
  const response = NextResponse.json({ success: true });
  
  const options = { httpOnly: true, sameSite: "lax", secure: false, path: "/", maxAge: 0 };
  
  response.cookies.set("access_token", "", options);
  response.cookies.set("refresh_token", "", options);
  response.cookies.set("user_email", "", options);
  response.cookies.set("region_code", "", options);
  response.cookies.set("region_name", "", options);

  return response;
}