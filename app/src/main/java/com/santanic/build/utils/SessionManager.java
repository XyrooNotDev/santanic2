package com.santanic.build.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "SantanicSession";
    private static final String KEY_TOKEN = "github_token";
    private static final String KEY_USERNAME = "github_username";
    private static final String KEY_IS_LOGIN = "is_login";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveSession(String token, String username) {
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_USERNAME, username);
        editor.putBoolean(KEY_IS_LOGIN, true);
        editor.apply();
    }

    public String getToken() { return pref.getString(KEY_TOKEN, ""); }
    public String getUsername() { return pref.getString(KEY_USERNAME, "User"); }
    public boolean isLoggedIn() { return pref.getBoolean(KEY_IS_LOGIN, false); }
    public void logout() { editor.clear().apply(); }
}