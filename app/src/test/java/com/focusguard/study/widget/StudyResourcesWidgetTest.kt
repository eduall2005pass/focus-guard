package com.focusguard.study.widget

import android.appwidget.AppWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyResourcesWidgetTest {

    @Test
    fun testWidgetProviderInheritance() {
        val provider: Any = StudyResourcesWidgetProvider()
        assertTrue(
            "StudyResourcesWidgetProvider must inherit from AppWidgetProvider",
            provider is AppWidgetProvider
        )
    }

    @Test
    fun testWidgetProviderInstantiation() {
        val provider = StudyResourcesWidgetProvider()
        assertNotNull(provider)
    }

    @Test
    fun testWidgetIntentConstants() {
        assertEquals("study_sites", StudyResourcesWidgetProvider.SCREEN_MODE_STUDY_SITES)
        assertEquals(1, StudyResourcesWidgetProvider.TAB_INDEX_STUDY)
        assertEquals("screen_mode", StudyResourcesWidgetProvider.EXTRA_SCREEN_MODE)
        assertEquals("tab_index", StudyResourcesWidgetProvider.EXTRA_TAB_INDEX)
    }
}
