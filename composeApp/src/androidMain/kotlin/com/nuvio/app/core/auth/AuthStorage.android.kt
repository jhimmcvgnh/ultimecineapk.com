package com.nuvio.app.core.auth

import android.content.Context
import android.content.SharedPreferences

actual object AuthStorage {
    private const val PREFS_NAME = "nuvio_auth"
    private const val PAYMENT_PREFS_NAME = "nuvio_payment_state"
    private const val KEY_ANONYMOUS_USER_ID = "anonymous_user_id"

    private var preferences: SharedPreferences? = null
    private var paymentPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        paymentPreferences = context.getSharedPreferences(PAYMENT_PREFS_NAME, Context.MODE_PRIVATE)
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
}
