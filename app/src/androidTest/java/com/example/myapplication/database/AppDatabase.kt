package com.example.myapplication.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.dao.UserDao
import com.example.myapplication.model.User

// @Database: Lista todas las Entities (tablas) y define la versión
@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Método abstracto para obtener el DAO
    abstract fun userDao(): UserDao

    companion object {
        // INSTANCE: Una instancia única (Singleton) de la base de datos
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Patrón Singleton: Si la instancia ya existe, la retorna.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database" // Nombre del archivo de la base de datos
                )
                    // Esto es crucial para manejar la ejecución de consultas en hilos.
                    .fallbackToDestructiveMigration() // Solo para desarrollo, maneja migraciones
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}