#Requires -Version 5.1

function Resolve-RuncheckProjectCheck {
    $repoRoot = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")
    $candidateRoots = New-Object System.Collections.Generic.List[string]

    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_CHECK_ROOT)) {
        $candidateRoots.Add($env:ANDROID_CHECK_ROOT)
    }

    $siblingRoot = Join-Path (Split-Path -Parent $repoRoot) "Android-check"
    $candidateRoots.Add($siblingRoot)

    foreach ($root in $candidateRoots) {
        $script = Join-Path $root "tools\InvokeProjectCheck.ps1"
        if (Test-Path -LiteralPath $script -PathType Leaf) {
            return (Resolve-Path -LiteralPath $script).Path
        }
    }

    throw (
        "Android-check wrapperia ei löytynyt. Aseta ANDROID_CHECK_ROOT osoittamaan Android-check-repoon " +
            "tai pidä Android-check runcheckin sisarhakemistona."
    )
}
