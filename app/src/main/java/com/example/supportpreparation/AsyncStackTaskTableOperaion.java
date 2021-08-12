package com.example.supportpreparation;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
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
    }

    private AppDatabase                 mDB;
    private StackTaskTableDao           mStackDao;
    private DB_OPERATION                mOperation;
    private StackTaskOperationListener  mListener;

    //-- DBからの読み込みデータ
    private StackTaskTable              mReadStackTask;     //
    private List<TaskTable>             mReadTaskList;      //※読み込んだデータを元に変換した「やること」リスト

    //-- DB登録対象のデータ
    private List<TaskTable>             mTaskList;          //「積み上げやること」のリスト
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
     *   生成
     */
    public AsyncStackTaskTableOperaion(AppDatabase db, StackTaskOperationListener listener, DB_OPERATION operation, List<TaskTable> taskList, String date, String time){
        mDB         = db;
        mListener   = listener;
        mOperation  = operation;
        mTaskList   = taskList;
        mDate       = date;
        mTime       = time;

        mStackDao = mDB.stackTaskTableDao();
    }
    
    @Override
    protected Integer doInBackground(Void... params) {

        Integer ret = 0;
        
        //--操作種別に応じた処理
        if(mOperation == DB_OPERATION.CREATE){
            //登録
            ret = createStackData();

        } else if(mOperation == DB_OPERATION.READ ){
            //読込
            readStackData();

        } else if(mOperation == DB_OPERATION.DELETE ){
            //削除
            deleteStackData();

        } else{
            //do nothing
        }

        return ret;
    }

    /*
     * 「積み上げやること」の生成処理
     */
    private Integer createStackData(){

        //全レコード削除
        mStackDao.deleteAll();

        //「やること」のPIDリストを生成
        List<Integer> pidList = new ArrayList<>();
        for( TaskTable task: mTaskList ){
            pidList.add(task.getId());
        }

        //「積み上げやること」の文字列を生成
        String taskPidsStr = TaskTableManager.getPidsStr(pidList);

        //DBに追加
        mStackDao.insert( new StackTaskTable( taskPidsStr, mDate, mTime ) );
        //正常終了
        return 0;
    }

    /*
     * 「積み上げやること」の読込処理
     */
    private void readStackData(){

        List<StackTaskTable> stackTaskList = mStackDao.getAll();
        Log.i("test", "pre size stackTaskList=");
        if( stackTaskList.size() == 0 ){
            //ないなら、終了
            Log.i("test", "size0 stackTaskList=");
            return;
        }

        //登録中の「積み上げやること」
        mReadStackTask = stackTaskList.get(0);

        //対象の「やること」を取得
        String str = mReadStackTask.getTaskPidsStr();
        List<Integer> pids = TaskTableManager.getPidsIntArray(str);

        //「やること」テーブル操作用DAO
        TaskTableDao taskTableDao = mDB.taskTableDao();

        //「やること」をリスト化する
        mReadTaskList = new ArrayList<>();
        for( Integer pid: pids ){
            //pidに対応する「やること」を取得し、リストに追加
            TaskTable task = taskTableDao.getRecord(pid);
            mReadTaskList.add(task);
        }
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
                mListener.onSuccessStackRead(mReadStackTask, mReadTaskList);

            } else if( mOperation == DB_OPERATION.CREATE ){
                //処理終了：新規作成
                mListener.onSuccessStackCreate();

            } else if( mOperation == DB_OPERATION.DELETE ){
                //処理終了：削除
                mListener.onSuccessStackDelete();

            } else {
                //do nothing
            }
        }
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
         * 取得完了時
         */
        void onSuccessStackRead( StackTaskTable stack, List<TaskTable> taskList );

        /*
         * 削除完了時
         */
        void onSuccessStackDelete();

    }
}