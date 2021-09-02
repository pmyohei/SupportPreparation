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

public class MainActivity extends AppCompatActivity implements AsyncGroupTableOperaion.GroupOperationListener,
                                                                AsyncTaskTableOperaion.TaskOperationListener,
                                                                AsyncStackTaskTableOperaion.StackTaskOperationListener {

    private AppDatabase             mDB;                                //DB

    //-- フラグメント間共通データ
    private List<TaskTable>     mTaskList;                              //「やること」リスト
    private List<GroupTable>    mGroupList;                             //「やることグループ」リスト
    private List<TaskTable>     mStackTaskList = new ArrayList<>();     //「積み上げやること」リスト
    private String              mLimitDate;                             //リミット-日（"yyyy/MM/dd"）
    private String              mLimitTime;                             //リミット-時（"hh:mm"）

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
     * 「やることグループ」データを取得
     */
    public List<GroupTable> getGroupData() {
        return mGroupList;
    }

    /*
     * 「積み上げやること」データを取得
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
    public void onSuccessReadGroup(List<GroupTable> groupList) {

        //DBから取得したデータを保持
        mGroupList       = groupList;

        //「積み上げやること」
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