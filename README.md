# Focus Guard — Native Android Device Restriction System

Focus Guard is a persistent, device-level restriction system built for personally managed Android devices using **Android Enterprise Device Owner / Device Policy Controller (DPC)** capabilities.

This is **not** a habit tracker, productivity dashboard, accountability application, or screen-time analytics application. Its sole objective is deterministic enforcement of an essential application allowlist, prevention of general-purpose web browsing, and device tamper protection with low battery and RAM consumption.

---

## Key Architectural Principles

- **Native Android & Kotlin**: Built on standard Android Enterprise APIs and Jetpack Compose (Material 3).
- **Event-Driven, Zero-Polling Enforcement**:
  - DPM policies are enforced at the Android OS framework layer and persist across reboots automatically without running continuous background services.
  - `BootReceiver` re-asserts policies after system startup.
  - `PackageChangeReceiver` immediately evaluates newly installed packages and hides unapproved apps on arrival.
- **Embedded WebViews Preserved**: Standalone browsers (Chrome, Firefox, etc.) are blocked, but `com.google.android.webview` is strictly preserved, ensuring that banking apps, payment gateways, and authentication flows remain functional.
- **System Updates Preserved**: Google Play Store (`com.android.vending`) and OS system updates remain enabled so approved apps and Android security patches stay up-to-date.
- **Hardware-Level Protection**: Sideloading is blocked via `DISALLOW_INSTALL_UNKNOWN_SOURCES`, VPN bypass is blocked via `DISALLOW_CONFIG_VPN`, Safe Mode is disabled via `DISALLOW_SAFE_BOOT`, and Focus Guard uninstallation is locked via `setUninstallBlocked`.
- **System-Wide Private DNS**: Enforces DNS-over-TLS using `high.kahfguard.com` (Kahf Guard High Protection) and locks DNS settings using `DISALLOW_CONFIG_PRIVATE_DNS`.
- **WhatsApp Communities Guard**: Uses a scoped, low-overhead `AccessibilityService` (active solely within `com.whatsapp`) to detect and exit the Communities section while preserving normal WhatsApp chats, calls, and media.

---

## Application Catalog

### 1. Allowed Applications

| Category | Application | Package Name | Notes |
| :--- | :--- | :--- | :--- |
| **Communication** | WhatsApp | `com.whatsapp` | Communities guarded via Accessibility |
| | WhatsApp Business | `com.whatsapp.w4b` | Communities guarded via Accessibility |
| | Brilliant Connect | `com.brilliant.connect.com.bd` | IP Telephony & Low-cost calls |
| **Banking & Payments** | iSmart (IBBL) | `com.ibbl.ibbl_ismart`, `com.ibbl.ismart` | Full banking flow supported |
| | Cellfin (IBBL) | `com.ibbl.cellfin` | In-app WebView preserved |
| | bKash | `com.bKash.customerapp` | MFS |
| | Nagad | `com.konasl.nagad` | MFS |
| | Rocket (DBBL) | `com.dbbl.mbs.apps.main` | MFS |
| | Upay | `bd.com.ucb.upay` | MFS |
| **Transportation** | Google Maps | `com.google.android.apps.maps` | Navigation |
| | Rapid Pass | `bd.gov.dmtcl.rapidpass`, `bd.gov.rapidpass` | Metro & transit |
| **AI & Study** | ChatGPT | `com.openai.chatgpt` | AI Assistant |
| | Claude | `com.anthropic.claude` | AI Assistant |
| | Gemini | `com.google.android.apps.bard` | AI Assistant |
| | NotebookLM | `com.google.android.apps.notebooklm` | Study Tool |
| | Google PDF Viewer | `com.google.android.apps.pdfviewer` | Document Viewer |
| | Adobe Acrobat | `com.adobe.reader` | PDF Viewer |
| | Samsung Notes | `com.samsung.android.app.notes` | Note-taking |
| **Google Productivity** | Google Drive | `com.google.android.apps.docs` | Cloud Storage |
| | Google Docs | `com.google.android.apps.docs.editors.docs` | Document Editor |
| | Google Sheets | `com.google.android.apps.docs.editors.sheets` | Spreadsheet Editor |
| | Google Photos | `com.google.android.apps.photos` | Cloud Gallery |
| **Essential System** | Phone / Dialer | `com.google.android.dialer`, `com.android.dialer`, etc. | Calls |
| | Messages / SMS | `com.google.android.apps.messaging`, `com.android.mms` | SMS |
| | Contacts | `com.google.android.contacts`, `com.android.contacts` | Address Book |
| | Camera | `com.google.android.GoogleCamera`, `com.android.camera` | Hardware Camera |
| | Gallery | `com.google.android.apps.photosgo`, OEM Gallery | Local Media |
| | File Manager | `com.google.android.apps.nbu.files`, `com.android.documentsui` | Local Files |
| | Clock & Calculator | Google / OEM Clock & Calculator | Utilities |
| | Keyboards | Gboard, Samsung Keyboard, SwiftKey, OpenBoard | Input Methods |
| | Android System WebView | `com.google.android.webview` | **Crucial for auth/banking** |
| | Google Play Services | `com.google.android.gms`, `com.google.android.gsf` | Core Services |
| | Google Play Store | `com.android.vending` | App updates |

