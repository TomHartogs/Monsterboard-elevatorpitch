package com.example.tomha.elevatorPitch.Preferences;

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

    public boolean getSharedPreferenceBooleanValue(String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(key, false);
    }
}
