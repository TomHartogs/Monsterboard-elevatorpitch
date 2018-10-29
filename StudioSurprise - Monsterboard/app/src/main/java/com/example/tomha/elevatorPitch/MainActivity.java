package com.example.tomha.elevatorPitch;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int PERMISSION_ALL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            Toast.makeText(this, "Permission for camera, mic and external storage is required to run the app!", Toast.LENGTH_LONG).show();
        } else {
            /*setContentView(R.layout.elevator_doors);

            TranslateAnimation moveLefttoRight = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, (float) 0, Animation.RELATIVE_TO_PARENT, (float) 1.0, Animation.RELATIVE_TO_PARENT, (float) 0, Animation.RELATIVE_TO_PARENT, (float) 0);
            moveLefttoRight.setDuration(3000);
            moveLefttoRight.setFillAfter(true);
            TranslateAnimation moveRighttoLeft = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, (float) 0, Animation.RELATIVE_TO_PARENT, (float) -1.0, Animation.RELATIVE_TO_PARENT, (float) 0, Animation.RELATIVE_TO_PARENT, (float) 0);
            moveRighttoLeft.setDuration(3000);
            moveRighttoLeft.setFillAfter(true);
            ImageView door2 = findViewById(R.id.door2);
            ImageView door1 = findViewById(R.id.door1);
            door2.startAnimation(moveLefttoRight);
            door1.startAnimation(moveRighttoLeft);*/
            startRecordingActivity();
        }
    }

    public static boolean hasPermissions(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if(grantResults.length > 0) {
            ArrayList<String> deniedPermissions = new ArrayList<>();
            for (int i = 0; i < grantResults.length; i++){
                if(grantResults[i] == PackageManager.PERMISSION_DENIED){
                    deniedPermissions.add(permissions[i]);
                }
            }
            if(!deniedPermissions.isEmpty()){
                ActivityCompat.requestPermissions(this, deniedPermissions.toArray(new String[0]), requestCode);
            } else if (hasPermissions(this, PERMISSIONS)){
                startRecordingActivity();
            }
        }
    }

    private void startRecordingActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }
}

