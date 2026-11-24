package com.localentertainmentgames.homeinventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.localentertainmentgames.homeinventory.platform.AndroidAppPlatform
import com.localentertainmentgames.homeinventory.platform.AndroidRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val platform = AndroidAppPlatform(this)
        val repository = AndroidRepository.get(applicationContext)
        setContent { App(platform = platform, repository = repository) }
    }
}
