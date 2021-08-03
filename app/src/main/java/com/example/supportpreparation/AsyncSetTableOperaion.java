package com.example.supportpreparation;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/*
 * 非同期-DBアクセス-やることセット
 */
public class AsyncSetTableOperaion extends AsyncTask<Void, Void, Integer> {

    //-- DB操作種別
    public enum DB_OPERATION {
        CREATE,         //生成
        READ,           //参照
        UPDATE,         //更新
        DELETE,         //削除
        ADD_TASK,       //やることを追加
        DELETE_TASK;    //やることを削除
    }

    private AppDatabase                 db;
    private DB_OPERATION                operation;
    private String                      preSetName;
    private String                      setName;
    private List<SetTable>              setList;
    private List<List<TaskTable>>       tasksList;
    private int                         selectedTaskPid;
    private SetOperationListener        listener;

    /*
     * コンストラクタ
     *   表示
     */
    public AsyncSetTableOperaion(AppDatabase db, SetOperationListener listener, DB_OPERATION operation){
        this.db        = db;
        this.listener  = listener;
        this.operation = operation;

        this.tasksList = new ArrayList<>();
    }

    /*
     * コンストラクタ
     *   生成・削除
     */
    public AsyncSetTableOperaion(AppDatabase db, SetOperationListener listener, DB_OPERATION operation, String setName){
        this.db         = db;
        this.listener   = listener;
        this.operation  = operation;
        this.setName    = setName;
    }

    /*
     * コンストラクタ
     *   更新
     */
    public AsyncSetTableOperaion(AppDatabase db, SetOperationListener listener, DB_OPERATION operation, String preSetName, String setName){
        this.db             = db;
        this.listener       = listener;
        this.operation      = operation;
        this.preSetName     = preSetName;
        this.setName        = setName;
    }

    @Override
    protected Integer doInBackground(Void... params) {

        Integer ret = 0;

        SetTableDao setTableDao   = db.setTableDao();

        //--操作種別に応じた処理
        if(this.operation == DB_OPERATION.CREATE){
            //登録
            ret = this.createSetData(setTableDao);

        } else if(this.operation == DB_OPERATION.READ ){
            //表示
            this.displaySetData(setTableDao);

        } else if(this.operation == DB_OPERATION.UPDATE ){
            //編集
            this.updateSetData(setTableDao);

        } else if(this.operation == DB_OPERATION.DELETE ){
            //削除
            this.deleteSetData(setTableDao);

        } else if(this.operation == DB_OPERATION.ADD_TASK ){
            //やること追加
            this.addTaskToSet(setTableDao);

        } else if(this.operation == DB_OPERATION.DELETE_TASK ){
            //やること削除
            this.deleteTaskInSet(setTableDao);

        } else{
            //do nothing
        }

        return ret;
    }

    /*
     * 「やること」の生成処理
     */
    private Integer createSetData(SetTableDao dao ){

        //プライマリーキー取得
        int pid = dao.getPid( this.setName);

        if( pid > 0 ){
            //すでに登録済みであれば、DBには追加しない
            return -1;
        }

        //DBに追加
        dao.insert( new SetTable( this.setName) );
        //正常終了
        return 0;
    }

    /*
     * 「やることセット」の表示処理
     */
    private void displaySetData(SetTableDao dao ){

        TaskTableDao taskTableDao = this.db.taskTableDao();

        //DBから、保存済みのセットリストを取得
        this.setList = dao.getAll();

        //-- 各セットの「選択済みやること」をリスト化する
        //セット分ループ
        for( SetTable setInfo: this.setList ){

            Log.i("test", "displaySetData setName=" + setInfo.getSetName());

            //セットに紐づいた「やること」pidを取得
            String tasksStr = setInfo.getTaskPidsStr();
            List<Integer> pids = SetTable.getPidsIntArray(tasksStr);

            //セットに紐づいた「やること」
            List<TaskTable> tasks = new ArrayList<>();

            Log.i("test", "displaySetData tasksStr=" + tasksStr);
            //Pidあれば
            if( pids != null ) {
                Log.i("test", "pids");
                //pid分繰り返し
                for( Integer pid: pids ){
                    Log.i("test", "displaySetData pid loop");
                    //pidに対応する「やること」を取得し、リストに追加
                    TaskTable task = taskTableDao.getRecord(pid);
                    tasks.add(task);
                }
            }

            Log.i("test", "pre add");
            //「やること」を追加
            this.tasksList.add(tasks);
            Log.i("test", "after add");
        }
    }

