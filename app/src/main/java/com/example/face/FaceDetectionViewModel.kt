package com.example.face

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.FaceUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class FaceDetectionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = FaceUserRepository(database.faceUserDao())
    private val faceRecognizer = FaceRecognizer(application)

    private val _uiState = MutableStateFlow(FaceDetectorUiState())
    val uiState: StateFlow<FaceDetectorUiState> = _uiState.asStateFlow()

    private var referenceEmbedding: FloatArray? = null
    private var referenceUserName: String = FaceRecognizer.DEFAULT_USER_NAME
    private val isProcessingFrame = AtomicBoolean(false)
    private var consecutiveMismatches = 0

    init {
        loadReferenceEmbedding()
    }

    fun loadReferenceEmbedding() {
        _uiState.update { it.copy(referenceStatus = ReferenceStatus.Loading) }
        viewModelScope.launch {
            when (val result = faceRecognizer.initializeReference(repository)) {
                is ReferenceLoadResult.Ready -> {
                    referenceEmbedding = result.embedding
                    referenceUserName = result.userName
                    _uiState.update {
                        it.copy(
                            referenceStatus = ReferenceStatus.Ready(result.userName),
                            errorMessage = null,
                        )
                    }
                }

                ReferenceLoadResult.ModelMissing -> {
                    referenceEmbedding = null
                    _uiState.update {
                        it.copy(referenceStatus = ReferenceStatus.ModelMissing)
                    }
                }

                ReferenceLoadResult.ReferenceMissing -> {
                    referenceEmbedding = null
                    _uiState.update {
                        it.copy(referenceStatus = ReferenceStatus.ReferenceMissing)
                    }
                }

                is ReferenceLoadResult.Error -> {
                    referenceEmbedding = null
                    _uiState.update {
                        it.copy(referenceStatus = ReferenceStatus.Error(result.message))
                    }
                }
            }
        }
    }

    fun updatePermissionState(granted: Boolean, shouldShowRationale: Boolean) {
        _uiState.update { state ->
            state.copy(
                hasCameraPermission = granted,
                shouldShowPermissionRationale = shouldShowRationale,
                errorMessage = if (granted) null else state.errorMessage,
            )
        }
    }

    fun turnCameraOn() {
        consecutiveMismatches = 0
        _uiState.update { state ->
            state.copy(
                isCameraActive = true,
                recognitionOutcome = RecognitionOutcome.Scanning,
                detectedFaces = emptyList(),
                lastSimilarity = null,
                errorMessage = null,
            )
        }
    }

    fun turnCameraOff() {
        consecutiveMismatches = 0
        _uiState.update { state ->
            state.copy(
                isCameraActive = false,
                detectedFaces = emptyList(),
                lastSimilarity = null,
                errorMessage = null,
            )
        }
    }

    fun toggleCameraLens() {
        consecutiveMismatches = 0
        _uiState.update { state ->
            val nextLens = if (state.lens == CameraLens.FRONT) CameraLens.BACK else CameraLens.FRONT
            state.copy(
                lens = nextLens,
                detectedFaces = emptyList(),
                lastSimilarity = null,
            )
        }
    }

    fun retryRecognition() {
        consecutiveMismatches = 0
        _uiState.update { state ->
            state.copy(
                isCameraActive = true,
                recognitionOutcome = RecognitionOutcome.Scanning,
                lastSimilarity = null,
            )
        }
    }

    fun processCameraFrame(bitmap: Bitmap) {
        val currentState = _uiState.value
        if (!currentState.isCameraActive) {
            if (!bitmap.isRecycled) bitmap.recycle()
            return
        }
        if (currentState.recognitionOutcome !is RecognitionOutcome.Scanning) {
            if (!bitmap.isRecycled) bitmap.recycle()
            return
        }
        if (!isProcessingFrame.compareAndSet(false, true)) {
            if (!bitmap.isRecycled) bitmap.recycle()
            return
        }

        viewModelScope.launch {
            try {
                val result = faceRecognizer.analyzeFrame(bitmap)
                val refEmbedding = referenceEmbedding
                val liveEmbedding = result.primaryEmbedding

                var nextOutcome: RecognitionOutcome = RecognitionOutcome.Scanning
                var similarityScore: Float? = null

                if (refEmbedding != null && liveEmbedding != null) {
                    val similarity = faceRecognizer.cosineSimilarity(refEmbedding, liveEmbedding)
                    similarityScore = similarity
                    if (similarity >= FaceRecognizer.SIMILARITY_THRESHOLD) {
                        consecutiveMismatches = 0
                        nextOutcome = RecognitionOutcome.Verified(
                            userName = referenceUserName,
                            similarity = similarity,
                        )
                    } else {
                        consecutiveMismatches += 1
                        if (consecutiveMismatches >= 2) {
                            nextOutcome = RecognitionOutcome.NotRecognized(similarity = similarity)
                        }
                    }
                } else if (result.faces.isEmpty()) {
                    consecutiveMismatches = 0
                }

                _uiState.update { state ->
                    if (!state.isCameraActive) return@update state
                    state.copy(
                        detectedFaces = result.faces,
                        frameWidth = result.frameWidth,
                        frameHeight = result.frameHeight,
                        lastSimilarity = similarityScore ?: state.lastSimilarity,
                        recognitionOutcome = if (state.recognitionOutcome is RecognitionOutcome.Scanning) {
                            nextOutcome
                        } else {
                            state.recognitionOutcome
                        },
                    )
                }
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
                isProcessingFrame.set(false)
            }
        }
    }

    fun onCameraError(message: String) {
        _uiState.update { state ->
            state.copy(
                isCameraActive = false,
                detectedFaces = emptyList(),
                errorMessage = message,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        faceRecognizer.close()
    }
}
