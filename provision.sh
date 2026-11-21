#!/usr/bin/env bash
set -e

# Focus Guard — Automated ADB Device Owner Provisioning Script

echo "=================================================="
echo "    Focus Guard — Device Owner Provisioning       "
echo "=================================================="
echo ""

# 1. Check for adb
if ! command -v adb &> /dev/null; then
    if [ -x "/home/prof21/.android-toolchain/sdk/platform-tools/adb" ]; then
        export PATH="/home/prof21/.android-toolchain/sdk/platform-tools:$PATH"
    else
        echo "[-] Error: 'adb' command not found in PATH."
        echo "    Please install Android platform-tools or add adb to your PATH."
        exit 1
    fi
fi

# 2. Check connected devices
echo "[*] Checking connected Android devices..."
DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | awk '{print $1}')
NUM_DEVICES=$(echo "$DEVICES" | grep -v '^$' | wc -l)

if [ "$NUM_DEVICES" -eq 0 ]; then
    echo "[-] No Android device detected in 'device' state."
    echo "    Please connect your phone via USB and enable USB Debugging."
    exit 1
fi

DEVICE_ID=$(echo "$DEVICES" | head -n 1)
echo "[+] Detected device: $DEVICE_ID"

# 3. Check for existing accounts on device
echo "[*] Checking for existing accounts on device..."
ACCOUNTS=$(adb -s "$DEVICE_ID" shell dumpsys account | grep "Account {" || true)
if [ -n "$ACCOUNTS" ]; then
    echo ""
    echo "[!] WARNING: Detected accounts on device:"
    echo "$ACCOUNTS" | sed 's/^/    /'
    echo ""
    echo "[-] Android Enterprise requires that NO accounts exist when setting a Device Owner."
    echo "    Please remove all accounts in Settings > Passwords & Accounts (or Accounts),"
    echo "    or perform this setup immediately after a factory reset."
    echo "    Otherwise, 'dpm set-device-owner' will fail with an IllegalStateException."
    echo ""
    read -p "Do you want to attempt provisioning anyway? (y/N): " CONFIRM
    if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
        echo "Aborted by user."
        exit 1
    fi
fi

# 4. Check APK file
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    echo "[*] APK not found. Building APK with Gradle..."
    export JAVA_HOME="/home/prof21/.android-toolchain/jdk-17"
    export PATH="$JAVA_HOME/bin:$PATH"
    ./gradlew assembleDebug
fi

echo "[*] Installing Focus Guard APK ($APK_PATH)..."
adb -s "$DEVICE_ID" install -r -g "$APK_PATH"

# 5. Set Device Owner
echo "[*] Granting Device Owner privileges to Focus Guard..."
RESULT=$(adb -s "$DEVICE_ID" shell dpm set-device-owner com.focusguard/.receiver.FocusGuardDeviceAdminReceiver 2>&1 || true)

echo "$RESULT"

if echo "$RESULT" | grep -iq "Success"; then
    echo ""
    echo "=================================================="
    echo "[+] SUCCESS: Focus Guard is now the Device Owner!"
    echo "=================================================="

    # 6. Apply Controlled Google App Policies on Samsung One UI
    echo "[*] Disabling Google Discover / -1 screen on Samsung One UI Launcher..."
    adb -s "$DEVICE_ID" shell settings put system page_minus_one_type 0 || true

    echo "[*] Disabling Circle to Search & Floating Shortcuts..."
    adb -s "$DEVICE_ID" shell settings put secure touch_and_hold_to_search 0 || true
    adb -s "$DEVICE_ID" shell settings put secure accessibility_button_targets "" || true

    echo "[*] Granting WRITE_SECURE_SETTINGS and POST_NOTIFICATIONS to Focus Guard..."
    adb -s "$DEVICE_ID" shell pm grant com.focusguard android.permission.WRITE_SECURE_SETTINGS || true
    adb -s "$DEVICE_ID" shell pm grant com.focusguard android.permission.POST_NOTIFICATIONS || true

    echo "[*] Enabling Focus Guard Accessibility Protection..."
    adb -s "$DEVICE_ID" shell settings put secure enabled_accessibility_services com.focusguard/com.focusguard.accessibility.WhatsAppCommunityAccessibilityService || true
    adb -s "$DEVICE_ID" shell settings put secure accessibility_enabled 1 || true

    echo "[*] Enforcing Private DNS (high.kahfguard.com)..."
    adb -s "$DEVICE_ID" shell settings put global private_dns_mode hostname || true
    adb -s "$DEVICE_ID" shell settings put global private_dns_specifier high.kahfguard.com || true

    echo ""
    echo "Next steps:"
    echo "1. Open Focus Guard on your phone."
    echo "2. Tap 'Enforce All Policies Now'."
    echo "3. You may now safely log back into your Google account."
else
    echo ""
    echo "[-] Device Owner provisioning was not successful."
    echo "    Review the error message above."
    echo "    Common causes:"
    echo "    - An account is still present on the device."
    echo "    - Another Device Policy Controller is already active."
    echo "    - On Xiaomi devices, turn off 'MIUI Optimization' in Developer Options."
fi
