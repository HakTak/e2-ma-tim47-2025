package com.example.projekatmobilne.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.fragments.LoginFragment;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Prikaži LoginFragment kao default
        if (savedInstanceState == null) {
            loadFragment(new LoginFragment());
        }
    }

    // Helper metoda za zamenu fragmenata
    public void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.auth_fragment_container, fragment);
        transaction.commit();
    }
}