package com.example.anu.camerahacx;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public final String APP_TAG = "CameraHacx";
    private static final int REQ_CODE_SPEECH_INPUT = 200;
    private static final int MY_CAMERA_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(getApplicationContext());

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED){


        } else if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            //Permission not granted, ask for permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    MY_CAMERA_REQUEST_CODE);
        }

        listeningIntent();

    }

    public void listeningIntent() {
        try {
            Intent intentV2S = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intentV2S.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intentV2S.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intentV2S.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something!");
            startActivityForResult(intentV2S, REQ_CODE_SPEECH_INPUT);
        }catch(ActivityNotFoundException a) {
            Log.e("ERROR2","Could not find activity");
        }

    }

    public boolean openCamera()
    {
        if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera

            String CUSTOM_ACTION = "com.example.anu.camerahacx.Camera_capture";

            Intent i = new Intent(MainActivity.this,Camera_capture.class);
            startActivityForResult(i,90);


            return true;
        }
        return false;
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        String strSaid = new String();
        Log.d("IMPO", "HELLLOOOO");
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == REQ_CODE_SPEECH_INPUT){
            ArrayList<String> list = intent.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            if(list.size() == 0)
            {
                strSaid = "";
                return;
            }
            strSaid = list.get(0);
            Log.d("IMPO", strSaid);

        };
       openCamera();

    }



    private String generateFileName()
    {

        return "Image"+"_"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())+".jpg";

    }

    private void createDirectoryAndSaveFile(String fileName, ImageView ivView)
    {
        Bitmap img = ivView.getDrawingCache();
        File mediaStorageDir = new File( getExternalFilesDir(Environment.DIRECTORY_PICTURES), APP_TAG);
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()){
            Log.d(APP_TAG, "failed to create directory");
        }

        File file = new File(mediaStorageDir.getPath() + File.separator + fileName);

        if (file.exists())
        {
            file.delete();
        }

        try {
            FileOutputStream out = new FileOutputStream(file);
            img.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
