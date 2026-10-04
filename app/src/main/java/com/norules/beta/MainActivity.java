package com.norules.beta;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private static final int REQ_CAMERA_PERMISSION = 9101;
    private static final int REQ_SELFIE = 9102;
    private GameView gameView;
    private Activity.ScreenCaptureCallback screenCaptureCallback;
    private MediaPlayer bgm;
    private boolean musicWanted = true;
    private boolean audioPuzzle = false;
    private boolean pendingSelfieAfterPermission = false;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        gameView = new GameView(this);
        setContentView(gameView);
        prepareOptionalBgm();

        if (Build.VERSION.SDK_INT >= 34) {
            screenCaptureCallback = new Activity.ScreenCaptureCallback() {
                @Override public void onScreenCaptured() {
                    if (gameView != null) gameView.onScreenshotDetected();
                }
            };
        }
    }

    // Drop app/src/main/res/raw/bgm_main.ogg into the project and it will be picked up automatically.
    private void prepareOptionalBgm() {
        int id = getResources().getIdentifier("bgm_main", "raw", getPackageName());
        if (id == 0) return;
        try {
            bgm = MediaPlayer.create(this, id);
            if (bgm != null) {
                bgm.setLooping(true);
                bgm.setVolume(0.32f, 0.32f);
            }
        } catch (Exception ignored) { bgm = null; }
    }

    public void syncMusicForScreen(int screen) {
        audioPuzzle = false;
        if (bgm == null) return;
        if (audioPuzzle) {
            if (bgm.isPlaying()) bgm.pause();
        } else if (musicWanted && !isFinishing()) {
            try { bgm.start(); } catch (Exception ignored) {}
        }
    }


    public void requestStartupCameraPermission() {
        pendingSelfieAfterPermission = false;
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
        }
    }

    public void startSelfieCapture() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingSelfieAfterPermission = true;
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
            return;
        }
        launchSelfieCamera();
    }

    private void launchSelfieCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
        intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
        try { startActivityForResult(intent, REQ_SELFIE); }
        catch (Exception e) { if (gameView != null) gameView.onSelfieCancelled(); }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERMISSION) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted && pendingSelfieAfterPermission) launchSelfieCamera();
            else if (!granted && pendingSelfieAfterPermission && gameView != null) gameView.onSelfieCancelled();
            pendingSelfieAfterPermission = false;
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_SELFIE) return;
        if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
            Object value = data.getExtras().get("data");
            if (value instanceof Bitmap && gameView != null) {
                gameView.onSelfieCaptured((Bitmap)value);
                return;
            }
        }
        if (gameView != null) gameView.onSelfieCancelled();
    }

    @Override protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= 34 && screenCaptureCallback != null) registerScreenCaptureCallback(getMainExecutor(), screenCaptureCallback);
    }

    @Override protected void onStop() {
        if (Build.VERSION.SDK_INT >= 34 && screenCaptureCallback != null) unregisterScreenCaptureCallback(screenCaptureCallback);
        super.onStop();
    }

    @Override protected void onResume() {
        super.onResume();
        if (gameView != null) gameView.onResumeGame();
        if (bgm != null && musicWanted && !audioPuzzle) { try { bgm.start(); } catch (Exception ignored) {} }
    }

    @Override protected void onPause() {
        if (gameView != null) gameView.onPauseGame();
        if (bgm != null && bgm.isPlaying()) bgm.pause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (bgm != null) { bgm.release(); bgm = null; }
        super.onDestroy();
    }
}
