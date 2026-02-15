package com.example.projekatmobilne.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.projekatmobilne.R;
import com.example.projekatmobilne.activities.AuthActivity;
import com.example.projekatmobilne.viewModels.AuthViewModel;

public class RegisterFragment extends Fragment {

    private EditText etEmail, etPassword, etConfirmPassword, etUsername;
    private ImageView ivAvatar1, ivAvatar2, ivAvatar3, ivAvatar4, ivAvatar5;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private AuthViewModel authViewModel;
    private String selectedAvatar = "avatar_1"; // Default

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        etEmail = view.findViewById(R.id.etRegisterEmail);
        etPassword = view.findViewById(R.id.etRegisterPassword);
        etConfirmPassword = view.findViewById(R.id.etRegisterConfirmPassword);
        etUsername = view.findViewById(R.id.etRegisterUsername);

        ivAvatar1 = view.findViewById(R.id.ivAvatar1);
        ivAvatar2 = view.findViewById(R.id.ivAvatar2);
        ivAvatar3 = view.findViewById(R.id.ivAvatar3);
        ivAvatar4 = view.findViewById(R.id.ivAvatar4);
        ivAvatar5 = view.findViewById(R.id.ivAvatar5);

        btnRegister = view.findViewById(R.id.btnRegister);
        tvGoToLogin = view.findViewById(R.id.tvGoToLogin);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // Avatar selection
        setupAvatarSelection();

        // REGISTER BUTTON
        btnRegister.setOnClickListener(v -> handleRegister());

        // GO TO LOGIN
        tvGoToLogin.setOnClickListener(v -> {
            ((AuthActivity) requireActivity()).loadFragment(new LoginFragment());
        });

        // Observe registration status
        authViewModel.authStatus.observe(getViewLifecycleOwner(), status -> {
            if ("registration_success".equals(status)) {
                Toast.makeText(getContext(), R.string.email_verification_sent, Toast.LENGTH_LONG).show();

                // Izloguj korisnika nakon registracije (email nije verifikovan)
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();

                ((AuthActivity) requireActivity()).loadFragment(new LoginFragment());
            }
        });

        // Observe errors
        authViewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
        });

        return view;
    }

    private void setupAvatarSelection() {
        View.OnClickListener avatarClickListener = v -> {
            // Reset all borders
            resetAvatarBorders();

            // Set selected border
            if (v.getId() == R.id.ivAvatar1) {
                selectedAvatar = "avatar_1";
                ivAvatar1.setBackgroundResource(R.drawable.avatar_selected_border);
            } else if (v.getId() == R.id.ivAvatar2) {
                selectedAvatar = "avatar_2";
                ivAvatar2.setBackgroundResource(R.drawable.avatar_selected_border);
            } else if (v.getId() == R.id.ivAvatar3) {
                selectedAvatar = "avatar_3";
                ivAvatar3.setBackgroundResource(R.drawable.avatar_selected_border);
            } else if (v.getId() == R.id.ivAvatar4) {
                selectedAvatar = "avatar_4";
                ivAvatar4.setBackgroundResource(R.drawable.avatar_selected_border);
            } else if (v.getId() == R.id.ivAvatar5) {
                selectedAvatar = "avatar_5";
                ivAvatar5.setBackgroundResource(R.drawable.avatar_selected_border);
            }
        };

        ivAvatar1.setOnClickListener(avatarClickListener);
        ivAvatar2.setOnClickListener(avatarClickListener);
        ivAvatar3.setOnClickListener(avatarClickListener);
        ivAvatar4.setOnClickListener(avatarClickListener);
        ivAvatar5.setOnClickListener(avatarClickListener);

        // Default selection
        ivAvatar1.setBackgroundResource(R.drawable.avatar_selected_border);
    }

    private void resetAvatarBorders() {
        ivAvatar1.setBackgroundResource(0);
        ivAvatar2.setBackgroundResource(0);
        ivAvatar3.setBackgroundResource(0);
        ivAvatar4.setBackgroundResource(0);
        ivAvatar5.setBackgroundResource(0);
    }

    private void handleRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String username = etUsername.getText().toString().trim();

        // Validacija
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || username.isEmpty()) {
            Toast.makeText(getContext(), "Popuni sva polja!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Lozinke se ne poklapaju!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(getContext(), "Lozinka mora imati minimum 6 karaktera!", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.register(email, password, username, selectedAvatar);
    }
}