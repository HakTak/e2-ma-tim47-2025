package com.example.projekatmobilne.viewModels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.repositories.UserRepository;
import com.example.projekatmobilne.services.UserService;

/**
 * UserViewModel - Presentation Logic Layer
 *
 * Odgovornosti:
 * - Izlaže podatke UI sloju (Fragment/Activity)
 * - Poziva UserService za poslovnu logiku
 * - Upravlja LiveData objektima
 */
public class UserViewModel extends AndroidViewModel {

    private final UserService userService;
    public MutableLiveData<User> userData = new MutableLiveData<>();
    public MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public UserViewModel(@NonNull Application application) {
        super(application);
        this.userService = new UserService();
    }

    // LOAD USER
    public void loadUser(String userId) {
        userService.getUser(userId, new UserRepository.UserCallback() {
            @Override
            public void onUserLoaded(User user) {
                userData.postValue(user);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }
}