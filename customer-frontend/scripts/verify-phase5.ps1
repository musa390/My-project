$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

Write-Host "[1/5] Installing platform-specific dependencies..." -ForegroundColor Cyan
npm ci
if ($LASTEXITCODE -ne 0) { throw "npm ci failed with exit code $LASTEXITCODE" }

Write-Host "[2/5] TypeScript check..." -ForegroundColor Cyan
npm run typecheck
if ($LASTEXITCODE -ne 0) { throw "TypeScript check failed with exit code $LASTEXITCODE" }

Write-Host "[3/5] ESLint check..." -ForegroundColor Cyan
npm run lint
if ($LASTEXITCODE -ne 0) { throw "ESLint failed with exit code $LASTEXITCODE" }

Write-Host "[4/5] Production build..." -ForegroundColor Cyan
npm run build
if ($LASTEXITCODE -ne 0) { throw "Next.js production build failed with exit code $LASTEXITCODE" }

Write-Host "[5/5] Production route smoke test..." -ForegroundColor Cyan
$port = 3187
$baseUrl = "http://127.0.0.1:$port"
$stdout = Join-Path $ProjectRoot "phase5-server.stdout.log"
$stderr = Join-Path $ProjectRoot "phase5-server.stderr.log"
$server = Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "npm run start -- --hostname 127.0.0.1 --port $port" -WorkingDirectory $ProjectRoot -PassThru -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr

try {
    $ready = $false
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 1
        try {
            $response = Invoke-WebRequest -Uri $baseUrl -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -eq 200) { $ready = $true; break }
        } catch { }
        if ($server.HasExited) { throw "Next.js server exited early. Check phase5-server.stdout.log and phase5-server.stderr.log." }
    }
    if (-not $ready) { throw "Next.js server did not become ready. Check phase5-server logs." }

    $routes = @(
        "/", "/shop", "/search", "/best-sellers", "/new-arrivals",
        "/collections", "/collections/signature", "/collections/lumi%C3%A8re",
        "/collections/oud", "/collections/fresh", "/collections/floral",
        "/collections/woody", "/collections/amber", "/collections/citrus",
        "/products/eclat-noir", "/products/lumiere-doree", "/products/oud-imperial",
        "/products/vert-eternel", "/products/rose-velours", "/products/ambre-sacre",
        "/products/bois-mystique", "/products/fleur-de-nuit", "/products/ambre-velours",
        "/products/rose-de-minuit", "/products/citrus-atelier", "/products/bois-de-soir",
        "/cart", "/checkout", "/order-success", "/wishlist", "/account", "/login",
        "/my-orders", "/orders", "/track-order", "/about", "/contact", "/faq",
        "/shipping-policy", "/return-refund-policy", "/privacy-policy",
        "/terms-conditions", "/policies"
    )
    $failed = @()
    foreach ($route in $routes) {
        try {
            $r = Invoke-WebRequest -Uri ($baseUrl + $route) -UseBasicParsing -TimeoutSec 15
            if ($r.StatusCode -ne 200) { $failed += "$route => HTTP $($r.StatusCode)" }
            else { Write-Host "PASS $route" -ForegroundColor Green }
        } catch {
            $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { $_.Exception.Message }
            $failed += "$route => $status"
        }
    }
    if ($failed.Count -gt 0) {
        $failed | ForEach-Object { Write-Host "FAIL $_" -ForegroundColor Red }
        throw "$($failed.Count) route smoke test(s) failed."
    }
    Write-Host "PASS: all $($routes.Count) route smoke tests returned HTTP 200." -ForegroundColor Green
    Write-Host "Next: complete the manual desktop/mobile browser checklist in BROWSER-QA-CHECKLIST.md." -ForegroundColor Yellow
}
finally {
    if ($server -and -not $server.HasExited) {
        Stop-Process -Id $server.Id -Force -ErrorAction SilentlyContinue
    }
}
