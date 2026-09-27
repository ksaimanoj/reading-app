package com.littlewords.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.littlewords.app.data.ThemeMode
import com.littlewords.app.ui.LearningProgressBar
import com.littlewords.app.ui.LittleWordsTheme
import org.junit.Rule
import org.junit.Test

class LearningProgressBarTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsPercentageAndConfidentCountOverFullCollection() {
        compose.setContent { LittleWordsTheme(ThemeMode.LIGHT) { LearningProgressBar(7, 10) } }

        compose.onNodeWithText("70%").assertExists()
        compose.onNodeWithText("7 of 10 words confident").assertExists()
    }

    @Test fun emptyCollectionDisplaysZeroWithoutDividingByZero() {
        compose.setContent { LittleWordsTheme(ThemeMode.DARK) { LearningProgressBar(0, 0) } }

        compose.onNodeWithText("0%").assertExists()
        compose.onNodeWithText("0 of 0 words confident").assertExists()
    }
}
