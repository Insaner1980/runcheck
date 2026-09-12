$ProjectCheckCommand = "android-check"
. "$PSScriptRoot\Invoke-RuncheckProjectCheck.ps1"
$ProjectCheckScript = Resolve-RuncheckProjectCheck
& $ProjectCheckScript -ProjectCheckCommand $ProjectCheckCommand -Root (Split-Path -Parent $PSScriptRoot) @args
exit $LASTEXITCODE
