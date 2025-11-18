package com.example.myapplication.util

import java.text.SimpleDateFormat
import java.util.*

object AgeCalculator {

    /**
     * Calcula la edad basada en la fecha de nacimiento en formato "dd/MM/yyyy"
     */
    fun calculateAge(birthDateString: String): Int {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val birthDate = sdf.parse(birthDateString) ?: return 0

            val today = Calendar.getInstance()
            val birthCalendar = Calendar.getInstance().apply {
                time = birthDate
            }

            var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)

            // Ajustar si aún no ha pasado el cumpleaños este año
            if (today.get(Calendar.MONTH) < birthCalendar.get(Calendar.MONTH) ||
                (today.get(Calendar.MONTH) == birthCalendar.get(Calendar.MONTH) &&
                 today.get(Calendar.DAY_OF_MONTH) < birthCalendar.get(Calendar.DAY_OF_MONTH))) {
                age--
            }

            age
        } catch (e: Exception) {
            0
        }
    }
}

