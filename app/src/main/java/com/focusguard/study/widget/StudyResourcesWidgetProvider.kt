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

    companion object {
        const val EXTRA_SCREEN_MODE = "screen_mode"
        const val EXTRA_TAB_INDEX = "tab_index"
        const val SCREEN_MODE_STUDY_SITES = "study_sites"
        const val TAB_INDEX_STUDY = 1
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_SCREEN_MODE, SCREEN_MODE_STUDY_SITES)
                putExtra(EXTRA_TAB_INDEX, TAB_INDEX_STUDY)
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
