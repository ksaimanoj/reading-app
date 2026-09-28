package com.littlewords.app

import android.os.Bundle
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalConfiguration
import com.littlewords.app.ui.LittleWordsApp
import com.littlewords.app.ui.ReadingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as LittleWordsApplication).repository
        setContent {
            val model: ReadingViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = ReadingViewModel(repository) as T
            })
            val smallestWidth = LocalConfiguration.current.smallestScreenWidthDp
            LittleWordsApp(model, onReadingChanged = { reading ->
                // Tablets and resizable windows follow their available space.
                // Pause belongs to the reading route, so it never rotates the phone.
                val orientation = when {
                    smallestWidth >= 600 || isInMultiWindowMode -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    reading -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                if (requestedOrientation != orientation) requestedOrientation = orientation
            })
        }
    }
}
