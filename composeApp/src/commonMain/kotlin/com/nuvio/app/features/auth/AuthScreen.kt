package com.nuvio.app.features.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.CineAuthClient
import com.nuvio.app.core.auth.CineSessionPersistence
import com.nuvio.app.core.auth.DeviceLinkAuthRepository
import com.nuvio.app.core.auth.DeviceLinkAuthState
import com.nuvio.app.core.auth.PersistedPaymentState
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.deeplink.AppDeepLink
import com.nuvio.app.core.deeplink.AppDeepLinkRepository
import com.nuvio.app.core.network.CineBackendConfig
import com.nuvio.app.core.sync.AppForegroundMonitor
import com.nuvio.app.core.sync.AppVisibility
import com.nuvio.app.features.settings.AppBrandWordmark
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sin
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_auth_already_have_account
import nuvio.composeapp.generated.resources.compose_auth_continue_without_account
import nuvio.composeapp.generated.resources.compose_auth_create_account
import nuvio.composeapp.generated.resources.compose_auth_dont_have_account
import nuvio.composeapp.generated.resources.compose_auth_email
import nuvio.composeapp.generated.resources.compose_auth_or_separator
import nuvio.composeapp.generated.resources.compose_auth_password
import nuvio.composeapp.generated.resources.compose_auth_sign_in
import nuvio.composeapp.generated.resources.compose_auth_sign_in_subtitle
import nuvio.composeapp.generated.resources.compose_auth_sign_up
import nuvio.composeapp.generated.resources.compose_auth_sign_up_subtitle
import nuvio.composeapp.generated.resources.compose_auth_store_locally
import nuvio.composeapp.generated.resources.compose_auth_tagline
import nuvio.composeapp.generated.resources.compose_auth_terms_link
import nuvio.composeapp.generated.resources.compose_auth_terms_prefix
import nuvio.composeapp.generated.resources.compose_auth_welcome_back
import org.jetbrains.compose.resources.stringResource

internal val AuthTextPrimary = Color(0xFFF3F6FC)
internal val AuthTextSecondary = Color(0xFF93A1B8)
private val AuthTextMuted = Color(0xFF69768E)
private val AuthAccentBlue = Color(0xFF2563EB)
private val AuthAccentBlueBright = Color(0xFF3B82F6)
private val AuthAccentBlueSoft = Color(0xFF60A5FA)
private val AuthPrimaryButtonBackground = Color(0xFF2563EB)
private val AuthPrimaryButtonText = Color(0xFFF8FAFC)
internal val AuthFieldBackground = Color(0xFF0D1119)
private val AuthFieldBackgroundMobile = Color(0xFF0D1119)
internal val AuthFieldBorder = Color(0xFF1F2B42)
private val AuthDividerColor = Color(0xFF1E293B)
private val AuthSecondaryButtonBackground = Color(0xFF101520)
private val AuthSecondaryButtonBorder = Color(0xFF253552)

internal data class AuthFormMetrics(
    val fieldHeight: Dp,
    val fieldHorizontalPadding: Dp,
    val iconSize: Dp,
    val passwordIconSize: Dp,
    val primaryTop: Dp,
    val primaryHeight: Dp,
    val toggleTop: Dp,
    val dividerTop: Dp,
    val secondaryTop: Dp,
    val secondaryHeight: Dp,
    val fieldBackground: Color,
)

private val MobileAuthFormMetrics = AuthFormMetrics(
    fieldHeight = 56.dp,
    fieldHorizontalPadding = 16.dp,
    iconSize = 19.dp,
    passwordIconSize = 20.dp,
    primaryTop = 22.dp,
    primaryHeight = 54.dp,
    toggleTop = 18.dp,
    dividerTop = 28.dp,
    secondaryTop = 24.dp,
    secondaryHeight = 54.dp,
    fieldBackground = AuthFieldBackgroundMobile,
)

internal val LargeAuthFormMetrics = AuthFormMetrics(
    fieldHeight = 58.dp,
    fieldHorizontalPadding = 18.dp,
    iconSize = 20.dp,
    passwordIconSize = 21.dp,
    primaryTop = 24.dp,
    primaryHeight = 56.dp,
    toggleTop = 18.dp,
    dividerTop = 30.dp,
    secondaryTop = 26.dp,
    secondaryHeight = 56.dp,
    fieldBackground = AuthFieldBackground,
)

private fun largeAuthScale(screenWidth: Dp): Float =
    (screenWidth.value / 1194f).coerceIn(1f, 1.32f)

private fun largeAuthFormMetrics(scale: Float): AuthFormMetrics = LargeAuthFormMetrics.copy(
    fieldHeight = 58.dp * scale,
    fieldHorizontalPadding = 18.dp * scale,
    iconSize = 20.dp * scale,
    passwordIconSize = 21.dp * scale,
    primaryTop = 24.dp * scale,
    primaryHeight = 56.dp * scale,
    toggleTop = 18.dp * scale,
    dividerTop = 30.dp * scale,
    secondaryTop = 26.dp * scale,
    secondaryHeight = 56.dp * scale,
)

