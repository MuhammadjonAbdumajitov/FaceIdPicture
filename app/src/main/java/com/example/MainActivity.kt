package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.FaceDetectionScreen
import com.example.ui.theme.FacePulseTheme

class MainActivity : ComponentActivity() {

    private var hasNavigatedToHome = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            FacePulseTheme {
                FaceDetectionScreen(
                    onVerified = { userName, similarity ->
                        if (!hasNavigatedToHome) {
                            hasNavigatedToHome = true
                            val intent = Intent(this, HomeActivity::class.java).apply {
                                putExtra(HomeActivity.EXTRA_USER_NAME, userName)
                                putExtra(HomeActivity.EXTRA_SIMILARITY, similarity)
                            }
                            startActivity(intent)
                            finish()
                        }
                    },
                )
            }
        }
    }
}
