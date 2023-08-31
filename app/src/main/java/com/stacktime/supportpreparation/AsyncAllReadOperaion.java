package com.stacktime.supportpreparation;

import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

/*
 * 非同期-DBアクセス-やること
 */
public class AsyncAllReadOperaion extends AsyncTask<Void, Void, Integer> {

    //フィールド変数
    private final AppDatabase mDB;
    private final TaskArrayList<TaskTable> mTaskList = new TaskArrayList<>();
    private final GroupArrayList<GroupTable> mGroupList = new GroupArrayList<>();
    private final AsyncAllReadOperaionListener mListener;
    private StackTaskTable mStackTable;        //スタック情報用
    private StackTaskTable mAlarmStack;        //アラーム情報用

    /*
     * コンストラクタ
     *   表示
     */
    public AsyncAllReadOperaion(AppDatabase db, AsyncAllReadOperaionListener listener){
        mDB         = db;
        mListener   = listener;
    }


    @Override
    protected Integer doInBackground(Void... params) {

        //保存済みのタスクリストを取得
        TaskTableDao taskTableDao = mDB.taskTableDao();
        readTaskData( taskTableDao );

        //保存済みのグループリストを取得
        GroupTableDao groupTableDao = mDB.groupTableDao();
        readGroup( groupTableDao );

        //保存済みのスタックデータを取得
        StackTaskTableDao stackTaskTableDao = mDB.stackTaskTableDao();
        readStackData( stackTaskTableDao );

        return 0;
    }

    /*
     * 「やること」の表示処理
     */
    private void readTaskData(TaskTableDao dao ){

        //DBから、保存済みのタスクリストを取得
        List<TaskTable> taskList = dao.getAll();
        //やることリスト用クラスのインスタンスに格納
        mTaskList.addAll(taskList);
    }

    /*
     * 「やることグループ」の読み込み
     */
    private void readGroup(GroupTableDao dao ){

        TaskTableDao taskTableDao = mDB.taskTableDao();

        //DBから、保存済みのグループリストを取得
        List<GroupTable> groupList = dao.getAll();

        //-- 各グループの「選択済みやること」をリスト化する
        //グループ分ループ
        for( GroupTable groupInfo: groupList){

            //グループに紐づいた「やること」pidを取得
            String tasksStr = groupInfo.getTaskPidsStr();
            List<Integer> pids = TaskTableManager.convertIntArray(tasksStr);

            //Log.i("test", "readGroup groupName=" + groupInfo.getGroupName() + " tasksStr=" + tasksStr);

            //グループに紐づいた「やること」
            TaskArrayList<TaskTable> tasks = new TaskArrayList<>();

            //やることがあれば
            if( pids != null ) {
                //pid数分
                for( Integer pid: pids ){
                    //pidに対応する「やること」を取得し、リストに追加
                    TaskTable task = taskTableDao.getRecord(pid);
                    if( task != null ){
                        //--フェールセーフ
                        //該当する「やること」あり
                        //Log.i("failsafe", "group task is null. pid=" + pid);

                        //グループ内「やること」リストに追加
                        tasks.add(task);
                    }
                }
            }

            //GroupTable内のリストに保持
            groupInfo.setTaskInGroupList(tasks);
            //グループリスト用クラスのインスタンスに格納
            mGroupList.add( groupInfo );
        }
    }

    /*
     * 「積み上げやること」の読込処理
     */
    private Integer readStackData( StackTaskTableDao dao ){

        List<StackTaskTable> stackList = dao.getAll();

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
        return 0;
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

    @Override
    protected void onPostExecute(Integer code) {
        //super.onPostExecute(code);

        //リスナーを実装していれば、成功後の処理を行う
        if (mListener != null) {
            mListener.onRead(mTaskList, mGroupList, mStackTable, mAlarmStack);
        }
    }

    /*
     * 処理結果通知用のインターフェース
     */
    public interface AsyncAllReadOperaionListener {
        /*
         * 取得完了時
         */
        void onRead(
                TaskArrayList<TaskTable> taskList,
                GroupArrayList<GroupTable> groupList,
                StackTaskTable stack,
                StackTaskTable alarmStack );
    }
}
