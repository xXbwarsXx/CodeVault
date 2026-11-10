package hu.cardvault.app

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The key is hardware-backed where the device supports it; never exported or backed up. */
internal class SecureStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("vault_ciphertext_v2", Context.MODE_PRIVATE)
    private val alias = "cardvault_aes_v2"

    /** Create the non-exportable Keystore key before the first biometric unlock. */
    fun prepareKey() {
        key()
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationValidityDurationSeconds(300)
        if (Build.VERSION.SDK_INT >= 30) {
            builder.setUserAuthenticationParameters(300,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
        }
        generator.init(builder.build())
        return generator.generateKey()
    }

    fun save(number: String, expiry: String) {
        require(number.matches(Regex("[0-9 ]{12,23}"))) { "Érvénytelen kártyaszám" }
        require(expiry.matches(Regex("(0[1-9]|1[0-2])/[0-9]{2,4}"))) { "Lejárat: HH/ÉÉ" }
        val plaintext = "$number\n$expiry".toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(plaintext)
        prefs.edit()
            .putString("data", Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP))
            .apply()
        plaintext.fill(0)
    }

    fun load(): Pair<String, String>? {
        val data = prefs.getString("data", null) ?: return null
        val bytes = Base64.decode(data, Base64.NO_WRAP)
        require(bytes.size > 28) { "Sérült tárolt adat" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        val decoded = cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
        val fields = decoded.split('\n', limit = 2)
        require(fields.size == 2) { "Sérült tárolt adat" }
        return fields[0] to fields[1]
    }

    fun hasCard() = prefs.contains("data")
    fun delete() { prefs.edit().remove("data").apply() }
}
