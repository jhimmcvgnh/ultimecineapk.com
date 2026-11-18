package com.nuvio.app.core.auth

import co.touchlab.kermit.Logger
import com.nuvio.app.core.network.SupabaseProvider
import com.nuvio.app.core.storage.LocalAccountDataCleaner
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString

object AuthRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("AuthRepository")

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _deviceTrialLock = MutableStateFlow<PersistedDeviceTrialLock?>(null)
    internal val deviceTrialLock: StateFlow<PersistedDeviceTrialLock?> = _deviceTrialLock.asStateFlow()

    private var initialized = false
    private var explicitSignOutRequested = false
    private var sessionStatusJob: Job? = null
    private var validatedRemoteUserId: String? = null

    private fun currentTimeMillis(): Long =
        kotlin.time.Clock.System.now().toEpochMilliseconds()

    internal fun refreshDeviceTrialLock(): PersistedDeviceTrialLock? {
        val loaded = CineSessionPersistence.loadDeviceTrialLock() ?: run {
            _deviceTrialLock.value = null
            return null
        }
        val expiresAt = loaded.trialExpiresAtEpochMs
        val now = currentTimeMillis()
        val updated = if (!loaded.isPaidUpgraded && expiresAt != null && expiresAt > 0L) {
            val remainingMs = expiresAt - now
            if (remainingMs <= 0L) {
                loaded.copy(isTrialExpired = true, trialDaysRemaining = 0)
            } else {
                val days = ((remainingMs + 86_400_000L - 1L) / 86_400_000L).toInt().coerceAtLeast(1)
                loaded.copy(isTrialExpired = false, trialDaysRemaining = days)
            }
        } else {
            loaded
        }
        if (updated != loaded) {
            CineSessionPersistence.saveDeviceTrialLock(updated)
        }
        _deviceTrialLock.value = updated
        return updated
    }

    internal suspend fun syncDeviceTrialFromServer(): PersistedDeviceTrialLock? {
        CineAuthClient.checkDeviceTrialStatus()
        return refreshDeviceTrialLock()
    }

    fun initialize() {
        if (initialized) return
        initialized = true

        AuthStorage.clearAnonymousUserId()
        refreshDeviceTrialLock()
        val initialHostSession = CineSessionPersistence.loadHostSession()
        val initialSupaSession = runCatching { SupabaseProvider.client.auth.currentSessionOrNull() }.getOrNull()
        if (initialHostSession != null) {
            val expiresAt = initialHostSession.trialExpiresAtEpochMs
            if (initialHostSession.isTrial && expiresAt != null && expiresAt > 0L && currentTimeMillis() >= expiresAt) {
                CineSessionPersistence.saveDeviceTrialLock(
                    PersistedDeviceTrialLock(
                        boundTrialEmail = initialHostSession.email,
                        isTrialExpired = true,
                        trialExpiresAtEpochMs = expiresAt,
                        trialDaysRemaining = 0,
                        isPaidUpgraded = false,
                    ),
                )
                refreshDeviceTrialLock()
                _state.value = AuthState.Unauthenticated
                scope.launch {
                    clearLocalSessionAfterRemoteInvalidation()
                    _error.value = "Seu teste grátis de 20 dias terminou! Para voltar a assistir, realize o pagamento de ${CineAuthClient.currentFormattedPrice()} no site oficial usando obrigatoriamente o Gmail ${initialHostSession.email}."
                }
            } else {
                _state.value = AuthState.Authenticated(
                    userId = initialHostSession.userId,
                    email = initialHostSession.email,
                    isAnonymous = false,
                )
                scope.launch {
                    val restoredOk = validateRemoteSession(
                        userId = initialHostSession.userId,
                        email = initialHostSession.email,
                        forceEntitlementCheck = initialHostSession.isTrial,
                    )
                    if (restoredOk) {
                        refreshAccessToken()
                    }
                }
            }
        } else if (initialSupaSession == null) {
            _state.value = AuthState.Unauthenticated
            scope.launch {
                syncDeviceTrialFromServer()
            }
        }

        sessionStatusJob = scope.launch {
            SupabaseProvider.client.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        val saved = CineSessionPersistence.loadHostSession()
                        val userId = user?.id?.takeIf { it.isNotBlank() } ?: saved?.userId.orEmpty()
                        val email = user?.email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
                            ?: saved?.email.orEmpty()
                        if (!validateRemoteSession(userId, email, forceEntitlementCheck = saved?.isTrial == true)) return@collect
                        val latestSaved = CineSessionPersistence.loadHostSession()
                        explicitSignOutRequested = false
                        CineSessionPersistence.saveHostSession(
                            SavedHostSession(
                                userId = userId,
                                email = email,
                                accessToken = status.session.accessToken.ifBlank { latestSaved?.accessToken ?: saved?.accessToken.orEmpty() },
                                refreshToken = status.session.refreshToken.ifBlank { latestSaved?.refreshToken ?: saved?.refreshToken.orEmpty() },
                                paidVerified = true,
                                isTrial = latestSaved?.isTrial ?: saved?.isTrial ?: false,
                                trialExpiresAtEpochMs = latestSaved?.trialExpiresAtEpochMs ?: saved?.trialExpiresAtEpochMs,
                                trialDaysRemaining = latestSaved?.trialDaysRemaining ?: saved?.trialDaysRemaining ?: 0,
                            ),
                        )
                        _state.value = AuthState.Authenticated(
                            userId = userId,
                            email = email,
                            isAnonymous = false,
                        )
                    }
                    is SessionStatus.NotAuthenticated -> {
                        val saved = CineSessionPersistence.loadHostSession()
                        if (!explicitSignOutRequested && saved != null) {
                            _state.value = AuthState.Authenticated(
                                userId = saved.userId,
                                email = saved.email,
                                isAnonymous = false,
                            )
                        } else {
                            _state.value = AuthState.Unauthenticated
                        }
                    }
                    is SessionStatus.Initializing -> {
                        if (CineSessionPersistence.loadHostSession() == null) {
                            _state.value = AuthState.Loading
                        }
                    }
                    is SessionStatus.RefreshFailure -> {
                        val saved = CineSessionPersistence.loadHostSession()
                        if (!explicitSignOutRequested && saved != null) {
                            log.w { "Supabase session refresh failed; keeping saved host login for ${saved.email}" }
                            _state.value = AuthState.Authenticated(
                                userId = saved.userId,
                                email = saved.email,
                                isAnonymous = false,
                            )
                        } else {
                            _state.value = AuthState.Unauthenticated
                        }
                    }
                }
            }
        }
    }

    internal fun currentAccessToken(): String? =
        runCatching { SupabaseProvider.client.auth.currentAccessTokenOrNull() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: CineSessionPersistence.loadHostSession()?.accessToken?.takeIf { it.isNotBlank() }

    internal suspend fun refreshAccessToken(): String? {
        runCatching { SupabaseProvider.client.auth.refreshCurrentSession() }
        val supaSession = runCatching { SupabaseProvider.client.auth.currentSessionOrNull() }.getOrNull()
        val saved = CineSessionPersistence.loadHostSession()
        if (supaSession != null && supaSession.accessToken.isNotBlank()) {
            val uid = supaSession.user?.id?.takeIf { it.isNotBlank() } ?: saved?.userId.orEmpty()
            val email = supaSession.user?.email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
                ?: saved?.email.orEmpty()
            if (uid.isNotBlank() && email.isNotBlank()) {
                CineSessionPersistence.saveHostSession(
                    SavedHostSession(
                        userId = uid,
                        email = email,
                        accessToken = supaSession.accessToken,
                        refreshToken = supaSession.refreshToken.ifBlank { saved?.refreshToken.orEmpty() },
                        paidVerified = true,
                        isTrial = saved?.isTrial ?: false,
                        trialExpiresAtEpochMs = saved?.trialExpiresAtEpochMs,
                        trialDaysRemaining = saved?.trialDaysRemaining ?: 0,
                    ),
                )
            }
            return supaSession.accessToken
        }

        val refreshToken = saved?.refreshToken?.takeIf { it.isNotBlank() } ?: return saved?.accessToken
        val refreshed = CineAuthClient.refreshSupabaseToken(refreshToken).getOrNull() ?: return saved.accessToken
        CineSessionPersistence.saveHostSession(
            saved.copy(
                accessToken = refreshed.access_token,
                refreshToken = refreshed.refresh_token.ifBlank { saved.refreshToken },
            ),
        )
        return refreshed.access_token
    }

    internal suspend fun revalidateActiveEntitlement(): Boolean {
        val saved = CineSessionPersistence.loadHostSession() ?: return false
        val expiresAt = saved.trialExpiresAtEpochMs
        if (saved.isTrial && expiresAt != null && expiresAt > 0L && currentTimeMillis() >= expiresAt) {
            CineSessionPersistence.saveDeviceTrialLock(
                PersistedDeviceTrialLock(
                    boundTrialEmail = saved.email,
                    isTrialExpired = true,
                    trialExpiresAtEpochMs = expiresAt,
                    trialDaysRemaining = 0,
                    isPaidUpgraded = false,
                ),
            )
            refreshDeviceTrialLock()
            clearLocalSessionAfterRemoteInvalidation()
            _error.value = "Seu teste grátis de 20 dias terminou! Para voltar a assistir, realize o pagamento único de ${CineAuthClient.currentFormattedPrice()} no site oficial usando obrigatoriamente o Gmail ${saved.email}."
            return false
        }
        return validateRemoteSession(
            userId = saved.userId,
            email = saved.email,
            forceEntitlementCheck = true,
        )
    }

    private suspend fun validateRemoteSession(
        userId: String,
        email: String,
        forceEntitlementCheck: Boolean = false,
    ): Boolean {
        if (userId.isBlank() || email.isBlank()) {
            clearLocalSessionAfterRemoteInvalidation()
            return false
        }
        if (!forceEntitlementCheck && validatedRemoteUserId == userId) return true

        val saved = CineSessionPersistence.loadHostSession()
        val localExpiresAt = saved?.trialExpiresAtEpochMs
        if (saved?.isTrial == true && localExpiresAt != null && localExpiresAt > 0L && currentTimeMillis() >= localExpiresAt) {
            val check = CineAuthClient.checkEmail(email).getOrNull()
            refreshDeviceTrialLock()
            if (check == null || !check.entitlementActive) {
                clearLocalSessionAfterRemoteInvalidation()
                _error.value = "Seu teste grátis de 20 dias para $email encerrou! Para voltar a assistir, realize o pagamento único de ${CineAuthClient.currentFormattedPrice()} no site oficial usando obrigatoriamente o Gmail $email."
                return false
            }
        }

        if (SupabaseProvider.client.auth.currentSessionOrNull() != null) {
            val sessionValid = runCatching {
                SupabaseProvider.client.auth.retrieveUserForCurrentSession(false)
                true
            }.getOrElse { e ->
                if (isExplicitMissingAccountError(e)) {
                    log.w(e) { "Stored Supabase session no longer belongs to an active account; clearing local auth" }
                    clearLocalSessionAfterRemoteInvalidation()
                    false
                } else {
                    log.w(e) { "Unable to validate stored Supabase session; keeping cached auth state" }
                    true
                }
            }
            if (!sessionValid) return false
        }

        val entitlementCheck = CineAuthClient.checkEmail(email)
        refreshDeviceTrialLock()
        if (entitlementCheck.isSuccess) {
            val info = entitlementCheck.getOrThrow()
            if (!info.entitlementActive) {
                log.w { "Account $email does not have an active entitlement; clearing session" }
                clearLocalSessionAfterRemoteInvalidation()
                refreshDeviceTrialLock()
                _error.value = if (info.isTrialExpired) {
                    "Seu teste grátis de 20 dias para $email encerrou! Para voltar a assistir e consumir conteúdos, realize o pagamento único de ${CineAuthClient.currentFormattedPrice()} no site oficial usando obrigatoriamente o mesmo Gmail ($email)."
                } else {
                    "Acesso negado: Este Gmail ($email) não possui pagamento aprovado de ${CineAuthClient.currentFormattedPrice()}."
                }
                return false
            }
            if (saved != null && saved.email.equals(email, ignoreCase = true)) {
                CineSessionPersistence.saveHostSession(
                    saved.copy(
                        isTrial = info.isTrial && !info.isPaid,
                        trialExpiresAtEpochMs = info.trialExpiresAtMs,
                        trialDaysRemaining = info.trialDaysRemaining,
                    ),
                )
            }
            validatedRemoteUserId = userId
        }
        return true
    }

    fun signInAnonymously() {
        _error.value = "Acesso anônimo não permitido. Realize o pagamento de ${CineAuthClient.currentFormattedPrice()} no site oficial para liberar seu acesso."
        AuthStorage.clearAnonymousUserId()
        _state.value = AuthState.Unauthenticated
    }

    private suspend fun finishHostAuthentication(
        backend: CineAuthSuccess,
        normalizedEmail: String,
        password: String,
        isSignUp: Boolean,
    ): Result<Unit> = runCatching {
        val backendUserId = backend.user.id
        validatedRemoteUserId = backendUserId

        runCatching {
            SupabaseProvider.client.auth.signInWith(Email) {
                this.email = normalizedEmail
                this.password = password
            }
        }.onFailure { e ->
            log.w(e) { "Native Supabase session signInWith fell back to verified backend token" }
        }

        val currentSession = runCatching { SupabaseProvider.client.auth.currentSessionOrNull() }.getOrNull()
        val userId = currentSession?.user?.id?.takeIf { it.isNotBlank() } ?: backendUserId
        val finalEmail = currentSession?.user?.email?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: normalizedEmail
        val accessToken = currentSession?.accessToken?.takeIf { it.isNotBlank() } ?: backend.token
        val refreshToken = currentSession?.refreshToken?.takeIf { it.isNotBlank() }
            ?: backend.refreshToken.orEmpty()

        validatedRemoteUserId = userId
        CineSessionPersistence.saveHostSession(
            SavedHostSession(
                userId = userId,
                email = finalEmail,
                accessToken = accessToken,
                refreshToken = refreshToken,
                paidVerified = true,
                isTrial = backend.user.isTrial,
                trialExpiresAtEpochMs = backend.user.trialExpiresAtMs,
                trialDaysRemaining = backend.user.trialDaysRemaining,
            ),
        )
        refreshDeviceTrialLock()
        CineSessionPersistence.clearPaymentState()
        if (isSignUp) {
            CineSessionPersistence.markAccountJustCreated(userId)
        }
        _state.value = AuthState.Authenticated(
            userId = userId,
            email = finalEmail,
            isAnonymous = false,
        )
        Unit
    }.onFailure { e ->
        log.e(e) { "Host session initialization failed" }
        validatedRemoteUserId = null
        _error.value = e.message ?: "Não foi possível iniciar sessão. Tente fazer login."
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        isTrial: Boolean = false,
    ): Result<Unit> {
        _error.value = null
        explicitSignOutRequested = false
        AuthStorage.clearAnonymousUserId()
        val normalizedEmail = email.trim().lowercase()

        val backendResult = CineAuthClient.register(normalizedEmail, password, isTrial = isTrial)
        refreshDeviceTrialLock()
        if (backendResult.isFailure) {
            val ex = backendResult.exceptionOrNull()
            val msg = ex?.message ?: if (isTrial) {
                "Não foi possível ativar o teste grátis de 20 dias neste dispositivo."
            } else {
                "Acesso negado: O Gmail $normalizedEmail não possui pagamento PIX de ${CineAuthClient.currentFormattedPrice()} aprovado."
            }
            _error.value = msg
            return Result.failure(ex ?: Exception(msg))
        }

        return finishHostAuthentication(
            backend = backendResult.getOrThrow(),
            normalizedEmail = normalizedEmail,
            password = password,
            isSignUp = true,
        )
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        _error.value = null
        explicitSignOutRequested = false
        AuthStorage.clearAnonymousUserId()
        val normalizedEmail = email.trim().lowercase()

        val backendResult = CineAuthClient.login(normalizedEmail, password)
        refreshDeviceTrialLock()
        if (backendResult.isFailure) {
            val ex = backendResult.exceptionOrNull()
            val msg = ex?.message ?: "Gmail ou senha incorretos ou pagamento não encontrado."
            _error.value = msg
            return Result.failure(ex ?: Exception(msg))
        }

        return finishHostAuthentication(
            backend = backendResult.getOrThrow(),
            normalizedEmail = normalizedEmail,
            password = password,
            isSignUp = false,
        )
    }

    suspend fun signOut(): Result<Unit> {
        _error.value = null
        explicitSignOutRequested = true
        val anonymousRead = runCatching { AuthStorage.loadAnonymousUserId() }
        val wasAnonymous = anonymousRead.getOrNull() != null
        val anonymousClear = runCatching { AuthStorage.clearAnonymousUserId() }
        CineSessionPersistence.clearHostSession()
        CineSessionPersistence.clearPaymentState()
        validatedRemoteUserId = null
        val remoteSignOut = if (wasAnonymous) {
            Result.success(Unit)
        } else {
            runCatching { SupabaseProvider.client.auth.signOut() }
        }

        val fallbackSessionClear = if (remoteSignOut.isFailure) {
            runCatching { SupabaseProvider.client.auth.clearSession() }
                .onFailure { error -> log.w(error) { "Failed to clear Supabase session after sign-out failure" } }
        } else {
            Result.success(Unit)
        }
        val localCleanup = runCatching { LocalAccountDataCleaner.wipe() }
        _state.value = AuthState.Unauthenticated

        val failure = anonymousRead.exceptionOrNull()
            ?: anonymousClear.exceptionOrNull()
            ?: remoteSignOut.exceptionOrNull()
            ?: fallbackSessionClear.exceptionOrNull()
            ?: localCleanup.exceptionOrNull()
        val cancellation = remoteSignOut.exceptionOrNull() as? CancellationException
            ?: fallbackSessionClear.exceptionOrNull() as? CancellationException
        if (cancellation != null) throw cancellation
        return if (failure == null) {
            Result.success(Unit)
        } else {
            log.e(failure) { "Sign-out did not complete cleanly; all local cleanup steps were attempted" }
            _error.value = failure.message ?: runCatching {
                getString(Res.string.auth_sign_out_failed)
            }.getOrDefault("Sign out failed")
            Result.failure(failure)
        }
    }

    suspend fun prepareForServerSwitch(): Result<Unit> {
        _error.value = null
        val anonymousClear = runCatching { AuthStorage.clearAnonymousUserId() }
        validatedRemoteUserId = null
        val sessionClear = runCatching { SupabaseProvider.client.auth.clearSession() }
        _state.value = AuthState.Unauthenticated
        val failure = anonymousClear.exceptionOrNull() ?: sessionClear.exceptionOrNull()
        val cancellation = sessionClear.exceptionOrNull() as? CancellationException
        if (cancellation != null) throw cancellation
        return if (failure == null) Result.success(Unit) else Result.failure(failure)
    }

    fun reinitialize() {
        sessionStatusJob?.cancel()
        sessionStatusJob = null
        initialized = false
        validatedRemoteUserId = null
        _state.value = AuthState.Loading
        initialize()
    }

    suspend fun signOutIfSessionInvalid(error: Throwable, source: String): Boolean {
        if (!isInvalidRemoteSessionError(error)) return false
        if (!isExplicitMissingAccountError(error)) {
            val refreshed = refreshAccessToken()
            if (!refreshed.isNullOrBlank()) {
                log.w(error) { "$source got auth error, but token refresh succeeded; preserving host session" }
                return false
            }
            val savedEmail = CineSessionPersistence.loadHostSession()?.email
            if (!savedEmail.isNullOrBlank()) {
                val ent = CineAuthClient.checkEmail(savedEmail).getOrNull()
                if (ent == null || (ent.entitlementActive && ent.hasAccount)) {
                    log.w(error) { "$source session check inconclusive/valid for $savedEmail; preserving host session" }
                    return false
                }
            }
        }

        log.w(error) { "$source failed because the current Supabase account/session is no longer valid; clearing local auth" }
        clearLocalSessionAfterRemoteInvalidation()
        return true
    }

    private suspend fun clearLocalSessionAfterRemoteInvalidation() {
        _error.value = null
        AuthStorage.clearAnonymousUserId()
        CineSessionPersistence.clearHostSession()
        validatedRemoteUserId = null
        runCatching {
            SupabaseProvider.client.auth.clearSession()
        }.onFailure { e ->
            log.w(e) { "Failed to clear Supabase session after remote invalidation; continuing local reset" }
        }
        val localCleanup = runCatching { LocalAccountDataCleaner.wipe() }
        _state.value = AuthState.Unauthenticated
        localCleanup.onFailure { error ->
            log.e(error) { "Local account cleanup failed after remote session invalidation" }
        }
    }

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        _error.value = null
        SupabaseProvider.client.functions.invoke("delete-account")
        SupabaseProvider.client.auth.signOut()
        CineSessionPersistence.clearHostSession()
        CineSessionPersistence.clearPaymentState()
        validatedRemoteUserId = null
        try {
            LocalAccountDataCleaner.wipe()
        } finally {
            _state.value = AuthState.Unauthenticated
        }
    }.onFailure { e ->
        log.e(e) { "Account deletion failed" }
        _error.value = e.message ?: getString(Res.string.auth_account_deletion_failed)
    }

    fun clearError() {
        _error.value = null
    }

    private fun isExplicitMissingAccountError(error: Throwable): Boolean {
        val restError = error.findCause<RestException>()
        val message = buildString {
            append(error.message.orEmpty())
            if (restError != null) {
                append(' ')
                append(restError.error)
                append(' ')
                append(restError.description)
            }
        }.lowercase()

        return (
            "user" in message &&
                ("does not exist" in message || "not found" in message || "deleted" in message || "banned" in message)
            ) || (
            "foreign key" in message &&
                ("auth.users" in message || "user_id" in message)
            )
    }

    private fun isInvalidRemoteSessionError(error: Throwable): Boolean {
        if (isExplicitMissingAccountError(error)) return true
        val restError = error.findCause<RestException>()
        if (restError?.statusCode == 401 || restError?.statusCode == 403) return true

        val message = buildString {
            append(error.message.orEmpty())
            if (restError != null) {
                append(' ')
                append(restError.error)
                append(' ')
                append(restError.description)
            }
        }.lowercase()

        return "jwt" in message &&
            ("invalid" in message || "expired" in message || "malformed" in message)
    }

    private inline fun <reified T : Throwable> Throwable.findCause(): T? {
        var current: Throwable? = this
        while (current != null) {
            if (current is T) return current
            current = current.cause
        }
        return null
    }

    private fun Throwable.safeAuthErrorDescription(): String? =
        findCause<AuthRestException>()
            ?.errorDescription
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: findCause<RestException>()
                ?.description
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
}
