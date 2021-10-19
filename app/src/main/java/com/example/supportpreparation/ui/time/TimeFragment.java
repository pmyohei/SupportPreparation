package com.example.supportpreparation.ui.time;

import static com.example.supportpreparation.StackTaskTable.NO_DATA;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.supportpreparation.CreateSetAlarmDialog;
import com.example.supportpreparation.MainActivity;
import com.example.supportpreparation.R;
import com.example.supportpreparation.ResourceManager;
import com.example.supportpreparation.StackTaskTable;
import com.example.supportpreparation.TaskArrayList;
import com.example.supportpreparation.TaskTable;
import com.google.android.gms.ads.AdView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TimeFragment extends Fragment {

    //--定数
    private final int REF_WAITING = -1;                 //積み上げ「やること」進行待ち状態

    //--定数（単位変換）
    private final int CONV_SEC_TO_MSEC = 1000;         //単位変換：sec → msec
    private final int CONV_MIN_TO_MSEC = 60000;        //単位変換：min → msec
    private final int INTERVAL_PROGRESS = 1000;         //進行中やることのインターバル（1sec）
    private final int INTERVAL_FINAL = 60000;        //最終時刻までのインターバル（1min）

    //--フィールド
    private MainActivity mParentActivity;               //親アクティビティ
    private Fragment mFragment;                         //本フラグメント
    private Context mContext;                           //コンテキスト（親アクティビティ）
    private View mRootLayout;                           //本フラグメントに設定しているレイアウト
    private StackTaskTable mAlarmStack;       //アラームスタック情報
    private TaskArrayList<TaskTable> mAlarmStackList;   //アラームスタック情報中の積み上げ「やること」
    private int mTaskRefIdx;                            //積み上げ「やること」の参照中インデックス


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
        mAlarmStack = mParentActivity.getAlarmStack();
        mAlarmStackList = mAlarmStack.getStackTaskList();

        //スライド検知リスナーの設定
        //setupSlide();

        //ガイドクローズ
        mParentActivity.closeGuide();

        //アラーム参照Fabの設定
        setupFabRefAlarm();

        //Admodの表示
        mParentActivity.setVisibilityAdmod( View.VISIBLE );

        //Admod分のマージンを設定
        setupMarginTop();

        //グラフの設定
        setupGragh();

        //「やること」の参照インデックス
        mTaskRefIdx = REF_WAITING;

        //やること未スタック
        int size = mAlarmStackList.size();
        if (size == 0) {
            //カウントダウンなし
            return mRootLayout;
        }

        //指定時刻のDate型を取得
        Date dateBaseTime = mAlarmStack.getBaseTimeDate();
        if (dateBaseTime == null) {
            //カウントダウンなし
            return mRootLayout;
        }

        //最終時刻
        Date finalTime = mAlarmStackList.get(size - 1).getEndCalendar().getTime();

        //最終時刻が既に過ぎていた場合
        Date dateNow = new Date();
        if (dateNow.after(finalTime)) {
            //カウントダウンなし
            return mRootLayout;
        }

        //現在時刻から見て一番初めのやることindex
        int firstIdx = mAlarmStack.getFirstArriveIdx();
        if (firstIdx == NO_DATA) {
            //カウントダウンなし
            return mRootLayout;
        }

        //先頭のやることの開始時間
        TaskTable firstTask = mAlarmStackList.get(firstIdx);
        Date dateStart = firstTask.getStartCalendar().getTime();

        //カウントダウン数
        long countdown;

        //一番初めのやることが先頭で、現在時刻がそのやることの時間帯に割り込んでいるか判定
        if (firstIdx == 0 && dateNow.before(dateStart)) {
            //--現在時刻 → 開始時刻 のため、割り込んでいない状態

            //「開始時刻」ー「現在時刻」（現在から開始時刻までの時間）がカウントダウン時間
            countdown = dateStart.getTime() - dateNow.getTime();

            Log.i("test", "countdown a=" + countdown);

            mTaskRefIdx = REF_WAITING;

        } else {
            //--割り込んでいる場合

            //「終了時刻」ー「現在時刻」（現在から終了時刻までの時間）がカウントダウン時間
            Date dateEnd = firstTask.getEndCalendar().getTime();
            countdown = dateEnd.getTime() - dateNow.getTime();

            Log.i("test", "countdown b=" + countdown);

            mTaskRefIdx = firstIdx;
        }

        //進行中タイマーの設定
        setNextTimer(countdown);

        //「やること」表示処理（進行中／次）
        setupDisplayText();

        //現在時刻から最終時刻までの時間を算出
        long toLastCount = finalTime.getTime() - dateNow.getTime();

        //カウントダウンインスタンスの生成
        FinalCountDown countDownFinal = new FinalCountDown(toLastCount, INTERVAL_FINAL);
        countDownFinal.start();

        return mRootLayout;
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    /*
     *　layout_marginTopの設定
     * 　 AdView分のサイズをマージンに追加する
     */
    public void setupMarginTop() {

        AdView adView = mParentActivity.findViewById(R.id.adView);

        //レイアウト確定を受ける
        ViewTreeObserver observer = adView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

                        //レイアウト確定後は、不要なので本リスナー削除
                        adView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        //マージン設定対象
                        View v_border = mRootLayout.findViewById(R.id.v_border);

                        //AdViewの高さ
                        int Height = adView.getHeight();

                        //レイアウトパラメータ
                        ViewGroup.LayoutParams lp = v_border.getLayoutParams();
                        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                        mlp.setMargins(mlp.leftMargin, Height, mlp.rightMargin, mlp.bottomMargin);

                        //マージンを設定
                        v_border.setLayoutParams(mlp);
                    }
                }
        );
    }

    /*
     * スライド検知リスナーの設定
     */
    public void setupSlide() {

        //スライド検知
        mRootLayout.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_MOVE) {


                }
                return true;
            }
        });
    }

    /*
     * FAB(アラーム参照)の設定
     */
    private void setupFabRefAlarm() {

        FloatingActionButton fab_refAlarm = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_stackTaskClear);

        // アラーム参照ボタンの設定
        fab_refAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //ダイアログの生成
                createSetAlarmDialog();

                //アラームをすべて削除
                mParentActivity.cancelAllAlarm();
            }
        });
    }

    /*
     * アラームダイアログの生成
     */
    private void createSetAlarmDialog() {

        //★備考★メッセージを出力する
        if( mAlarmStack.getStackTaskList().size() == 0 ){
            return;
        }

        //FragmentManager生成
        FragmentManager transaction = getParentFragmentManager();

        //ダイアログを生成
        CreateSetAlarmDialog dialog = new CreateSetAlarmDialog(mAlarmStack);

        //設定ボタン押下時リスナー
        dialog.setOnSetBtnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //DB更新
                mParentActivity.setAlarmStack(mAlarmStack);

                //アラーム設定
                mParentActivity.setupAlarm(mAlarmStack);
            }
        });

        dialog.show(transaction, "alarm");
    }

    /*
     * Admodの設定
     */
