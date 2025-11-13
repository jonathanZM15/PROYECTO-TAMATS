plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        // Forzar language version 1.9 para compatibilidad con KAPT actualmente
        freeCompilerArgs += listOf("-language-version", "1.9")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //declara dependencias
    implementation(libs.room.runtime)
    //ejecutar secuencias de sql con room
    implementation(libs.room.ktx)
    //generar sentencias sql automaticas
    kapt(libs.room.compiler)
    //ejecutar procesos en segundo plano
    implementation(libs.kotlinx.coroutines.android)

    //dependencias para firebase
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    //ejecutar un CRUD completo
    implementation("com.google.firebase:firebase-firestore-ktx")
    //por si en algun momento se quiere trabajar conn google analitics
    implementation("com.google.firebase:firebase-analytics")
    // Firebase Storage (subir archivos)
    implementation("com.google.firebase:firebase-storage-ktx")

    // Glide para cargar previews de imágenes en el grid
    implementation("com.github.bumptech.glide:glide:4.15.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")

    //Librerias lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    //para actualizar datos en tiempo real en firebase y mi interfaz de mi app
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    //se guarden los datos en segundo plano
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    //las coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

}