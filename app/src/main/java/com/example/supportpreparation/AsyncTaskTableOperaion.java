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

    private AppDatabase             db;
    private DB_OPERATION            operation;
    private String                  preTask;
    private int                     preTaskTime;
    private String                  task;
    private int                     taskTime;
    private TaskTable               taskTable;
    private List<TaskTable>         taskList;
    private TaskOperationListener   listener;

    /*
     * コンストラクタ
     *   表示
     */
    public AsyncTaskTableOperaion(AppDatabase db, TaskOperationListener listener, DB_OPERATION operation){
        this.db        = db;
        this.listener  = listener;
        this.operation = operation;
    }

    /*
     * コンストラクタ
     *   生成・削除
     */
    public AsyncTaskTableOperaion(AppDatabase db, TaskOperationListener listener, DB_OPERATION operation, String task, int taskTime){
        this.db        = db;
        this.listener  = listener;
        this.operation = operation;
        this.task      = task;
        this.taskTime  = taskTime;
    }

    /*
     * コンストラクタ
     *   更新
     */
    public AsyncTaskTableOperaion(AppDatabase db, TaskOperationListener listener, DB_OPERATION operation, String preTask, int preTaskTime, String task, int taskTime){
        this.db             = db;
        this.listener       = listener;
        this.operation      = operation;
        this.preTask        = preTask;
        this.preTaskTime    = preTaskTime;
        this.task           = task;
        this.taskTime       = taskTime;
    }

    @Override
    protected Integer doInBackground(Void... params) {

        Integer ret = 0;

        TaskTableDao taskTableDao = db.taskTableDao();

        //--操作種別に応じた処理
        if(this.operation == DB_OPERATION.CREATE){
            //登録
            ret = this.createTaskData(taskTableDao);

        } else if(this.operation == DB_OPERATION.READ ){
            //表示
            this.displayTaskData(taskTableDao);

        } else if(this.operation == DB_OPERATION.UPDATE ){
            //編集
            this.updateTaskData(taskTableDao);

        } else if(this.operation == DB_OPERATION.DELETE ){
            //削除
            this.deleteTaskData(taskTableDao);

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
        int pid = dao.getPid( this.task, this.taskTime );
        Log.i("test", "pid=" + pid);
        if( pid > 0 ){
            //すでに登録済みであれば、DBには追加しない
            return -1;
        }

        //DBに追加
        this.taskTable = new TaskTable( this.task, this.taskTime );
        dao.insert( this.taskTable );
        //正常終了
        return 0;
    }

    /*
     * 「やること」の表示処理
     */
    private void displayTaskData( TaskTableDao dao ){

        //DBから、保存済みのタスクリストを取得
        this.taskList = dao.getAll();
    }

    /*
     * 「やること」の編集処理
     */
    private void updateTaskData( TaskTableDao dao ){
        //更新対象のPidを取得
        int pid = dao.getPid( this.preTask, this.preTaskTime );

        //更新
        dao.updateByPid( pid, this.task, this.taskTime );

        //更新したレコードを取得
        this.taskTable = dao.getRecord( pid );
    }

    /*
     * 「やること」の削除処理
     */
    private void deleteTaskData( TaskTableDao dao ){
        //Pidを取得
        int pid = dao.getPid( this.task, this.taskTime );

        //削除
        dao.deleteByPid( pid );
    }

    @Override
    protected void onPostExecute(Integer code) {
        //super.onPostExecute(code);

        //リスナーを実装していれば、成功後の処理を行う
        if (listener != null) {

            if( this.operation == DB_OPERATION.READ ){
                //処理終了：読み込み
                listener.onSuccessTaskRead(this.taskList);

            } else if( this.operation == DB_OPERATION.CREATE ){
                //処理終了：新規作成
                listener.onSuccessTaskCreate(code, this.taskTable);

            } else if( this.operation == DB_OPERATION.DELETE ){
                //処理終了：削除
                listener.onSuccessTaskDelete(this.task, this.taskTime);

            } else if( this.operation == DB_OPERATION.UPDATE ){
                //処理終了：更新
                listener.onSuccessTaskUpdate(this.preTask, this.preTaskTime, this.taskTable);

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
        this.listener = listener;
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
        void onSuccessTaskUpdate(String preTask, int preTaskTime, TaskTable updatedtask);

    }
}
