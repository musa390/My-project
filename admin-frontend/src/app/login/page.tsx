"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { signInAsStaff } from "@/lib/auth";

export default function AdminLoginPage() {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    const form = event.currentTarget;
    if (!form.reportValidity()) return;
    const data = new FormData(form);
    setBusy(true);
    try {
      await signInAsStaff(String(data.get("email") || ""), String(data.get("password") || ""));
      router.replace("/");
      router.refresh();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Admin sign-in failed. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  return <div className="mx-auto mt-12 max-w-md rounded-2xl border border-[var(--line)] bg-[var(--panel)] p-7">
    <p className="text-xs uppercase tracking-[.22em] text-[var(--gold)]">Restricted workspace</p>
    <h1 className="mt-3 text-2xl font-semibold">Admin sign in</h1>
    <p className="mt-2 text-sm leading-6 text-[var(--muted)]">Sign in with an authorized Admin or Manager account.</p>
    <form onSubmit={submit} className="mt-7 space-y-4">
      <label className="block text-sm text-[#d2c9bc]">Email
        <input name="email" type="email" autoComplete="username" required maxLength={254} placeholder="admin@example.com" className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-3"/>
      </label>
      <label className="block text-sm text-[#d2c9bc]">Password
        <input name="password" type="password" autoComplete="current-password" required minLength={12} maxLength={72} placeholder="Your account password" className="mt-2 w-full rounded-lg border border-[var(--line)] bg-[#100f0d] px-3 py-3"/>
      </label>
      <button type="submit" disabled={busy} className="w-full rounded-lg bg-[#80663e] px-4 py-3 text-sm text-white disabled:cursor-wait disabled:opacity-60">{busy ? "SIGNING IN…" : "SIGN IN"}</button>
    </form>
    {error && <p className="mt-4 rounded-lg border border-red-900/70 bg-red-950/30 p-3 text-sm text-red-200" role="alert">{error}</p>}
    <p className="mt-5 text-xs leading-5 text-[var(--muted)]">Authentication is handled by the backend. Session tokens remain in HttpOnly cookies; only Admin and Manager roles can enter this workspace.</p>
  </div>;
}
