package com.example.myapplication.util

import android.util.Log

/**
 * Utilidad para validar contraseñas con requisitos de seguridad
 */
object PasswordValidator {

    /**
     * Resultado de la validación de contraseña
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val meetsLength: Boolean = false,
        val meetsUppercase: Boolean = false,
        val meetsNumber: Boolean = false
    )

    /**
     * Requisitos mínimos de contraseña
     */
    const val MIN_LENGTH = 8

    /**
     * Valida una contraseña con todos los requisitos
     */
    fun validate(password: String): ValidationResult {
        Log.d("PasswordValidator", "Validando contraseña: '$password' (longitud: ${password.length})")

        val errors = mutableListOf<String>()

        // Verificar longitud mínima
        val meetsLength = password.length >= MIN_LENGTH
        Log.d("PasswordValidator", "Longitud >= $MIN_LENGTH: $meetsLength")
        if (!meetsLength) {
            errors.add("La contraseña debe tener al menos $MIN_LENGTH caracteres")
        }

        // Verificar mayúscula
        val meetsUppercase = password.any { it.isUpperCase() }
        Log.d("PasswordValidator", "Tiene mayúscula: $meetsUppercase")
        if (!meetsUppercase) {
            errors.add("La contraseña debe contener al menos una letra mayúscula")
        }

        // Verificar número
        val meetsNumber = password.any { it.isDigit() }
        Log.d("PasswordValidator", "Tiene número: $meetsNumber")
        if (!meetsNumber) {
            errors.add("La contraseña debe contener al menos un número")
        }

        val isValid = meetsLength && meetsUppercase && meetsNumber
        Log.d("PasswordValidator", "Resultado final - isValid: $isValid, errors: $errors")

        return ValidationResult(
            isValid = isValid,
            errors = errors,
            meetsLength = meetsLength,
            meetsUppercase = meetsUppercase,
            meetsNumber = meetsNumber
        )
    }

    /**
     * Verifica que las dos contraseñas coincidan
     */
    fun passwordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }

    /**
     * Obtiene un mensaje de error amigable para el usuario
     */
    fun getErrorMessage(result: ValidationResult): String {
        return when {
            result.errors.isEmpty() -> ""
            result.errors.size == 1 -> result.errors[0]
            else -> "La contraseña no cumple los requisitos:\n${result.errors.joinToString("\n• ", "• ")}"
        }
    }
}

