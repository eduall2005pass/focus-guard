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
- **Device Owner App Tamper Protection**: `DISALLOW_APPS_CONTROL` blocks Settings/launcher actions to clear app data or cache, disable, force-stop, or uninstall apps; `setUninstallBlocked` specifically protects Focus Guard from uninstall. This Settings restriction applies to apps across the device, and does not prevent a factory reset.
- **Hardware-Level Protection**: Sideloading is blocked via `DISALLOW_INSTALL_UNKNOWN_SOURCES`, VPN bypass is blocked via `DISALLOW_CONFIG_VPN`, and Safe Mode is disabled via `DISALLOW_SAFE_BOOT`.
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
| **Transportation & Delivery** | Google Maps | `com.google.android.apps.maps` | Navigation |
| | Rapid Pass | `bd.gov.dmtcl.rapidpass`, `bd.gov.rapidpass` | Metro & transit |
| | Pathao | `com.pathao.user` | Rides, Food, Courier & Delivery |
| | Uber | `com.ubercab` | Rides & Transit |
| | Foodpanda | `com.global.foodpanda.android` | Food & Grocery Delivery |
| | Foodi | `com.usbangla.foodi` | Food Delivery by US-Bangla |
| | Daraz | `com.daraz.android` | Online Shopping & Logistics |
| | Chaldal | `com.chaldal.poached` | Online Grocery Delivery |
| **AI & Study** | ChatGPT | `com.openai.chatgpt` | AI Assistant |
| | Claude | `com.anthropic.claude` | AI Assistant |
| | Gemini | `com.google.android.apps.bard` | AI Assistant |
| | NotebookLM | `com.google.android.apps.notebooklm` | Study Tool |
| | Google PDF Viewer | `com.google.android.apps.pdfviewer` | Document Viewer |
| | Adobe Acrobat | `com.adobe.reader` | PDF Viewer |
| | Samsung Notes | `com.samsung.android.app.notes` | Note-taking |
| | Muslim Bangla | `com.tos.namajtime`, `com.muslimbangla` | Prayer Times, Quran & Dua |
| | iHadis | `com.ihadis.ihadis` | Authentic Hadith Collection |
| | Mahfil | `com.mahfil.app` | Islamic Discussions & Lectures |
| | Al Quran (Tafsir & by Word) | `com.greentech.quran` | Quran Study & Tafsir by Greentech |
| | Complete Anatomy | `com.a3d4medical.completeanatomy`, `com.elsevier.threeD4Medical.CompleteAnatomy` | 3D Medical Anatomy Learning by Elsevier |
| **Google Productivity** | Gmail | `com.google.android.gm` | Email & Communication |
| | Google Drive | `com.google.android.apps.docs` | Cloud Storage |
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
| | Samsung Music | `com.sec.android.app.music` | Local Audio Player |
| | My Teletalk | `teletalk.teletalkcustomerapp` | Teletalk SIM & Carrier Self-Care |
| | Samsung Pass & Wallet | `com.samsung.android.samsungpass`, `com.samsung.android.samsungpassautofill`, `com.samsung.android.spay` | Biometric Credentials & Passkeys |
| | Google App Services | `com.google.android.googlequicksearchbox` | **Circle to Search & OCR text copy active** (Discover feed & search browsing blocked) |
| | Android System WebView | `com.google.android.webview` | **Crucial for auth/banking** |
| | Google Play Services | `com.google.android.gms`, `com.google.android.gsf` | Core Services |
| | Google Play Store | `com.android.vending` | App updates |

*The allowlist is expandable through the in-app "Add Custom Package" feature or by updating the policy repository.*

### 2. Blocked Applications

- **General-Purpose Browsers**: Google Chrome, Mozilla Firefox, Samsung Internet, Opera, Brave, Microsoft Edge, DuckDuckGo, Mi Browser, ColorOS Browser, Vivo Browser, Phoenix, UC Browser, Tor Browser.
- **Telegram Applications**: Regular standalone Telegram (`org.telegram.messenger`), Telegram Web, Telegram X, and NekoX remain strictly blocked at the OS framework level. Selected educational study channels and groups are accessed exclusively within Focus Guard's embedded **Telegram Study Mode** (using an integrated, restricted Telegram Web K engine).
- **Social Media & Distractions**: YouTube, YouTube Music, YouTube Kids, Facebook, Facebook Lite, Messenger, Instagram, TikTok, Twitter/X, Snapchat, Reddit, Pinterest, Netflix.

---

## Study Portal & Restricted Telegram Web K Integration

Focus Guard features an embedded, distraction-free **Study Portal** enabling verified access to approved medical education resources and Telegram channels without exposing general-purpose browsing or the unrestricted Telegram client.

### 1. Controlled WebView Architecture
- **Sandboxed Execution**: Educational websites open within a controlled WebView. No external browsers or intent fallbacks are exposed.
- **Focus-Control Filtering Engine**: Reuses the production Focus-Control whitelist (`_is_nav_permitted`) and real-time input filter (`BLOCKED_INPUT_WORDS`, Unicode normalization, leetspeak defeat, and zero-width filtering) ported directly from `focus-control`.
- **Approved Websites Registry**: Managed via `approved_study_websites.json`:
  - **Guess The Case (GTC)**: `https://guessthecase.eu.org/`
  - **Booimate**: `https://booimate.com/`
  - **NotebookLM**: `https://notebooklm.google.com/`
  - **Medical Contents**: `https://medical-contents.abdursakib.workers.dev/0:/`
  - **Muslim Bangla**: `https://muslimbangla.com/`
  - **iHadis**: `https://ihadis.com/en`
  - **LiveMCQ**: `https://web.livemcq.com/`
  *(Expandable with administrator authorization)*

### 2. Restricted Telegram Study Mode (Official Web K Engine)
- **Maintained Source Fork**: Includes a modified Telegram Web K source fork in `third_party/telegram-web-k/`, pinned to its upstream revision and distributed with its GPL-3.0 license and source. The app bundles this locally built frontend; it does not load Telegram's hosted Web K page. Rebuild it with `scripts/build-focus-study-webk.sh` (Node.js 22.18+ and Corepack).
- **Client Peer Gate**: Checks Telegram's resolved peer ID and peer type at Web K's conversation entry points. The native approved `type:id` registry permits channels, supergroups, and groups; user chats and bots are denied. Unknown communities request native device-credential approval before their conversation opens.
- **One-Time Authentication**: Initial login is performed inside the controlled WebView. Session credentials (`localStorage`, `IndexedDB`) persist across device reboots and app updates.
- **Kiosk Mode Enforcement**:
  - Left column (chat list, folder tabs, search bar, hamburger menu) is completely suppressed.
  - Social feeds, stories, global discovery, voice/video calls, and bot stores are blocked.
  - Center viewport expands to 100% of the display for maximum focus.
  - The WebView's persistent appassets origin keeps the Web K session local to Focus Guard. Telegram login and chat content still require internet access.

### 3. GTC Channel Interception & Admin Approval Flow
- Selecting a Telegram link (`t.me/...`, `tg://...`) from Guess The Case or study portals triggers automatic native interception.
- Focus Guard verifies the destination against the **Permanent Approved Telegram Registry** (`approved_telegram_registry.json`).
- If approved, it opens immediately in Telegram Study Mode.
- If unapproved, Web K resolves its real peer metadata, but the peer gate prevents the chat from opening and presents an **Admin Approval Prompt**. Only an approved channel, group, or supergroup is added to the registry and allowed to open.

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
