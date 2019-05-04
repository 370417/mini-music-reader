package com.albertford.autoflip.editsheetactivity

import android.content.Intent
import android.support.test.runner.AndroidJUnit4
import android.support.test.rule.ActivityTestRule
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.onData
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.espresso.matcher.BoundedMatcher
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.view.View
import com.albertford.autoflip.R
import org.hamcrest.Description
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditSheetActivityTest {

    @Rule
    val activityRule = ActivityTestRule( EditSheetActivity::class.java, false, false)

    // we can test the placeholderadpater easily by passing in no extras in the intent
    // actually we can't because the activity will finsih
    @Test
    fun testPlaceholderAdapter() {
        val scenario = activityRule.launchActivity(Intent())
        onData(isDescendantOfA(withId(R.id.page_recycler))).check(matches(SizeMatcher()))
    }
}

class SizeMatcher : BoundedMatcher<View, View>(View::class.java) {
    override fun describeTo(description: Description?) {
        description?.appendText("with width and height > 0")
    }

    override fun matchesSafely(item: View?): Boolean {
        item ?: return false
        return item.width > 0 && item.height > 0
    }

}