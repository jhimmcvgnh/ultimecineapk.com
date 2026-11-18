package com.nuvio.app.features.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import kotlin.math.PI
import kotlin.math.sin
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_auth_create_account
import nuvio.composeapp.generated.resources.compose_auth_email
import nuvio.composeapp.generated.resources.compose_auth_password
import nuvio.composeapp.generated.resources.compose_auth_sign_in
import nuvio.composeapp.generated.resources.compose_auth_sign_in_subtitle
import nuvio.composeapp.generated.resources.compose_auth_store_locally
import nuvio.composeapp.generated.resources.compose_auth_welcome_back
import org.jetbrains.compose.resources.stringResource

private val LoginBackdropDeep = Color(0xFF05070C)
private val LoginBackdropWash = Color(0xFF0B1633)
private val LoginCardGlass = Color(0xFF0A0F19).copy(alpha = 0.90f)
private val LoginFieldFocusedBg = Color(0xFF121A2B)
private val LoginNavyGlowDeep = Color(0xFF1E3A8A)

/**
 * Obsidian + cobalt-blue atmospheric backdrop with pulsing radial glows modeled after
 * `sign-in-card-2` while preserving the Cine Ultimate blue palette.
 */
@Composable
internal fun AnimatedLoginAtmosphere(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "loginAtmosphere")
    val topGlowPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "topGlowPulse",
    )
    val bottomGlowPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bottomGlowPulse",
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            drawRect(
                brush = Brush.verticalGradient(
                    0f to LoginBackdropWash.copy(alpha = 0.44f),
                    0.42f to LoginNavyGlowDeep.copy(alpha = 0.14f),
                    1f to LoginBackdropDeep.copy(alpha = 0.85f),
                ),
            )

            // Top radial cobalt aura
            val topRadius = maxOf(w, h) * (0.62f + topGlowPulse * 0.05f)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to AuthAccentBlue.copy(alpha = 0.14f + topGlowPulse * 0.10f),
                    0.5f to LoginNavyGlowDeep.copy(alpha = 0.09f),
                    1f to Color.Transparent,
                    center = Offset(w * 0.5f, h * 0.06f),
                    radius = topRadius,
                ),
                radius = topRadius,
                center = Offset(w * 0.5f, h * 0.06f),
            )

            // Bottom subtle sapphire counter-glow
            val bottomRadius = maxOf(w, h) * (0.68f + bottomGlowPulse * 0.07f)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to AuthAccentBlueBright.copy(alpha = 0.10f + bottomGlowPulse * 0.09f),
                    0.55f to LoginNavyGlowDeep.copy(alpha = 0.07f),
                    1f to Color.Transparent,
                    center = Offset(w * 0.5f, h * 0.96f),
                    radius = bottomRadius,
                ),
                radius = bottomRadius,
                center = Offset(w * 0.5f, h * 0.96f),
            )

            // Offset ambient glow spots
            val spotRadius = maxOf(w, h) * 0.36f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AuthAccentBlueSoft.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(w * 0.22f, h * 0.25f),
                    radius = spotRadius,
                ),
                radius = spotRadius,
                center = Offset(w * 0.22f, h * 0.25f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AuthAccentBlueSoft.copy(alpha = 0.045f), Color.Transparent),
                    center = Offset(w * 0.78f, h * 0.75f),
                    radius = spotRadius,
                ),
                radius = spotRadius,
                center = Offset(w * 0.78f, h * 0.75f),
            )
        }
        content()
    }
}

/**
 * Animated glassmorphism login/sign-up card inspired by `sign-in-card-2`:
 * 3D tilt on pointer move/drag, 4 traveling perimeter light beams, pulsing corner spots,
 * diagonal micro-grid texture, animated focus inputs, and a cobalt shimmer primary CTA.
 */
