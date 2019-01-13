package com.example.anu.camerahacx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
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
    private boolean shouldCapture = false;

    String strSaid = new String();

    TreeMap<String,Double> firebaseConfVariables = new TreeMap<>();

    TextToSpeech tts;
    boolean ttsReady =  false;


    Task<List<FirebaseVisionLabel>> result = null;
    FirebaseVisionLabelDetectorOptions options =
            new FirebaseVisionLabelDetectorOptions.Builder()
                    .setConfidenceThreshold(0.8f)
                    .build();

    private static final int REQ_CODE_SPEECH_INPUT = 200;


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

    public void talkingIntent(String text){
        if(ttsReady){
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null); }
        shouldCapture = true;


    }

    public String talkingLogic(String spokenWord){
        capture();
        String returnStr = new String();
        if(spokenWord.contains("take"))
        {
            //TODO: find a way to save picture
            returnStr = "Picture taken!";
        }
        else if(spokenWord.contains("what"))
        {
            returnStr = "I see ";
            boolean first = true;
            for (String key : firebaseConfVariables.keySet()) {
                if(first && firebaseConfVariables.get(key)<.60)
                {
                    returnStr="Sorry, I don't know";
                    break;
                }
                else
                {
                    first = false;
                    if(firebaseConfVariables.get(key)>=.60)
                        returnStr+=key+", ";
                }

            }
        }
        else if(spokenWord.contains("in"))
        {
            for (String key : firebaseConfVariables.keySet()) {
                if(spokenWord.contains(key))
                {
                    returnStr="Yes";
                    break;
                }

            }

            returnStr="No";

        }

        if(returnStr.equals("")) {
            returnStr = "Sorry, I didn't catch that";
        }
        return returnStr;
    }



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

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                ttsReady = true;
            }
        });
        listeningIntent();
    }



    private void capture() {
        Log.d("IMPO", "capture Called");
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
                String result1 = talkingLogic(strSaid);
                //String result1 = "happy brithday";
                talkingIntent(result1);
                if(shouldCapture)
                {
                    shouldCapture = false;
                    listeningIntent();
                }



            }
        });

    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        String result = new String();
        Log.d("IMPO", "HELLLOOOO2");
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == REQ_CODE_SPEECH_INPUT){
            if(intent==null)
            {
                Log.e("ERROR", "intent is null");
                return;
            }
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
       String resutle =  talkingLogic(strSaid);
       talkingIntent(resutle);




    }


    public void printResults(){
        for (FirebaseVisionLabel label:result.getResult())
        {
            String text = label.getLabel();
            String entityId = label.getEntityId();
            double confidence = label.getConfidence();
            firebaseConfVariables.put(text,confidence);
            Log.d("VALUES",text +" "+confidence);
        }

    }
    public boolean openCamera()
    {
        if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera

            String CUSTOM_ACTION = "com.example.anu.camerahacx.Camera_capture";

            Intent i = new Intent(this ,Camera_capture.class);
            startActivityForResult(i,90);

            return true;
        }
        return false;
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
       // mCamera.release();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.stopPreview();
           // mCamera.release();
        }
    }




}