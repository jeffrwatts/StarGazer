package com.jeffrwatts.stargazer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jeffrwatts.stargazer.ui.StarGazerApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appContainer = (application as StarGazerApplication).container
            setContent {
                StarGazerApp(appContainer)
            }
        }
    }
}
