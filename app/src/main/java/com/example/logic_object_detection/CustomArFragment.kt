package com.example.logic_object_detection


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private var AND_model: Renderable? = null
    private var OR_model: Renderable? = null
    private var NOT_model: Renderable? = null
    private var NAND_model: Renderable? = null
    private var NOR_model: Renderable? = null
    private var XOR_model: Renderable? = null
    private val scene get() = arSceneView.scene
    var targetBitmap: Bitmap? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        this.setOnSessionConfigurationListener(this)
        val view = super.onCreateView(inflater, container, savedInstanceState)
        arSceneView.scene.addOnUpdateListener { frameTime -> // 每幀畫面改變都會call這個方法，不應該於此設定初始內容。
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

    // from Sceneform Sample and Timmy Test
    override fun onSessionConfiguration(session: Session?, config: Config?) {
        // 關閉平面搜尋模式，節省資源
        config?.planeFindingMode = Config.PlaneFindingMode.DISABLED
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
//            augmentedImageDatabase.addImage("AndImage", targetBitmap) // 使用MainActivity頁選擇的Bitmap
            augmentedImageDatabase.addImage("AndImage", BitmapFactory.decodeStream(requireContext().assets.open("and_logic_for_ar_core.jpg"))) // 使用assets的圖片

        } catch (e: IOException) {
            Log.e("TAG", "IO exception loading augmented image database.", e)
            return false
        }
        config.augmentedImageDatabase = augmentedImageDatabase
        return true
    }

    fun Vector3.add(others: Vector3) = Vector3.add(this, others) // 個人比較喜歡extension的寫法

    val augmentedImageMap = hashMapOf<String, com.google.ar.sceneform.Node>() // 避免重複新增同一個圖片的Node。

    override fun onUpdate(frameTime: FrameTime?) { // 每幀畫面改變都會call這個方法。務必小心，不要增加裡面的code。
        try {
            val frame: Frame = arSceneView.arFrame!!

            val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
            if (updatedAugmentedImages.isEmpty()) {
                return
            }

            // 檢查所有狀態。
            for (augmentedImage in updatedAugmentedImages) {
                when (augmentedImage.trackingState) {
                    TrackingState.PAUSED -> {
                        tryToRemove3DModel(augmentedImage)
                    }
                    TrackingState.TRACKING -> {
                        if (!augmentedImageMap.containsKey(augmentedImage.name)) {
                            val modelNode = com.google.ar.sceneform.Node().apply {
                                localPosition = augmentedImage.centerPose.position.add(Vector3(0.11f, -0.15f, -0.5f)) // 3D模型的位置的原點和圖片有實際落差，於此修正。
                                localRotation = Quaternion(Vector3(90f, 0f, 0f))
                                renderable = AND_model
                            }
                            scene.addChild(modelNode)
                            augmentedImageMap[augmentedImage.name] = modelNode
                        }
                    }
                    TrackingState.STOPPED -> {
                        tryToRemove3DModel(augmentedImage)
                    }
                }
            }

        } catch (e: NotYetAvailableException) {
            Log.e("JAMES", "frame is not ready")
        }
    }

    private fun tryToRemove3DModel(augmentedImage: AugmentedImage) {
        if (augmentedImageMap.containsKey(augmentedImage.name)) {
            scene.removeChild(augmentedImageMap[augmentedImage.name])
            augmentedImageMap.remove(augmentedImage.name)
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