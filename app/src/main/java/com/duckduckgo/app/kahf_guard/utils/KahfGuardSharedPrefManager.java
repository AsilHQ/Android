package com.duckduckgo.app.kahf_guard.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.duckduckgo.app.browser.R;

//initialize it once SharedPrefManager.INSTANCE.init() then just access by SharedPrefManager.INSTANCE.pref
public enum KahfGuardSharedPrefManager {
    INSTANCE;

    public SharedPreferences sharedPreferences;
    public SharedPreferences.Editor sharedPreferencesEditor;

    public final String KEY_CONNECT_TYPE = "connectType";
    public final String KEY_YOUTUBE_NO_SS_ENABLED = "youtubeNoSsEnabled";
    public final String KEY_FIX_SETTINGS = "fixSettings";

    public void init(Context context) {
        sharedPreferences = context.getSharedPreferences("dns_shared_prefs", Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
    }

    public String getConnectType(Context context){
        return INSTANCE.sharedPreferences.getString(KEY_CONNECT_TYPE, context.getString(R.string.connect_type_default));
    }

    public void setConnectType(String connectType){
        INSTANCE.sharedPreferencesEditor.putString(KEY_CONNECT_TYPE, connectType).commit();
    }

    public void removeConnectType(){
        INSTANCE.sharedPreferencesEditor.remove(KEY_CONNECT_TYPE).commit();
    }

    public Boolean isYoutubeNoSsEnabled(Context context){
        return INSTANCE.sharedPreferences.getBoolean(KEY_YOUTUBE_NO_SS_ENABLED, false);
    }

    public void setYoutubeNoSsEnabled(Boolean enabled){
        INSTANCE.sharedPreferencesEditor.putBoolean(KEY_YOUTUBE_NO_SS_ENABLED, enabled).commit();
    }

    public boolean getSettingsFix() {
        return INSTANCE.sharedPreferences.getBoolean(KEY_FIX_SETTINGS, false);
    }

    public void setFixSettings(boolean isSettingsFixed){
        INSTANCE.sharedPreferencesEditor.putBoolean(KEY_FIX_SETTINGS, isSettingsFixed).commit();
    }
}
