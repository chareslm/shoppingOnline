<#
.SYNOPSIS
    Prepare configuration and start the Windows development environment.

.DESCRIPTION
    Copies gitignored local files from examples when missing, fills empty
    path/secret placeholders, starts Docker Compose, waits until the backend
    health endpoint is UP, then starts Vite. Committed application.yml and
    docker-compose.yml are never overwritten.

    Chinese usage notes: scripts/windows/README.md
#>
[CmdletBinding()]
param(
    [switch]$InstallDependencies,
    [switch]$SkipFrontends,
    [switch]$Recreate
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'local-env.ps1')

$RepoRoot = Get-RepoRoot
$DeployEnv = Get-DeployEnvPath
$ComposeFile = Get-ComposeFilePath

$createdEnv = Ensure-LocalCopy `
    -Example (Join-Path (Get-DeployDir) '.env.example') `
    -Local $DeployEnv
Initialize-DeployEnv -File $DeployEnv -Fresh:$createdEnv

Ensure-LocalCopy `
    -Example (Join-Path $RepoRoot 'backend\src\main\resources\application-local.yml.example') `
    -Local (Join-Path $RepoRoot 'backend\src\main\resources\application-local.yml') | Out-Null
Ensure-LocalCopy `
    -Example (Join-Path $RepoRoot 'frontend-web\.env.example') `
    -Local (Join-Path $RepoRoot 'frontend-web\.env') | Out-Null
Ensure-LocalCopy `
    -Example (Join-Path $RepoRoot 'frontend-admin\.env.example') `
    -Local (Join-Path $RepoRoot 'frontend-admin\.env') | Out-Null

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

    $escapedDirectory = $Directory.Replace("'", "''")
    $command = "`$host.UI.RawUI.WindowTitle='$Title'; Set-Location -LiteralPath '$escapedDirectory'; npm run dev"
    Start-Process powershell -ArgumentList '-NoExit', '-Command', $command | Out-Null
}

$DataRoot = Get-EnvValue -Name 'DATA_DIR'
@('mysql', 'redis', 'elasticsearch', 'uploads') | ForEach-Object {
    New-Item -ItemType Directory -Force -Path (Join-Path $DataRoot "shopping\$_") | Out-Null
}

$mavenRepo = Get-EnvValue -Name 'MAVEN_REPO_DIR'
if (-not [string]::IsNullOrWhiteSpace($mavenRepo)) {
    New-Item -ItemType Directory -Force -Path $mavenRepo | Out-Null
}

& docker info *> $null
if ($LASTEXITCODE -ne 0) { throw 'Docker Desktop is unavailable.' }

& docker compose --env-file $DeployEnv -f $ComposeFile config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Docker Compose configuration validation failed.' }

$composeArgs = @('compose', '--env-file', $DeployEnv, '-f', $ComposeFile, 'up', '-d')
if ($Recreate) { $composeArgs += '--force-recreate' }
& docker @composeArgs
if ($LASTEXITCODE -ne 0) { throw 'Docker services failed to start.' }

$healthUrl = Get-BackendHealthUrl
& powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'wait-backend.ps1') -TimeoutSeconds 360 -HealthUrl $healthUrl
if ($LASTEXITCODE -ne 0) {
    & docker compose --env-file $DeployEnv -f $ComposeFile logs --tail 80 backend
    throw 'Backend did not become healthy within six minutes.'
}

if (-not $SkipFrontends) {
    Start-Frontend -Directory (Join-Path $RepoRoot 'frontend-admin') -Port $script:AdminDevPort -Title 'Shopping Admin'
    Start-Frontend -Directory (Join-Path $RepoRoot 'frontend-web') -Port $script:WebDevPort -Title 'Shopping Web'
}

$backendPort = Get-EnvValue -Name 'BACKEND_PORT'
if ([string]::IsNullOrWhiteSpace($backendPort)) { $backendPort = '8080' }

Write-Host 'Local environment is ready:' -ForegroundColor Green
Write-Host "  Admin:   http://localhost:$script:AdminDevPort"
Write-Host "  Web:     http://127.0.0.1:$script:WebDevPort"
Write-Host "  Backend: http://localhost:$backendPort"
Write-Host "  Data:    $DataRoot/shopping"