*The allowlist is expandable through the in-app "Add Custom Package" feature or by updating the policy repository.*

### 2. Blocked Applications

- **General-Purpose Browsers**: Google Chrome, Mozilla Firefox, Samsung Internet, Opera, Brave, Microsoft Edge, DuckDuckGo, Mi Browser, ColorOS Browser, Vivo Browser, Phoenix, UC Browser, Tor Browser.
- **Telegram**: Regular Telegram (`org.telegram.messenger`), Telegram Web, Telegram X, NekoX. *(A dedicated personal cloud file browser may be allowed separately in the future).*
- **Social Media & Distractions**: YouTube, YouTube Music, YouTube Kids, Facebook, Facebook Lite, Messenger, Instagram, TikTok, Twitter/X, Snapchat, Reddit, Pinterest, Netflix.

---

### 3. Permanent Approved App Registry & Reinstallation

Focus Guard features an administrator-controlled **Permanent Approved App Registry** (`approved_app_registry.json`) designed to address a critical usability requirement:
- **Reinstallability**: When an approved app (e.g., WhatsApp, Brilliant Connect, bKash) is uninstalled by the user, its record in the registry is **not** deleted. Focus Guard only marks `isCurrentlyInstalled = false`.
- **Install Policy Discrimination**:
  1. **Approved Application (New / Reinstall)**: Recognized in `ApprovedAppRegistry`. Permitted to install and unhide immediately.
  2. **Baseline Component Update**: Cryptographically verified against pre-provisioning SHA-256 certificate fingerprints. Permitted to update.
  3. **Unapproved Application**: Instantly suspended, hidden via `setApplicationHidden(true)`, and passed to `PackageInstaller.uninstall()`.
- **One-Tap Store Reinstallation**: Uninstalled approved apps remain clearly visible in the **Allowed Apps** screen with an `UNINSTALLED (APPROVED)` badge and a direct store reinstall button to open Google Play Store / Galaxy Store.

---

## Device Owner Provisioning Guide

To activate Focus Guard as the **Device Owner (DO)**, choose one of the two standard Android Enterprise provisioning paths:

### Method A: Provisioning via ADB (Recommended)

1. **Prepare Device**:
   - Temporarily remove all user accounts from your phone: **Settings > Passwords & Accounts** (remove Google, WhatsApp, Samsung accounts).
   - Alternatively, perform this on a fresh device immediately after setup before adding any accounts.
2. **Enable USB Debugging**:
   - Go to **Settings > About Phone > Tap 'Build Number' 7 times**.
   - Go to **Settings > System > Developer Options > Enable 'USB Debugging'**.
