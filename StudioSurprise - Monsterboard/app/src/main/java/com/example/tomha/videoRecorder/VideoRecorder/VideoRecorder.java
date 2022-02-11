package com.example.tomha.videoRecorder.VideoRecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.SeekBar;

import androidx.annotation.NonNull;

import com.example.tomha.videoRecorder.Preferences.CameraPreferenceReader;
import com.example.tomha.videoRecorder.R;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class VideoRecorder {
    private SurfaceHolder mPreviewSurfaceHolder;
    public Camera mCamera;
    private MediaRecorder mMediaRecorder;

    public boolean isRecording() {
        return recording;
    }

    private boolean recording = false;
    private boolean recorderInitialized = false;

    private CameraPreferenceReader pr;
    private boolean prefMaxRecordingLengthEnabled;
    private int prefMaxRecordingLength;
    private int prefAudioSource;
    private int zoomLevel;
    private CamcorderProfile prefProfile;

    private Context mContext;
    private IRecorderCallback callback;

    public VideoRecorder(Context context, SurfaceHolder previewSurfaceHolder){
        mContext = context;
        mPreviewSurfaceHolder = previewSurfaceHolder;
        pr = new CameraPreferenceReader(mContext);
        updatePreferences();
        mCamera = CameraHelper.getDefaultFrontFacingCameraInstance();
        mPreviewSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                if(mCamera == null) {
                    mCamera = CameraHelper.getDefaultFrontFacingCameraInstance();
                }
                try {
                    Camera.Parameters parameters = mCamera.getParameters();
                    parameters.setZoom(zoomLevel);
                    mCamera.setParameters(parameters);
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
                shutdown();
            }
        });
        mPreviewSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        if (mContext instanceof IRecorderCallback) {
            callback = (IRecorderCallback) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement IRecorderCallback");
        }
    }

    private void shutdown(){
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

    private void updatePreferences(){
        prefMaxRecordingLengthEnabled = pr.getMaxLengthEnabled();
        if(prefMaxRecordingLengthEnabled) {
            prefMaxRecordingLength = pr.getMaxRecordingLength();
        }
        prefAudioSource = pr.getSavedAudioSource();
        prefProfile = pr.getSavedCamcorderProfile();
        zoomLevel = pr.getZoomLevel();
    }

    public String startRecording() throws Exception {
        return startRecording("default");
    }
    public String startRecording(String fileName) throws Exception {
        if(recording) throw new Exception("Recording already running");
        String recordedFileName = "";
        if(!recorderInitialized){
            try {
                recordedFileName = initRecorder(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mCamera.unlock();
        mMediaRecorder.start();
        recording = true;
        callback.onRecordingStarted();
        return recordedFileName;
    }

    public void stopRecording(){
        if(isRecording()) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            recording = false;
            recorderInitialized = false;
            callback.onRecordingStopped();
        }
    }

    private static long getAvailableInternalMemorySize() {
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

    public String initRecorder(String fileName) throws IOException {
        if(getAvailableInternalMemorySize() < 500000000L){
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("Insufficient memory");
            builder.setMessage("Er is onvoldoende ruimte beschikbaar. Gelieve de operator om hulp vragen.");

            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.show();
        }

        // It is very important to unlock the camera before doing setCamera
        // or it will results in a black preview
        if (mMediaRecorder == null) mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setPreviewDisplay(mPreviewSurfaceHolder.getSurface());
        mMediaRecorder.setCamera(mCamera);
        if(prefMaxRecordingLengthEnabled){
            VideoLimiter vl = new VideoLimiter();
            vl.observeLimit(prefMaxRecordingLength);
        }
        mMediaRecorder.setAudioSource(prefAudioSource);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setProfile(prefProfile);

        if(fileName == null){
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss").format(new Date());
            fileName = "/video_" + timeStamp + ".mp4";
        }
        Uri videoUri;
        ParcelFileDescriptor file;
        ContentValues values = new ContentValues(5);
        values.put(MediaStore.Video.Media.TITLE, fileName);
        values.put(MediaStore.Video.Media.DATE_ADDED, (int) (System.currentTimeMillis() / 1000));
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/StudioSurprise/");

        videoUri = this.mContext.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        file = this.mContext.getContentResolver().openFileDescriptor(videoUri, "wt");

        //String dir;
        //if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        //    dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/StudioSurprise";
        //} else {
        //dir = getFilesDir().getAbsolutePath();
        //}
        //dir = mContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath();
        //mMediaRecorder.setOutputFile(dir + fileName);
        FileDescriptor fileDescriptor = file.getFileDescriptor();
        mMediaRecorder.setOutputFile(fileDescriptor);

        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        recorderInitialized = true;
        return fileName;
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
                stopRecording();
            }
        }
    }
}