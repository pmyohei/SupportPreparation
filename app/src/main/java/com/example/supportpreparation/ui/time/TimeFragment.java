package com.example.supportpreparation.ui.time;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.supportpreparation.MainActivity;
import com.example.supportpreparation.R;
import com.example.supportpreparation.TaskTable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimeFragment extends Fragment {

    private MainActivity            mParentActivity;            //
    private Fragment                mFragment;                  //本フラグメント
    private Context                 mContext;                   //コンテキスト（親アクティビティ）
    private View                    mRootLayout;                //本フラグメントに設定しているレイアウト
    private List<TaskTable>         mStackTask;                 //積み上げ「やること」
    private int                     mTaskRefIdx;                //積み上げ「やること」の参照中インデックス
    private FloatingActionButton    mFab;                       //フローティングボタン

    private TextView                mtv_finalTime;              //
    private TextView                mtv_progressTime;           //
    private TextView                mtv_progressTask;           //
    private TextView                mtv_nextTask;               //

                                                                //カウントダウンフォーマット
    private SimpleDateFormat sdfFinalTime =
            new SimpleDateFormat("HH:mm", Locale.JAPAN);
    private SimpleDateFormat sdfNextTime =
            new SimpleDateFormat("HH:mm:ss", Locale.JAPAN);

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //自身のフラグメントを保持
        mFragment = getParentFragmentManager().getFragments().get(0);
        //設定レイアウト
        mRootLayout = inflater.inflate(R.layout.fragment_time, container, false);
        //親アクティビティのコンテキスト
        mContext = mRootLayout.getContext();
        //親アクティビティ
        mParentActivity = (MainActivity) getActivity();

        //設定された情報を取得
        List<TaskTable> stackTask = mParentActivity.getStackTaskData();
        String limitDate = mParentActivity.getLimitDate();
        String limitTime = mParentActivity.getLimitTime();

        //「やること」の参照インデックス
        mTaskRefIdx = -1;

        //-- 表示ビューの取得
        mtv_finalTime    = mRootLayout.findViewById(R.id.tv_finalTime);
        mtv_progressTime = mRootLayout.findViewById(R.id.tv_progressTime);;
        mtv_progressTask = mRootLayout.findViewById(R.id.tv_plainProgressTask);;
        mtv_nextTask     = mRootLayout.findViewById(R.id.tv_nextTask);;

        //「やること時間」を合計
        int totalMinute = 0;
        for( TaskTable task: stackTask ){
            //時間を累算
            totalMinute += task.getTaskTime();
        }

        //現在時刻
        Date nowTime = new Date();
        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.setTime(nowTime);

        //リミットをDate型に変換
        Date limitDateTime;
        try {
            //期限日と期限時間を連結
            String limitStr = limitDate + " " + limitTime;
            //Date型へ変換
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            limitDateTime = sdf.parse(limitStr);

        } catch (ParseException e) {
            e.printStackTrace();

            //エラーメッセージを表示
            Toast toast = new Toast(mContext);
            toast.setText("エラーが発生しました。再度設定してください");
            toast.show();

            //例外発生時は、終了
            return mRootLayout;
        }

        //最初の開始時間を計算
        Calendar limitCalendar = Calendar.getInstance();
        limitCalendar.setTime(limitDateTime);
        limitCalendar.add(Calendar.MINUTE, -totalMinute);

        //最初の開始時間をDate型として取得
        Date beginTime = limitCalendar.getTime();

        Log.i("test", "nowTime=" + nowTime);
        Log.i("test", "initDate=" + beginTime);
        Log.i("test", "limitDateTime=" + limitDateTime);

        //現在時刻から、先頭の「やること」開始時間の差を取得
        long nextReminingSec = beginTime.getTime() - nowTime.getTime();
        long lastReminingSec = limitDateTime.getTime() - nowTime.getTime();
        Log.i("test", "diff=" + sdfFinalTime.format(nextReminingSec));

        //すでに開始時刻になっている場合
        if( nextReminingSec <= 0 ){

        }

        //カウントダウンの設定
        long nextToCount  = nextReminingSec * 1000;   //次の時間までの時間(msec)
        long finalToCount = lastReminingSec * 1000;   //最終時刻までの時間(msec)

        //カウントダウンインスタンスの生成
        NextCountDown countDownProgress = new NextCountDown(mtv_progressTime, nextToCount, 1000);
        FinalCountDown countDownFinal   = new FinalCountDown(mtv_finalTime, finalToCount, 60000);
        //開始
        countDownFinal.start();
        countDownProgress.start();

        return mRootLayout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /*
     * タイマーをセットする
     */
    public boolean setTimer(){

        return true;
    }


    /*
     * カウントダウン(次の時刻)
     */
    class NextCountDown extends CountDownTimer {

        TextView tv_time;

        public NextCountDown(View view, long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);

            tv_time = (TextView)view;
        }

        @Override
        public void onFinish() {
            // 完了
            tv_time.setText(sdfFinalTime.format(0));

            //-- 次のやることのタイマーを設定

            //リファレンスのやることへ進める
            mTaskRefIdx++;
            if( mTaskRefIdx >= mStackTask.size() ){
                //すべて計算したら、終了
                return;
            }

            //次に設定する「やること」
            String taskName = mStackTask.get(mTaskRefIdx).getTaskName();
            int    taskTime = mStackTask.get(mTaskRefIdx).getTaskTime();

            //時間を再設定
            setTimer();
        }

        // インターバルで呼ばれる
        @Override
        public void onTick(long millisUntilFinished) {
            // 残り時間を分、秒、ミリ秒に分割
            //long mm = millisUntilFinished / 1000 / 60;
            //long ss = millisUntilFinished / 1000 % 60;
            //long ms = millisUntilFinished - ss * 1000 - mm * 1000 * 60;
            //timerText.setText(String.format("%1$02d:%2$02d.%3$03d", mm, ss, ms));

            tv_time.setText(sdfNextTime.format(millisUntilFinished));
        }
    }

    /*
     * カウントダウン(最終時刻)
     */
    class FinalCountDown extends CountDownTimer {

        TextView tv_time;

        public FinalCountDown(View view, long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);

            tv_time = (TextView)view;
        }

        @Override
        public void onFinish() {
            // 完了
            tv_time.setText(sdfFinalTime.format(0));
        }

        // インターバルで呼ばれる
        @Override
        public void onTick(long millisUntilFinished) {
            // 残り時間を分、秒、ミリ秒に分割
            tv_time.setText(sdfFinalTime.format(millisUntilFinished));
        }
    }

}