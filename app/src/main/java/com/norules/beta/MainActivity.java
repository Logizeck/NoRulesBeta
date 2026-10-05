package com.norules.beta;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private GameView game;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                5894 | 4096 | 2 | 4);
        game = new GameView(this);
        setContentView(game);
    }
    @Override protected void onResume(){ super.onResume(); if(game!=null) game.onResumeGame(); }
    @Override protected void onPause(){ if(game!=null) game.onPauseGame(); super.onPause(); }
}