3. **Connect via USB and Install APK**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. **Set Device Owner**:
   ```bash
   adb shell dpm set-device-owner com.focusguard/.receiver.FocusGuardDeviceAdminReceiver
   ```
   *Expected Output*: `Success: Device owner set to package com.focusguard`
5. **Enforce Restrictions**:
   - Open Focus Guard on the phone.
   - Tap **"Enforce All Policies Now"**.
   - You may now re-add your Google account in Settings.

### Method B: Factory Reset QR Code Provisioning (Zero-Touch)

1. Factory reset the device.
2. On the first "Welcome" or "Start" setup screen, **tap the blank screen area 6 times**.
3. Scan a QR code containing the provisioning bundle:
   ```json
   {
     "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.focusguard/.receiver.FocusGuardDeviceAdminReceiver",
     "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://your-server.com/focusguard.apk",
     "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": "<SHA-256-OF-APK>",
     "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true
   }
   ```
4. Android will download Focus Guard and provision it directly as the Device Owner.

---

## WhatsApp Communities Guard

Because Android's security model prevents Device Policy Controller APIs from inspecting internal views of third-party apps, Focus Guard employs a dedicated, lightweight `AccessibilityService` (`WhatsAppCommunityAccessibilityService`).

- **Scope**: The service configuration specifies `android:packageNames="com.whatsapp,com.whatsapp.w4b"`. Android's accessibility framework dispatches events **only** when WhatsApp is in the foreground. Outside WhatsApp, resource usage is zero.
- **Action**: Detects the Communities tab or community header nodes and immediately executes `GLOBAL_ACTION_BACK`, redirecting the user back to normal chats.
- **Limitations**:
  - Requires manually enabling in **Settings > Accessibility > Focus Guard — WhatsApp Communities Guard**.
  - Significant future WhatsApp UI layout overhauls may require updating the view ID or text matchers.

---

## Safety & Escape Routes

- **Factory Reset Unrestricted**: Focus Guard does not lock the bootloader or modify recovery partitions. Hardware recovery factory reset remains accessible as an ultimate escape route.
- **Anti-Brick Safeguards**: The package policy strictly excludes critical system packages (`android`, `com.android.systemui`, `com.android.settings`, `com.google.android.webview`) from being hidden or suspended.

---

## Project Structure

```
focus-guard/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── res/
│   │   │   │   ├── xml/
│   │   │   │   │   ├── device_admin_receiver.xml
│   │   │   │   │   └── accessibility_service_config.xml
│   │   │   │   └── values/
│   │   │   │       ├── strings.xml
│   │   │   │       └── themes.xml
│   │   │   └── java/com/focusguard/
│   │   │       ├── FocusGuardApp.kt
│   │   │       ├── data/
│   │   │       │   ├── model/
│   │   │       │   │   ├── AppModels.kt
│   │   │       │   │   └── PolicyModels.kt
│   │   │       │   └── repository/
│   │   │       │       └── AllowlistRepository.kt
│   │   │       ├── dpc/
│   │   │       │   └── DevicePolicyController.kt
│   │   │       ├── receiver/
│   │   │       │   ├── FocusGuardDeviceAdminReceiver.kt
│   │   │       │   ├── BootReceiver.kt
│   │   │       │   └── PackageChangeReceiver.kt
│   │   │       ├── accessibility/
│   │   │       │   └── WhatsAppCommunityAccessibilityService.kt
│   │   │       └── ui/
│   │   │           ├── MainActivity.kt
│   │   │           ├── theme/
│   │   │           │   ├── Color.kt
│   │   │           │   └── Theme.kt
│   │   │           └── screens/
│   │   │               ├── StatusScreen.kt
│   │   │               ├── AllowedAppsScreen.kt
│   │   │               └── AdminProvisioningScreen.kt
│   │   └── test/java/com/focusguard/
│   │       └── PolicyVerificationTest.kt
│   └── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Building and Testing

```bash
# Set Java 17 environment
export JAVA_HOME=/home/prof21/.android-toolchain/jdk-17
export PATH=$JAVA_HOME/bin:$PATH

# Run unit tests
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```
