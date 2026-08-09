package com.rearch.launcher

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.rearch.launcher.ui.screens.HomeScreen
import com.rearch.launcher.ui.theme.ReArchTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on while launcher is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            ReArchTheme {
                HomeScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }

    override fun onBackPressed() {
        // Swallow back — home screen never goes "back"
    }
}
