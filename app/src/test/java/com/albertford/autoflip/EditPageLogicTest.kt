package com.albertford.autoflip

import android.graphics.PointF
import android.graphics.RectF
import com.albertford.autoflip.editsheetactivity.EditPageLogic
import com.albertford.autoflip.editsheetactivity.InitialTouch
import com.albertford.autoflip.editsheetactivity.TouchLocation
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
        val logic = EditPageLogic(page, 0.09f)
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

    @Test
    fun calcTouchLocationForRectsTest() {
        val page = Page(100, 100, 0L, 0)
        val logic = EditPageLogic(page, 0.09f)
        val rect = RectF(0.0f, 0.2f, 0.4f, 0.6f)
        val outside = logic.calcTouchLocation(PointF(0f, 0f), rect)
        val inside = logic.calcTouchLocation(PointF(rect.centerX(), rect.centerY()), rect)
        val nw = logic.calcTouchLocation(PointF(0.0f, 0.2f), rect)
        val ne = logic.calcTouchLocation(PointF(0.4f, 0.2f), rect)
        val sw = logic.calcTouchLocation(PointF(0.0f, 0.6f), rect)
        val se = logic.calcTouchLocation(PointF(0.4f, 0.6f), rect)
        val top = logic.calcTouchLocation(PointF(0.2f, 0.2f), rect)
        val bottom = logic.calcTouchLocation(PointF(0.2f, 0.6f), rect)
        val left = logic.calcTouchLocation(PointF(0.0f, 0.4f), rect)
        val right = logic.calcTouchLocation(PointF(0.4f, 0.4f), rect)
        assertEquals(null, outside)
        assertEquals(InitialTouch.CURRENT_SELECTION, inside)
        assertEquals(InitialTouch.RESIZE_NW, nw)
        assertEquals(InitialTouch.RESIZE_NE, ne)
        assertEquals(InitialTouch.RESIZE_SW, sw)
        assertEquals(InitialTouch.RESIZE_SE, se)
        assertEquals(InitialTouch.RESIZE_TOP, top)
        assertEquals(InitialTouch.RESIZE_BOTTOM, bottom)
        assertEquals(InitialTouch.RESIZE_LEFT, left)
        assertEquals(InitialTouch.RESIZE_RIGHT, right)
    }
}
