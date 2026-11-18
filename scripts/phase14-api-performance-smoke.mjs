const baseUrl = process.env.PERF_URL ?? "http://127.0.0.1:8080/actuator/health";
const samples = [];

async function measure() {
  const started = performance.now();
  const response = await fetch(baseUrl, { signal: AbortSignal.timeout(5000) });
  const elapsed = performance.now() - started;
  if (!response.ok) throw new Error(`Health endpoint returned HTTP ${response.status}`);
  const body = await response.json();
  if (body.status !== "UP") throw new Error(`Application health is ${body.status}`);
  return elapsed;
}

for (let i = 0; i < 40; i += 1) samples.push(await measure());
const concurrent = await Promise.all(Array.from({ length: 10 }, () => measure()));
samples.push(...concurrent);
const sorted = [...samples].sort((a, b) => a - b);
const p50 = sorted[Math.floor((sorted.length - 1) * 0.50)];
const p95 = sorted[Math.ceil(sorted.length * 0.95) - 1];
const max = sorted.at(-1);

console.log(`API health latency smoke: ${samples.length} requests`);
console.log(`p50=${p50.toFixed(1)}ms p95=${p95.toFixed(1)}ms max=${max.toFixed(1)}ms`);
if (p95 > 2000 || max > 5000) {
  console.error("Performance smoke exceeded the CI safety thresholds.");
  process.exitCode = 1;
} else {
  console.log("Performance smoke: PASS (CI health-endpoint baseline only)");
}