    /*
     * 「やることセット」の編集処理
     */
    private void updateSetData(SetTableDao dao ){
        //更新対象のPidを取得
        int pid = dao.getPid( this.preSetName);

        //更新
        dao.updateSetNameByPid( pid, this.setName);
    }

    /*
     * 「やることセット」の削除処理
     */
    private void deleteSetData(SetTableDao dao ){
        //Pidを取得
        int pid = dao.getPid( this.setName);

        //削除
        dao.deleteByPid( pid );
    }

    /*
     * 「やることセット」の追加処理
     */
    private Integer addTaskToSet(SetTableDao dao ){

        //追加先セットのPidを取得
        int setPid = dao.getPid( this.setName);

        //選択済みの「やること」を取得
        String taskPidsStr = dao.getTaskPidsStr(setPid);

        //「やること」Pidを文字列に追加
        taskPidsStr = SetTable.addTaskPidsStr(taskPidsStr, this.selectedTaskPid);
        if( taskPidsStr == null ){
            //既に追加済みなら、何もせず終了
            return -1;
        }

        //選択済みの「やること」を更新
        dao.updateTaskPidsStrByPid(setPid, taskPidsStr);

        //正常終了
        return 0;
    }

    /*
     * 「やることセット」の削除
     */
    private void deleteTaskInSet(SetTableDao dao ) {

        //選択済みの「やること」を取得
        int setPid = dao.getPid( this.setName);
        String taskPidsStr = dao.getTaskPidsStr(setPid);

        //「やること」Pidを文字列から削除
        taskPidsStr = SetTable.deleteTaskPidInStr(taskPidsStr, this.selectedTaskPid);

        //選択済みの「やること」を更新
        dao.updateTaskPidsStrByPid(setPid, taskPidsStr);
    }

    /*
     * doInBackground()にコールされる
     */
    @Override
    protected void onPostExecute(Integer code) {
        //super.onPostExecute(code);

        //リスナーを実装していれば、処理に対応する後処理を行う
        if (listener != null) {

            if( this.operation == DB_OPERATION.READ ){
                //処理終了：読み込み
                listener.onSuccessSetRead(this.setList, this.tasksList);

            } else if( this.operation == DB_OPERATION.CREATE ){
                //処理終了：新規作成
                listener.onSuccessSetCreate(code, this.setName);

            } else if( this.operation == DB_OPERATION.DELETE ){
                //処理終了：削除
                listener.onSuccessSetDelete(this.setName);

            } else if( this.operation == DB_OPERATION.UPDATE ){
                //処理終了：更新
                listener.onSuccessSetUpdate(this.preSetName, this.setName);

            } else {
                //do nothing
            }
        }
    }

    /*
     * インターフェース（リスナー）の設定
     */
    void setListener(SetOperationListener listener) {
        //リスナー設定
        this.listener = listener;
    }

    /*
     * 処理結果通知用のインターフェース
     */
    public interface SetOperationListener {

        /*
         * 取得完了時
         */
        void onSuccessSetRead(List<SetTable> taskSetList, List<List<TaskTable>> tasksList );

        /*
         * 新規生成完了時
         */
        void onSuccessSetCreate(Integer code, String taskSet );

        /*
         * 削除完了時
         */
        void onSuccessSetDelete(String task);

        /*
         * 更新完了時
         */
        void onSuccessSetUpdate(String preTask, String task);

    }
}
