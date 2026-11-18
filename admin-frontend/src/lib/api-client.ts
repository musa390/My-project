const API_BASE_URL = (process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080").replace(/\/$/, "");

type ApiErrorPayload = { message?: string; error?: string; detail?: string };

export class ApiError extends Error {
  readonly status: number;
  readonly payload: unknown;

  constructor(message: string, status: number, payload: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.payload = payload;
  }
}

let csrfToken: string | null = null;
let csrfRequest: Promise<string> | null = null;

async function readResponse(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) return null;
  try { return JSON.parse(text) as unknown; } catch { return text; }
}

export async function getCsrfToken(forceRefresh = false): Promise<string> {
  if (csrfToken && !forceRefresh) return csrfToken;
  if (csrfRequest && !forceRefresh) return csrfRequest;

  csrfRequest = (async () => {
    const response = await fetch(`${API_BASE_URL}/api/v1/auth/csrf`, {
      method: "GET", credentials: "include", cache: "no-store",
      headers: { Accept: "application/json" },
    });
    const payload = await readResponse(response) as { token?: string } | null;
    if (!response.ok || !payload?.token) {
      csrfToken = null;
      throw new ApiError("Could not initialise secure session. Please reload and try again.", response.status, payload);
    }
    csrfToken = payload.token;
    return csrfToken;
  })();

  try { return await csrfRequest; } finally { csrfRequest = null; }
}

export type ApiRequestOptions = Omit<RequestInit, "body"> & { body?: unknown };

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const method = (options.method || "GET").toUpperCase();
  const unsafe = !["GET", "HEAD", "OPTIONS", "TRACE"].includes(method);
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");

  let body: BodyInit | undefined;
  if (options.body instanceof FormData || typeof options.body === "string" || options.body instanceof Blob) {
    body = options.body as BodyInit;
  } else if (options.body !== undefined && options.body !== null) {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(options.body);
  }

  if (unsafe) headers.set("X-XSRF-TOKEN", await getCsrfToken());

  const response = await fetch(`${API_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`, {
    ...options, method, headers, body, credentials: "include", cache: "no-store",
  });
  const payload = await readResponse(response);

  if (!response.ok) {
    if (response.status === 403 && unsafe) csrfToken = null;
    const error = payload as ApiErrorPayload | null;
    const message = error?.message || error?.detail || error?.error
      || (response.status === 401 ? "Please sign in to continue." : `Request failed (${response.status}).`);
    throw new ApiError(message, response.status, payload);
  }
  return payload as T;
}

export async function refreshSession(): Promise<void> {
  await apiRequest<{ success: boolean }>("/api/v1/auth/refresh", { method: "POST" });
}

export async function logoutSession(): Promise<void> {
  try { await apiRequest<{ success: boolean }>("/api/v1/auth/logout", { method: "POST" }); }
  finally { csrfToken = null; }
}
