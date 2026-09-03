import { NextResponse } from "next/server";

export async function POST(request) {
  const response = NextResponse.redirect(new URL("/", request.url), 303);
  response.cookies.delete("mokoco_access_token");
  response.cookies.delete("mokoco_refresh_token");
  response.cookies.delete("mokoco_user_email");
  return response;
}
