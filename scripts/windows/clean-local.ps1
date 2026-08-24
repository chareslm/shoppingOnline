<#
.SYNOPSIS
    Safely clean the Windows development environment.

.DESCRIPTION
    Default: stop project services and delete reproducible build output.
    Persistent data, node_modules, and gitignored secrets are kept unless
    you pass explicit switches.

    Chinese usage notes: scripts/windows/README.md
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

. (Join-Path $PSScriptRoot 'local-env.ps1')

$RepoRoot = Get-RepoRoot
$DeployEnv = Get-DeployEnvPath
$ComposeFile = Get-ComposeFilePath

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
            if ($process -and $process.ProcessName -match '^node') {
                Stop-Process -Id $process.Id -Force
                Write-Host "Stopped Node development server on port $Port."
            }
        }
}

$dataRoot = Get-EnvValue -Name 'DATA_DIR'
if (Test-EnvPlaceholder $dataRoot) {
    $dataRoot = Get-DefaultDataDir
}

Stop-NodeListener -Port $script:AdminDevPort
Stop-NodeListener -Port $script:WebDevPort

if (Test-Path -LiteralPath $DeployEnv) {
    & docker compose --env-file $DeployEnv -f $ComposeFile down
} else {
    Write-Host 'deploy/.env is missing; Docker Compose stop skipped.' -ForegroundColor DarkYellow
}

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
        $answer = Read-Host "This permanently deletes $dataRoot/shopping. Type PURGE to confirm"
        $confirmed = $answer -ceq 'PURGE'
    }

    if (-not $confirmed) {
        Write-Host 'Persistent-data cleanup cancelled.' -ForegroundColor Yellow
    } else {
        Remove-PathIfPresent -Path (Join-Path $dataRoot 'shopping')
    }
}

Write-Host 'Local cleanup completed. Default-preserved content is unchanged.' -ForegroundColor Green
