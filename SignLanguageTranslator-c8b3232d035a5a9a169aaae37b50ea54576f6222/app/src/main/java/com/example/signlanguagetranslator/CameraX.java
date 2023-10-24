package com.example.signlanguagetranslator;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class CameraX extends Fragment implements View.OnClickListener {
    //fragment for CameraX which will open the camera which will for simple showing that the camera is working
    //but when the scanner button will pressed the fragment of java camera view will call and the scanning will start

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private ImageCapture image;
    private int cameraPosition = 1;
    private ImageButton shiftCamera;
    private CameraManager cameraManager;
    private String getCameraId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera_x, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shiftCamera = getActivity().findViewById(R.id.switchCamera);

        previewView = view.findViewById(R.id.cameraView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(getActivity());

        startCamera();

        boolean isFlashAvailable = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!isFlashAvailable) {
            Toast.makeText(getActivity(), "Sorry! You don't have any Flash light", Toast.LENGTH_SHORT).show();
        } else {
            cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
            try {
                getCameraId = cameraManager.getCameraIdList()[0];
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        shiftCamera.setOnClickListener(this);

    }

    private void startCamera() {

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(getActivity()));
    }

    private void bindImageAnalysis(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector cameraSelector;

        if (cameraPosition == 1) {
            cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            image = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, image);
            Camera cam = cameraProvider.bindToLifecycle(this, cameraSelector, preview, image);
        } else if (cameraPosition == 0) {
            cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            image = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, image);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.switchCamera) {
            if (cameraPosition == 1) {
                cameraPosition = 0;
                startCamera();

            } else if (cameraPosition == 0) {
                cameraPosition = 1;
                startCamera();
            }
        }
    }
}