package com.kino.screenrecorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.kino.screenrecorder.ui.navigation.KinoNavigation
import com.kino.screenrecorder.ui.theme.KinoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            KinoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KinoNavigation(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}