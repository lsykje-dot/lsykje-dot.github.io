package com.lsykje.diary.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * 설계 문서 7-1절: PIN·패턴은 원문을 저장하지 않고 SHA-256 + 무작위 salt로 해시해서
 * Android Keystore로 암호화된 EncryptedSharedPreferences에 저장한다.
 * 패턴은 3x3 격자에서 선택한 칸 번호(0~8)의 순서를 콤마로 이어붙인 문자열을 해시 입력으로 사용한다.
 */
class LockPrefs(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "lock_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    val hasPin: Boolean get() = prefs.contains(KEY_PIN_HASH)
    val hasPattern: Boolean get() = prefs.contains(KEY_PATTERN_HASH)

    /** 최소 1개의 잠금 방식은 항상 유지되어야 한다는 설계 원칙(7-1절)을 지키기 위한 체크 */
    fun hasAnyLockMethod(): Boolean = biometricEnabled || hasPin || hasPattern

    fun setPin(pin: String) = storeSecret(pin, KEY_PIN_HASH, KEY_PIN_SALT)
    fun verifyPin(pin: String): Boolean = verifySecret(pin, KEY_PIN_HASH, KEY_PIN_SALT)
    fun clearPin() = prefs.edit().remove(KEY_PIN_HASH).remove(KEY_PIN_SALT).apply()

    fun setPattern(cells: List<Int>) = storeSecret(cells.joinToString(","), KEY_PATTERN_HASH, KEY_PATTERN_SALT)
    fun verifyPattern(cells: List<Int>): Boolean =
        verifySecret(cells.joinToString(","), KEY_PATTERN_HASH, KEY_PATTERN_SALT)
    fun clearPattern() = prefs.edit().remove(KEY_PATTERN_HASH).remove(KEY_PATTERN_SALT).apply()

    private fun storeSecret(secret: String, hashKey: String, saltKey: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(secret, salt)
        prefs.edit()
            .putString(hashKey, hash)
            .putString(saltKey, salt.toHex())
            .apply()
    }

    private fun verifySecret(secret: String, hashKey: String, saltKey: String): Boolean {
        val storedHash = prefs.getString(hashKey, null) ?: return false
        val salt = prefs.getString(saltKey, null)?.fromHex() ?: return false
        return hash(secret, salt) == storedHash
    }

    private fun hash(secret: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val bytes = digest.digest(secret.toByteArray(Charsets.UTF_8))
        return bytes.toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun String.fromHex(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PATTERN_HASH = "pattern_hash"
        private const val KEY_PATTERN_SALT = "pattern_salt"
    }
}
