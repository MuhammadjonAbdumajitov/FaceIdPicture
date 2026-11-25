package com.example.face

import android.graphics.PointF
import android.graphics.RectF

enum class CameraLens {
    FRONT,
    BACK,
}

sealed interface ReferenceStatus {
    data object Loading : ReferenceStatus
    data class Ready(val userName: String) : ReferenceStatus
    data object ModelMissing : ReferenceStatus
    data object ReferenceMissing : ReferenceStatus
    data class Error(val message: String) : ReferenceStatus
}

sealed interface RecognitionOutcome {
    data object Scanning : RecognitionOutcome
    data class Verified(val userName: String, val similarity: Float) : RecognitionOutcome
    data class NotRecognized(val similarity: Float) : RecognitionOutcome
}

data class DetectedFaceUi(
    val trackingId: Int?,
    val boundingBox: RectF,
    val landmarks: List<PointF>,
    val smilingProbability: Float?,
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    val headEulerAngleY: Float,
    val headEulerAngleZ: Float,
)

data class FaceDetectorUiState(
    val isCameraActive: Boolean = true,
    val lens: CameraLens = CameraLens.FRONT,
    val hasCameraPermission: Boolean = false,
    val shouldShowPermissionRationale: Boolean = false,
    val referenceStatus: ReferenceStatus = ReferenceStatus.Loading,
    val recognitionOutcome: RecognitionOutcome = RecognitionOutcome.Scanning,
    val detectedFaces: List<DetectedFaceUi> = emptyList(),
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
    val lastSimilarity: Float? = null,
    val errorMessage: String? = null,
)
