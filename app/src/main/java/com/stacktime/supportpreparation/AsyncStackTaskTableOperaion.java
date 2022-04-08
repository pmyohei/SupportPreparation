package com.stacktime.supportpreparation;

import android.os.AsyncTask;

import java.util.List;

/*
 * 非同期-DBアクセス-スタックされたやること
 */
public class AsyncStackTaskTableOperaion extends AsyncTask<Void, Void, Integer> {

    //DB操作種別
    public enum DB_OPERATION {
        CREATE,         //生成(or再生成)
        DELETE,         //削除
        UPDATE,         //更新
    }

    public final static int                     READ_NORMAL = 0;    //正常

    //フィールド変数
    private final AppDatabase                   mDB;                //DB
    private final StackTaskTableDao             mStackDao;          //DAO
    private final DB_OPERATION                  mOperation;         //DB操作種別
    private final StackTaskOperationListener    mListener;          //スタックテーブル操作リスナー
    private StackTaskTable                      mStackTable;        //スタック情報用
    private StackTaskTable                      mAlarmStack;        //アラーム情報用

    /*
     * コンストラクタ
     *   生成・更新
     */
    public AsyncStackTaskTableOperaion(AppDatabase db, StackTaskOperationListener listener, DB_OPERATION operation, StackTaskTable stackTable){
        mDB         = db;
        mListener   = listener;
        mOperation  = operation;
        mStackTable = stackTable;
        mStackDao   = mDB.stackTaskTableDao();
    }

    @Override
    protected Integer doInBackground(Void... params) {

        Integer ret = 0;
        
        //--操作種別に応じた処理
        if( mOperation == DB_OPERATION.CREATE  ){
            //登録
            ret = createStackData();

        } else if(mOperation == DB_OPERATION.DELETE ){
            //削除
            deleteStackData();

        }

        return ret;
    }

    /*
     * 「積み上げやること」の生成処理
     */
    private Integer createStackData(){

        TaskArrayList<TaskTable> stackTaskList = mStackTable.getStackTaskList();
        List<Boolean>            alarmOnOffList = mStackTable.getAlarmOnOffList();

        //「積み上げやること」の文字列を生成
        String taskPidsStr = TaskTableManager.getPidsStr(stackTaskList);
        mStackTable.setTaskPidsStr( taskPidsStr );

        //アラームOn/Off文字列
        String alarmStr = TaskTableManager.getAlarmStr( alarmOnOffList );
        mStackTable.setAlarmOnOffStr( alarmStr );

        //プライマリーキー取得
        int pid = mStackDao.getPid( mStackTable.isStack() );
        if( pid != 0 ){
            //登録ありなら、更新
            mStackDao.update(pid,
                    mStackTable.getTaskPidsStr(),
                    mStackTable.getAlarmOnOffStr(),
                    mStackTable.getDate(),
                    mStackTable.getTime(),
                    mStackTable.isLimit(),
                    mStackTable.isStack(),
                    mStackTable.isOnAlarm() );

        } else {
            //登録ないなら、新規登録
            mStackDao.insert( mStackTable );
        }

        //正常終了
        return 0;
    }

    /*
     * 「積み上げやること」の削除処理
     */
    private void deleteStackData(){
        //全レコード削除
        mStackDao.deleteAll();
    }

    /*
     * doInBackground()にコールされる
     */
    @Override
    protected void onPostExecute(Integer code) {
        //super.onPostExecute(code);

        //リスナーを実装していれば、処理に対応する後処理を行う
        if (mListener != null) {

            if( mOperation == DB_OPERATION.CREATE ){
                //処理終了：新規作成
                mListener.onSuccessStackCreate();

            } else if( mOperation == DB_OPERATION.DELETE ){
                //処理終了：削除
                mListener.onSuccessStackDelete();

            }
        }
    }

    /*
     * 処理結果通知用のインターフェース
     */
    public interface StackTaskOperationListener {
        /*
         * 新規生成完了時
         */
        void onSuccessStackCreate( );
        /*
         * 削除完了時
         */
        void onSuccessStackDelete();
    }
}
