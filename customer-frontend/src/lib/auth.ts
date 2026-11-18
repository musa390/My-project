import { apiRequest, logoutSession } from "@/lib/api-client";

export type CurrentUser = {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
};

type SessionResponse = { success: boolean; sessionId?: string };
type CurrentUserResponse = { success: boolean; user: CurrentUser };

function announceSessionChange(): void {
  if (typeof window !== "undefined") window.dispatchEvent(new Event("luxe:session-changed"));
}

export async function signIn(email: string, password: string): Promise<CurrentUser> {
  await apiRequest<SessionResponse>("/api/v1/auth/login", {
    method: "POST",
    body: { email: email.trim().toLowerCase(), password },
  });
  const user = await getCurrentUser();
  announceSessionChange();
  return user;
}

export async function registerAccount(input: {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}): Promise<CurrentUser> {
  await apiRequest<SessionResponse>("/api/v1/auth/register", {
    method: "POST",
    body: {
      email: input.email.trim().toLowerCase(),
      password: input.password,
      firstName: input.firstName.trim(),
      lastName: input.lastName.trim(),
    },
  });
  const user = await getCurrentUser();
  announceSessionChange();
  return user;
}

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await apiRequest<CurrentUserResponse>("/api/v1/auth/me");
  return response.user;
}

export async function signOut(): Promise<void> {
  await logoutSession();
  announceSessionChange();
}

export async function requestPasswordReset(email: string): Promise<void> {
  await apiRequest<{ success: boolean; message?: string }>("/api/v1/auth/forgot-password", {
    method: "POST",
    body: { email: email.trim().toLowerCase() },
  });
}

export async function resetPassword(token: string, newPassword: string): Promise<void> {
  await apiRequest<{ success: boolean; message?: string }>("/api/v1/auth/reset-password", {
    method: "POST",
    body: { token, newPassword },
  });
}
