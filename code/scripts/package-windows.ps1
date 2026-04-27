param(
    [string]$AppName = "EnglishChineseDictionary",
    [string]$AppVersion = "1.0.0",
    [string]$Vendor = "Course Project",
    [ValidateSet("app-image", "exe", "msi")]
    [string]$Type = "app-image"
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$TargetDir = Join-Path $ProjectRoot "target"
$JarName = "english-chinese-dictionary-1.0.0.jar"
$MainJar = Join-Path $TargetDir $JarName
$LibDir = Join-Path $TargetDir "lib"
$InputDir = Join-Path $TargetDir "jpackage-input"
$DistDir = Join-Path $TargetDir "dist"
$OptionalIcon = Join-Path $ProjectRoot "assets\app-icon.ico"

Push-Location $ProjectRoot
try {
    & mvn "-Dmaven.repo.local=.m2" clean package

    if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
        throw "jpackage was not found. Please make sure you are using JDK 21."
    }

    if (-not (Test-Path $MainJar)) {
        throw "Main jar was not found: $MainJar"
    }

    if (-not (Test-Path $LibDir)) {
        throw "Runtime library folder was not found: $LibDir"
    }

    if (Test-Path $InputDir) {
        Remove-Item -LiteralPath $InputDir -Recurse -Force
    }
    if (Test-Path $DistDir) {
        Remove-Item -LiteralPath $DistDir -Recurse -Force
    }

    New-Item -ItemType Directory -Path $InputDir | Out-Null
    New-Item -ItemType Directory -Path $DistDir | Out-Null

    Copy-Item -LiteralPath $MainJar -Destination $InputDir
    Get-ChildItem -LiteralPath $LibDir -Filter *.jar | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination $InputDir
    }

    $arguments = @(
        "--type", $Type,
        "--input", $InputDir,
        "--dest", $DistDir,
        "--name", $AppName,
        "--main-jar", $JarName,
        "--main-class", "com.ecdictionary.DictionaryApplication",
        "--app-version", $AppVersion,
        "--vendor", $Vendor,
        "--java-options", "-Djavafx.cachedir=`"`$APPDIR\javafx-cache`"",
        "--java-options", "--module-path=`"`$APPDIR`"",
        "--java-options", "--add-modules=javafx.controls,javafx.graphics"
    )

    if ($Type -ne "app-image") {
        $arguments += @(
            "--win-dir-chooser",
            "--win-menu",
            "--win-shortcut"
        )
    }

    if (Test-Path $OptionalIcon) {
        $arguments += @("--icon", $OptionalIcon)
    }

    & jpackage @arguments
    Write-Host "Package completed. Output directory: $DistDir"
}
finally {
    Pop-Location
}
