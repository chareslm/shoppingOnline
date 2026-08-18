<#
.SYNOPSIS
    Prepare configuration and start the Windows development environment.

.DESCRIPTION
    Keeps local configuration separate from committed example templates,
    validates required secrets, prepares persistent directories, starts
    Docker Compose, waits for backend health, and optionally starts Vite.

    This executable script intentionally uses ASCII text for Windows
    PowerShell 5 compatibility. Chinese usage notes are in README.md.
#>
[CmdletBinding()]
param(
    [switch]$InstallDependencies,
    [switch]$SkipFrontends,
    [switch]$Recreate
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$DeployDir = Join-Path $RepoRoot 'deploy'
$DeployEnv = Join-Path $DeployDir '.env'
$ComposeFile = Join-Path $DeployDir 'docker-compose.yml'

function Ensure-LocalCopy {
    param(
        [Parameter(Mandatory)] [string]$Example,
        [Parameter(Mandatory)] [string]$Local
    )

    if (-not (Test-Path -LiteralPath $Local)) {
        Copy-Item -LiteralPath $Example -Destination $Local
        Write-Host "Created local configuration: $Local" -ForegroundColor Yellow
    }
}

function Get-EnvValue {
    param([Parameter(Mandatory)] [string]$Name)

    $line = Get-Content -LiteralPath $DeployEnv |
        Where-Object { $_ -match "^\s*$([regex]::Escape($Name))=" } |
        Select-Object -Last 1
    if (-not $line) { return $null }
    return ($line -split '=', 2)[1].Trim()
}

function Assert-RequiredSecret {
    param([Parameter(Mandatory)] [string]$Name)

    $value = Get-EnvValue -Name $Name
    if ([string]::IsNullOrWhiteSpace($value) -or $value -like 'replace-*') {
        throw "Set $Name in deploy/.env before running this script again."
    }
}

function Test-LocalPort {
    param([Parameter(Mandatory)] [int]$Port)
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Start-Frontend {
    param(
        [Parameter(Mandatory)] [string]$Directory,
        [Parameter(Mandatory)] [int]$Port,
        [Parameter(Mandatory)] [string]$Title
    )

    if (Test-LocalPort -Port $Port) {
        Write-Host "$Title already listens on port $Port; duplicate start skipped." -ForegroundColor DarkYellow
        return
    }

    if ($InstallDependencies -or -not (Test-Path -LiteralPath (Join-Path $Directory 'node_modules'))) {
        Write-Host "Installing dependencies for $Title..."
        & npm install --prefix $Directory
        if ($LASTEXITCODE -ne 0) { throw "Dependency installation failed for $Title." }
    }

    # A separate terminal preserves Vite logs and gives the developer an obvious stop point.
    $escapedDirectory = $Directory.Replace("'", "''")
    $command = "`$host.UI.RawUI.WindowTitle='$Title'; Set-Location -LiteralPath '$escapedDirectory'; npm run dev"
    Start-Process powershell -ArgumentList '-NoExit', '-Command', $command | Out-Null
}

Ensure-LocalCopy `
    -Example (Join-Path $DeployDir '.env.example') `
    -Local $DeployEnv
Ensure-LocalCopy `
    -Example (Join-Path $DeployDir 'docker-compose.yml.example') `
    -Local $ComposeFile
Ensure-LocalCopy `
    -Example (Join-Path $RepoRoot 'backend\src\main\resources\application-local.yml.example') `
    -Local (Join-Path $RepoRoot 'backend\src\main\resources\application-local.yml')
Ensure-LocalCopy `
    -Example (Join-Path $RepoRoot 'frontend-web\.env.example') `
    -Local (Join-Path $RepoRoot 'frontend-web\.env')
Ensure-LocalCopy `
    -Example (Join-Path $RepoRoot 'frontend-admin\.env.example') `
    -Local (Join-Path $RepoRoot 'frontend-admin\.env')

Assert-RequiredSecret -Name 'MYSQL_APP_PASSWORD'
Assert-RequiredSecret -Name 'MYSQL_ROOT_PASSWORD'
Assert-RequiredSecret -Name 'JWT_SECRET'

$DataRoot = Get-EnvValue -Name 'DATA_DIR'
if ([string]::IsNullOrWhiteSpace($DataRoot)) { $DataRoot = 'D:/Project/data' }
$ProjectDataRoot = Join-Path $DataRoot 'shopping'
@('mysql', 'redis', 'elasticsearch', 'uploads') | ForEach-Object {
    New-Item -ItemType Directory -Force -Path (Join-Path $ProjectDataRoot $_) | Out-Null
}

& docker info *> $null
if ($LASTEXITCODE -ne 0) { throw 'Docker Desktop is unavailable.' }

& docker compose --env-file $DeployEnv -f $ComposeFile config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Docker Compose configuration validation failed.' }

$composeArgs = @('compose', '--env-file', $DeployEnv, '-f', $ComposeFile, 'up', '-d')
if ($Recreate) { $composeArgs += '--force-recreate' }
& docker @composeArgs
if ($LASTEXITCODE -ne 0) { throw 'Docker services failed to start.' }

$deadline = (Get-Date).AddMinutes(6)
$status = $null
do {
    Start-Sleep -Seconds 10
    try {
        $status = (Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health' -TimeoutSec 5).status
        if ($status -eq 'UP') { break }
    } catch {
        # First startup can download Maven dependencies; keep waiting until the deadline.
    }
} while ((Get-Date) -lt $deadline)

if ($status -ne 'UP') {
    & docker compose --env-file $DeployEnv -f $ComposeFile logs --tail 80 backend
    throw 'Backend did not become healthy within six minutes.'
}

if (-not $SkipFrontends) {
    Start-Frontend -Directory (Join-Path $RepoRoot 'frontend-admin') -Port 5173 -Title 'Shopping Admin'
    Start-Frontend -Directory (Join-Path $RepoRoot 'frontend-web') -Port 5174 -Title 'Shopping Web'
}

Write-Host 'Local environment is ready:' -ForegroundColor Green
Write-Host '  Admin:   http://localhost:5173'
Write-Host '  Web:     http://127.0.0.1:5174'
Write-Host '  Backend: http://localhost:8080'
