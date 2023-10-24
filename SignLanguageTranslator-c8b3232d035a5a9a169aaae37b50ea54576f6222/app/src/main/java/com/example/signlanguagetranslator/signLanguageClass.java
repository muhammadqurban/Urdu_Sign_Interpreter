package com.example.signlanguagetranslator;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class signLanguageClass {
    private Handler handler;
    private int previousOption;
    private String previousType;
    String[] temp2 = {"","",""};
    private Interpreter interpreter;
    private Interpreter interpreter2;
    private List<String> labelList;
    private int INPUT_SIZE;
    private int PIXEL_SIZE = 3; // for RGB
    private int IMAGE_MEAN = 0;
    private float IMAGE_STD = 255.0f;
    // use to initialize gpu in app
    private GpuDelegate gpuDelegate;
    private int height = 0;
    private int width = 0;
    private int Classification_Input_size = 0;
    private int count=0;
    private TextView finalSignText;
    private TextView currentSelection;
    String sign_val;
    private Boolean isFront;
    private int selectedOption = 0;
    private String temp = "";
    private String englishText = "";
    private String currentLanguage;
    private Context context;


    signLanguageClass(Context con, String lan, Boolean Front, TextView signText, TextView selected, AssetManager assetManager, String modelPath, String labelPath, int inputSize, String classification_model, int classification_input_size) throws IOException {
        isFront = Front;
        finalSignText = signText;
        currentLanguage = lan;
        context = con;
        currentSelection = selected;
        handler = new Handler();

        INPUT_SIZE = inputSize;
        Classification_Input_size = classification_input_size;
        Interpreter.Options options = new Interpreter.Options();
        gpuDelegate = new GpuDelegate();
        options.addDelegate(gpuDelegate);
        options.setNumThreads(4);
        interpreter = new Interpreter(loadModelFile(assetManager, modelPath), options);
        labelList = loadLabelList(assetManager, labelPath);
        Interpreter.Options options2 = new Interpreter.Options();
        options2.setNumThreads(2);
        interpreter2 = new Interpreter(loadModelFile(assetManager, classification_model), options2);

    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        // to store label
        List<String> labelList = new ArrayList<>();
        // create a new reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;
        // looping through each line and store it to labelList
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private ByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    // create new Mat function
    public Mat recognizeImage(Mat mat_image) {
        Mat rotated_mat_image = new Mat();
        Mat a = mat_image.t();
        if (isFront) {
            Core.flip(a, rotated_mat_image, -1);
        } else {
            Core.flip(a, rotated_mat_image, 1);
        }
        a.release();

        Bitmap bitmap = null;
        bitmap = Bitmap.createBitmap(rotated_mat_image.cols(), rotated_mat_image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rotated_mat_image, bitmap);
        height = bitmap.getHeight();
        width = bitmap.getWidth();

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
        //converting the bitmap to byteBuffer
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);

        Object[] input = new Object[1];
        input[0] = byteBuffer;

        Map<Integer, Object> output_map = new TreeMap<>();

        float[][][] boxes = new float[1][10][4];
        float[][] scores = new float[1][10];
        float[][] classes = new float[1][10];

        // add it to object_map;
        output_map.put(0, boxes);
        output_map.put(1, classes);
        output_map.put(2, scores);

        // now predict
        interpreter.runForMultipleInputsOutputs(input, output_map);

        Object value = output_map.get(0);
        Object Object_class = output_map.get(1);
        Object score = output_map.get(2);

        for (int i = 0; i < 10; i++) {

            float class_value = (float) Array.get(Array.get(Object_class, 0), i);
            float score_value = (float) Array.get(Array.get(score, 0), i);
            if (score_value > 0.5) {
                Object box1 = Array.get(Array.get(value, 0), i);
                // we are multiplying it with Original height and width of frame
                //change this x1,y1 and x2,y2 coordinates
                float y1 = (float) Array.get(box1, 0) * height;
                float x1 = (float) Array.get(box1, 1) * width;
                float y2 = (float) Array.get(box1, 2) * height;
                float x2 = (float) Array.get(box1, 3) * width;

                if (y1 < 0) {
                    y1 = 0;
                }
                if (x1 < 0) {
                    x1 = 0;
                }
                if (x2 > width) {
                    x2 = width;
                }
                if (y2 > height) {
                    y2 = height;
                }
                //now set height and width of the box
                float w1 = x2 - x1;
                float h1 = y2 - y1;

                Rect cropped_roi = new Rect((int) x1, (int) y1, (int) w1, (int) h1);
                Mat cropped = new Mat(rotated_mat_image, cropped_roi).clone();
                Bitmap bitmap1 = null;
                bitmap1 = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(cropped, bitmap1);
                Bitmap scaledBitmap1 = Bitmap.createScaledBitmap(bitmap1, Classification_Input_size, Classification_Input_size, false);
                ByteBuffer byteBuffer1 = convertBitmapToByteBuffer1(scaledBitmap1);

                float[][] output_class_value = new float[1][1];
                interpreter2.run(byteBuffer1, output_class_value);

                Log.d("signLanguageClass", "output_class_value: " + output_class_value[0][0]);
                sign_val = get_alphabets(output_class_value[0][0]);
                temp2[count]=sign_val;
                count++;

                if(count==3){
                    if(temp2[0]==temp2[1] && temp2[2]==temp2[1]){
                        get_String(sign_val);
                    }
                    count=0;
                }
                Imgproc.putText(rotated_mat_image, "", new Point(x1 + 10, y1 + 40), 2, 1.5, new Scalar(255, 255, 255, 255), 2);
                Imgproc.rectangle(rotated_mat_image, new Point(x1, y1), new Point(x2, y2), new Scalar(0, 255, 0, 255), 2);
            }
        }
        Mat b = rotated_mat_image.t();
        Core.flip(b, mat_image, 0);
        b.release();
        return mat_image;
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
                        finalSignText.setText(s);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(context, "Fail to translate" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
//                Toast.makeText(context, "Fail to download language, connect to internet" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String get_String(String sign_val){

        if (sign_val == "change Mode") {

            selectedOption = 2;
            currentSelection.setText("Mode");
            sign_val="";


        }else if (sign_val == "space") {
            englishText = englishText + " ";
            sign_val="";
            selectedOption=previousOption;
            currentSelection.setText(previousType);

        }
        else if (sign_val == "Alphabets") {
            selectedOption=1;
            previousOption=selectedOption;
            currentSelection.setText("Alphabets");
            previousType="Alphabets";
            sign_val="";

        }else if (sign_val == "Words") {
            selectedOption=0;
            currentSelection.setText("Words");
            previousOption=selectedOption;
            previousType="Words";
            sign_val="";
        }
        else if (sign_val == "repeat") {
            temp = "";
            sign_val = "";
            selectedOption=previousOption;
            currentSelection.setText(previousType);

        } else if (sign_val == "EndOF") {
            if (currentLanguage.equals("Urdu")) {
                translateText(FirebaseTranslateLanguage.UR, FirebaseTranslateLanguage.EN, "Waiting for New Sign");
            } else {
                translateText(FirebaseTranslateLanguage.EN, FirebaseTranslateLanguage.EN, "Waiting for New Sign");
            }
            temp = "";
            sign_val = "";
            englishText = "";
            selectedOption=previousOption;
            currentSelection.setText(previousOption);
        }else if(sign_val=="Numbers"){
            selectedOption=4;
            previousOption=selectedOption;
            sign_val = "";
            currentSelection.setText("Numbers");

        }else if(sign_val=="XtoZ"){
            selectedOption=3;
            currentSelection.setText("XtoZ");
            sign_val="";
        }else if(sign_val=="goBack"){
            sign_val = "";
            selectedOption=1;
            currentSelection.setText("Alphabets");
        }
        else// Handling String
        {
            //Checking weather it is the start of String "englishText"
            if (sign_val != "" && englishText == "") {
                englishText = englishText + " " + sign_val;
                temp = sign_val;
                //Set Text View
                if (currentLanguage.equals("Urdu")) {
                    translateText(FirebaseTranslateLanguage.UR, FirebaseTranslateLanguage.EN, englishText);
                } else {
                    translateText(FirebaseTranslateLanguage.EN, FirebaseTranslateLanguage.EN, englishText);
                }
            }
            //Checking that englishText is Not empty and there is No duplication in sign
            else if (sign_val != "" && englishText != "" && sign_val != temp) {
                //Check weather the coming sign is Letter or word
                if (sign_val.length() == 1) // If sign is Letter
                {
                    // check whether the previous sign is also Letter
                    if (temp.length() == 1 || temp.length() == 0) {
                        englishText = englishText + sign_val;
                        temp = sign_val;
                    } else {
                        englishText = englishText + " " + sign_val;
                        temp = sign_val;

                    }
                } else if (sign_val.length() > 1) {
                    englishText = englishText + " " + sign_val;
                    temp = sign_val;
                }

                //Set Text View
                if (currentLanguage.equals("Urdu")) {
                    translateText(FirebaseTranslateLanguage.UR, FirebaseTranslateLanguage.EN, englishText);
                } else {
                    translateText(FirebaseTranslateLanguage.EN, FirebaseTranslateLanguage.EN, englishText);
                }
            } else if (sign_val == "" || sign_val == temp) {
            }
        }
        return sign_val;
    }

    private String get_alphabets(float sign_v) {
        String val = "";
        switch (selectedOption) {
            case 0:
                //0 means that words are selected
                if(sign_v >= -0.05 && sign_v < 0.15){
                    val = "Hello";
                }else if(sign_v >= 0.85 && sign_v < 1.05){
                    val = "my";
                }else if(sign_v >= 1.85 && sign_v < 2.05){
                    val = "name";
                }else if(sign_v >= 2.85 && sign_v < 3.05){
                    val = "is";
                }else if(sign_v >= 3.85 && sign_v < 4.05){
                    val = "what";
                }else if(sign_v >= 4.85 && sign_v < 5.05){
                    val = "your";
                }else if(sign_v >= 5.85 && sign_v < 6.05){
                    val = "when";
                }else if(sign_v >= 6.85 && sign_v < 7.05){
                    val = "where";
                }else if(sign_v >= 7.85 && sign_v < 8.05){
                    val = "this";
                }else if(sign_v >= 8.25 && sign_v < 9.05){
                    val = "that";
                }else if(sign_v >= 9.85 && sign_v < 10.05){
                    val = "need";
                }else if(sign_v >= 10.85 && sign_v < 11.05){
                    val = "help";
                }else if(sign_v >= 11.85 && sign_v < 12.05){
                    val = "want";
                }else if(sign_v >= 12.85 && sign_v < 13.05){
                    val = "how";
                }else if(sign_v >= 13.85 && sign_v < 14.05){
                    val = "are";
                }else if(sign_v >= 14.85 && sign_v < 15.05){
                    val = "you";
                }else if(sign_v >= 15.85 && sign_v < 16.05){
                    val = "happy";
                }else if(sign_v >= 16.85 && sign_v < 17.15){
                    val = "sad";
                }else if(sign_v >= 17.85 && sign_v < 18.05){
                    val = "he";
                }else if(sign_v >= 18.85 && sign_v < 19.05){
                    val = "she";
                }else if(sign_v >= 19.85 && sign_v < 20.05){
                    val = "love";
                }else if(sign_v >= 20.85 && sign_v < 21.05){
                    val = "good";
                }else if(sign_v >= 21.85 && sign_v < 22.05){
                    val = "bye";
                }else if(sign_v >= 22.55 && sign_v < 23.05){
                    val = "change Mode";
                }else {
                    val = "";
                }
                return val;
            case 1:
                //1 means that alphabets are selected
                if(sign_v >= -0.05 && sign_v < 0.15){
                    val = "A";
                }else if(sign_v >= 0.85 && sign_v < 1.05){
                    val = "B";
                }else if(sign_v >= 1.85 && sign_v < 2.05){
                    val = "C";
                }else if(sign_v >= 2.85 && sign_v < 3.05){
                    val = "D";
                }else if(sign_v >= 3.85 && sign_v < 4.05){
                    val = "E";
                }else if(sign_v >= 4.85 && sign_v < 5.05){
                    val = "F";
                }else if(sign_v >= 5.85 && sign_v < 6.05){
                    val = "G";
                }else if(sign_v >= 6.85 && sign_v < 7.05){
                    val = "H";
                }else if(sign_v >= 7.85 && sign_v < 8.05){
                    val = "I";
                }else if(sign_v >= 8.25 && sign_v < 9.05){
                    val = "J";
                }else if(sign_v >= 9.85 && sign_v < 10.05){
                    val = "K";
                }else if(sign_v >= 10.85 && sign_v < 11.05){
                    val = "L";
                }else if(sign_v >= 11.85 && sign_v < 12.05){
                    val = "M";
                }else if(sign_v >= 12.85 && sign_v < 13.05){
                    val = "N";
                }else if(sign_v >= 13.85 && sign_v < 14.05){
                    val = "O";
                }else if(sign_v >= 14.85 && sign_v < 15.05){
                    val = "P";
                }else if(sign_v >= 15.85 && sign_v < 16.05){
                    val = "Q";
                }else if(sign_v >= 16.85 && sign_v < 17.15){
                    val = "R";
                }else if(sign_v >= 17.85 && sign_v < 18.05){
                    val = "S";
                }else if(sign_v >= 18.85 && sign_v < 19.05){
                    val = "T";
                }else if(sign_v >= 19.85 && sign_v < 20.05){
                    val = "U";
                }else if(sign_v >= 20.85 && sign_v < 21.05){
                    val = "V";
                }else if(sign_v >= 21.85 && sign_v < 22.05){
                    val = "W";
                }else if(sign_v >= 22.85 && sign_v < 23.05){
                    val = "change Mode";
                }else {
                    val = "";
                }
                return val;
            case 2:
                //2 means that the modes are selected
                if(sign_v >= -0.05 && sign_v < 0.15){
                    val = "Alphabets";
                }else if(sign_v >= 0.85 && sign_v < 1.05){
                    val = "Words";
                }else if(sign_v >= 1.85 && sign_v < 2.05){
                    val = "EndOF";
                }else if(sign_v >= 10.85 && sign_v < 11.05){
                    val = "Numbers";
                }else if(sign_v >= 3.85 && sign_v < 4.05){
                    val = "XtoZ";
                }else if(sign_v >= 4.85 && sign_v < 5.05){
                    val = "repeat";
                }else if(sign_v >= 23.85 && sign_v < 24.05){
                    val = "space";
                }
                else{val="";}
                return val;
            case 3:
                //3 means that letter X,Y,Z are selected to show to the signLanguageOutput
                if(sign_v >= -0.05 && sign_v < 0.15){
                    val = "X";
                }else if(sign_v >= 0.85 && sign_v < 1.05){
                    val = "Y";
                }else if(sign_v >= 1.85 && sign_v < 2.05){
                    val = "Z";
                }else if(sign_v >= 22.85 && sign_v < 23.05){
                    val = "goBack";
                }
                else{val="";}
                return val;
            case 4:
                //4 means that digits are selected
                if(sign_v >= -0.05 & sign_v < 0.15){
                    val = "0";
                }else if(sign_v >= 0.85 & sign_v < 1.05){
                    val = "1";
                }else if(sign_v >= 1.85 & sign_v < 2.05){
                    val = "2";
                }else if(sign_v >= 2.85 & sign_v < 3.05){
                    val = "3";
                }else if(sign_v >= 3.85 & sign_v < 4.05){
                    val = "4";
                }else if(sign_v >= 4.85 & sign_v < 5.05){
                    val = "5";
                }else if(sign_v >= 5.85 & sign_v < 6.05){
                    val = "6";
                }else if(sign_v >= 6.85 & sign_v < 7.05){
                    val = "7";
                }else if(sign_v >= 7.85 & sign_v < 8.05){
                    val = "8";
                }else if(sign_v >= 8.25 & sign_v < 9.05){
                    val = "9";
                }
                else if(sign_v >= 22.85 && sign_v < 23.05){
                    val = "change Mode";
                }else{val="";}
                return val;
        }
        return val;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer;
        int quant = 1;
        int size_images = INPUT_SIZE;
        if (quant == 0) {
            byteBuffer = ByteBuffer.allocateDirect(1 * size_images * size_images * 3);
        } else {
            byteBuffer = ByteBuffer.allocateDirect(4 * 1 * size_images * size_images * 3);
        }
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[size_images * size_images];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < size_images; ++i) {
            for (int j = 0; j < size_images; ++j) {
                final int val = intValues[pixel++];
                if (quant == 0) {
                    byteBuffer.put((byte) ((val >> 16) & 0xFF));
                    byteBuffer.put((byte) ((val >> 8) & 0xFF));
                    byteBuffer.put((byte) (val & 0xFF));
                } else {
                    byteBuffer.putFloat((((val >> 16) & 0xFF)) / 255.0f);
                    byteBuffer.putFloat((((val >> 8) & 0xFF)) / 255.0f);
                    byteBuffer.putFloat((((val) & 0xFF)) / 255.0f);
                }
            }
        }
        return byteBuffer;
    }
    private ByteBuffer convertBitmapToByteBuffer1(Bitmap bitmap) {
        ByteBuffer byteBuffer;
        int quant = 1;
        int size_images = Classification_Input_size;
        if (quant == 0) {
            byteBuffer = ByteBuffer.allocateDirect(1 * size_images * size_images * 3);
        } else {
            byteBuffer = ByteBuffer.allocateDirect(4 * 1 * size_images * size_images * 3);
        }
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[size_images * size_images];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < size_images; ++i) {
            for (int j = 0; j < size_images; ++j) {
                final int val = intValues[pixel++];
                if (quant == 0) {
                    byteBuffer.put((byte) ((val >> 16) & 0xFF));
                    byteBuffer.put((byte) ((val >> 8) & 0xFF));
                    byteBuffer.put((byte) (val & 0xFF));
                } else {
                    byteBuffer.putFloat((((val >> 16) & 0xFF)));
                    byteBuffer.putFloat((((val >> 8) & 0xFF)));
                    byteBuffer.putFloat((((val) & 0xFF)));
                }
            }
        }
        return byteBuffer;
    }

    public void reset() {
        englishText = "";
        temp = "";
        finalSignText.setText("");
        if (currentLanguage.equals("English")) {
            finalSignText.setText(context.getString(R.string.SignTranslationTextView));
        } else if (currentLanguage.equals("Urdu")) {
            finalSignText.setText(context.getString(R.string.SignTranslationTVUr));
        }
    }
}
