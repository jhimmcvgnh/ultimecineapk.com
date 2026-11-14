package com.nuvio.app.core.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class SavedHostSession(
    val userId: String,
    val email: String,
    val accessToken: String = "",
    val refreshToken: String = "",
    val paidVerified: Boolean = true,
)

@Serializable
internal data class PersistedPaymentState(
    val pendingCheckoutSessionId: String? = null,
    val confirmedPaidEmail: String? = null,
    val confirmedPaidHasAccount: Boolean = false,
    val confirmedPaymentId: String? = null,
    val verifiedPaidEmail: String? = null,
    val isSignUpMode: Boolean = true,
)

internal object CineSessionPersistence {
    private const val KEY_HOST_SESSION = "cine_host_session_v1"
    private const val KEY_PAYMENT_STATE = "cine_pending_payment_v1"
    private const val KEY_ACCOUNT_JUST_CREATED = "cine_account_just_created_v1"

    private val json = Json { ignoreUnknownKeys = true }

    fun loadHostSession(): SavedHostSession? {
        val raw = AuthStorage.loadValue(KEY_HOST_SESSION).orEmpty().trim()
        if (raw.isEmpty()) return null
        return runCatching { json.decodeFromString<SavedHostSession>(raw) }
            .getOrNull()
            ?.takeIf { it.userId.isNotBlank() && it.email.isNotBlank() && it.paidVerified }
    }

    fun saveHostSession(session: SavedHostSession) {
        val normalized = session.copy(
            userId = session.userId.trim(),
            email = session.email.trim().lowercase(),
        )
        if (normalized.userId.isBlank() || normalized.email.isBlank()) return
        AuthStorage.saveValue(KEY_HOST_SESSION, json.encodeToString(normalized))
    }

    fun clearHostSession() {
        AuthStorage.saveValue(KEY_HOST_SESSION, null)
    }

    fun loadPaymentState(): PersistedPaymentState? {
        val raw = AuthStorage.loadPaymentValue(KEY_PAYMENT_STATE).orEmpty().trim()
        if (raw.isEmpty()) return null
        return runCatching { json.decodeFromString<PersistedPaymentState>(raw) }.getOrNull()
    }

    fun savePaymentState(state: PersistedPaymentState) {
        val normalized = state.copy(
            confirmedPaidEmail = state.confirmedPaidEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() },
            verifiedPaidEmail = state.verifiedPaidEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() },
            pendingCheckoutSessionId = state.pendingCheckoutSessionId?.trim()?.takeIf { it.isNotBlank() },
            confirmedPaymentId = state.confirmedPaymentId?.trim()?.takeIf { it.isNotBlank() },
        )
        if (
            normalized.pendingCheckoutSessionId == null &&
            normalized.confirmedPaidEmail == null &&
            normalized.verifiedPaidEmail == null
        ) {
            clearPaymentState()
            return
        }
        AuthStorage.savePaymentValue(KEY_PAYMENT_STATE, json.encodeToString(normalized))
    }

    fun clearPaymentState() {
        AuthStorage.savePaymentValue(KEY_PAYMENT_STATE, null)
    }

    fun markAccountJustCreated(userId: String) {
        AuthStorage.saveValue(KEY_ACCOUNT_JUST_CREATED, userId.trim())
    }

    fun consumeAccountJustCreated(userId: String): Boolean {
        val pending = AuthStorage.loadValue(KEY_ACCOUNT_JUST_CREATED)?.trim()
        if (pending.isNullOrBlank() || pending != userId.trim()) return false
        AuthStorage.saveValue(KEY_ACCOUNT_JUST_CREATED, null)
        return true
    }
}
