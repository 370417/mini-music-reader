package com.albertford.autoflip.editsheetactivity

import com.albertford.autoflip.room.Page
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditPageLogicTest {

    @Test
    fun calcTouchLocationForFloatsTest() {
        val page = Page(100, 100, 0L, 0)
        val logic = EditPageLogic(page, 0.09f, 0f)

        val tooLow = logic.calcTouchLocation(0.1f, 0.2f, 0.8f)
        val tooHigh = logic.calcTouchLocation(0.9f, 0.2f, 0.8f)
        val low1 = logic.calcTouchLocation(0.12f, 0.2f, 0.8f)
        val low2 = logic.calcTouchLocation(0.2f, 0.2f, 0.8f)
        val low3 = logic.calcTouchLocation(0.28f, 0.2f, 0.8f)
        val mid = logic.calcTouchLocation(0.3f, 0.2f, 0.8f)
        val high1 = logic.calcTouchLocation(0.72f, 0.2f, 0.8f)
        val high2 = logic.calcTouchLocation(0.8f, 0.2f, 0.8f)
        val high3 = logic.calcTouchLocation(0.88f, 0.2f, 0.8f)
        val highSlop1 = logic.calcTouchLocation(0.103f, 0.1f, 0.11f)
        val highSlop2 = logic.calcTouchLocation(0.106f, 0.1f, 0.11f)
        assertEquals(TouchLocation.OUTSIDE, tooLow)
        assertEquals(TouchLocation.OUTSIDE, tooHigh)
        assertEquals(TouchLocation.LOW_HANDLE, low1)
        assertEquals(TouchLocation.LOW_HANDLE, low2)
        assertEquals(TouchLocation.LOW_HANDLE, low3)
        assertEquals(TouchLocation.INSIDE, mid)
        assertEquals(TouchLocation.HIGH_HANDLE, high1)
        assertEquals(TouchLocation.HIGH_HANDLE, high2)
        assertEquals(TouchLocation.HIGH_HANDLE, high3)
        assertEquals(TouchLocation.LOW_HANDLE, highSlop1)
        assertEquals(TouchLocation.HIGH_HANDLE, highSlop2)
    }

}
