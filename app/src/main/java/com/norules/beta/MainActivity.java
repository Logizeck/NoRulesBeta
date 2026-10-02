package com.norules.beta;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private GameView gameView;
    private Activity.ScreenCaptureCallback screenCaptureCallback;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        gameView = new GameView(this);
        setContentView(gameView);

        if (Build.VERSION.SDK_INT >= 34) {
            screenCaptureCallback = new Activity.ScreenCaptureCallback() {
                @Override public void onScreenCaptured() {
                    if (gameView != null) gameView.onScreenshotDetected();
                }
            };
        }
    }

    @Override protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= 34 && screenCaptureCallback != null) {
            registerScreenCaptureCallback(getMainExecutor(), screenCaptureCallback);
        }
    }

    @Override protected void onStop() {
        if (Build.VERSION.SDK_INT >= 34 && screenCaptureCallback != null) {
            unregisterScreenCaptureCallback(screenCaptureCallback);
        }
        super.onStop();
    }

    @Override protected void onResume() {
        super.onResume();
        if (gameView != null) gameView.onResumeGame();
    }

    @Override protected void onPause() {
        if (gameView != null) gameView.onPauseGame();
        super.onPause();
    }
}
