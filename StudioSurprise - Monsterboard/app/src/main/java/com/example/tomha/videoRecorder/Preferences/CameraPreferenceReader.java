package com.example.tomha.videoRecorder.Preferences;

import android.content.Context;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;

import com.example.tomha.videoRecorder.R;

/**
 * Created by tomha on 14-3-2018.
 */

public class CameraPreferenceReader extends PreferenceReader {

    public CameraPreferenceReader (Context context){
        super(context);
    }

    public int getMaxRecordingLength()
    {
        int maxLength = Integer.parseInt(super.getSharedPreferenceValue(context.getString(R.string.pref_key_maxRecordingLength)));
        if(maxLength < 0){
            maxLength = 60;
        }
        return maxLength;
    }

    public CamcorderProfile getSavedCamcorderProfile(){
        CamcorderProfile profile;
        switch(super.getSharedPreferenceValue(context.getString(R.string.pref_key_resolution)))
        {
            case "480": {
                profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
                break;
            }
            case "720": {
                profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
                break;
            }
            case "1080": {
                profile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
                break;
            }
            default: {
                profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
                break;
            }
        }
        return profile;
    }

    public int getSavedAudioSource(){
        int audioSource;
        switch (super.getSharedPreferenceValue(context.getString(R.string.pref_key_audioSource))){
            case "Camcorder": {
                audioSource = MediaRecorder.AudioSource.CAMCORDER;
                break;
            }
            case "Default": {
                audioSource = MediaRecorder.AudioSource.DEFAULT;
                break;
            }
            case "Mic": {
                audioSource = MediaRecorder.AudioSource.MIC;
                break;
            }
            case "Voice uplink": {
                audioSource = MediaRecorder.AudioSource.VOICE_UPLINK;
                break;
            }
            case "Voice recognition": {
                audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
                break;
            }
            case "Voice call": {
                audioSource = MediaRecorder.AudioSource.VOICE_CALL;
                break;
            }
            case "Voice communication": {
                audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
                break;
            }
            default:
                audioSource = MediaRecorder.AudioSource.CAMCORDER;
                break;
        }
        return audioSource;
    }

    public boolean getMaxLengthEnabled(){
        return super.getSharedPreferenceBooleanValue(context.getString(R.string.pref_key_maxRecordingLengthEnabled));
    }

    public int getZoomLevel(){
        String preferenceValue = super.getSharedPreferenceValue(context.getString(R.string.pref_key_zoomlevel));
        return preferenceValue == "" ? 0 : Integer.valueOf(preferenceValue);
    }

    public String getFolderName(){
        return super.getSharedPreferenceValue(context.getString(R.string.pref_key_folderName));
    }
}
