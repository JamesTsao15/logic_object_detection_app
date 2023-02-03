package com.example.logic_object_detection


import android.graphics.Bitmap
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
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.BaseArFragment
import com.gorisse.thomas.sceneform.position
import com.gorisse.thomas.sceneform.scene.await
import java.io.IOException


class CustomArFragment : BaseArFragment(), BaseArFragment.OnSessionConfigurationListener {
    private var loadDone: Boolean = false
    private var AND_model: Renderable? = null
    private var OR_model: Renderable? = null
    private var NOT_model: Renderable? = null
    private var NAND_model: Renderable? = null
    private var NOR_model: Renderable? = null
    private var XOR_model: Renderable? = null
    private var ARObjectArraylist: ArrayList<ARObject> = ArrayList()
    private val scene get() = arSceneView.scene
    private val session get() = arSceneView.session!!
    var targetBitmap: Bitmap? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        this.setOnSessionConfigurationListener(this)
        val view = super.onCreateView(inflater, container, savedInstanceState)
        arSceneView.scene.addOnUpdateListener { frameTime ->
//            val config = Config(arSceneView.session)
//            config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR)
//            config.setFocusMode(Config.FocusMode.AUTO)
//            config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL)
//            arSceneView.session?.configure(config)
        }

        lifecycleScope.launchWhenCreated {
            loadModels()
        }
        return view
    }

    // from Sceneform Sample
    override fun onSessionConfiguration(session: Session?, config: Config?) {
        // 關閉平面搜尋模式，節省資源
        config?.planeFindingMode = Config.PlaneFindingMode.DISABLED
        config?.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        setupAugmentedImageDatabase(config ?: return, session ?: return)
    }

    override fun onArUnavailableException(sessionException: UnavailableException?) {
        throw sessionException ?: return
    }

    private fun setupAugmentedImageDatabase(config: Config, session: Session): Boolean {
        val augmentedImageDatabase: AugmentedImageDatabase

        val assetManager = if (context != null) requireContext().assets else null
        if (assetManager == null) {
            Log.e("TAG", "Context is null, cannot intitialize image database.")
            return false
        }

        try {
            augmentedImageDatabase = AugmentedImageDatabase(session)
            augmentedImageDatabase.addImage("defaultImage", targetBitmap)

        } catch (e: IOException) {
            Log.e("TAG", "IO exception loading augmented image database.", e)
            return false
        }
        config.augmentedImageDatabase = augmentedImageDatabase
        return true
    }


    fun Vector3.add(others: Vector3) = Vector3.add(this, others)

    override fun onUpdate(frameTime: FrameTime?) {
        try {
            val frame: Frame = arSceneView.arFrame!!

            val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
            if (updatedAugmentedImages.isEmpty()) {
                return
            }
            val augmentedImageMap = hashMapOf<AugmentedImage, com.google.ar.sceneform.Node>()
            // recorro todas ellas comprobando su estado
            for (augmentedImage in updatedAugmentedImages) {
                when (augmentedImage.trackingState) {
                    TrackingState.PAUSED -> {
                        Toast.makeText(
                            this@CustomArFragment.requireContext(),
                            "Detected image with index " + augmentedImage.index,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    TrackingState.TRACKING -> {
                        if (!augmentedImageMap.containsKey(augmentedImage)) {
                            val modelNode = com.google.ar.sceneform.Node().apply {
                                localPosition = augmentedImage.centerPose.position.add(
                                    Vector3(
                                        0.11f,
                                        -0.15f,
                                        -0.5f
                                    )
                                ) // 3D模型的位置和
                                localRotation = Quaternion(Vector3(90f, 0f, 0f))
                                renderable = AND_model
                            }
                            scene.addChild(modelNode)
                            augmentedImageMap.put(augmentedImage, modelNode)
                        }
                    }
                    TrackingState.STOPPED -> {
                        augmentedImageMap.remove(augmentedImage)
                    }
                    else -> {

                    }
                }
            }

        } catch (e: NotYetAvailableException) {
            Log.e("JAMES", "frame is not ready")
        }
    }
    // from Sceneform Sample and Timmy Test

    override fun isArRequired() = true

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