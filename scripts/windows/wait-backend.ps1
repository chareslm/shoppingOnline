param(
    [int]$TimeoutSeconds = 180,
    [string]$HealthUrl = 'http://127.0.0.1:8080/actuator/health'
)

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
