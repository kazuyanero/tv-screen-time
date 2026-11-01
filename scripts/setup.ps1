# Install Screen Time on an Android TV over ADB and set it up in one go:
# install the APK, grant its permissions, set the parent PIN and the daily limit, start it.
#
# Usage (from the repo folder, USB/network debugging on, TV and PC on the same network):
#   powershell -ExecutionPolicy Bypass -File scripts\setup.ps1 -TvHost 192.168.1.50 -Pin 1234 [-Limit 30] [-Apk path\to\app-release.apk]
#
# Needs Google's adb (Android SDK platform-tools) on PATH or in %LOCALAPPDATA%\Android\Sdk\platform-tools.
param(
    [Parameter(Mandatory = $true)][string]$TvHost,
    [Parameter(Mandatory = $true)][ValidatePattern('^\d{4}$')][string]$Pin,
    [ValidateRange(5, 480)][int]$Limit = 30,
    [string]$Apk = (Join-Path $PSScriptRoot '..\app\build\outputs\apk\release\app-release.apk')
)
$ErrorActionPreference = 'Stop'
$pkg = 'local.tv.screentime'

$adb = (Get-Command adb -ErrorAction SilentlyContinue).Source
if (-not $adb) { $adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe' }
if (-not (Test-Path $adb)) { throw 'adb not found: install Android SDK platform-tools or put adb on PATH' }
if (-not (Test-Path $Apk)) { throw "APK not found: $Apk (build it first: .\gradlew.bat assembleRelease)" }
$target = if ($TvHost -match ':') { $TvHost } else { "${TvHost}:5555" }

function Adb { & $adb -s $target @args; if ($LASTEXITCODE -ne 0) { throw "adb $($args -join ' ') failed" } }

Write-Host "Connecting to $target (accept 'Allow USB debugging?' on the TV the first time)"
& $adb connect $target | Write-Host
Adb wait-for-device

Write-Host 'Installing'
Adb install -r $Apk

Write-Host 'Granting permissions'
Adb shell appops set $pkg GET_USAGE_STATS allow
Adb shell appops set $pkg SYSTEM_ALERT_WINDOW allow
# TCL and some other brands block apps from starting at boot unless they hold this app-op; others do not have it.
& $adb -s $target shell appops set $pkg APP_AUTO_START allow 2>$null | Out-Null
Adb shell appops write-settings

Write-Host "Setting the PIN and a daily limit of $Limit minutes"
Adb shell am broadcast -n "$pkg/.AdminReceiver" --es pin $Pin --ei limit $Limit

Write-Host 'Checking'
Adb shell "appops get $pkg | grep -E 'USAGE|ALERT|AUTO_START'"
Adb shell "dumpsys activity services $pkg | grep -m1 ServiceRecord"
Write-Host "Done. Open 'Screen Time' on the TV and enter the PIN to see its settings."
