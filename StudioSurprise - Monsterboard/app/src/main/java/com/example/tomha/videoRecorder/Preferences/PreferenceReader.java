package com.example.tomha.videoRecorder.Preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by tomha on 14-3-2018.
 */

public class PreferenceReader{

    Context context;

    PreferenceReader(Context context){
        this.context = context;
    }

    public String getSharedPreferenceValue(String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, "");
    }

    public Integer getSharedPreferenceIntegerValue(String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Integer value = prefs.getInt(key, 0);

        return value;
    }

    public boolean getSharedPreferenceBooleanValue(String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(key, false);
    }
}
