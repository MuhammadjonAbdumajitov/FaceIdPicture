package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FaceRetouchingNatural
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.face.CameraLens
import com.example.face.CameraPreviewWithAnalyzer
import com.example.face.FaceDetectionViewModel
import com.example.face.FaceDetectorUiState
import com.example.face.FaceOverlayCanvas
import com.example.face.FaceRecognizer
import com.example.face.RecognitionOutcome
import com.example.face.ReferenceStatus
import com.example.ui.theme.AmberSignal
import com.example.ui.theme.CoolMuted
import com.example.ui.theme.CoralStop
import com.example.ui.theme.CoralStopContainer
import com.example.ui.theme.CyanScanner
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.OnCoralStop
import com.example.ui.theme.OnCyanScanner
import com.example.ui.theme.SkyElectric
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SlateSurface
import kotlin.math.roundToInt

@Composable
fun FaceDetectionScreen(
    onVerified: (userName: String, similarity: Float) -> Unit,
    viewModel: FaceDetectionViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val rationale = shouldShowCameraRationale(context)
        viewModel.updatePermissionState(granted = granted, shouldShowRationale = rationale)
        if (granted) {
            viewModel.turnCameraOn()
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updatePermissionState(
            granted = granted,
            shouldShowRationale = shouldShowCameraRationale(context),
        )
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val outcome = uiState.recognitionOutcome
    LaunchedEffect(outcome) {
        if (outcome is RecognitionOutcome.Verified) {
            onVerified(outcome.userName, outcome.similarity)
        }
    }

    val onStartCameraClick = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.updatePermissionState(granted = true, shouldShowRationale = false)
            viewModel.turnCameraOn()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val onOpenSettingsClick = {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
        context.startActivity(intent)
    }

    if (outcome is RecognitionOutcome.NotRecognized) {
        NotRecognizedDialog(
            similarity = outcome.similarity,
            onRetry = viewModel::retryRecognition,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianDeep, Color(0xFF0C1424), ObsidianDeep),
                ),
            ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val isWideLayout = maxWidth >= 680.dp && maxWidth > maxHeight

            if (isWideLayout) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.35f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HeaderBar(uiState = uiState)
                        CameraStage(
                            uiState = uiState,
                            onFrameBitmap = viewModel::processCameraFrame,
                            onCameraError = viewModel::onCameraError,
                            onGrantPermission = onStartCameraClick,
                            onOpenSettings = onOpenSettingsClick,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AssetStatusBanner(
                            referenceStatus = uiState.referenceStatus,
                            onRetryInit = viewModel::loadReferenceEmbedding,
                        )
                        ErrorBanner(
                            message = uiState.errorMessage,
                            onDismiss = viewModel::clearError,
                        )
                        SimilarityPanel(
                            uiState = uiState,
                            modifier = Modifier.weight(1f),
                        )
                        ControlDock(
                            uiState = uiState,
                            onStartCamera = onStartCameraClick,
                            onStopCamera = viewModel::turnCameraOff,
                            onSwitchLens = viewModel::toggleCameraLens,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HeaderBar(uiState = uiState)
                    AssetStatusBanner(
                        referenceStatus = uiState.referenceStatus,
                        onRetryInit = viewModel::loadReferenceEmbedding,
                    )
                    ErrorBanner(
                        message = uiState.errorMessage,
                        onDismiss = viewModel::clearError,
                    )
                    CameraStage(
                        uiState = uiState,
                        onFrameBitmap = viewModel::processCameraFrame,
                        onCameraError = viewModel::onCameraError,
                        onGrantPermission = onStartCameraClick,
                        onOpenSettings = onOpenSettingsClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                    SimilarityPanel(uiState = uiState)
                    ControlDock(
                        uiState = uiState,
                        onStartCamera = onStartCameraClick,
                        onStopCamera = viewModel::turnCameraOff,
                        onSwitchLens = viewModel::toggleCameraLens,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotRecognizedDialog(
    similarity: Float,
    onRetry: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onRetry,
        containerColor = Color(0xFF2B1115),
        titleContentColor = OnCoralStop,
        textContentColor = Color(0xFFFFE4E6),
        iconContentColor = CoralStop,
        icon = {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_not_recognized_title),
                style = MaterialTheme.typography.headlineSmall,
                color = CoralStop,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.dialog_not_recognized_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${stringResource(R.string.similarity_label)}: ${(similarity.coerceAtLeast(0f) * 100).roundToInt()}% " +
                        "(talab: ${(FaceRecognizer.SIMILARITY_THRESHOLD * 100).roundToInt()}%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFCA5A5),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralStop,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("retry_recognition_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_retry))
            }
        },
    )
}

@Composable
private fun HeaderBar(uiState: FaceDetectorUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyanScanner.copy(alpha = 0.14f))
                    .border(1.dp, CyanScanner.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.FaceRetouchingNatural,
                    contentDescription = null,
                    tint = CyanScanner,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                StatusLine(uiState = uiState)
            }
        }

        val refText = when (uiState.referenceStatus) {
            is ReferenceStatus.Ready -> stringResource(R.string.reference_ready)
            ReferenceStatus.Loading -> stringResource(R.string.reference_loading)
            ReferenceStatus.ModelMissing -> "Model yo'q"
            ReferenceStatus.ReferenceMissing -> "Etalon yo'q"
            is ReferenceStatus.Error -> "Xato"
        }
        val refColor = when (uiState.referenceStatus) {
            is ReferenceStatus.Ready -> CyanScanner
            ReferenceStatus.Loading -> SkyElectric
            else -> AmberSignal
        }

        Surface(
            color = SlateElevated,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, refColor.copy(alpha = 0.45f)),
            modifier = Modifier.testTag("reference_status_badge"),
        ) {
            Text(
                text = refText,
                style = MaterialTheme.typography.labelSmall,
                color = refColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun StatusLine(uiState: FaceDetectorUiState) {
    val (label, dotColor) = when {
        !uiState.isCameraActive -> stringResource(R.string.status_camera_off) to CoolMuted
        uiState.referenceStatus == ReferenceStatus.ModelMissing -> stringResource(R.string.status_model_missing) to AmberSignal
        uiState.referenceStatus == ReferenceStatus.ReferenceMissing -> stringResource(R.string.status_reference_missing) to AmberSignal
        else -> stringResource(R.string.status_scanning) to CyanScanner
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = dotColor,
        )
    }
}

@Composable
private fun AssetStatusBanner(
    referenceStatus: ReferenceStatus,
    onRetryInit: () -> Unit,
) {
    val warningMessage = when (referenceStatus) {
        ReferenceStatus.ModelMissing -> stringResource(R.string.model_missing_hint)
        ReferenceStatus.ReferenceMissing -> stringResource(R.string.reference_missing_hint)
        is ReferenceStatus.Error -> referenceStatus.message
        else -> null
    }

    AnimatedVisibility(
        visible = warningMessage != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        if (warningMessage != null) {
            Surface(
                color = Color(0xFF2B2110),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AmberSignal.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = AmberSignal,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = warningMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRetryInit) {
                        Text(stringResource(R.string.btn_retry), color = AmberSignal)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraStage(
    uiState: FaceDetectorUiState,
    onFrameBitmap: (android.graphics.Bitmap) -> Unit,
    onCameraError: (String) -> Unit,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (uiState.isCameraActive && uiState.hasCameraPermission) {
        CyanScanner.copy(alpha = 0.55f)
    } else {
        SlateBorder
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(SlateSurface)
            .border(1.5.dp, borderColor, RoundedCornerShape(24.dp))
            .testTag("camera_stage"),
        contentAlignment = Alignment.Center,
    ) {
        if (uiState.isCameraActive) {
            if (uiState.hasCameraPermission) {
                CameraPreviewWithAnalyzer(
                    lens = uiState.lens,
                    onFrameBitmap = onFrameBitmap,
                    onCameraError = onCameraError,
                    modifier = Modifier.fillMaxSize(),
                )
                FaceOverlayCanvas(
                    faces = uiState.detectedFaces,
                    sourceImageWidth = uiState.frameWidth,
                    sourceImageHeight = uiState.frameHeight,
                    isFrontCamera = uiState.lens == CameraLens.FRONT,
                    similarity = uiState.lastSimilarity,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                PermissionRequiredCard(
                    shouldOpenSettings = !uiState.shouldShowPermissionRationale,
                    onGrantPermission = onGrantPermission,
                    onOpenSettings = onOpenSettings,
                )
            }
        } else {
            StandbyView()
        }
    }
}

@Composable
private fun StandbyView() {
    val infiniteTransition = rememberInfiniteTransition(label = "standby_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = CyanScanner.copy(alpha = 0.24f),
                    radius = size.minDimension * 0.42f * pulse,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Icon(
                imageVector = Icons.Rounded.VideocamOff,
                contentDescription = null,
                tint = CoolMuted,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.status_camera_off),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PermissionRequiredCard(
    shouldOpenSettings: Boolean,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Videocam,
            contentDescription = null,
            tint = AmberSignal,
            modifier = Modifier.size(44.dp),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.permission_needed_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.permission_needed_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = CoolMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = if (shouldOpenSettings) onOpenSettings else onGrantPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanScanner,
                contentColor = OnCyanScanner,
            ),
            modifier = Modifier.height(48.dp),
        ) {
            if (shouldOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_open_settings))
            } else {
                Text(stringResource(R.string.btn_grant_permission))
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String?,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(visible = !message.isNullOrBlank()) {
        if (!message.isNullOrBlank()) {
            Surface(
                color = CoralStopContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CoralStop.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnCoralStop,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss) {
                        Text("OK", color = OnCoralStop)
                    }
                }
            }
        }
    }
}

@Composable
private fun SimilarityPanel(
    uiState: FaceDetectorUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SlateSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SlateBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.similarity_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = CoolMuted,
                )
                Text(
                    text = uiState.lastSimilarity?.let {
                        "${(it.coerceAtLeast(0f) * 100).roundToInt()}%"
                    } ?: "--",
                    style = MaterialTheme.typography.titleLarge,
                    color = CyanScanner,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Aniqlangan yuzlar",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoolMuted,
                )
                Text(
                    text = uiState.detectedFaces.size.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = SkyElectric,
                )
            }
        }
    }
}