/*
    public void setupAdmod() {

        AdView adView = mRootLayout.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }
*/

    /*
     * グラフの設定
     */
    public void setupGragh() {
        //NavigationView がオープンされた時のリスナーを設定
        DrawerLayout dl = (DrawerLayout) mRootLayout.findViewById(R.id.dl_time);
        DrawerLayout.DrawerListener listener = new TimeDrawerListener();
        dl.addDrawerListener(listener);

        //グラフ表示ボタンリスナー
        ImageView iv_openGragh = (ImageView) mRootLayout.findViewById(R.id.iv_openGragh);
        iv_openGragh.setOnClickListener(new View.OnClickListener() {
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
/*    public Date getBaseTimeDate(String baseDate, String baseTime) {

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
    }*/

    /*
     * 積まれたやること参照Indexの調整
     */
    public void adjustTaskRefIdx(long overmsec) {

        //「やること時間」累計（msec）
        long total = 0;

        //進行中の「やること」まで進める
        int idx = 0;
        for (TaskTable task : mAlarmStackList) {

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

        int size = mAlarmStackList.size();
        for (int i = mTaskRefIdx; i < size; i++) {
            //進行中タスク以降の「やること時間」を累計
            remainMinute += mAlarmStackList.get(i).getTaskTime();
        }

        //進行中やることの開始時刻を算出
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(finalLimit);
        calendar.add(Calendar.MINUTE, -remainMinute);
        Date progressStartTime = calendar.getTime();

        //現在時刻 - 進行中やることの開始時刻
        long diff = now.getTime() - progressStartTime.getTime();

        //進行中やることの時間
        long taskTime = (long) mAlarmStackList.get(mTaskRefIdx).getTaskTime() * CONV_MIN_TO_MSEC;

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
        String noneStr = mContext.getString(R.string.next_none);

        //カラーID
        int colorId = R.color.tx_time_not_reached;

        if (mTaskRefIdx == REF_WAITING) {
            //-- まだ、初めの「やること」の開始時間に至っていない場合

            //進行中の「やること」
            progressTask = waitingStr;
            //次の「やること」
            nextTask = mAlarmStackList.get(0).getTaskName();

        } else if (mTaskRefIdx >= mAlarmStackList.size()) {
            //-- 「やること」全て完了

            //進行中の「やること」
            progressTask = noneStr;
            //次の「やること」なし
            nextTask = noneStr;

        } else {
            //-- 「やること」突入

            //進行中の「やること」
            progressTask = mAlarmStackList.get(mTaskRefIdx).getTaskName();

            //次の「やること」
            int nextIdx = mTaskRefIdx + 1;
            if (nextIdx < mAlarmStackList.size()) {
                //まだ次の「やること」あり
                nextTask = mAlarmStackList.get(nextIdx).getTaskName();
            } else {
                //次の「やること」なし
                nextTask = noneStr;
            }

            //テキストに設定する色を取得
            int taskTime = mAlarmStackList.get(mTaskRefIdx).getTaskTime();
            colorId = ResourceManager.getTaskTimeColorId(taskTime);
        }

        //カウントダウン画面親ビュー
        ConstraintLayout cl_time = mRootLayout.findViewById(R.id.cl_time);

        //「やること」（進行中／次）の表示設定
        TextView tv_progressTask = cl_time.findViewById(R.id.tv_plainProgressTask);
        TextView tv_nextTask = cl_time.findViewById(R.id.tv_nextTask);
        tv_progressTask.setText(progressTask);
        tv_nextTask.setText(nextTask);

        //テキストカラーの変更
        for (int i = 0; i < ((ViewGroup) cl_time).getChildCount(); i++) {
            //子ビューを取得
            View v = ((ViewGroup) cl_time).getChildAt(i);
            //テキストビューのみ対象
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setTextColor(mContext.getResources().getColor(colorId));
            }
        }
    }

    /*
     * タイマーセット(直近のやることまでのタイマー)
     */
    public void setNextTimer(long count) {

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
        private final TextView tv_time;

        /*
         * コンストラクタ
         */
        public NextCountDown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);

            //表示ビュー
            tv_time = mRootLayout.findViewById(R.id.tv_progressTime);

            //タイムフォーマットのタイムゾーンをUTCに設定
            TimeZone tz = TimeZone.getTimeZone("UTC");
            ResourceManager.sdf_TimeSec.setTimeZone(tz);
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
            if (mTaskRefIdx >= mAlarmStackList.size()) {
                //すべて計算したら、終了
                return;
            }

            //タイマーを再設定
            int taskTime = mAlarmStackList.get(mTaskRefIdx).getTaskTime();
            setNextTimer((long) taskTime * CONV_MIN_TO_MSEC);
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
            tv_time.setText(ResourceManager.sdf_TimeSec.format(millisUntilFinished));
        }
    }

    /*
     * カウントダウン(最終時刻)
     */
    private class FinalCountDown extends CountDownTimer {

        //カウントダウンフォーマット
        private final TextView         tv_time;

        /*
         * コンストラクタ
         */
        public FinalCountDown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);

            //表示ビュー
            tv_time = mRootLayout.findViewById(R.id.tv_finalTime);

            //タイムフォーマットのタイムゾーンをUTCに設定
            TimeZone tz = TimeZone.getTimeZone("UTC");
            ResourceManager.sdf_Time.setTimeZone(tz);
        }

        @Override
        public void onFinish() {
            // 完了
            tv_time.setText(ResourceManager.sdf_Time.format(0));
        }

        // インターバルで呼ばれる
        @Override
        public void onTick(long millisUntilFinished) {
            // 残り時間を表示
            tv_time.setText(ResourceManager.sdf_Time.format(millisUntilFinished));
        }
    }

    /*
     * タイム画面用 DrawerListener
     */
    private class TimeDrawerListener implements DrawerLayout.DrawerListener {

        //--フィールド変数
        private boolean isCreate = false;
        private int mMinHeight;
        private boolean isClose = true;

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onDrawerOpened(@NonNull View drawerView) {
            Log.i("DrawerListener", "onDrawerOpened");

            //現在時間線の描画
            drawCurrentLine();

            //クローズOFF
            isClose = false;
        }

        @Override
        public void onDrawerClosed(@NonNull View drawerView) {
            Log.i("DrawerListener", "onDrawerClosed");

            //広告とヘルプボタンを表示
            mParentActivity.setVisibilityAdmod(View.VISIBLE);
            mParentActivity.setVisibilityHelpBtn(View.VISIBLE);

            //クローズON
            isClose = true;
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            Log.i("DrawerListener", "onDrawerStateChanged newState=" + newState);
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            Log.i("DrawerListener", "onDrawerSlide");

            if( isClose ){
                //閉じている状態からのスライドなら、広告とヘルプボタンを非表示
                mParentActivity.setVisibilityAdmod(View.INVISIBLE);
                mParentActivity.setVisibilityHelpBtn(View.INVISIBLE);
            }

            //やることを積んでいないなら、描画なし
            int size = mAlarmStackList.size();
            if (size == 0) {
                return;
            }

            //グラフ未生成なら、描画
            if (!isCreate) {

                //グラフ最小高さ確定待ち処理。確定後、グラフ生成を行う。
                standByConfirmMinHeight();

                //描画後は、フラグを落とす
                //※画面が切り替わるまで、描画はしない
                isCreate = true;
            }

            //現在時間線の描画
            //drawCurrentLine();
        }

        /*
         * グラフ最小高さ確定待ち処理
         */
        private void standByConfirmMinHeight() {

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            //高さを取得したいビューを一時的に割り当て
            //※高さ確定後はレイアウトから除外する
            LinearLayout ll_limitTime = (LinearLayout) mRootLayout.findViewById(R.id.ll_limitTime);
            inflater.inflate(R.layout.gragh_unit, ll_limitTime, true);

            //レイアウト確定を受ける
            ViewTreeObserver observer = ll_limitTime.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @RequiresApi(api = Build.VERSION_CODES.M)
                        @Override
                        public void onGlobalLayout() {

                            //レイアウト確定後は、不要なので本リスナー削除
                            ll_limitTime.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            //グラフの最小高さを取得
                            LinearLayout ll_taskInfo = ll_limitTime.findViewById(R.id.ll_taskInfo);
                            mMinHeight = ll_taskInfo.getHeight();

                            Log.i("test", "mMinHeight=" + mMinHeight);

                            //高さ確定後は、レイアウトから除外
                            LinearLayout ll_graghInfo = ll_limitTime.findViewById(R.id.ll_graghInfo);
                            ll_limitTime.removeView(ll_graghInfo);

                            //グラフを描画
                            //※スクロールがあることを前提にグラフを生成
                            drawGragh(true);
                        }
                    }
            );

            //Log.i("test", "call check ll_taskInfo = " + ll_taskInfo.getHeight());
        }

        /*
         * グラフ生成
         *   ※1回目のグラフ生成では、スクロールが発生することを全体にコールすること
         */
        @RequiresApi(api = Build.VERSION_CODES.M)
        private void drawGragh(boolean isScroll) {

            //スクロールする　：上詰めレイアウトを使用
            //スクロールしない：下詰めレイアウトを使用
            int graghRootLayout = (isScroll ? R.layout.gragh_parent_topped : R.layout.gragh_parent_bottoming);

            //ScrollView
            ScrollView sv_gragh = mRootLayout.findViewById(R.id.sv_gragh);

            //グラフレイアウトを生成、同時にScrollViewに追加（このレイアウトにグラフ１つ１つのバーを追加する）
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v_rootGragh = inflater.inflate(graghRootLayout, sv_gragh, true);

            //最終時間の設定
            setupLastTimeLine();

            //グラフ追加先view
            LinearLayout ll_gragh = v_rootGragh.findViewById(R.id.ll_gragh);

            //ベース時間
            Date dateBaseTime = mAlarmStack.getBaseTimeDate();

            //現在時間と先頭のやることの間のスペースを設定
            setupCurrentBetweenSpace(inflater, ll_gragh, dateBaseTime);

            //やることグラフを設定
            setupTaskGragh(inflater, ll_gragh, dateBaseTime);

            //スクロールなしなら2回目の処理であるため、ここで終了
            if (!isScroll) {
                return;
            }

            //レイアウト確定後リスナー
            ViewTreeObserver observer = sv_gragh.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @RequiresApi(api = Build.VERSION_CODES.M)
                        @Override
                        public void onGlobalLayout() {

                            //本リスナー削除
                            sv_gragh.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            Log.i("root", "sv_gragh.canScrollVertically()=" + sv_gragh.canScrollVertically(1));
                            Log.i("root", "sv_gragh.canScrollVertically()=" + sv_gragh.canScrollVertically(-1));

                            //スクロールしないのであれば、再度グラフを下詰めで作り直し
                            boolean isScroll = sv_gragh.canScrollVertically(1);
                            if (!isScroll) {

                                //一度作ったグラフを削除
                                FrameLayout fl_gragh = v_rootGragh.findViewById(R.id.fl_gragh);
                                sv_gragh.removeView(fl_gragh);

                                //下詰めで再度グラフ生成
                                drawGragh(false);
                            }
                        }
                    }
            );
        }

        /*
         * 現在時刻線から「先頭のやること」の間のスペースを設定
         */
        private void setupCurrentBetweenSpace(LayoutInflater inflater, LinearLayout root, Date dateBaseTime) {

            //ベース時間の指定なし
            if (dateBaseTime == null) {
                return;
            }

            //現在時刻がやることに割り込んでいるか否か
            boolean isInterrupt = mAlarmStack.isInterruptTask();
            if (isInterrupt) {
                //現在時刻状況を「やること到達」に設定
                return;
            }

            //「現在時刻」から「一番初めのやること開始時間」の時間を計算
            int timeToStart = calcTimeToStartFirstTask();

            //空白グラフ
            View layout_empty = inflater.inflate(R.layout.gragh_empty, null);

            //先頭のやることまでまだ時間があるか
            if (timeToStart > 0) {
                //空グラフ
                View v_empty = layout_empty.findViewById(R.id.v_empty);

                //高さ取得
                int height = getGraghUnitHeight(timeToStart);

                Log.i("root", "setupCurrentBetweenSpace height=" + height);

                //高さ設定
                ViewGroup.LayoutParams params = v_empty.getLayoutParams();
                params.height = height;
                v_empty.setLayoutParams(params);
            }

            //現在時間線の表示
            View v_currentLine = layout_empty.findViewById(R.id.v_currentLine);
            v_currentLine.setVisibility(View.VISIBLE);

            //現在時刻の設定
            Date now = new Date();
            TextView tv_current = layout_empty.findViewById(R.id.tv_current);
            tv_current.setText(ResourceManager.sdf_Time.format(now));

            //グラフに追加
            root.addView(layout_empty);
        }

        /*
         * やることのグラフ設定
         */
        @RequiresApi(api = Build.VERSION_CODES.M)
        private void setupTaskGragh(LayoutInflater inflater, LinearLayout root, Date dateBaseTime) {

            //やること分作成
            int size = mAlarmStackList.size();
            for (int i = 0; i < size; i++) {

                //追加レイアウト(グラフ)
                View v_graghUnit = inflater.inflate(R.layout.gragh_unit, null);

                //やること情報の設定
                setupTaskInfo(v_graghUnit, i, dateBaseTime);

                //グラフにdrawableリソースを設定
                int taskTime = mAlarmStackList.get(i).getTaskTime();
                designGragh(v_graghUnit, taskTime);

                //グラフに追加
                root.addView(v_graghUnit);
            }
        }

        /*
         * 現在時刻線から「先頭のやること」の間のスペースを更新
         */
        private void adjustCurrentBetweenSpace() {

            //空グラフ
            View v_empty = mRootLayout.findViewById(R.id.v_empty);
            if( v_empty == null ){
                //※最小高さの取得よりも先に本処理が走った場合、何もしない（フェールセーフ）
                Log.i("test", "v_empty is null");
                return;
            }

            //「現在時刻」から「一番初めのやること開始時間」の時間を計算
            int timeToStart = calcTimeToStartFirstTask();

            //高さ取得
            int height = getGraghUnitHeight(timeToStart);;

            Log.i("test", "adjustCurrentBetweenSpace  height=" + height);

            //高さ更新
            ViewGroup.LayoutParams params = v_empty.getLayoutParams();
            params.height = height;
            v_empty.setLayoutParams(params);

            //現在時刻の設定
            Date now = new Date();
            TextView tv_current = mRootLayout.findViewById(R.id.tv_current);
            tv_current.setText( ResourceManager.sdf_Time.format(now) );
        }

        /*
         * グラフ（１やること辺り）の高さ取得（単位：dp）
         */
        private int getGraghUnitHeight(int taskTime) {

            //グラフ高さ固定境界値
            final int GRAGH_FIX_HEIGHT_MIN = 60;

            //1min辺りのグラフ高さ増分( 1/4とする )
            int addValue = mMinHeight / 4;

            int height = mMinHeight;

            if( taskTime == 0 ){
                //時間なし
                height = 0;

            } else if( taskTime <= GRAGH_FIX_HEIGHT_MIN){
                //60min以下は時間に応じた高さを設定
                height += (taskTime - 1) * addValue;

            } else{
                //60minを超えるものはすべて同じ高さとする
                height += GRAGH_FIX_HEIGHT_MIN * addValue;
            }

            return height;
        }

        /*
         * 現在時刻から、先頭のやること開始時刻までの時間(分)を取得する。
         *   既にやることに割り込んでいる場合、マイナス値を返す
         */
        private int calcTimeToStartFirstTask() {
            //現在時刻
            Date nowTime = new Date();
            //先頭のやることの開始時刻
            Date startDate = mAlarmStackList.get(0).getStartCalendar().getTime();

            //現在時刻との差を計算
            long nowMills   = nowTime.getTime();
            long startMills = startDate.getTime();

            //分単位で返却
            return (int) ((startMills - nowMills) / CONV_MIN_TO_MSEC);
        }

        /*
         * 最終時間ラインの設定
         */
        private void setupLastTimeLine() {

            //最終時間のビュー
            TextView tv_limitTime = mRootLayout.findViewById(R.id.tv_limitTime);

            //ベース時間チェック
            Date dateBaseTime = mAlarmStack.getBaseTimeDate();
            if (dateBaseTime == null) {
                //未設定なら、未設定文字列を設定
                String baseTimeStr = mContext.getString(R.string.limittime_no_input);
                tv_limitTime.setText(baseTimeStr);
                return;
            }

            //最後のやることの終了時間を設定
            int last = mAlarmStackList.getLastIdx();
            Date endDate = mAlarmStackList.get(last).getEndCalendar().getTime();

            tv_limitTime.setText( ResourceManager.sdf_Time.format(endDate));
        }

        /*
         * 「やること」情報の設定
         */
        private void setupTaskInfo(View view, int idx, Date dateBaseTime) {

            TaskTable task = mAlarmStackList.get(idx);

            //「やること」「やること時間」の設定
            TextView tv_taskName = view.findViewById(R.id.tv_taskName);
            TextView tv_taskTime = view.findViewById(R.id.tv_taskTime);

            //単位文字列
            String unit = mContext.getString(R.string.unit_task_time);
            String timeStr = Integer.toString(task.getTaskTime()) + unit;

            tv_taskName.setText(task.getTaskName());
            tv_taskTime.setText(timeStr);

            //開始時間
            TextView tv_taskStartTime = view.findViewById(R.id.tv_taskStartTime);

            //ベース時間入力なし
            if (dateBaseTime == null) {
                //時間未指定文字列を設定
                String noTime = mContext.getString(R.string.limittime_no_input);
                tv_taskStartTime.setText(noTime);
                return;
            }

            //終了時間を取得
            Date convertedDate = mAlarmStackList.get(idx).getStartCalendar().getTime();

            //文字列変換
            tv_taskStartTime.setText( ResourceManager.sdf_Time.format(convertedDate) );
        }


        /*
         * グラフにdrawable、高さを設定
         */
        @RequiresApi(api = Build.VERSION_CODES.M)
        private void designGragh(View view, int taskTime) {

            //表示目的に応じて、drawableリソースを取得
            @SuppressLint("UseCompatLoadingForDrawables")
            Drawable drawable = mContext.getDrawable(R.drawable.frame_item_task_for_gragh);

            //時間に応じて、色を設定
            int colorId = ResourceManager.getTaskTimeColorId(taskTime);
            drawable.setTint(mContext.getColor(colorId));

            //drawableの設定
            View v_gragh = (View) view.findViewById(R.id.v_gragh);
            v_gragh.setBackground(drawable);

            //設定する高さの取得
            int height = getGraghUnitHeight(taskTime);

            //高さを設定
            ViewGroup.LayoutParams params = v_gragh.getLayoutParams();
            Log.i("test", "designGragh height=" + params.height);
            params.height = height;
            v_gragh.setLayoutParams(params);
        }

        /*
         * 現在時間線の描画
         */
        private void drawCurrentLine() {

            //最終時刻が既に過ぎている場合
            //※ベース時間未入力の判定は、以下の判定に内包されているため、不要
            boolean isPassed = mAlarmStack.isAllTaskPassed();
            if (isPassed) {
                //過ぎているなら、最終ラインに描画
                setMarginCurrentLine(0);
                return;
            }

            //現在時刻がやることに割り込んでいるかどうか
            boolean isInterrupt = mAlarmStack.isInterruptTask();
            if (isInterrupt) {
                //--割り込みあり

                //上部に空白スペースがあれば、削除
                LinearLayout ll_empty = (LinearLayout) mRootLayout.findViewById(R.id.ll_empty);
                if( ll_empty != null ){
                    ViewGroup p = (ViewGroup) ll_empty.getParent();
                    p.removeView(ll_empty);
                    Log.i("test", "ll_empty remove");
                }

                //やることの間に描画
                drawCurrentLineOverlapTask();

            } else {
                //※割り込みなし（先頭のやることに未到達）の場合

                //更新
                adjustCurrentBetweenSpace();
            }
        }

        /*
         * 現在時間線の描画（やることの間に描画する）
         */
        private void drawCurrentLineOverlapTask() {

            //割り込み中のIdxを取得
            //※この前に「isInterruptTask()」で割り込みチェックをしているため、NO_DATAは考慮不要
            int idx = mAlarmStack.getFirstArriveIdx();

            //割り込みIdxより後の高さを取得
            int height = 0;
            for (int i = idx + 1; i < mAlarmStackList.size(); i++) {
                int taskTime = mAlarmStackList.get(i).getTaskTime();
                height += getGraghUnitHeight(taskTime);
            }

            //現在時間ー終了時間
            Date now = new Date();
            long timeToEnd = mAlarmStackList.get(idx).getEndCalendar().getTime().getTime() - now.getTime();
            long timeToEndMin = timeToEnd / CONV_MIN_TO_MSEC;

            //分単位変換後の値に、1分分追加
            //※例えば、残時間 02min30s だったとき、3min分の高さを設定するため。
            //  10:30 に開始/終了ラインがあり、10:30:50 が現在時刻とすると、10:30 のラインに現在時刻線を重ねるための調整
            timeToEndMin += 1;

            //割り込みやることの高さ
            int taskTime = mAlarmStackList.get(idx).getTaskTime();
            int heightTask = getGraghUnitHeight(taskTime);

            //割り込みやることの高さから、残時間分の高さを算出
            float ratio = (float) timeToEndMin / (float) taskTime;
            height += (heightTask * ratio);

            //現在時刻線にマージンを設定
            setMarginCurrentLine(height);
        }

        /*
         * 現在時間線の描画（やることの間に描画する）
         */
        private void setMarginCurrentLine( int value ) {

            //現在時刻線
            View v_currentLine = (View) mRootLayout.findViewById(R.id.v_currentLine);

            if(v_currentLine == null){
                //グラフが一度も描画されていない場合、何もしない
                return;
            }

            //マージン調整用レイアウトパラメータ
            ViewGroup.LayoutParams lp = v_currentLine.getLayoutParams();
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            mlp.setMargins(mlp.leftMargin, mlp.topMargin, mlp.rightMargin, value);

            //マージンを設定
            v_currentLine.setLayoutParams(mlp);

            //表示
            v_currentLine.setVisibility(View.VISIBLE);
        }
    }
}
