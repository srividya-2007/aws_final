# ============================================================
# EcoGrid - Run All Unit Tests (Review 2 Demo Script)
# Usage: Right-click → Run with PowerShell  OR  PS> .\run-all-tests.ps1
# ============================================================

$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$mvn   = "C:\apache-maven-3.9.6\bin\mvn.cmd"
$base  = "c:\Users\lanka_lc39l1s\OneDrive - K L University\Documents\SOA_Final"

$services = @(
    "ecogrid-auth-service",
    "ecogrid-prosumer-service",
    "ecogrid-telemetry-service",
    "ecogrid-matching-service",
    "ecogrid-settlement-service",
    "ecogrid-api-gateway"
)

$totalTests   = 0
$totalFailed  = 0
$totalErrors  = 0
$results      = @()

foreach ($svc in $services) {
    Write-Host ""
    Write-Host "=" * 60 -ForegroundColor Cyan
    Write-Host "  Testing: $svc" -ForegroundColor Yellow
    Write-Host "=" * 60 -ForegroundColor Cyan

    $output = & $mvn clean test --no-transfer-progress -f "$base\$svc\pom.xml" 2>&1

    # Parse results
    $summaryLine = $output | Select-String "Tests run:" | Select-Object -Last 1
    $buildStatus = if ($output -match "BUILD SUCCESS") { "PASS ✅" } else { "FAIL ❌" }

    if ($summaryLine) {
        $line = $summaryLine.ToString()
        # Extract numbers
        if ($line -match "Tests run: (\d+), Failures: (\d+), Errors: (\d+)") {
            $ran    = [int]$Matches[1]
            $failed = [int]$Matches[2]
            $errors = [int]$Matches[3]
            $totalTests  += $ran
            $totalFailed += $failed
            $totalErrors += $errors
            $results += [PSCustomObject]@{
                Service  = $svc
                Tests    = $ran
                Failed   = $failed
                Errors   = $errors
                Status   = $buildStatus
            }
            Write-Host "  Results: $line" -ForegroundColor $(if ($failed -eq 0 -and $errors -eq 0) { "Green" } else { "Red" })
        }
    }

    Write-Host "  Build: $buildStatus"
}

# ─── SUMMARY TABLE ──────────────────────────────────────────
Write-Host ""
Write-Host "=" * 60 -ForegroundColor Cyan
Write-Host "  ECOGRID UNIT TEST SUMMARY" -ForegroundColor Yellow
Write-Host "=" * 60 -ForegroundColor Cyan

$results | Format-Table -AutoSize

Write-Host ""
Write-Host "  TOTAL TESTS  : $totalTests"   -ForegroundColor White
Write-Host "  TOTAL FAILED : $totalFailed"  -ForegroundColor $(if ($totalFailed -eq 0) { "Green" } else { "Red" })
Write-Host "  TOTAL ERRORS : $totalErrors"  -ForegroundColor $(if ($totalErrors -eq 0) { "Green" } else { "Red" })
Write-Host ""

if ($totalFailed -eq 0 -and $totalErrors -eq 0) {
    Write-Host "  ALL TESTS PASSED! Ready for Review 2. " -ForegroundColor Green
} else {
    Write-Host "  SOME TESTS FAILED. Check output above." -ForegroundColor Red
}

Write-Host ""
Write-Host "  Surefire reports: <service>/target/surefire-reports/"
Write-Host ""
