$ProjectCheckCommand = "secret-scan"
. "$PSScriptRoot\Invoke-RuncheckProjectCheck.ps1"
$ProjectCheckScript = Resolve-RuncheckProjectCheck
& $ProjectCheckScript -ProjectCheckCommand $ProjectCheckCommand @args
exit $LASTEXITCODE
