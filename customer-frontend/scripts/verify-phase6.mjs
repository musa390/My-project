import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const checks = [];
const exists = (file) => fs.existsSync(path.join(root, file));
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8');
const check = (name, passed, detail) => checks.push({ name, status: passed ? 'PASS' : 'FAIL', detail });

const packageJson = JSON.parse(read('package.json'));
check('Next.js App Router source', exists('src/app/layout.tsx') && exists('src/app/page.tsx'), 'Root layout and homepage source are present.');
check('Phase 1–5 source modules', ['src/components/store.tsx', 'src/lib/products.ts', 'src/lib/site-config.ts', 'src/lib/brand-content.ts'].every(exists), 'Shared store, product data and site content modules are present.');
check('Required npm scripts', ['install', 'dev', 'build', 'start', 'lint', 'typecheck'].every((name) => name === 'install' || name === 'dev' || name === 'build' || name === 'start' || name === 'lint' || name === 'typecheck') && ['dev', 'build', 'start', 'lint', 'typecheck'].every((name) => typeof packageJson.scripts?.[name] === 'string'), 'Development, build, start, lint and typecheck scripts are declared.');
check('SEO metadata', /openGraph/.test(read('src/app/layout.tsx')) && /twitter/.test(read('src/app/layout.tsx')), 'Global Open Graph and Twitter metadata are configured.');
check('Sitemap route', exists('src/app/sitemap.ts') && /products\.map/.test(read('src/app/sitemap.ts')), 'Sitemap source derives product URLs from the product catalogue.');
check('Robots route', exists('src/app/robots.ts') && /disallow/.test(read('src/app/robots.ts')), 'Robots route includes private/demo route exclusions.');
check('Loading UI', exists('src/app/loading.tsx') && /aria-busy/.test(read('src/app/loading.tsx')), 'Shared route loading skeleton and busy announcement are present.');
check('Skip link and focus styling', /skip-link/.test(read('src/app/layout.tsx')) && /:focus-visible/.test(read('src/app/globals.css')), 'Keyboard skip link and visible focus styling are present.');
check('Responsive breakpoints', /max-width: 900px/.test(read('src/app/globals.css')) && /max-width: 650px/.test(read('src/app/globals.css')) && /max-width: 380px/.test(read('src/app/globals.css')), 'Tablet, mobile and narrow mobile CSS breakpoints are present.');
check('Reduced motion', /prefers-reduced-motion/.test(read('src/app/globals.css')), 'Reduced-motion preference is respected.');
check('Image optimization configuration', /formats: \["image\/avif", "image\/webp"\]/.test(read('next.config.ts')), 'Next.js AVIF/WebP output formats are configured.');
check('Phase 6 documentation', exists('PHASE6-FINAL-VERIFICATION-REPORT.md') && exists('README.md'), 'Phase 6 report and project README are present.');

for (const result of checks) console.log(`${result.status.padEnd(4)} ${result.name} — ${result.detail}`);
const failed = checks.filter((item) => item.status === 'FAIL').length;
console.log(`\nStatic source checks: ${checks.length - failed}/${checks.length} passed.`);
if (failed) process.exitCode = 1;