@Composable
internal fun AnimatedSignInCard(
    priceLabel: String,
    verifiedPaidEmail: String?,
    isSignUp: Boolean,
    email: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    authError: String?,
    metrics: AuthFormMetrics,
    scale: Float,
    headingStyle: TextStyle,
    subtitleStyle: TextStyle,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSubmit: () -> Unit,
    onToggleAuthMode: () -> Unit,
    onReturnToPaywall: () -> Unit,
    onEmailBoundsChange: (Rect) -> Unit,
    onPasswordBoundsChange: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    isTrialSignUp: Boolean = false,
    lockedTrialEmail: String? = null,
) {
    val entrance = remember { Animatable(0f) }
    val logoPop = remember { Animatable(0.55f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        logoPop.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "loginCardBeams")
    val beamCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "beamCycle",
    )
    val haloBreath by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.46f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "haloBreath",
    )

    var tiltTarget by remember { mutableStateOf(Offset.Zero) }
    val tiltAnimated by animateOffsetAsState(
        targetValue = tiltTarget,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 170f),
        label = "cardTilt",
    )

    val cardRadius = 22.dp
    val cardShape = RoundedCornerShape(cardRadius)

    Box(
        modifier = modifier
            .graphicsLayer {
                val p = entrance.value
                alpha = p
                translationY = (1f - p) * 18.dp.toPx()
                rotationX = tiltAnimated.y
                rotationY = tiltAnimated.x
                cameraDistance = 14f * density
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Move -> {
                                val pos = event.changes.lastOrNull()?.position
                                if (pos != null && size.width > 0 && size.height > 0) {
                                    val nx = ((pos.x / size.width.toFloat()) - 0.5f) * 2f
                                    val ny = ((pos.y / size.height.toFloat()) - 0.5f) * 2f
                                    tiltTarget = Offset(
                                        x = (nx * 6f).coerceIn(-6f, 6f),
                                        y = (-ny * 6f).coerceIn(-6f, 6f),
                                    )
                                }
                            }
                            PointerEventType.Exit, PointerEventType.Release -> {
                                tiltTarget = Offset.Zero
                            }
                        }
                    }
                }
            },
    ) {
        // Subtle outer breathing cobalt halo behind the card border
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val cornerPx = cardRadius.toPx()
                    drawRoundRect(
                        color = AuthAccentBlueBright.copy(alpha = haloBreath * 0.24f),
                        topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                        size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
                        cornerRadius = CornerRadius(cornerPx + 2.dp.toPx()),
                        style = Stroke(width = 4.dp.toPx()),
                    )
                },
        )

        // Main glass card with diagonal micro-pattern & traveling edge light beams
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(LoginCardGlass)
                .drawWithCache {
                    val gridStep = 28.dp.toPx()
                    onDrawBehind {
                        // Subtle 45-degree + 135-degree micro-grid overlay
                        val w = size.width
                        val h = size.height
                        val gridColor = Color.White.copy(alpha = 0.024f)
                        var x = -h
                        while (x < w + h) {
                            drawLine(
                                color = gridColor,
                                start = Offset(x, 0f),
                                end = Offset(x + h, h),
                                strokeWidth = 0.7f,
                            )
                            drawLine(
                                color = gridColor,
                                start = Offset(x + h, 0f),
                                end = Offset(x, h),
                                strokeWidth = 0.7f,
                            )
                            x += gridStep
                        }

                        // Traveling 4-edge light beams + pulsing corner spots
                        drawCardLightBeamsAndCorners(
                            cycle = beamCycle,
                            haloBreath = haloBreath,
                            cornerRadiusPx = cardRadius.toPx(),
                        )
                    }
                }
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AuthAccentBlueSoft.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.08f),
                            AuthAccentBlue.copy(alpha = 0.18f),
                        ),
                    ),
                    shape = cardShape,
                )
                .padding(horizontal = (20 * scale).coerceIn(18f, 26f).dp, vertical = (20 * scale).coerceIn(18f, 24f).dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Circular spring emblem badge
                Box(
                    modifier = Modifier
                        .size(44.dp * scale)
                        .graphicsLayer {
                            scaleX = logoPop.value
                            scaleY = logoPop.value
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AuthAccentBlue.copy(alpha = 0.34f),
                                    Color(0xFF0D1422),
                                ),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    AuthAccentBlueSoft.copy(alpha = 0.65f),
                                    Color.White.copy(alpha = 0.15f),
                                ),
                            ),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp * scale),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp * scale))

                // Centered Heading + Subtitle
                AnimatedContent(
                    targetState = Triple(isSignUp, verifiedPaidEmail != null, isTrialSignUp),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "cardHeading",
                ) { (signUp, _, trialMode) ->
                    Text(
                        text = when {
                            signUp && trialMode -> "Teste Grátis • 20 Dias"
                            signUp -> stringResource(Res.string.compose_auth_create_account)
                            else -> stringResource(Res.string.compose_auth_welcome_back)
                        },
                        style = headingStyle,
                        color = AuthTextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp * scale))

                AnimatedContent(
                    targetState = Triple(isSignUp, verifiedPaidEmail != null, isTrialSignUp),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "cardSubtitle",
                ) { (signUp, isVerified, trialMode) ->
                    Text(
                        text = when {
                            isVerified && signUp -> "Pagamento aprovado! Agora crie uma senha para o seu Gmail."
                            isVerified -> "Pagamento aprovado! Entre com a senha do seu Gmail."
                            signUp && trialMode -> "Crie sua conta com seu Gmail para liberar 20 dias grátis neste aparelho."
                            signUp -> "Informe o mesmo Gmail usado no pagamento PIX do site."
                            else -> stringResource(Res.string.compose_auth_sign_in_subtitle)
                        },
                        style = subtitleStyle,
                        color = AuthTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(6.dp * scale))

                TextButton(
                    onClick = onReturnToPaywall,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "← Voltar para Informações do Plano",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AuthAccentBlueSoft,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (12.5f * scale).sp,
                        ),
                    )
                }

                if (verifiedPaidEmail != null) {
                    Spacer(modifier = Modifier.height(6.dp * scale))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = AuthAccentBlue.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, AuthAccentBlueBright.copy(alpha = 0.45f)),
                    ) {
                        Text(
                            text = if (isSignUp) {
                                "✓ PIX de $priceLabel aprovado! Crie sua Conta Principal (Host) com o mesmo Gmail da compra ($verifiedPaidEmail) e escolha sua senha. Depois você poderá criar vários perfis."
                            } else {
                                "✓ PIX já aprovado para $verifiedPaidEmail! Esta conta Host já possui cadastro — entre com sua senha abaixo."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF93C5FD),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = (12f * scale).sp,
                                lineHeight = (17f * scale).sp,
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp * scale))
                } else if (isTrialSignUp || lockedTrialEmail != null) {
                    Spacer(modifier = Modifier.height(6.dp * scale))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = AuthAccentBlue.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, AuthAccentBlueBright.copy(alpha = 0.45f)),
                    ) {
                        Text(
                            text = if (lockedTrialEmail != null) {
                                "🔒 Aparelho vinculado ao Gmail $lockedTrialEmail. Limite de 1 conta de teste grátis por dispositivo — após os 20 dias, o pagamento de $priceLabel deverá ser feito com este mesmo Gmail."
                            } else {
                                "🎁 Teste Grátis de 20 Dias (1 conta por aparelho): Cadastre seu Gmail verdadeiro. Após os 20 dias, para continuar assistindo será obrigatório pagar $priceLabel usando este mesmo Gmail."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF93C5FD),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = (12f * scale).sp,
                                lineHeight = (17f * scale).sp,
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp * scale))
                } else {
                    Spacer(modifier = Modifier.height(8.dp * scale))
                }

                // Email input with focus elevation + glow border
                AnimatedAuthTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    placeholder = when {
                        isSignUp && isTrialSignUp -> "Seu Gmail (@gmail.com) para o Teste Grátis"
                        isSignUp -> "Gmail usado na compra PIX"
                        else -> stringResource(Res.string.compose_auth_email)
                    },
                    icon = Icons.Rounded.Email,
                    metrics = metrics,
                    scale = scale,
                    readOnly = lockedTrialEmail != null || verifiedPaidEmail != null,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        onEmailBoundsChange(coordinates.boundsInRoot())
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp * scale))

                // Password input with focus elevation + glow border
                AnimatedAuthTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    placeholder = stringResource(Res.string.compose_auth_password),
                    icon = Icons.Rounded.Lock,
                    metrics = metrics,
                    scale = scale,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityToggle = onPasswordVisibilityToggle,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        onPasswordBoundsChange(coordinates.boundsInRoot())
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onSubmit() },
                    ),
                )

                authError?.let { errorText ->
                    Spacer(modifier = Modifier.height(10.dp * scale))
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.error,
                            fontSize = (12.5f * scale).sp,
                            lineHeight = (17.5f * scale).sp,
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(18.dp * scale))

                // Cobalt-blue animated shimmer primary CTA button
                AnimatedShimmerBlueButton(
                    text = when {
                        isSignUp && isTrialSignUp -> "Ativar Teste Grátis de 20 Dias"
                        isSignUp && verifiedPaidEmail != null -> "Criar Conta Principal (Host)"
                        isSignUp -> stringResource(Res.string.compose_auth_create_account)
                        else -> stringResource(Res.string.compose_auth_sign_in)
                    },
                    isLoading = isLoading,
                    enabled = !isLoading,
                    height = metrics.primaryHeight,
                    scale = scale,
                    onClick = onSubmit,
                )

                Spacer(modifier = Modifier.height(14.dp * scale))

                AuthModeToggle(
                    isSignUp = isSignUp,
                    scale = scale,
                    onToggleAuthMode = onToggleAuthMode,
                )

                Spacer(modifier = Modifier.height(14.dp * scale))

                AuthDivider(scale = scale)

                Spacer(modifier = Modifier.height(14.dp * scale))

                AuthSecondaryButton(
                    text = "← Voltar para a Tela de Pagamento PIX ($priceLabel)",
                    enabled = !isLoading,
                    height = metrics.secondaryHeight,
                    scale = scale,
                    onClick = onReturnToPaywall,
                )

                Spacer(modifier = Modifier.height(12.dp * scale))

                Text(
                    text = stringResource(Res.string.compose_auth_store_locally),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AuthTextMuted,
                        fontSize = (12f * scale).sp,
                        lineHeight = (16.5f * scale).sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun DrawScope.drawCardLightBeamsAndCorners(
    cycle: Float,
    haloBreath: Float,
    cornerRadiusPx: Float,
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    val beamThickness = 2.4.dp.toPx()
    val haloThickness = 6.dp.toPx()
    val coreColor = Color.White.copy(alpha = 0.85f)
    val midBlue = AuthAccentBlueSoft.copy(alpha = 0.72f)
    val edgeBlue = AuthAccentBlue.copy(alpha = 0.28f)

    // Clip-aligned perimeter tracks inset slightly inside the card corners
    val pad = beamThickness * 0.5f

    // 1. Top beam: left -> right
    val pTop = (cycle % 1f)
    val topLen = w * 0.52f
    val topStartX = -topLen + pTop * (w + topLen * 1.4f)
    val topEndX = topStartX + topLen
    val topBrush = Brush.horizontalGradient(
        colors = listOf(Color.Transparent, edgeBlue, midBlue, coreColor, midBlue, Color.Transparent),
        startX = topStartX,
        endX = topEndX,
    )
    drawLine(brush = topBrush, start = Offset(0f, pad), end = Offset(w, pad), strokeWidth = beamThickness)
    drawLine(brush = topBrush, start = Offset(0f, pad), end = Offset(w, pad), strokeWidth = haloThickness, alpha = 0.25f)

    // 2. Right beam: top -> bottom
    val pRight = ((cycle + 0.25f) % 1f)
    val rightLen = h * 0.52f
    val rightStartY = -rightLen + pRight * (h + rightLen * 1.4f)
    val rightEndY = rightStartY + rightLen
    val rightBrush = Brush.verticalGradient(
        colors = listOf(Color.Transparent, edgeBlue, midBlue, coreColor, midBlue, Color.Transparent),
        startY = rightStartY,
        endY = rightEndY,
    )
    drawLine(brush = rightBrush, start = Offset(w - pad, 0f), end = Offset(w - pad, h), strokeWidth = beamThickness)
    drawLine(brush = rightBrush, start = Offset(w - pad, 0f), end = Offset(w - pad, h), strokeWidth = haloThickness, alpha = 0.25f)

    // 3. Bottom beam: right -> left
    val pBottom = ((cycle + 0.50f) % 1f)
    val bottomLen = w * 0.52f
    val bottomRightX = w + bottomLen - pBottom * (w + bottomLen * 1.4f)
    val bottomLeftX = bottomRightX - bottomLen
    val bottomBrush = Brush.horizontalGradient(
        colors = listOf(Color.Transparent, midBlue, coreColor, midBlue, edgeBlue, Color.Transparent),
        startX = bottomLeftX.coerceAtLeast(-w),
        endX = bottomRightX.coerceAtMost(w * 2f),
    )
    drawLine(brush = bottomBrush, start = Offset(0f, h - pad), end = Offset(w, h - pad), strokeWidth = beamThickness)
    drawLine(brush = bottomBrush, start = Offset(0f, h - pad), end = Offset(w, h - pad), strokeWidth = haloThickness, alpha = 0.25f)

    // 4. Left beam: bottom -> top
    val pLeft = ((cycle + 0.75f) % 1f)
    val leftLen = h * 0.52f
    val leftBottomY = h + leftLen - pLeft * (h + leftLen * 1.4f)
    val leftTopY = leftBottomY - leftLen
    val leftBrush = Brush.verticalGradient(
        colors = listOf(Color.Transparent, midBlue, coreColor, midBlue, edgeBlue, Color.Transparent),
        startY = leftTopY.coerceAtLeast(-h),
        endY = leftBottomY.coerceAtMost(h * 2f),
    )
    drawLine(brush = leftBrush, start = Offset(pad, 0f), end = Offset(pad, h), strokeWidth = beamThickness)
    drawLine(brush = leftBrush, start = Offset(pad, 0f), end = Offset(pad, h), strokeWidth = haloThickness, alpha = 0.25f)

    // 4 subtle corner glow spots
    val cornerInset = (cornerRadiusPx * 0.28f).coerceAtLeast(5.dp.toPx())
    val spotRadius = 11.dp.toPx()
    val corners = listOf(
        Offset(cornerInset, cornerInset),
        Offset(w - cornerInset, cornerInset),
        Offset(w - cornerInset, h - cornerInset),
        Offset(cornerInset, h - cornerInset),
    )
    corners.forEachIndexed { idx, pt ->
        val phase = sin(((cycle + idx * 0.25f) * 2.0 * PI)).toFloat() * 0.5f + 0.5f
        val alpha = 0.16f + phase * 0.28f + haloBreath * 0.12f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = alpha * 0.85f),
                    AuthAccentBlueSoft.copy(alpha = alpha * 0.55f),
                    Color.Transparent,
                ),
                center = pt,
                radius = spotRadius,
            ),
            radius = spotRadius,
            center = pt,
        )
    }
}

