#Requires -Version 5.1

$ErrorActionPreference = "Stop"
$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ("runcheck-root-test-" + [Guid]::NewGuid().ToString("N"))
$originalCheckerRoot = $env:ANDROID_CHECK_ROOT
$pwsh = (Get-Process -Id $PID).Path

try {
    $fixtureRoot = Join-Path $tempRoot "project"
    $checkerRoot = Join-Path $tempRoot "checker"
    $otherRoot = Join-Path $tempRoot "other"
    foreach ($directory in @("$fixtureRoot/tools", "$fixtureRoot/.git", "$checkerRoot/tools", $otherRoot)) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }
    Copy-Item -Path "$PSScriptRoot/*.ps1" -Destination "$fixtureRoot/tools"
    Set-Content "$fixtureRoot/sonar-project.properties" "sonar.projectKey=runcheck-root-fixture"
    Set-Content "$checkerRoot/tools/InvokeProjectCheck.ps1" @'
param([string]$ProjectCheckCommand, [string]$Root = (Get-Location).Path, [switch]$PlanOnly)
Write-Output ([IO.Path]::GetFullPath($Root))
exit 0
'@
    $env:ANDROID_CHECK_ROOT = $checkerRoot
    Push-Location $otherRoot
    try {
        $wrappers = Get-ChildItem "$fixtureRoot/tools/*.ps1" | Where-Object {
            (Get-Content -LiteralPath $_.FullName -Raw) -match '(?m)^\$ProjectCheckCommand = '
        }
        foreach ($wrapper in $wrappers) {
            $output = & $pwsh -NoProfile -File $wrapper.FullName -PlanOnly
            if ($LASTEXITCODE -ne 0 -or ($output -join "").TrimEnd('\', '/') -ne $fixtureRoot) {
                throw "$($wrapper.Name) did not target its owning project."
            }
        }
        $output = & $pwsh -NoProfile -File "$fixtureRoot/tools/sonar.ps1" -PlanOnly
        if ($LASTEXITCODE -ne 0 -or ($output -join "`n") -notmatch 'project: runcheck-root-fixture') {
            throw "sonar.ps1 did not target its owning project."
        }
        Write-Output "project-root-test: PASS ($($wrappers.Count) wrappers and Sonar)"
    }
    finally {
        Pop-Location
    }
}
finally {
    $env:ANDROID_CHECK_ROOT = $originalCheckerRoot
    $resolvedTemp = [IO.Path]::GetFullPath($tempRoot)
    $expectedParent = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\') + '\'
    if (-not $resolvedTemp.StartsWith($expectedParent, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Unexpected fixture cleanup path."
    }
    if (Test-Path -LiteralPath $resolvedTemp) {
        Remove-Item -LiteralPath $resolvedTemp -Recurse -Force
    }
}
