package com.nuvio.app.core.auth

import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIDevice

actual object AuthStorage {
    private const val KEY_ANONYMOUS_USER_ID = "anonymous_user_id"
    private const val PAYMENT_PREFIX = "cine_payment_"
    private const val KEY_DEVICE_FINGERPRINT = "cine_hw_device_fingerprint_v1"

    actual fun loadAnonymousUserId(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(KEY_ANONYMOUS_USER_ID)

    actual fun saveAnonymousUserId(userId: String) {
        NSUserDefaults.standardUserDefaults.setObject(userId, forKey = KEY_ANONYMOUS_USER_ID)
    }

    actual fun clearAnonymousUserId() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_ANONYMOUS_USER_ID)
    }

    actual fun loadValue(key: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(key)

    actual fun saveValue(key: String, value: String?) {
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
        }
    }

    actual fun loadPaymentValue(key: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey("$PAYMENT_PREFIX$key")

    actual fun savePaymentValue(key: String, value: String?) {
        val fullKey = "$PAYMENT_PREFIX$key"
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(fullKey)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(value, forKey = fullKey)
        }
    }

    actual fun loadDeviceFingerprint(): String {
        val stored = NSUserDefaults.standardUserDefaults.stringForKey(KEY_DEVICE_FINGERPRINT)?.trim()
        if (!stored.isNullOrBlank()) return stored
        val vendorId = UIDevice.currentDevice.identifierForVendor?.UUIDString?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: NSUUID().UUIDString
        val fp = "ios_${vendorId.lowercase()}"
        NSUserDefaults.standardUserDefaults.setObject(fp, forKey = KEY_DEVICE_FINGERPRINT)
        return fp
    }
}
