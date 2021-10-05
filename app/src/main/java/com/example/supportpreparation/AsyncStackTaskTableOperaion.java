package com.example.supportpreparation;

import android.os.AsyncTask;

import java.util.List;

/*
 * 非同期-DBアクセス-やることセット
 */
public class AsyncStackTaskTableOperaion extends AsyncTask<Void, Void, Integer> {

    //-- DB操作種別
    public enum DB_OPERATION {
        CREATE,         //生成(or再生成)
        READ,           //参照
        DELETE,         //削除
        UPDATE,         //更新
    }

    public final static int                    READ_NONE   = -1;
    public final static int                    READ_NORMAL = 0;


    private AppDatabase                 mDB;
    private StackTaskTableDao           mStackDao;
    private DB_OPERATION                mOperation;
    private StackTaskOperationListener  mListener;

    private StackTaskTable              mStackTable;        //
    private StackTaskTable              mAlarmStack;        //

    //-- DB登録対象のデータ
    private TaskArrayList<TaskTable>    mTaskList;          //「積み上げやること」のリスト
    private String                      mDate;              //リミット-年月日
    private String                      mTime;              //リミット-時間

    /*
     * コンストラクタ
     *   読込・削除
     */
    public AsyncStackTaskTableOperaion(AppDatabase db, StackTaskOperationListener listener, DB_OPERATION operation){
        mDB         = db;
        mListener   = listener;
        mOperation  = operation;

        mStackDao = mDB.stackTaskTableDao();
    }

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

        } else if(mOperation == DB_OPERATION.READ ){
            //読込
            ret = readStackData();

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

        TaskArrayList<TaskTable> mStackTaskList = mStackTable.getStackTaskList();
        List<Boolean> AlarmOnOffList = mStackTable.getAlarmOnOffList();

        //「積み上げやること」の文字列を生成
        String taskPidsStr = TaskTableManager.getPidsStr(mStackTaskList);
        mStackTable.setTaskPidsStr( taskPidsStr );

        //アラームOn/Off文字列
        String alarmStr = TaskTableManager.getAlarmStr( AlarmOnOffList );
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
     * 「積み上げやること」の読込処理
     */
    private Integer readStackData(){

        List<StackTaskTable> stackList = mStackDao.getAll();

        for( StackTaskTable stack: stackList ){

            if( stack.isStack() ){
                //スタック情報の場合
                mStackTable = stack;

                //「やること」情報の生成
                setupTaskData( mStackTable );

            } else {
                //アラーム情報の場合
                mAlarmStack = stack;

                //「やること」情報の生成
                setupTaskData( mAlarmStack );
            }
        }

        if( mStackTable == null ){
            //ないなら、空のテーブルを返す
            mStackTable = new StackTaskTable( true );
        }

        if( mAlarmStack == null ){
            //ないなら、空のテーブルを返す
            mAlarmStack = new StackTaskTable( false );
        }

        //正常終了
        return READ_NORMAL;
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

            if( mOperation == DB_OPERATION.READ ){
                //処理終了：読み込み
                mListener.onSuccessStackRead(code, mStackTable, mAlarmStack);

            } else if( mOperation == DB_OPERATION.CREATE ){
                //処理終了：新規作成
                mListener.onSuccessStackCreate();

            } else if( mOperation == DB_OPERATION.DELETE ){
                //処理終了：削除
                mListener.onSuccessStackDelete();

            }
        }
    }

    /*
     * スタックテーブルに「やること」情報を設定
     */
    private void setupTaskData( StackTaskTable table ){

        //「やること」文字列
        String taskPidsStr = table.getTaskPidsStr();
        if( taskPidsStr.isEmpty() ){
            //ないなら、終了
            return;
        }

        //やることをint型リストに変換
        List<Integer> pids = TaskTableManager.convertIntArray(taskPidsStr);
        if( pids == null ){
            //フェールセーフ
            return;
        }

        //アラームOn/Off
        List<Boolean> alarmOnOffList = table.getAlarmOnOffList();
        String alarmStr = table.getAlarmOnOffStr();
        TaskTableManager.convertAlarmList(alarmStr, alarmOnOffList);

        //「やること」テーブル操作用DAO
        TaskTableDao taskTableDao = mDB.taskTableDao();

        TaskArrayList<TaskTable> stackTaskList = table.getStackTaskList();

        //「やること」をリスト化
        int i = 0;
        for( Integer pid: pids ){

            //pidに対応する「やること」を取得し、リストに追加
            TaskTable task = taskTableDao.getRecord(pid);
            if( task != null ){
                //フェールセーフ

                //アラーム
                task.setOnAlarm( alarmOnOffList.get(i) );

                //リストに追加
                stackTaskList.add(task);

                i++;
            }
        }

        //開始／終了時刻のカレンダーを全更新
        table.allUpdateStartEndTime();
    }


    /*
     * インターフェース（リスナー）の設定
     */
    void setmListener(StackTaskOperationListener mListener) {
        //リスナー設定
        mListener = mListener;
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
         * 読み込み完了時
         */
        void onSuccessStackRead( Integer code, StackTaskTable stack, StackTaskTable alarmStack);

        /*
         * 削除完了時
         */
        void onSuccessStackDelete();

    }
}
