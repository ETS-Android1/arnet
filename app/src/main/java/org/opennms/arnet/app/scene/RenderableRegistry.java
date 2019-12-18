package org.opennms.arnet.app.scene;

import android.content.Context;
import android.util.Log;

import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contains renderables
 */
public class RenderableRegistry {

    private final CompletableFuture<ModelRenderable> redBall;
    private final CompletableFuture<ModelRenderable> cube;
    private final CompletableFuture<Void> renderableFuture;

    public RenderableRegistry(Context context) {
        redBall = MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.RED))
                .thenApply(material -> ShapeFactory.makeCube(new Vector3(0.1f, 0.1f, 0.1f), new Vector3(0.0f, 0.0f, 0.0f), material));
        cube = MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.WHITE))
                .thenApply(
                        material -> ShapeFactory.makeCube(new Vector3(.01f, .01f, .01f),
                                Vector3.zero(), material));
        renderableFuture = CompletableFuture.allOf(redBall, cube);
    }

    public ModelRenderable getRedBall() {
        return redBall.getNow(null);
    }

    public ModelRenderable getCube() {
        return cube.getNow(null);
    }

    public CompletableFuture<Void> getFuture() {
        return renderableFuture;
    }

}
