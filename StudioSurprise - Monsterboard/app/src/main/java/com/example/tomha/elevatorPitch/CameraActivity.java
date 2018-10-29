package com.example.tomha.elevatorPitch;

/**
 * Created by tomha on 27-2-2018.
 */

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tomha.elevatorPitch.Preferences.CameraPreferenceReader;
import com.example.tomha.elevatorPitch.Preferences.SettingsActivity;

import java.io.File;

public class CameraActivity extends Activity implements SurfaceHolder.Callback, View.OnTouchListener {

    private MediaRecorder mMediaRecorder;
    private Camera mCamera;
    private boolean recording = false;
    private TextView resterendeText;
    private SurfaceHolder mHolder;

    private static final int SETTINGS_REQUEST = 0;
    private static final int FIVE_SECONDS = 5 * 1000; // 5s * 1000 ms/s

    private SlidingDoor door1;
    private SlidingDoor door2;
    private TextView nrlbl;
    private TextView nrTxt;

    private boolean prefMaxRecordingLengthEnabled;
    private int prefMaxRecordingLength;
    private int prefAudioSource;
    private CamcorderProfile prefProfile;
    private boolean prefCountdownTimerEnabled;

    private CameraPreferenceReader pr;
    private CountDownTimer timeLeftTimer;
    private CountDownTimer secretMenuTimer = new CountDownTimer(FIVE_SECONDS, 1000) {
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

    public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize, availableBlocks;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = stat.getBlockSizeLong();
            availableBlocks = stat.getAvailableBlocksLong();
        } else {
            blockSize = stat.getBlockSize();
            availableBlocks = stat.getAvailableBlocks();
        }
        return availableBlocks * blockSize;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_recorder_recipe);

        resterendeText = findViewById(R.id.resterendTextView);

        door2 = new SlidingDoor((ImageView)findViewById(R.id.door2), 0);
        door1 = new SlidingDoor((ImageView)findViewById(R.id.door1), 1);

        pr = new CameraPreferenceReader(this);
        updatePreferences();
        mHolder = ((SurfaceView)findViewById(R.id.surfaceView)).getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        findViewById(R.id.door1).setOnTouchListener(this);
        findViewById(R.id.door2).setOnTouchListener(this);
        nrlbl = findViewById(R.id.nrLbl);
        nrTxt = findViewById(R.id.nrTxt);
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
        final Button mButton = (Button) v;
        if (!recording) {
            if(getAvailableInternalMemorySize() < 500000000L){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Insufficient memory");
                builder.setMessage("Er is onvoldoende ruimte beschikbaar. Gelieve de operator om hulp vragen.");

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.show();
            } else {
                final String lineNr = ((EditText) findViewById(R.id.nrTxt)).getText().toString();
                File file = CameraHelper.getOutputMediaFileConcrete(CameraHelper.MEDIA_TYPE_VIDEO, lineNr);
                if (file.exists()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Existing entry");
                    builder.setMessage("Er bestaat al een opname met dit nummer. Wil je deze overschrijven? (Indien deze opname niet van jou is, gelieve de operator te roepen)");

                    // Set up the buttons
                    builder.setPositiveButton("Doorgaan", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startRecording(mButton, lineNr);
                        }
                    });
                    builder.setNegativeButton("Annuleren", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    builder.show();
                } else {
                    startRecording(mButton, lineNr);
                }
            }
        } else {
            recording = false;
            try {
                mMediaRecorder.stop();
            } catch(RuntimeException e) {

            }
            door1.close();
            door2.close();

            mMediaRecorder.reset();
            if (prefMaxRecordingLengthEnabled && prefCountdownTimerEnabled) {
                timeLeftTimer.cancel();
                resterendeText.setVisibility(View.INVISIBLE);
            }
            mButton.setVisibility(View.INVISIBLE);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    mButton.setVisibility(View.VISIBLE);
                    mButton.setText(R.string.start);
                    mButton.getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
                    nrTxt.setText("");
                    nrlbl.setVisibility(View.VISIBLE);
                    nrTxt.setVisibility(View.VISIBLE);
                    EditText myEditText = findViewById(R.id.nrTxt);

                    ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                            .showSoftInput(myEditText, InputMethodManager.SHOW_FORCED);
                }

            }, 3000); // 3000ms delay
        }
    }

    private void startRecording(final Button mButton, String lineNr){
        try {
            initRecorder(mHolder.getSurface(), lineNr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.unlock();
        door1.open();
        door2.open();
        final TextView timer = new TextView(getApplicationContext());
        timer.setTextSize(200);
        timer.setTextColor(Color.WHITE);
        final ConstraintLayout layout = ((ConstraintLayout) findViewById(R.id.constraintLayout));
        layout.addView(timer);
        ConstraintSet set = new ConstraintSet();
        set.clone(layout);
        set.connect(timer.getId(), ConstraintSet.TOP, layout.getId(), ConstraintSet.TOP, 0);
        set.connect(timer.getId(), ConstraintSet.BOTTOM, layout.getId(), ConstraintSet.BOTTOM, 0);
        set.connect(timer.getId(), ConstraintSet.LEFT, layout.getId(), ConstraintSet.LEFT, 0);
        set.connect(timer.getId(), ConstraintSet.RIGHT, layout.getId(), ConstraintSet.RIGHT, 0);
        set.applyTo(layout);
        CountDownTimer countDownTimer = new CountDownTimer(3000, 500) {
            public void onTick(long millisUntilFinished) {
                if(millisUntilFinished > 2000)
                    timer.setText("3");
                else if(millisUntilFinished < 2000 && millisUntilFinished > 1000)
                    timer.setText("2");
                else if(millisUntilFinished < 1000)
                    timer.setText("1");
            }
            public void onFinish() {
                layout.removeView(timer);
            }
        };
        countDownTimer.start();
        nrlbl.setVisibility(View.INVISIBLE);
        nrTxt.setVisibility(View.INVISIBLE);
        mButton.setVisibility(View.INVISIBLE);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                mMediaRecorder.start();
                recording = true;
                mButton.setVisibility(View.VISIBLE);
                mButton.setText(R.string.stop);
                mButton.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                if (prefMaxRecordingLengthEnabled && prefCountdownTimerEnabled) {
                    setTimeLeftTimer(prefMaxRecordingLength);
                    timeLeftTimer.start();
                }
            }

        }, 3000); // 3000ms delay
    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {
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

    private void openSettingsMenu(){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, SETTINGS_REQUEST);
    }

    private void setTimeLeftTimer(int maxTime){
        timeLeftTimer = new CountDownTimer(maxTime * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                resterendeText.setText("Seconden resterend: " + millisUntilFinished / 1000);
                resterendeText.setVisibility(View.VISIBLE);
            }

            public void onFinish() {
                resterendeText.setVisibility(View.INVISIBLE);
            }
        };
    }

    private void updatePreferences(){
        prefMaxRecordingLengthEnabled = pr.getMaxLengthEnabled();
        if(prefMaxRecordingLengthEnabled) {
            prefMaxRecordingLength = pr.getMaxRecordingLength();
            prefCountdownTimerEnabled = pr.getCountdownTimerEnabled();
            if(prefCountdownTimerEnabled) {
                setTimeLeftTimer(prefMaxRecordingLength);
            }
        }
        prefAudioSource = pr.getSavedAudioSource();
        prefProfile = pr.getSavedCamcorderProfile();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == SETTINGS_REQUEST) {
            updatePreferences();
        }
    }

    /* Init the MediaRecorder, the order the methods are called is vital to
     * its correct functioning */
    private void initRecorder(Surface surface) throws IOException {
        // It is very important to unlock the camera before doing setCamera
        // or it will results in a black preview

        if (mMediaRecorder == null) mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setPreviewDisplay(surface);
        if(mCamera == null){
            initCamera();
        }
        mMediaRecorder.setCamera(mCamera);
        if(prefMaxRecordingLengthEnabled){
            VideoLimiter vl = new VideoLimiter();
            vl.observeLimit(prefMaxRecordingLength);
        }
        mMediaRecorder.setAudioSource(prefAudioSource);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setProfile(prefProfile);
        File mOutputFile;
        mOutputFile = CameraHelper.getOutputMediaFileDate(CameraHelper.MEDIA_TYPE_VIDEO, "StudioSurprise");

        if (mOutputFile != null) {
            mMediaRecorder.setOutputFile(mOutputFile.getPath());
        }
        else{
            mMediaRecorder = null;
        }

        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            // This is thrown if the previous calls are not called with the
            // proper order
            e.printStackTrace();
        }
    }

    /* Init the MediaRecorder, the order the methods are called is vital to
  * its correct functioning */
    private void initRecorder(Surface surface, String fileName) throws IOException {
        // It is very important to unlock the camera before doing setCamera
        // or it will results in a black preview

        if (mMediaRecorder == null) mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setPreviewDisplay(surface);
        if(mCamera == null){
            initCamera();
        }
        mMediaRecorder.setCamera(mCamera);
        if(prefMaxRecordingLengthEnabled){
            VideoLimiter vl = new VideoLimiter();
            vl.observeLimit(prefMaxRecordingLength);
        }
        mMediaRecorder.setAudioSource(prefAudioSource);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setProfile(prefProfile);
        File mOutputFile;
        mOutputFile = CameraHelper.getOutputMediaFileConcrete(CameraHelper.MEDIA_TYPE_VIDEO, fileName);

        if (mOutputFile != null) {
            mMediaRecorder.setOutputFile(mOutputFile.getPath());
        }
        else{
            mMediaRecorder = null;
        }

        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            // This is thrown if the previous calls are not called with the
            // proper order
            e.printStackTrace();
        }
    }

    private void initCamera(){
        mCamera = CameraHelper.getDefaultFrontFacingCameraInstance();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initCamera();
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        shutdown();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    private void shutdown() {
        // Release MediaRecorder and especially the Camera as it's a shared
        // object that can be used by other applications
        if(mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if(mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    public class VideoLimiter extends Activity implements MediaRecorder.OnInfoListener {

        public void observeLimit(int delay) {
            // Normal MediaRecorder Setup
            mMediaRecorder.setMaxDuration(delay * 1000);
            mMediaRecorder.setOnInfoListener(this);
        }

        public void onInfo(MediaRecorder mr, int what, int extra) {
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                Log.v("VIDEOCAPTURE","Maximum Duration Reached");
                onRecordButtonClick();
            }
        }
    }
}