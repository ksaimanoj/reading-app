package com.littlewords.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.littlewords.app.data.ConfigCodec
import com.littlewords.app.domain.Category
import com.littlewords.app.domain.PracticeConfig
import com.littlewords.app.domain.PracticeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigCodecTest {
    @Test fun legacyWeightsBecomeEnabledCategories() {
        val decoded = ConfigCodec.decode(
            """{"weights":[10,70,20,0,0],"letters":"abc","patterns":["short_a"]}""",
        )

        assertEquals(
            setOf(Category.TWO_REAL, Category.THREE_REAL, Category.THREE_SILLY),
            decoded.enabledCategories,
        )
        assertEquals("abc", decoded.letters)
        assertEquals(setOf("short_a"), decoded.patterns)
        assertEquals(PracticeMode.WORDS, decoded.mode)
    }

    @Test fun everyPositiveLegacyWeightEnablesItsCategory() {
        val decoded = ConfigCodec.decode(
            """{"weights":[0,1,0,999,2],"letters":"abc","patterns":["short_a"]}""",
        )

        assertEquals(setOf(Category.THREE_REAL, Category.FOUR_REAL, Category.FIVE_REAL), decoded.enabledCategories)
    }

    @Test fun newConfigRoundTripsWithoutWeights() {
        val value = PracticeConfig(
            enabledCategories = setOf(Category.FOUR_REAL, Category.FIVE_REAL),
            letters = "abcd",
            patterns = setOf("short_a", "blends"),
            mode = PracticeMode.SENTENCES,
        )

        val encoded = ConfigCodec.encode(value)

        assertEquals(value, ConfigCodec.decode(encoded))
        assertTrue("enabledCategories" in encoded)
        assertFalse("weights" in encoded)
        assertTrue("SENTENCES" in encoded)
    }
}
