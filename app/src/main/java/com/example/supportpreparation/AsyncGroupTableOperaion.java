package com.example.supportpreparation;

import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/*
 * 非同期-DBアクセス-やることグループ
 */
public class AsyncGroupTableOperaion extends AsyncTask<Void, Void, Integer> {

    //-- DB操作種別
    public enum DB_OPERATION {
        CREATE,         //生成
        READ,           //参照
        UPDATE,         //更新
        DELETE,         //削除
        ADD_TASK,       //やることを追加
        DELETE_TASK;    //やることを削除
    }

    private AppDatabase             mDB;
    private DB_OPERATION            mOperation;
    private String                  mPreGroupName;
    private int                     mGroupPid;
    private String                  mGroupName;
    private GroupTable mNewGroupTable;
    private List<GroupTable>        mGroupList;
    private List<List<TaskTable>>   mTaskListInGroup;
    private int                     mSelectedTaskPid;
    private int                     mAddTaskPid;
    private int                     mDeleteTaskPos;
    private GroupOperationListener  mListener;

    /*
     * コンストラクタ
     *   表示
     */
    public AsyncGroupTableOperaion(AppDatabase db, GroupOperationListener listener, DB_OPERATION operation){
        mDB = db;
        mListener = listener;
        mOperation = operation;

        mTaskListInGroup = new ArrayList<>();
    }

    /*
     * コンストラクタ
     *   生成・削除
     */
    public AsyncGroupTableOperaion(AppDatabase db, GroupOperationListener listener, DB_OPERATION operation, String groupName){
        mDB = db;
        mListener = listener;
        mOperation = operation;
        mGroupName = groupName;
    }

    /*
     * コンストラクタ
     *   更新
     */
    public AsyncGroupTableOperaion(AppDatabase db, GroupOperationListener listener, DB_OPERATION operation, String preGroupName, String groupName){
        mDB = db;
        mListener = listener;
        mOperation = operation;
        mPreGroupName = preGroupName;
        mGroupName = groupName;
    }

    /*
     * コンストラクタ
     *   「やること」追加／削除
     */
    public AsyncGroupTableOperaion(AppDatabase db, GroupOperationListener listener, DB_OPERATION operation, int groupPid, int taskNum){
        mDB         = db;
        mListener   = listener;
        mOperation  = operation;
        mGroupPid   = groupPid;

        if( operation == DB_OPERATION.ADD_TASK ){
            mAddTaskPid = taskNum;
        }else {
            mDeleteTaskPos = taskNum;
        }
    }

    @Override
    protected Integer doInBackground(Void... params) {

        Integer ret = 0;

        GroupTableDao groupTableDao = mDB.groupTableDao();

        //--操作種別に応じた処理
        if(mOperation == DB_OPERATION.CREATE){
            //登録
            ret = createGroup(groupTableDao);

        } else if(mOperation == DB_OPERATION.READ ){
            //表示
            readGroup(groupTableDao);

        } else if(mOperation == DB_OPERATION.UPDATE ){
            //編集
            editGroupName(groupTableDao);

        } else if(mOperation == DB_OPERATION.DELETE ){
            //削除
            deleteGroup(groupTableDao);

        } else if(mOperation == DB_OPERATION.ADD_TASK ){
            //やること追加
            addTaskToGroup(groupTableDao);

        } else if(mOperation == DB_OPERATION.DELETE_TASK ){
            //やること削除
            deleteTaskInGroup(groupTableDao);

        } else{
            //do nothing
        }

        return ret;
    }

    /*
     * 「やること」の生成処理
     */
    private Integer createGroup(GroupTableDao dao ){

        //プライマリーキー取得
        int pid = dao.getPid( mGroupName);
        if( pid > 0 ){
            //すでに登録済みであれば、DBには追加しない
            return -1;
        }

        //DBに追加
        mNewGroupTable = new GroupTable( mGroupName);
        dao.insert(mNewGroupTable);

        //今追加した「やること」のPIDを新規作成データとして設定
        pid = dao.getPid(mGroupName);
        mNewGroupTable.setId(pid);

        //正常終了
        return 0;
    }

