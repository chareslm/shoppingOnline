param(
    [int]$TimeoutSeconds = 180,
    [string]$HealthUrl = ''
)

Set-StrictMode -Version Latest

if ([string]::IsNullOrWhiteSpace($HealthUrl)) {
    . (Join-Path $PSScriptRoot 'local-env.ps1')
    $HealthUrl = Get-BackendHealthUrl
}

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
do {
    try {
        $status = (Invoke-RestMethod -Uri $HealthUrl -TimeoutSec 3).status
        if ($status -eq 'UP') {
            Write-Host "Backend is UP: $HealthUrl"
            exit 0
        }
    } catch {
        # Maven compile plus Spring Boot startup commonly takes 60-120 seconds.
    }
    Start-Sleep -Seconds 3
} while ((Get-Date) -lt $deadline)

Write-Host "Backend did not become healthy within $TimeoutSeconds seconds: $HealthUrl"
exit 1
