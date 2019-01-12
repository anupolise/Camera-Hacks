package com.example.anu.camerahacx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions;

public class Camera_capture extends Activity implements SurfaceHolder.Callback {

    private Camera mCamera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Button capture_image;

    Task<List<FirebaseVisionLabel>> result = null;
    FirebaseVisionLabelDetectorOptions options =
            new FirebaseVisionLabelDetectorOptions.Builder()
                    .setConfidenceThreshold(0.8f)
                    .build();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_layout);
        capture_image = (Button) findViewById(R.id.capture_image);
        capture_image.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                capture();
            }
        });
        surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(Camera_capture.this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        try {
            mCamera = Camera.open();
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();

            Camera.Parameters params= mCamera.getParameters();
            params.set("rotation", 180);
            mCamera.setParameters(params);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void capture() {
        mCamera.takePicture(null, null, null, new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Toast.makeText(getApplicationContext(), "Picture Taken",
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.putExtra("image_arr", data);
                setResult(RESULT_OK, intent);
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
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
                camera.startPreview();

            }
        });

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

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.e("Surface Changed", "format   ==   " + format + ",   width  ===  "
                + width + ", height   ===    " + height);
        mCamera.setDisplayOrientation(90);
        Camera.Parameters p = mCamera.getParameters();
        p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);


        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            Camera.Parameters params= mCamera.getParameters();
            params.set("rotation", 180);
            mCamera.setParameters(params);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e("Surface Created", "");
        mCamera=Camera.open();
        mCamera.setDisplayOrientation(90);
        Camera.Parameters p = mCamera.getParameters();
        p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                                         

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("Surface Destroyed", "");
        mCamera.release();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
    }




}