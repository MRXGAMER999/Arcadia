package com.example.arcadia.util

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PhotoPicker {
    var openPhotoPicker by mutableStateOf(false)
        private set

    @Composable
    fun InitializePhotoPicker(
        onImageSelect: (Uri?) -> Unit
    ) {
        val pickMedia = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            onImageSelect(uri)
            openPhotoPicker = false
        }

        LaunchedEffect(openPhotoPicker) {
            if (openPhotoPicker) {
                pickMedia.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }
        }
    }

    fun open() {
        openPhotoPicker = true
    }
}


