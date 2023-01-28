package com.example.logic_object_detection


import android.content.res.Configuration
import android.graphics.*
import android.graphics.ImageFormat
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.*
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.gorisse.thomas.sceneform.scene.await
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Function

class CustomArFragment: ArFragment() {
    private var loadDone:Boolean=false
    private var AND_model:Renderable?=null
    private var OR_model:Renderable?=null
    private var NOT_model:Renderable?=null
    private var NAND_model:Renderable?=null
    private var NOR_model:Renderable?=null
    private var XOR_model:Renderable?=null
    private val scene get() = arSceneView.scene
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
        lifecycleScope.launchWhenCreated {
            loadModels()
        }
        return view
    }

    override fun onUpdate(frameTime: FrameTime?) {
        try {
            val frame: Frame=arSceneView.arFrame!!
            val image=frame.acquireCameraImage()
            val bitmap=frameToBitmap(image)
            image.close()
            val objectInfo=runOjectDetection(bitmap)
        }catch (e: NotYetAvailableException){
            Log.e("JAMES","frame is not ready")
        }
        setOnTapArPlaneListener{
                hitResult,plane,motionEvent->
            Log.e("JAMES","inTapLoop")
            if(AND_model!=null){
                Log.e("JAMES","model is not null")
                val transformableNode=TransformableNode(this.transformationSystem)
                transformableNode.apply {
                    renderable=AND_model
                    renderableInstance.setCulling(false)
                    renderableInstance.animate(true).start()
                }
                scene.addChild(AnchorNode(hitResult.createAnchor()).apply {
                    addChild(transformableNode)
                })
            }
            else{
                Log.e("JAMES","model is null")
            }

        }
    }

    private fun drawARObject() {
        if (loadDone==false){
            Toast.makeText(requireContext(),"載入模型中．．．",Toast.LENGTH_SHORT).show()
            return
        }
       val transformableNode=TransformableNode(this.transformationSystem)
        transformableNode.localPosition= Vector3(10f,10f,10f)
        transformableNode.renderable=AND_model
        scene.addChild(transformableNode)
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
    fun runOjectDetection(bitmap:Bitmap): MutableList<Detection>? {
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
        return results
    }
    private suspend fun loadModels() {
        AND_model = ModelRenderable.builder()
            .setSource(context, Uri.parse("and_logic.glb"))
            .setIsFilamentGltf(true)
            .await()
        Log.e("JAMES", "is load Done")
    }
}