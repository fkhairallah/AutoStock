# Launches the Android Auto Desktop Head Unit for testing.
# Works with both physical devices and emulators.
# One-time setup: Android Auto app > About > tap version 10x to unlock developer mode > Start head unit server

$sdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME }
           elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT }
           else { "$env:LOCALAPPDATA\Android\Sdk" }

$adb = "$sdkRoot\platform-tools\adb.exe"
$dhu = "$sdkRoot\extras\google\auto\desktop-head-unit.exe"

if (-not (Test-Path $adb)) {
    Write-Error "adb not found at: $adb"
    exit 1
}
if (-not (Test-Path $dhu)) {
    Write-Error "Desktop Head Unit not found at: $dhu"
    Write-Error "Install via Android Studio: SDK Manager > SDK Tools > Android Auto Desktop Head Unit Emulator"
    exit 1
}

# Parse connected devices — @() forces array so .Count always works
$devices = @(& $adb devices | Select-Object -Skip 1 | ForEach-Object {
    if ($_ -match '^(\S+)\s+(device|offline|unauthorized)\s*$') {
        [PSCustomObject]@{ Serial = $Matches[1]; State = $Matches[2] }
    }
} | Where-Object { $_ -ne $null -and $_.State -eq "device" })

if ($devices.Count -eq 0) {
    Write-Error "No devices connected. Start your emulator or connect a phone."
    exit 1
}

if ($devices.Count -eq 1) {
    $target = $devices[0].Serial
} else {
    Write-Host "Multiple devices found:"
    for ($i = 0; $i -lt $devices.Count; $i++) {
        $label = if ($devices[$i].Serial -like "emulator-*") { "emulator" } else { "device" }
        Write-Host "  [$i] $($devices[$i].Serial) ($label)"
    }
    $choice = Read-Host "Select device number"
    $target = $devices[[int]$choice].Serial
}

$label = if ($target -like "emulator-*") { "emulator $target" } else { "device $target" }

# Check Android Auto is installed and the head unit server is likely running
$aaPackage = & $adb -s $target shell pm list packages com.google.android.projection.gearhead 2>$null
if (-not ($aaPackage -match "gearhead")) {
    Write-Error "Android Auto is not installed on $label."
    Write-Error "Install it from Google Play, then open Android Auto > About > tap version 10x > Start head unit server."
    exit 1
}

Write-Host "Forwarding ADB port 5277 on $label..."
& $adb -s $target forward tcp:5277 tcp:5277
if ($LASTEXITCODE -ne 0) {
    Write-Error "ADB forward failed."
    exit 1
}

# Verify something is actually listening on 5277
$listening = & $adb -s $target shell ss -tlnp 2>$null | Select-String "5277"
if (-not $listening) {
    Write-Warning "Android Auto head unit server does not appear to be running."
    Write-Warning "Open Android Auto > About > tap version 10x > Start head unit server, then retry."
}

Write-Host "Launching Desktop Head Unit..."
& $dhu
