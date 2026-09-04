package com.vie.mit.datastore.security

/**
 * Service to handle encryption and decryption of sensitive data.
 */
interface EncryptionService {
    /**
     * Encrypts the given plaintext.
     * @param plaintext The string to encrypt.
     * @return The encrypted string (Base64 encoded), or null if encryption fails.
     */
    suspend fun encrypt(plaintext: String): String?

    /**
     * Decrypts the given ciphertext.
     * @param ciphertext The Base64 encoded encrypted string.
     * @return The decrypted plaintext, or null if decryption fails.
     */
    suspend fun decrypt(ciphertext: String): String?
}
