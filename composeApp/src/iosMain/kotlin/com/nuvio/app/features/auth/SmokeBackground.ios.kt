package com.nuvio.app.features.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

@Composable
internal actual fun SmokeShaderSurface(
    time: Float,
    pointerNormalized: Offset,
    modifier: Modifier,
) {
    SmokeFallbackCanvas(
        time = time,
        pointerNormalized = pointerNormalized,
        modifier = modifier,
    )
}
