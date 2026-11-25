package com.example.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.RectF
import com.example.data.local.FaceUserRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

sealed interface ReferenceLoadResult {
    data class Ready(
        val userName: String,
        val embedding: FloatArray,
    ) : ReferenceLoadResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Ready) return false
            return userName == other.userName && embedding.contentEquals(other.embedding)
        }

        override fun hashCode(): Int {
            return 31 * userName.hashCode() + embedding.contentHashCode()
        }
    }

    data object ModelMissing : ReferenceLoadResult
    data object ReferenceMissing : ReferenceLoadResult
    data class Error(val message: String) : ReferenceLoadResult
}

data class FrameRecognitionResult(
    val faces: List<DetectedFaceUi>,
    val frameWidth: Int,
    val frameHeight: Int,
    val primaryEmbedding: FloatArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FrameRecognitionResult) return false
        return faces == other.faces &&
            frameWidth == other.frameWidth &&
            frameHeight == other.frameHeight &&
            (primaryEmbedding?.contentEquals(other.primaryEmbedding) ?: (other.primaryEmbedding == null))
    }

    override fun hashCode(): Int {
        var result = faces.hashCode()
        result = 31 * result + frameWidth
        result = 31 * result + frameHeight
        result = 31 * result + (primaryEmbedding?.contentHashCode() ?: 0)
        return result
    }
}

class FaceRecognizer(private val context: Context) {

    companion object {
        const val MODEL_ASSET_NAME = "mobile_face_net.tflite"
        const val REFERENCE_ASSET_NAME = "my_face.jpg"
        const val DEFAULT_USER_NAME = "Muhammad aka"
        const val SIMILARITY_THRESHOLD = 0.72f
        private const val DEFAULT_INPUT_SIZE = 112
        private const val DEFAULT_EMBEDDING_SIZE = 192
    }

    private val appContext = context.applicationContext
    private val interpreterMutex = Mutex()
    private var interpreter: Interpreter? = null
    private var inputWidth: Int = DEFAULT_INPUT_SIZE
    private var inputHeight: Int = DEFAULT_INPUT_SIZE
    private var inputDataType: DataType = DataType.FLOAT32
    private var embeddingSize: Int = DEFAULT_EMBEDDING_SIZE

