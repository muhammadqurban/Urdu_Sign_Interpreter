package com.example.signlanguagetranslator;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;

import java.util.ArrayList;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private int cameraPosition = -1, whichFragment = 1;
    private ImageButton cancelSpeech, startScan, speaker, voiceScanner, cancelSigns;
    private Spinner languageSpinner;
    String selectedLanguage; //is public so fragment2 can access it
    private TextInputLayout userText;
    private TextView signT, userS;
    private TextToSpeech tts;
    private boolean firstTimeFragment = false, scanButtonPresssed = false;
    private SpeechRecognizer speechRecognizer;
    private final float forAlpha = 0.0f;
    private String[] Permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getSupportActionBar().setTitle(Html.fromHtml("<font color=\"Gray\">" + getString(R.string.app_name) + "</font>"));

        //asking for my permissions
        if (!hasPermissions(CameraActivity.this, Permissions)) {
            ActivityCompat.requestPermissions(CameraActivity.this, Permissions, 1);
        }

        startScan = findViewById(R.id.startScan);
        speaker = findViewById(R.id.speaker);
        signT = findViewById(R.id.signTranslation);
        userS = findViewById(R.id.userSpeech);
        languageSpinner = findViewById(R.id.language);
        voiceScanner = findViewById(R.id.mic);
        cancelSpeech = findViewById(R.id.userSpeechCross);
        cancelSigns = findViewById(R.id.signCross);
        userText = findViewById(R.id.responseField);

        startScan.setOnClickListener(this);
        speaker.setOnClickListener(this);
        voiceScanner.setOnClickListener(this);
        cancelSpeech.setOnClickListener(this);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedLanguage = languageSpinner.getSelectedItem().toString();
                //whenever the language changes we have to call the fragments again.
                if (firstTimeFragment && scanButtonPresssed) {
                    mainCameraLoader(2);
                }

                if (selectedLanguage.equals("English")) {
                    signT.setText(getResources().getString(R.string.SignTranslationTextView));
                    userS.setText(getResources().getString(R.string.UserResponseTextView));
                } else if (selectedLanguage.equals("Urdu")) {
                    signT.setText(getResources().getString(R.string.SignTranslationTVUr));
                    userS.setText(getResources().getString(R.string.UserResponseTVUr));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //we have nothing to do here
            }
        });

        mainCameraLoader(1); //the application will start with cameraX by default.

        //setting the from animation point of the singT
        signT.setTranslationX(800);
        userS.setTranslationX(800);
        userText.setTranslationX(800);
        cancelSigns.setTranslationX(200);
        cancelSpeech.setTranslationX(200);
        speaker.setTranslationY(100);
        startScan.setTranslationY(100);
        voiceScanner.setTranslationY(100);

        //setting their alpha values
        signT.setAlpha(forAlpha);
        userS.setAlpha(forAlpha);
        userText.setAlpha(forAlpha);
        speaker.setAlpha(forAlpha);
        startScan.setAlpha(forAlpha);
        voiceScanner.setAlpha(forAlpha);

        //now its time to programme some animations
        signT.animate().translationX(0).alpha(1).setDuration(1500).setStartDelay(500).start();
        userS.animate().translationX(0).alpha(1).setDuration(1500).setStartDelay(550).start();
        userText.animate().translationX(0).alpha(1).setDuration(1500).setStartDelay(550).start();
        cancelSigns.animate().translationX(0).setDuration(1500).setStartDelay(600).start();
        cancelSpeech.animate().translationX(0).setDuration(1500).setStartDelay(600).start();
        speaker.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(600).start();
        startScan.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(800).start();
        voiceScanner.animate().translationY(0).alpha(1).setDuration(1000).setStartDelay(1000).start();

    }

    private boolean hasPermissions(Context context, String... PERMISSIONS) {
        if (context != null && PERMISSIONS != null) {
            for (String perms : PERMISSIONS) {
                if (ActivityCompat.checkSelfPermission(context, perms) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Audio Permission Denied", Toast.LENGTH_SHORT).show();

            }
            if (grantResults[1] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Camera Permission require to proceed", Toast.LENGTH_SHORT).show();
                moveTaskToBack(true);
            }
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.startScan) {
            firstTimeFragment = true;
            mainCameraLoader(whichFragment);
        } else if (id == R.id.speaker) {
            if (signT.getText().toString().isEmpty()) {
                Toast.makeText(this, "There is nothing to speak", Toast.LENGTH_SHORT).show();
            } else {
                tts = new TextToSpeech(CameraActivity.this, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            if (selectedLanguage.equals("Urdu")) {
                                int result = tts.setLanguage(new Locale(("ur")));
                                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                } else {
                                    speaker.setEnabled(true);
                                    speakWords();
                                }
                            } else if (selectedLanguage.equals("English")) {
                                int result = tts.setLanguage(Locale.ENGLISH);
                                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                } else {
                                    speaker.setEnabled(true);
                                    speakWords();
                                }
                            }
                        } else {
                            Log.d(TAG, "onInit: initialization failed");
                        }
                    }
                });
            }
        } else if (id == R.id.mic) {
            final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle bundle) {
                }

                @Override
                public void onBeginningOfSpeech() {
                    if (selectedLanguage.equals("English")) {
                        userS.setText("Listening...");
                    } else if (selectedLanguage.equals("Urdu")) {
                        userS.setText("سن رہا ہے...");
                    }
                }

                @Override
                public void onRmsChanged(float v) {
                }

                @Override
                public void onBufferReceived(byte[] bytes) {
                }

                @Override
                public void onEndOfSpeech() {
                }

                @Override
                public void onError(int i) {
                    if (selectedLanguage.equals("English")) {
                        userS.setText(getResources().getString(R.string.mic));
                    } else if (selectedLanguage.equals("Urdu")) {
                        userS.setText(getResources().getString(R.string.micUr));
                    }

                }

                @Override
                public void onResults(Bundle bundle) {
                    ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if(selectedLanguage.equals("Urdu")){
                        translateText(FirebaseTranslateLanguage.UR, FirebaseTranslateLanguage.EN, data.get(0));
                    }else{
                        userS.setText(data.get(0));
                    }
                }

                @Override
                public void onPartialResults(Bundle bundle) {

                }

                @Override
                public void onEvent(int i, Bundle bundle) {

                }
            });
            speechRecognizer.startListening(speechRecognizerIntent);
        } else if (id == R.id.userSpeechCross) {
            if (selectedLanguage.equals("English")) {
                userS.setText(getResources().getString(R.string.UserResponseTextView));
            } else if (selectedLanguage.equals("Urdu")) {
                userS.setText(getResources().getString(R.string.UserResponseTVUr));
            }
        }
    }

    public void mainCameraLoader(int fragmentNo) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (fragmentNo == 1) {
            scanButtonPresssed = false;
            //load the cameraX
            whichFragment = 2;
            fragmentTransaction.replace(R.id.frameLayout, new CameraX());
            fragmentTransaction.commit();
            Toast.makeText(this, "Scanning Off", Toast.LENGTH_SHORT).show();
        } else if (fragmentNo == 2) {
            //load the javaCameraView for recognizing
            scanButtonPresssed = true;
            whichFragment = 1;
            fragmentTransaction.replace(R.id.frameLayout, new JavaCameraView());
            fragmentTransaction.commit();
            Toast.makeText(this, "Scanning On", Toast.LENGTH_SHORT).show();
        }
    }

    private void speakWords() {
        String inputText = signT.getText().toString();
        tts.speak(inputText, tts.QUEUE_FLUSH, null);
    }

    public void translateText(int fromCode, int toCode, String source) {
        FirebaseTranslatorOptions options = new FirebaseTranslatorOptions.Builder().setSourceLanguage(toCode).setTargetLanguage(fromCode).build();
        FirebaseTranslator translator = FirebaseNaturalLanguage.getInstance().getTranslator(options);
        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder().build();
        translator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                translator.translate(source).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        userS.setText(s);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("translation", e.getMessage());
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("translation", e.getMessage());
            }
        });
    }


}