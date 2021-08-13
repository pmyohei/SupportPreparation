package com.example.supportpreparation;

import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements  AsyncSetTableOperaion.SetOperationListener,
                                                                AsyncTaskTableOperaion.TaskOperationListener,
                                                                AsyncStackTaskTableOperaion.StackTaskOperationListener {

    private AppDatabase         mDB;                        //DB

    //-- フラグメント間共通データ
    private List<TaskTable>     mTaskList;                  //「やること」リスト
                                                            //「積み上げやること」リスト
    private List<TaskTable>     mStackTaskList = new ArrayList<>();
    private String              mLimitDate;                 //リミット-日（"yyyy/MM/dd"）
    private String              mLimitTime;                 //リミット-時（"hh:mm"）

    private Boolean             mReadTask;                  //DB読み込みフラグ-やること
    private Boolean             mReadTaskSet;               //DB読み込みフラグ-やることセット
    private Boolean             mReadStackTask;             //DB読み込みフラグ-積み上げやること

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //DB操作インスタンスを取得
        mDB = AppDatabaseSingleton.getInstance(this);

        //-- 非同期スレッドにて、読み込み開始
        new AsyncTaskTableOperaion(mDB, this, AsyncTaskTableOperaion.DB_OPERATION.READ).execute();

        Log.i("test", "main onSuccessTaskRead");
    }

    /*
     * 「やること」データを取得する
     */
    public List<TaskTable> getTaskData() {
        return mTaskList;
    }

    /*
     * 「積み上げやること」データを取得する
     */
    public List<TaskTable> getStackTaskData() {
        return mStackTaskList;
    }

    /*
     * 「積み上げやること」データの設定
     */
    public void setStackTaskData( List<TaskTable> taskList ) {
        //「積み上げやること」を設定
        mStackTaskList = taskList;

        //DBを更新
        new AsyncStackTaskTableOperaion(mDB, this, AsyncStackTaskTableOperaion.DB_OPERATION.CREATE, mStackTaskList, mLimitDate, mLimitTime).execute();
    }


    /*
     * 「リミット-日付」を取得する
     */
    public String getLimitDate() {
        return mLimitDate;
    }

    /*
     * 「リミット-日付」を設定する
     */
    public void setLimitDate(String value) {
        mLimitDate = value;
    }

    /*
     * 「リミット-時分」を取得する
     */
    public String getLimitTime() {
        return mLimitTime;
    }

    /*
     * 「リミット-時分」を設定する
     */
    public void setLimitTime(String value) {
        mLimitTime = value;
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
    public void onSuccessTaskRead(List<TaskTable> taskList) {
        //「やること」リストを保持
        mTaskList = taskList;

        //「やることセット」
        new AsyncSetTableOperaion(mDB, this, AsyncSetTableOperaion.DB_OPERATION.READ).execute();
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
    public void onSuccessTaskUpdate(String preTask, int preTaskTime, TaskTable updatedTask) {
        //do nothing
    }

    /* --------------------------------------
     * 「やることセット」
     */

    @Override
    public void onSuccessSetRead(List<SetTable> setList, List<List<TaskTable>> tasksList) {
        mReadTaskSet = false;

        //「積み上げやること」
        new AsyncStackTaskTableOperaion(mDB, this, AsyncStackTaskTableOperaion.DB_OPERATION.READ).execute();

        /*
        //-- レイアウトの設定は、データ取得後に行う
        setContentView(R.layout.activity_main);

        //下部ナビゲーション設定
        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(navView, navController);
         */
    }

    @Override
    public void onSuccessSetCreate(Integer code, String setName) {
    }

    @Override
    public void onSuccessSetDelete(String task) {
    }

    @Override
    public void onSuccessSetUpdate(String preTask, String task) {
    }

    /* --------------------------------------
     * 「積み上げやること」
     */
    @Override
    public void onSuccessStackRead( Integer code, StackTaskTable stack, List<TaskTable> taskList ) {

        //DBからデータを取れれば
        if( code == AsyncStackTaskTableOperaion.READ_NORMAL ){
            //DBから取得した「積み上げやること」データを保持
            mStackTaskList = taskList;
            mLimitDate     = stack.getDate();
            mLimitTime     = stack.getTime();

            Log.i("test", "onSuccessStackRead");

        } else {
            //データなければ、未入力文字列
            mLimitTime = getString(R.string.limittime_no_input);
        }

        //-- レイアウトの設定は、データ取得後に行う
        setContentView(R.layout.activity_main);

        //下部ナビゲーション設定
        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(navView, navController);
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