    private val referenceDetector: FaceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build(),
        )
    }

    private val liveDetector: FaceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build(),
        )
    }

    suspend fun initializeReference(repository: FaceUserRepository): ReferenceLoadResult {
        return withContext(Dispatchers.IO) {
            val loaded = ensureInterpreterLoaded()
            if (!loaded) {
                return@withContext ReferenceLoadResult.ModelMissing
            }

            val referenceBitmap = loadAssetBitmap(REFERENCE_ASSET_NAME)
                ?: return@withContext loadCachedUserOrMissing(repository)

            try {
                val faces = referenceDetector.process(InputImage.fromBitmap(referenceBitmap, 0)).await()
                val primaryFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                    ?: return@withContext loadCachedUserOrMissing(repository)

                val cropped = cropFaceSafely(referenceBitmap, primaryFace.boundingBox)
                    ?: return@withContext ReferenceLoadResult.ReferenceMissing

                val embedding = extractEmbedding(cropped)
                    ?: return@withContext ReferenceLoadResult.Error("Etalon embedding ajratib bo'lmadi.")

                repository.saveReferenceUser(DEFAULT_USER_NAME, embedding)
                ReferenceLoadResult.Ready(
                    userName = DEFAULT_USER_NAME,
                    embedding = embedding,
                )
            } catch (e: Exception) {
                loadCachedUserOrMissing(repository, e.localizedMessage)
            } finally {
                if (!referenceBitmap.isRecycled) {
                    referenceBitmap.recycle()
                }
            }
        }
    }

    private suspend fun loadCachedUserOrMissing(
        repository: FaceUserRepository,
        errorMessage: String? = null,
    ): ReferenceLoadResult {
        val cached = repository.getPrimaryUser()
        if (cached != null && cached.embedding.isNotEmpty()) {
            return ReferenceLoadResult.Ready(
                userName = cached.fullName,
                embedding = cached.embedding,
            )
        }
        return if (errorMessage != null) {
            ReferenceLoadResult.Error(errorMessage)
        } else {
            ReferenceLoadResult.ReferenceMissing
        }
    }

    suspend fun analyzeFrame(frameBitmap: Bitmap): FrameRecognitionResult {
        return withContext(Dispatchers.Default) {
            val faces = try {
                liveDetector.process(InputImage.fromBitmap(frameBitmap, 0)).await()
            } catch (_: Exception) {
                emptyList()
            }

            val uiFaces = faces.map { it.toUiModel() }
            val primaryFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }

            val embedding = if (primaryFace != null && interpreter != null) {
                val cropped = cropFaceSafely(frameBitmap, primaryFace.boundingBox)
                if (cropped != null) {
                    extractEmbedding(cropped)
                } else {
                    null
                }
            } else {
                null
            }

            FrameRecognitionResult(
                faces = uiFaces,
                frameWidth = frameBitmap.width,
                frameHeight = frameBitmap.height,
                primaryEmbedding = embedding,
            )
        }
    }

    fun cosineSimilarity(first: FloatArray, second: FloatArray): Float {
        if (first.isEmpty() || first.size != second.size) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in first.indices) {
            val a = first[i]
            val b = second[i]
            dot += a * b
            normA += a * a
            normB += b * b
        }
        val denom = sqrt(normA) * sqrt(normB)
        if (denom <= 1e-6f) return 0f
        return (dot / denom).coerceIn(-1f, 1f)
    }

    private suspend fun ensureInterpreterLoaded(): Boolean {
        if (interpreter != null) return true
        return interpreterMutex.withLock {
            if (interpreter != null) return@withLock true
            val modelBuffer = loadModelFile(MODEL_ASSET_NAME) ?: return@withLock false
            try {
                val options = Interpreter.Options().apply {
                    numThreads = 4
                }
                val tflite = Interpreter(modelBuffer, options)
                val inputTensor = tflite.getInputTensor(0)
                val inputShape = inputTensor.shape()
                if (inputShape.size >= 4) {
                    inputHeight = inputShape[1].takeIf { it > 0 } ?: DEFAULT_INPUT_SIZE
                    inputWidth = inputShape[2].takeIf { it > 0 } ?: DEFAULT_INPUT_SIZE
                }
                inputDataType = inputTensor.dataType()

                val outputShape = tflite.getOutputTensor(0).shape()
                embeddingSize = outputShape.lastOrNull()?.takeIf { it > 0 } ?: DEFAULT_EMBEDDING_SIZE
                interpreter = tflite
                true
            } catch (_: Throwable) {
                interpreter = null
                false
            }
        }
    }

    private suspend fun extractEmbedding(faceBitmap: Bitmap): FloatArray? {
        val tflite = interpreter ?: return null
        val resized = if (faceBitmap.width == inputWidth && faceBitmap.height == inputHeight) {
            faceBitmap
        } else {
            Bitmap.createScaledBitmap(faceBitmap, inputWidth, inputHeight, true)
        }

        val inputBuffer = bitmapToInputBuffer(resized)
        if (resized !== faceBitmap && !resized.isRecycled) {
            resized.recycle()
        }
        if (!faceBitmap.isRecycled) {
            faceBitmap.recycle()
        }

        return interpreterMutex.withLock {
            try {
                val output = Array(1) { FloatArray(embeddingSize) }
                tflite.run(inputBuffer, output)
                l2Normalize(output[0])
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun bitmapToInputBuffer(bitmap: Bitmap): ByteBuffer {
        val bytesPerChannel = if (inputDataType == DataType.UINT8) 1 else 4
        val buffer = ByteBuffer.allocateDirect(inputWidth * inputHeight * 3 * bytesPerChannel)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputWidth * inputHeight)
        bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            if (inputDataType == DataType.UINT8) {
                buffer.put(r.toByte())
                buffer.put(g.toByte())
                buffer.put(b.toByte())
            } else {
                buffer.putFloat((r - 127.5f) / 128.0f)
                buffer.putFloat((g - 127.5f) / 128.0f)
                buffer.putFloat((b - 127.5f) / 128.0f)
            }
        }
        buffer.rewind()
        return buffer
    }

    private fun l2Normalize(embedding: FloatArray): FloatArray {
        var sumSq = 0f
        for (value in embedding) {
            sumSq += value * value
        }
        val norm = sqrt(sumSq)
        if (norm <= 1e-6f) return embedding
        return FloatArray(embedding.size) { index -> embedding[index] / norm }
    }

    private fun cropFaceSafely(source: Bitmap, box: Rect): Bitmap? {
        if (source.width <= 0 || source.height <= 0) return null
        val marginX = (box.width() * 0.08f).roundToInt()
        val marginY = (box.height() * 0.08f).roundToInt()
        val left = max(0, box.left - marginX)
        val top = max(0, box.top - marginY)
        val right = min(source.width, box.right + marginX)
        val bottom = min(source.height, box.bottom + marginY)
        val width = right - left
        val height = bottom - top
        if (width <= 8 || height <= 8) return null
        return try {
            Bitmap.createBitmap(source, left, top, width, height)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun loadAssetBitmap(assetName: String): Bitmap? {
        return try {
            appContext.assets.open(assetName).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadModelFile(assetName: String): ByteBuffer? {
        return try {
            appContext.assets.openFd(assetName).use { afd ->
                FileInputStream(afd.fileDescriptor).use { fis ->
                    val channel = fis.channel
                    val mapped: MappedByteBuffer = channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        afd.startOffset,
                        afd.length,
                    )
                    mapped
                }
            }
        } catch (_: Exception) {
            try {
                val bytes = appContext.assets.open(assetName).use { it.readBytes() }
                if (bytes.isEmpty()) return null
                ByteBuffer.allocateDirect(bytes.size).apply {
                    order(ByteOrder.nativeOrder())
                    put(bytes)
                    rewind()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun Face.toUiModel(): DetectedFaceUi {
        return DetectedFaceUi(
            trackingId = trackingId,
            boundingBox = RectF(boundingBox),
            landmarks = allLandmarks.map { it.position },
            smilingProbability = smilingProbability,
            leftEyeOpenProbability = leftEyeOpenProbability,
            rightEyeOpenProbability = rightEyeOpenProbability,
            headEulerAngleY = headEulerAngleY,
            headEulerAngleZ = headEulerAngleZ,
        )
    }

    fun close() {
        try {
            referenceDetector.close()
        } catch (_: Exception) {
        }
        try {
            liveDetector.close()
        } catch (_: Exception) {
        }
        try {
            interpreter?.close()
            interpreter = null
        } catch (_: Exception) {
        }
    }
}
