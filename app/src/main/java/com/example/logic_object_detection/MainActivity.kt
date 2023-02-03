package com.example.logic_object_detection


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.logic_object_detection.databinding.ActivityMainBinding
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import org.opencv.android.Utils
import org.opencv.core.Mat
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream


class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding:ActivityMainBinding
    private lateinit var getImage:ActivityResultLauncher<String>
    private lateinit var mSession:Session
    var mUserRequestedInstall = true
    @RequiresApi(Build.VERSION_CODES.M)
    private val CAMERA_ACTION_CODE=100
    private lateinit var uri: Uri
    @SuppressLint("Recycle")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        maybeEnableArButton()
        val hasPermission:Boolean=checkCameraPermission(this)
        Log.e("JAMES","camera_permission:$hasPermission")
        activityMainBinding.imgViewShowPhoto.setImageResource(R.drawable.image)
        getImage=registerForActivityResult(ActivityResultContracts.GetContent(),
        ActivityResultCallback{
            try{
                uri=it
                try{
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    val inputStream: InputStream? =contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream,null,options)
                    activityMainBinding.imgViewShowPhoto.setImageURI(it)
                    activityMainBinding.btnNextStep.visibility= View.VISIBLE
                    Log.e("JAMES","inLoop")
                }catch (e:FileNotFoundException){
                    e.stackTrace
                    Toast.makeText(this,"找不到此檔案",Toast.LENGTH_SHORT).show()
                }
            }catch (e:java.lang.NullPointerException){
                e.stackTrace
                activityMainBinding.imgViewShowPhoto.setImageResource(R.drawable.image)
                activityMainBinding.btnNextStep.visibility= View.INVISIBLE
                Toast.makeText(this,"獲取圖片失敗",Toast.LENGTH_SHORT).show()
            }

        })

    }
    fun maybeEnableArButton() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            // Continue to query availability at 5Hz while compatibility is checked in the background.
            Handler().postDelayed({
                maybeEnableArButton()
            }, 200)
        }
        if (availability.isSupported) {
            activityMainBinding.btnTakePhoto.visibility = View.VISIBLE
            activityMainBinding.btnTakePhoto.isEnabled = true
        } else { // The device is unsupported or unknown.
            activityMainBinding.btnTakePhoto.visibility = View.INVISIBLE
            activityMainBinding.btnTakePhoto.isEnabled = false
        }
    }
    override fun onResume() {
        super.onResume()
        checkARCoreInstallStatus()
        activityMainBinding.btnTakePhoto.setOnClickListener{
            Log.e("JAMES","on_click_take_photo")
            val intent = Intent(this, CameraActivity::class.java).apply {
                putExtra(CameraActivity.TARGET_IMAGE_URI, uri)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
        activityMainBinding.btnPickPhoto.setOnClickListener {
            getImage.launch("image/*")
        }
        activityMainBinding.btnNextStep.setOnClickListener {
            val intent=Intent(this,detect_logic_picture_Activity::class.java)
            intent.putExtra("picture_uri",uri.toString())
            startActivity(intent)
        }

    }

    private fun checkARCoreInstallStatus() {
        try {
                when(ArCoreApk.getInstance().requestInstall(this,
                    mUserRequestedInstall)){
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        // Success: Safe to create the AR session.
                        mSession = Session(this)
                    }
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        mUserRequestedInstall = false
                        return
                    }
                }
        }catch (e: UnavailableUserDeclinedInstallationException){
            Log.e("JAMES","TODO: handle exception " + e)
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
