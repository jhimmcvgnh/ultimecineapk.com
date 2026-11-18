package com.nuvio.app.core.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

actual object AuthStorage {
    private const val PREFS_NAME = "nuvio_auth"
    private const val PAYMENT_PREFS_NAME = "nuvio_payment_state"
    private const val KEY_ANONYMOUS_USER_ID = "anonymous_user_id"
    private const val KEY_DEVICE_FINGERPRINT = "cine_hw_device_fingerprint_v1"

    private var preferences: SharedPreferences? = null
    private var paymentPreferences: SharedPreferences? = null
    private var cachedDeviceFingerprint: String? = null

    @SuppressLint("HardwareIds")
    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        paymentPreferences = context.getSharedPreferences(PAYMENT_PREFS_NAME, Context.MODE_PRIVATE)
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()?.trim()?.lowercase().orEmpty()

        val hwRaw = buildString {
            if (androidId.isNotBlank() && androidId != "9774d56d682e549c") {
                append("aid:").append(androidId)
            } else {
                val savedFallback = paymentPreferences?.getString(KEY_DEVICE_FINGERPRINT, null)
                    ?: UUID.randomUUID().toString()
                append("fb:").append(savedFallback)
            }
            append("|b:").append(Build.BOARD.orEmpty())
            append("|br:").append(Build.BRAND.orEmpty())
            append("|d:").append(Build.DEVICE.orEmpty())
            append("|hw:").append(Build.HARDWARE.orEmpty())
            append("|m:").append(Build.MODEL.orEmpty())
        }
        val hashed = sha256Hex(hwRaw)
        cachedDeviceFingerprint = "and_$hashed"
        paymentPreferences?.edit()?.putString(KEY_DEVICE_FINGERPRINT, cachedDeviceFingerprint)?.apply()
    }

    actual fun loadAnonymousUserId(): String? =
        preferences?.getString(KEY_ANONYMOUS_USER_ID, null)

    actual fun saveAnonymousUserId(userId: String) {
        preferences?.edit()?.putString(KEY_ANONYMOUS_USER_ID, userId)?.apply()
    }

    actual fun clearAnonymousUserId() {
        preferences?.edit()?.remove(KEY_ANONYMOUS_USER_ID)?.apply()
    }

    actual fun loadValue(key: String): String? =
        preferences?.getString(key, null)

    actual fun saveValue(key: String, value: String?) {
        val editor = preferences?.edit() ?: return
        if (value == null) editor.remove(key) else editor.putString(key, value)
        editor.apply()
    }

    actual fun loadPaymentValue(key: String): String? =
        paymentPreferences?.getString(key, null)

    actual fun savePaymentValue(key: String, value: String?) {
        val editor = paymentPreferences?.edit() ?: return
        if (value == null) editor.remove(key) else editor.putString(key, value)
        editor.apply()
    }

    actual fun loadDeviceFingerprint(): String {
        cachedDeviceFingerprint?.takeIf { it.isNotBlank() }?.let { return it }
        paymentPreferences?.getString(KEY_DEVICE_FINGERPRINT, null)?.takeIf { it.isNotBlank() }?.let {
            cachedDeviceFingerprint = it
            return it
        }
        val fallbackRaw = "${Build.BOARD}|${Build.BRAND}|${Build.DEVICE}|${Build.HARDWARE}|${Build.MODEL}"
        val computed = "and_${sha256Hex(fallbackRaw)}"
        cachedDeviceFingerprint = computed
        paymentPreferences?.edit()?.putString(KEY_DEVICE_FINGERPRINT, computed)?.apply()
        return computed
    }

    private fun sha256Hex(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
