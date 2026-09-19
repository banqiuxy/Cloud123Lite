package com.banqiu.thirdparty123pan

import com.banqiu.thirdparty123pan.data.transfer.validateDownloadName
import com.banqiu.thirdparty123pan.data.transfer.validateContentRange
import com.banqiu.thirdparty123pan.ui.components.upstream.components.tabIndexAtPosition
import com.banqiu.thirdparty123pan.ui.components.upstream.components.toggleValueOnRelease
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class DownloadAndDockTest {
    @Test fun validNamesKeepUnicodeAndExtensions() {
        listOf("报告 2026.pdf", "movie.mp4", "archive.tar.gz").forEach(::validateDownloadName)
    }
    @Test fun namesCannotEscapeTheDirectory() {
        listOf("", " ", ".", "..", "../secret", "a/b", "a\\b", "x\u0000.txt", " a.txt", "a".repeat(241)).forEach {
            assertThrows("accepted: $it", IllegalArgumentException::class.java) { validateDownloadName(it) }
        }
    }
    @Test fun validRangesResumeAtExactOffset() {
        validateContentRange("bytes 0-99/100", 0, 100)
        validateContentRange("bytes 50-99/100", 50, 100)
        validateContentRange("bytes 0-99/*", 0, 0)
    }
    @Test fun invalidRangesNeverAppend() {
        listOf(null, "bad", "bytes 49-99/100", "bytes 50-100/100", "bytes 50-98/100", "bytes 50-99/101", "bytes 50-99/*").forEach {
            assertThrows(IOException::class.java) { validateContentRange(it, 50, 100) }
        }
    }
    @Test fun everyTabIsAnEntryPointAndEndsClamp() {
        for (i in 0..3) assertEquals(i, tabIndexAtPosition(4f + i * 50f + 25f, 4f, 50f, 4, true))
        assertEquals(0, tabIndexAtPosition(-100f, 4f, 50f, 4, true))
        assertEquals(3, tabIndexAtPosition(300f, 4f, 50f, 4, true))
    }
    @Test fun wholePanelInputMapsRtl() {
        for (i in 0..3) assertEquals(3 - i, tabIndexAtPosition(4f + i * 50f + 25f, 4f, 50f, 4, false))
    }
    @Test fun dockSlightOverflowKeepsTheNearestEndTab() {
        assertEquals(0, tabIndexAtPosition(3f, 4f, 50f, 4, true))
        assertEquals(3, tabIndexAtPosition(205f, 4f, 50f, 4, true))
        assertEquals(3, tabIndexAtPosition(3f, 4f, 50f, 4, false))
    }
    @Test fun switchTapAlwaysTogglesBothDirections() {
        assertTrue(toggleValueOnRelease(false, 0f, 8f, 0f))
        assertFalse(toggleValueOnRelease(true, 0f, 8f, 1f))
    }
    @Test fun switchFingerJitterIsStillATap() {
        assertTrue(toggleValueOnRelease(false, -2f, 8f, 0f))
        assertFalse(toggleValueOnRelease(true, 3f, 8f, 1f))
    }
    @Test fun switchDragCommitsItsSynchronousFraction() {
        assertTrue(toggleValueOnRelease(false, 20f, 8f, 1f))
        assertFalse(toggleValueOnRelease(true, -20f, 8f, 0f))
    }
}
