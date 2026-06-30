$ProjectCheckCommand = "security-check"
. "$PSScriptRoot\Invoke-RuncheckProjectCheck.ps1"
$ProjectCheckScript = Resolve-RuncheckProjectCheck
& $ProjectCheckScript -ProjectCheckCommand $ProjectCheckCommand @args
exit $LASTEXITCODE
