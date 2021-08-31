package com.example.supportpreparation;

import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

/*
 * 非同期-DBアクセスクラス
 */
public class AsyncTaskTableOperaion extends AsyncTask<Void, Void, Integer> {

    //-- DB操作種別
    public enum DB_OPERATION {
        CREATE,         //生成
        READ,           //参照
        UPDATE,         //更新
        DELETE;         //削除
    }

    private AppDatabase mDB;
    private DB_OPERATION mOperation;
    private String mPreTask;
    private int mPreTaskTime;
    private String mNewTaskName;
    private int mNewTaskTime;
    private TaskTable mTaskTable;
    private List<TaskTable> mTaskList;
    private TaskOperationListener mListener;

    /*
     * コンストラクタ
     *   表示
     */
    public AsyncTaskTableOperaion(AppDatabase db, TaskOperationListener listener, DB_OPERATION operation){
        mDB         = db;
        mListener   = listener;
        mOperation  = operation;
    }

    /*
     * コンストラクタ
     *   生成・削除
     */
    public AsyncTaskTableOperaion(AppDatabase db, TaskOperationListener listener, DB_OPERATION operation, String task, int taskTime){
        mDB = db;
        mListener = listener;
        mOperation = operation;
        mNewTaskName = task;
        mNewTaskTime = taskTime;
    }

    /*
     * コンストラクタ
     *   更新
     */
    public AsyncTaskTableOperaion(AppDatabase db, TaskOperationListener listener, DB_OPERATION operation, String preTask, int preTaskTime, String task, int taskTime){
        mDB = db;
        mListener = listener;
        mOperation = operation;
        mPreTask = preTask;
        mPreTaskTime = preTaskTime;
        mNewTaskName = task;
        mNewTaskTime = taskTime;
    }

    @Override
    protected Integer doInBackground(Void... params) {

        Integer ret = 0;

        TaskTableDao taskTableDao = mDB.taskTableDao();

        //--操作種別に応じた処理
        if(mOperation == DB_OPERATION.CREATE){
            //登録
            ret = createTaskData(taskTableDao);

        } else if(mOperation == DB_OPERATION.READ ){
            //表示
            displayTaskData(taskTableDao);

        } else if(mOperation == DB_OPERATION.UPDATE ){
            //編集
            updateTaskData(taskTableDao);

        } else if(mOperation == DB_OPERATION.DELETE ){
            //削除
            deleteTaskData(taskTableDao);

        } else{
            //do nothing
        }

        return ret;
    }

    /*
     * 「やること」の生成処理
     */
    private Integer createTaskData( TaskTableDao dao ){

        //プライマリーキー取得
        int pid = dao.getPid( mNewTaskName, mNewTaskTime);
        if( pid > 0 ){
            //すでに登録済みであれば、DBには追加しない
            return -1;
        }

        //DBに追加
        mTaskTable = new TaskTable( mNewTaskName, mNewTaskTime);
        dao.insert( mTaskTable);

        //正常終了
        return 0;
    }

    /*
     * 「やること」の表示処理
     */
    private void displayTaskData( TaskTableDao dao ){

        //DBから、保存済みのタスクリストを取得
        mTaskList = dao.getAll();
    }

    /*
     * 「やること」の編集処理
     */
    private void updateTaskData( TaskTableDao dao ){
        //更新対象のPidを取得
        int pid = dao.getPid( mPreTask, mPreTaskTime);

        //更新
        dao.updateByPid( pid, mNewTaskName, mNewTaskTime);

        //更新したレコードを取得
        mTaskTable = dao.getRecord( pid );
    }

    /*
     * 「やること」の削除処理
     */
    private void deleteTaskData( TaskTableDao dao ){
        //Pidを取得
        int pid = dao.getPid( mNewTaskName, mNewTaskTime);

        //削除
        dao.deleteByPid( pid );
    }

    @Override
    protected void onPostExecute(Integer code) {
        //super.onPostExecute(code);

        //リスナーを実装していれば、成功後の処理を行う
        if (mListener != null) {

            if( mOperation == DB_OPERATION.READ ){
                //処理終了：読み込み
                mListener.onSuccessTaskRead(mTaskList);

            } else if( mOperation == DB_OPERATION.CREATE ){
                //処理終了：新規作成
                mListener.onSuccessTaskCreate(code, mTaskTable);

            } else if( mOperation == DB_OPERATION.DELETE ){
                //処理終了：削除
                mListener.onSuccessTaskDelete(mNewTaskName, mNewTaskTime);

            } else if( mOperation == DB_OPERATION.UPDATE ){
                //処理終了：更新
                mListener.onSuccessEditTask(mPreTask, mPreTaskTime, mTaskTable);

            } else {
                //do nothing
            }
        }
    }

    /*
     * インターフェース（リスナー）の設定
     */
    void setListener(TaskOperationListener listener) {
        //リスナー設定
        mListener = listener;
    }

    /*
     * 処理結果通知用のインターフェース
     */
    public interface TaskOperationListener {

        /*
         * 取得完了時
         */
        void onSuccessTaskRead(List<TaskTable> taskList );

        /*
         * 新規生成完了時
         */
        void onSuccessTaskCreate(Integer code, TaskTable taskTable );

        /*
         * 削除完了時
         */
        void onSuccessTaskDelete(String task, int taskTime);

        /*
         * 更新完了時
         */
        void onSuccessEditTask(String preTask, int preTaskTime, TaskTable updatedtask);

    }
}
