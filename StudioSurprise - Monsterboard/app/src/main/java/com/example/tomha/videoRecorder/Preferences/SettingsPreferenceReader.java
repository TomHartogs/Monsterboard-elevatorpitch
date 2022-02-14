package com.example.tomha.videoRecorder.Preferences;

import android.content.Context;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.widget.TextView;

import com.example.tomha.videoRecorder.R;

/**
 * Created by tomha on 14-3-2018.
 */

public class SettingsPreferenceReader extends PreferenceReader {

    public SettingsPreferenceReader (Context context){
        super(context);
    }

    public String getWelcomeMessage() {
        String welcomeMessage = getSharedPreferenceValue(context.getString(R.string.pref_key_welcomeMessage));
        return welcomeMessage != "" ? welcomeMessage : context.getString(R.string.welcomeMessage);
    }

    public String getRecordingFinishedMessage() {
        String recordingFinishedMessage = getSharedPreferenceValue(context.getString(R.string.pref_key_recordingFinishedMessage));
        return recordingFinishedMessage != "" ? recordingFinishedMessage : context.getString(R.string.recordingFinishedMessage);
    }

    public Integer getResetTimer(){
        String resetTimer = super.getSharedPreferenceValue(context.getString(R.string.pref_key_resetTimer));
        return resetTimer != "" ? Integer.parseInt(resetTimer) : 10;
    }
}
