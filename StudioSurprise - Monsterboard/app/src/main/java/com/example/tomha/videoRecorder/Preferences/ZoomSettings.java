package com.example.tomha.videoRecorder.Preferences;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.SeekBar;

import com.example.tomha.videoRecorder.R;
import com.example.tomha.videoRecorder.VideoRecorder.CameraHelper;

import java.io.IOException;

public class ZoomSettings extends Activity {
    private Camera mCamera;
    private SeekBar mSeekBar;
    private SurfaceHolder mPreviewSurfaceHolder;
    private Float mDist = 0.0f;
    private int zoomLevel = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_zoom_settings);
        mPreviewSurfaceHolder = ((SurfaceView)findViewById(R.id.surfaceView)).getHolder();
        ((SurfaceView)findViewById(R.id.surfaceView)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                Camera.Parameters params = mCamera.getParameters();
                int action = event.getAction();


                if (event.getPointerCount() > 1) {
                    // handle multi-touch events
                    if (action == MotionEvent.ACTION_POINTER_DOWN) {
                        mDist = getFingerSpacing(event);
                    } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
                        ///mCamera.cancelAutoFocus();
                        handleZoom(event, params);
                    }
                } else {
//                    // handle single touch events
//                    if (action == MotionEvent.ACTION_UP) {
//                        handleFocus(event, params);
//                    }
                }
                return true;
            }
        });
        mPreviewSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                if(mCamera == null) {
                    mCamera = CameraHelper.getDefaultFrontFacingCameraInstance();
                }
                try {
                    Camera.Parameters params = mCamera.getParameters();
                    params.setZoom(zoomLevel);
                    mCamera.setParameters(params);
                    mCamera.setPreviewDisplay(surfaceHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCamera.startPreview();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                mCamera.release();
                mCamera = null;
            }
        });
        mPreviewSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mCamera = CameraHelper.getDefaultFrontFacingCameraInstance();
        zoomLevel = getIntent().getIntExtra(getString(R.string.pref_key_zoomlevel), 0);
        mSeekBar = findViewById(R.id.seekBar);
        mSeekBar.setMax( mCamera.getParameters().getMaxZoom() * 5);
        mSeekBar.setProgress(zoomLevel*5);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Camera.Parameters params = mCamera.getParameters();
                zoomLevel = i;
                params.setZoom(i/5);
                mCamera.setParameters(params);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom() * 5;
        //zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > mDist) {
            //zoom in
            if (zoomLevel < maxZoom)
                zoomLevel++;
        } else if (newDist < mDist) {
            //zoom out
            if (zoomLevel > 0)
                zoomLevel--;
        }
        mDist = newDist;
        params.setZoom(zoomLevel /5);
        mSeekBar.setProgress(zoomLevel);
        mCamera.setParameters(params);
    }

    public void onConfirmButtonClick(View v){
        Intent resultIntent = new Intent();
        resultIntent.putExtra(getString(R.string.pref_key_zoomlevel), zoomLevel /5);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private float getFingerSpacing(MotionEvent event) {
        // ...
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }
}