package com.example.logic_object_detection

import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import com.example.logic_object_detection.databinding.ActivityCameraBinding
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.util.Vector
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class CameraActivity : AppCompatActivity(),CameraBridgeViewBase.CvCameraViewListener2 {
    private lateinit var mRgba:Mat
    private lateinit var mGray:Mat
    private lateinit var mYuv:Mat
    private lateinit var objectDetectionCameraView:CameraBridgeViewBase
    private lateinit var binding:ActivityCameraBinding
    private lateinit var lineArrayList:ArrayList<Line>
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
        lineArrayList= ArrayList()
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
                detectHoughLinesP()
                val objectDetectBitmap=runObjectDetection(myBitmap)
                val lineDetectBitmap=drawLineDetection(objectDetectBitmap)
                Utils.bitmapToMat(lineDetectBitmap,mat)
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
//        debugPrint(results)
        val drawingBitmap=drawObjectDetection(bitmap,results)
        return drawingBitmap
    }

    private fun drawObjectDetection(bitmap:Bitmap,results: List<Detection>):Bitmap {

        val tempBitmap=bitmap.copy(Bitmap.Config.ARGB_8888,true)
        val canvas=Canvas(tempBitmap)
        for ((i, obj) in results.withIndex()) {
            val box=obj.boundingBox
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

    private fun detectHoughLinesP(){
        val ret:Mat=Mat()
        val mEdge:Mat=Mat()
        val lines:Mat=Mat()
        Imgproc.adaptiveThreshold(mGray,ret,255.0,Imgproc.ADAPTIVE_THRESH_MEAN_C,
            Imgproc.THRESH_BINARY_INV,55,10.0)
        ret.convertTo(mEdge,CvType.CV_8UC1)
        Imgproc.HoughLinesP(mEdge,lines,1.0,Math.PI/180.0,100,100.0,0.0)
        val out = Mat.zeros(mGray.size(), mGray.type())
        for (i in 0 until lines.rows()) {
            val data = IntArray(4)
            lines.get(i, 0, data)
            val pt1 = Point(data[0].toDouble(), data[1].toDouble())
            val pt2 = Point(data[2].toDouble(), data[3].toDouble())
            Imgproc.line(out, pt1, pt2, Scalar(255.0, 255.0, 255.0), 2, Imgproc.LINE_AA, 0)
            lineArrayList.add(Line(pt1,pt2))
        }
    }

    private fun drawLineDetection(bitmap: Bitmap):Bitmap{
        val tempBitmap=bitmap.copy(Bitmap.Config.ARGB_8888,true)
        val canvas=Canvas(tempBitmap)
        val paint=Paint().apply {
            color=Color.BLUE
            strokeWidth=10f
        }
        for(i in 0 until lineArrayList.size){
            val startPoint=lineArrayList[i].startPoint
            val endPoint=lineArrayList[i].endPoint
            canvas.drawLine(startPoint.x.toFloat(),
                startPoint.y.toFloat(),
                endPoint.x.toFloat(),
                endPoint.y.toFloat(),paint)
        }
        lineArrayList.clear()

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