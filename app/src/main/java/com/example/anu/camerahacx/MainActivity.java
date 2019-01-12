package com.example.anu.camerahacx;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
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
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int MY_CAMERA_REQUEST_CODE = 100;
    public final static int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1034;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    Task<List<FirebaseVisionLabel>> result = null;
    public final String APP_TAG = "CameraHacx";


    FirebaseVisionLabelDetectorOptions options =
            new FirebaseVisionLabelDetectorOptions.Builder()
                    .setConfidenceThreshold(0.8f)
                    .build();




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

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        ImageView imgView = new ImageView(this);
        Bitmap bitmapPhoto = null;
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                bitmapPhoto = (Bitmap) data.getExtras().get("data");
                imgView.setImageBitmap(bitmapPhoto);
                imgView.setVisibility(View.VISIBLE);

                // store img
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmapPhoto, "IMG"+new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) , "");


                createDirectoryAndSaveFile(generateFileName(), imgView);

                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmapPhoto);
                FirebaseVisionLabelDetector detector = FirebaseVision.getInstance()
                        .getVisionLabelDetector();

                result =
                        detector.detectInImage(image)
                                .addOnSuccessListener(
                                        new OnSuccessListener<List<FirebaseVisionLabel>>() {
                                            @Override
                                            public void onSuccess(List<FirebaseVisionLabel> labels) {
                                                // Task completed successfully
                                                // ...
                                                printResults();

                                            }
                                        })
                                .addOnFailureListener(
                                        new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // Task failed with an exception
                                                // ...
                                            }
                                        });

                Toast.makeText(this, "Picture saved!", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

                Log.d("ANU", "we took pic");






            } else {
                Toast.makeText(this, "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void printResults(){
        for (FirebaseVisionLabel label:result.getResult())
        {
            String text = label.getLabel();
            String entityId = label.getEntityId();
            float confidence = label.getConfidence();
            Log.d("VALUES",text +" "+confidence);
        }

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
