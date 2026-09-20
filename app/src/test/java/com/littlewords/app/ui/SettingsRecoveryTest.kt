package com.littlewords.app.ui

import com.littlewords.app.domain.Category
import com.littlewords.app.domain.PracticeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRecoveryTest {
    @Test fun toggleCategoryRemovesAnEnabledGroup() {
        val original = PracticeConfig()

        val changed = toggleCategory(original, Category.THREE_SILLY)

        assertFalse(Category.THREE_SILLY in changed.enabledCategories)
        assertEquals(original.enabledCategories - Category.THREE_SILLY, changed.enabledCategories)
    }

    @Test fun toggleCategoryAddsADisabledGroupWithoutChangingOtherSettings() {
        val original = PracticeConfig(letters = "abc", patterns = setOf("short_a"))

        val changed = toggleCategory(original, Category.FOUR_REAL)

        assertTrue(Category.FOUR_REAL in changed.enabledCategories)
        assertEquals(original.letters, changed.letters)
        assertEquals(original.patterns, changed.patterns)
    }
}
