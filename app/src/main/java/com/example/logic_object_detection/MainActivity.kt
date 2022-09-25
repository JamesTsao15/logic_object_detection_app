package com.example.logic_object_detection


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.logic_object_detection.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding:ActivityMainBinding
    private lateinit var getImage:ActivityResultLauncher<String>
    @RequiresApi(Build.VERSION_CODES.M)
    private val CAMERA_ACTION_CODE=100
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        val hasPermission:Boolean=checkCameraPermission(this)
        Log.e("JAMES","camera_permission:$hasPermission")
        activityMainBinding.imgViewShowPhoto.setImageResource(R.drawable.image)
        getImage=registerForActivityResult(ActivityResultContracts.GetContent(),
        ActivityResultCallback{
            activityMainBinding.imgViewShowPhoto.setImageURI(it)
            activityMainBinding.btnNextStep.visibility= View.VISIBLE
        })
    }

    override fun onResume() {
        super.onResume()
        activityMainBinding.btnTakePhoto.setOnClickListener{
            Log.e("JAMES","on_click_take_photo")
            val intent=Intent(this,CameraActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
        activityMainBinding.btnPickPhoto.setOnClickListener {
            getImage.launch("image/*")
        }
    }
    private fun checkCameraPermission(activity:Activity):Boolean {
        val cameraPermissionCheck:Int = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
        val readPermissionCheck:Int = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        val  writePermissionCheck:Int = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (cameraPermissionCheck != PackageManager.PERMISSION_GRANTED
            || readPermissionCheck != PackageManager.PERMISSION_GRANTED
            || writePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            val permissions= arrayOf<String>(Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE
                ,Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(
                activity,
                permissions,
                0);
            return false;
        } else {
            return true;
        }
    }
}
