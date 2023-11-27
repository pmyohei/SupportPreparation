package com.stacktime.supportpreparation;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_NO_CREATE;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.stacktime.supportpreparation.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/*
 * メインとなるActivity
 *   本Activityの上にフラグメントを生成
 */
public class MainActivity extends AppCompatActivity implements  AsyncAllReadOperaion.AsyncAllReadOperaionListener,
                                                                AsyncGroupTableOperaion.GroupOperationListener,
                                                                AsyncTaskTableOperaion.TaskOperationListener,
                                                                AsyncStackTaskTableOperaion.StackTaskOperationListener {

    //画面種別
    public enum FRAGMENT_KIND {
        STACK(0),          //スタック画面
        TASK(1),           //タスク画面
        GROUP(2),          //グループ画面
        TIME(3);           //タイマ画面

        private final int value;

        FRAGMENT_KIND(int i) {
            value = i;
        }

        int getValue() {
            return value;
        }
    }

    //UI操作情報保存
    private final String SHARED_DATA_NAME = "UIData";
    private final String SHARED_KEY_COUNTDOWN_STOP = "CountDownStop";

    //-- フラグメント間共通データ
    private AppDatabase mDB;                                //DB
    private TaskArrayList<TaskTable> mTaskList;             //「やること」リスト
    private GroupArrayList<GroupTable> mGroupList;          //「やることグループ」リスト
    private StackTaskTable mStackTable;                     //スタックテーブル
    private StackTaskTable mAlarmStack;                     //スタックテーブル(アラーム設定)
    private boolean mIsSelectTask;                          //フラグ-「やること」選択エリア表示中
    private List<List<Integer>> mGuideList;                 //操作案内レイアウトIDリスト(画面毎のすべてのID)
    private AdSize mAdSize;                                 //AdViewのサイズ
    private FRAGMENT_KIND mPreFrgKind;                      //前回のガイド要求フラグメント種別
    private boolean mReadData;                              //DB読み込みフラグ
    private boolean mIsStop;                                //カウントダウン停止フラグ
    private Snackbar mSnackbar;                             //スナックバー


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_splash);
        //setContentView(R.layout.activity_main);

        //ダークモード非対応
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO);

        //非同期スレッドにて、読み込み開始
        //-- レイアウト設定は、DBread完了後に行う --//
        mDB = AppDatabaseSingleton.getInstance(this);
        new AsyncAllReadOperaion(mDB, this).execute();

        //スプラッシュ用アニメーション開始
/*        if (Build.VERSION.SDK_INT >= 23) {
            startSplashAnimation();
        } else {
            startSplashAnimation_less_23();
        }*/

        //起動時の選択エリアは「やること」
        mIsSelectTask = true;
        //DB読み込み終了OFF
        mReadData = false;

        //広告サイズ初期値
        mAdSize = null;

        //前回のガイド要求フラグメント種別
        mPreFrgKind = null;

        //UIデータ読み込み
        SharedPreferences spData = getSharedPreferences(SHARED_DATA_NAME, MODE_PRIVATE);
        mIsStop = spData.getBoolean(SHARED_KEY_COUNTDOWN_STOP, false);

        //AdMob初期化
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });


    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /*
     * スプラッシュアニメーション開始
     */
/*    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startSplashAnimation() {

        //アイコンアニメーション
        ImageView iv_splash = findViewById(R.id.iv_splash);
        iv_splash.setBackgroundResource(R.drawable.avd_splash);
        AnimatedVectorDrawable rocketAnimation = (AnimatedVectorDrawable) iv_splash.getBackground();
        rocketAnimation.start();

        rocketAnimation.registerAnimationCallback(new Animatable2.AnimationCallback() {
            @Override
            public void onAnimationEnd(Drawable drawable) {
                super.onAnimationEnd(drawable);

                mSplashEnd = true;

                //DBの読み取りが完了していれば、レイアウト設定
                if (mReadData) {
                    setupMainLayout();
                }
            }
        });
    }*/

    /*
     * スプラッシュアニメーション開始
     */
