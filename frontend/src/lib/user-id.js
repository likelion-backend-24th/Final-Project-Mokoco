import { jwtDecode } from "jwt-decode";

// Presentation only. API authorization always uses the server-verified token.
export function userIdFromToken(token) {
  try {
    const subject = jwtDecode(token).sub;
    return typeof subject === "string" && /^[1-9][0-9]*$/.test(subject) ? subject : null;
  } catch { return null; }
}