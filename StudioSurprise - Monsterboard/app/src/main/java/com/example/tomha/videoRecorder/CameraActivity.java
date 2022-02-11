package com.example.tomha.videoRecorder;

/**
 * Created by tomha on 27-2-2018.
 */


import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.motion.widget.MotionLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.example.tomha.videoRecorder.Preferences.CameraPreferenceReader;
import com.example.tomha.videoRecorder.Preferences.SettingsActivity;
import com.example.tomha.videoRecorder.VideoRecorder.IRecorderCallback;
import com.example.tomha.videoRecorder.VideoRecorder.VideoRecorder;

import org.w3c.dom.Text;

import java.io.IOException;


public class CameraActivity extends Activity implements IRecorderCallback {
    private static final int SETTINGS_REQUEST = 0;

    private LottieAnimationView recordingButton;
    private LottieAnimationView countDownTimer;
    private LottieAnimationView restartButton;
    private LottieAnimationView recording;
    private String welcomeMessage;
    private String recordingFinishedMessage;
    private Integer resetTimer;
    private TextSwitcher mTextSwitcher;
    private Boolean restart = false;
    private VideoRecorder videoRecorder;
    private String mPreviousFileName;

    private CameraPreferenceReader pr;
    private CountDownTimer secretMenuTimer = new CountDownTimer(1000, 1000) {
        public void onTick(long millisUntilFinished) {
            //Do nothing
        }
        public void onFinish() {
            if (pr.getSharedPreferenceBooleanValue(getString(R.string.pref_key_passwordEnabled))) {
                showPasswordPrompt();
            } else {
                openSettingsMenu();
            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_recorder_recipe);

        pr = new CameraPreferenceReader(this);
        videoRecorder = new VideoRecorder(this, ((SurfaceView)findViewById(R.id.surfaceView)).getHolder());
        countDownTimer = findViewById(R.id.countdown);
        countDownTimer.addAnimatorUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                Double value = Double.parseDouble(valueAnimator.getAnimatedValue().toString());
                if(value > 0.8 && !videoRecorder.isRecording()){
                    try {
                        mPreviousFileName = restart ? videoRecorder.startRecording(mPreviousFileName): videoRecorder.startRecording();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        countDownTimer.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
            });
        restartButton = findViewById(R.id.restartButton);
        restartButton.setVisibility(View.INVISIBLE);

        recording = findViewById(R.id.recording);
        recordingButton = findViewById(R.id.recordingButton);
        recordingButton.setMinAndMaxProgress(0.2f, 0.5f);
        recordingButton.setProgress(0.2f);

        updatePreferences();
        mTextSwitcher = findViewById(R.id.textSwitcher);
        mTextSwitcher.setInAnimation(this, android.R.anim.slide_in_left);
        mTextSwitcher.setOutAnimation(this, android.R.anim.slide_out_right);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
                onRecordButtonClick();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onRecordButtonClick(){
        onRecordButtonClick(findViewById(R.id.recordingButton));
    }

    public void onRecordButtonClick(View v){
        if (videoRecorder.isRecording()) {
            videoRecorder.stopRecording();
        } else {
            if(countDownTimer.isAnimating()){
                countDownTimer.cancelAnimation();
                countDownTimer.setFrame(0);
                ((MotionLayout)findViewById(R.id.motionLayout)).transitionToStart();
                recordingButton.setSpeed(-1);
                recordingButton.playAnimation();
            } else {
                ((MotionLayout) findViewById(R.id.motionLayout)).transitionToEnd();
                if (recordingButton.getSpeed() == -1) recordingButton.setSpeed(1);
                recordingButton.playAnimation();
                countDownTimer.playAnimation();
            }
        }
    }

    public void onRestartButtonClick(View v) {
        restart = true;
        onRecordButtonClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        final int action = e.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int fingerCount = e.getPointerCount();
                if (fingerCount == 2) {
                    // We have four fingers touching, so start the timer
                    secretMenuTimer.cancel();
                    secretMenuTimer.start();
                    //twoFingerDownTime = System.currentTimeMillis();
                } else if (fingerCount > 2) {
                    secretMenuTimer.cancel();
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int fingerCount = e.getPointerCount() - 1;
                 if (fingerCount < 2) {
                    // Fewer than four fingers, so reset the timer
                    secretMenuTimer.cancel();
                    //twoFingerDownTime = -1;
                }
                else if (fingerCount == 2) {
                    secretMenuTimer.cancel();
                    secretMenuTimer.start();
                }
            }
        }
        return true;
    }

    private void showPasswordPrompt() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.password_prompt, null);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setView(promptsView);

        final EditText userInput = promptsView.findViewById(R.id.editTextPasswordInput);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (userInput.getText().toString().equals(pr.getSharedPreferenceValue(getString(R.string.pref_key_password)))) {
                    openSettingsMenu();
                } else {
                    Toast message = Toast.makeText(getApplicationContext(), "Wrong password entered", Toast.LENGTH_SHORT);
                    message.show();
                    showPasswordPrompt();
                }
            }
        });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        alertDialogBuilder.create().show();
    }

    private void updatePreferences(){
        welcomeMessage = pr.getSharedPreferenceValue(getString(R.string.pref_key_welcomeMessage));
        recordingFinishedMessage = pr.getSharedPreferenceValue(getString(R.string.pref_key_recordingFinishedMessage));
        ((TextView)findViewById(R.id.welcomeMessage)).setText(welcomeMessage);
        ((TextView)findViewById(R.id.recordingFinishedMessage)).setText(recordingFinishedMessage);

        String resetPreference = pr.getSharedPreferenceValue(getString(R.string.pref_key_resetTimer));
        if(resetPreference == "") resetPreference = "10";
        resetTimer = Integer.parseInt(resetPreference);
    }

    private void openSettingsMenu(){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, SETTINGS_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == SETTINGS_REQUEST) {
            updatePreferences();
        }
    }

    @Override
    public void onRecordingStarted() {
        recording.getHandler().post(new Runnable() {
            @Override
            public void run() {
                recording.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onRecordingStopped() {
        ((MotionLayout)findViewById(R.id.motionLayout)).transitionToStart();
        restart = false;
        recordingButton.setSpeed(-1);
        recordingButton.playAnimation();
        mTextSwitcher.setText(recordingFinishedMessage);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                restartButton.setVisibility(View.VISIBLE);
                restartButton.playAnimation();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        restartButton.setVisibility(View.INVISIBLE);
                        mTextSwitcher.setText(welcomeMessage);
                    }
                }, resetTimer * 1000);
            }
        }, 4000);
        findViewById(R.id.recording).setVisibility(View.INVISIBLE);
    }
}