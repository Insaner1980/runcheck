$ProjectCheckCommand = "osv-scan"
$ProjectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
. "$PSScriptRoot\Invoke-RuncheckProjectCheck.ps1"
$ProjectCheckScript = Resolve-RuncheckProjectCheck
& $ProjectCheckScript -ProjectCheckCommand $ProjectCheckCommand -Root $ProjectRoot @args
exit $LASTEXITCODE
