package com.example.projekatmobilne.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsManager {

    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_AVATAR = "avatar";
    private static final String KEY_LEVEL = "level";

    private final SharedPreferences prefs;

    public SharedPrefsManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // SAVE SESSION
    public void saveSession(String userId, String username, String avatar, int level) {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putString(KEY_AVATAR, avatar)
                .putInt(KEY_LEVEL, level)
                .apply();
    }

    // CLEAR SESSION (logout)
    public void clearSession() {
        prefs.edit().clear().apply();
    }

    // IS LOGGED IN
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // GET USER ID
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    // GET USERNAME
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    // GET AVATAR
    public String getAvatar() {
        return prefs.getString(KEY_AVATAR, "avatar_1");
    }

    // GET LEVEL
    public int getLevel() {
        return prefs.getInt(KEY_LEVEL, 0);
    }
}