package com.example.projekatmobilne.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.activities.AuthActivity;
import com.example.projekatmobilne.activities.HomeActivity;
import com.example.projekatmobilne.viewModels.AuthViewModel;

/**
 * LoginFragment - Presentation Layer
 *
 * Odgovornosti:
 * - Prikuplja input od korisnika
 * - Poziva AuthViewModel za login
 * - Prikazuje poruke korisniku
 * - NEMA poslovne logike (to je u AuthService)
 */
public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        etEmail = view.findViewById(R.id.etLoginEmail);
        etPassword = view.findViewById(R.id.etLoginPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        tvGoToRegister = view.findViewById(R.id.tvGoToRegister);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // LOGIN BUTTON
        btnLogin.setOnClickListener(v -> handleLogin());

        // GO TO REGISTER
        tvGoToRegister.setOnClickListener(v -> {
            ((AuthActivity) requireActivity()).loadFragment(new RegisterFragment());
        });

        // Observe login status
        authViewModel.authStatus.observe(getViewLifecycleOwner(), status -> {
            if ("login_success".equals(status)) {
                Toast.makeText(getContext(), R.string.login_success, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getActivity(), HomeActivity.class));
                requireActivity().finish();
            }
        });

        // Observe errors
        authViewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
        });

        return view;
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // ===== MINIMALNA VALIDACIJA =====
        // Detaljnija validacija je u AuthService!
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Popuni sva polja!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Poziv ViewModel-a (koji poziva AuthService)
        authViewModel.login(email, password);
    }
}