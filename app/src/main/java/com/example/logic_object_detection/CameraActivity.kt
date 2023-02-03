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
                    targetBitmap =BitmapFactory.decodeStream(contentResolver.openInputStream(uri)).compress(1f).apply{
                        Log.e("CameraActivity", "完成的Bitmap是=>${this}")
                        Log.e("CameraActivity", "完成的Bitmap的大小是=>[${this.height},${this.width}]")
                        Log.e("CameraActivity", "完成的Bitmap的密度是=>[${this.density}]")
                    }
                }
            }
//        arFragment.arSceneView.scene.addOnUpdateListener(this)
    }

//    override fun onUpdate(frameTime: FrameTime?) {
//        val frame: Frame = arFragment.arSceneView.arFrame!!
//    }

    private fun Bitmap.compress(scale: Float): Bitmap {
        // Use matrix 改變圖片長、寬
        val matrix = Matrix()
        // 縮小的比例
        matrix.setScale(scale, scale)
        val after = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
//        this.recycle() //不可以回收，若回收的話會導致ArSceneActivity Destroy的時候失敗。
        return after
    }

}