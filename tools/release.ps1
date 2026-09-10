param([string]$Version = '1.1.0')
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    if (-not (Test-Path -LiteralPath 'keystore.properties')) { throw 'Configure your release signing identity first (tools/init-signing.ps1).' }
    & .\gradlew.bat --no-daemon testDebugUnitTest lintRelease assembleRelease bundleRelease
    if ($LASTEXITCODE -ne 0) { throw 'Build or validation failed.' }
    New-Item -ItemType Directory -Force -Path dist | Out-Null
    Copy-Item -LiteralPath app/build/outputs/apk/release/app-release.apk -Destination "dist/Qingling-$Version.apk"
    Copy-Item -LiteralPath app/build/outputs/bundle/release/app-release.aab -Destination "dist/Qingling-$Version.aab"
    & git archive --format=zip "--output=dist/Qingling-$Version-source.zip" HEAD
    if ($LASTEXITCODE -ne 0) { throw 'Commit the source before packaging a release archive.' }
    $checks = Get-ChildItem -LiteralPath dist -File | Where-Object { $_.Name -like "Qingling-$Version*" -and $_.Extension -in '.apk','.aab','.zip' } | ForEach-Object {
        $hash = Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256
        "$($hash.Hash.ToLower())  $($_.Name)"
    }
    [IO.File]::WriteAllLines((Join-Path $root 'dist/SHA256SUMS.txt'),$checks,(New-Object Text.UTF8Encoding($false)))
} finally { Pop-Location }
