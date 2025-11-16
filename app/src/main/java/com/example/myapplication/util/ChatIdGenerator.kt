package com.example.myapplication.util

import java.security.MessageDigest

object ChatIdGenerator {
    /**
     * Genera un ID de chat seguro y consistente basado en dos emails.
     * Usa MD5 hash para evitar caracteres especiales que causen problemas en Firebase/Android.
     * El mismo par de emails siempre genera el mismo ID.
     */
    fun generateChatId(email1: String, email2: String): String {
        // Ordenar emails para garantizar consistencia (email1_email2 = email2_email1)
        val emails = listOf(email1, email2).sorted()
        val combined = "${emails[0]}_${emails[1]}"

        // Generar hash MD5
        val md = MessageDigest.getInstance("MD5")
        val messageDigest = md.digest(combined.toByteArray())

        // Convertir a hexadecimal
        return messageDigest.joinToString("") { "%02x".format(it) }
    }
}

