package com.example.face

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun CameraPreviewWithAnalyzer(
    lens: CameraLens,
    onFrameBitmap: (Bitmap) -> Unit,
    onCameraError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFrameBitmap by rememberUpdatedState(onFrameBitmap)
    val currentOnCameraError by rememberUpdatedState(onCameraError)

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner, lens, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var boundProvider: ProcessCameraProvider? = null

        cameraProviderFuture.addListener(
            {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    boundProvider = cameraProvider

                    val preview = Preview.Builder().build().apply {
                        surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .apply {
                            setAnalyzer(analysisExecutor) { imageProxy ->
                                val bitmap = imageProxyToUprightBitmap(imageProxy)
                                imageProxy.close()
                                if (bitmap != null) {
                                    currentOnFrameBitmap(bitmap)
                                }
                            }
                        }

                    val selector = when (lens) {
                        CameraLens.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                        CameraLens.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageAnalysis,
                    )
                } catch (e: Exception) {
                    currentOnCameraError(
                        e.localizedMessage ?: "Kamerani ishga tushirib bo'lmadi.",
                    )
                }
            },
            mainExecutor,
        )

        onDispose {
            try {
                boundProvider?.unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { previewView },
    )
}

private fun imageProxyToUprightBitmap(imageProxy: ImageProxy): Bitmap? {
    return try {
        val rawBitmap = imageProxy.toBitmap()
        val rotation = imageProxy.imageInfo.rotationDegrees
        if (rotation == 0) {
            rawBitmap
        } else {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            val rotated = Bitmap.createBitmap(
                rawBitmap,
                0,
                0,
                rawBitmap.width,
                rawBitmap.height,
                matrix,
                true,
            )
            if (rotated !== rawBitmap && !rawBitmap.isRecycled) {
                rawBitmap.recycle()
            }
            rotated
        }
    } catch (_: Exception) {
        null
    }
}
