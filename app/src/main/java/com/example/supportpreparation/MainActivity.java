package com.example.supportpreparation;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AsyncGroupTableOperaion.GroupOperationListener,
                                                                AsyncTaskTableOperaion.TaskOperationListener,
                                                                AsyncStackTaskTableOperaion.StackTaskOperationListener {

    private AppDatabase             mDB;                                //DB

    //-- フラグメント間共通データ
    private TaskArrayList<TaskTable>    mTaskList;                              //「やること」リスト
    private GroupArrayList<GroupTable>  mGroupList;                             //「やることグループ」リスト
    private StackTaskTable              mStackTable;                            //スタックテーブル
    private StackTaskTable              mAlarmStack;                            //スタックテーブル(アラーム設定)
    private TaskArrayList<TaskTable>    mStackTaskList = new TaskArrayList<>(); //「積み上げやること」リスト
    private String                      mLimitDate;                             //リミット-日（"yyyy/MM/dd"）
    private String                      mLimitTime;                             //リミット-時（"hh:mm"）
    private boolean                     mIsSelectTask;                          //フラグ-「やること」選択エリア表示中
    private boolean                     mIsLimit;                               //フラグ-リミット選択中


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //起動時の選択エリアは「やること」
        mIsSelectTask = true;

        //DB操作インスタンスを取得
        mDB = AppDatabaseSingleton.getInstance(this);
        //非同期スレッドにて、読み込み開始
        new AsyncTaskTableOperaion(mDB, this, AsyncTaskTableOperaion.DB_OPERATION.READ).execute();

        Log.i("test", "main onSuccessTaskRead");
    }

    /*
     *
     */
    public static void readDB(){

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
     * 「積み上げやること」データを取得・設定

    public TaskArrayList<TaskTable> getStackTaskData() {
        return mStackTaskList;
    }
    public void setStackTaskData( TaskArrayList<TaskTable> taskList ) {
        //「積み上げやること」を設定
        mStackTaskList = taskList;

        //DBを更新
        new AsyncStackTaskTableOperaion(mDB, this, AsyncStackTaskTableOperaion.DB_OPERATION.CREATE, mStackTaskList, mLimitDate, mLimitTime).execute();
    }
 */

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
     * 「リミット-日付」を取得・設定

    public String getLimitDate() {
        return mLimitDate;
    }
    public void setLimitDate(String value) {
        mLimitDate = value;
    }
*/
    /*
     * 「リミット-時分」を取得・設定

    public String getLimitTime() {
        return mLimitTime;
    }
    public void setLimitTime(String value) {
        mLimitTime = value;
    }
*/

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
     * 「フラグ-リミット選択中」の取得・設定
    public boolean isLimit() {
        return mIsLimit;
    }
    public void setFlgLimit(boolean flg) {
        mIsLimit = flg;
    }
    */

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
/*
        mTaskTestList = testaList;
        for( TaskTable task: mTaskTestList ){
            Log.i("test", "mTaskTestList task=" + task.getTaskName());
        }
        Log.i("test", "mTaskTestList getTaskByPid=" + mTaskTestList.getTaskByPid(0));
        Log.i("test", "mTaskTestList getTotalTaskTime=" + mTaskTestList.getTotalTaskTime());
        Log.i("test", "mTaskTestList getTopAlarmIndex=" + mTaskTestList.getTopAlarmIndex());

        //test
        mTaskTestList = new TaskArrayList<>();
        TaskTable test = new TaskTable("test1", 10 );
        test.setId(0);
        mTaskTestList.add( test );

        TaskTable test1 = new TaskTable("test2", 30 );
        test1.setId(1);
        mTaskTestList.add( test1 );

        TaskTable task = mTaskTestList.getTaskByPid(0);
        if( task != null ){
            Log.i("test", "mTaskTestList task=" + task.getTaskName());
        }
        task = mTaskTestList.getTaskByPid(1);
        if( task != null ){
            Log.i("test", "mTaskTestList task=" + task.getTaskName());
        }
        //test
        */
        
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
    public void onSuccessStackRead( Integer code, StackTaskTable stack, StackTaskTable alarmStack ) {

        //DBからデータを取れれば
        if( code == AsyncStackTaskTableOperaion.READ_NORMAL ){

            mStackTable = stack;
            mAlarmStack = alarmStack;

            //DBから取得した「積み上げやること」データを保持
            //mStackTaskList = taskList;
            //mLimitDate     = stack.getDate();
            //mLimitTime     = stack.getTime();

            Log.i("test", "onSuccessStackRead");

        } else {
            //データなければ、未入力文字列
            //mLimitTime = getString(R.string.limittime_no_input);
        }

        //※レイアウトの設定は、データ取得後に行う
        setContentView(R.layout.activity_main);

        //下部ナビゲーション設定
        BottomNavigationView navView = findViewById(R.id.bnv_nav);
        NavController navController = Navigation.findNavController(this, R.id.fragment_host);
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