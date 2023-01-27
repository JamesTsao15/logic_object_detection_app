package com.example.logic_object_detection


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toolbar
import androidx.appcompat.app.ActionBar
import com.example.logic_object_detection.databinding.ActivityCameraBinding
import com.google.ar.core.Frame
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Scene
import org.tensorflow.lite.InterpreterApi


class CameraActivity : AppCompatActivity(),Scene.OnUpdateListener {
    private lateinit var binding:ActivityCameraBinding
    private lateinit var arFragment:CustomArFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(binding.root)
        supportActionBar!!.hide()
        arFragment=supportFragmentManager.findFragmentById(R.id.ArFagment) as CustomArFragment
        arFragment.arSceneView.scene.addOnUpdateListener(this)
    }
    override fun onUpdate(frameTime: FrameTime?) {
        val frame: Frame=arFragment.arSceneView.arFrame!!
    }

}