package com.nuvio.app.core.auth

import com.nuvio.app.core.network.CineBackendConfig
import com.nuvio.app.core.network.createApiHttpClient
import com.nuvio.app.core.network.readBoundedResponseBody
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.encodeURLParameter
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

internal data class CinePlanInfo(
    val id: String = "plano_unico_29",
    val amount: Double = 29.0,
    val formattedPrice: String = "R$ 29,00",
)

internal data class CheckEmailResult(
    val email: String,
    val entitlementActive: Boolean,
    val hasAccount: Boolean,
)

internal data class SessionStatusResult(
    val approved: Boolean,
    val email: String,
    val hasAccount: Boolean,
    val paymentId: String? = null,
    val endpointUnavailable: Boolean = false,
)

internal data class CineAuthUser(
    val id: String,
    val email: String,
    val name: String,
    val subscriptionActive: Boolean,
)

internal data class CineAuthSuccess(
    val token: String,
    val refreshToken: String? = null,
    val user: CineAuthUser,
)

@Serializable
internal data class SupabaseAuthSessionDto(
    val access_token: String = "",
    val refresh_token: String = "",
    val expires_in: Long = 3600L,
    val user: SupabaseAuthUserDto? = null,
)

@Serializable
internal data class SupabaseAuthUserDto(
    val id: String = "",
    val email: String? = null,
    val user_metadata: JsonObject? = null,
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
private data class HealthPlanDto(
    val id: String? = null,
    val amount: Double? = null,
    val formattedAmount: String? = null,
)

@Serializable
private data class HealthApiResponse(
    val status: String? = null,
    val plan: HealthPlanDto? = null,
)

@Serializable
private data class SessionStatusApiResponse(
    val ok: Boolean = false,
    val approved: Boolean = false,
    val email: String = "",
    val hasAccount: Boolean = false,
    val paymentId: String? = null,
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
    val refreshToken: String? = null,
    val user: AuthUserDto? = null,
)

internal object CineAuthClient {
    private val http: HttpClient by lazy { createApiHttpClient() }
    private val json = Json { ignoreUnknownKeys = true }
    private val _planInfo = MutableStateFlow(CinePlanInfo())
    val planInfo: StateFlow<CinePlanInfo> = _planInfo.asStateFlow()

    fun currentFormattedPrice(): String = _planInfo.value.formattedPrice

    suspend fun refreshPlanInfo(): Result<CinePlanInfo> = runCatching {
        val response = http.get("${CineBackendConfig.baseUrl}/api/health") {
            timeout {
                connectTimeoutMillis = 3_500
                socketTimeoutMillis = 4_000
                requestTimeoutMillis = 4_000
            }
            header("Accept", "application/json")
        }
        val rawBody = readBoundedResponseBody(response.bodyAsChannel(), response.contentLength())
        val parsed = json.decodeFromString<HealthApiResponse>(rawBody)
        val plan = parsed.plan
        val amount = plan?.amount
        if (response.status.value in 200..299 && amount != null && amount > 0.0) {
            val formatted = plan.formattedAmount?.trim()?.takeIf { it.isNotBlank() } ?: formatBrl(amount)
            val updated = CinePlanInfo(
                id = plan.id?.trim()?.takeIf { it.isNotBlank() } ?: _planInfo.value.id,
                amount = amount,
                formattedPrice = formatted,
            )
            _planInfo.value = updated
            updated
        } else {
            _planInfo.value
        }
    }

    suspend fun checkEmail(email: String, paymentId: String? = null): Result<CheckEmailResult> = runCatching {
        val normalized = email.trim().lowercase()
        val payload = buildJsonObject {
            put("email", normalized)
            paymentId?.trim()?.takeIf { it.isNotBlank() }?.let { put("paymentId", it) }
        }.toString()

        val response = http.post("${CineBackendConfig.baseUrl}/api/auth/check-email") {
            timeout {
                connectTimeoutMillis = 4_000
                socketTimeoutMillis = 5_000
                requestTimeoutMillis = 5_000
            }
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
            timeout {
                connectTimeoutMillis = 4_000
                socketTimeoutMillis = 5_000
                requestTimeoutMillis = 5_000
            }
            header("Accept", "application/json")
        }
        if (response.status.value in setOf(404, 405, 501)) {
            return@runCatching SessionStatusResult(
                approved = false,
                email = "",
                hasAccount = false,
                endpointUnavailable = true,
            )
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
            paymentId = parsed.paymentId?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    private fun parseAuthSuccess(
        rawBody: String,
        statusCode: Int,
        fallbackEmail: String,
        defaultError: String,
    ): CineAuthSuccess {
        val parsed = json.decodeFromString<AuthApiResponse>(rawBody)
        if (statusCode !in 200..299 || !parsed.ok || parsed.token.isNullOrBlank() || parsed.user == null) {
            throw CineAuthException(
                code = parsed.code,
                message = parsed.error ?: defaultError,
            )
        }
        if (!parsed.user.subscriptionActive) {
            throw CineAuthException(
                code = "PAYMENT_REQUIRED",
                message = "Acesso negado: Este Gmail não possui pagamento de ${currentFormattedPrice()} aprovado.",
            )
        }
        return CineAuthSuccess(
            token = parsed.token,
            refreshToken = parsed.refreshToken?.takeIf { it.isNotBlank() },
            user = CineAuthUser(
                id = parsed.user.id,
                email = parsed.user.email.ifBlank { fallbackEmail },
                name = parsed.user.name,
                subscriptionActive = parsed.user.subscriptionActive,
            ),
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
        parseAuthSuccess(
            rawBody = rawBody,
            statusCode = response.status.value,
            fallbackEmail = normalized,
            defaultError = "Não foi possível criar sua conta. Verifique se o PIX foi aprovado.",
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
        parseAuthSuccess(
            rawBody = rawBody,
            statusCode = response.status.value,
            fallbackEmail = normalized,
            defaultError = "Gmail ou senha incorretos ou pagamento não encontrado.",
        )
    }

    suspend fun refreshSupabaseToken(refreshToken: String): Result<SupabaseAuthSessionDto> = runCatching {
        val token = refreshToken.trim()
        require(token.isNotBlank()) { "Refresh token vazio" }
        val payload = buildJsonObject {
            put("refresh_token", token)
        }.toString()

        val response = http.post("${CineBackendConfig.SUPABASE_URL}/auth/v1/token?grant_type=refresh_token") {
            timeout {
                connectTimeoutMillis = 5_000
                socketTimeoutMillis = 8_000
                requestTimeoutMillis = 8_000
            }
            header("apikey", CineBackendConfig.SUPABASE_ANON_KEY)
            header("Content-Type", "application/json")
            setBody(payload)
        }
        val rawBody = readBoundedResponseBody(response.bodyAsChannel(), response.contentLength())
        val parsed = json.decodeFromString<SupabaseAuthSessionDto>(rawBody)
        if (response.status.value !in 200..299 || parsed.access_token.isBlank()) {
            throw CineAuthException(
                code = "SESSION_REFRESH_FAILED",
                message = "Falha ao renovar sessão (${response.status.value})",
            )
        }
        parsed
    }

    suspend fun fetchSupabaseUser(accessToken: String): Result<SupabaseAuthUserDto> = runCatching {
        val token = accessToken.trim()
        require(token.isNotBlank()) { "Access token vazio" }
        val response = http.get("${CineBackendConfig.SUPABASE_URL}/auth/v1/user") {
            timeout {
                connectTimeoutMillis = 5_000
                socketTimeoutMillis = 8_000
                requestTimeoutMillis = 8_000
            }
            header("apikey", CineBackendConfig.SUPABASE_ANON_KEY)
            header("Authorization", "Bearer $token")
        }
        val rawBody = readBoundedResponseBody(response.bodyAsChannel(), response.contentLength())
        if (response.status.value !in 200..299) {
            throw CineAuthException(
                code = if (response.status.value == 401 || response.status.value == 403) "SESSION_UNAUTHORIZED" else null,
                message = "HTTP ${response.status.value}",
            )
        }
        json.decodeFromString<SupabaseAuthUserDto>(rawBody)
    }

    suspend fun saveSupabaseProfilesMetadata(
        accessToken: String,
        profilesArray: JsonArray,
    ): Result<JsonArray?> = runCatching {
        val token = accessToken.trim()
        require(token.isNotBlank()) { "Access token vazio" }
        val body = buildJsonObject {
            put(
                "data",
                buildJsonObject {
                    put("cine_profiles", profilesArray)
                },
            )
        }.toString()
        val response = http.put("${CineBackendConfig.SUPABASE_URL}/auth/v1/user") {
            timeout {
                connectTimeoutMillis = 5_000
                socketTimeoutMillis = 8_000
                requestTimeoutMillis = 8_000
            }
            header("apikey", CineBackendConfig.SUPABASE_ANON_KEY)
            header("Authorization", "Bearer $token")
            header("Content-Type", "application/json")
            setBody(body)
        }
        val rawBody = readBoundedResponseBody(response.bodyAsChannel(), response.contentLength())
        if (response.status.value !in 200..299) {
            throw CineAuthException(
                code = if (response.status.value == 401 || response.status.value == 403) "SESSION_UNAUTHORIZED" else null,
                message = "HTTP ${response.status.value}",
            )
        }
        val user = json.decodeFromString<SupabaseAuthUserDto>(rawBody)
        user.user_metadata?.get("cine_profiles") as? JsonArray
    }
}

internal fun formatBrl(amount: Double): String {
    val totalCents = (amount * 100.0).roundToInt()
    val sign = if (totalCents < 0) "-" else ""
    val absCents = abs(totalCents)
    val reais = absCents / 100
    val centavos = absCents % 100
    return "${sign}R$ $reais,${centavos.toString().padStart(2, '0')}"
}