@OptIn(ExperimentalUuidApi::class)
@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
) {
    val authError by AuthRepository.error.collectAsStateWithLifecycle()
    val deviceLinkAuthState by DeviceLinkAuthRepository.state.collectAsStateWithLifecycle()
    val serverConnectionState by ServerConnectionController.state.collectAsStateWithLifecycle()
    val pendingDeepLink by AppDeepLinkRepository.pendingDeepLink.collectAsStateWithLifecycle()
    val appVisibility by AppForegroundMonitor.events().collectAsStateWithLifecycle(initialValue = AppVisibility.Foreground)
    val planInfo by CineAuthClient.planInfo.collectAsStateWithLifecycle()
    val priceLabel = planInfo.formattedPrice
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current
    val initialPaymentState = remember { CineSessionPersistence.loadPaymentState() }

    var showLoginScreen by rememberSaveable {
        mutableStateOf(!initialPaymentState?.verifiedPaidEmail.isNullOrBlank())
    }
    var verifiedPaidEmail by rememberSaveable { mutableStateOf(initialPaymentState?.verifiedPaidEmail) }
    var activeCheckoutSessionId by rememberSaveable {
        mutableStateOf(
            initialPaymentState?.pendingCheckoutSessionId
                ?: initialPaymentState?.confirmedPaidEmail?.let { "chk_restored" },
        )
    }
    var confirmedPaidEmail by rememberSaveable { mutableStateOf(initialPaymentState?.confirmedPaidEmail) }
    var confirmedPaidHasAccount by rememberSaveable {
        mutableStateOf(initialPaymentState?.confirmedPaidHasAccount ?: false)
    }
    var confirmedPaymentId by rememberSaveable { mutableStateOf(initialPaymentState?.confirmedPaymentId) }
    var manualCheckEmail by rememberSaveable { mutableStateOf(initialPaymentState?.confirmedPaidEmail.orEmpty()) }
    var showManualEmailFallback by rememberSaveable { mutableStateOf(false) }
    var isConfirmingPayment by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    var isSignUp by rememberSaveable {
        mutableStateOf(
            if (!initialPaymentState?.verifiedPaidEmail.isNullOrBlank()) {
                initialPaymentState?.isSignUpMode ?: true
            } else {
                true
            },
        )
    }
    var email by rememberSaveable { mutableStateOf(initialPaymentState?.verifiedPaidEmail.orEmpty()) }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var emailFieldBounds by remember { mutableStateOf<Rect?>(null) }
    var passwordFieldBounds by remember { mutableStateOf<Rect?>(null) }
    var showServerSheet by rememberSaveable { mutableStateOf(false) }
    var showOfficialServerDialog by rememberSaveable { mutableStateOf(false) }

    val effectiveError = localError ?: authError

    fun syncPersistedPaymentState(
        pendingSessionId: String? = activeCheckoutSessionId?.takeUnless { it == "chk_restored" || it == "chk_deeplink" },
        paidEmail: String? = confirmedPaidEmail,
        hasAccount: Boolean = confirmedPaidHasAccount,
        paymentId: String? = confirmedPaymentId,
        verifiedEmail: String? = verifiedPaidEmail,
        signUpMode: Boolean = isSignUp,
    ) {
        CineSessionPersistence.savePaymentState(
            PersistedPaymentState(
                pendingCheckoutSessionId = pendingSessionId,
                confirmedPaidEmail = paidEmail,
                confirmedPaidHasAccount = hasAccount,
                confirmedPaymentId = paymentId,
                verifiedPaidEmail = verifiedEmail,
                isSignUpMode = signUpMode,
            ),
        )
    }

    fun unlockWithConfirmedPayment(approvedEmail: String, hasExistingAccount: Boolean) {
        val cleanEmail = approvedEmail.trim().lowercase()
        val nextSignUpMode = !hasExistingAccount
        email = cleanEmail
        verifiedPaidEmail = cleanEmail
        isSignUp = nextSignUpMode
        activeCheckoutSessionId = null
        confirmedPaidEmail = cleanEmail
        confirmedPaidHasAccount = hasExistingAccount
        showManualEmailFallback = false
        isConfirmingPayment = false
        localError = null
        AuthRepository.clearError()
        showLoginScreen = true
        syncPersistedPaymentState(
            pendingSessionId = null,
            paidEmail = cleanEmail,
            hasAccount = hasExistingAccount,
            paymentId = confirmedPaymentId,
            verifiedEmail = cleanEmail,
            signUpMode = nextSignUpMode,
        )
    }

    LaunchedEffect(appVisibility) {
        if (appVisibility == AppVisibility.Foreground) {
            CineAuthClient.refreshPlanInfo()
        }
    }

    LaunchedEffect(pendingDeepLink) {
        val deepLink = pendingDeepLink
        if (deepLink is AppDeepLink.PaymentApproved) {
            val paidEmail = deepLink.email.trim().lowercase()
            AppDeepLinkRepository.markConsumed(deepLink)
            if (paidEmail.isNotBlank()) {
                if (verifiedPaidEmail == paidEmail && showLoginScreen) {
                    activeCheckoutSessionId = null
                    return@LaunchedEffect
                }
                isConfirmingPayment = true
                localError = null
                AuthRepository.clearError()
                val check = CineAuthClient.checkEmail(paidEmail, deepLink.paymentId)
                isConfirmingPayment = false
                if (check.isSuccess) {
                    val info = check.getOrThrow()
                    if (info.entitlementActive) {
                        confirmedPaidEmail = info.email
                        confirmedPaidHasAccount = info.hasAccount
                        confirmedPaymentId = deepLink.paymentId
                        if (activeCheckoutSessionId == null) {
                            activeCheckoutSessionId = "chk_deeplink"
                        }
                        if (manualCheckEmail.isBlank()) {
                            manualCheckEmail = info.email
                        }
                        syncPersistedPaymentState(
                            paidEmail = info.email,
                            hasAccount = info.hasAccount,
                            paymentId = deepLink.paymentId,
                        )
                    } else {
                        localError = "Acesso negado: O Gmail $paidEmail ainda não possui pagamento PIX de ${CineAuthClient.currentFormattedPrice()} aprovado."
                        showLoginScreen = false
                    }
                } else {
                    localError = check.exceptionOrNull()?.message
                        ?: "Não foi possível confirmar o pagamento do Gmail $paidEmail."
                }
            }
        }
    }

    LaunchedEffect(activeCheckoutSessionId, showLoginScreen, confirmedPaidEmail, appVisibility) {
        val sessionId = activeCheckoutSessionId ?: return@LaunchedEffect
        if (
            showLoginScreen ||
            confirmedPaidEmail != null ||
            sessionId == "chk_deeplink" ||
            sessionId == "chk_restored"
        ) {
            return@LaunchedEffect
        }

        repeat(600) { attempt ->
            if (attempt > 0) {
                val intervalMs = when {
                    appVisibility != AppVisibility.Foreground -> 2_500L
                    attempt < 45 -> 700L
                    else -> 1_200L
                }
                delay(intervalMs)
            }
            val statusResult = CineAuthClient.checkSessionStatus(sessionId)
            val status = statusResult.getOrNull() ?: return@repeat
            if (status.endpointUnavailable) {
                showManualEmailFallback = true
                return@LaunchedEffect
            }
            if (!status.paymentId.isNullOrBlank()) {
                confirmedPaymentId = status.paymentId
            }
            if (status.approved && status.email.isNotBlank()) {
                val resolvedPaymentId = status.paymentId?.takeIf { it.isNotBlank() } ?: confirmedPaymentId
                confirmedPaidEmail = status.email
                confirmedPaidHasAccount = status.hasAccount
                confirmedPaymentId = resolvedPaymentId
                localError = null
                AuthRepository.clearError()
                syncPersistedPaymentState(
                    pendingSessionId = sessionId,
                    paidEmail = status.email,
                    hasAccount = status.hasAccount,
                    paymentId = resolvedPaymentId,
                )
                return@LaunchedEffect
            }
        }
    }

    LaunchedEffect(activeCheckoutSessionId, showLoginScreen, confirmedPaidEmail) {
        val sessionId = activeCheckoutSessionId ?: return@LaunchedEffect
        if (showLoginScreen || confirmedPaidEmail != null || showManualEmailFallback) return@LaunchedEffect
        delay(8_000L)
        if (activeCheckoutSessionId == sessionId && confirmedPaidEmail == null && !showLoginScreen) {
            showManualEmailFallback = true
        }
    }

    fun startCheckout() {
        localError = null
        AuthRepository.clearError()
        val sessionId = "chk_${Uuid.random()}"
        activeCheckoutSessionId = sessionId
        confirmedPaidEmail = null
        confirmedPaidHasAccount = false
        confirmedPaymentId = null
        verifiedPaidEmail = null
        showManualEmailFallback = false
        isConfirmingPayment = false
        syncPersistedPaymentState(
            pendingSessionId = sessionId,
            paidEmail = null,
            hasAccount = false,
            paymentId = null,
            verifiedEmail = null,
            signUpMode = true,
        )
        uriHandler.openUri("${CineBackendConfig.checkoutUrl}?session=$sessionId")
    }

    fun proceedIfPaymentConfirmed() {
        if (isConfirmingPayment || activeCheckoutSessionId == null) return
        localError = null
        AuthRepository.clearError()

        val alreadyConfirmedEmail = confirmedPaidEmail
        if (!alreadyConfirmedEmail.isNullOrBlank()) {
            unlockWithConfirmedPayment(alreadyConfirmedEmail, confirmedPaidHasAccount)
            return
        }

        val sessionId = activeCheckoutSessionId
        val typedEmail = manualCheckEmail.trim().lowercase()
        val hasTypedEmail = typedEmail.contains("@") && typedEmail.contains(".")
        val querySessionId = sessionId?.takeIf { it != "chk_deeplink" && it != "chk_restored" }
        isConfirmingPayment = true
        scope.launch {
            coroutineScope {
                val sessionDeferred = querySessionId?.let { id ->
                    async { CineAuthClient.checkSessionStatus(id).getOrNull() }
                }
                val emailDeferred = if (hasTypedEmail) {
                    async { CineAuthClient.checkEmail(typedEmail, confirmedPaymentId) }
                } else {
                    null
                }

                val sessionStatus = sessionDeferred?.await()
                if (sessionStatus != null) {
                    if (!sessionStatus.paymentId.isNullOrBlank()) {
                        confirmedPaymentId = sessionStatus.paymentId
                    }
                    if (sessionStatus.approved && sessionStatus.email.isNotBlank()) {
                        emailDeferred?.cancel()
                        unlockWithConfirmedPayment(sessionStatus.email, sessionStatus.hasAccount)
                        return@coroutineScope
                    }
                }

                if (emailDeferred != null) {
                    val check = emailDeferred.await()
                    isConfirmingPayment = false
                    if (check.isSuccess) {
                        val info = check.getOrThrow()
                        if (info.entitlementActive) {
                            unlockWithConfirmedPayment(info.email, info.hasAccount)
                        } else {
                            localError = "Pagamento PIX de ${CineAuthClient.currentFormattedPrice()} ainda não confirmado para ${info.email}. Conclua o PIX no site ou aguarde alguns segundos e tente novamente."
                        }
                    } else {
                        localError = check.exceptionOrNull()?.message
                            ?: "Não foi possível confirmar o pagamento agora. Tente novamente em instantes."
                    }
                } else {
                    isConfirmingPayment = false
                    localError = "Confirmação automática pendente. Digite o mesmo Gmail usado no PIX do site para confirmar e prosseguir."
                }
            }
        }
    }

    fun openExistingAccountLogin() {
        localError = null
        AuthRepository.clearError()
        verifiedPaidEmail = null
        isConfirmingPayment = false
        isSignUp = false
        showLoginScreen = true
    }

    fun returnToPaywall() {
        localError = null
        AuthRepository.clearError()
        val preservedEmail = confirmedPaidEmail ?: verifiedPaidEmail
        verifiedPaidEmail = null
        confirmedPaidEmail = preservedEmail
        if (preservedEmail != null && activeCheckoutSessionId == null) {
            activeCheckoutSessionId = "chk_restored"
        }
        isConfirmingPayment = false
        showLoginScreen = false
        syncPersistedPaymentState(
            paidEmail = preservedEmail,
            verifiedEmail = null,
        )
    }

    fun submitAuth() {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || password.length < 6 || isLoading) {
            if (password.length in 1..5) {
                localError = "Escolha uma senha com pelo menos 6 caracteres para sua conta Host."
            }
            return
        }
        localError = null
        DeviceLinkAuthRepository.cancel()
        isLoading = true
        focusManager.clearFocus(force = true)
        scope.launch {
            if (isSignUp) AuthRepository.signUpWithEmail(cleanEmail, password)
            else AuthRepository.signInWithEmail(cleanEmail, password)
            isLoading = false
        }
    }

    fun toggleAuthMode() {
        DeviceLinkAuthRepository.cancel()
        isSignUp = !isSignUp
        localError = null
        AuthRepository.clearError()
    }

    fun startDeviceLink() {
        if (isLoading) return
        focusManager.clearFocus(force = true)
        localError = null
        AuthRepository.clearError()
        DeviceLinkAuthRepository.start()
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(serverConnectionState.activeServer.backendUrl) {
        DeviceLinkAuthRepository.cancel()
        if (!serverConnectionState.activeServer.isCustom) {
            showOfficialServerDialog = false
        }
    }

    DisposableEffect(Unit) {
        onDispose(DeviceLinkAuthRepository::cancel)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(emailFieldBounds, passwordFieldBounds) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    val tappedTextField = listOfNotNull(emailFieldBounds, passwordFieldBounds)
                        .any { bounds -> bounds.contains(down.position) }
                    if (!tappedTextField) {
                        focusManager.clearFocus(force = true)
                    }
                }
            },
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            val screenWidth = maxWidth
            val largeScreen = screenWidth >= 900.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .authGradientBackground(largeScreen = largeScreen),
            ) {
                PaperGrainOverlay(
                    alpha = 0.032f,
                    modifier = Modifier.fillMaxSize(),
                )
                if (largeScreen) {
                    val largeScale = largeAuthScale(screenWidth)
                    val formMetrics = largeAuthFormMetrics(largeScale)

                    AuthLargeLayout(
                        modifier = Modifier.fillMaxSize(),
                        showLoginScreen = showLoginScreen,
                        priceLabel = priceLabel,
                        verifiedPaidEmail = verifiedPaidEmail,
                        isWaitingPayment = activeCheckoutSessionId != null && !showLoginScreen,
                        confirmedPaidEmail = confirmedPaidEmail,
                        manualCheckEmail = manualCheckEmail,
                        showManualEmailFallback = showManualEmailFallback,
                        isConfirmingPayment = isConfirmingPayment,
                        isSignUp = isSignUp,
                        email = email,
                        password = password,
                        passwordVisible = passwordVisible,
                        isLoading = isLoading,
                        authError = effectiveError,
                        deviceLinkAuthState = deviceLinkAuthState,
                        deviceLinkEnabled = serverConnectionState.activeServer.capabilities.tvLogin,
                        statusBarTop = statusBarTop,
                        formMetrics = formMetrics,
                        scale = largeScale,
                        onEmailChange = {
                            if (verifiedPaidEmail == null) {
                                email = it
                            }
                            localError = null
                            AuthRepository.clearError()
                        },
                        onPasswordChange = {
                            password = it
                            localError = null
                            AuthRepository.clearError()
                        },
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                        onSubmit = ::submitAuth,
                        onToggleAuthMode = ::toggleAuthMode,
                        onStartCheckout = ::startCheckout,
                        onManualCheckEmailChange = {
                            manualCheckEmail = it
                            localError = null
                            AuthRepository.clearError()
                        },
                        onProceedPaid = ::proceedIfPaymentConfirmed,
                        onOpenExistingAccountLogin = ::openExistingAccountLogin,
                        onReturnToPaywall = ::returnToPaywall,
                        onStartDeviceLink = ::startDeviceLink,
                        onCancelDeviceLink = DeviceLinkAuthRepository::cancel,
                        onEmailBoundsChange = { emailFieldBounds = it },
                        onPasswordBoundsChange = { passwordFieldBounds = it },
                    )
                } else {
                    AuthMobileLayout(
                        showLoginScreen = showLoginScreen,
                        priceLabel = priceLabel,
                        verifiedPaidEmail = verifiedPaidEmail,
                        isWaitingPayment = activeCheckoutSessionId != null && !showLoginScreen,
                        confirmedPaidEmail = confirmedPaidEmail,
                        manualCheckEmail = manualCheckEmail,
                        showManualEmailFallback = showManualEmailFallback,
                        isConfirmingPayment = isConfirmingPayment,
                        isSignUp = isSignUp,
                        email = email,
                        password = password,
                        passwordVisible = passwordVisible,
                        isLoading = isLoading,
                        authError = effectiveError,
                        deviceLinkAuthState = deviceLinkAuthState,
                        deviceLinkEnabled = serverConnectionState.activeServer.capabilities.tvLogin,
                        statusBarTop = statusBarTop,
                        onEmailChange = {
                            if (verifiedPaidEmail == null) {
                                email = it
                            }
                            localError = null
                            AuthRepository.clearError()
                        },
                        onPasswordChange = {
                            password = it
                            localError = null
                            AuthRepository.clearError()
                        },
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                        onSubmit = ::submitAuth,
                        onToggleAuthMode = ::toggleAuthMode,
                        onStartCheckout = ::startCheckout,
                        onManualCheckEmailChange = {
                            manualCheckEmail = it
                            localError = null
                            AuthRepository.clearError()
                        },
                        onProceedPaid = ::proceedIfPaymentConfirmed,
                        onOpenExistingAccountLogin = ::openExistingAccountLogin,
                        onReturnToPaywall = ::returnToPaywall,
                        onStartDeviceLink = ::startDeviceLink,
                        onCancelDeviceLink = DeviceLinkAuthRepository::cancel,
                        onEmailBoundsChange = { emailFieldBounds = it },
                        onPasswordBoundsChange = { passwordFieldBounds = it },
                    )
                }
            }
        }

        if (AppFeaturePolicy.customServerConnectionsEnabled) {
            ServerConnectionMenu(
                activeServer = serverConnectionState.activeServer,
                onUseOfficial = {
                    ServerConnectionController.resetDiscovery()
                    showOfficialServerDialog = true
                },
                onConnectCustom = {
                    ServerConnectionController.resetDiscovery()
                    showServerSheet = true
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = statusBarTop + 6.dp),
            )
        }
    }

    if (
        AppFeaturePolicy.customServerConnectionsEnabled &&
        showServerSheet &&
        serverConnectionState.discoveredServer == null
    ) {
        ServerConnectionSheet(
            state = serverConnectionState,
            onDiscover = ServerConnectionController::discover,
            onDismiss = {
                showServerSheet = false
                ServerConnectionController.resetDiscovery()
            },
        )
    }

    serverConnectionState.discoveredServer?.let { server ->
        if (AppFeaturePolicy.customServerConnectionsEnabled) {
            ServerTrustDialog(
                server = server,
                isSwitching = serverConnectionState.isSwitching,
                switchFailure = serverConnectionState.switchFailure,
                onConfirm = ServerConnectionController::connectDiscovered,
                onDismiss = {
                    showServerSheet = false
                    ServerConnectionController.resetDiscovery()
                },
            )
        }
    }

    if (AppFeaturePolicy.customServerConnectionsEnabled && showOfficialServerDialog) {
        OfficialServerDialog(
            isSwitching = serverConnectionState.isSwitching,
            switchFailure = serverConnectionState.switchFailure,
            onConfirm = ServerConnectionController::useOfficial,
            onDismiss = {
                showOfficialServerDialog = false
                ServerConnectionController.resetDiscovery()
            },
        )
    }
}

