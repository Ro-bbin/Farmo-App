package com.farmo.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "FarmoSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_PROFILE_PIC_NAME = "profile_pic_name";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * Saves the session.
     */
    public void saveSession(String userId, String userType, String token,
                            String refreshToken, boolean isLoggedIn) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_TYPE, userType);
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, "");
    }

    public String getUserType() {
        return pref.getString(KEY_USER_TYPE, "");
    }

    public String getAuthToken() {
        return pref.getString(KEY_AUTH_TOKEN, "");
    }

    public String getRefreshToken() {
        return pref.getString(KEY_REFRESH_TOKEN, "");
    }

    public void setProfilePicName(String name) {
        saveValue(KEY_PROFILE_PIC_NAME, name);
    }

    public String getProfilePicName() {
        return getValue(KEY_PROFILE_PIC_NAME, "");
    }

    // Generic helpers for product images etc.
    public void saveValue(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    public String getValue(String key, String defaultValue) {
        return pref.getString(key, defaultValue);
    }

    /**
     * Clears all data.
     */
    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}