/*    private void startSplashAnimation_less_23() {

        //アニメーション
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.splash_less_23);

        //API23以上用のビューは非表示
        findViewById(R.id.iv_splash).setVisibility(View.GONE);

        //API23未満用のビューを表示
        ImageView iv_splash_less_23 = findViewById(R.id.iv_splash_less_23);
        iv_splash_less_23.setVisibility(View.VISIBLE);

        //アニメーション開始
        iv_splash_less_23.startAnimation(animation);

        //アニメーションリスナー
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                mSplashEnd = true;

                //DBの読み取りが完了していれば、レイアウト設定
                if (mReadData) {
                    setupMainLayout();
                }
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }*/

    /*
     * レイアウト設定
     */
    private void setupMainLayout() {

        //スプラッシュレイアウトを削除
/*        View cl_splash = findViewById(R.id.cl_splash);
        View v_parent = cl_splash.getRootView();
        ((ViewGroup) v_parent).removeView(cl_splash);
*/
        //メインのレイアウト設定
        setContentView(R.layout.activity_main);

        //下部ナビゲーション設定
        BottomNavigationView navView = findViewById(R.id.bnv_nav);
        NavController navController = Navigation.findNavController(this, R.id.fragment_host);
        NavigationUI.setupWithNavController(navView, navController);

        //アイコン再選択時のリスナー
        navView.setOnItemReselectedListener(new BottomNavigationView.OnItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem item) {
                //何もしない処理をオーバライドすることで、再選択時の再描画を防ぐ
                return;
            }
        });

        //広告のロード
        loadAdmod();
        //各画面の操作案内のレイアウトIDを保持
        holdGuideList();
        //ヘルプボタンの設定
        setupHelp();
    }

    /*
     * 各画面の操作案内レイアウトIDを保持
     */
    private void holdGuideList() {

        mGuideList = new ArrayList<>();

        for (FRAGMENT_KIND kind : FRAGMENT_KIND.values()) {

            List<Integer> layoutList = new ArrayList<>();

            //画面種別に応じて、レイアウトIDを保持
            switch (kind) {
                case STACK:
                    layoutList.add(R.layout.guide_stack_page_1);
                    layoutList.add(R.layout.guide_stack_page_2);
                    layoutList.add(R.layout.guide_stack_page_3);
                    layoutList.add(R.layout.guide_stack_page_4);
                    layoutList.add(R.layout.guide_stack_page_5);
                    layoutList.add(R.layout.guide_stack_page_6);
                    break;

                case TASK:
                    layoutList.add(R.layout.guide_task_page_1);
                    layoutList.add(R.layout.guide_task_page_2);
                    layoutList.add(R.layout.guide_task_page_3);
                    layoutList.add(R.layout.guide_task_page_4);
                    break;

                case GROUP:
                    layoutList.add(R.layout.guide_group_page_1);
                    layoutList.add(R.layout.guide_group_page_2);
                    layoutList.add(R.layout.guide_group_page_3);
                    layoutList.add(R.layout.guide_group_page_4);
                    layoutList.add(R.layout.guide_group_page_5);
                    layoutList.add(R.layout.guide_group_page_6);
                    break;

                case TIME:
                    layoutList.add(R.layout.guide_time_page_1);
                    layoutList.add(R.layout.guide_time_page_2);
                    layoutList.add(R.layout.guide_time_page_3);
                    break;
            }

            mGuideList.add(kind.getValue(), layoutList);
        }
    }

    /*
     * 「ヘルプボタン」設定
     */
    private void setupHelp() {

        ImageButton ib_help = findViewById(R.id.ib_help);
        ib_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LinearLayout ll_guide = findViewById(R.id.ll_guide);
                if (ll_guide.getVisibility() == View.VISIBLE) {
                    //非表示
                    ll_guide.setVisibility(View.GONE);
                    //アイコン変更
                    v.setBackgroundResource(R.drawable.baseline_help_outline_24);

                    return;
                }

                //アイコン変更
                v.setBackgroundResource(R.drawable.baseline_help_24);

                //下部ナビゲーション
                BottomNavigationView bnv_nav = findViewById(R.id.bnv_nav);

                //メニュー
                Menu menu = bnv_nav.getMenu();

                //表示中の画面のガイドを表示
                for (FRAGMENT_KIND kind : FRAGMENT_KIND.values()) {

                    //メニューアイテム
                    MenuItem menuItem = menu.getItem(kind.getValue());

                    //選択中のメニューアイテム
                    if (menuItem.isChecked()) {
                        //ガイド表示
                        openOperationGuide(kind);
                        return;
                    }
                }
            }
        });
    }

    /*
     * 操作案内の表示
     */
    @SuppressLint("NotifyDataSetChanged")
    private void openOperationGuide(FRAGMENT_KIND kind) {

        //要求元のフラグメントが異なるなら、アダプタ再アタッチ
        if (mPreFrgKind != kind) {

            //ViewPager2
            ViewPager2 vp2_guide = findViewById(R.id.vp2_guide);

            //表示するレイアウトリスト
            List<Integer> list = mGuideList.get(kind.getValue());

            //アダプタ生成・割り当て
            //※ガイド表示時、前のガイドで閉じたページ位置から表示されてしまうため、
            //  アダプタを毎回割り当てる方針としている
            OperationGuideRecyclerAdapter adapter = new OperationGuideRecyclerAdapter(list);
            vp2_guide.setAdapter(adapter);

            //インジケータの設定
            TabLayout tabLayout = findViewById(R.id.tab_layout);
            new TabLayoutMediator(tabLayout, vp2_guide,
                    (tab, position) -> tab.setText("")
            ).attach();
        }

        //表示
        LinearLayout ll_guide = findViewById(R.id.ll_guide);
        ll_guide.setVisibility(View.VISIBLE);

        //種別保持
        mPreFrgKind = kind;
    }

    /*
     * ガイドクローズ
     */
    public void closeGuide() {

        LinearLayout ll_guide = findViewById(R.id.ll_guide);
        if (ll_guide == null) {
            return;
        }

        if (ll_guide.getVisibility() == View.VISIBLE) {
            //非表示
            ll_guide.setVisibility(View.GONE);

            //アイコン変更
            ImageButton ib_help = findViewById(R.id.ib_help);
            ib_help.setBackgroundResource(R.drawable.baseline_help_outline_24);
        }
    }

    /*
     * アラーム設定
     */
    public void setAlarm(StackTaskTable stackTable) {

        //AlarmManagerの取得
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) {
            //メッセージを表示
            Toast.makeText(this, R.string.toast_error_occurred, Toast.LENGTH_SHORT).show();
            return;
        }

        //設定中アラームの削除
        cancelAllAlarm();

        //最終時刻カレンダー
        TaskArrayList<TaskTable> taskList = stackTable.getStackTaskList();
        int last = taskList.getLastIdx();
        Calendar calender = taskList.get(last).getEndCalendar();

        //現在時刻
        Date dateNow = new Date();

        //最終時刻が過ぎていれば、アラーム設定はなし
        if (dateNow.after(calender.getTime())) {
            return;
        }

        //リクエストコード
        int requestCode = 0;

        //通知メッセージに付与する接尾文
        String suffixStr = getString(R.string.notify_task_suffix);

        //各「やること」のアラームを設定
        for (TaskTable task : taskList) {

            //アラーム対象外なら次へ
            if (!task.isOnAlarm()) {
                continue;
            }

            //現在時刻を過ぎているアラームなら、設定せず次へ
            if (dateNow.after(task.getStartCalendar().getTime())) {
                //Log.i("skip", "skip check task=" + task.getTaskName());
                continue;
            }

            //Receiver側へのデータ
            Intent intent = new Intent(this, AlarmBroadcastReceiver.class);
            intent.putExtra(ResourceManager.NOTIFY_SEND_KEY, task.getTaskName() + suffixStr);

            //Log.i("setAlarm", "message=" + task.getTaskName() + suffixStr);

            //アラーム設定時間
            long millis = task.getStartCalendar().getTimeInMillis();

            //アラームの設定
            int flag = (Build.VERSION.SDK_INT > Build.VERSION_CODES.R ? FLAG_IMMUTABLE : 0 );
            PendingIntent pending
                    = PendingIntent.getBroadcast(this, requestCode, intent, flag);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, millis, pending);

            //リクエストコードを更新
            requestCode++;
        }

        //最終時刻のアラーム設定
        //アラーム設定あり、かつ、現在時刻の方が前の時間
        if (stackTable.isOnAlarm()) {

            String message = getString(R.string.notify_final_name);

            Log.i("setAlarm", "message=" + message);

            //Receiver側へのデータ
            Intent intent = new Intent(this, AlarmBroadcastReceiver.class);
            intent.putExtra(ResourceManager.NOTIFY_SEND_KEY, message);

            long millis = calender.getTimeInMillis();

            //フラグ
            int flag = (Build.VERSION.SDK_INT > Build.VERSION_CODES.R ? FLAG_IMMUTABLE : 0 );

            //アラームの設定
            PendingIntent pending
                    = PendingIntent.getBroadcast(this, requestCode, intent, flag);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, millis, pending);

            //リクエストコードを更新
            requestCode++;
        }

        //Toastメッセージ
        //リクエストコードが増えていなければ、アラームは未設定
        int stringId = ((requestCode == 0) ? R.string.toast_nothing_notification : R.string.toast_set_notification);

        //メッセージを表示
        Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show();
    }

    /*
     * 設定中アラームの全キャンセル
     */
    public void cancelAllAlarm() {

        //AlarmManagerの取得
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

        for (int i = 0; i < ResourceManager.MAX_STACK_TASK_NUM; i++) {
            //PendingIntentを取得
            Intent intent = new Intent(this, AlarmBroadcastReceiver.class);
            PendingIntent pendingIntent;

            Log.i("test", "Build.VERSION.SDK_INT=" + Build.VERSION.SDK_INT);
            if( Build.VERSION.SDK_INT > Build.VERSION_CODES.R ){

                Log.i("test", "aa Build.VERSION.SDK_INT=" + Build.VERSION.SDK_INT);

                //API30を超える場合
                pendingIntent = PendingIntent.getBroadcast(this, i, intent, FLAG_IMMUTABLE);
            } else {
                //※「FLAG_NO_CREATE」を指定することで、新規のPendingIntent（アラーム未生成）の場合は、nullを取得する
                pendingIntent = PendingIntent.getBroadcast(this, i, intent, FLAG_NO_CREATE);
            }

            if (pendingIntent == null) {
                //未生成ならキャンセル処理終了
                Log.i("test", "cancelAllAlarm= + i");
                break;
            }

            //アラームキャンセル
            pendingIntent.cancel();
            am.cancel(pendingIntent);
        }
    }

    /*
     * Admodのロード
     */
    private void loadAdmod() {

        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    /*
     * Admodの表示／非表示
     */
    public void setVisibilityAdmod(int value) {

        //--------------------
        // 広告無効化
        //--------------------
        // ！常時非表示　2023/11/27
        boolean disableAd = true;
        if( disableAd ){
//           return;
        }

        //--------------------
        // 広告表示制御
        //--------------------
        AdView adView = findViewById(R.id.adView);
        if (adView == null) {
            //起動時、未読み込み時のガード
            return;
        }

        adView.setVisibility(value);
    }

    /*
     * HelpUIの表示／非表示
     */
    public void setVisibilityHelpBtn(int value) {

        ImageButton ib_help = findViewById(R.id.ib_help);
        if (ib_help == null) {
            //起動時、未読み込み時のガード
            return;
        }

        ib_help.setVisibility(value);
    }

    /*
     * Admodのサイズを取得
     */
    public AdSize getAdsize() {

        if (mAdSize != null) {
            return mAdSize;
        }

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);

        //サイズ保持
        mAdSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);

        return mAdSize;
    }

    /*
     * ヘルプボタンの高さを取得
     */
    public int getHelpButtonHeight() {

        return findViewById(R.id.ib_help).getHeight();
    }

    /*
     * スナックバーの表示
     */
    public void showSnackbar(View.OnClickListener actionListerner, Snackbar.Callback callbackListerner) {

        //表示先親ビュー
        FrameLayout fl_main = findViewById(R.id.fl_main);

        //下部ナビゲーション
        BottomNavigationView bnv = findViewById(R.id.bnv_nav);

        //スナックバー
        mSnackbar = Snackbar
                //オブジェクト生成
                .make(fl_main, R.string.snackbar_delete, Snackbar.LENGTH_LONG)

                //アクションボタン押下時の動作
                .setAction(R.string.snackbar_undo, actionListerner)

                //スナックバークローズ時の動作
                .addCallback(callbackListerner)

                //レイアウト
                .setAnchorView(bnv)
                .setBackgroundTint(getResources().getColor(R.color.main))
                .setTextColor(getResources().getColor(R.color.white))
                .setActionTextColor(getResources().getColor(R.color.white));

        //表示
        mSnackbar.show();
    }

    /*
     * スナックバーの非表示
     */
    public void dismissSnackbar() {

        if (mSnackbar == null) {
            return;
        }

        if (mSnackbar.isShown()) {
            //表示中なら非表示に
            mSnackbar.dismiss();
        }
    }

    /*
     * BottomNavigationViewの高さを取得
     */
    public int getBottomNavigationViewHeight() {
        return findViewById(R.id.bnv_nav).getHeight();
    }

    //-- Gettert Settert --------------------

    /*
     * 「やること」データを取得する
     */
    public TaskArrayList<TaskTable> getTaskData() {
        return mTaskList;
    }

    /*
     * 「やることグループ」データを取得
     */
    public GroupArrayList<GroupTable> getGroupData() {
        return mGroupList;
    }

    /*
     * 「スタック」データを取得・設定
     */
    public StackTaskTable getStackTable() {
        return mStackTable;
    }
    public void setStackTable( StackTaskTable stackTable ) {

        //DBを更新
        new AsyncStackTaskTableOperaion(mDB, this, AsyncStackTaskTableOperaion.DB_OPERATION.CREATE, mStackTable).execute();
    }

    /*
     * 「スタック」データ(アラーム)を取得・設定
     */
    public StackTaskTable getAlarmStack() {
        return mAlarmStack;
    }
    public void setAlarmStack( StackTaskTable alarmStack ) {

        //！スタック画面上で、clone()生成されているため、インスタンスをコピーすることで同期
        mAlarmStack = alarmStack;

        //DBを更新
        new AsyncStackTaskTableOperaion(mDB, this, AsyncStackTaskTableOperaion.DB_OPERATION.CREATE, mAlarmStack).execute();
    }

    /*
     * 「フラグ-「やること」選択エリア表示中」を取得・設定
     */
    public boolean isSelectTask() {
        return mIsSelectTask;
    }
    public void setFlgSelectTask(boolean flg) {
        mIsSelectTask = flg;
    }

    /*
     * 「フラグ-「カウントダウン停止」を取得・設定
     */
    public boolean isStop() {
        return mIsStop;
    }
    public void setIsStop(boolean isStop) {
        mIsStop = isStop;

        //UIデータ保存処理
        SharedPreferences spData = getSharedPreferences(SHARED_DATA_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = spData.edit();
        editor.putBoolean(SHARED_KEY_COUNTDOWN_STOP, mIsStop);
        editor.apply();
    }


    /*
     *  -------------------------------------------------
     *  インターフェース
     *  -------------------------------------------------
     */
    @Override
    public void onRead(TaskArrayList<TaskTable> taskList,
                       GroupArrayList<GroupTable> groupList,
                       StackTaskTable stack,
                       StackTaskTable alarmStack ) {

        //「やること」リストを保持
        mTaskList = taskList;
        //０件なら、空のデータをリストに入れておく
        //※選択エリアのサイズを確保するため
        mTaskList.addEmpty();

        //DBから取得したデータを保持
        mGroupList = groupList;
        //０件なら、空のデータをリストに入れておく
        //※選択エリアのサイズを確保するため
        mGroupList.addEmpty();

        //DBからデータを取れれば
        mStackTable = stack;
        mAlarmStack = alarmStack;

        //スプラッシュアニメーションが終了していれば、レイアウト設定
        setupMainLayout();
    }

    /* --------------------------------------
     * 「やること」
     */
    @Override
    public void onSuccessTaskCreate(Integer code, TaskTable taskTable) {
        //do nothing
    }
    @Override
    public void onSuccessTaskDelete(String task, int taskTime) {
        //do nothing
    }
    @Override
    public void onSuccessEditTask(Integer code, String preTask, int preTaskTime, TaskTable updatedTask) {
        //do nothing
    }

    /* --------------------------------------
     * 「やることグループ」
     */
    @Override
    public void onSuccessCreateGroup(Integer code, GroupTable group) {
    }
    @Override
    public void onSuccessDeleteGroup(String task) {
    }
    @Override
    public void onSuccessEditGroup(Integer code, String preTask, String groupName) {
    }
    @Override
    public void onSuccessUpdateTask(int groupPid, String taskPidsStr){
    }

    /* --------------------------------------
     * 「積み上げやること」
     */
    @Override
    public void onSuccessStackCreate() {
        //do nothing
    }
    @Override
    public void onSuccessStackDelete() {
        //do nothing
    }
}