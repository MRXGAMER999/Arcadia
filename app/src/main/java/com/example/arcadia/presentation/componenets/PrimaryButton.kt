package com.example.arcadia.presentation.componenets

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.arcadia.ui.theme.ButtonDisabled
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.ButtonSecondary
import com.example.arcadia.ui.theme.FontSize
import com.example.arcadia.ui.theme.TextPrimary
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.util.Constants.ALPHA_DISABLED

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: Int? = null,
    enabled: Boolean = true,
    secondary: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(size = 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) ButtonSecondary else ButtonPrimary,
            contentColor = TextSecondary,
            disabledContainerColor = ButtonDisabled,
            disabledContentColor = TextPrimary.copy(ALPHA_DISABLED)
        ),
        contentPadding = PaddingValues(20.dp)
    ) {
        if(icon != null){
            Icon(
                modifier = Modifier.size(14.dp),
                painter = painterResource(icon),
                contentDescription = "Button icon"
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = FontSize.REGULAR,
            fontWeight = FontWeight.Medium
        )
    }
}