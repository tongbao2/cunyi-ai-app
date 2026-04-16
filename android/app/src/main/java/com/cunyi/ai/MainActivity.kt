package com.cunyi.ai.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cunyi.ai.ui.theme.CunyiAITheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.cunyi.ai.model.LiteRTEngine

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var liteRTEngine: LiteRTEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CunyiAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        liteRTEngine.release()
    }
}
