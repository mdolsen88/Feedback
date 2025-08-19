package com.example.mdol.feedback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;

import static android.content.Context.AUDIO_SERVICE;

public class BackgroundCamera implements SurfaceHolder.Callback{

    private WindowManager windowManager;
    private SurfaceView surfaceView;
    private Camera camera        = null;
    private MediaRecorder mediaRecorder = null;
    private Activity activity;
    private File file;
    private AudioManager audioManager = null;

    public BackgroundCamera(Activity activity, File file)
    {
        this.file = file;
        this.activity = activity;
        windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        surfaceView = new SurfaceView(activity);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                1, 1,
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_TOAST  :
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.START | Gravity.TOP;
        windowManager.addView(surfaceView, layoutParams);
        surfaceView.getHolder().addCallback(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, 0);
            }
        }
        audioManager = (AudioManager) activity.getSystemService(AUDIO_SERVICE);
    }

    private boolean isFrontFacing = true;
    private Camera openFrontFacingCameraGingerbread()
    {
        if (camera != null)
        {
            camera.stopPreview();
            camera.release();
        }
        Camera cam = null;
        if (isFrontFacing && checkFrontCamera(activity))
        {
            Log.d("MDOL_DEBUG","Started frontfaceing");
            int cameraCount = 0;
            cam = null;
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            cameraCount = Camera.getNumberOfCameras();
            for (int camIdx = 0; camIdx < cameraCount; camIdx++)
            {
                Camera.getCameraInfo(camIdx, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                {
                    try
                    {
                        cam = Camera.open(camIdx);
                    }
                    catch (RuntimeException e)
                    {
                        Log.e("MDOL_DEBUG","Camera failed to open: " + e.getLocalizedMessage());
                    }
                }
            }
        }
        else
        {
            Log.d("MDOL_DEBUG","Started default");
            cam = Camera.open();
        }
        return cam;
    }

    int[] volumes = new int[4];
    // Method called right after Surface created (initializing and starting MediaRecorder)
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder)
    {
        camera = openFrontFacingCameraGingerbread();

        mediaRecorder = new MediaRecorder();
        camera.unlock();

        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        mediaRecorder.setCamera(camera);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        //mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
        mediaRecorder.setOutputFile(file.getAbsolutePath());

        try {
            mediaRecorder.prepare();
            if(audioManager != null)
            {
                volumes[0] = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                volumes[1] = audioManager.getStreamVolume(AudioManager.STREAM_DTMF);
                volumes[2] = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                volumes[3] = audioManager.getStreamVolume(AudioManager.STREAM_RING);

                audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC,true);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_DTMF, 0, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
            }
            mediaRecorder.start();
            try {
                Thread.sleep(250);
                audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC,false);
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volumes[0], 0);
                audioManager.setStreamVolume(AudioManager.STREAM_DTMF, volumes[1], 0);
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volumes[2], 0);
                audioManager.setStreamVolume(AudioManager.STREAM_RING, volumes[3], 0);
            }
            catch (InterruptedException ex)
            {
                Log.d("MDOL_DEBUG","Mute Sleep Exception");
            }
            Log.d("MDOL_DEBUG", "Started camera successfully");
        }
        catch(IOException ex)
        {
            Log.e("MDOL_DEBUG", ex.getMessage());
        }
    }

    // Stop recording and remove SurfaceView
    void stop()
    {
        if(audioManager != null)
        {
            volumes[0] = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            volumes[1] = audioManager.getStreamVolume(AudioManager.STREAM_DTMF);
            volumes[2] = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            volumes[3] = audioManager.getStreamVolume(AudioManager.STREAM_RING);

            audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC,true);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_DTMF, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
        }
        mediaRecorder.stop();
        mediaRecorder.reset();
        mediaRecorder.release();
        camera.lock();
        camera.release();
        windowManager.removeView(surfaceView);
        try {
            Thread.sleep(250);
            audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC,false);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volumes[0], 0);
            audioManager.setStreamVolume(AudioManager.STREAM_DTMF, volumes[1], 0);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volumes[2], 0);
            audioManager.setStreamVolume(AudioManager.STREAM_RING, volumes[3], 0);
        }
        catch (InterruptedException ex)
        {
            Log.d("MDOL_DEBUG","Mute Sleep Exception");
        }
        Log.d("MDOL_DEBUG","Stopped camera successfully");
    }

    private boolean checkFrontCamera(Context context)
    {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height)
    {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder)
    {
    }

}
