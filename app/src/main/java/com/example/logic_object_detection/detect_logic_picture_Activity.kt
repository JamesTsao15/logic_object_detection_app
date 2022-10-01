package com.example.logic_object_detection

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
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
import org.opencv.imgproc.Imgproc


class detect_logic_picture_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityDetectLogicPictureBinding
    private lateinit var thread_decodeBitmap:Thread
    private lateinit var detectBitmap:Bitmap
    private lateinit var lineArrayList: ArrayList<Line>
    private val myLoaderCallback: BaseLoaderCallback =object : BaseLoaderCallback(this){
        override fun onManagerConnected(status: Int) {
            when(status){
                LoaderCallbackInterface.SUCCESS->{
                    Log.e("JAMES","Opencv is loaded")
                }
                else->{
                    super.onManagerConnected(status)
                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityDetectLogicPictureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.e("JAMES","detect_onCreate")
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
    }

    override fun onResume() {
        super.onResume()
        if(OpenCVLoader.initDebug()){
            Log.e("JAMES","Opencv initialization is done")
            myLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
            thread_decodeBitmap.join()
            val detectMat=bitmapToMat(detectBitmap)
            val detectDoneMat=detectHoughLinesP(detectMat)
            val detectionBitmap=drawLinedetection(detectBitmap)
            binding.imageViewLineDetect.setImageBitmap(detectionBitmap)
        }
        else{
            Log.e("JAMES","Opencv is not loaded. try again")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION,this,myLoaderCallback)
        }
    }
    private fun bitmapToMat(bitmap: Bitmap):Mat{
        val mat:Mat=Mat()
        val bmp32=bitmap.copy(Bitmap.Config.ARGB_8888, true)
        Utils.bitmapToMat(bmp32,mat)
        return mat
    }
    private fun detectHoughLinesP(mat:Mat):Mat{
        val mGray:Mat=Mat()
        val ret:Mat=Mat()
        val mEdge:Mat=Mat()
        val lines:Mat=Mat()
        Imgproc.cvtColor(mat,mGray,Imgproc.COLOR_RGB2GRAY)
        Imgproc.adaptiveThreshold(mGray,ret,255.0,Imgproc.ADAPTIVE_THRESH_MEAN_C,
            Imgproc.THRESH_BINARY_INV,55,10.0)
        ret.convertTo(mEdge,CvType.CV_8UC1)
        Imgproc.HoughLinesP(mEdge,lines,1.0,Math.PI/180.0,100,100.0,10.0)
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
        Log.e("JAMES","lineArrayList:")
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
            canvas.drawLine(startPoint.x.toFloat(),
                            startPoint.y.toFloat(),
                            endPoint.x.toFloat(),
                            endPoint.y.toFloat(),paint)
        }
        return tempBitmap
    }
}