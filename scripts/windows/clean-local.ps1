<#
.SYNOPSIS
    Safely clean the Windows development environment.

.DESCRIPTION
    The default action stops project services and removes reproducible build
    output. Persistent data, dependencies, and local secrets are preserved.
    Destructive cleanup requires explicit switches and confirmation.

    This executable script intentionally uses ASCII text for Windows
    PowerShell 5 compatibility. Chinese usage notes are in README.md.
#>
[CmdletBinding()]
param(
    [switch]$RemoveDependencies,
    [switch]$RemoveLocalConfig,
    [switch]$PurgePersistentData,
    [switch]$Force
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$DeployDir = Join-Path $RepoRoot 'deploy'
$DeployEnv = Join-Path $DeployDir '.env'
$ComposeFile = Join-Path $DeployDir 'docker-compose.yml'

function Remove-PathIfPresent {
    param([Parameter(Mandatory)] [string]$Path)
    if (Test-Path -LiteralPath $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
        Write-Host "Removed: $Path"
    }
}

function Stop-NodeListener {
    param([Parameter(Mandatory)] [int]$Port)

    Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique |
        ForEach-Object {
            $process = Get-Process -Id $_ -ErrorAction SilentlyContinue
            # Only stop Node on project-owned ports; never terminate unrelated processes.
            if ($process -and $process.ProcessName -match '^node') {
                Stop-Process -Id $process.Id -Force
                Write-Host "Stopped Node development server on port $Port."
            }
        }
}

function Get-DataRoot {
    if (-not (Test-Path -LiteralPath $DeployEnv)) { return 'D:/Project/data' }
    $line = Get-Content -LiteralPath $DeployEnv |
        Where-Object { $_ -match '^\s*DATA_DIR=' } |
        Select-Object -Last 1
    if (-not $line) { return 'D:/Project/data' }
    $value = ($line -split '=', 2)[1].Trim()
    if ([string]::IsNullOrWhiteSpace($value)) { return 'D:/Project/data' }
    return $value
}

$DataRootBeforeCleanup = Get-DataRoot

Stop-NodeListener -Port 5173
Stop-NodeListener -Port 5174

if (Test-Path -LiteralPath $DeployEnv) {
    & docker compose --env-file $DeployEnv -f $ComposeFile down
} else {
    Write-Host 'deploy/.env is missing; Docker Compose stop skipped.' -ForegroundColor DarkYellow
}

# Default cleanup removes only content that can be regenerated from source.
@(
    'backend\target',
    'frontend-web\dist',
    'frontend-admin\dist'
) | ForEach-Object { Remove-PathIfPresent -Path (Join-Path $RepoRoot $_) }

if ($RemoveDependencies) {
    @(
        'frontend-web\node_modules',
        'frontend-admin\node_modules'
    ) | ForEach-Object { Remove-PathIfPresent -Path (Join-Path $RepoRoot $_) }
}

if ($RemoveLocalConfig) {
    @(
        'deploy\.env',
        'backend\src\main\resources\application-local.yml',
        'frontend-web\.env',
        'frontend-admin\.env'
    ) | ForEach-Object { Remove-PathIfPresent -Path (Join-Path $RepoRoot $_) }
}

if ($PurgePersistentData) {
    $confirmed = $Force
    if (-not $confirmed) {
        $answer = Read-Host 'This permanently deletes project data. Type PURGE to confirm'
        $confirmed = $answer -ceq 'PURGE'
    }

    if (-not $confirmed) {
        Write-Host 'Persistent-data cleanup cancelled.' -ForegroundColor Yellow
    } else {
        # Scope deletion to the shopping directory; preserve every sibling project.
        Remove-PathIfPresent -Path (Join-Path $DataRootBeforeCleanup 'shopping')
    }
}

Write-Host 'Local cleanup completed. Default-preserved content is unchanged.' -ForegroundColor Green
