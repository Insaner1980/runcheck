#Requires -Version 5.1

$ErrorActionPreference = "Stop"
$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ("sonar-upload-retry-test-" + [Guid]::NewGuid().ToString("N"))
$originalToken = $env:SONAR_TOKEN
$originalPath = $env:PATH
$pwsh = (Get-Process -Id $PID).Path

try {
    New-Item -ItemType Directory -Path (Join-Path $tempRoot ".git") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $tempRoot "tools") -Force | Out-Null
    foreach ($name in @("sonar.ps1", "Invoke-RuncheckProjectCheck.ps1")) {
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot $name) -Destination (Join-Path $tempRoot "tools\$name")
    }
    Set-Content -LiteralPath (Join-Path $tempRoot "sonar-project.properties") -Value "sonar.projectKey=fixture"
    $env:SONAR_TOKEN = "fixture-token"
    $env:PATH = Join-Path $env:SystemRoot "System32"

    $uploadFailure = @(
        'echo Fail to request https://sonarcloud.io/api/ce/submit?projectKey=fixture 1>&2'
        'echo Caused by: java.net.SocketTimeoutException: timeout 1>&2'
        'echo at okhttp3.internal.http2.Http2Stream$FramingSink.emitFrame 1>&2'
    )
    $cases = @(
        @{ Name = "upload-recovers"; Failures = 1; ExpectedAttempts = 2; ExpectedExit = 0; ErrorLines = $uploadFailure }
        @{ Name = "upload-exhausted"; Failures = 9; ExpectedAttempts = 3; ExpectedExit = 2; ErrorLines = $uploadFailure }
        @{ Name = "build-failure"; Failures = 9; ExpectedAttempts = 1; ExpectedExit = 2; ErrorLines = @('echo Compilation failed 1>&2') }
        @{ Name = "authorization-failure"; Failures = 9; ExpectedAttempts = 1; ExpectedExit = 2; ErrorLines = @($uploadFailure[0], 'echo HTTP 401 Unauthorized 1>&2') }
        @{ Name = "read-timeout"; Failures = 9; ExpectedAttempts = 1; ExpectedExit = 2; ErrorLines = @($uploadFailure[0], $uploadFailure[1], 'echo at okhttp3.internal.http2.Http2Stream.takeHeaders 1>&2') }
    )
    foreach ($case in $cases) {
        $counterPath = Join-Path $tempRoot "attempts.txt"
        Set-Content -LiteralPath $counterPath -Value "0"
        Set-Content -LiteralPath (Join-Path $tempRoot "gradlew.bat") -Encoding ascii -Value (@(
            '@echo off'
            'set /p attempts=<attempts.txt'
            'set /a attempts+=1 >nul'
            '>attempts.txt echo %attempts%'
            'echo fixture-attempt-%attempts%'
            "if %attempts% GTR $($case.Failures) exit /b 0"
        ) + $case.ErrorLines + @('exit /b 1'))
        $output = & $pwsh -NoProfile -File (Join-Path $tempRoot "tools\sonar.ps1") -AllowExternalUpload *>&1
        $exitCode = $LASTEXITCODE
        $attempts = [int](Get-Content -LiteralPath $counterPath -Raw)
        if ($exitCode -ne $case.ExpectedExit -or $attempts -ne $case.ExpectedAttempts) {
            throw "$($case.Name): expected exit $($case.ExpectedExit), attempts $($case.ExpectedAttempts); got $exitCode, $attempts. $output"
        }
        $report = Get-Content -LiteralPath (Join-Path $tempRoot "reports\sonar.txt") -Raw
        for ($attempt = 1; $attempt -le $attempts; $attempt++) {
            if (-not $report.Contains("fixture-attempt-$attempt")) {
                throw "$($case.Name): missing log for attempt $attempt."
            }
        }
        $retryCount = [regex]::Matches($report, 'RETRY: SONAR_UPLOAD_WRITE_TIMEOUT').Count
        if ($retryCount -ne $attempts - 1) {
            throw "$($case.Name): unexpected retry log count $retryCount."
        }
        Write-Output "$($case.Name): PASS"
    }

    Set-Content -LiteralPath $counterPath -Value "0"
    $budgetScript = @(
        '@echo off'
        'set /p attempts=<attempts.txt'
        'set /a attempts+=1 >nul'
        '>attempts.txt echo %attempts%'
        '"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -Command "Start-Sleep -Milliseconds 1500"'
    ) + $uploadFailure + @('exit /b 1')
    Set-Content -LiteralPath (Join-Path $tempRoot "gradlew.bat") -Encoding ascii -Value $budgetScript
    $output = & $pwsh -NoProfile -File (Join-Path $tempRoot "tools\sonar.ps1") -AllowExternalUpload -GradleTimeoutSeconds 3 *>&1
    $exitCode = $LASTEXITCODE
    $attempts = [int](Get-Content -LiteralPath $counterPath -Raw)
    $report = Get-Content -LiteralPath (Join-Path $tempRoot "reports\sonar.txt") -Raw
    if ($exitCode -ne 2 -or $attempts -ge 3 -or -not $report.Contains("ERROR: SONAR_ANALYSIS_TIMEOUT (3 s)")) {
        throw "shared-time-budget: expected timeout before three attempts; got exit $exitCode, attempts $attempts. $output"
    }
    Write-Output "shared-time-budget: PASS"
}
finally {
    $env:SONAR_TOKEN = $originalToken
    $env:PATH = $originalPath
    $resolvedTemp = [IO.Path]::GetFullPath($tempRoot)
    $expectedParent = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\') + '\'
    if ($resolvedTemp.StartsWith($expectedParent, [StringComparison]::OrdinalIgnoreCase) -and
        (Split-Path -Leaf $resolvedTemp).StartsWith("sonar-upload-retry-test-")) {
        Remove-Item -LiteralPath $resolvedTemp -Recurse -Force
    }
}
