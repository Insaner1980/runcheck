#Requires -Version 5.1

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ("runcheck-icon-test-" + [Guid]::NewGuid().ToString("N"))
$pwsh = (Get-Process -Id $PID).Path

try {
    New-Item -ItemType Directory -Path "$tempRoot/tools", "$tempRoot/app/src/main/res" -Force | Out-Null
    Copy-Item -LiteralPath "$PSScriptRoot/export-launcher-icons.ps1" -Destination "$tempRoot/tools"
    Copy-Item -LiteralPath "$repoRoot/app/src/main/AndroidManifest.xml" -Destination "$tempRoot/app/src/main"
    $resourceRoot = "$repoRoot/app/src/main/res"
    Get-ChildItem -LiteralPath $resourceRoot -Recurse -Filter "ic_launcher*.webp" | ForEach-Object {
        $relativePath = $_.FullName.Substring($resourceRoot.Length + 1)
        $destination = Join-Path "$tempRoot/app/src/main/res" $relativePath
        New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
        Copy-Item -LiteralPath $_.FullName -Destination $destination
    }
    $script = "$tempRoot/tools/export-launcher-icons.ps1"
    $output = & $pwsh -NoProfile -File $script -VerifyOnly 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Verification without SVG sources failed: $output" }

    $ErrorActionPreference = "Continue"
    $output = & $pwsh -NoProfile -File $script 2>&1
    $ErrorActionPreference = "Stop"
    if ($LASTEXITCODE -eq 0 -or ($output -join "`n") -notmatch 'Puuttuva tiedosto') {
        throw "Export did not reject missing SVG sources: $output"
    }

    Copy-Item -LiteralPath "$tempRoot/app/src/main/res/mipmap-hdpi/ic_launcher.webp" `
        -Destination "$tempRoot/app/src/main/res/mipmap-mdpi/ic_launcher.webp" -Force
    $ErrorActionPreference = "Continue"
    $output = & $pwsh -NoProfile -File $script -VerifyOnly 2>&1
    $ErrorActionPreference = "Stop"
    if ($LASTEXITCODE -eq 0 -or ($output -join "`n") -notmatch 'Vaarat ikonimitat') {
        throw "Verification accepted an incorrect image size: $output"
    }
    Write-Output "export-launcher-icons-test: PASS (existing assets, missing export sources, invalid dimensions)"
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
