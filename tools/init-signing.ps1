param([string]$KeyDirectory = (Join-Path $env:LOCALAPPDATA 'QinglingSigning'))
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$props = Join-Path $root 'keystore.properties'
if (Test-Path -LiteralPath $props) { throw 'keystore.properties already exists; keep the original signing identity.' }
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to JDK 17 first.' }
New-Item -ItemType Directory -Force -Path $KeyDirectory | Out-Null
$key = Join-Path $KeyDirectory 'qingling-release.jks'
$secretFile = Join-Path $KeyDirectory 'signing-backup.json'
if (Test-Path -LiteralPath $key) { throw 'Signing key already exists. Restore keystore.properties from your secure backup.' }
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$secret = [Convert]::ToBase64String($bytes)
$env:QINGLING_KEY_PASSWORD = $secret
try {
    & (Join-Path $env:JAVA_HOME 'bin/keytool.exe') -genkeypair -v -keystore $key -storetype JKS -alias qingling -keyalg RSA -keysize 4096 -validity 10000 -storepass:env QINGLING_KEY_PASSWORD -keypass:env QINGLING_KEY_PASSWORD -dname 'CN=Qingling Release, OU=Mobile, O=Qingling, C=CN'
    if ($LASTEXITCODE -ne 0) { throw 'keytool failed' }
    $portableKey = $key.Replace('\','/')
    $lines = @("storeFile=$portableKey", "storePassword=$secret", 'keyAlias=qingling', "keyPassword=$secret")
    [IO.File]::WriteAllLines($props, $lines, (New-Object Text.UTF8Encoding($false)))
    @{ storeFile=$key; keyAlias='qingling'; password=$secret } | ConvertTo-Json | Set-Content -LiteralPath $secretFile -Encoding UTF8
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent().Name
    & icacls.exe $KeyDirectory /inheritance:r /grant:r "${identity}:(OI)(CI)F" 'SYSTEM:(OI)(CI)F' | Out-Null
    Write-Output "Signing identity created in $KeyDirectory. Back up this folder securely; never upload it."
} finally { Remove-Item Env:QINGLING_KEY_PASSWORD -ErrorAction SilentlyContinue }