@Composable
private fun AuthMobileLayout(
    showLoginScreen: Boolean,
    priceLabel: String,
    verifiedPaidEmail: String?,
    isWaitingPayment: Boolean,
    confirmedPaidEmail: String?,
    manualCheckEmail: String,
    showManualEmailFallback: Boolean,
    isConfirmingPayment: Boolean,
    isSignUp: Boolean,
    email: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    authError: String?,
    deviceLinkAuthState: DeviceLinkAuthState,
    deviceLinkEnabled: Boolean,
    statusBarTop: Dp,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSubmit: () -> Unit,
    onToggleAuthMode: () -> Unit,
    onStartCheckout: () -> Unit,
    onManualCheckEmailChange: (String) -> Unit,
    onProceedPaid: () -> Unit,
    onOpenExistingAccountLogin: () -> Unit,
    onReturnToPaywall: () -> Unit,
    onStartDeviceLink: () -> Unit,
    onCancelDeviceLink: () -> Unit,
    onEmailBoundsChange: (Rect) -> Unit,
    onPasswordBoundsChange: (Rect) -> Unit,
) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                top = statusBarTop + 14.dp,
                end = 16.dp,
                bottom = 32.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SmokePaperCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            cornerRadius = 26,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                AppBrandWordmark(
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .height(28.dp),
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .widthIn(max = 310.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.compose_auth_tagline),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = AuthTextPrimary,
                            fontSize = 21.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.compose_auth_sign_up_subtitle),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF9FB0CC),
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .widthIn(max = 384.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val p = entrance.value
                    alpha = p
                    translationY = (1f - p) * 16.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthHeading(
                showLoginScreen = showLoginScreen,
                verifiedPaidEmail = verifiedPaidEmail,
                isSignUp = isSignUp,
                headingStyle = MaterialTheme.typography.headlineLarge.copy(
                    color = AuthTextPrimary,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                subtitleStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = AuthTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthForm(
                showLoginScreen = showLoginScreen,
                priceLabel = priceLabel,
                verifiedPaidEmail = verifiedPaidEmail,
                isWaitingPayment = isWaitingPayment,
                confirmedPaidEmail = confirmedPaidEmail,
                manualCheckEmail = manualCheckEmail,
                showManualEmailFallback = showManualEmailFallback,
                isConfirmingPayment = isConfirmingPayment,
                isSignUp = isSignUp,
                email = email,
                password = password,
                passwordVisible = passwordVisible,
                isLoading = isLoading,
                authError = authError,
                deviceLinkAuthState = deviceLinkAuthState,
                deviceLinkEnabled = deviceLinkEnabled,
                metrics = MobileAuthFormMetrics,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onPasswordVisibilityToggle = onPasswordVisibilityToggle,
                onSubmit = onSubmit,
                onToggleAuthMode = onToggleAuthMode,
                onStartCheckout = onStartCheckout,
                onManualCheckEmailChange = onManualCheckEmailChange,
                onProceedPaid = onProceedPaid,
                onOpenExistingAccountLogin = onOpenExistingAccountLogin,
                onReturnToPaywall = onReturnToPaywall,
                onStartDeviceLink = onStartDeviceLink,
                onCancelDeviceLink = onCancelDeviceLink,
                onEmailBoundsChange = onEmailBoundsChange,
                onPasswordBoundsChange = onPasswordBoundsChange,
            )
        }
    }
}