@Composable
private fun AnimatedAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    metrics: AuthFormMetrics,
    scale: Float,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityToggle: () -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var isFocused by remember { mutableStateOf(false) }

    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.016f else 1f,
        animationSpec = spring(stiffness = 380f, dampingRatio = 0.78f),
        label = "fieldScale",
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) LoginFieldFocusedBg else metrics.fieldBackground,
        animationSpec = tween(220),
        label = "fieldBg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> AuthAccentBlueSoft.copy(alpha = 0.78f)
            readOnly -> AuthAccentBlueBright.copy(alpha = 0.65f)
            else -> AuthFieldBorder
        },
        animationSpec = tween(220),
        label = "fieldBorder",
    )
    val iconTint by animateColorAsState(
        targetValue = when {
            isFocused -> AuthAccentBlueSoft
            readOnly -> AuthAccentBlueSoft
            else -> AuthTextMuted
        },
        animationSpec = tween(220),
        label = "fieldIcon",
    )

    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
            }
            .height(metrics.fieldHeight)
            .background(bgColor, shape)
            .border(
                width = if (isFocused) 1.25.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .padding(horizontal = metrics.fieldHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(metrics.iconSize),
            tint = iconTint,
        )
        Spacer(modifier = Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .onFocusChanged { isFocused = it.isFocused },
            readOnly = readOnly,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = AuthTextPrimary,
                fontSize = (16f * scale).sp,
                lineHeight = (22f * scale).sp,
                fontWeight = FontWeight.Normal,
            ),
            cursorBrush = SolidColor(AuthAccentBlueSoft),
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = AuthTextMuted,
                                fontSize = (16f * scale).sp,
                                lineHeight = (22f * scale).sp,
                                fontWeight = FontWeight.Normal,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (isPassword) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onPasswordVisibilityToggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff
                    else Icons.Rounded.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(metrics.passwordIconSize),
                    tint = if (isFocused) AuthAccentBlueSoft else AuthTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun AnimatedShimmerBlueButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    height: Dp,
    scale: Float,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(stiffness = 450f, dampingRatio = 0.75f),
        label = "btnPressScale",
    )

    val shimmerTransition = rememberInfiniteTransition(label = "ctaShimmer")
    val shimmerProgress by shimmerTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isLoading) 1200 else 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )

    val shape = RoundedCornerShape(14.dp)
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        enabled = enabled,
        shape = shape,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) AuthAccentBlueSoft.copy(alpha = 0.44f) else AuthAccentBlue.copy(alpha = 0.18f),
        ),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthPrimaryButtonBackground,
            contentColor = AuthPrimaryButtonText,
            disabledContainerColor = if (isLoading) AuthPrimaryButtonBackground else AuthPrimaryButtonBackground.copy(alpha = 0.45f),
            disabledContentColor = AuthPrimaryButtonText.copy(alpha = 0.65f),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    if (w <= 0f || h <= 0f) return@drawBehind
                    val bandWidth = w * 0.48f
                    val startX = shimmerProgress * (w + bandWidth) - bandWidth
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (isLoading) 0.24f else 0.14f),
                                Color.Transparent,
                            ),
                            start = Offset(startX, 0f),
                            end = Offset(startX + bandWidth, h),
                        ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                NuvioLoadingIndicator(
                    modifier = Modifier.size(20.dp * scale),
                    color = AuthPrimaryButtonText,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = (15.5f * scale).sp,
                            lineHeight = (20f * scale).sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp * scale),
                    )
                }
            }
        }
    }
}