    /*
     * 「やることグループ」の読み込み
     */
    private void readGroup(GroupTableDao dao ){

        TaskTableDao taskTableDao = mDB.taskTableDao();

        //DBから、保存済みのグループリストを取得
        mGroupList = dao.getAll();

        //-- 各グループの「選択済みやること」をリスト化する
        //グループ分ループ
        for( GroupTable groupInfo: mGroupList){

            //グループに紐づいた「やること」pidを取得
            String tasksStr = groupInfo.getTaskPidsStr();
            List<Integer> pids = TaskTableManager.getPidsIntArray(tasksStr);

            //グループに紐づいた「やること」
            List<TaskTable> tasks = new ArrayList<>();

            //Pidあれば
            if( pids != null ) {
                //pid分繰り返し
                for( Integer pid: pids ){
                    Log.i("test", "displaySetData pid loop");
                    //pidに対応する「やること」を取得し、リストに追加
                    TaskTable task = taskTableDao.getRecord(pid);
                    if( task != null ){
                        //フェールセーフ
                        //該当する「やること」がない場合
                        Log.i("failsafe", "group task is null. pid=" + pid);

                        //グループ内「やること」リストに追加
                        tasks.add(task);
                    }
                }
            }

            //「やること」を追加
            mTaskListInGroup.add(tasks);
        }
    }

    /*
     * 「やることグループ」の編集処理
     */
    private void editGroupName(GroupTableDao dao ){
        //更新対象のPidを取得
        int pid = dao.getPid( mPreGroupName);

        //更新
        dao.updateGroupNameByPid( pid, mGroupName);
    }

    /*
     * 「やることグループ」の削除処理
     */
    private void deleteGroup(GroupTableDao dao ){
        //Pidを取得
        int pid = dao.getPid( mGroupName);

        //削除
        dao.deleteByPid( pid );
    }

    /*
     * 「やることグループ」の追加処理
     */
    private Integer addTaskToGroup(GroupTableDao dao ){

        //選択済みの「やること」を取得
        String taskPidsStr = dao.getTaskPidsStr(mGroupPid);
        if( taskPidsStr == null ){
            //フェールセーフ
            //存在しないグループPIDが指定された場合
            Log.i("failsafe", "error getTaskPidsStr mGroupPid=" + mGroupPid);
        }

        //「やること」Pidを文字列に追加（重複は許容する）
        taskPidsStr = TaskTableManager.addTaskPidsStrDuplicate(taskPidsStr, mAddTaskPid);

        //選択済みの「やること」を更新
        dao.updateTaskPidsStrByPid(mGroupPid, taskPidsStr);

        Log.i("test", "taskPidsStr=" + taskPidsStr);

        //正常終了
        return 0;
    }

    /*
     * 「やることグループ」の削除
     */
    private void deleteTaskInGroup(GroupTableDao dao ) {

        //選択済みの「やること」を取得
        String taskPidsStr = dao.getTaskPidsStr(mGroupPid);

        //「やること」Pidを文字列から削除
        taskPidsStr = TaskTableManager.deleteTaskPosInStr(taskPidsStr, mDeleteTaskPos);

        //選択済みの「やること」を更新
        dao.updateTaskPidsStrByPid(mGroupPid, taskPidsStr);
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
                mListener.onSuccessReadGroup(mGroupList, mTaskListInGroup);

            } else if( mOperation == DB_OPERATION.CREATE ){
                //処理終了：新規作成
                mListener.onSuccessCreateGroup(code, mNewGroupTable);

            } else if( mOperation == DB_OPERATION.DELETE ){
                //処理終了：削除
                mListener.onSuccessDeleteGroup(mGroupName);

            } else if( mOperation == DB_OPERATION.UPDATE ){
                //処理終了：更新
                mListener.onSuccessEditGroup(mPreGroupName, mGroupName);

            } else {
                //do nothing
            }
        }
    }

    /*
     * インターフェース（リスナー）の設定
     */
    void setListener(GroupOperationListener listener) {
        //リスナー設定
        mListener = listener;
    }

    /*
     * 処理結果通知用のインターフェース
     */
    public interface GroupOperationListener {

        /*
         * 取得完了時
         */
        void onSuccessReadGroup(List<GroupTable> groupList, List<List<TaskTable>> tasksList );

        /*
         * 新規生成完了時
         */
        void onSuccessCreateGroup(Integer code, GroupTable group );

        /*
         * 削除完了時
         */
        void onSuccessDeleteGroup(String task);

        /*
         * 更新完了時
         */
        void onSuccessEditGroup(String preTask, String groupName);

    }
}
