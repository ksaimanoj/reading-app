package com.littlewords.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingTimerTest {
    @Test fun countsVisibleTimeButNotPauses() {
        var now = 100L
        val timer = ReadingTimer { now }

        timer.start(1)
        now = 1_100
        timer.stop(1)
        now = 10_000
        timer.start(1)
        now = 11_500

        assertEquals(2_500L, timer.elapsed(1))
    }

    @Test fun changingCardOrClearingResetsTheTimer() {
        var now = 100L
        val timer = ReadingTimer { now }

        timer.start(1)
        now = 900
        timer.start(2)
        assertNull(timer.elapsed(1))
        now = 1_200
        assertEquals(300L, timer.elapsed(2))
        timer.clear(2)
        assertNull(timer.elapsed(2))
    }

    @Test fun repeatedStartDoesNotRestartAnActiveCard() {
        var now = 100L
        val timer = ReadingTimer { now }

        timer.start(1)
        now = 300
        timer.start(1)
        now = 800

        assertEquals(700L, timer.elapsed(1))
    }
}
