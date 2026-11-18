import { apiRequest, logoutSession } from "@/lib/api-client";

export type AdminSessionUser = {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
};

type SessionResponse = { success: boolean; sessionId?: string };
type CurrentUserResponse = { success: boolean; user: AdminSessionUser };

export async function getStaffCurrentUser(): Promise<AdminSessionUser> {
  const response = await apiRequest<CurrentUserResponse>("/api/v1/auth/me");
  const user = response.user;
  if (!user.roles.some((role) => ["ADMIN", "MANAGER"].includes(role))) {
    await logoutSession();
    throw new Error("This account does not have Admin or Manager access.");
  }
  return user;
}

export async function signInAsStaff(email: string, password: string): Promise<AdminSessionUser> {
  await apiRequest<SessionResponse>("/api/v1/auth/login", {
    method: "POST",
    body: { email: email.trim().toLowerCase(), password },
  });

  try {
    return await getStaffCurrentUser();
  } catch (error) {
    if (error instanceof Error && error.message.includes("does not have Admin")) throw error;
    throw error;
  }
}

export async function signOutAdmin(): Promise<void> {
  await logoutSession();
}
