package com.albertford.autoflip.activities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.albertford.autoflip.*
import com.albertford.autoflip.models.SheetPartition
import kotlinx.android.synthetic.main.activity_measure_sheet.*

private const val STATE_LAST_PAGE = "STATE_LAST_PAGE"
private const val STATE_SHEET = "STATE_SHEET"

class MeasureSheetActivity : AppCompatActivity() {

    private var leftButtonText = LeftButtonText.SAVE_STAFF
        set(value) {
            field = value
            left_button.text = resources.getText(value.id)
        }

    private var rightButtonText = RightButtonText.NEXT_PAGE
        set(value) {
            field = value
            right_button.text = resources.getText(value.id)
        }

    private var onLastPage = false

    private val leftButtonListener = View.OnClickListener {
        when (leftButtonText) {
            LeftButtonText.SAVE_STAFF -> {
                preview_image.saveSelection()
                leftButtonText = LeftButtonText.SAVE_BAR
                rightButtonText = RightButtonText.NEXT_STAFF
                right_button.isEnabled = false
            }
            LeftButtonText.SAVE_BAR -> {
                preview_image.saveSelection()
                right_button.isEnabled = true
            }
        }
    }

    private val rightButtonListener = View.OnClickListener {
        when (rightButtonText) {
            RightButtonText.NEXT_STAFF -> {
                preview_image.nextStaff()
                leftButtonText = LeftButtonText.SAVE_STAFF
                rightButtonText = if (onLastPage) RightButtonText.FINISH else RightButtonText.NEXT_PAGE
            }
            RightButtonText.FINISH -> {
//                writeToRealm(preview_image.sheetPartition)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            RightButtonText.NEXT_PAGE -> {
                onLastPage = preview_image.nextPage()
                if (onLastPage) {
                    rightButtonText = RightButtonText.FINISH
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measure_sheet)

        setPadding()

        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        } else {
            createState()
        }

        left_button.setOnClickListener(leftButtonListener)
        right_button.setOnClickListener(rightButtonListener)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putBoolean(STATE_LAST_PAGE, onLastPage)
        outState?.putParcelable(STATE_SHEET, preview_image.sheetPartition)

        super.onSaveInstanceState(outState)
    }

    private fun setPadding() {
        val isLandscape = resources.getBoolean(R.bool.is_landscape)
        val navBarHeight = if (isLandscape) 0 else getNavBarHeight()
        val statusBarHeight = getStatusBarHeight()

        button_panel.setPadding(0, 0, 0, navBarHeight)
        image_container.setPadding(0, statusBarHeight, 0, navBarHeight)
    }

    private fun createState() {
        val name = intent.getStringExtra(NAME_KEY)
        val uri = intent.getStringExtra(URI_KEY)
        val bpm = intent.getFloatExtra(BPM_KEY, 0f)
        val bpb = intent.getIntExtra(BPB_KEY, 0)
        preview_image.sheetPartition = SheetPartition(name, uri, bpm, bpb)
        loadPdf(uri)
    }

    private fun restoreState(savedInstanceState: Bundle) {
        val sheet = savedInstanceState.getParcelable<SheetPartition>(
                STATE_SHEET)
        preview_image.sheetPartition = sheet
        onLastPage = savedInstanceState.getBoolean(STATE_LAST_PAGE)
        loadPdf(sheet.uri)
        when (preview_image.sheetPartition?.selection) {
            is StaffSelection -> {
                leftButtonText = LeftButtonText.SAVE_STAFF
                rightButtonText = if (onLastPage) RightButtonText.FINISH else RightButtonText.NEXT_PAGE
            }
            is BarSelection -> {
                leftButtonText = LeftButtonText.SAVE_BAR
                rightButtonText = RightButtonText.NEXT_STAFF
                right_button.isEnabled = false
            }
            is BarLineSelection -> {
                leftButtonText = LeftButtonText.SAVE_BAR
                rightButtonText = RightButtonText.NEXT_STAFF
            }
        }
    }

    private fun getNavBarHeight(): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    private fun loadPdf(uri: String) {
        val renderer = PdfSheetRenderer(this, uri)
        preview_image.renderer = renderer
        onLastPage = renderer.getPageCount() == 1
        if (onLastPage) {
            rightButtonText = RightButtonText.FINISH
        }
        val sheet = preview_image.sheetPartition
        sheet ?: return
        val pageCount = sheet.pages.size
        preview_image.post {
            preview_image.renderPage(sheet, if (pageCount == 0) 0 else pageCount - 1)
        }
    }
}

private enum class LeftButtonText(val id: Int) {
    SAVE_BAR(R.string.save_bar),
    SAVE_STAFF(R.string.save_staff)
}

private enum class RightButtonText(val id: Int) {
    NEXT_STAFF(R.string.next_staff),
    NEXT_PAGE(R.string.next_page),
    FINISH(R.string.finish)
}
