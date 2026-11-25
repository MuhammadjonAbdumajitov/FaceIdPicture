package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.face.FaceRecognizer
import com.example.ui.theme.CoolMuted
import com.example.ui.theme.CyanScanner
import com.example.ui.theme.FacePulseTheme
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.OnCyanScanner
import com.example.ui.theme.SkyElectric
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SlateSurface
import kotlin.math.roundToInt

class HomeActivity : ComponentActivity() {

    companion object {
        const val EXTRA_USER_NAME = "extra_user_name"
        const val EXTRA_SIMILARITY = "extra_similarity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: FaceRecognizer.DEFAULT_USER_NAME
        val similarity = intent.getFloatExtra(EXTRA_SIMILARITY, 0.95f)

        setContent {
            FacePulseTheme {
                HomeWelcomeScreen(
                    userName = userName,
                    similarity = similarity,
                    onRescanClick = {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
fun HomeWelcomeScreen(
    userName: String,
    similarity: Float,
    onRescanClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        CyanScanner.copy(alpha = 0.14f),
                        ObsidianDeep,
                    ),
                ),
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            color = SlateSurface,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.5.dp, CyanScanner.copy(alpha = 0.5f)),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(CyanScanner.copy(alpha = 0.15f))
                        .border(2.dp, CyanScanner, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = stringResource(R.string.cd_verified_icon),
                        tint = CyanScanner,
                        modifier = Modifier.size(42.dp),
                    )
                }

                Surface(
                    color = SlateElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyanScanner.copy(alpha = 0.45f)),
                ) {
                    Text(
                        text = stringResource(R.string.home_verified_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanScanner,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }

                Text(
                    text = if (userName == FaceRecognizer.DEFAULT_USER_NAME) {
                        stringResource(R.string.home_welcome_title)
                    } else {
                        "Xush kelibsiz, $userName"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("home_welcome_text"),
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SlateElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.home_similarity_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoolMuted,
                            )
                            Text(
                                text = "${(similarity.coerceIn(0f, 1f) * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyanScanner,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.home_engine_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoolMuted,
                            )
                            Text(
                                text = stringResource(R.string.home_engine_value),
                                style = MaterialTheme.typography.labelMedium,
                                color = SkyElectric,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onRescanClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanScanner,
                        contentColor = OnCyanScanner,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("rescan_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_back_to_scan),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
