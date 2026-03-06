param(
    [switch]$Install
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-LatestFile {
    param(
        [string[]]$Roots,
        [string]$Filter,
        [string]$Label
    )

    $matches = foreach ($root in $Roots) {
        if (Test-Path $root) {
            Get-ChildItem -Path $root -Recurse -Filter $Filter -ErrorAction SilentlyContinue
        }
    }

    $selected = $matches | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $selected) {
        throw "Could not locate $Label ($Filter)."
    }

    return $selected.FullName
}

$projectRoot = Split-Path -Parent $PSCommandPath
$sourceRoot = Join-Path $projectRoot "src\\main\\java"
$resourceRoot = Join-Path $projectRoot "src\\main\\resources"
$buildRoot = Join-Path $projectRoot "build"
$classesRoot = Join-Path $buildRoot "classes"
$jarPath = Join-Path $buildRoot "StsControllerVibrationSupport.jar"

$gameRoots = @(
    "E:\\SteamLibrary\\steamapps\\common\\SlayTheSpire",
    "D:\\SteamLibrary\\steamapps\\common\\SlayTheSpire",
    "C:\\Program Files (x86)\\Steam\\steamapps\\common\\SlayTheSpire",
    "C:\\Program Files\\Steam\\steamapps\\common\\SlayTheSpire"
)

$workshopRoots = @(
    "E:\\SteamLibrary\\steamapps\\workshop\\content\\646570",
    "D:\\SteamLibrary\\steamapps\\workshop\\content\\646570",
    "C:\\Program Files (x86)\\Steam\\steamapps\\workshop\\content\\646570",
    "C:\\Program Files\\Steam\\steamapps\\workshop\\content\\646570"
)

$gameJar = Resolve-LatestFile -Roots $gameRoots -Filter "desktop-1.0.jar" -Label "Slay the Spire desktop jar"
$mtsJar = Resolve-LatestFile -Roots $workshopRoots -Filter "ModTheSpire.jar" -Label "ModTheSpire jar"
$baseModJar = Resolve-LatestFile -Roots $workshopRoots -Filter "BaseMod.jar" -Label "BaseMod jar"
$modsDir = Join-Path (Split-Path -Parent $gameJar) "mods"

$javaFiles = Get-ChildItem -Path $sourceRoot -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
if (-not $javaFiles) {
    throw "No Java source files found under $sourceRoot."
}

if (Test-Path $classesRoot) {
    Remove-Item -Path $classesRoot -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $classesRoot | Out-Null

$classpath = ($gameJar, $mtsJar, $baseModJar) -join ";"
& javac -g -proc:none -Xlint:-options --release 8 -classpath $classpath -d $classesRoot $javaFiles
if ($LASTEXITCODE -ne 0) {
    throw "javac failed."
}

if (Test-Path $resourceRoot) {
    Copy-Item -Path (Join-Path $resourceRoot "*") -Destination $classesRoot -Recurse -Force
}

if (Test-Path $jarPath) {
    Remove-Item -Path $jarPath -Force
}

& jar --create --file $jarPath -C $classesRoot .
if ($LASTEXITCODE -ne 0) {
    throw "jar packaging failed."
}

if ($Install) {
    New-Item -ItemType Directory -Force -Path $modsDir | Out-Null
    Copy-Item -Path $jarPath -Destination (Join-Path $modsDir "StsControllerVibrationSupport.jar") -Force
    Write-Host "Installed mod jar to $modsDir"
}

Write-Host "Built $jarPath"
Write-Host "Game jar: $gameJar"
Write-Host "ModTheSpire jar: $mtsJar"
Write-Host "BaseMod jar: $baseModJar"
