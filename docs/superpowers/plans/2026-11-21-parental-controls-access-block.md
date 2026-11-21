# Parental Control Access Block Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement DPC-level restriction for Parental Controls (triggering native Android "Action not allowed by your admin" dialog), add a defense-in-depth accessibility shield, and expose an administrative toggle inside Focus Guard's 3-dot Security Menu.

**Architecture:** A dedicated method pair `isParentalControlAccessBlocked()` and `setParentalControlAccessBlocked()` in `DevicePolicyController` manages package suspension of `com.samsung.android.app.parentalcare` via `DevicePolicyManager.setPackagesSuspended()` with SharedPreferences persistence. `WhatsAppCommunityAccessibilityService` provides a secondary safeguard in Settings. `SecurityThreeDotMenu` in `SecurityPinDialogs.kt` displays a reactive toggle after Focus Guard PIN verification.

**Tech Stack:** Kotlin, Android DevicePolicyManager (Android Enterprise Device Owner API), Jetpack Compose Material 3, Android AccessibilityService, SharedPreferences, JUnit 4, Robolectric / Mockito.

**Spec:** [docs/superpowers/specs/2026-11-21-parental-controls-access-block-design.md](file:///run/media/prof21/A/projects/focus-guard/docs/superpowers/specs/2026-11-21-parental-controls-access-block-design.md)

## Global Constraints
- Target package for parental controls: `com.samsung.android.app.parentalcare`.
- When blocked, `dpm.setPackagesSuspended` must be used so that Android OS displays the native system "Action not allowed by your admin" dialog.
- Focus Guard self-protection, critical system packages, and existing allowed apps must never be broken or hidden.
- UI changes must strictly reside inside the PIN-protected `SecurityThreeDotMenu` dialog.
- Build & test environment must use JDK 17 (`/home/prof21/.android-toolchain/jdk-17`).

## Review Focus
1. When Parental Controls is blocked, `com.samsung.android.app.parentalcare` is marked as suspended by DPM, triggering the system "Action not allowed" dialog.
2. When Parental Controls is unblocked via the 3-dot menu, `com.samsung.android.app.parentalcare` is unsuspended immediately and accessible again.
3. During automated policy re-enforcement (`enforceAllPolicies`), the user's Parental Control block setting is preserved and reapplied.
4. If a user bypasses or directly launches `SupervisionDashboardActivity`, the Accessibility Service backs out safely.
5. In the 3-dot menu, the button visibly indicates whether Parental Controls is currently Locked 🔒 or Allowed 🔓.

---

### Task 1: DPC Parental Controls Blocking & Persistence (TDD)

**Files:**
- Create: `app/src/test/java/com/focusguard/ParentalControlAccessBlockTest.kt`
- Modify: `app/src/main/java/com/focusguard/dpc/DevicePolicyController.kt`

**Interfaces:**
- Produces:
  - `DevicePolicyController.isParentalControlAccessBlocked(): Boolean`
  - `DevicePolicyController.setParentalControlAccessBlocked(blocked: Boolean): Boolean`
  - `DevicePolicyController.PARENTAL_CARE_PACKAGE = "com.samsung.android.app.parentalcare"`

- [ ] **Step 1: Write the unit test for Parental Control DPC restriction**

In `app/src/test/java/com/focusguard/ParentalControlAccessBlockTest.kt`:
```kotlin
package com.focusguard

import com.focusguard.dpc.DevicePolicyController
import org.junit.Assert.*
import org.junit.Test

class ParentalControlAccessBlockTest {

    @Test
    fun testParentalCarePackageConstant() {
        assertEquals("com.samsung.android.app.parentalcare", DevicePolicyController.PARENTAL_CARE_PACKAGE)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
export JAVA_HOME="/home/prof21/.android-toolchain/jdk-17" && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew testDebugUnitTest --tests com.focusguard.ParentalControlAccessBlockTest
```
Expected: FAIL with compilation error (unresolved reference `PARENTAL_CARE_PACKAGE`).

- [ ] **Step 3: Implement `PARENTAL_CARE_PACKAGE`, `isParentalControlAccessBlocked()`, and `setParentalControlAccessBlocked()` in `DevicePolicyController.kt`**

In `DevicePolicyController.kt`:
1. Add constant `const val PARENTAL_CARE_PACKAGE = "com.samsung.android.app.parentalcare"` in `companion object`.
2. Add SharedPreferences helper:
   ```kotlin
   private val parentalControlPrefs = context.getSharedPreferences("focusguard_parental_control_prefs", Context.MODE_PRIVATE)
   private const val KEY_PARENTAL_CONTROL_BLOCKED = "key_parental_control_blocked"

   fun isParentalControlAccessBlocked(): Boolean {
       return parentalControlPrefs.getBoolean(KEY_PARENTAL_CONTROL_BLOCKED, false)
   }

   fun setParentalControlAccessBlocked(blocked: Boolean): Boolean {
       parentalControlPrefs.edit().putBoolean(KEY_PARENTAL_CONTROL_BLOCKED, blocked).apply()
       if (!isDeviceOwner()) return false
       return try {
           if (blocked) {
               dpm.setApplicationHidden(adminComponent, PARENTAL_CARE_PACKAGE, false)
               val failed = dpm.setPackagesSuspended(adminComponent, arrayOf(PARENTAL_CARE_PACKAGE), true)
               failed.isEmpty()
           } else {
               dpm.setPackagesSuspended(adminComponent, arrayOf(PARENTAL_CARE_PACKAGE), false)
               dpm.setApplicationHidden(adminComponent, PARENTAL_CARE_PACKAGE, false)
               true
           }
       } catch (e: Exception) {
           Log.e(TAG, "Error updating parental control restriction", e)
           false
       }
   }
   ```
3. In `enforceAllPolicies()`:
   Ensure `PARENTAL_CARE_PACKAGE` is handled according to `isParentalControlAccessBlocked()`:
   ```kotlin
   if (pkgName == PARENTAL_CARE_PACKAGE) {
       if (isParentalControlAccessBlocked()) {
           try {
               dpm.setApplicationHidden(adminComponent, pkgName, false)
               dpm.setPackagesSuspended(adminComponent, arrayOf(pkgName), true)
           } catch (_: Exception) {}
       } else {
           try {
               dpm.setPackagesSuspended(adminComponent, arrayOf(pkgName), false)
               dpm.setApplicationHidden(adminComponent, pkgName, false)
           } catch (_: Exception) {}
       }
       continue
   }
   ```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
export JAVA_HOME="/home/prof21/.android-toolchain/jdk-17" && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew testDebugUnitTest --tests com.focusguard.ParentalControlAccessBlockTest
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/focusguard/dpc/DevicePolicyController.kt app/src/test/java/com/focusguard/ParentalControlAccessBlockTest.kt
git commit -m "feat(dpc): add parental control suspension restriction and persistence"
```

---

### Task 2: Accessibility Secondary Shield for Settings Supervision

**Files:**
- Modify: `app/src/main/java/com/focusguard/accessibility/WhatsAppCommunityAccessibilityService.kt`

**Interfaces:**
- Consumes:
  - `DevicePolicyController.isParentalControlAccessBlocked(): Boolean`
  - `DevicePolicyController.PARENTAL_CARE_PACKAGE`

- [ ] **Step 1: Update `handleSettingsAccessibilityEvent` in `WhatsAppCommunityAccessibilityService.kt`**

Add check for Parental Control block in Settings:
```kotlin
        // 1.5. Intercept attempts to access Parental Controls when blocked by policy
        val isParentalControlBlocked = try {
            DevicePolicyController(applicationContext).isParentalControlAccessBlocked()
        } catch (_: Exception) { false }

        if (isParentalControlBlocked) {
            val isParentalControlScreen = combinedText.contains("parental controls") ||
                    combinedText.contains("parental control") ||
                    combinedText.contains("অভিভাবকীয় নিয়ন্ত্রণ") ||
                    (event.className?.toString()?.contains("SupervisionDashboardActivity", ignoreCase = true) == true)

            // Do not block when we are only in main settings homepage list
            val isDashboardScreen = event.className?.toString()?.contains("SupervisionDashboardActivity", ignoreCase = true) == true ||
                    (isParentalControlScreen && !combinedText.contains("device care") && !combinedText.contains("general management"))

            if (isDashboardScreen) {
                val now = System.currentTimeMillis()
                if (now - lastBlockTimestamp > DEBOUNCE_MILLIS) {
                    lastBlockTimestamp = now
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    mainHandler.post {
                        Toast.makeText(
                            applicationContext,
                            "Focus Guard: প্যারেন্টাল কন্ট্রোল অ্যাক্সেস পলিসি অনুযায়ী ব্লক করা রয়েছে।",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                return
            }
        }
```

- [ ] **Step 2: Run all unit tests to ensure zero regressions**

Run:
```bash
export JAVA_HOME="/home/prof21/.android-toolchain/jdk-17" && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew testDebugUnitTest
```
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/focusguard/accessibility/WhatsAppCommunityAccessibilityService.kt
git commit -m "feat(accessibility): add secondary safeguard against Settings parental controls access"
```

---

### Task 3: Security 3-Dot Menu UI Toggle & Feedback

**Files:**
- Modify: `app/src/main/java/com/focusguard/ui/components/SecurityPinDialogs.kt`

**Interfaces:**
- Consumes:
  - `actualDpcController.isParentalControlAccessBlocked(): Boolean`
  - `actualDpcController.setParentalControlAccessBlocked(blocked: Boolean): Boolean`

- [ ] **Step 1: Add Parental Controls access toggle item in `SecurityThreeDotMenu`**

In `SecurityPinDialogs.kt`, inside `SecurityThreeDotMenu`'s `if (showSecurityOptionsDialog)` dialog:
Add state:
```kotlin
var isParentalBlocked by remember { mutableStateOf(actualDpcController.isParentalControlAccessBlocked()) }
```
Add action button in the `Column`:
```kotlin
OutlinedButton(
    onClick = {
        val newTarget = !isParentalBlocked
        val success = actualDpcController.setParentalControlAccessBlocked(newTarget)
        if (success) {
            isParentalBlocked = newTarget
            val msg = if (newTarget) {
                "প্যারেন্টাল কন্ট্রোল অ্যাক্সেস বন্ধ করা হয়েছে (Action Not Allowed)।"
            } else {
                "প্যারেন্টাল কন্ট্রোল অ্যাক্সেস চালু করা হয়েছে (Allowed)।"
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "ডিভাইস ওনার অনুমতি প্রয়োজন।", Toast.LENGTH_SHORT).show()
        }
    },
    modifier = Modifier.fillMaxWidth(),
    colors = ButtonDefaults.outlinedButtonColors(
        contentColor = if (isParentalBlocked) Color(0xFFFFB74D) else GuardGreenText
    ),
    shape = RoundedCornerShape(10.dp)
) {
    Icon(
        if (isParentalBlocked) Icons.Default.Lock else Icons.Default.LockOpen,
        contentDescription = null,
        modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            if (isParentalBlocked) "প্যারেন্টাল কন্ট্রোল আনব্লক করুন (Unblock)" else "প্যারেন্টাল কন্ট্রোল ব্লক করুন (Block Access)",
            fontWeight = FontWeight.SemiBold
        )
        Text(
            if (isParentalBlocked) "স্ট্যাটাস: ব্লকড (Action Not Allowed) 🔒" else "স্ট্যাটাস: সচল (Allowed) 🔓",
            style = MaterialTheme.typography.bodySmall,
            color = if (isParentalBlocked) Color(0xFFFFB74D) else GuardGreenText
        )
    }
}
```

- [ ] **Step 2: Run build and unit tests**

Run:
```bash
export JAVA_HOME="/home/prof21/.android-toolchain/jdk-17" && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew assembleDebug testDebugUnitTest
```
Expected: BUILD SUCCESSFUL and all tests pass.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/focusguard/ui/components/SecurityPinDialogs.kt
git commit -m "feat(ui): add parental control block toggle in 3-dot security menu"
```

---

### Task 4: Compact Study Resources Home Screen Widget

**Files:**
- Create: `app/src/main/res/layout/widget_study_resources.xml`
- Create: `app/src/main/res/drawable/bg_study_widget.xml`
- Create: `app/src/main/res/xml/study_resources_widget_info.xml`
- Create: `app/src/main/java/com/focusguard/study/widget/StudyResourcesWidgetProvider.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/focusguard/ui/MainActivity.kt`

**Interfaces:**
- Produces:
  - `StudyResourcesWidgetProvider : AppWidgetProvider`
  - BroadcastReceiver action `android.appwidget.action.APPWIDGET_UPDATE`
  - PendingIntent launching `MainActivity` with `screen_mode = "study_sites"` and `tab_index = 1`

- [ ] **Step 1: Create widget background drawable and layout**

Create `app/src/main/res/drawable/bg_study_widget.xml`:
Rounded corner container (18dp), `#0F241D` dark forest green background, `1.5dp` sage green border (`#2E7D32` / `#4CAF50`).

Create `app/src/main/res/layout/widget_study_resources.xml`:
Compact layout with:
- Root `LinearLayout` with id `widget_root` and `@drawable/bg_study_widget`
- `ImageView` displaying `@drawable/ic_book_open` (or study icon) with tint `#A5D6A7`
- `TextView` displaying `@string/study_resources_widget_title` ("Study Resources")
- `TextView` displaying `@string/study_resources_widget_subtitle` ("Focus Guard")

- [ ] **Step 2: Create widget provider info XML and string resources**

In `app/src/main/res/values/strings.xml`:
Add:
- `study_resources_widget_title`: "Study Resources"
- `study_resources_widget_subtitle`: "Focus Guard"
- `study_resources_widget_desc`: "Direct shortcut to Study Resources & Approved Sites"

Create `app/src/main/res/xml/study_resources_widget_info.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="110dp"
    android:minHeight="54dp"
    android:targetCellWidth="2"
    android:targetCellHeight="1"
    android:updatePeriodMillis="0"
    android:initialLayout="@layout/widget_study_resources"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen"
    android:description="@string/study_resources_widget_desc" />
```

- [ ] **Step 3: Implement `StudyResourcesWidgetProvider` and update `MainActivity`**

Create `app/src/main/java/com/focusguard/study/widget/StudyResourcesWidgetProvider.kt`:
```kotlin
package com.focusguard.study.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.focusguard.R
import com.focusguard.ui.MainActivity

class StudyResourcesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("screen_mode", "study_sites")
                putExtra("tab_index", 1)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.widget_study_resources)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
```

In `app/src/main/java/com/focusguard/ui/MainActivity.kt`:
Ensure `handleScreenModeIntent` recognizes `"study_sites"` and `"study_resources"`:
```kotlin
    private fun handleScreenModeIntent(intent: android.content.Intent?) {
        when (intent?.getStringExtra("screen_mode")) {
            "security" -> {
                initialSecurityTabIndex = intent.getIntExtra("tab_index", 0)
                currentScreenMode = AppScreenMode.DEVICE_SECURITY
            }
            "study_sites", "study_resources" -> {
                initialSecurityTabIndex = 1
                currentScreenMode = AppScreenMode.DEVICE_SECURITY
            }
            "essentials" -> {
                currentScreenMode = AppScreenMode.DAILY_ESSENTIALS
            }
        }
    }
```

In `app/src/main/AndroidManifest.xml`:
Register receiver:
```xml
        <receiver
            android:name=".study.widget.StudyResourcesWidgetProvider"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/study_resources_widget_info" />
        </receiver>
```

- [ ] **Step 4: Verify build and unit tests**

Run:
```bash
export JAVA_HOME="/home/prof21/.android-toolchain/jdk-17" && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew testDebugUnitTest assembleDebug
```
Expected: BUILD SUCCESSFUL and all tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/widget_study_resources.xml app/src/main/res/drawable/bg_study_widget.xml app/src/main/res/xml/study_resources_widget_info.xml app/src/main/java/com/focusguard/study/widget/StudyResourcesWidgetProvider.kt app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml app/src/main/java/com/focusguard/ui/MainActivity.kt
git commit -m "feat(widget): add compact study resources home screen shortcut widget"
```

---

### Task 5: Strict In-App Browser Domain Lockdown (Zero YouTube & Unapproved Browsing)

**Files:**
- Create: `app/src/test/java/com/focusguard/study/StudyNavigationGuardTest.kt`
- Modify: `app/src/main/java/com/focusguard/study/filter/StudyNavigationGuard.kt`
- Modify: `app/src/main/java/com/focusguard/study/ui/RestrictedAuthWebViewActivity.kt`
- Modify: `app/src/main/java/com/focusguard/study/ui/FullscreenStudyBrowser.kt`
- Modify: `app/src/main/java/com/focusguard/study/ui/ControlledStudyWebView.kt`

**Interfaces:**
- Produces:
  - `StudyNavigationGuard.isNavPermitted(url, approvedHosts, telegramMode, mainFrame): Boolean` with zero YouTube exceptions and strict domain checking.
  - `RestrictedAuthWebViewActivity` domain allowlist validator enforcing Auth SSO domains + `StudyWebsiteRegistry` approved hosts.
  - `onCreateWindow` popup navigation interception in `FullscreenStudyBrowser` and `ControlledStudyWebView`.

- [ ] **Step 1: Write TDD unit test verifying strict domain lockdown and YouTube blocking**

In `app/src/test/java/com/focusguard/study/StudyNavigationGuardTest.kt`:
```kotlin
package com.focusguard.study

import com.focusguard.study.filter.StudyNavigationGuard
import org.junit.Assert.*
import org.junit.Test

class StudyNavigationGuardTest {

    private val sampleApprovedHosts = setOf("boimate.com", "du.ac.bd", "gtc.edu.bd")

    @Test
    fun testYouTubeUrlsAreStrictlyBlocked() {
        val youtubeUrls = listOf(
            "https://www.youtube.com",
            "https://m.youtube.com",
            "https://youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://music.youtube.com",
            "https://www.youtube.com/shorts/abcdef12345"
        )
        for (url in youtubeUrls) {
            assertFalse("YouTube URL must be blocked: $url", StudyNavigationGuard.isNavPermitted(url, sampleApprovedHosts))
        }
    }

    @Test
    fun testApprovedHostsArePermitted() {
        val allowedUrls = listOf(
            "https://boimate.com",
            "https://du.ac.bd/study",
            "https://gtc.edu.bd/portal"
        )
        for (url in allowedUrls) {
            assertTrue("Approved study host must be allowed: $url", StudyNavigationGuard.isNavPermitted(url, sampleApprovedHosts))
        }
    }

    @Test
    fun testEssentialAuthHostsArePermitted() {
        val authUrls = listOf(
            "https://accounts.google.com/o/oauth2/auth",
            "https://appleid.apple.com/auth/authorize",
            "https://challenges.cloudflare.com/turnstile/v0/api.js"
        )
        for (url in authUrls) {
            assertTrue("Essential auth/supporting host must be allowed: $url", StudyNavigationGuard.isNavPermitted(url, sampleApprovedHosts))
        }
    }

    @Test
    fun testUnapprovedDomainsAreStrictlyBlocked() {
        val blockedUrls = listOf(
            "https://facebook.com",
            "https://instagram.com",
            "https://reddit.com",
            "https://google.com/search?q=test",
            "https://bing.com",
            "https://random-unapproved-site.org"
        )
        for (url in blockedUrls) {
            assertFalse("Unapproved URL must be blocked: $url", StudyNavigationGuard.isNavPermitted(url, sampleApprovedHosts))
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails on YouTube URLs**

Run:
```bash
export JAVA_HOME="/home/prof21/.android-toolchain/jdk-17" && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew testDebugUnitTest --tests com.focusguard.study.StudyNavigationGuardTest
```
Expected: FAIL because `youtube.com/watch?v=...` is currently permitted by `specificYoutubeVideo`.

- [ ] **Step 3: Remove YouTube exception from `StudyNavigationGuard.kt` and lock down `RestrictedAuthWebViewActivity.kt`**

In `StudyNavigationGuard.kt`:
1. Remove `specificYoutubeVideo` checks. YouTube is always blocked:
   ```kotlin
   if (isHostBlocked(host) && !directDriveDownload) {
       return false
   }
   if (isYoutubeHost(host)) return false
   ```
2. In `BUILTIN_SUPPORTING_DOMAINS`, add standard auth SSO domains: `appleid.apple.com`, `login.microsoftonline.com`, `login.live.com`, `github.com`.
3. In `RestrictedAuthWebViewActivity.kt`:
   In `shouldOverrideUrlLoading` AND in `onCreate` targetUrl validation:
   Check if the URL is permitted:
   ```kotlin
   val approvedHosts = StudyWebsiteRegistry(this).getAllowedHosts()
   val isAllowed = StudyNavigationGuard.isNavPermitted(url, approvedHosts, telegramMode = false)
   if (!isAllowed) {
       Toast.makeText(this, "Focus Guard: এই ডোমেইনটি অনুমোদিত নয় ($url)", Toast.LENGTH_SHORT).show()
       return true // Cancel navigation
   }
   ```
4. In `FullscreenStudyBrowser.kt` and `ControlledStudyWebView.kt`:
   In `onCreateWindow` popup WebView:
   Check `shouldOverrideUrlLoading` in popup WebView:
   ```kotlin
   if (!StudyNavigationGuard.isNavPermitted(popupUrl, approvedHosts, isTelegramMode)) {
       Toast.makeText(ctx, "Navigation blocked: Unapproved domain", Toast.LENGTH_SHORT).show()
       return true
   }
   view?.loadUrl(popupUrl)
   ```

- [ ] **Step 4: Run tests to verify all pass**

Run:
```bash
export JAVA_HOME="/home/prof21/.android-toolchain/jdk-17" && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew testDebugUnitTest --tests com.focusguard.study.StudyNavigationGuardTest
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/focusguard/study/filter/StudyNavigationGuard.kt app/src/main/java/com/focusguard/study/ui/RestrictedAuthWebViewActivity.kt app/src/main/java/com/focusguard/study/ui/FullscreenStudyBrowser.kt app/src/main/java/com/focusguard/study/ui/ControlledStudyWebView.kt app/src/test/java/com/focusguard/study/StudyNavigationGuardTest.kt
git commit -m "fix(security): lock down in-app browser with zero youtube exceptions and strict domain allowlist"
```

---

### Task 6: Deployment & Verification on Device

**Files:**
- Build output: `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 1: Build debug APK**

Run:
```bash
export JAVA_HOME="/home/prof21/.android-toolchain/jdk-17" && export PATH="$JAVA_HOME/bin:$PATH" && ./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Install updated APK on connected device**

Run:
```bash
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
```
Expected: `Success`.

- [ ] **Step 3: Verify DPC status on device via adb shell**

Run:
```bash
adb shell dumpsys device_policy | grep "FocusGuardDeviceAdminReceiver"
```
Expected: Admin confirmed active.

- [ ] **Step 4: Verify widget registration on device**

Run:
```bash
adb shell dumpsys appwidget | grep "StudyResourcesWidgetProvider"
```
Expected: Provider registered and available in widget picker.

---

### Task 7: Deconflict Parental Control PIN and Focus Guard Reset

**Files:**
- Modify: `app/src/main/java/com/focusguard/dpc/DevicePolicyController.kt`
- Modify: `app/src/main/java/com/focusguard/accessibility/WhatsAppCommunityAccessibilityService.kt`
- Modify: `app/src/main/java/com/focusguard/ui/components/SecurityPinDialogs.kt`
- Modify: `app/src/test/java/com/focusguard/ParentalControlAccessBlockTest.kt`

- [x] **Step 1: Remove PIN requirement from Focus Guard Factory Reset**
  - Updated `FactoryResetConfirmDialog` in `SecurityPinDialogs.kt` to present a direct confirmation dialog without PIN input field.
  - Wipes device immediately upon user confirmation when reset feature is enabled.

- [x] **Step 2: Gate Parental Control Access & Settings behind Focus Guard PIN**
  - In `SecurityThreeDotMenu`, tapping 3-dot presents a DropdownMenu:
    - "প্যারেন্টাল কন্ট্রোল ও সিকিউরিটি" -> Prompts for Security PIN -> Opens Parental Control Management dialog.
    - "ফোন ফ্যাক্টরি রিসেট" -> Direct confirmation warning (NO PIN required).
    - "পিন পরিবর্তন" -> Change PIN dialog.

- [x] **Step 3: Enable Reset Feature Management via Parental Control**
  - Added `KEY_FACTORY_RESET_ENABLED`, `isFactoryResetFeatureEnabled()`, and `setFactoryResetFeatureEnabled(enabled: Boolean)` in `DevicePolicyController`.
  - Inside PIN-verified Parental Control Settings dialog, added toggle button:
    - "রিসেট ফিচার নিষ্ক্রিয় / সক্রিয় করুন" (Enable/Disable Reset Feature via Parental Control).
  - In `DevicePolicyController.enforceAllPolicies()` and `WhatsAppCommunityAccessibilityService.handleSettingsAccessibilityEvent()`:
    - When reset feature is enabled: `DISALLOW_FACTORY_RESET` is cleared and Settings reset is not intercepted, allowing Parental Control's native PIN/rules to govern reset without conflict.
    - When reset feature is disabled: DPC adds `DISALLOW_FACTORY_RESET` and Accessibility Service blocks Settings reset.

- [x] **Step 4: Verify with unit tests and assembleDebug**



