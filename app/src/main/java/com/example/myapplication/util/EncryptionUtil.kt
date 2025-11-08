package com.example.myapplication.util

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Utilidad para cifrar y descifrar contraseñas usando AES.
 * Las contraseñas se almacenan en formato cifrado en Firebase y Room.
 */
object EncryptionUtil {

    private const val ALGORITHM = "AES"
    private const val CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding"

    // Clave de cifrado fija (en producción, considera usar Android Keystore)
    private val SECRET_KEY = "TamatsSecretKey1".toByteArray() // 16 bytes para AES-128

    /**
     * Cifra una contraseña en texto plano usando AES
     * @param plainPassword La contraseña en texto plano
     * @return La contraseña cifrada en formato Base64
     */
    fun encryptPassword(plainPassword: String): String {
        return try {
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val secretKey = SecretKeySpec(SECRET_KEY, 0, SECRET_KEY.size, ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val encryptedBytes = cipher.doFinal(plainPassword.toByteArray())
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            throw RuntimeException("Error al cifrar la contraseña: ${e.message}", e)
        }
    }

    /**
     * Descifra una contraseña cifrada en Base64 usando AES
     * @param encryptedPassword La contraseña cifrada en formato Base64
     * @return La contraseña descifrada en texto plano
     */
    fun decryptPassword(encryptedPassword: String): String {
        return try {
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val secretKey = SecretKeySpec(SECRET_KEY, 0, SECRET_KEY.size, ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)

            val decodedBytes = Base64.decode(encryptedPassword, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes)
        } catch (e: Exception) {
            throw RuntimeException("Error al descifrar la contraseña: ${e.message}", e)
        }
    }

    /**
     * Verifica si una contraseña en texto plano coincide con una contraseña cifrada
     * @param plainPassword La contraseña en texto plano a verificar
     * @param encryptedPassword La contraseña cifrada almacenada
     * @return true si coinciden, false en caso contrario
     */
    fun verifyPassword(plainPassword: String, encryptedPassword: String): Boolean {
        return try {
            val decrypted = decryptPassword(encryptedPassword)
            decrypted == plainPassword
        } catch (e: Exception) {
            false
        }
    }
}

