package com.nuvio.app.features.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal val SmokeVoidBackground = Color(0xFF07080B)
internal val SmokePaperColor = Color(0xFF0B0E14)
internal val SmokeUnderSheetBackColor = Color(0xFF121723)
internal val SmokeUnderSheetMidColor = Color(0xFF0E121C)
internal val SmokeBlueDeep = Color(0xFF1D4ED8)
internal val SmokeBluePrimary = Color(0xFF2563EB)
internal val SmokeBlueBright = Color(0xFF3B82F6)
internal val SmokeBlueGlow = Color(0xFF60A5FA)

@Composable
internal expect fun SmokeShaderSurface(
    time: Float,
    pointerNormalized: Offset,
    modifier: Modifier = Modifier,
)

/**
 * Stacked dark-paper card featuring live animated charcoal smoke with subtle strong-blue veins.
 * Pointer movement or touch gently stirs the smoke plumes without blocking vertical scrolling.
 */
@Composable
internal fun SmokePaperCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 28,
    content: @Composable BoxScope.() -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "smokeDrift")
    val driftPhase by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 280_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "smokeTime",
    )

    var pointerTarget by remember { mutableStateOf(Offset(0.68f, 0.42f)) }
    val pointerAnimated by animateOffsetAsState(
        targetValue = pointerTarget,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 42f),
        label = "smokePointer",
    )

    val ambientX = sin((driftPhase * 0.22f).toDouble()).toFloat() * 0.04f
    val ambientY = cos((driftPhase * 0.17f).toDouble()).toFloat() * 0.03f
    val effectivePointer = Offset(
        x = (pointerAnimated.x + ambientX).coerceIn(0.05f, 0.95f),
        y = (pointerAnimated.y + ambientY).coerceIn(0.05f, 0.95f),
    )

    val cardShape = RoundedCornerShape(cornerRadius.dp)

    Box(
        modifier = modifier
            .padding(6.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.lastOrNull()
                        if (change != null && size.width > 0 && size.height > 0) {
                            pointerTarget = Offset(
                                x = (change.position.x / size.width.toFloat()).coerceIn(0.05f, 0.95f),
                                y = (change.position.y / size.height.toFloat()).coerceIn(0.05f, 0.95f),
                            )
                        }
                    }
                }
            },
    ) {
        // Back loose sheet tilted slightly counter-clockwise
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = -1.4f }
                .background(SmokeUnderSheetBackColor, cardShape)
                .border(
                    width = 1.dp,
                    color = SmokeBluePrimary.copy(alpha = 0.16f),
                    shape = cardShape,
                ),
        )

        // Middle loose sheet tilted slightly clockwise
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = 0.9f }
                .shadow(
                    elevation = 14.dp,
                    shape = cardShape,
                    ambientColor = Color.Black,
                    spotColor = Color(0xFF0B1F4A),
                )
                .background(SmokeUnderSheetMidColor, cardShape)
                .border(
                    width = 1.dp,
                    color = SmokeBlueBright.copy(alpha = 0.18f),
                    shape = cardShape,
                ),
        )

        // Front obsidian smoke card
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = 24.dp,
                    shape = cardShape,
                    ambientColor = Color.Black,
                    spotColor = SmokeBlueDeep.copy(alpha = 0.45f),
                )
                .clip(cardShape)
                .background(SmokePaperColor)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SmokeBlueGlow.copy(alpha = 0.24f),
                            Color(0xFF1E293B).copy(alpha = 0.55f),
                            SmokeBlueDeep.copy(alpha = 0.22f),
                        ),
                    ),
                    shape = cardShape,
                ),
        ) {
            SmokeShaderSurface(
                time = driftPhase,
                pointerNormalized = effectivePointer,
                modifier = Modifier.matchParentSize(),
            )

            // Soft vignette scrim to preserve text legibility over luminous smoke ribbons
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xFF05070B).copy(alpha = 0.48f),
                            0.28f to Color.Transparent,
                            0.62f to Color.Transparent,
                            1f to Color(0xFF05070B).copy(alpha = 0.82f),
                        ),
                    ),
            )

            PaperGrainOverlay(
                alpha = 0.045f,
                modifier = Modifier.matchParentSize(),
            )

            content()
        }
    }
}

private data class GrainDot(
    val nx: Float,
    val ny: Float,
    val radius: Float,
    val isLight: Boolean,
    val alphaFactor: Float,
)

