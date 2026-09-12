#Requires -Version 5.1

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ("runcheck-release-gate-" + [Guid]::NewGuid().ToString("N"))

try {
    New-Item -ItemType Directory -Path $tempRoot | Out-Null
    Set-Content "$tempRoot/settings.gradle.kts" 'rootProject.name = "release-gate-fixture"'
    $source = Get-Content "$repoRoot/app/build.gradle.kts" -Raw
    $start = $source.IndexOf('fun isReleaseArtifactTaskName')
    $end = $source.IndexOf('fun validateReleaseArtifactRequest')
    $functions = $source.Substring($start, $end - $start)
    $start = $source.IndexOf('if (isReleaseArtifactTaskRequested())')
    $end = $source.IndexOf('hilt {', $start)
    $wiring = $source.Substring($start, $end - $start)
    $fixture = @'
fun validateReleaseArtifactRequest() {
    check(providers.gradleProperty("fixtureAllowed").orNull == "true") { "FIXTURE_GATE_REJECTED" }
}
tasks.register("packageReleaseBundle") {
    val artifact = layout.buildDirectory.file("fixture.txt")
    outputs.file(artifact)
    doLast { artifact.get().asFile.apply { parentFile.mkdirs(); writeText("fixture") } }
}
tasks.register("assembleDebug")
'@
    Set-Content "$tempRoot/build.gradle.kts" ($functions + $fixture + $wiring)
    $gradle = Join-Path $repoRoot "gradlew.bat"
    $commonArgs = @("-p", $tempRoot, "--offline", "--no-daemon", "--console=plain")
    $output = & $gradle @commonArgs packageReleaseBundle -PfixtureAllowed=true 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Fixture setup failed: $output" }
    $artifact = Join-Path $tempRoot "build/fixture.txt"
    $writeTime = (Get-Item $artifact).LastWriteTimeUtc
    $output = & $gradle @commonArgs packageReleaseB 2>&1
    if ($LASTEXITCODE -eq 0 -or ($output -join "`n") -notmatch 'FIXTURE_GATE_REJECTED') {
        throw "Abbreviated up-to-date release task bypassed validation: $output"
    }
    if ((Get-Item $artifact).LastWriteTimeUtc -ne $writeTime) { throw "Rejected task changed the artifact." }
    $output = & $gradle @commonArgs assembleDebug 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Debug task incorrectly required release inputs: $output" }
    Write-Output "release-gate-test: PASS (release setup, rejected abbreviation, preserved artifact, debug isolation)"
}
finally {
    $resolvedTemp = [IO.Path]::GetFullPath($tempRoot)
    $expectedParent = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\') + '\'
    if (-not $resolvedTemp.StartsWith($expectedParent, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Unexpected fixture cleanup path."
    }
    if (Test-Path -LiteralPath $resolvedTemp) {
        Remove-Item -LiteralPath $resolvedTemp -Recurse -Force
    }
}
