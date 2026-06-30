#Requires -Version 5.1

[CmdletBinding()]
param(
    [switch]$VerifyOnly
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")
$foregroundSource = Join-Path $repoRoot "icons\runcheck-adaptive-foreground.svg"
$monochromeSource = Join-Path $repoRoot "icons\runcheck-adaptive-monochrome.svg"
$fallbackSource = Join-Path $repoRoot "icons\runcheck-icon-color-flat.svg"
$manifestPath = Join-Path $repoRoot "app\src\main\AndroidManifest.xml"

$foregroundDensities = [ordered]@{
    "mdpi" = 108
    "hdpi" = 162
    "xhdpi" = 216
    "xxhdpi" = 324
    "xxxhdpi" = 432
}

$fallbackDensities = [ordered]@{
    "mdpi" = 48
    "hdpi" = 72
    "xhdpi" = 96
    "xxhdpi" = 144
    "xxxhdpi" = 192
}

function Assert-FileExists {
    param([Parameter(Mandatory)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Puuttuva tiedosto: $Path"
    }
}

function Get-MagickCommand {
    $command = Get-Command magick -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        throw "ImageMagick magick.exe puuttuu PATHista."
    }
    return $command.Source
}

function Convert-IconLayer {
    param(
        [Parameter(Mandatory)][string]$Magick,
        [Parameter(Mandatory)][string]$Source,
        [Parameter(Mandatory)][string]$Destination,
        [Parameter(Mandatory)][int]$Size,
        [switch]$OpaqueFallback
    )

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Destination) | Out-Null
    $resize = "${Size}x${Size}"
    if ($OpaqueFallback) {
        & $Magick $Source -background "#0B1E24" -resize $resize -gravity center -extent $resize -alpha remove -alpha off "WEBP:$Destination"
    } else {
        & $Magick $Source -background none -resize $resize "WEBP:$Destination"
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Ikonin generointi epaonnistui: $Destination"
    }
}

function Get-ImageSize {
    param(
        [Parameter(Mandatory)][string]$Magick,
        [Parameter(Mandatory)][string]$Path
    )

    $size = & $Magick identify -format "%wx%h" $Path
    if ($LASTEXITCODE -ne 0) {
        throw "Ikonin mittojen luku epaonnistui: $Path"
    }
    return $size
}

function Assert-ImageSize {
    param(
        [Parameter(Mandatory)][string]$Magick,
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][int]$Size
    )

    Assert-FileExists -Path $Path
    $actual = Get-ImageSize -Magick $Magick -Path $Path
    $expected = "${Size}x${Size}"
    if ($actual -ne $expected) {
        throw "Vaarat ikonimitat: $Path. Odotettu $expected, saatu $actual."
    }
}

Assert-FileExists -Path $foregroundSource
Assert-FileExists -Path $monochromeSource
Assert-FileExists -Path $fallbackSource
Assert-FileExists -Path $manifestPath

$manifest = Get-Content -Raw -LiteralPath $manifestPath
if ($manifest -notmatch 'android:icon="@mipmap/ic_launcher"') {
    throw "Manifestista puuttuu android:icon=@mipmap/ic_launcher."
}
if ($manifest -notmatch 'android:roundIcon="@mipmap/ic_launcher_round"') {
    throw "Manifestista puuttuu android:roundIcon=@mipmap/ic_launcher_round."
}

$magick = Get-MagickCommand

foreach ($entry in $foregroundDensities.GetEnumerator()) {
    $density = $entry.Key
    $size = [int]$entry.Value
    $foregroundDestination = Join-Path $repoRoot "app\src\main\res\drawable-$density\ic_launcher_foreground.webp"
    $monochromeDestination = Join-Path $repoRoot "app\src\main\res\drawable-$density\ic_launcher_monochrome.webp"

    if (-not $VerifyOnly) {
        Convert-IconLayer -Magick $magick -Source $foregroundSource -Destination $foregroundDestination -Size $size
        Convert-IconLayer -Magick $magick -Source $monochromeSource -Destination $monochromeDestination -Size $size
    }

    Assert-ImageSize -Magick $magick -Path $foregroundDestination -Size $size
    Assert-ImageSize -Magick $magick -Path $monochromeDestination -Size $size
}

foreach ($entry in $fallbackDensities.GetEnumerator()) {
    $density = $entry.Key
    $size = [int]$entry.Value
    $launcherDestination = Join-Path $repoRoot "app\src\main\res\mipmap-$density\ic_launcher.webp"
    $roundDestination = Join-Path $repoRoot "app\src\main\res\mipmap-$density\ic_launcher_round.webp"

    if (-not $VerifyOnly) {
        Convert-IconLayer -Magick $magick -Source $fallbackSource -Destination $launcherDestination -Size $size -OpaqueFallback
        Convert-IconLayer -Magick $magick -Source $fallbackSource -Destination $roundDestination -Size $size -OpaqueFallback
    }

    Assert-ImageSize -Magick $magick -Path $launcherDestination -Size $size
    Assert-ImageSize -Magick $magick -Path $roundDestination -Size $size
}

Write-Output "Launcher icon -exportit OK"
