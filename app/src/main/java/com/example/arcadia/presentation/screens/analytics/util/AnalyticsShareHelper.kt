package com.example.arcadia.presentation.screens.analytics.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.arcadia.presentation.screens.analytics.AnalyticsContent
import com.example.arcadia.presentation.screens.analytics.AnalyticsState
import com.example.arcadia.ui.theme.ArcadiaTheme
import com.example.arcadia.ui.theme.Surface as SurfaceColor
import java.io.File
import java.io.FileOutputStream

/**
 * Helper object for sharing analytics as an image.
 * Extracts sharing logic from the composable to improve maintainability.
 */
object AnalyticsShareHelper {
    
    private const val SHARE_IMAGE_WIDTH_DP = 360
    private const val COMPOSITION_DELAY_MS = 500L
    
    /**
     * Shares the analytics state as an image.
     * 
     * @param context The context (must be an Activity)
     * @param state The analytics state to share
     */
    fun shareAnalytics(context: Context, state: AnalyticsState) {
        val activity = context as? Activity ?: return
        val root = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: return

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            alpha = 0f // Hide visually but keep enabled for drawing
            
            setContent {
                ArcadiaTheme {
                    Surface(
                        modifier = Modifier
                            .width(SHARE_IMAGE_WIDTH_DP.dp)
                            .wrapContentHeight(),
                        color = SurfaceColor
                    ) {
                        AnalyticsContent(state = state)
                    }
                }
            }
        }

        // Add to hierarchy to get Recomposer and Lifecycle
        root.addView(composeView)

        // Wait for composition and layout
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val bitmap = captureComposeViewAsBitmap(context, composeView)
                if (bitmap != null) {
                    val uri = saveBitmapToCache(context, bitmap)
                    if (uri != null) {
                        shareImageUri(context, uri)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                root.removeView(composeView)
            }
        }, COMPOSITION_DELAY_MS)
    }
    
    /**
     * Captures a ComposeView as a bitmap.
     */
    private fun captureComposeViewAsBitmap(context: Context, composeView: ComposeView): Bitmap? {
        val density = context.resources.displayMetrics.density
        val widthPx = (SHARE_IMAGE_WIDTH_DP * density).toInt()
        
        val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        
        composeView.measure(widthSpec, heightSpec)
        val heightPx = composeView.measuredHeight
        
        if (heightPx <= 0) return null
        
        composeView.layout(0, 0, widthPx, heightPx)
        
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        composeView.draw(canvas)
        
        return bitmap
    }
    
    /**
     * Saves a bitmap to the app's cache directory.
     */
    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val imagesDir = File(context.cacheDir, "images")
            imagesDir.mkdirs()
            val file = File(imagesDir, "gaming_dna_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Shares an image URI via Android share sheet.
     */
    private fun shareImageUri(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "My Gaming DNA on Arcadia! 🎮\n#ArcadiaApp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(intent, "Share Analytics")
        context.startActivity(chooser)
    }
}
