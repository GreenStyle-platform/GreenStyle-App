1. CONTEXT:
2. GOAL
3. REQUIREMENTS :
   3.1: Create core:datastore module.

   3.2: Tạo encryptionService để mã hóa token khi lưu vào datastore và giải mã token khi lấy token
   ra.
   Code example:
   package com.mit.learning_english.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.mit.learning_english.data.security.EncryptionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**

* Repository để quản lý DataStore Preferences
*
* Đây là ví dụ về cách sử dụng DataStore được inject từ Hilt.
*
* Cách hoạt động:
*
    1. DataStore được inject vào constructor thông qua @Inject
*
    2. Hilt tự động tìm provider trong DatabaseModule và inject instance vào đây
*
    3. EncryptionService được inject để mã hóa/giải mã token
*
    4. Repository cung cấp các hàm để đọc/ghi preferences
       */
       @Singleton
       class PreferencesDatasource @Inject constructor(
       private val dataStore: DataStore<Preferences>,
       private val encryptionService: EncryptionService
       ) {

/**

* Đọc giá trị String từ DataStore
*
* @param key Key của preference cần đọc
* @param defaultValue Giá trị mặc định nếu key chưa tồn tại
* @return Flow<String> - Flow để observe giá trị (tự động update khi giá trị thay đổi)
  */
  fun getString(key: Preferences.Key<String>, defaultValue: String = ""): Flow<String> {
  return dataStore.data.map { preferences ->
  preferences[key] ?: defaultValue
  }
  }

/**

* Ghi giá trị String vào DataStore
*
* @param key Key của preference
* @param value Giá trị cần lưu
  */
  suspend fun saveString(key: Preferences.Key<String>, value: String) {
  dataStore.edit { preferences ->
  preferences[key] = value
  }
  }

suspend fun saveInteger(key: Preferences.Key<Int>, value: Int){
dataStore.edit { preferences ->
preferences[key]=value
}
}
suspend fun saveLong(key: Preferences.Key<Long>, value: Long){
dataStore.edit { preferences ->
preferences[key]=value
}
}

fun getInteger(key: Preferences.Key<Int>, defaultValue: Int):Flow<Int>{
return dataStore.data.map{ preferences ->
preferences[key]?: defaultValue
}
}

    suspend fun saveExpiresTime(expiresAt: Long) {
        // Mã hóa token trước khi lưu
        val expiresAtString: String = expiresAt.toString()
        val encryptedToken = encryptionService.encrypt(expiresAtString)
        if (encryptedToken != null) {
            saveString(PreferencesKeys.EXPIRES_AT, encryptedToken)
        } else {
            // Nếu mã hóa thất bại, có thể log error hoặc throw exception
            // Ở đây chúng ta sẽ lưu plaintext như fallback (không khuyến khích trong production)
            // Trong production nên throw exception hoặc retry
            throw SecurityException("Failed to encrypt token")
        }
    }

    /**
     * Lưu user token đã được mã hóa
     * 
     * Token sẽ được mã hóa bằng EncryptionService trước khi lưu vào DataStore
     * để đảm bảo bảo mật ngay cả khi device bị root hoặc bị truy cập trái phép
     */
    suspend fun saveUserToken(token: String) {
        // Mã hóa token trước khi lưu
        val encryptedToken = encryptionService.encrypt(token)
        if (encryptedToken != null) {
            saveString(PreferencesKeys.USER_TOKEN, encryptedToken)
        } else {
            // Nếu mã hóa thất bại, có thể log error hoặc throw exception
            // Ở đây chúng ta sẽ lưu plaintext như fallback (không khuyến khích trong production)
            // Trong production nên throw exception hoặc retry
            throw SecurityException("Failed to encrypt token")
        }
    }

    /**
     * Đọc và giải mã user token
     * 
     * Token được đọc từ DataStore và giải mã về plaintext gốc
     * 
     * @return Flow<String> - Flow chứa token đã giải mã, hoặc chuỗi rỗng nếu không có hoặc giải mã thất bại
     */
    fun getUserToken(): Flow<String> {
        return getString(PreferencesKeys.USER_TOKEN).map { encryptedToken ->
            if (encryptedToken.isNotEmpty()) {
                // Giải mã token
                encryptionService.decrypt(encryptedToken) ?: ""
            } else {
                ""
            }
        }
    }
     fun getExpiresTime(): Flow<Long> {
        return getString(PreferencesKeys.EXPIRES_AT).map { encryptedExpiresTime ->
            if (encryptedExpiresTime.isNotEmpty()) {
                encryptionService.decrypt(encryptedExpiresTime)?.toLong()?: 0L
            } else {
                0L
            }
        }
    }
    
    /**
     * Lấy token một lần (không phải Flow)
     * 
     * Hữu ích khi cần token ngay lập tức mà không cần observe
     * 
     * @return Token đã giải mã, hoặc chuỗi rỗng nếu không có hoặc giải mã thất bại
     */
    suspend fun getUserTokenOnce(): String {
        val encryptedToken = getString(PreferencesKeys.USER_TOKEN).first()
        return if (encryptedToken.isNotEmpty()) {
            encryptionService.decrypt(encryptedToken) ?: ""
        } else {
            ""
        }
    }

    /**
     * Lưu refresh token đã được mã hóa
     */
    suspend fun saveRefreshToken(refreshToken: String) {
        val encryptedToken = encryptionService.encrypt(refreshToken)
        if (encryptedToken != null) {
            saveString(PreferencesKeys.REFRESH_TOKEN, encryptedToken)
        } else {
            throw SecurityException("Failed to encrypt refresh token")
        }
    }

    /**
     * Lấy refresh token đã giải mã
     */
    suspend fun getRefreshTokenOnce(): String {
        val encryptedToken = getString(PreferencesKeys.REFRESH_TOKEN).first()
        return if (encryptedToken.isNotEmpty()) {
            encryptionService.decrypt(encryptedToken) ?: ""
        } else {
            ""
        }
    }

    /**
     * Xóa một key
     */
    suspend fun removeKey(key: Preferences.Key<*>) {
        dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    /**
     * Ví dụ: Xóa tất cả preferences
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun hasSeenBeforeLoginOnboarding():Boolean{
       return dataStore.data.map{ preferences ->
           preferences[PreferencesKeys.HAS_SEEN_BEFORE_LOGIN_ONBOARDING]?:false
       }.first()
    }

    suspend fun updateOnboardingStatus(hasSeen:Boolean){
         dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SEEN_BEFORE_LOGIN_ONBOARDING] = hasSeen
        }
    }

    suspend fun hasCompletedAfterLoginOnboarding(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_AFTER_LOGIN_ONBOARDING] ?: false
        }.first()
    }

}
package com.mit.learning_english.data.security

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

* Implementation của EncryptionService sử dụng Android Keystore
*
* Android Keystore là hệ thống bảo mật phần cứng của Android, cho phép:
*
    - Lưu trữ keys an toàn trong secure hardware (nếu thiết bị hỗ trợ)
*
    - Keys không thể được extract ra ngoài
*
    - Tự động mã hóa keys khi lưu trữ
*
* Sử dụng AES/GCM/NoPadding - một trong những thuật toán mã hóa an toàn nhất
  */
  @Singleton
  class KeystoreEncryptionService @Inject constructor(
  @param: ApplicationContext private val context: Context
  ) : EncryptionService {

  companion object {
  private const val ANDROID_KEYSTORE = "AndroidKeyStore"
  private const val KEY_ALIAS = "LearningEnglishTokenKey"
  private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/$
  {KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"
  private const val GCM_IV_LENGTH = 12 // 12 bytes cho GCM
  private const val GCM_TAG_LENGTH = 128 // 128 bits cho authentication tag
  }

  private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
  load(null)
  }

  init {
  // Tạo key nếu chưa tồn tại
  if (!keyStore.containsAlias(KEY_ALIAS)) {
  createKey()
  }
  }

  /**
    * Tạo key trong Android Keystore
      */
      private fun createKey() {
      val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
      val keyGenParameterSpec = KeyGenParameterSpec.Builder(
      KEY_ALIAS,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256) // 256-bit key
      .build()

      keyGenerator.init(keyGenParameterSpec)
      keyGenerator.generateKey()
      }

  /**
    * Lấy SecretKey từ Keystore
      */
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
  và tạo 1 object để lưu key cho datastore
*

4. CONSTRAINTS
5. EXISTING CODE / REFERENCES
6. IMPLEMENTATION PLAN
7. VALIDATION
8. OUTPUT / REPORT