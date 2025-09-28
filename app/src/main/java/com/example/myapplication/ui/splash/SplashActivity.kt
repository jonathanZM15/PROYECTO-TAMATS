package com.example.myapplication.ui.splash

    import androidx.appcompat.app.AppCompatActivity
    import android.os.Bundle



class SplashActivity : AppCompatActivity() { // o ComponentActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Aquí va tu lógica para el Splash screen
        // Si usas Compose:
        // setContent {
        //     TuComposableDeSplash()
        // }
        // Si usas XML (activity_splash.xml):
        // setContentView(R.layout.activity_splash)
    }
}