private val PaperGrainDots: List<GrainDot> by lazy {
    List(360) { idx ->
        val f = idx.toFloat()
        val nx = (((sin((f * 12.9898 + 78.233)) * 43758.5453) % 1.0) + 1.0).toFloat() % 1f
        val ny = (((sin((f * 93.9898 + 19.133)) * 28411.1237) % 1.0) + 1.0).toFloat() % 1f
        val seed = (((sin((f * 41.321 + 7.71)) * 15731.743) % 1.0) + 1.0).toFloat() % 1f
        GrainDot(
            nx = nx,
            ny = ny,
            radius = 0.7f + seed * 1.15f,
            isLight = seed > 0.42f,
            alphaFactor = 0.45f + seed * 0.55f,
        )
    }
}

@Composable
internal fun PaperGrainOverlay(
    alpha: Float = 0.04f,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        PaperGrainDots.forEach { dot ->
            drawCircle(
                color = if (dot.isLight) Color(0xFFCFD9EC) else Color.Black,
                radius = dot.radius,
                center = Offset(dot.nx * w, dot.ny * h),
                alpha = alpha * dot.alphaFactor,
            )
        }
    }
}

/**
 * Pure Compose Canvas smoke renderer used on iOS, Android API < 33, and headless tests.
 * Layered dark-charcoal billowing plumes + subtle strong cobalt/sapphire curl veins.
 */
@Composable
internal fun SmokeFallbackCanvas(
    time: Float,
    pointerNormalized: Offset,
    modifier: Modifier = Modifier,
) {
    val pathPrimary = remember { Path() }
    val pathSecondary = remember { Path() }
    val pathTertiary = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val t = time * 0.45f
        val px = (pointerNormalized.x - 0.5f) * w * 0.14f
        val py = (pointerNormalized.y - 0.5f) * h * 0.14f

        // Base dark night gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF07090E),
                    Color(0xFF0B1019),
                    Color(0xFF05070B),
                ),
            ),
        )

        // Subtle deep blue ambient floor glow near plume origin
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SmokeBlueDeep.copy(alpha = 0.24f),
                    Color(0xFF0B1D44).copy(alpha = 0.12f),
                    Color.Transparent,
                ),
                center = Offset(w * 0.22f + px * 0.5f, h * 0.78f + py * 0.5f),
                radius = maxOf(w, h) * 0.78f,
            ),
            center = Offset(w * 0.22f + px * 0.5f, h * 0.78f + py * 0.5f),
            radius = maxOf(w, h) * 0.78f,
        )

        // Billowing dark-charcoal/slate smoke clouds
        val plumes = listOf(
            PlumeSpec(0.24f, 0.74f, 0.54f, -22f, Color(0xFF1B2436), 0.52f, 0.0f),
            PlumeSpec(0.46f, 0.54f, 0.62f, 18f, Color(0xFF232E44), 0.46f, 1.3f),
            PlumeSpec(0.68f, 0.34f, 0.56f, -15f, Color(0xFF182030), 0.48f, 2.7f),
            PlumeSpec(0.35f, 0.38f, 0.46f, 31f, Color(0xFF26334D), 0.32f, 4.1f),
            PlumeSpec(0.76f, 0.68f, 0.50f, -34f, Color(0xFF121826), 0.54f, 5.2f),
        )

        plumes.forEach { plume ->
            val driftX = sin((t * 0.55f + plume.phase).toDouble()).toFloat() * w * 0.055f
            val driftY = cos((t * 0.42f + plume.phase * 1.2f).toDouble()).toFloat() * h * 0.055f
            val center = Offset(
                x = w * plume.nx + driftX + px * 0.45f,
                y = h * plume.ny + driftY + py * 0.45f,
            )
            val radius = maxOf(w, h) * plume.radiusFraction
            withTransform({
                rotate(plume.rotationDeg + sin((t * 0.2f + plume.phase).toDouble()).toFloat() * 8f, center)
                scale(1.35f, 0.78f, center)
            }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            plume.color.copy(alpha = plume.alpha),
                            plume.color.copy(alpha = plume.alpha * 0.45f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = radius,
                    ),
                    center = center,
                    radius = radius,
                )
            }
        }

        // Flowing blue smoke veins threading through the dark clouds
        drawSmokeVeinPath(
            path = pathPrimary,
            w = w,
            h = h,
            t = t,
            phase = 0f,
            eddy = Offset(px, py),
            yStart = 0.94f,
            yEnd = 0.08f,
        )
        drawVeinStrokes(
            path = pathPrimary,
            haloWidth = 34.dp.toPx(),
            midWidth = 13.dp.toPx(),
            coreWidth = 3.2.dp.toPx(),
            coreColor = SmokeBlueGlow,
            midColor = SmokeBluePrimary,
            haloColor = SmokeBlueDeep,
            size = Size(w, h),
        )

        drawSmokeVeinPath(
            path = pathSecondary,
            w = w,
            h = h,
            t = t * 0.85f,
            phase = (PI * 0.65).toFloat(),
            eddy = Offset(-px * 0.7f, py * 0.8f),
            yStart = 0.82f,
            yEnd = 0.16f,
        )
        drawVeinStrokes(
            path = pathSecondary,
            haloWidth = 26.dp.toPx(),
            midWidth = 9.dp.toPx(),
            coreWidth = 2.4.dp.toPx(),
            coreColor = SmokeBlueBright,
            midColor = SmokeBlueDeep,
            haloColor = Color(0xFF102A6B),
            size = Size(w, h),
        )

        drawSmokeVeinPath(
            path = pathTertiary,
            w = w,
            h = h,
            t = t * 1.1f,
            phase = (PI * 1.35).toFloat(),
            eddy = Offset(px * 0.5f, -py * 0.6f),
            yStart = 1.02f,
            yEnd = 0.32f,
        )
        drawVeinStrokes(
            path = pathTertiary,
            haloWidth = 20.dp.toPx(),
            midWidth = 7.dp.toPx(),
            coreWidth = 1.8.dp.toPx(),
            coreColor = Color(0xFF93C5FD),
            midColor = SmokeBluePrimary,
            haloColor = SmokeBlueDeep,
            size = Size(w, h),
        )
    }
}

