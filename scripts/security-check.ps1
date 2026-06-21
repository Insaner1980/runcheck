#Requires -Version 5.1

$scriptPath = Join-Path (Split-Path -Parent $PSScriptRoot) "tools\sc.ps1"
& $scriptPath @args
exit $LASTEXITCODE
