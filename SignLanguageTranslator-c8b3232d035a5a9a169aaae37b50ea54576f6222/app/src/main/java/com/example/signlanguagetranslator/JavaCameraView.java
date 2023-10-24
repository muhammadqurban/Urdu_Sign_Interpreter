package com.example.signlanguagetranslator;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;

public class JavaCameraView extends Fragment implements View.OnClickListener, CameraBridgeViewBase.CvCameraViewListener2 {
    //fragment for Java camera view which will open the camera to scan and the application will start to scan

    private ImageButton shiftCamera;
    private String TAG = "JAVA FRAGMENT";
    private CameraBridgeViewBase mOpenCvCameraView;
    private signLanguageClass mSignLanguageClass;
    private Mat mRgba, mGray;
    private int cameraPosition = -1; //back camera position
    private TextView signT;
    private Boolean isFront = false;
    private TextView currentWorAlp;
    private String dualLanguage = "";
    private ImageButton signCross;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getActivity()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface
                        .SUCCESS: {
                    Log.i(TAG, "OpenCv Is loaded");
                    mOpenCvCameraView.enableView();
                }
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_java_camera_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shiftCamera = getActivity().findViewById(R.id.switchCamera);
        signT = getActivity().findViewById(R.id.signTranslation);

        currentWorAlp = getActivity().findViewById(R.id.changer);
        mOpenCvCameraView = (CameraBridgeViewBase) getActivity().findViewById(R.id.frame_Surface);
        CameraActivity cA = (CameraActivity) getActivity();
        dualLanguage = cA.selectedLanguage;
        signCross = getActivity().findViewById(R.id.signCross);

        shiftCamera.setOnClickListener(this);
        signCross.setOnClickListener(this);
        turnCameraONScanner(cameraPosition);
    }

    @Override
    public void onClick(View view) {

        int id = view.getId();

        if (id == R.id.switchCamera) {
            //changing camera position here
            if (cameraPosition == -1) {
                //turn on front camera
                cameraPosition = 1;
                turnCameraONScanner(cameraPosition);
            } else if (cameraPosition == 1) {
                //turn on back camera
                cameraPosition = -1;
                turnCameraONScanner(cameraPosition);
            }
        } else if (id == R.id.signCross) {
            mSignLanguageClass.reset();
        }
    }

    private void turnCameraONScanner(int cameraPosition) {

        if (cameraPosition == 1) {
            isFront = true;
        } else if (cameraPosition == -1) {
            isFront = false;
        }

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(cameraPosition); //you can shift to front or rear camera -1 back, 1 front
        mOpenCvCameraView.enableView();
        mOpenCvCameraView.setCvCameraViewListener(this);
        try {
            mSignLanguageClass = new signLanguageClass(getActivity(), dualLanguage, isFront, signT, currentWorAlp ,getActivity().getAssets(),
                    "hand_model.tflite", "custom_label.txt", 300, "Sign_language_model.tflite", 96);
            Log.d("MainActivity", "Model is successfully loaded");
        } catch (IOException e) {
            Log.d("MainActivity", "Getting some error");
            e.printStackTrace();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        Mat out = new Mat();
        out = mSignLanguageClass.recognizeImage(mRgba);

        return out;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            //if load success
            Log.d(TAG, "Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            //if not loaded
            Log.d(TAG, "Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, getActivity(), mLoaderCallback);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }
}