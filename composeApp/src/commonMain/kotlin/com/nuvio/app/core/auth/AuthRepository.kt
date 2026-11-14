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

    private var initialized = false
    private var explicitSignOutRequested = false
    private var sessionStatusJob: Job? = null
    private var validatedRemoteUserId: String? = null

    fun initialize() {
        if (initialized) return
        initialized = true

        AuthStorage.clearAnonymousUserId()
        if (SupabaseProvider.client.auth.currentSessionOrNull() == null) {
            _state.value = AuthState.Unauthenticated
        }

        sessionStatusJob = scope.launch {
            SupabaseProvider.client.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        val userId = user?.id.orEmpty()
                        val email = user?.email?.trim()?.lowercase().orEmpty()
                        if (!validateRemoteSession(userId, email)) return@collect
                        explicitSignOutRequested = false
                        _state.value = AuthState.Authenticated(
                            userId = userId,
                            email = email,
                            isAnonymous = false,
                        )
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _state.value = AuthState.Unauthenticated
                    }
                    is SessionStatus.Initializing -> {
                        _state.value = AuthState.Loading
                    }
                    is SessionStatus.RefreshFailure -> {
                        _state.value = AuthState.Unauthenticated
                    }
                }
            }
        }
    }

    private suspend fun validateRemoteSession(userId: String, email: String): Boolean {
        if (userId.isBlank() || email.isBlank()) {
            clearLocalSessionAfterRemoteInvalidation()
            return false
        }
        if (validatedRemoteUserId == userId) return true

        val sessionValid = runCatching {
            SupabaseProvider.client.auth.retrieveUserForCurrentSession(false)
            true
        }.getOrElse { e ->
            if (isInvalidRemoteSessionError(e)) {
                log.w(e) { "Stored Supabase session no longer belongs to an active account; clearing local auth" }
                clearLocalSessionAfterRemoteInvalidation()
                false
            } else {
                log.w(e) { "Unable to validate stored Supabase session; keeping cached auth state" }
                true
            }
        }
        if (!sessionValid) return false

        val entitlementCheck = CineAuthClient.checkEmail(email)
        if (entitlementCheck.isSuccess) {
            val info = entitlementCheck.getOrThrow()
            if (!info.entitlementActive) {
                log.w { "Account $email does not have an active PIX entitlement; clearing session" }
                clearLocalSessionAfterRemoteInvalidation()
                _error.value = "Acesso negado: Este Gmail ($email) não possui pagamento aprovado de R$ 10,00."
                return false
            }
            validatedRemoteUserId = userId
        }
        return true
    }

    fun signInAnonymously() {
        _error.value = "Acesso anônimo não permitido. Realize o pagamento de R$ 10,00 no site oficial para liberar seu acesso."
        AuthStorage.clearAnonymousUserId()
        _state.value = AuthState.Unauthenticated
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        _error.value = null
        explicitSignOutRequested = false
        AuthStorage.clearAnonymousUserId()
        val normalizedEmail = email.trim().lowercase()

        val backendResult = CineAuthClient.register(normalizedEmail, password)
        if (backendResult.isFailure) {
            val ex = backendResult.exceptionOrNull()
            val msg = ex?.message ?: "Acesso negado: O Gmail $normalizedEmail não possui pagamento PIX de R$ 10,00 aprovado."
            _error.value = msg
            return Result.failure(ex ?: Exception(msg))
        }

        return runCatching {
            val backendUserId = backendResult.getOrThrow().user.id
            validatedRemoteUserId = backendUserId
            SupabaseProvider.client.auth.signInWith(Email) {
                this.email = normalizedEmail
                this.password = password
            }
            val currentSession = SupabaseProvider.client.auth.currentSessionOrNull()
                ?: throw Exception("Sessão não iniciada após criação da conta.")
            val userId = currentSession.user?.id ?: backendUserId
            validatedRemoteUserId = userId
            _state.value = AuthState.Authenticated(
                userId = userId,
                email = currentSession.user?.email ?: normalizedEmail,
                isAnonymous = false,
            )
            Unit
        }.onFailure { e ->
            log.e(e) { "Email sign-up session initialization failed" }
            validatedRemoteUserId = null
            _error.value = e.message ?: "Não foi possível iniciar sessão. Tente fazer login."
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        _error.value = null
        explicitSignOutRequested = false
        AuthStorage.clearAnonymousUserId()
        val normalizedEmail = email.trim().lowercase()

        val backendResult = CineAuthClient.login(normalizedEmail, password)
        if (backendResult.isFailure) {
            val ex = backendResult.exceptionOrNull()
            val msg = ex?.message ?: "Gmail ou senha incorretos ou pagamento não encontrado."
            _error.value = msg
            return Result.failure(ex ?: Exception(msg))
        }

        return runCatching {
            val backendUserId = backendResult.getOrThrow().user.id
            validatedRemoteUserId = backendUserId
            SupabaseProvider.client.auth.signInWith(Email) {
                this.email = normalizedEmail
                this.password = password
            }
            val currentSession = SupabaseProvider.client.auth.currentSessionOrNull()
                ?: throw Exception("Sessão não iniciada.")
            val userId = currentSession.user?.id ?: backendUserId
            validatedRemoteUserId = userId
            _state.value = AuthState.Authenticated(
                userId = userId,
                email = currentSession.user?.email ?: normalizedEmail,
                isAnonymous = false,
            )
            Unit
        }.onFailure { e ->
            log.e(e) { "Email sign-in failed" }
            validatedRemoteUserId = null
            _error.value = e.message ?: "Gmail ou senha incorretos."
        }
    }

    suspend fun signOut(): Result<Unit> {
        _error.value = null
        explicitSignOutRequested = true
        val anonymousRead = runCatching { AuthStorage.loadAnonymousUserId() }
        val wasAnonymous = anonymousRead.getOrNull() != null
        val anonymousClear = runCatching { AuthStorage.clearAnonymousUserId() }
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

        log.w(error) { "$source failed because the current Supabase account/session is no longer valid; clearing local auth" }
        clearLocalSessionAfterRemoteInvalidation()
        return true
    }

    private suspend fun clearLocalSessionAfterRemoteInvalidation() {
        _error.value = null
        AuthStorage.clearAnonymousUserId()
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

    private fun isInvalidRemoteSessionError(error: Throwable): Boolean {
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

        return (
            "jwt" in message &&
                ("invalid" in message || "expired" in message || "malformed" in message)
            ) || (
            "user" in message &&
                ("does not exist" in message || "not found" in message || "deleted" in message)
            ) || (
            "foreign key" in message &&
                ("auth.users" in message || "user_id" in message)
            )
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