@Composable
private fun AuthLargeLayout(
    modifier: Modifier = Modifier,
    showLoginScreen: Boolean,
    priceLabel: String,
    verifiedPaidEmail: String?,
    isWaitingPayment: Boolean,
    confirmedPaidEmail: String?,
    manualCheckEmail: String,
    showManualEmailFallback: Boolean,
    isConfirmingPayment: Boolean,
    isSignUp: Boolean,
    email: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    authError: String?,
    deviceLinkAuthState: DeviceLinkAuthState,
    deviceLinkEnabled: Boolean,
    statusBarTop: Dp,
    formMetrics: AuthFormMetrics,
    scale: Float,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSubmit: () -> Unit,
    onToggleAuthMode: () -> Unit,
    onStartCheckout: () -> Unit,
    onManualCheckEmailChange: (String) -> Unit,
    onProceedPaid: () -> Unit,
    onOpenExistingAccountLogin: () -> Unit,
    onReturnToPaywall: () -> Unit,
    onStartDeviceLink: () -> Unit,
    onCancelDeviceLink: () -> Unit,
    onEmailBoundsChange: (Rect) -> Unit,
    onPasswordBoundsChange: (Rect) -> Unit,
) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }

    val safeTop = if (statusBarTop > 8.dp) statusBarTop + 8.dp else 14.dp

    Row(
        modifier = modifier.padding(
            start = 16.dp,
            top = safeTop,
            end = 16.dp,
            bottom = 14.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Left: stacked dark paper stack with live black-and-blue billowing smoke
        SmokePaperCard(
            modifier = Modifier
                .weight(1.06f)
                .fillMaxHeight(),
            cornerRadius = 28,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp * scale, vertical = 26.dp * scale),
            ) {
                AppBrandWordmark(
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .height(34.dp * scale)
                        .graphicsLayer { alpha = entrance.value },
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .widthIn(max = 390.dp * scale)
                        .graphicsLayer {
                            val p = entrance.value
                            alpha = p
                            translationY = (1f - p) * 14.dp.toPx()
                        },
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = AuthAccentBlue.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, AuthAccentBlueBright.copy(alpha = 0.34f)),
                    ) {
                        Text(
                            text = "CINE ULTIMATE • 4K ULTRA HD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AuthAccentBlueSoft,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                fontSize = (10.5f * scale).sp,
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp * scale, vertical = 4.dp * scale),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp * scale))
                    Text(
                        text = stringResource(Res.string.compose_auth_tagline),
                        style = MaterialTheme.typography.displaySmall.copy(
                            color = AuthTextPrimary,
                            fontSize = (31f * scale).sp,
                            lineHeight = (35f * scale).sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Spacer(modifier = Modifier.height(8.dp * scale))
                    Text(
                        text = stringResource(Res.string.compose_auth_sign_up_subtitle),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color(0xFF9EB0CC),
                            fontSize = (14.5f * scale).sp,
                            lineHeight = (21f * scale).sp,
                            fontWeight = FontWeight.Normal,
                        ),
                    )
                }
            }
        }

        // Right: Cine Ultimate payment & profile access flow on obsidian paper
        BoxWithConstraints(
            modifier = Modifier
                .weight(0.94f)
                .fillMaxHeight(),
        ) {
            val availableHeight = maxHeight
            val compactVertical = availableHeight < 760.dp
            val rightHorizontalPadding = if (maxWidth < 460.dp) 18.dp else 32.dp * scale
            val rightVerticalPadding = if (compactVertical) 10.dp else 24.dp * scale

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = availableHeight)
                    .padding(horizontal = rightHorizontalPadding, vertical = rightVerticalPadding)
                    .graphicsLayer {
                        val p = entrance.value
                        alpha = p
                        translationY = (1f - p) * 14.dp.toPx()
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 420.dp * scale)
                        .fillMaxWidth(),
                ) {
                    AuthHeading(
                        showLoginScreen = showLoginScreen,
                        verifiedPaidEmail = verifiedPaidEmail,
                        isSignUp = isSignUp,
                        headingStyle = MaterialTheme.typography.headlineLarge.copy(
                            color = AuthTextPrimary,
                            fontSize = if (compactVertical) 25.sp else (29f * scale).sp,
                            lineHeight = if (compactVertical) 29.sp else (33f * scale).sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        subtitleStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = AuthTextSecondary,
                            fontSize = if (compactVertical) 13.5.sp else (14.5f * scale).sp,
                            lineHeight = if (compactVertical) 19.sp else (20.5f * scale).sp,
                            fontWeight = FontWeight.Normal,
                        ),
                    )

                    Spacer(modifier = Modifier.height(if (compactVertical) 12.dp else 20.dp * scale))

                    AuthForm(
                        showLoginScreen = showLoginScreen,
                        priceLabel = priceLabel,
                        verifiedPaidEmail = verifiedPaidEmail,
                        isWaitingPayment = isWaitingPayment,
                        confirmedPaidEmail = confirmedPaidEmail,
                        manualCheckEmail = manualCheckEmail,
                        showManualEmailFallback = showManualEmailFallback,
                        isConfirmingPayment = isConfirmingPayment,
                        isSignUp = isSignUp,
                        email = email,
                        password = password,
                        passwordVisible = passwordVisible,
                        isLoading = isLoading,
                        authError = authError,
                        deviceLinkAuthState = deviceLinkAuthState,
                        deviceLinkEnabled = deviceLinkEnabled,
                        metrics = formMetrics,
                        scale = scale,
                        onEmailChange = onEmailChange,
                        onPasswordChange = onPasswordChange,
                        onPasswordVisibilityToggle = onPasswordVisibilityToggle,
                        onSubmit = onSubmit,
                        onToggleAuthMode = onToggleAuthMode,
                        onStartCheckout = onStartCheckout,
                        onManualCheckEmailChange = onManualCheckEmailChange,
                        onProceedPaid = onProceedPaid,
                        onOpenExistingAccountLogin = onOpenExistingAccountLogin,
                        onReturnToPaywall = onReturnToPaywall,
                        onStartDeviceLink = onStartDeviceLink,
                        onCancelDeviceLink = onCancelDeviceLink,
                        onEmailBoundsChange = onEmailBoundsChange,
                        onPasswordBoundsChange = onPasswordBoundsChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthHeading(
    showLoginScreen: Boolean,
    verifiedPaidEmail: String?,
    isSignUp: Boolean,
    headingStyle: TextStyle,
    subtitleStyle: TextStyle,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedContent(
            targetState = Triple(showLoginScreen, isSignUp, verifiedPaidEmail != null),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "authHeading",
        ) { (loginVisible, signUp, isVerified) ->
            Text(
                text = when {
                    !loginVisible -> "Acesso Exclusivo"
                    signUp -> stringResource(Res.string.compose_auth_create_account)
                    else -> stringResource(Res.string.compose_auth_welcome_back)
                },
                style = headingStyle,
                color = AuthTextPrimary,
            )
        }
        AnimatedContent(
            targetState = Triple(showLoginScreen, isSignUp, verifiedPaidEmail != null),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "authSubtitle",
        ) { (loginVisible, signUp, isVerified) ->
            Text(
                text = when {
                    !loginVisible -> "Pague no site oficial para liberar automaticamente sua conta."
                    isVerified && signUp -> "Pagamento aprovado! Agora crie uma senha para o seu Gmail."
                    isVerified -> "Pagamento aprovado! Entre com a senha do seu Gmail."
                    signUp -> "Informe o mesmo Gmail usado no pagamento PIX do site."
                    else -> stringResource(Res.string.compose_auth_sign_in_subtitle)
                },
                style = subtitleStyle,
                color = AuthTextSecondary,
            )
        }
    }
}

@Composable
private fun AuthForm(
    showLoginScreen: Boolean,
    priceLabel: String,
    verifiedPaidEmail: String?,
    isWaitingPayment: Boolean,
    confirmedPaidEmail: String?,
    manualCheckEmail: String,
    showManualEmailFallback: Boolean,
    isConfirmingPayment: Boolean,
    isSignUp: Boolean,
    email: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    authError: String?,
    deviceLinkAuthState: DeviceLinkAuthState,
    deviceLinkEnabled: Boolean,
    metrics: AuthFormMetrics,
    scale: Float = 1f,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSubmit: () -> Unit,
    onToggleAuthMode: () -> Unit,
    onStartCheckout: () -> Unit,
    onManualCheckEmailChange: (String) -> Unit,
    onProceedPaid: () -> Unit,
    onOpenExistingAccountLogin: () -> Unit,
    onReturnToPaywall: () -> Unit,
    onStartDeviceLink: () -> Unit,
    onCancelDeviceLink: () -> Unit,
    onEmailBoundsChange: (Rect) -> Unit,
    onPasswordBoundsChange: (Rect) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (!showLoginScreen) {
            AppPaywallPreCheckoutSection(
                metrics = metrics,
                scale = scale,
                primaryHeight = metrics.primaryHeight,
                priceLabel = priceLabel,
                isWaitingPayment = isWaitingPayment,
                confirmedPaidEmail = confirmedPaidEmail,
                manualCheckEmail = manualCheckEmail,
                showManualEmailFallback = showManualEmailFallback,
                isConfirmingPayment = isConfirmingPayment,
                authError = authError,
                onGoToCheckout = onStartCheckout,
                onManualCheckEmailChange = onManualCheckEmailChange,
                onProceedPaid = onProceedPaid,
                onOpenExistingAccountLogin = onOpenExistingAccountLogin,
            )
        } else {
            TextButton(
                onClick = onReturnToPaywall,
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text(
                    text = "← Voltar para Informações do Plano",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = AuthAccentBlueSoft,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            if (verifiedPaidEmail != null) {
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
                            fontSize = (12.5f * scale).sp,
                            lineHeight = (18f * scale).sp,
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp * scale))
            } else {
                Spacer(modifier = Modifier.height(6.dp * scale))
            }

            AuthTextField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = if (isSignUp) "Gmail usado na compra PIX" else stringResource(Res.string.compose_auth_email),
                icon = Icons.Rounded.Email,
                metrics = metrics,
                scale = scale,
                readOnly = false,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    onEmailBoundsChange(coordinates.boundsInRoot())
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(modifier = Modifier.height(14.dp))

            AuthTextField(
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
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.error,
                        fontSize = (13f * scale).sp,
                        lineHeight = (18f * scale).sp,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(metrics.primaryTop))

            AuthPrimaryButton(
                text = when {
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

            Spacer(modifier = Modifier.height(metrics.toggleTop))

            AuthModeToggle(
                isSignUp = isSignUp,
                scale = scale,
                onToggleAuthMode = onToggleAuthMode,
            )

            Spacer(modifier = Modifier.height(metrics.secondaryTop))

            AuthSecondaryButton(
                text = "← Voltar para a Tela de Pagamento PIX ($priceLabel)",
                enabled = !isLoading,
                height = metrics.secondaryHeight,
                scale = scale,
                onClick = onReturnToPaywall,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(Res.string.compose_auth_store_locally),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = AuthTextMuted,
                fontSize = (13f * scale).sp,
                lineHeight = (18f * scale).sp,
                fontWeight = FontWeight.Normal,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AuthTextField(
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
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(metrics.fieldHeight)
            .background(metrics.fieldBackground, shape)
            .border(
                1.dp,
                if (readOnly) AuthAccentBlueBright.copy(alpha = 0.65f) else AuthFieldBorder,
                shape,
            )
            .padding(horizontal = metrics.fieldHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(metrics.iconSize),
            tint = if (readOnly) AuthAccentBlueSoft else AuthTextMuted,
        )
        Spacer(modifier = Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            readOnly = readOnly,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = AuthTextPrimary,
                fontSize = (16f * scale).sp,
                lineHeight = (22f * scale).sp,
                fontWeight = FontWeight.Normal,
            ),
            cursorBrush = SolidColor(AuthTextPrimary),
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
                    .size(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .clickable(onClick = onPasswordVisibilityToggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff
                    else Icons.Rounded.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(metrics.passwordIconSize),
                    tint = AuthTextSecondary,
                )
            }
        }
    }
}

@Composable
internal fun AuthPrimaryButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    height: Dp,
    scale: Float,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (enabled) AuthAccentBlueSoft.copy(alpha = 0.36f) else AuthAccentBlue.copy(alpha = 0.16f),
        ),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthPrimaryButtonBackground,
            contentColor = AuthPrimaryButtonText,
            disabledContainerColor = if (isLoading) {
                AuthPrimaryButtonBackground
            } else {
                AuthPrimaryButtonBackground.copy(alpha = 0.45f)
            },
            disabledContentColor = AuthPrimaryButtonText.copy(alpha = 0.55f),
        ),
    ) {
        if (isLoading) {
            NuvioLoadingIndicator(
                modifier = Modifier.size(20.dp * scale),
                color = AuthPrimaryButtonText,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (16f * scale).sp,
                    lineHeight = (22f * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@Composable
private fun AuthModeToggle(
    isSignUp: Boolean,
    scale: Float,
    onToggleAuthMode: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = isSignUp,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "authTogglePrompt",
        ) { signUp ->
            Text(
                text = if (signUp) {
                    stringResource(Res.string.compose_auth_already_have_account).trimEnd()
                } else {
                    stringResource(Res.string.compose_auth_dont_have_account).trimEnd()
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = AuthTextSecondary,
                    fontSize = (14f * scale).sp,
                    lineHeight = (20f * scale).sp,
                    fontWeight = FontWeight.Normal,
                ),
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        AnimatedContent(
            targetState = isSignUp,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "authToggleAction",
        ) { signUp ->
            Text(
                text = if (signUp) stringResource(Res.string.compose_auth_sign_in)
                else stringResource(Res.string.compose_auth_sign_up),
                modifier = Modifier.clickable(onClick = onToggleAuthMode),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = AuthAccentBlueSoft,
                    fontSize = (14f * scale).sp,
                    lineHeight = (20f * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@Composable
private fun AuthDivider(scale: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(AuthDividerColor),
        )
        Text(
            text = stringResource(Res.string.compose_auth_or_separator).trim(),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = AuthTextMuted,
                fontSize = (13f * scale).sp,
                lineHeight = (18f * scale).sp,
                fontWeight = FontWeight.Normal,
            ),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(AuthDividerColor),
        )
    }
}

@Composable
internal fun AuthSecondaryButton(
    text: String,
    enabled: Boolean,
    height: Dp,
    scale: Float,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AuthSecondaryButtonBorder),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthSecondaryButtonBackground,
            contentColor = AuthTextPrimary,
            disabledContainerColor = AuthSecondaryButtonBackground.copy(alpha = 0.45f),
            disabledContentColor = AuthTextPrimary.copy(alpha = 0.55f),
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = (15f * scale).sp,
                lineHeight = (21f * scale).sp,
                fontWeight = FontWeight.Medium,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

private fun Modifier.authGradientBackground(largeScreen: Boolean): Modifier = drawWithCache {
    val angleDegrees = if (largeScreen) 122.0 else 148.0
    val angleRadians = angleDegrees * PI / 180.0
    val directionX = sin(angleRadians).toFloat()
    val directionY = (-cos(angleRadians)).toFloat()
    val halfLength = (abs(size.width * directionX) + abs(size.height * directionY)) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val start = Offset(
        x = center.x - directionX * halfLength,
        y = center.y - directionY * halfLength,
    )
    val end = Offset(
        x = center.x + directionX * halfLength,
        y = center.y + directionY * halfLength,
    )
    val colorStops = if (largeScreen) {
        arrayOf(
            0f to Color(0xFF0B152A),
            0.16f to Color(0xFF0A1121),
            0.34f to Color(0xFF080B12),
            0.58f to Color(0xFF07080B),
            1f to Color(0xFF050608),
        )
    } else {
        arrayOf(
            0f to Color(0xFF0B152B),
            0.18f to Color(0xFF090E1A),
            0.38f to Color(0xFF07090D),
            0.65f to Color(0xFF06070A),
            1f to Color(0xFF050608),
        )
    }
    val brush = Brush.linearGradient(
        colorStops = colorStops,
        start = start,
        end = end,
    )
    onDrawBehind {
        drawRect(brush = brush)
    }
}

private fun LayoutCoordinates.boundsInRoot(): Rect {
    val position = positionInRoot()
    return Rect(
        left = position.x,
        top = position.y,
        right = position.x + size.width,
        bottom = position.y + size.height,
    )
}

@Composable
internal fun AppPaywallPreCheckoutSection(
    metrics: AuthFormMetrics,
    scale: Float,
    primaryHeight: Dp,
    priceLabel: String,
    isWaitingPayment: Boolean,
    confirmedPaidEmail: String?,
    manualCheckEmail: String,
    showManualEmailFallback: Boolean,
    isConfirmingPayment: Boolean,
    authError: String?,
    onGoToCheckout: () -> Unit,
    onManualCheckEmailChange: (String) -> Unit,
    onProceedPaid: () -> Unit,
    onOpenExistingAccountLogin: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0C1018),
            border = BorderStroke(1.dp, AuthAccentBlue.copy(alpha = 0.34f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp * scale),
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = AuthAccentBlue.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, AuthAccentBlueBright.copy(alpha = 0.38f)),
                ) {
                    Text(
                        text = "🔒 ACESSO RESTRITO POR ASSINATURA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AuthAccentBlueSoft,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = (10.5f * scale).sp,
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp * scale, vertical = 4.dp * scale),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp * scale))

                Text(
                    text = "Libere o Acesso ao Cine Ultimate",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = AuthTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = (19f * scale).sp,
                    ),
                )

                Spacer(modifier = Modifier.height(5.dp * scale))

                Text(
                    text = "Para assistir a milhares de filmes, séries, canais ao vivo em 4K e curtir a Sala Cinema 3D, realize o pagamento único de $priceLabel no site oficial.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF93A3BD),
                        fontSize = (12.5f * scale).sp,
                        lineHeight = (18f * scale).sp,
                    ),
                )

                Spacer(modifier = Modifier.height(14.dp * scale))

                Column(verticalArrangement = Arrangement.spacedBy(7.dp * scale)) {
                    PaywallBenefitItem("✓ Pagamento Único de $priceLabel (Sem mensalidade)", scale)
                    PaywallBenefitItem("✓ Canais Ao Vivo 4K & Full HD", scale)
                    PaywallBenefitItem("✓ Catálogo completo de Filmes e Séries", scale)
                    PaywallBenefitItem("✓ Sala Cinema 3D com som imersivo", scale)
                    PaywallBenefitItem("✓ Liberação automática após o PIX no site", scale)
                }

                Spacer(modifier = Modifier.height(14.dp * scale))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.34f)),
                ) {
                    Text(
                        text = "⚠️ Importante: Use o mesmo Gmail do pagamento no site para liberar automaticamente sua conta no app.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (12f * scale).sp,
                            lineHeight = (17f * scale).sp,
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp * scale, vertical = 9.dp * scale),
                    )
                }

                if (isWaitingPayment) {
                    Spacer(modifier = Modifier.height(12.dp * scale))
                    if (confirmedPaidEmail != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = AuthAccentBlue.copy(alpha = 0.20f),
                            border = BorderStroke(1.dp, AuthAccentBlueBright.copy(alpha = 0.62f)),
                        ) {
                            Text(
                                text = "✓ Pagamento PIX de $priceLabel confirmado para $confirmedPaidEmail! Toque abaixo para prosseguir.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF93C5FD),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (12.5f * scale).sp,
                                    lineHeight = (17.5f * scale).sp,
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp * scale, vertical = 10.dp * scale),
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0E1627),
                            border = BorderStroke(1.dp, AuthAccentBlueBright.copy(alpha = 0.36f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp * scale, vertical = 9.dp * scale),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp * scale),
                            ) {
                                NuvioLoadingIndicator(
                                    size = 18.dp * scale,
                                    color = AuthAccentBlueSoft,
                                )
                                Text(
                                    text = if (showManualEmailFallback) {
                                        "⏳ Confirmação automática aguardando resposta. Se já pagou o PIX no site, informe o mesmo Gmail abaixo para verificar e prosseguir:"
                                    } else {
                                        "⏳ Confirmando seu PIX automaticamente... Assim que o pagamento for aprovado, o botão para prosseguir aparecerá aqui."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFBFDBFE),
                                        fontSize = (12f * scale).sp,
                                        lineHeight = (17f * scale).sp,
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        if (showManualEmailFallback) {
                            Spacer(modifier = Modifier.height(10.dp * scale))
                            AuthTextField(
                                value = manualCheckEmail,
                                onValueChange = onManualCheckEmailChange,
                                placeholder = "Gmail usado no PIX do site",
                                icon = Icons.Rounded.Email,
                                metrics = metrics,
                                scale = scale,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { onProceedPaid() },
                                ),
                            )
                        }
                    }
                }

                authError?.let { errorText ->
                    Spacer(modifier = Modifier.height(10.dp * scale))
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.error,
                            fontSize = (13f * scale).sp,
                            lineHeight = (18f * scale).sp,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp * scale))

                if (isWaitingPayment && (confirmedPaidEmail != null || showManualEmailFallback)) {
                    Button(
                        onClick = onProceedPaid,
                        enabled = !isConfirmingPayment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(primaryHeight),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AuthAccentBlueSoft.copy(alpha = 0.40f)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuthAccentBlue,
                            contentColor = Color.White,
                            disabledContainerColor = AuthAccentBlue.copy(alpha = 0.55f),
                            disabledContentColor = Color.White.copy(alpha = 0.8f),
                        ),
                    ) {
                        if (isConfirmingPayment) {
                            NuvioLoadingIndicator(
                                size = 20.dp * scale,
                                color = Color.White,
                            )
                        } else {
                            Text(
                                text = "✓ Já fiz o pagamento • Prosseguir →",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (14.5f * scale).sp,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(9.dp * scale))

                    AuthSecondaryButton(
                        text = "🌐 Reabrir Site de Pagamento PIX",
                        enabled = !isConfirmingPayment,
                        height = primaryHeight,
                        scale = scale,
                        onClick = onGoToCheckout,
                    )
                } else {
                    Button(
                        onClick = onGoToCheckout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(primaryHeight),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AuthAccentBlueSoft.copy(alpha = 0.40f)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuthAccentBlue,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = if (isWaitingPayment) "🌐 Reabrir Site de Pagamento PIX" else "⚡ Pagar $priceLabel no Site e Liberar",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = (14.5f * scale).sp,
                            ),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp * scale))

        TextButton(onClick = onOpenExistingAccountLogin) {
            Text(
                text = "Já possui conta e senha cadastradas? Fazer Login",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = AuthAccentBlueSoft,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (13f * scale).sp,
                ),
            )
        }
    }
}

@Composable
private fun PaywallBenefitItem(text: String, scale: Float) {
    val cleanLabel = text.removePrefix("✓ ").removePrefix("✓").trimStart()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = "✓",
            style = MaterialTheme.typography.bodySmall.copy(
                color = AuthAccentBlueSoft,
                fontWeight = FontWeight.Bold,
                fontSize = (12.5f * scale).sp,
            ),
        )
        Text(
            text = cleanLabel,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFE2E8F0),
                fontWeight = FontWeight.Medium,
                fontSize = (12.5f * scale).sp,
            ),
        )
    }
}
