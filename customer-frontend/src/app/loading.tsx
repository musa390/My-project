export default function Loading() {
  return (
    <main className="page wrap" aria-busy="true" aria-live="polite" aria-label="Loading page">
      <div className="loading-heading">
        <span className="skeleton skeleton-eyebrow" />
        <span className="skeleton skeleton-title" />
        <span className="skeleton skeleton-copy" />
      </div>
      <div className="loading-grid">
        {Array.from({ length: 4 }, (_, index) => (
          <div className="loading-card" key={index}>
            <span className="skeleton skeleton-image" />
            <span className="skeleton skeleton-line" />
            <span className="skeleton skeleton-line short" />
          </div>
        ))}
      </div>
      <span className="sr-only">Loading LUXE PERFUME content…</span>
    </main>
  );
}
