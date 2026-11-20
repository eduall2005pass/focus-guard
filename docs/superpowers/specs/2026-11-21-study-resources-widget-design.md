# Study Resources Compact Home Screen Widget Design

## 1. Intent & The Why
Focus Guard provides a curated repository of approved study portals (BoiMate, Medical Study materials, Dhaka University services, GTC, and custom registered educational domains) inside `StudySitesScreen`.
Currently, reaching this screen requires unlocking the device, finding Focus Guard, opening the app, navigating to Profile or switching to the Security/Study tab.
The user wants a dedicated home screen widget accessible from the Android launcher's Widget Manager (Widget Picker), allowing 1-tap direct launch into the **Study Resources & Approved Sites** page.

## 2. Goals, Non-Goals, and Anti-Goals

### Goals
- Provide an Android `AppWidgetProvider` (`StudyResourcesWidgetProvider`) discoverable in the Android OS Home screen widget picker under Focus Guard.
- Render a compact, clean widget (Approach 1: Compact 1x1 / 2x1 cell) matching Focus Guard's Islamic/Emerald aesthetic (Sage green / Deep emerald container, school/book icon, and clear labels: "Study Resources" / "অধ্যয়ন রিসোর্স").
- When tapped on the home screen, immediately launch `MainActivity` directly into the Study Sites screen (`AppScreenMode.DEVICE_SECURITY`, `selectedTabIndex = 1`) without needing PIN or multi-step navigation.
- Handle both cold launches (app not in memory) and warm launches (`onNewIntent`).

### Non-Goals
- Complex interactive list views inside the widget displaying live web feeds or full scrollable databases (avoiding battery drain and launcher memory bloat).
- Editing or modifying approved website lists directly from the widget (all management happens inside the app).

### Anti-Goals
- Bypassing Focus Guard security: The widget only launches the uninhibited Study Sites directory. It does NOT bypass DPC policies, Knox controls, or PIN-protected security settings.

## 3. Constraints
- **Android AppWidget Architecture:** Must use standard Android RemoteViews and `AppWidgetProvider`. Jetpack Glance is unnecessary overhead for a compact shortcut widget and would add significant dependencies.
- **Theme Consistency:** Background colors must align with Focus Guard palette (`#0F241D` Deep Forest / `#2E7D32` Emerald / `#E8F5E9` Mint).
- **One UI Launcher Compatibility:** Samsung One UI home launcher expects valid `targetCellWidth` (2), `targetCellHeight` (1), `minWidth` (~110dp), `minHeight` (~50dp), and `widgetCategory="home_screen"`.

## 4. Approaches Considered
- **Approach 1: Compact Quick Shortcut Widget (1x1 or 2x1)** [CHOSEN BY USER]
  - Sleek, compact badge with icon and label that triggers a direct Intent to open Study Resources.
  - *Trade-off:* Minimal screen footprint, zero battery/CPU overhead, highly reliable across launchers.
- **Approach 2: Interactive 4x2 Grid Widget**
  - Displays top 4 sites as interactive buttons directly on the home screen.
  - *Trade-off:* Takes significant screen space and requires complex RemoteViews click collections. Rejected by user in favor of Compact Widget.

## 5. Architectural & Technical Implementation

### A. AppWidget XML Metadata (`app/src/main/res/xml/study_resources_widget_info.xml`)
- `minWidth="110dp"`, `minHeight="54dp"`
- `targetCellWidth="2"`, `targetCellHeight="1"`
- `updatePeriodMillis="0"` (static shortcut; no background battery drain)
- `initialLayout="@layout/widget_study_resources"`
- `resizeMode="horizontal|vertical"`
- `widgetCategory="home_screen"`
- `description="@string/study_resources_widget_desc"`

### B. Widget RemoteViews Layout (`app/src/main/res/layout/widget_study_resources.xml`)
- Root: `LinearLayout` or `RelativeLayout` with `@drawable/bg_study_widget` (rounded corners, dark emerald / sage green border).
- `ImageView` with school/book icon (`ic_study_widget` or `@drawable/ic_book_open`).
- `TextView` for title: "Study Resources" (`@string/study_resources_widget_title`).
- Subtitle: "Focus Guard" (`@string/study_resources_widget_subtitle`).

### C. AppWidgetProvider (`com.focusguard.study.widget.StudyResourcesWidgetProvider`)
- Implements `onUpdate`:
  - Iterates all `appWidgetIds`.
  - Constructs `Intent(context, MainActivity::class.java)`:
    - Flags: `Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP`
    - Extras: `putExtra("screen_mode", "study_sites")` and `putExtra("tab_index", 1)`
  - Creates `PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)`.
  - Binds click listener to root container.
  - Calls `appWidgetManager.updateAppWidget(widgetId, views)`.

### D. Intent Handling in `MainActivity.kt`
- Update `handleScreenModeIntent(intent)`:
  - Add case `"study_sites"`, `"study_resources"` -> sets `initialSecurityTabIndex = 1` and `currentScreenMode = AppScreenMode.DEVICE_SECURITY`.

### E. Manifest Declaration (`app/src/main/AndroidManifest.xml`)
- `<receiver android:name="com.focusguard.study.widget.StudyResourcesWidgetProvider" android:exported="true">`
  - `<intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE" /></intent-filter>`
  - `<meta-data android:name="android.appwidget.provider" android:resource="@xml/study_resources_widget_info" />`
- Ensure `MainActivity` supports opening with `exported="true"` or direct PendingIntent dispatch.
