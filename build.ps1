param(
    [switch]$Install
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSCommandPath
$wrapperPath = Join-Path $projectRoot "gradlew.bat"
$gradleExecutable = Get-Command gradle -ErrorAction SilentlyContinue
$gradleCommand = if ($gradleExecutable) {
    $gradleExecutable.Source
} elseif (Test-Path $wrapperPath) {
    $wrapperPath
} else {
    throw "Neither Gradle nor gradlew.bat is available."
}

$gradleTasks = @("clean", "build")
if ($Install) {
    $gradleTasks += "installMod"
}

& $gradleCommand @gradleTasks
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed."
}
