package com.nuvio.app.core.auth

import com.nuvio.app.core.network.CineBackendConfig
import com.nuvio.app.core.network.createApiHttpClient
import com.nuvio.app.core.network.readBoundedResponseBody
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal data class CheckEmailResult(
    val email: String,
    val entitlementActive: Boolean,
    val hasAccount: Boolean,
)

internal data class SessionStatusResult(
    val approved: Boolean,
    val email: String,
    val hasAccount: Boolean,
)

internal data class CineAuthUser(
    val id: String,
    val email: String,
    val name: String,
    val subscriptionActive: Boolean,
)

internal data class CineAuthSuccess(
    val token: String,
    val user: CineAuthUser,
)

internal class CineAuthException(
    val code: String?,
    override val message: String,
) : Exception(message)

@Serializable
private data class CheckEmailApiResponse(
    val ok: Boolean = false,
    val email: String = "",
    val entitlementActive: Boolean = false,
    val hasAccount: Boolean = false,
    val error: String? = null,
)

@Serializable
private data class SessionStatusApiResponse(
    val ok: Boolean = false,
    val approved: Boolean = false,
    val email: String = "",
    val hasAccount: Boolean = false,
    val error: String? = null,
)

@Serializable
private data class AuthUserDto(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val subscriptionActive: Boolean = false,
)

@Serializable
private data class AuthApiResponse(
    val ok: Boolean = false,
    val code: String? = null,
    val error: String? = null,
    val token: String? = null,
    val user: AuthUserDto? = null,
)

internal object CineAuthClient {
    private val http: HttpClient by lazy { createApiHttpClient() }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkEmail(email: String, paymentId: String? = null): Result<CheckEmailResult> = runCatching {
        val normalized = email.trim().lowercase()
        val payload = buildJsonObject {
            put("email", normalized)
            paymentId?.trim()?.takeIf { it.isNotBlank() }?.let { put("paymentId", it) }
        }.toString()

        val response = http.post("${CineBackendConfig.baseUrl}/api/auth/check-email") {
            header("Content-Type", "application/json")
            header("Accept", "application/json")
            setBody(payload)
        }
        val rawBody = readBoundedResponseBody(response.bodyAsChannel(), response.contentLength())
        val parsed = json.decodeFromString<CheckEmailApiResponse>(rawBody)
        if (response.status.value !in 200..299 || !parsed.ok) {
            throw CineAuthException(null, parsed.error ?: "Falha ao verificar pagamento do Gmail.")
        }
        CheckEmailResult(
            email = parsed.email.ifBlank { normalized },
            entitlementActive = parsed.entitlementActive,
            hasAccount = parsed.hasAccount,
        )
    }

    suspend fun checkSessionStatus(sessionId: String): Result<SessionStatusResult> = runCatching {
        val encoded = sessionId.trim().encodeURLParameter()
        val response = http.get("${CineBackendConfig.baseUrl}/api/pix/session-status?sessionId=$encoded") {
            header("Accept", "application/json")
        }
        val rawBody = readBoundedResponseBody(response.bodyAsChannel(), response.contentLength())
        val parsed = json.decodeFromString<SessionStatusApiResponse>(rawBody)
        if (response.status.value !in 200..299 || !parsed.ok) {
            throw CineAuthException(null, parsed.error ?: "Não foi possível consultar o pagamento.")
        }
        SessionStatusResult(
            approved = parsed.approved,
            email = parsed.email.trim().lowercase(),
            hasAccount = parsed.hasAccount,
        )
    }

    suspend fun register(email: String, password: String): Result<CineAuthSuccess> = runCatching {
        val normalized = email.trim().lowercase()
        val payload = buildJsonObject {
            put("email", normalized)
            put("password", password)
        }.toString()

        val response = http.post("${CineBackendConfig.baseUrl}/api/auth/register") {
            header("Content-Type", "application/json")
            header("Accept", "application/json")
            setBody(payload)
        }
        val rawBody = readBoundedResponseBody(response.bodyAsChannel(), response.contentLength())
        val parsed = json.decodeFromString<AuthApiResponse>(rawBody)
        if (response.status.value !in 200..299 || !parsed.ok || parsed.token.isNullOrBlank() || parsed.user == null) {
            throw CineAuthException(
                code = parsed.code,
                message = parsed.error ?: "Não foi possível criar sua conta. Verifique se o PIX foi aprovado.",
            )
        }
        if (!parsed.user.subscriptionActive) {
            throw CineAuthException(
                code = "PAYMENT_REQUIRED",
                message = "Acesso negado: Este Gmail não possui pagamento de R$ 10,00 aprovado.",
            )
        }
        CineAuthSuccess(
            token = parsed.token,
            user = CineAuthUser(
                id = parsed.user.id,
                email = parsed.user.email.ifBlank { normalized },
                name = parsed.user.name,
                subscriptionActive = parsed.user.subscriptionActive,
            ),
        )
    }

    suspend fun login(email: String, password: String): Result<CineAuthSuccess> = runCatching {
        val normalized = email.trim().lowercase()
        val payload = buildJsonObject {
            put("email", normalized)
            put("password", password)
        }.toString()

        val response = http.post("${CineBackendConfig.baseUrl}/api/auth/login") {
            header("Content-Type", "application/json")
            header("Accept", "application/json")
            setBody(payload)
        }
        val rawBody = readBoundedResponseBody(response.bodyAsChannel(), response.contentLength())
        val parsed = json.decodeFromString<AuthApiResponse>(rawBody)
        if (response.status.value !in 200..299 || !parsed.ok || parsed.token.isNullOrBlank() || parsed.user == null) {
            throw CineAuthException(
                code = parsed.code,
                message = parsed.error ?: "Gmail ou senha incorretos ou pagamento não encontrado.",
            )
        }
        if (!parsed.user.subscriptionActive) {
            throw CineAuthException(
                code = "PAYMENT_REQUIRED",
                message = "Acesso negado: Sua assinatura não está ativa.",
            )
        }
        CineAuthSuccess(
            token = parsed.token,
            user = CineAuthUser(
                id = parsed.user.id,
                email = parsed.user.email.ifBlank { normalized },
                name = parsed.user.name,
                subscriptionActive = parsed.user.subscriptionActive,
            ),
        )
    }
}

