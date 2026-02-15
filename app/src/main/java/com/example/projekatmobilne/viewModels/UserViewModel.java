package com.example.projekatmobilne.viewModels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.projekatmobilne.models.User;
import com.example.projekatmobilne.repositories.UserRepository;

public class UserViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    public MutableLiveData<User> userData = new MutableLiveData<>();
    public MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public UserViewModel(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepository();
    }

    // LOAD USER
    public void loadUser(String userId) {
        userRepository.getUser(userId, new UserRepository.UserCallback() {
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