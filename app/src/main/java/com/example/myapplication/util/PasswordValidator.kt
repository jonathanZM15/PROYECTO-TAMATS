package com.example.myapplication.util

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
        val errors = mutableListOf<String>()

        // Verificar longitud mínima
        val meetsLength = password.length >= MIN_LENGTH
        if (!meetsLength) {
            errors.add("La contraseña debe tener al menos $MIN_LENGTH caracteres")
        }

        // Verificar mayúscula
        val meetsUppercase = password.any { it.isUpperCase() }
        if (!meetsUppercase) {
            errors.add("La contraseña debe contener al menos una letra mayúscula")
        }

        // Verificar número
        val meetsNumber = password.any { it.isDigit() }
        if (!meetsNumber) {
            errors.add("La contraseña debe contener al menos un número")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
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

