package com.example.logic_object_detection


import android.app.ProgressDialog
import android.graphics.*
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.logic_object_detection.databinding.ActivityDetectLogicPictureBinding
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector


class detect_logic_picture_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityDetectLogicPictureBinding
    private lateinit var thread_decodeBitmap:Thread
    private lateinit var detectBitmap:Bitmap
    private lateinit var lineArrayList: ArrayList<Line>
    private lateinit var progressDialog: ProgressDialog
    private var resizeTimes:Float=3.0f
    private val myLoaderCallback: BaseLoaderCallback =object : BaseLoaderCallback(this){
        override fun onManagerConnected(status: Int) {
            when(status){
                LoaderCallbackInterface.SUCCESS->{
                    Log.e("JAMES","Opencv is loaded")
                    sendMessage(2)
                }
                else->{
                    super.onManagerConnected(status)
                }
            }
        }
    }
    private val mHandler:Handler=object : Handler() {
        override fun handleMessage(msg: Message) {
            when(msg.what){
                0->{
                    progressDialog= ProgressDialog(this@detect_logic_picture_Activity).apply {
                        title="System"
                        setMessage("轉化電路中......")
                    }
                    progressDialog.show()
                    sendMessage(1)
                }
                1->{
                    Log.e("JAMES","dialog show")
                    if(OpenCVLoader.initDebug()){
                        Log.e("JAMES","Opencv initialization is done")
                        myLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
                        thread_decodeBitmap.join()
                        val objectDetectBitmap=runObjectDetection(detectBitmap.copy(Bitmap.Config.ARGB_8888,true))
                        val detectMat=bitmapToMat(detectBitmap)
                        val detectDoneMat=detectHoughLinesP(detectMat)
                        val detectionBitmap=drawLinedetection(objectDetectBitmap)
                        binding.imageViewLineDetect.setImageBitmap(detectionBitmap)
                    }
                    else{
                        Log.e("JAMES","Opencv is not loaded. try again")
                        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION,applicationContext,myLoaderCallback)
                    }
                }
                2->{
                    Log.e("JAMES","dialog Dismiss")
                    try {
                        progressDialog.dismiss()
                    }catch (e:UninitializedPropertyAccessException){
                        e.printStackTrace()
                    }

                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityDetectLogicPictureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sendMessage(0)
        Log.e("JAMES","detect_onCreate_start")
        lineArrayList= ArrayList()
        val intent=intent
        val pictureUriString=intent.getStringExtra("picture_uri")
        val pictrueUri=Uri.parse(pictureUriString)
        try {
            if (Build.VERSION.SDK_INT < 28) {
               detectBitmap = MediaStore.Images.Media.getBitmap(contentResolver, pictrueUri)
            } else {
                val source = ImageDecoder.createSource(contentResolver, pictrueUri)
                thread_decodeBitmap=Thread(Runnable {
                    detectBitmap = ImageDecoder.decodeBitmap(source)
                    runOnUiThread{
                        binding.imageViewDetectPicture.setImageBitmap(detectBitmap)
                    }
                })
                thread_decodeBitmap.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.e("JAMES","detect_onCreate_end")
    }

    override fun onResume() {
        super.onResume()
        Log.e("JAMES","detect_OnResume")
    }
    private fun sendMessage(cmd:Int){
        val msg:Message= Message()
        msg.what=cmd
        mHandler.sendMessage(msg)
    }
    private fun bitmapToMat(bitmap: Bitmap):Mat{
        val mat:Mat=Mat()
        val bmp32=bitmap.copy(Bitmap.Config.ARGB_8888, true)
        Utils.bitmapToMat(bmp32,mat)
        return mat
    }
    private fun detectHoughLinesP(mat:Mat):Mat{
        val mResize:Mat= Mat()
        val mGray:Mat=Mat()
        val ret:Mat=Mat()
        val mEdge:Mat=Mat()
        val lines:Mat=Mat()
        Imgproc.resize(mat,mResize,Size((mat.cols()/resizeTimes).toDouble(),(mat.rows()/resizeTimes).toDouble()))
        Imgproc.cvtColor(mResize,mGray,Imgproc.COLOR_RGB2GRAY)
        Imgproc.adaptiveThreshold(mGray,ret,255.0,Imgproc.ADAPTIVE_THRESH_MEAN_C,
            Imgproc.THRESH_BINARY_INV,55,10.0)
        ret.convertTo(mEdge,CvType.CV_8UC1)
        Imgproc.HoughLinesP(mEdge,lines,1.0,Math.PI/180.0,100,10.0,1.0)
        val out = Mat.zeros(mGray.size(), mGray.type())
        for (i in 0 until lines.rows()) {
            val data = IntArray(4)
            lines.get(i, 0, data)
            val pt1 = Point(data[0].toDouble(), data[1].toDouble())
            val pt2 = Point(data[2].toDouble(), data[3].toDouble())
            Imgproc.line(out, pt1, pt2, Scalar(255.0, 255.0, 255.0), 2, Imgproc.LINE_AA, 0)
            lineArrayList.add(Line(pt1,pt2))
        }
        lineArrayList.sortWith(compareBy<Line>{it.startPoint.x}
            .thenBy { it.startPoint.y}
            .thenBy {it.endPoint.x}
            .thenBy { it.endPoint.y})
        Log.e("JAMES","lineArrayList Size:${lineArrayList.size}")
        lineArrayList.forEach {
            Log.e("JAMES",it.toString())
        }
        return out
    }
    private fun matToBitmap(mat: Mat):Bitmap?{
        val bitmap:Bitmap=Bitmap.createBitmap(mat.width(),mat.height(),Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
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
                textPaint.typeface= Typeface.DEFAULT_BOLD
                textPaint.setColor(Color.RED)
                canvas.drawText(category.label,box.left+50,box.top-50,textPaint)
            }
        }

        return tempBitmap
    }
    private fun drawLinedetection(bitmap: Bitmap):Bitmap{
        val tempBitmap=bitmap.copy(Bitmap.Config.ARGB_8888,true)
        val canvas=Canvas(tempBitmap)
        val paint=Paint().apply {
            color=Color.BLUE
            strokeWidth=10f
        }
        for(i in 0 until lineArrayList.size){
            val startPoint=lineArrayList[i].startPoint
            val endPoint=lineArrayList[i].endPoint
            canvas.drawLine(startPoint.x.toFloat()*resizeTimes,
                            startPoint.y.toFloat()*resizeTimes,
                            endPoint.x.toFloat()*resizeTimes,
                            endPoint.y.toFloat()*resizeTimes,paint)
        }
        return tempBitmap
    }
}