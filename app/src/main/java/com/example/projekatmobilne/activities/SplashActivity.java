package com.example.projekatmobilne.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.utils.SharedPrefsManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Proveri session nakon 1.5 sekundi (splash delay)
        new Handler().postDelayed(() -> {
            SharedPrefsManager prefsManager = new SharedPrefsManager(this);

            if (prefsManager.isLoggedIn()) {
                // Korisnik je ulogovan → HomeActivity
                startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            } else {
                // Korisnik NIJE ulogovan → AuthActivity
                startActivity(new Intent(SplashActivity.this, AuthActivity.class));
            }
            finish(); // Zatvori SplashActivity
        }, 5000);
    }
}