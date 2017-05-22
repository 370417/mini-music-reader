package io.github.as_f.barpager

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

  val PICK_PHOTO_REQUEST = 1

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    pickPhoto()
  }

  fun pickPhoto() {
    val pickPhotoIntent = Intent(Intent.ACTION_GET_CONTENT)
    pickPhotoIntent.type = "image/*"
    if (pickPhotoIntent.resolveActivity(packageManager) != null) {
      startActivityForResult(pickPhotoIntent, PICK_PHOTO_REQUEST)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == PICK_PHOTO_REQUEST && resultCode == Activity.RESULT_OK) {
      if (data?.data != null) {
        Picasso.with(this)
            .load(data.data)
            .into(zoomImage)
      }
    }
  }
}
