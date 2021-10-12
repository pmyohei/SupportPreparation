package com.example.supportpreparation;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

public class _SplashActivity extends AppCompatActivity {

    final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //起動までの時間
        final int START_ANIMATION_TIME = 200;
        final int SPLASH_TIME = 2300;

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
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void run() {
            //アイコンアニメーション
            //※avdファイルそのものでアニメーションの遅延を設定しても反映されないため、ここで遅延させる
            ImageView iv_splash = (ImageView)findViewById(R.id.iv_splash);
            iv_splash.setBackgroundResource(R.drawable.avd_splash);
            AnimatedVectorDrawable rocketAnimation = (AnimatedVectorDrawable) iv_splash.getBackground();
            rocketAnimation.start();

            rocketAnimation.registerAnimationCallback( new Animatable2.AnimationCallback() {
                @Override
                public void onAnimationEnd(Drawable drawable) {
                    super.onAnimationEnd(drawable);



                }
            });



        }
    };

    /*
     * 時間経過後処理
     */
    private Runnable mSplashTask = new Runnable() {
        @Override
        public void run() {
            // メイン画面に遷移して、現在のActivityを終了
            Intent intent = new Intent(_SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    };

}