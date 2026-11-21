#!/usr/bin/env bash
set -e

# Focus Guard Update & Deployment Script
echo "[*] Building latest debug APK..."
JAVA_HOME=/home/prof21/.android-toolchain/jdk-17.0.13+11 ./gradlew assembleDebug

echo "[*] Temporarily lifting install restriction via maintenance broadcast..."
adb shell am broadcast -a com.focusguard.action.CLEAR_INSTALL_RESTRICTION -n com.focusguard/.receiver.BootReceiver

echo "[*] Installing APK..."
adb install -r -d app/build/outputs/apk/debug/app-debug.apk

echo "[*] Enforcing policies and unhiding/suspending apps..."
adb shell am broadcast -a com.focusguard.action.ENFORCE_POLICY -n com.focusguard/.receiver.BootReceiver

echo "[*] Launching Focus Guard in Essentials mode..."
adb shell am start -n com.focusguard/.ui.MainActivity --es screen_mode essentials

echo "[+] Focus Guard successfully deployed and active!"