private data class PlumeSpec(
    val nx: Float,
    val ny: Float,
    val radiusFraction: Float,
    val rotationDeg: Float,
    val color: Color,
    val alpha: Float,
    val phase: Float,
)

private fun drawSmokeVeinPath(
    path: Path,
    w: Float,
    h: Float,
    t: Float,
    phase: Float,
    eddy: Offset,
    yStart: Float,
    yEnd: Float,
) {
    path.reset()
    val sway1 = sin((t * 0.65f + phase).toDouble()).toFloat()
    val sway2 = cos((t * 0.52f + phase * 1.3f).toDouble()).toFloat()
    val sway3 = sin((t * 0.81f + phase * 0.7f).toDouble()).toFloat()

    path.moveTo(
        x = w * (0.06f + sway1 * 0.05f),
        y = h * yStart,
    )
    path.cubicTo(
        x1 = w * (0.36f + sway2 * 0.14f) + eddy.x * 0.8f,
        y1 = h * (0.78f + sway1 * 0.08f) + eddy.y * 0.6f,
        x2 = w * (0.14f - sway3 * 0.12f) + eddy.x * 0.9f,
        y2 = h * (0.52f + sway2 * 0.09f) + eddy.y * 0.8f,
        x3 = w * (0.48f + sway1 * 0.11f) + eddy.x * 0.6f,
        y3 = h * (0.46f - sway3 * 0.08f) + eddy.y * 0.7f,
    )
    path.cubicTo(
        x1 = w * (0.78f + sway3 * 0.13f) + eddy.x * 0.5f,
        y1 = h * (0.41f + sway1 * 0.09f) + eddy.y * 0.4f,
        x2 = w * (0.56f - sway2 * 0.12f),
        y2 = h * (0.22f - sway3 * 0.07f),
        x3 = w * (0.90f + sway2 * 0.06f),
        y3 = h * yEnd,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVeinStrokes(
    path: Path,
    haloWidth: Float,
    midWidth: Float,
    coreWidth: Float,
    coreColor: Color,
    midColor: Color,
    haloColor: Color,
    size: Size,
) {
    val start = Offset(0f, size.height)
    val end = Offset(size.width, 0f)

    drawPath(
        path = path,
        brush = Brush.linearGradient(
            0f to Color.Transparent,
            0.20f to haloColor.copy(alpha = 0.24f),
            0.58f to haloColor.copy(alpha = 0.30f),
            1f to Color.Transparent,
            start = start,
            end = end,
        ),
        style = Stroke(width = haloWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    drawPath(
        path = path,
        brush = Brush.linearGradient(
            0f to Color.Transparent,
            0.18f to midColor.copy(alpha = 0.48f),
            0.62f to midColor.copy(alpha = 0.58f),
            1f to Color.Transparent,
            start = start,
            end = end,
        ),
        style = Stroke(width = midWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    drawPath(
        path = path,
        brush = Brush.linearGradient(
            0f to Color.Transparent,
            0.22f to coreColor.copy(alpha = 0.78f),
            0.60f to coreColor.copy(alpha = 0.86f),
            1f to Color.Transparent,
            start = start,
            end = end,
        ),
        style = Stroke(width = coreWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
