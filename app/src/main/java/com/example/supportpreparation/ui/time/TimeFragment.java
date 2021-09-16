package com.example.supportpreparation.ui.time;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.supportpreparation.MainActivity;
import com.example.supportpreparation.R;
import com.example.supportpreparation.TaskArrayList;
import com.example.supportpreparation.TaskTable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class TimeFragment extends Fragment {

    //-- 定数
    private final int REF_WAITING = -1;                         //積み上げ「やること」進行待ち状態
    private final int CONV_SEC_TO_MSEC = 1000;                  //単位変換：sec → msec
    private final int CONV_MIN_TO_MSEC = 60000;                 //単位変換：min → msec
    private final int INTERVAL_PROGRESS = 1000;                 //進行中やることのインターバル（1sec）
    private final int INTERVAL_FINAL = 60000;                //最終時刻までのインターバル（1min）

    //-- フィールド
    private MainActivity mParentActivity;            //親アクティビティ
    private Fragment mFragment;                  //本フラグメント
    private Context mContext;                   //コンテキスト（親アクティビティ）
    private View mRootLayout;                //本フラグメントに設定しているレイアウト
    private TaskArrayList<TaskTable> mStackTask;                 //積み上げ「やること」
    private int mTaskRefIdx;                //積み上げ「やること」の参照中インデックス
    private TextView mtv_finalTime;              //
    private TextView mtv_progressTime;           //
    private TextView mtv_progressTask;           //
    private TextView mtv_nextTask;               //

    //カウントダウンフォーマット
    final private SimpleDateFormat sdfFinalTime =
            new SimpleDateFormat("HH:mm");
    final private SimpleDateFormat sdfProgressTime =
            new SimpleDateFormat("HH:mm:ss");


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
        mStackTask = mParentActivity.getStackTaskData();
        String limitDate = mParentActivity.getLimitDate();
        String limitTime = mParentActivity.getLimitTime();

        //「やること」の参照インデックス
        mTaskRefIdx = REF_WAITING;

        //-- 表示ビューの取得
        mtv_finalTime = mRootLayout.findViewById(R.id.tv_finalTime);
        mtv_progressTime = mRootLayout.findViewById(R.id.tv_progressTime);
        mtv_progressTask = mRootLayout.findViewById(R.id.tv_plainProgressTask);
        mtv_nextTask = mRootLayout.findViewById(R.id.tv_nextTask);

        //mtv_progressTask.setTextColor(R.color.orange);

        //タイムフォーマットのタイムゾーンをUTCに設定
        TimeZone tz = TimeZone.getTimeZone("UTC");
        sdfFinalTime.setTimeZone(tz);
        sdfProgressTime.setTimeZone(tz);

        //「やること時間」を合計
        //★TaskArrayList にメソッドを用意
        int totalMinute = 0;
        for (TaskTable task : mStackTask) {
            //時間を累算
            totalMinute += task.getTaskTime();
        }
        //★

        //-- 最終時刻をDate型に変換
        Date finalTime;
        try {
            //期限日と期限時間を連結
            String limitStr = limitDate + " " + limitTime;
            //Date型へ変換
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            finalTime = sdf.parse(limitStr);

        } catch (ParseException e) {
            e.printStackTrace();

            //エラーメッセージを表示
            Toast toast = new Toast(mContext);
            toast.setText("時間を設定してください");
            toast.show();

            //例外発生時は、終了
            return mRootLayout;
        }

        //-- 現在時刻
        Date nowTime = new Date();
        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.setTime(nowTime);

        //最終時刻が既に過ぎていた場合

        //★TaskArrayList にメソッドを用意
        if (nowTime.after(finalTime)) {
            //カウントダウンはせず、終了
            return mRootLayout;
        }
        //★

        //最初の開始時間
        Calendar beginCalendar = Calendar.getInstance();
        beginCalendar.setTime(finalTime);
        beginCalendar.add(Calendar.MINUTE, -totalMinute);

        //最初の開始時間をDate型として取得
        Date beginTime = beginCalendar.getTime();

        //現在時刻が既に割り込んでいる場合
        //（先頭やること開始時刻 ＜ 現在時刻）
        long progressToCount;
        if (nowTime.after(beginTime)) {

            //カウントダウン時間(ms)の算出
            long overmsec = nowTime.getTime() - beginTime.getTime();

            //積まれた「やること」参照Indexを調整する(過ぎた分を進める)
            adjustTaskRefIdx(overmsec);

            //タイマーに設定するカウントを取得
            progressToCount = getRemainCount(nowTime, finalTime);

        } else {
            //-- 割り込んでなければ、そのままタイマー時間を取得

            //カウント算出
            progressToCount = beginTime.getTime() - nowTime.getTime();
        }

        //進行中タイマーの設定
        setNextTimer(progressToCount);

        //「やること」表示処理（進行中／次）
        setDisplayTaskName();

        Log.i("test", "nowTime=" + nowTime);
        Log.i("test", "beginTime=" + beginTime);
        Log.i("test", "finalTime=" + finalTime);

        //現在時刻から、最終期限までの時間を算出
        long finalCount = finalTime.getTime() - nowTime.getTime();

        //カウントダウンインスタンスの生成
        FinalCountDown countDownFinal = new FinalCountDown(mtv_finalTime, finalCount, INTERVAL_FINAL);
        countDownFinal.start();

        return mRootLayout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /*
     * 積まれたやること参照Indexの調整
     */
    public void adjustTaskRefIdx(long overmsec) {

        //「やること時間」累計（msec）
        long total = 0;

        //進行中の「やること」まで進める
        int idx = 0;
        for (TaskTable task : mStackTask) {

            //やること時間をmsecに変換し、累計に加算
            long taskmsec = (long) task.getTaskTime() * CONV_MIN_TO_MSEC;
            total += taskmsec;

            //割り込んだ時間が、やること時間（累計）未満であれば
            if (overmsec < total) {
                //進行中のやること発見したため、終了
                break;
            }

            //Indexを次へ
            idx++;
        }

        //現在進行中のやることのIndexに更新
        mTaskRefIdx = idx;

        Log.i("test", "adjustTaskRefIdx=" + mTaskRefIdx);
    }

    /*
     * 進行中「やること」の残り時間を取得（カウンタ値の算出）
     */
    public long getRemainCount(Date now, Date finalLimit) {

        int remainMinute = 0;

        int size = mStackTask.size();
        for (int i = mTaskRefIdx; i < size; i++) {
            //進行中タスク以降の「やること時間」を累計
            remainMinute += mStackTask.get(i).getTaskTime();
        }

        //進行中やることの開始時刻を算出
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(finalLimit);
        calendar.add(Calendar.MINUTE, -remainMinute);
        Date progressStartTime = calendar.getTime();

        //現在時刻 - 進行中やることの開始時刻
        long diff = now.getTime() - progressStartTime.getTime();

        //進行中やることの時間
        long taskTime = (long) mStackTask.get(mTaskRefIdx).getTaskTime() * CONV_MIN_TO_MSEC;

        //残りのやること時間を返す
        return (taskTime - diff);
    }

    /*
     * 「やること」（進行中／次）の表示処理
     */
    public void setDisplayTaskName() {

        //設定文字列
        String progressTask;
        String nextTask;

        //固定文字列の取得
        String waitingStr = getString(R.string.waiting);
        String noneStr = getString(R.string.next_none);

        //カラーID
        int colorId = R.color.tx_time_not_reached;

        if (mTaskRefIdx == REF_WAITING) {
            //-- まだ、初めの「やること」の開始時間に至っていない場合

            //進行中の「やること」
            progressTask = waitingStr;
            //次の「やること」
            nextTask = mStackTask.get(0).getTaskName();

        } else if (mTaskRefIdx >= mStackTask.size()) {
            //-- 「やること」全て完了

            //進行中の「やること」
            progressTask = noneStr;
            //次の「やること」なし
            nextTask = noneStr;

        } else {
            //-- 「やること」突入

            //進行中の「やること」
            progressTask = mStackTask.get(mTaskRefIdx).getTaskName();

            //次の「やること」
            if ((mTaskRefIdx + 1) < mStackTask.size()) {
                //まだ次の「やること」あり
                nextTask = mStackTask.get(mTaskRefIdx + 1).getTaskName();
            } else {
                //次の「やること」なし
                nextTask = noneStr;
            }

            //テキストに設定する色を取得
            colorId = getColorId(mStackTask.get(mTaskRefIdx).getTaskTime());
        }

        //「やること」（進行中／次）の表示設定
        mtv_progressTask.setText(progressTask);
        mtv_nextTask.setText(nextTask);

        //テキストカラーの変更
        for (int i = 0; i < ((ViewGroup)mRootLayout).getChildCount(); i++) {
            //子ビューを取得
            View v = ((ViewGroup)mRootLayout).getChildAt(i);
            //テキストビューのみ対象
            if( v instanceof TextView ){
                TextView tv = (TextView)v;
                tv.setTextColor( getResources().getColor(colorId) );
            }
        }
    }

    /*
     * やること時間に対応する色を取得
     */
    public int getColorId(int taskTime){

        int id;

        if (taskTime <= 5) {
            id = R.color.bg_task_very_short;
        } else if (taskTime <= 10) {
            id = R.color.bg_task_short;
        } else if (taskTime <= 30) {
            id = R.color.bg_task_normal;
        } else if (taskTime <= 60) {
            id = R.color.bg_task_long;
        } else {
            id = R.color.bg_task_very_long;
        }

        return id;
    }

    /*
     * タイマーセット(直近のやることまでのタイマー)
     */
    public void setNextTimer(long count){

        Log.i("test", "setNextTimer=" + sdfProgressTime.format(count));

        //カウントダウンインスタンスを生成し、タイマー開始
        NextCountDown countDownProgress = new NextCountDown(mtv_progressTime, count, INTERVAL_PROGRESS);
        countDownProgress.start();

        Log.i("test", "setNextTimer started");
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
            //tv_time.setText(sdfProgressTime.format(0));

            //-- 次のやることのタイマーを設定

            //リファレンスのやることへ進める
            mTaskRefIdx++;

            //表示中の「やること」を更新
            setDisplayTaskName();
            if( mTaskRefIdx >= mStackTask.size() ){
                //すべて計算したら、終了
                return;
            }

            //タイマーを再設定
            int taskTime = mStackTask.get(mTaskRefIdx).getTaskTime();
            setNextTimer((long)taskTime * CONV_MIN_TO_MSEC);
        }

        // インターバルで呼ばれる
        @Override
        public void onTick(long millisUntilFinished) {
            // 残り時間を分、秒、ミリ秒に分割
            //long mm = millisUntilFinished / 1000 / 60;
            //long ss = millisUntilFinished / 1000 % 60;
            //long ms = millisUntilFinished - ss * 1000 - mm * 1000 * 60;
            //timerText.setText(String.format("%1$02d:%2$02d.%3$03d", mm, ss, ms));

            // 残り時間を表示
            tv_time.setText(sdfProgressTime.format(millisUntilFinished));
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
            // 残り時間を表示
            tv_time.setText(sdfFinalTime.format(millisUntilFinished));
        }
    }
}
