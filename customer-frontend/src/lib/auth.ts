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

export async function signIn(email: string, password: string): Promise<CurrentUser> {
  await apiRequest<SessionResponse>("/api/v1/auth/login", {
    method: "POST",
    body: { email: email.trim().toLowerCase(), password },
  });
  return getCurrentUser();
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
  return getCurrentUser();
}

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await apiRequest<CurrentUserResponse>("/api/v1/auth/me");
  return response.user;
}

export async function signOut(): Promise<void> {
  await logoutSession();
}
