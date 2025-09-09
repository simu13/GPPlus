package com.example.gpplus.ui.theme


import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

// ----------------------------------------------------------
//                ANIMATED MIC WAVES
// ----------------------------------------------------------
@Composable
fun PulsingWaves() {
    val infinite = rememberInfiniteTransition(label = "pulse")

    val scale1 by infinite.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "s1"
    )

    val scale2 by infinite.animateFloat(
        initialValue = 1.15f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "s2"
    )

    Box(
        modifier = Modifier
            .size(140.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        CanvasRing(scale = scale2, alpha = 0.25f)
        CanvasRing(scale = scale1, alpha = 0.4f)
    }
}

@Composable
fun CanvasRing(scale: Float, alpha: Float) {
    val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        drawCircle(color = ringColor, style = Stroke(width = 8f))
    }
}
