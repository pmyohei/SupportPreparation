package com.example.supportpreparation;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

public class SplashActivity extends AppCompatActivity {

    final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //起動までの時間
        final int START_ANIMATION_TIME = 200;
        final int SPLASH_TIME = 1400;

        //指定時間経過後、Runnableインスタンスを実行
        mHandler.postDelayed(mSplashAnimation, START_ANIMATION_TIME);
        mHandler.postDelayed(mSplashTask, SPLASH_TIME);
    }

    @Override
    protected void onStop() {
        super.onStop();

        //アプリ停止時には、MainActivityへは遷移させない
        mHandler.removeCallbacks(mSplashTask);
    }

    /*
     * アニメーション
     */
    private Runnable mSplashAnimation = new Runnable() {
        @Override
        public void run() {
            //アイコンアニメーション
            //※avdファイルそのものでアニメーションの遅延を設定しても反映されないため、ここで遅延させる
            ImageView iv_splash = (ImageView)findViewById(R.id.iv_splash);
            iv_splash.setBackgroundResource(R.drawable.avd_splash);
            AnimatedVectorDrawable rocketAnimation = (AnimatedVectorDrawable) iv_splash.getBackground();
            rocketAnimation.start();
        }
    };

    /*
     * 時間経過後処理
     */
    private Runnable mSplashTask = new Runnable() {
        @Override
        public void run() {
            // メイン画面に遷移して、現在のActivityを終了
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    };

}