# Dot-source from the backend directory: . .\scripts\load-env.ps1
# Loads root .env into this PowerShell process without printing secret values.
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$envFile = Join-Path $repoRoot '.env'

if (-not (Test-Path $envFile)) {
    throw "Root .env was not found. Copy .env.example to .env and set local MySQL passwords."
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) { return }

    $parts = $line -split '=', 2
    if ($parts.Count -ne 2) { return }

    $key = $parts[0].Trim()
    $value = $parts[1].Trim()
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    if (($key -eq 'MYSQL_PASSWORD' -or $key -eq 'MYSQL_ROOT_PASSWORD') -and
        $value.StartsWith('replace_with_')) {
        throw "Replace the local MySQL password placeholders in root .env before starting the backend."
    }

    if ($key -match '^[A-Za-z_][A-Za-z0-9_]*$') {
        [Environment]::SetEnvironmentVariable($key, $value, 'Process')
    }
}
Write-Host 'Loaded root .env variables into this PowerShell session. Secret values were not displayed.'