@Composable
private fun ControlDock(
    uiState: FaceDetectorUiState,
    onStartCamera: () -> Unit,
    onStopCamera: () -> Unit,
    onSwitchLens: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SlateSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SlateBorder),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onStartCamera,
                    enabled = !uiState.isCameraActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanScanner,
                        contentColor = OnCyanScanner,
                        disabledContainerColor = CyanScanner.copy(alpha = 0.22f),
                        disabledContentColor = CyanScanner,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .semantics(mergeDescendants = true) {}
                        .testTag("start_camera_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Videocam,
                        contentDescription = stringResource(R.string.cd_camera_on_icon),
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_camera_on),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Button(
                    onClick = onStopCamera,
                    enabled = uiState.isCameraActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralStop,
                        contentColor = Color.White,
                        disabledContainerColor = SlateElevated,
                        disabledContentColor = CoolMuted.copy(alpha = 0.55f),
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .semantics(mergeDescendants = true) {}
                        .testTag("stop_camera_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VideocamOff,
                        contentDescription = stringResource(R.string.cd_camera_off_icon),
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_camera_off),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            OutlinedButton(
                onClick = onSwitchLens,
                enabled = uiState.isCameraActive,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("switch_camera_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cameraswitch,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.lens == CameraLens.FRONT) {
                        "Orqa kameraga o'tish"
                    } else {
                        "Old kameraga o'tish"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun shouldShowCameraRationale(context: Context): Boolean {
    val activity = context as? Activity ?: return true
    return ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.CAMERA,
    )
}
