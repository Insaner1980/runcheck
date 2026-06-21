$defaultMinimumTokens = "100"
if ([string]::IsNullOrWhiteSpace($env:PMD_CPD_MINIMUM_TOKENS)) {
    $env:PMD_CPD_MINIMUM_TOKENS = $defaultMinimumTokens
}

$ProjectCheckCommand = "pmd-check"
& "C:\Dev\Android-check\tools\InvokeProjectCheck.ps1" -ProjectCheckCommand $ProjectCheckCommand @args
