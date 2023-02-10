package com.example.logic_object_detection


import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.android.filament.RenderableManager
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.math.Vector3
import com.gorisse.thomas.lifecycle.lifecycleOwner
import dev.romainguy.kotlin.math.abs
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.ArSession
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.ar.arcore.rotation
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.collision.overlapTest
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.Node
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import java.lang.Math.abs


class CameraActivity : AppCompatActivity(R.layout.activity_camera) {
    lateinit var sceneView: ArSceneView
    private var isBackPressed:Boolean=false
    private var nodeList:ArrayList <Node> = arrayListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sceneView = findViewById(R.id.sceneView)
        val augmentedImagePath= hashMapOf<String,String>()
        augmentedImagePath.apply {
            put("and Image","and_logic_for_ar_core.jpg")
            put("or Image","or_logic.png")
            put("nand Image","nand_logic.JPG")
            put("nor Image","nor_logic.JPG")
            put("not Image","not_logic.JPG")
            put("xor Image","xor_logic.JPG")
        }
        sceneView.planeFindingMode = Config.PlaneFindingMode.DISABLED
        sceneView.configureSession { arSession, config ->
            setupAugmentedImageDatabase(config, arSession,augmentedImagePath)
        }
        sceneView.apply {
            onFrame = { frameTime ->
                if(!isBackPressed){
                    try {
                        val frame: ArFrame? = sceneView.currentFrame
                        if (frame != null) {
                            val updateAugmentedImages = frame.updatedAugmentedImages
                            if (updateAugmentedImages.isNotEmpty()) {
                                Log.e("JAMES", "in Loop")
                                for (augmentedImage in updateAugmentedImages) {
                                    when (augmentedImage.trackingState) {
                                        TrackingState.PAUSED -> {
                                            Log.e("JAMES", "${augmentedImage.name} TrackingState Pause")
                                        }
                                        TrackingState.TRACKING -> {
                                            if (augmentedImage.trackingMethod==AugmentedImage.TrackingMethod.FULL_TRACKING){
                                                Log.e("JAMES", "${augmentedImage.name} TrackingState Tracking")
                                                anchorToAddChild(augmentedImage.name,augmentedImage.centerPose)
                                            }
                                            else{
                                                Log.e("JAMES","${augmentedImage.name} in loop not full tracking")
                                                for (augmentedImageNode in nodeList){
                                                    if(augmentedImageNode.name==augmentedImage.name){
                                                        sceneView.removeChild(augmentedImageNode)
                                                        nodeList-=augmentedImageNode
                                                        augmentedImageNode.destroy()
                                                    }
                                                }

                                            }
                                        }
                                        TrackingState.STOPPED -> {
                                            Log.e("JAMES", "${augmentedImage.name} TrackingState Stop")
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: NotYetAvailableException) {
                        Log.e("JAMES", "frame is not ready")
                    }
                }
            }
        }
    }
    private fun setupAugmentedImageDatabase(config: Config,session: ArSession,pathMap:HashMap<String,String>):Boolean {
        val augmentedImageDatabase: AugmentedImageDatabase
        val assetManger=assets
        if(assetManger==null){
            Log.e("TAG", "Context is null, cannot intitialize image database.")
            return false
        }
        try {
            augmentedImageDatabase= AugmentedImageDatabase(session)
            for (fileName in pathMap.keys){
                augmentedImageDatabase.addImage(fileName,
                    BitmapFactory.decodeStream(pathMap.get(fileName)?.let { assets.open(it) }))
            }

        }catch (e: IOException){
            Log.e("JAMES","IO exception loading augmented image database.",e)
            return false
        }
        config.augmentedImageDatabase=augmentedImageDatabase
        return true
    }
    private fun anchorToAddChild(modelName:String,anchorPose: Pose){
        val modelFilePathMap= hashMapOf<String,String>()
        modelFilePathMap.apply {
            put("and Image","and_logic_with_table.glb")
            put("nand Image","nand_logic_with_table.glb")
            put("or Image","or_logic_with_table.glb")
            put("nor Image","nor_logic_with_table.glb")
            put("not Image","not_logic_with_table.glb")
            put("xor Image","xor_logic_with_table.glb")
        }
        val tx=anchorPose.tx()
        val ty=anchorPose.ty()
        val tz=anchorPose.tz()
        val testModelNode= ArModelNode(
            placementMode= PlacementMode.BEST_AVAILABLE,
            hitPosition = Position(tx,ty,tz),
            followHitPosition = false,
            instantAnchor = false
        ).apply {
            modelFilePathMap.get(modelName)?.let {
                loadModelGlbAsync(
                    context=this@CameraActivity,
                    lifecycle=lifecycle,
                    glbFileLocation = it,
                    scaleToUnits = 0.25f
                )
            }
            rotation= Rotation(x=90.0f)
            collisionShape= Box(Vector3(0.01f,0.01f,0.01f))
            name=modelName
        }
        val nodeLapTest: Deferred<Node?>
                =lifecycleScope.async{
            sceneView.overlapTest(testModelNode)
        }
        val modelNodeCheckIsOk:Deferred<Boolean>
        =lifecycleScope.async {
            for (node in sceneView.children){
                if(node is ArModelNode){
                    val dx=node.position.x-testModelNode.position.x
                    val dy=node.position.y-testModelNode.position.y
                    val distanceMeters = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    Log.e("JAMES",distanceMeters.toString())
                    if (distanceMeters<0.1){
                        return@async false
                    }
                }
            }
            true
        }
        val checkOutOfScreenModelNode:Deferred<Node?> =lifecycleScope.async {
            for (arNode in nodeList){
                if (arNode is ArModelNode){
                    val modelPosition=arNode.worldPosition
                    val screenPoint=sceneView.cameraNode.
                    worldToScreenPoint(Vector3(modelPosition.x,modelPosition.y,modelPosition.z))
                    if(screenPoint.x<0 || screenPoint.x>sceneView.width ||
                        screenPoint.y<0 || screenPoint.y>sceneView.height){
                        Log.e("JAMES","${arNode.name} is out of screen")
                        return@async arNode
                    }
                }
            }
            null
        }
        lifecycleScope.launch {
            if(!isBackPressed){
                val outOfScreenNode=checkOutOfScreenModelNode.await()
                if (outOfScreenNode!=null){
                    sceneView.removeChild(outOfScreenNode)
                    nodeList-=outOfScreenNode
                    outOfScreenNode.destroy()
                }
                val node=nodeLapTest.await()
                val checkNodeDistanceIsOk=modelNodeCheckIsOk.await()
                if(node==null && checkNodeDistanceIsOk){
                    sceneView.addChild(testModelNode)
                    nodeList.add(testModelNode)
                }
                Log.e("JAMES","nodeList:"+nodeList.toString())
            }
        }
    }
    override fun onPause() {
        super.onPause()
        sceneView.children.toList().forEach{
            removeArModelNode(it)
        }
        nodeList.toList().forEach {
            removeArModelNode(it)
        }
    }
    private fun removeArModelNode(node: Node) {
        sceneView.children-=node
        sceneView.removeChild(node)
        node.destroy()
    }
}