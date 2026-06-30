$defaultMinimumTokens = "100"
if ([string]::IsNullOrWhiteSpace($env:PMD_CPD_MINIMUM_TOKENS)) {
    $env:PMD_CPD_MINIMUM_TOKENS = $defaultMinimumTokens
}

$ProjectCheckCommand = "pmd-check"
. "$PSScriptRoot\Invoke-RuncheckProjectCheck.ps1"
$ProjectCheckScript = Resolve-RuncheckProjectCheck
& $ProjectCheckScript -ProjectCheckCommand $ProjectCheckCommand @args
exit $LASTEXITCODE
