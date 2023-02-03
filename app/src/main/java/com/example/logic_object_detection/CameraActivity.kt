package com.example.logic_object_detection


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.logic_object_detection.databinding.ActivityCameraBinding


class CameraActivity : AppCompatActivity()/*, Scene.OnUpdateListener */{
    companion object{
        const val TARGET_IMAGE_URI ="TARGET_IMAGE_URI"
    }
    private lateinit var binding: ActivityCameraBinding
    private lateinit var arFragment: CustomArFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(binding.root)
        supportActionBar!!.hide()
        arFragment =
            (supportFragmentManager.findFragmentById(R.id.ArFagment) as CustomArFragment).apply {
                (intent.getParcelableExtra(TARGET_IMAGE_URI) as? Uri)?.let { uri ->
                    targetBitmap =BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                }
            }
//        arFragment.arSceneView.scene.addOnUpdateListener(this) // 如果用不到就刪除是美德。
    }

//    override fun onUpdate(frameTime: FrameTime?) {
//        val frame: Frame = arFragment.arSceneView.arFrame!!
//    }

}