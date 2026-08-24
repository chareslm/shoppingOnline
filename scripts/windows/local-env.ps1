<#
.SYNOPSIS
    Shared Windows local-environment helpers.

.DESCRIPTION
    Keep host-specific paths and secrets out of committed example files.
    This file is ASCII-only for Windows PowerShell 5.
#>

Set-StrictMode -Version Latest

$script:AdminDevPort = 5173
$script:WebDevPort = 5174

function Get-RepoRoot {
    return (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
}

function Get-DeployDir {
    return Join-Path (Get-RepoRoot) 'deploy'
}

function Get-DeployEnvPath {
    return Join-Path (Get-DeployDir) '.env'
}

function Get-ComposeFilePath {
    $local = Join-Path (Get-DeployDir) 'docker-compose.yml'
    if (Test-Path -LiteralPath $local) { return $local }
    return Join-Path (Get-DeployDir) 'docker-compose.yml.example'
}

function Get-DefaultDataDir {
    return ((Join-Path $env:USERPROFILE 'shopping-data') -replace '\\', '/')
}

function Get-DefaultMavenRepoDir {
    return ((Join-Path $env:USERPROFILE '.m2') -replace '\\', '/')
}

function Get-EnvValue {
    param(
        [Parameter(Mandatory)] [string]$Name,
        [string]$File = (Get-DeployEnvPath)
    )
    if (-not (Test-Path -LiteralPath $File)) { return $null }
    $line = Get-Content -LiteralPath $File |
        Where-Object { $_ -match "^\s*$([regex]::Escape($Name))=" } |
        Select-Object -Last 1
    if (-not $line) { return $null }
    return ($line -split '=', 2)[1].Trim()
}

function Set-EnvValue {
    param(
        [Parameter(Mandatory)] [string]$Name,
        [Parameter(Mandatory)] [AllowEmptyString()] [string]$Value,
        [string]$File = (Get-DeployEnvPath)
    )
    $pattern = "^\s*$([regex]::Escape($Name))="
    $lines = @(Get-Content -LiteralPath $File)
    $replaced = $false
    $output = foreach ($line in $lines) {
        if ($line -match $pattern) {
            $replaced = $true
            "${Name}=${Value}"
        } else {
            $line
        }
    }
    if (-not $replaced) {
        $output += "${Name}=${Value}"
    }
    Set-Content -LiteralPath $File -Value $output
}

function Test-EnvPlaceholder {
    param([AllowNull()] [string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return $true }
    $trimmed = $Value.Trim()
    return $trimmed -eq 'folder' -or
        $trimmed -like 'replace-*' -or
        $trimmed -eq 'smtp.example.com' -or
        $trimmed -eq 'no-reply@example.com'
}

function New-LocalSecret {
    $bytes = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return [Convert]::ToBase64String($bytes)
}

function New-BootstrapPassword {
    $token = ([guid]::NewGuid().ToString('N').Substring(0, 16))
    return "Adm#${token}aA1!"
}

function Initialize-DeployEnv {
    param(
        [string]$File = (Get-DeployEnvPath),
        [switch]$Fresh
    )

    $dataDir = Get-EnvValue -Name 'DATA_DIR' -File $File
    if (Test-EnvPlaceholder $dataDir) {
        Set-EnvValue -Name 'DATA_DIR' -Value (Get-DefaultDataDir) -File $File
        Write-Host "Set DATA_DIR to $(Get-DefaultDataDir)" -ForegroundColor Yellow
    }

    $mavenRepo = Get-EnvValue -Name 'MAVEN_REPO_DIR' -File $File
    if (Test-EnvPlaceholder $mavenRepo) {
        Set-EnvValue -Name 'MAVEN_REPO_DIR' -Value (Get-DefaultMavenRepoDir) -File $File
        Write-Host "Set MAVEN_REPO_DIR to $(Get-DefaultMavenRepoDir)" -ForegroundColor Yellow
    }

    $uploadDir = Get-EnvValue -Name 'MERCHANT_UPLOAD_DIR' -File $File
    if (Test-EnvPlaceholder $uploadDir) {
        $resolvedData = Get-EnvValue -Name 'DATA_DIR' -File $File
        Set-EnvValue -Name 'MERCHANT_UPLOAD_DIR' -Value "$resolvedData/shopping/uploads" -File $File
    }

    foreach ($secretName in @('MYSQL_APP_PASSWORD', 'MYSQL_ROOT_PASSWORD', 'JWT_SECRET')) {
        $current = Get-EnvValue -Name $secretName -File $File
        if (Test-EnvPlaceholder $current) {
            Set-EnvValue -Name $secretName -Value (New-LocalSecret) -File $File
            Write-Host "Generated $secretName in deploy/.env (gitignored)." -ForegroundColor Yellow
        }
    }

    $mailHost = Get-EnvValue -Name 'MAIL_HOST' -File $File
    if ($mailHost -eq 'smtp.example.com') {
        Set-EnvValue -Name 'MAIL_HOST' -Value '' -File $File
        Set-EnvValue -Name 'MAIL_USERNAME' -Value '' -File $File
        Set-EnvValue -Name 'MAIL_PASSWORD' -Value '' -File $File
        Set-EnvValue -Name 'MAIL_FROM' -Value '' -File $File
        Write-Host 'SMTP left disabled (empty MAIL_HOST). Enable later in deploy/.env or the admin SMTP page.' -ForegroundColor Yellow
    }

    if ($Fresh) {
        $password = New-BootstrapPassword
        Set-EnvValue -Name 'BOOTSTRAP_SUPER_ADMIN_ENABLED' -Value 'true' -File $File
        Set-EnvValue -Name 'BOOTSTRAP_SUPER_ADMIN_USERNAME' -Value 'admin_local' -File $File
        Set-EnvValue -Name 'BOOTSTRAP_SUPER_ADMIN_PASSWORD' -Value $password -File $File
        Write-Host "First-run SUPER_ADMIN bootstrap: admin_local / $password" -ForegroundColor Green
        Write-Host 'Set BOOTSTRAP_SUPER_ADMIN_ENABLED=false in deploy/.env after the first successful login.' -ForegroundColor Yellow
    }
}

function Get-BackendHealthUrl {
    $port = Get-EnvValue -Name 'BACKEND_PORT'
    if ([string]::IsNullOrWhiteSpace($port)) { $port = '8080' }
    return "http://127.0.0.1:${port}/actuator/health"
}

function Ensure-LocalCopy {
    param(
        [Parameter(Mandatory)] [string]$Example,
        [Parameter(Mandatory)] [string]$Local
    )
    if (-not (Test-Path -LiteralPath $Local)) {
        Copy-Item -LiteralPath $Example -Destination $Local
        Write-Host "Created local configuration: $Local" -ForegroundColor Yellow
        return $true
    }
    return $false
}
