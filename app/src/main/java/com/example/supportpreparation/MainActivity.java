package com.example.supportpreparation;

import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AsyncSetTableOperaion.SetOperationListener,
                                                               AsyncTaskTableOperaion.TaskOperationListener{

    private AppDatabase         mDB;                        //DB

    //-- フラグメント間共通データ
    private List<TaskTable>     mTaskList;                  //「やること」リスト
                                                            //積み上げられた「やること」リスト
    private List<TaskTable>     mStackTaskList = new ArrayList<>();
    private String              mLimitDate;             //リミット-日（"yyyy/MM/dd"）
    private String              mLimitTime;             //リミット-時（"hh:mm"）

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //※レイアウトの設定は、データ取得後に行う

        //初期値は未入力文字列
        mLimitTime = getString(R.string.limittime_no_input);

        //DB操作インスタンスを取得
        mDB = AppDatabaseSingleton.getInstance(this);

        //-- 非同期スレッドにて、読み込み開始
        //「やること」
        new AsyncTaskTableOperaion(mDB, this, AsyncTaskTableOperaion.DB_OPERATION.READ).execute();
        //「やることセット」
        new AsyncSetTableOperaion(mDB, this, AsyncSetTableOperaion.DB_OPERATION.READ).execute();

    }

    /*
     * 「やること」データを取得する
     */
    public List<TaskTable> getTaskData() {
        return mTaskList;
    }

    /*
     * 積み上げられた「やること」データを取得する
     */
    public List<TaskTable> getStackTaskData() {
        return mStackTaskList;
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

    /* -------------------
     * 「やることセット」
     */

    @Override
    public void onSuccessSetRead(List<SetTable> setList, List<List<TaskTable>> tasksList) {

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

    /* -------------------
     * 「やること」
     */

    @Override
    public void onSuccessTaskRead(List<TaskTable> taskList) {
        //「やること」リストを保持
        mTaskList = taskList;

        //-- レイアウトの設定は、データ取得後に行う
        setContentView(R.layout.activity_main);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_time)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        //タイトルバーを非表示にするため、コメントアウト
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        Log.i("test", "main onSuccessTaskRead");
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

}