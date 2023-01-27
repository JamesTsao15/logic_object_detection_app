package com.example.logic_object_detection


import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.ux.ArFragment
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.ByteArrayOutputStream

class CustomArFragment: ArFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view=super.onCreateView(inflater, container, savedInstanceState)
        arSceneView.scene.addOnUpdateListener {
            frameTime->
            val config=Config(arSceneView.session)
            config.setFocusMode(Config.FocusMode.AUTO)
            arSceneView.session?.configure(config)
        }
        return view
    }

    override fun onUpdate(frameTime: FrameTime?) {
        try {
            val frame: Frame=arSceneView.arFrame!!
            val image=frame.acquireCameraImage()
            val bitmap=frameToBitmap(image)
            image.close()
            runOjectDetection(bitmap)
        }catch (e: NotYetAvailableException){
            Log.e("JAMES","frame is not ready")
        }
    }

    fun frameToBitmap(cameraImage: Image):Bitmap{
        //The camera image received is in YUV YCbCr Format. Get buffers for each of the planes and use them to create a new bytearray defined by the size of all three buffers combined
        val cameraPlaneY = cameraImage.planes[0].buffer
        val cameraPlaneU = cameraImage.planes[1].buffer
        val cameraPlaneV = cameraImage.planes[2].buffer

//Use the buffers to create a new byteArray that
        val compositeByteArray = ByteArray(cameraPlaneY.capacity() + cameraPlaneU.capacity() + cameraPlaneV.capacity())

        cameraPlaneY.get(compositeByteArray, 0, cameraPlaneY.capacity())
        cameraPlaneV.get(compositeByteArray, cameraPlaneY.capacity(), cameraPlaneV.capacity())
        cameraPlaneU.get(compositeByteArray, cameraPlaneY.capacity() + cameraPlaneV.capacity(), cameraPlaneU.capacity())

        val baOutputStream = ByteArrayOutputStream()
        val yuvImage: YuvImage = YuvImage(compositeByteArray, ImageFormat.NV21, cameraImage.width, cameraImage.height, null)
        yuvImage.compressToJpeg(Rect(0, 0, cameraImage.width, cameraImage.height), 75, baOutputStream)
        val byteForBitmap = baOutputStream.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(byteForBitmap, 0, byteForBitmap.size)
        if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            val matrix=Matrix()
            matrix.postRotate(90f)
            val rotatedBitmap=Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
            return rotatedBitmap
        }else{
            return  bitmap
        }

    }
    fun runOjectDetection(bitmap:Bitmap){
        val image=TensorImage.fromBitmap(bitmap)
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(10)
            .setScoreThreshold(0.6f)
            .build()
        val detector = ObjectDetector.createFromFileAndOptions(
            requireContext(), // the application context
            "logic_detection_1.tflite", // must be same as the filename in assets folder
            options
        )
        val results = detector.detect(image)
        Log.e("JAMES",results.toString())
    }
}