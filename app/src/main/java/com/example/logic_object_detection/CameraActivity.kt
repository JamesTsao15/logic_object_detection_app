package com.example.logic_object_detection

import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.view.*
import androidx.annotation.ColorRes
import com.example.logic_object_detection.databinding.ActivityCameraBinding
import org.opencv.android.*
import org.opencv.core.*
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class CameraActivity : AppCompatActivity(),CameraBridgeViewBase.CvCameraViewListener2 {
    private lateinit var mRgba:Mat
    private lateinit var mGray:Mat
    private lateinit var objectDetectionCameraView:CameraBridgeViewBase
    private lateinit var binding:ActivityCameraBinding
    private val myLoaderCallback:BaseLoaderCallback=object :BaseLoaderCallback(this){
        override fun onManagerConnected(status: Int) {
            when(status){
                LoaderCallbackInterface.SUCCESS->{
                    Log.e("JAMES","Opencv is loaded")
                    objectDetectionCameraView.enableView()
                }
                else->{
                    super.onManagerConnected(status)
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding=ActivityCameraBinding.inflate(layoutInflater)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(binding.root)
        val actionBar=supportActionBar
        if (actionBar != null) {
            actionBar.hide()
        }
        objectDetectionCameraView=findViewById(R.id.frame_Surface)
        objectDetectionCameraView.visibility=SurfaceView.VISIBLE
        objectDetectionCameraView.setCvCameraViewListener(this)

    }

    override fun onResume() {
        super.onResume()
        if(OpenCVLoader.initDebug()){
            Log.e("JAMES","Opencv initialization is done")
            myLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
        else{
            Log.e("JAMES","Opencv is not loaded. try again")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION,this,myLoaderCallback)
        }

    }

    override fun onPause() {
        super.onPause()
        objectDetectionCameraView.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        objectDetectionCameraView.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba=Mat(height,width,CvType.CV_8UC4)
        mGray=Mat(height,width,CvType.CV_8UC1)
    }

    override fun onCameraViewStopped() {
        mRgba.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        if (inputFrame != null) {
            mRgba=inputFrame.rgba()
            mGray=inputFrame.gray()
            val myBitmap=Bitmap.createBitmap(mRgba.cols(),mRgba.rows(),Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(mRgba,myBitmap)
            val mat=Mat()
            if (myBitmap != null) {
                val objectDetectBitmap=runObjectDetection(myBitmap)
                Utils.bitmapToMat(objectDetectBitmap,mat)
            }
            return mat
        }
        return Mat()
    }

    private fun runObjectDetection(bitmap: Bitmap):Bitmap{
        val image = TensorImage.fromBitmap(bitmap)
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(10)
            .setScoreThreshold(0.6f)
            .build()
        val detector = ObjectDetector.createFromFileAndOptions(
            this, // the application context
            "logic_detection_1.tflite", // must be same as the filename in assets folder
            options
        )
        val results = detector.detect(image)
        debugPrint(results)
        val drawingBitmap=drawDetection(bitmap,results)
        return drawingBitmap
    }

    private fun drawDetection(bitmap:Bitmap,results: List<Detection>):Bitmap {
        val tempBitmap=bitmap.copy(Bitmap.Config.ARGB_8888,true)
        for ((i, obj) in results.withIndex()) {
            val box=obj.boundingBox
            val canvas=Canvas(tempBitmap)
            val paint=Paint()
            paint.setColor(Color.RED)
            paint.style=Paint.Style.STROKE
            paint.strokeWidth=5.0f
            canvas.drawRect(box.left,box.top,box.right,box.bottom,paint)
            for ((j, category) in obj.categories.withIndex()) {
                val textPaint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.DEV_KERN_TEXT_FLAG)
                textPaint.textSize=50.0f
                textPaint.typeface=Typeface.DEFAULT_BOLD
                textPaint.setColor(Color.RED)
                canvas.drawText(category.label,box.left+50,box.top-50,textPaint)
            }
        }
        return tempBitmap
    }


    private fun debugPrint(results : List<Detection>) {
        for ((i, obj) in results.withIndex()) {
            val box = obj.boundingBox

            Log.e("JAMES", "Detected object: ${i} ")
            Log.e("JAMES", "  boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")

            for ((j, category) in obj.categories.withIndex()) {
                Log.e("JAMES", "    Label $j: ${category.label}")
                val confidence: Int = category.score.times(100).toInt()
                Log.e("JAMES", "    Confidence: ${confidence}%")
            }
        }
        Log.e("JAMES","==================")
    }
}