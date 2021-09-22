package com.example.supportpreparation.ui.time;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.supportpreparation.MainActivity;
import com.example.supportpreparation.R;
import com.example.supportpreparation.ResourceManager;
import com.example.supportpreparation.TaskArrayList;
import com.example.supportpreparation.TaskTable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeFragment extends Fragment {

    //--定数
    private final int REF_WAITING = -1;                 //積み上げ「やること」進行待ち状態

    //--定数（単位変換）
    private final int CONV_SEC_TO_MSEC  = 1000;         //単位変換：sec → msec
    private final int CONV_MIN_TO_MSEC  = 60000;        //単位変換：min → msec
    private final int INTERVAL_PROGRESS = 1000;         //進行中やることのインターバル（1sec）
    private final int INTERVAL_FINAL    = 60000;        //最終時刻までのインターバル（1min）

    //--定数（グラフ高さ目安）
    private final int GRAGH_HEIGHT_1_MIN        = 20;   //
    private final int GRAGH_HEIGHT_30_MIN       = 100;  //
    private final int GRAGH_HEIGHT_60_MIN       = 160;  //
    private final int GRAGH_HEIGHT_120_MORE_MIN = 220;  //

    //--定数（1分辺りの高さ）
    private final int GRAGH_HEIGHT_1_30_PER_MIN   = ((GRAGH_HEIGHT_30_MIN - GRAGH_HEIGHT_1_MIN) / 29);
    private final int GRAGH_HEIGHT_30_60_PER_MIN  = ((GRAGH_HEIGHT_60_MIN - GRAGH_HEIGHT_30_MIN) / 30);
    private final int GRAGH_HEIGHT_60_120_PER_MIN = ((GRAGH_HEIGHT_120_MORE_MIN - GRAGH_HEIGHT_60_MIN) / 60);

    //--フィールド
    private MainActivity mParentActivity;           //親アクティビティ
    private Fragment mFragment;                     //本フラグメント
    private Context mContext;                       //コンテキスト（親アクティビティ）
    private View mRootLayout;                       //本フラグメントに設定しているレイアウト
    private TaskArrayList<TaskTable> mStackTask;    //積み上げ「やること」
    private int mTaskRefIdx;                        //積み上げ「やること」の参照中インデックス


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

        //ベース時間の指定
        boolean isLimit = mParentActivity.isLimit();

        //グラフの設定
        setupGragh();

        //「やること」の参照インデックス
        mTaskRefIdx = REF_WAITING;

        //やること未スタック
        if (mStackTask.size() == 0) {
            //カウントダウンなし
            return mRootLayout;
        }

        //指定時刻をDate型として取得
        Date dateBaseTime = getBaseTimeDate(limitDate, limitTime);
        if (dateBaseTime == null) {
            //カウントダウンなし
            return mRootLayout;
        }

        //最終時刻が既に過ぎていた場合
        Date dateNow = new Date();
        if (dateNow.after(dateBaseTime)) {
            //カウントダウンなし
            return mRootLayout;
        }

        //現在時刻から見て一番初めのやること、開始時間
        int firstAlarmIdx = mStackTask.getAlarmFirstArriveIdx();
        Date dateStart = mStackTask.getStartDate(firstAlarmIdx, dateBaseTime, isLimit);

        //カウントダウン数
        long countdown;

        //一番初めのやることが先頭で、現在時刻がそのやることの時間帯に割り込んでいるか判定
        if (firstAlarmIdx == 0 && dateNow.before(dateStart)) {
            //--現在時刻 → 開始時刻 のため、割り込んでいない状態

            //「開始時刻」ー「現在時刻」（現在から開始時刻までの時間）がカウントダウン時間
            countdown = dateStart.getTime() - dateNow.getTime();

            mTaskRefIdx = REF_WAITING;

        } else {
            //--割り込んでいる場合

            //「終了時刻」ー「現在時刻」（現在から終了時刻までの時間）がカウントダウン時間
            Date dateEnd = mStackTask.getEndDate(firstAlarmIdx, dateBaseTime, isLimit);
            countdown = dateEnd.getTime() - dateNow.getTime();

            mTaskRefIdx = firstAlarmIdx;
        }

        //進行中タイマーの設定
        setNextTimer(countdown);

        //「やること」表示処理（進行中／次）
        setupDisplayText();

/*        //最初の開始時間
        Calendar beginCalendar = Calendar.getInstance();
        beginCalendar.setTime(dateBaseTime);
        beginCalendar.add(Calendar.MINUTE, -totalMinute);

        //最初の開始時間をDate型として取得
        Date beginTime = beginCalendar.getTime();

        //現在時刻が既に割り込んでいる場合
        //（現在時刻 → 先頭のやること開始時刻
        long progressToCount;
        if (dateNow.after(beginTime)) {

            //カウントダウン時間(ms)の算出
            long overmsec = dateNow.getTime() - beginTime.getTime();

            //積まれた「やること」参照Indexを調整する(過ぎた分を進める)
            adjustTaskRefIdx(overmsec);

            //タイマーに設定するカウントを取得
            progressToCount = getRemainCount(dateNow, dateBaseTime);

        } else {
            //-- 割り込んでなければ、そのままタイマー時間を取得

            //カウント算出
            progressToCount = beginTime.getTime() - dateNow.getTime();
        }

        Log.i("test", "dateNow=" + dateNow);
        Log.i("test", "beginTime=" + beginTime);
        Log.i("test", "dateBaseTime=" + dateBaseTime);

        //進行中タイマーの設定
        setNextTimer(progressToCount);

        //「やること」表示処理（進行中／次）
        setupDisplayTaskName();
*/

        //最終時刻の取得
        long lastTime;
        if (isLimit) {
            //指定されたリミット時刻
            lastTime = dateBaseTime.getTime();
        } else {
            //最後のやることの終了時刻
            int lastIdx = mStackTask.size() - 1;
            Date dateEnd = mStackTask.getEndDate(lastIdx, dateBaseTime, isLimit);
            lastTime = dateEnd.getTime();
        }

        //現在時刻から最終時刻までの時間を算出
        long toLastCount = lastTime - dateNow.getTime();

        //カウントダウンインスタンスの生成
        FinalCountDown countDownFinal = new FinalCountDown(toLastCount, INTERVAL_FINAL);
        countDownFinal.start();

        return mRootLayout;
    }

    /*
     * グラフの設定
     */
    public void setupGragh() {
        //NavigationView がオープンされた時のリスナーを設定
        DrawerLayout dl = (DrawerLayout)mRootLayout.findViewById(R.id.dl_time);
        DrawerLayout.DrawerListener listener = new TimeDrawerListener();
        dl.addDrawerListener(listener);

        //グラフ表示ボタンリスナー
        ImageView iv_openGragh = (ImageView) mRootLayout.findViewById(R.id.iv_openGragh);
        iv_openGragh.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                //グラフを表示
                DrawerLayout drawer = (DrawerLayout) mRootLayout.findViewById(R.id.dl_time);
                drawer.openDrawer(GravityCompat.END);
            }
        });
    }

    /*
     * 積まれたやること参照Indexの調整
     */
    public Date getBaseTimeDate(String baseDate, String baseTime) {

        try {
            //期限日と期限時間を連結
            String baseTimeStr = baseDate + " " + baseTime;
            //Date型へ変換
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            return sdf.parse(baseTimeStr);

        } catch (ParseException e) {
            e.printStackTrace();

            //エラーメッセージを表示
            //Toast toast = new Toast(mContext);
            //toast.setText("時間を設定してください");
            //toast.show();

            //例外発生時は、終了
            return null;
        }
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
    public void setupDisplayText() {

        //設定文字列
        String progressTask;
        String nextTask;

        //固定文字列の取得
        String waitingStr = mContext.getString(R.string.waiting);
        String noneStr    = mContext.getString(R.string.next_none);

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
            int taskTime = mStackTask.get(mTaskRefIdx).getTaskTime();
            colorId = ResourceManager.getTaskTimeColorId( taskTime );
        }

        //「やること」（進行中／次）の表示設定
        TextView tv_progressTask = mRootLayout.findViewById(R.id.tv_plainProgressTask);
        TextView tv_nextTask     = mRootLayout.findViewById(R.id.tv_nextTask);
        tv_progressTask.setText(progressTask);
        tv_nextTask.setText(nextTask);

        //テキストカラーの変更
        for (int i = 0; i < ((ViewGroup)mRootLayout).getChildCount(); i++) {
            //子ビューを取得
            View v = ((ViewGroup)mRootLayout).getChildAt(i);
            //テキストビューのみ対象
            if( v instanceof TextView ){
                TextView tv = (TextView)v;
                tv.setTextColor( mContext.getResources().getColor(colorId) );
            }
        }
    }

    /*
     * タイマーセット(直近のやることまでのタイマー)
     */
    public void setNextTimer(long count){

        //カウントダウンインスタンスを生成し、タイマー開始
        NextCountDown countDownProgress = new NextCountDown(count, INTERVAL_PROGRESS);
        countDownProgress.start();

        Log.i("test", "setNextTimer started");
    }

    /*
     * カウントダウン(次の時刻)
     */
    private class NextCountDown extends CountDownTimer {

        //カウントダウンフォーマット
        private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        private       TextView         tv_time;

        /*
         * コンストラクタ
         */
        public NextCountDown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);

            //表示ビュー
            tv_time = mRootLayout.findViewById(R.id.tv_progressTime);

            //タイムフォーマットのタイムゾーンをUTCに設定
            TimeZone tz = TimeZone.getTimeZone("UTC");
            sdf.setTimeZone(tz);
        }

        @Override
        public void onFinish() {
            // 完了
            //tv_time.setText(sdfProgressTime.format(0));

            //-- 次のやることのタイマーを設定

            //リファレンスのやることへ進める
            mTaskRefIdx++;

            //表示中の「やること」を更新
            setupDisplayText();
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
            tv_time.setText(sdf.format(millisUntilFinished));
        }
    }

    /*
     * カウントダウン(最終時刻)
     */
    private class FinalCountDown extends CountDownTimer {

        //カウントダウンフォーマット
        private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        private       TextView         tv_time;

        /*
         * コンストラクタ
         */
        public FinalCountDown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);

            //表示ビュー
            tv_time = mRootLayout.findViewById(R.id.tv_finalTime);

            //タイムフォーマットのタイムゾーンをUTCに設定
            TimeZone tz = TimeZone.getTimeZone("UTC");
            sdf.setTimeZone(tz);
        }

        @Override
        public void onFinish() {
            // 完了
            tv_time.setText(sdf.format(0));
        }

        // インターバルで呼ばれる
        @Override
        public void onTick(long millisUntilFinished) {
            // 残り時間を表示
            tv_time.setText(sdf.format(millisUntilFinished));
        }
    }

    /*
     * タイム画面用 DrawerListener
     */
    private class TimeDrawerListener implements DrawerLayout.DrawerListener {

        private boolean isCreate = false;

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onDrawerOpened(@NonNull View drawerView) {
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            Log.i("test", "onDrawerSlide");

            //未生成なら、描画
            if (!isCreate) {

                //グラフを上詰めにするか
                boolean isTopped = isToppedLayout();
                //グラフ追加先のレイアウトファイルを取得
                int graghRootLayout = (isTopped ? R.layout.gragh_body_top : R.layout.gragh_body_bottom);

                setupGragh(graghRootLayout);

                isCreate = true;
            }
        }

        @Override
        public void onDrawerClosed(@NonNull View drawerView) {
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            Log.i("test", "onDrawerStateChanged newState=" + newState);

            //LinearLayout ll_gragh = (LinearLayout) mRootLayout.findViewById(R.id.ll_gragh);
            //Log.i("test", "ll_gragh getMeasuredHeight=" + ll_gragh.getMeasuredHeight());
        }

        /*
         * グラフを上詰めするか否か
         */
        private boolean isToppedLayout() {

            //リミット指定かどうか
            boolean isLimit = mParentActivity.isLimit();
            if (!isLimit) {
                //スタート指定なら、上詰め
                return true;
            }

            //リミット指定なら、グラフがスクロールするか否かで決定（スクロールするなら上詰め）
            //※スクロールありで上詰めするのは、layout_gravityに対してbottom指定すると、適切に表示されないため
            return whetherGraghScroll();
        }

        /*
         * グラフがスクロールするか否か
         */
        private boolean whetherGraghScroll() {

            //グラフ全体の高さを取得
            int graghHeight = calcGraghHeight();

            ScrollView sv_gragh = (ScrollView) mRootLayout.findViewById(R.id.sv_gragh);
            int scrollViewHeight = sv_gragh.getMeasuredHeight();

            Log.i("test", "scrollViewHeight=" + scrollViewHeight);
            Log.i("test", "graghHeight=" + graghHeight);

            //グラフの方が高いなら、trueを返す（スクロールする状態）
            return (graghHeight > scrollViewHeight);
        }

        /*
         * グラフ全体の高さを計算
         */
        private int calcGraghHeight() {

            //高さを集計
            int height = 0;
            for (TaskTable task : mStackTask) {
                int time = task.getTaskTime();
                height += getGraghUnitHeight(time);
            }

            return height;
        }

        /*
         * グラフ生成
         */
        @RequiresApi(api = Build.VERSION_CODES.M)
        private void setupGragh(int graghRootLayout) {

            //ScrollView
            ScrollView sv_gragh = (ScrollView) mRootLayout.findViewById(R.id.sv_gragh);

            //グラフレイアウトを取得(ScrollViewの子)
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v_rootGragh        = inflater.inflate(graghRootLayout, sv_gragh, true);
            LinearLayout ll_gragh   = (LinearLayout) v_rootGragh.findViewById(R.id.ll_gragh);

            //指定されたベース時間をDate型に変換
            String limitDate  = mParentActivity.getLimitDate();
            String limitTime  = mParentActivity.getLimitTime();
            Date dateBaseTime = getBaseTimeDate(limitDate, limitTime);

            //ベース時間の設定
            setupBaseTime(dateBaseTime);

            //グラフ表示
            int size = mStackTask.size();
            for (int i = 0; i < size; i++) {

                //追加するレイアウト(グラフ)
                View v_graghUnit = inflater.inflate(R.layout.outer_task_for_gragh, null);

                //やること情報の設定
                setupTaskInfo(v_graghUnit, i, dateBaseTime);

                //やること時間
                int taskTime = mStackTask.get(i).getTaskTime();

                //グラフにdrawableリソースを設定
                setupGraghInfo(v_graghUnit, taskTime);

                //グラフに追加
                ll_gragh.addView(v_graghUnit);
            }
        }

        /*
         * グラフ（１やること辺り）の高さ取得
         */
        private int getGraghUnitHeight(int taskTime) {

            int height;

            if (taskTime <= 30) {
                //--30min以下

                height = GRAGH_HEIGHT_1_MIN;
                height += (taskTime - 1) * GRAGH_HEIGHT_1_30_PER_MIN;

            } else if (taskTime <= 60) {
                //--60min以下

                height = GRAGH_HEIGHT_30_MIN;
                height += (taskTime - 30 - 1) * GRAGH_HEIGHT_30_60_PER_MIN;

            } else if (taskTime <= 120) {
                //--120min以下

                height = GRAGH_HEIGHT_60_MIN;
                height += (taskTime - 60 - 1) * GRAGH_HEIGHT_60_120_PER_MIN;

            } else {
                //--120min超過

                height = GRAGH_HEIGHT_120_MORE_MIN;
            }

            Log.i("test", "TypedValue=" + (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, getResources().getDisplayMetrics()));

            //高さをdp単位で返す
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, getResources().getDisplayMetrics());
        }

        /*
         * ベース時間の設定
         */
        private void setupBaseTime(Date dateBaseTime) {

            //ベース時間を文字列変換
            String baseTimeStr;
            if (dateBaseTime == null) {
                //未設定なら、未設定文字列を設定
                baseTimeStr = mContext.getString(R.string.limittime_no_input);
            } else {
                //変換
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.JAPANESE);
                baseTimeStr = sdf.format(dateBaseTime);
            }

            //設定対象ビュー
            LinearLayout ll_limitTime = (LinearLayout) mRootLayout.findViewById(R.id.ll_limitTime);
            LinearLayout ll_startTime = (LinearLayout) mRootLayout.findViewById(R.id.ll_startTime);

            boolean isLimit = mParentActivity.isLimit();
            if (isLimit) {
                //表示切り替え
                ll_limitTime.setVisibility(View.VISIBLE);
                ll_startTime.setVisibility(View.GONE);

                ((TextView) ll_limitTime.findViewById(R.id.tv_limitTime)).setText(baseTimeStr);

            } else {
                //表示切り替え
                ll_limitTime.setVisibility(View.GONE);
                ll_startTime.setVisibility(View.VISIBLE);

                ((TextView) ll_startTime.findViewById(R.id.tv_startTime)).setText(baseTimeStr);
            }
        }

        /*
         * 「やること」情報の設定
         */
        private void setupTaskInfo(View view, int idx, Date dateBaseTime) {

            TaskTable task = mStackTask.get(idx);

            TextView tv_taskName = view.findViewById(R.id.tv_taskName);
            TextView tv_taskTime = view.findViewById(R.id.tv_taskTime);
            String timeStr = Integer.toString(task.getTaskTime());

            tv_taskName.setText(task.getTaskName());
            tv_taskTime.setText(timeStr);

            //ベース時間入力なし
            String setTimeStr = null;
            if (dateBaseTime == null) {
                //時間未指定文字列を設定
                setTimeStr = mContext.getString(R.string.limittime_no_input);
            }

            TextView tv_target;
            Date convertedDate = null;

            boolean isLimit = mParentActivity.isLimit();
            if (isLimit) {
                //表示切り替え
                view.findViewById(R.id.ll_startTime).setVisibility(View.VISIBLE);
                view.findViewById(R.id.ll_endTime).setVisibility(View.GONE);

                tv_target = view.findViewById(R.id.tv_taskStartTime);

                //ベース時間入力あり
                if (setTimeStr == null) {
                    //ベース時間取得
                    convertedDate = mStackTask.getStartDateBaseLimit(idx, dateBaseTime);
                }

            } else {
                //表示切り替え
                view.findViewById(R.id.ll_startTime).setVisibility(View.GONE);
                view.findViewById(R.id.ll_endTime).setVisibility(View.VISIBLE);

                tv_target = view.findViewById(R.id.tv_taskEndTime);

                //ベース時間入力あり
                if (setTimeStr == null) {
                    //ベース時間取得
                    convertedDate = mStackTask.getEndDateBaseStart(idx, dateBaseTime);
                }
            }

            //ベース時間入力あり
            if( convertedDate != null ){
                //文字列変換
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.JAPANESE);
                setTimeStr = sdf.format(convertedDate);
            }

            tv_target.setText(setTimeStr);
        }


        /*
         * グラフにdrawable、高さを設定
         */
        @RequiresApi(api = Build.VERSION_CODES.M)
        private void setupGraghInfo(View view, int taskTime) {

            //表示目的に応じて、drawableリソースを取得
            Drawable drawable = mContext.getDrawable(R.drawable.frame_item_task_for_gragh);

            //時間に応じて、色を設定
            int colorId = ResourceManager.getTaskTimeColorId(taskTime);
            drawable.setTint(mContext.getColor(colorId));

            //drawableの設定
            LinearLayout ll_task = (LinearLayout) view.findViewById(R.id.ll_taskInfo);
            ll_task.setBackground(drawable);

            //設定する高さの取得
            int height = getGraghUnitHeight(taskTime);

            //高さを設定
            ViewGroup.LayoutParams params = ll_task.getLayoutParams();
            Log.i("test", "height=" + params.height);
            params.height = height;
            ll_task.setLayoutParams(params);
        }
    }

}
