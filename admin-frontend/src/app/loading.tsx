export default function Loading() {
  return (
    <div
      aria-label="Loading admin content"
      className="space-y-5 animate-pulse"
    >
      <div className="h-3 w-32 rounded bg-[#302a22]" />
      <div className="h-9 w-56 rounded bg-[#302a22]" />
      <div className="h-4 w-80 max-w-full rounded bg-[#24211c]" />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[1, 2, 3, 4].map((item) => (
          <div
            key={item}
            className="h-28 rounded-2xl border border-[var(--line)] bg-[var(--panel)]"
          />
        ))}
      </div>
      <div className="h-72 rounded-2xl border border-[var(--line)] bg-[var(--panel)]" />
    </div>
  );
}
