package com.example.logic_object_detection


import android.content.res.Configuration
import android.graphics.*
import android.graphics.ImageFormat
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.*
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.collision.Plane
import com.google.ar.sceneform.collision.Ray
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.gorisse.thomas.sceneform.scene.await
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.*

class CustomArFragment: ArFragment(){
    private var loadDone:Boolean=false
    private var AND_model:Renderable?=null
    private var OR_model:Renderable?=null
    private var NOT_model:Renderable?=null
    private var NAND_model:Renderable?=null
    private var NOR_model:Renderable?=null
    private var XOR_model:Renderable?=null
    private var ARObjectArraylist:ArrayList<ARObject> = ArrayList()
    private val scene get() = arSceneView.scene
    private val session get() = arSceneView.session!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view=super.onCreateView(inflater, container, savedInstanceState)
        arSceneView.scene.addOnUpdateListener {
            frameTime->
            val config=Config(arSceneView.session)
            config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR)
            config.setFocusMode(Config.FocusMode.AUTO)
            config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL)
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
            if(frame.camera.trackingState==TrackingState.TRACKING){
                val image=frame.acquireCameraImage()
                val bitmap=frameToBitmap(image)
                val resizeBitmap=Bitmap.createScaledBitmap(bitmap,arSceneView.width,arSceneView.height,true)
                image.close()
                val objectInfo=runOjectDetection(resizeBitmap)
                Log.e("JAMES","bitmap:${resizeBitmap.width},${resizeBitmap.height}")
                Log.e("JAMES","arFragment:${arSceneView.width},${arSceneView.height}")
                //requireActivity().findViewById<ImageView>(R.id.imageView2).setImageBitmap(bitmap)
                createAnchor(objectInfo)
                removeAllARObject()
                drawARObject()
            }
            else{
                Toast.makeText(requireContext(),"檢測平面中...請試圖移動手機",Toast.LENGTH_SHORT).show()
            }
        }catch (e: NotYetAvailableException){
            Log.e("JAMES","frame is not ready")
        }
    }
    private fun drawARObject() {
        removeAllARObject()
        Log.e("JAMES","ARObject:"+ARObjectArraylist.toString())
        for(obj in ARObjectArraylist){
            val anchorNode:AnchorNode=AnchorNode(obj.anchor)
            val transformableNode=TransformableNode(this.transformationSystem)
            transformableNode.renderable=AND_model
            anchorNode.addChild(transformableNode)
            scene.addChild(anchorNode)
        }
    }

    private fun removeAllARObject() {
        Log.e("JAMES","removeAllARObject")
        try{
            for(i in 0 until scene.children.size){
                val node=scene.children.get(i)
                if (node is AnchorNode) {
                    scene.removeChild(node)
                }
            }
        }catch (e:java.lang.IndexOutOfBoundsException){
            Log.e("JAMES","Error in removeAllARObject")
        }

        Log.e("JAMES","removeAllARObject Done")
    }

    private fun createAnchor(results: MutableList<Detection>?) {
        ARObjectArraylist= ArrayList()
        val camera=scene.camera
//        val ray:Ray=camera.screenPointToRay(0f,0f)
//        val hitResult =scene.hitTest(ray,true)
//        val hitPoint:Vector3=hitResult.point
//        val quaternion=Quaternion.identity()
//        val pose=Pose(
//            floatArrayOf(hitPoint.x,hitPoint.y,hitPoint.z),
//            floatArrayOf(quaternion.x,quaternion.y,quaternion.z,quaternion.w)
//        )
//        val anchor=session.createAnchor(pose)
//        ARObjectArraylist.add(ARObject(label="test",anchor=anchor))
        if(!results!!.isEmpty()){
            for ((i,obj) in results.withIndex()){
                    val box=obj.boundingBox
                    val x=(box.left+box.right)/2
                    val y=(box.top+box.bottom)/2
                    val ray:Ray=camera.screenPointToRay(x,y)
                    val hitResult =scene.hitTest(ray,true)
                    val hitPoint:Vector3=hitResult.point
                    val quaternion=Quaternion.identity()
                    val pose=Pose(
                        floatArrayOf(hitPoint.x,hitPoint.y,hitPoint.z),
                        floatArrayOf(quaternion.x,quaternion.y,quaternion.z,quaternion.w)
                    )
                    val anchor=session.createAnchor(pose)
                    for ((j, category) in obj.categories.withIndex()){
                        val label=category.label
                        ARObjectArraylist.add(ARObject(label=label,anchor=anchor))
                    }
                }

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
        OR_model = ModelRenderable.builder()
            .setSource(context, Uri.parse("or_logic.glb"))
            .setIsFilamentGltf(true)
            .await()
        NOR_model = ModelRenderable.builder()
            .setSource(context, Uri.parse("nor_logic.glb"))
            .setIsFilamentGltf(true)
            .await()
        NAND_model = ModelRenderable.builder()
            .setSource(context, Uri.parse("nand_logic.glb"))
            .setIsFilamentGltf(true)
            .await()
        XOR_model = ModelRenderable.builder()
            .setSource(context, Uri.parse("xor_logic.glb"))
            .setIsFilamentGltf(true)
            .await()
        NOT_model = ModelRenderable.builder()
            .setSource(context, Uri.parse("not_logic.glb"))
            .setIsFilamentGltf(true)
            .await()
        Log.e("JAMES", "is load Done")
    }

}