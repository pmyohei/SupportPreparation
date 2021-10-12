package com.example.supportpreparation;

import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity implements AsyncGroupTableOperaion.GroupOperationListener,
                                                                AsyncTaskTableOperaion.TaskOperationListener,
                                                                AsyncStackTaskTableOperaion.StackTaskOperationListener {

    private AppDatabase mDB;                                //DB

    //-- フラグメント間共通データ
    private TaskArrayList<TaskTable> mTaskList;                              //「やること」リスト
    private GroupArrayList<GroupTable> mGroupList;                             //「やることグループ」リスト
    private StackTaskTable mStackTable;                            //スタックテーブル
    private StackTaskTable mAlarmStack;                            //スタックテーブル(アラーム設定)
    private boolean mIsSelectTask;                          //フラグ-「やること」選択エリア表示中

    private boolean mSplashEnd;
    private boolean mReadData;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //起動時の選択エリアは「やること」
        mIsSelectTask = true;
        //アニメーション終了OFF
        mSplashEnd = false;
        //DB読み込み終了OFF
        mReadData = false;

        //DB操作インスタンスを取得
        mDB = AppDatabaseSingleton.getInstance(this);
        //非同期スレッドにて、読み込み開始
        new AsyncTaskTableOperaion(mDB, this, AsyncTaskTableOperaion.DB_OPERATION.READ).execute();

        //スプラッシュ用アニメーション開始
        startSplashAnimation();

        Log.i("test", "main onSuccessTaskRead");
    }

    /*
     * スプラッシュアニメーション開始
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startSplashAnimation(){

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
                if( mReadData ) {
                    setupMainLayout();
                }
            }
        });
    }

    /*
     * レイアウト設定
     */
    private void setupMainLayout(){

        //スプラッシュレイアウトを削除
        View cl_splash = findViewById(R.id.cl_splash);
        View v_parent = cl_splash.getRootView();
        ((ViewGroup)v_parent).removeView( cl_splash );

        //メインのレイアウト設定
        setContentView(R.layout.activity_main);

        //下部ナビゲーション設定
        BottomNavigationView navView = findViewById(R.id.bnv_nav);
        NavController navController = Navigation.findNavController(this, R.id.fragment_host);
        NavigationUI.setupWithNavController(navView, navController);
    }

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
     *  -------------------------------------------------
     *  インターフェース
     *  -------------------------------------------------
     */

    /* --------------------------------------
     * 「やること」
     */

    @Override
    public void onSuccessTaskRead(TaskArrayList<TaskTable> taskList) {
        //「やること」リストを保持
        mTaskList = taskList;

        //０件なら、空のデータをリストに入れておく
        //※選択エリアのサイズを確保するため
        mTaskList.addEmpty();

        //「やることセット」
        new AsyncGroupTableOperaion(mDB, this, AsyncGroupTableOperaion.DB_OPERATION.READ).execute();
    }

    @Override
    public void onSuccessTaskCreate(Integer code, TaskTable taskTable) {
        //do nothing
    }
    @Override
    public void onSuccessTaskDelete(String task, int taskTime) {
        //do nothing
    }
    @Override
    public void onSuccessEditTask(String preTask, int preTaskTime, TaskTable updatedTask) {
        //do nothing
    }

    /* --------------------------------------
     * 「やることグループ」
     */
    @Override
    public void onSuccessReadGroup(GroupArrayList<GroupTable> groupList) {

        //DBから取得したデータを保持
        mGroupList = groupList;

        //０件なら、空のデータをリストに入れておく
        //※選択エリアのサイズを確保するため
        mGroupList.addEmpty();

        //「積み上げやること」の読み込み
        new AsyncStackTaskTableOperaion(mDB, this, AsyncStackTaskTableOperaion.DB_OPERATION.READ).execute();
    }

    @Override
    public void onSuccessCreateGroup(Integer code, GroupTable group) {
    }
    @Override
    public void onSuccessDeleteGroup(String task) {
    }
    @Override
    public void onSuccessEditGroup(String preTask, String groupName) {
    }
    @Override
    public void onSuccessUpdateTask(int groupPid, String taskPidsStr){
    }


    /* --------------------------------------
     * 「積み上げやること」
     */
    @Override
    public void onSuccessStackRead( Integer code, StackTaskTable stack, StackTaskTable alarmStack ) {

        //DBからデータを取れれば
        if( code == AsyncStackTaskTableOperaion.READ_NORMAL ){

            mStackTable = stack;
            mAlarmStack = alarmStack;

            Log.i("test", "onSuccessStackRead");
        }

        //フラグON
        mReadData = true;

        //スプラッシュアニメーションが終了していれば、レイアウト設定
        if( mSplashEnd ){
            setupMainLayout();
        }
    }

    @Override
    public void onSuccessStackCreate() {
        //do nothing
    }
    @Override
    public void onSuccessStackDelete() {
        //do nothing
    }

}