package org.opennms.arnet.app

import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.ux.ArFragment
import org.opennms.arnet.WebSocketConsumerService
import org.opennms.arnet.app.mock.MockConsumerService
import org.opennms.arnet.app.scene.NetworkNode
import org.opennms.arnet.app.scene.RenderableRegistry
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : AppCompatActivity() {

    lateinit var sceneView : SceneView
    lateinit var arFragment: ArFragment
    lateinit var fitToScanView: ImageView
    lateinit var renderables: RenderableRegistry

   // private val consumerService = WebSocketConsumerService().apply { start() }
    private val consumerService = MockConsumerService().apply { updateGraphOnBackgroundThread() }

    private val augmentedImageMap: MutableMap<AugmentedImage, NetworkNode> = HashMap()

    private val resetView = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment
        fitToScanView = findViewById<View>(R.id.image_view_fit_to_scan) as ImageView
        sceneView = findViewById(R.id.sceneView)

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent?): Boolean {
                Log.d(TAG, "Got double tap! Requesting a view reset.")
                resetView.set(true)
                return true
            }
        })
        arFragment.getArSceneView().setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }

        // Load our renderables (3D assets)
        renderables = RenderableRegistry(this)

        // Start listening for tracked images
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
    }

    override fun onPause() {
        super.onPause()
        sceneView.pause()
    }

    override fun onResume() {
        super.onResume()
        sceneView.resume()
    }

    override fun onDestroy() {
        consumerService.stop()
        super.onDestroy()
    }

    fun onUpdateFrame(frameTime: FrameTime) {
        // Grab the frame, or return if there is none
        val frame: Frame = arFragment.arSceneView.arFrame ?: return

        if (resetView.get()) {
            for ((_,network) in augmentedImageMap) {
                arFragment.arSceneView.scene.removeChild(network)
                network.destroy()
            }
            augmentedImageMap.clear()
            resetView.set(false)
        }

        val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
        for (augmentedImage in updatedAugmentedImages) {
            when (augmentedImage.trackingState) {
                TrackingState.PAUSED -> {
                    // pass
                }
                TrackingState.TRACKING -> {
                    fitToScanView.visibility = View.GONE
                    // Create a new anchor for newly found images.
                    if (!augmentedImageMap.containsKey(augmentedImage)) {
                        // Create the network
                        val node = NetworkNode(sceneView.scene, renderables, consumerService)
                        node.setImage(augmentedImage)
                        augmentedImageMap[augmentedImage] = node
                        arFragment.arSceneView.scene.addChild(node)
                    }
                }
                TrackingState.STOPPED -> {
                    val node = augmentedImageMap.remove(augmentedImage)
                    if (node != null) {
                        arFragment.arSceneView.scene.removeChild(node)
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = "MainActivity"
    }
}
