# Phase 14 local verification helper for Windows PowerShell 5.1+
# Loads only non-empty KEY=VALUE entries from the project-root .env into this process.
# Never prints secret values. It does not reset, drop, or migrate the database destructively.
$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot ".env"

if (!(Test-Path -LiteralPath $envFile)) {
    throw "Project-root .env was not found. Create it from .env.example and configure local values."
}

Get-Content -LiteralPath $envFile | ForEach-Object {
    $line = $_
    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        $key = $Matches[1]
        $value = $Matches[2].Trim()
        if ($value.Length -ge 2 -and
            (($value.StartsWith('"') -and $value.EndsWith('"')) -or
             ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        Set-Item -Path "Env:$key" -Value $value
    }
}

if ([string]::IsNullOrWhiteSpace($env:MYSQL_PORT)) { $env:MYSQL_PORT = "3308" }
if ([string]::IsNullOrWhiteSpace($env:MYSQL_DATABASE)) { $env:MYSQL_DATABASE = "luxe_perfume" }
if ([string]::IsNullOrWhiteSpace($env:MYSQL_USER)) { $env:MYSQL_USER = "luxe_app" }
if ([string]::IsNullOrWhiteSpace($env:MYSQL_PASSWORD)) { throw "MYSQL_PASSWORD is missing from .env." }
if ([string]::IsNullOrWhiteSpace($env:DB_HOST)) { $env:DB_HOST = "127.0.0.1" }

Write-Host "Phase 14 local backend verification"
Write-Host "Database endpoint: $($env:DB_HOST):$($env:MYSQL_PORT)/$($env:MYSQL_DATABASE)"
Write-Host "Database user: $($env:MYSQL_USER)"
Write-Host "Password loaded: $([bool]$env:MYSQL_PASSWORD)"
Write-Host ""

Push-Location (Join-Path $projectRoot "backend")
try {
    & mvn --batch-mode --no-transfer-progress clean verify
    if ($LASTEXITCODE -ne 0) { throw "Maven verification failed with exit code $LASTEXITCODE." }
} finally {
    Pop-Location
}
Write-Host "PHASE 14 LOCAL BACKEND VERIFICATION: PASS" -ForegroundColor Green
