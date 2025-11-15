package com.example.arcadia.presentation.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.arcadia.R
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface

@Composable
fun ProfileImagePicker(
    imageUrl: String? = null,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        // Profile Image Circle with placeholder icon
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5E6D3))
                .clickable { onImageClick() },
            contentAlignment = Alignment.Center
        ) {
            // Show placeholder person icon
            // In a real app, you would load the actual profile image here using Coil or similar
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile Picture",
                tint = Color(0xFFFF9966),
                modifier = Modifier.size(80.dp)
            )
        }
        
        // Camera Icon Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onImageClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.camera),
                contentDescription = "Change Photo",
                tint = Surface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

