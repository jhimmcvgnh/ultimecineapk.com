package com.nuvio.app.core.auth

internal expect object AuthStorage {
    fun loadAnonymousUserId(): String?
    fun saveAnonymousUserId(userId: String)
    fun clearAnonymousUserId()

    fun loadValue(key: String): String?
    fun saveValue(key: String, value: String?)

    fun loadPaymentValue(key: String): String?
    fun savePaymentValue(key: String, value: String?)

    fun loadDeviceFingerprint(): String
}
