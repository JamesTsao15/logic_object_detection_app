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
import java.util.concurrent.atomic.AtomicLongArray


class detect_logic_picture_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityDetectLogicPictureBinding
    private lateinit var thread_decodeBitmap:Thread
    private lateinit var detectBitmap:Bitmap
    private lateinit var lineArrayList: ArrayList<Line>
    private lateinit var mergeLineArrayList: ArrayList<Line>
    private lateinit var LongMergeLineArrayList: ArrayList<Line>
    private lateinit var ObjectDetectResultArrayList:ArrayList<Logic_Object>
    private lateinit var progressDialog: ProgressDialog
    private val min_degree_to_merge=10
    private val min_distance_to_merge=15
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
                    Log.e("JAMES","logicResult:"+ObjectDetectResultArrayList.toString())
                    for(i in 0 until ObjectDetectResultArrayList.size){
                        checkInput(ObjectDetectResultArrayList[i],LongMergeLineArrayList)
                    }
                    sendMessage(3)
                }
                3->{
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

    private fun checkInput(logicObject: Logic_Object,lines:ArrayList<Line>) {
        val rangeX=logicObject.box.left
        val rangeTop=logicObject.box.top
        val rangeBottom=logicObject.box.bottom
        var inputCounter=0
        for(line in lines){
            val endPoint_x=line.endPoint.x.toFloat()*resizeTimes
            val endPoint_y=line.endPoint.y.toFloat()*resizeTimes
            if(endPoint_y>rangeTop && endPoint_y<rangeBottom){
                if(Math.abs(endPoint_x-rangeX)<150.0)
                {
                    val orientation_i=Math.atan2(line.endPoint.y-line.startPoint.y,
                    line.endPoint.x-line.startPoint.x)
                    if(Math.abs(Math.toDegrees(orientation_i))<15.0){
                        inputCounter+=1
                    }
                }

            }
        }
        Log.e("JAMES","${logicObject.logic} Input:$inputCounter")
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityDetectLogicPictureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sendMessage(0)
        title="PhotoToCircuit"
        Log.e("JAMES","detect_onCreate_start")
        ObjectDetectResultArrayList= ArrayList()
        lineArrayList= ArrayList()
        mergeLineArrayList= ArrayList()
        LongMergeLineArrayList= ArrayList()
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
        mergeLineArrayList=processLines()
        return out
    }

    private fun processLines():ArrayList<Line> {
        val _lines_x= arrayListOf<Line>()
        val _lines_y= arrayListOf<Line>()
        val merge_line_all= arrayListOf<Line>()
        lineArrayList.forEach {
            val orientation_i=Math.atan2((it.endPoint.y-it.startPoint.y),(it.endPoint.x-it.startPoint.x))//計算夾角
            if((Math.abs(Math.toDegrees(orientation_i))>45) &&
                Math.abs(Math.toDegrees(orientation_i))<(90+45)){
                _lines_y.add(it)
            }
            else{
                _lines_x.add(it)
            }
        }
        _lines_x.sortWith(compareBy<Line>{it.startPoint.x})
        _lines_y.sortWith(compareBy<Line>{it.startPoint.y})
        val merge_lines_x=merged_lines_pipeline(_lines_x)
        val merge_lines_y=merged_lines_pipeline(_lines_y)
        merge_line_all.addAll(merge_lines_x)
        merge_line_all.addAll(merge_lines_y)
        Log.e("JAMES","merge_line_Arraylist size:"+merge_line_all.size)
        return merge_line_all
    }
    private fun merged_lines_pipeline(lines: ArrayList<Line>):ArrayList<Line>{
        val super_lines_final= arrayListOf<Line>()
        val super_lines= arrayListOf<ArrayList<Line>>()
        for(line in lines){
            var create_new_group=true
            var group_update=false
            if(!super_lines.isEmpty()){
                for (group in super_lines){
                    for(line2 in group){
                        if(get_distance(line2,line)<min_distance_to_merge){
                            val orientation_i=Math.atan2(line.endPoint.y-line.startPoint.y
                                ,line.endPoint.x-line.startPoint.x)
                            val orientation_j=Math.atan2(line2.endPoint.y-line2.startPoint.y
                                ,line2.endPoint.x-line2.startPoint.x)
                            if(Math.abs(Math.abs(Math.toDegrees(orientation_i))-
                                        Math.abs(Math.toDegrees(orientation_j)))
                                    .toInt()<min_degree_to_merge){
                                group.add(line)
                                create_new_group=false
                                group_update=true
                                break
                            }
                        }
                    }
                    if(group_update)break
                }
            }
            if(create_new_group){
                val new_group= arrayListOf<Line>()
                new_group.add(line)
                for(i in 0 until lines.size){
                    if(get_distance(lines[i],line)<min_distance_to_merge.toDouble()){
                        val orientation_i=Math.atan2(line.endPoint.y-line.startPoint.y,
                            line.endPoint.x-line.startPoint.x)
                        val orientation_j=Math.atan2(lines[i].endPoint.y-lines[i].startPoint.y,
                            lines[i].endPoint.x-lines[i].startPoint.x)
                        if(Math.abs(Math.abs(Math.toDegrees(orientation_i))-
                                    Math.abs(Math.toDegrees(orientation_j)))
                                .toInt()<min_degree_to_merge){
                            new_group.add(lines[i])
                        }
                    }
                }
                super_lines.add(new_group)
            }
        }
        for (group in super_lines){
            super_lines_final.add(merge_lines_segments1(group))
        }
        return super_lines_final
    }

    private fun merge_lines_segments1(lines: ArrayList<Line>): Line {
        if(lines.size==1){
            return lines[0]
        }
        val line_i=lines[0]
        val orientation_i=Math.atan2(line_i.endPoint.y-line_i.startPoint.y,line_i.endPoint.x-line_i.startPoint.x)
        val points= arrayListOf<Line>()
        lines.forEach {
            points.add(it)
        }
        if(Math.abs(Math.toDegrees(orientation_i))>45 &&
            Math.abs(Math.toDegrees(orientation_i))<(90+45)){
            points.sortWith(compareBy{it.startPoint.y})
        }
        else{
            points.sortWith(compareBy{it.startPoint.x})
        }
        return Line(points[0].startPoint,points[points.size-1].endPoint)
    }

    private fun get_distance(line1:Line,line2:Line):Double{
        val dist1=distancePointLine(line1.startPoint.x,line1.startPoint.y,
            line2.startPoint.x,line2.startPoint.y,line2.endPoint.x,line2.endPoint.y)
        val dist2=distancePointLine(line1.endPoint.x,line1.endPoint.y,
            line2.startPoint.x,line2.startPoint.y,line2.endPoint.x,line2.endPoint.y)
        val dist3=distancePointLine(line2.startPoint.x,line2.startPoint.y,
            line1.startPoint.x,line1.startPoint.y,line1.endPoint.x,line1.endPoint.y)
        val dist4=distancePointLine(line2.endPoint.x,line2.endPoint.y,
            line1.startPoint.x,line1.startPoint.y,line1.endPoint.x,line1.endPoint.y)
        val dists= listOf(dist1,dist2,dist3,dist4).sorted()
        return dists[0]
    }
    private fun lineMagnitude(x1: Double,y1:Double,x2:Double,y2: Double):Double{
        val lineMagnitude=Math.sqrt(Math.pow(x2-x1,2.0)+Math.pow(y2-y1,2.0))
        return lineMagnitude
    }
    private fun distancePointLine(px: Double, py: Double, x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val lineMag=lineMagnitude(x1,y1,x2,y2)
        var lineDistance=9999.0
        if(lineMag<0.00000001){
            lineDistance=9999.0
            return lineDistance
        }
        val u1=(((px - x1) * (x2 - x1)) + ((py - y1) * (y2 - y1)))
        val u = u1 / (lineMag * lineMag)
        if(u<0.00001 || u>1){
            val ix=lineMagnitude(px,py,x1,y1)
            val iy=lineMagnitude(px,py,x2,y2)
            if (ix>iy)lineDistance=iy
            else lineDistance=ix
        }
        else{
            val ix=x1+u*(x2-x1)
            val iy=y1+u*(y2-y1)
            lineDistance=lineMagnitude(px,py,ix,iy)
        }
        return lineDistance
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
                ObjectDetectResultArrayList.add(Logic_Object(category.label,box))
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
//        for(i in 0 until lineArrayList.size){
//            val startPoint=lineArrayList[i].startPoint
//            val endPoint=lineArrayList[i].endPoint
//            canvas.drawLine(startPoint.x.toFloat()*resizeTimes,
//                            startPoint.y.toFloat()*resizeTimes,
//                            endPoint.x.toFloat()*resizeTimes,
//                            endPoint.y.toFloat()*resizeTimes,paint)
//        }
        for(i in 0 until mergeLineArrayList.size){
            val startPoint=mergeLineArrayList[i].startPoint
            val endPoint=mergeLineArrayList[i].endPoint
            if(lineMagnitude(startPoint.x,startPoint.y,endPoint.x,endPoint.y)>50.0){
                LongMergeLineArrayList.add(Line(startPoint,endPoint))
                canvas.drawLine(startPoint.x.toFloat()*resizeTimes,
                    startPoint.y.toFloat()*resizeTimes,
                    endPoint.x.toFloat()*resizeTimes,
                    endPoint.y.toFloat()*resizeTimes,paint)
            }
        }
        Log.e("JAMES","longMergeLine size:${LongMergeLineArrayList.size}")
        return tempBitmap
    }
}