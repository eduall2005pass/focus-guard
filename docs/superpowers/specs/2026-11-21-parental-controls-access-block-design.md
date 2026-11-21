# Parental Control Access Block Design

## Intent & Overview
The user manually configures the device's Parental Controls (PIN `159333` and Web Content Filtering to block explicit sites) in Android / Samsung One UI Settings. Once setup is complete, the user needs to lock down access to Parental Controls so that nobody can tamper with or open the Parental Controls settings without the Focus Guard admin PIN.

When Parental Control access is blocked via Focus Guard's Device Policy Controller (DPC), attempting to open Parental Controls triggers Android OS's native policy enforcement: **"Action not allowed: The app is suspended by your admin"** (or similar system-level lockout).

The control to block or unblock this access is placed inside Focus Guard's 3-dot Security Menu (`SecurityThreeDotMenu`), guarded by the Focus Guard PIN.

## Goals
1. **DPC-Enforced Restriction**: Suspend `com.samsung.android.app.parentalcare` (and any related supervision packages) using `DevicePolicyManager.setPackagesSuspended()`.
2. **Native System Feedback**: When suspended, opening Parental Controls from Settings displays the system's "Action not allowed by your admin" dialog.
3. **Defense-in-Depth Protection**: `WhatsAppCommunityAccessibilityService` intercepts `SupervisionDashboardActivity` as a secondary safeguard if accessed directly.
4. **PIN-Protected Management in 3-Dot Menu**:
   - Tapping 3-dot menu opens options:
     - "প্যারেন্টাল কন্ট্রোল ও সিকিউরিটি" (gated by Focus Guard PIN).
     - "ফোন ফ্যাক্টরি রিসেট" (direct confirmation alert, zero PIN required in Focus Guard).
     - "পিন পরিবর্তন" (Change PIN).
   - In Parental Control settings (post-PIN):
     - Toggle Parental Control access (🔒 Blocked / 🔓 Allowed).
     - Toggle Factory Reset feature ("parental control দিয়ে reset feature on/off করা যাবে").
5. **No PIN Conflict for Factory Reset**:
   - Focus Guard never requires a PIN to factory reset the device.
   - When Reset Feature is enabled, DPC clears `DISALLOW_FACTORY_RESET` and Accessibility allows Settings reset so that Parental Control's own PIN and policies govern device reset without conflict.
   - When Reset Feature is disabled via Parental Control, DPC enforces `DISALLOW_FACTORY_RESET` and Accessibility blocks Settings reset.
6. **Persistence**: The blocked state and reset feature state persist across reboots and policy re-enforcement cycles.

## Non-Goals
- Automating the initial PIN entry into Samsung Parental Controls via ADB (the user explicitly opted to perform the initial manual configuration themselves).
- Disabling the user's ability to unlock Parental Controls in Focus Guard when authenticated with their Focus Guard admin PIN.

## Architecture & Components

```
Focus Guard UI (3-Dot Security Menu)
        │
        ▼ (PIN Verified)
SecurityThreeDotMenu -> Toggle Parental Controls Access
        │
        ▼
DevicePolicyController.setParentalControlAccessBlocked(blocked)
   ├── Updates SharedPreferences ("focusguard_parental_control_prefs")
   ├── Calls dpm.setPackagesSuspended(adminComponent, ["com.samsung.android.app.parentalcare"], blocked)
   └── Re-evaluates baseline/enforcement state
        │
        ▼
Accessibility Service (Secondary Shield)
WhatsAppCommunityAccessibilityService.handleSettingsAccessibilityEvent()
   └── Intercepts SupervisionDashboardActivity if blocked -> GLOBAL_ACTION_BACK
```

## Detailed Component Specifications

### 1. `DevicePolicyController`
- **Preferences Key**: `focusguard_parental_control_prefs`, `key_parental_control_blocked: Boolean`
- **Methods**:
  ```kotlin
  fun isParentalControlAccessBlocked(): Boolean
  fun setParentalControlAccessBlocked(blocked: Boolean): Boolean
  ```
- **Target Packages**:
  - `com.samsung.android.app.parentalcare`
  - `com.google.android.apps.kids.familylinkhelper` (if installed)
- **Policy Enforcement Integration**:
  In `enforceAllPolicies()`:
  - If `isParentalControlAccessBlocked()` is true, suspend the target packages.
  - If false, ensure they are un-suspended and un-hidden.

### 2. `SecurityThreeDotMenu` (`SecurityPinDialogs.kt`)
- When the security dialog opens (post-PIN verification):
  - Read `isParentalControlBlocked = remember { mutableStateOf(actualDpcController.isParentalControlAccessBlocked()) }`
  - Add a styled button/row:
    - Label: `if (isParentalControlBlocked) "প্যারেন্টাল কন্ট্রোল আনব্লক করুন (Unblock)" else "প্যারেন্টাল কন্ট্রোল ব্লক করুন (Block Access)"`
    - Icon: `if (isParentalControlBlocked) Icons.Default.Lock else Icons.Default.LockOpen`
    - Subtitle / Tag: `if (isParentalControlBlocked) "স্ট্যাটাস: ব্লকড (Action Not Allowed)" else "স্ট্যাটাস: সচল (Allowed)"`
    - On click:
      - Call `actualDpcController.setParentalControlAccessBlocked(!isParentalControlBlocked)`
      - Update UI state and display confirmation Toast.

### 3. `WhatsAppCommunityAccessibilityService`
- In `handleSettingsAccessibilityEvent(event: AccessibilityEvent)`:
  - Check `DevicePolicyController(applicationContext).isParentalControlAccessBlocked()`
  - If blocked, check if current active window is `SupervisionDashboardActivity` or contains `Parental controls` title from settings:
    - If detected, `performGlobalAction(GLOBAL_ACTION_BACK)`
    - Toast: `"Focus Guard: প্যারেন্টাল কন্ট্রোল অ্যাক্সেস পলিসি অনুযায়ী ব্লক করা রয়েছে।"`
