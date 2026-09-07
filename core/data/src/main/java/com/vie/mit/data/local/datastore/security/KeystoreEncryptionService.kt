package com.vie.mit.data.local.datastore.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [EncryptionService] using Android Keystore.
 * Uses AES/GCM/NoPadding.
 */
@Singleton
class KeystoreEncryptionService @Inject constructor(
    @ApplicationContext private val context: Context
) : EncryptionService {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "GreenAppTokenKey"
        private const val TRANSFORMATION =
            "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            createKey()
        }
    }

    private fun createKey() {
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        val keyStoreEntry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return keyStoreEntry.secretKey
    }

    override suspend fun encrypt(plaintext: String): String? {
        return withContext(Dispatchers.Default) {
            try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
                val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
                val iv = cipher.iv
                val combined = ByteArray(iv.size + encryptedBytes.size)
                System.arraycopy(iv, 0, combined, 0, iv.size)
                System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

                Base64.encodeToString(combined, Base64.NO_WRAP)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    override suspend fun decrypt(ciphertext: String): String? {
        return withContext(Dispatchers.Default) {
            try {
                val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
                val iv = ByteArray(GCM_IV_LENGTH)
                val encryptedBytes = ByteArray(combined.size - GCM_IV_LENGTH)
                System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
                System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.size)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
                val decryptedBytes = cipher.doFinal(encryptedBytes)
                String(decryptedBytes, Charsets.UTF_8)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
