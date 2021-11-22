package com.stacktime.supportpreparation;

import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

/*
 * 非同期-DBアクセス-グループ
 */
public class AsyncGroupTableOperaion extends AsyncTask<Void, Void, Integer> {

    //DB操作種別
    public enum DB_OPERATION {
        CREATE,         //生成
        READ,           //参照
        UPDATE,         //更新
        DELETE,         //削除
        ADD_TASK,       //やることを追加
        REMOVE_TASK;    //やることを削除
    }

    //戻り値
    public static final Integer NORMAL     = 0;
    public static final Integer REGISTERED = -1;

    //フィールド変数
    private final AppDatabase           mDB;
    private final DB_OPERATION          mOperation;
    private String                      mPreGroupName;
    private int                         mGroupPid;
    private String                      mGroupName;
    private String                      mNewTaskPidsStr;
    private GroupTable                  mNewGroupTable;
    private GroupArrayList<GroupTable>  mGroupList  = new GroupArrayList<>();
    private int                         mAddTaskPid;
    private int                         mDeleteTaskPos;
    private GroupOperationListener      mListener;

    /*
     * コンストラクタ
     *   表示
     */
    public AsyncGroupTableOperaion(AppDatabase db, GroupOperationListener listener, DB_OPERATION operation){
        mDB = db;
        mListener = listener;
        mOperation = operation;
    }

    /*
     * コンストラクタ
     *   生成
     */
    public AsyncGroupTableOperaion(AppDatabase db, GroupOperationListener listener, DB_OPERATION operation, String groupName){
        mDB = db;
        mListener = listener;
        mOperation = operation;
        mGroupName = groupName;
    }

    /*
     * コンストラクタ
     *   削除
     */
    public AsyncGroupTableOperaion(AppDatabase db, GroupOperationListener listener, DB_OPERATION operation, int gPid){
        mDB        = db;
        mListener  = listener;
        mOperation = operation;
        mGroupPid  = gPid;
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

        Integer ret = NORMAL;

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
            ret = editGroupName(groupTableDao);

        } else if(mOperation == DB_OPERATION.DELETE ){
            //削除
            deleteGroup(groupTableDao);

        } else if(mOperation == DB_OPERATION.ADD_TASK || mOperation == DB_OPERATION.REMOVE_TASK ){
            //やること追加・削除
            updateTaskInGroup(groupTableDao);
        }

        return ret;
    }

    /*
     * 「グループ」の生成処理
     */
    private Integer createGroup(GroupTableDao dao ){

        //プライマリーキー取得
        int pid = dao.getPid( mGroupName);
        if( pid > 0 ){
            //すでに登録済みであれば、DBには追加しない
            return REGISTERED;
        }

        //DBに追加
        mNewGroupTable = new GroupTable( mGroupName );
        dao.insert(mNewGroupTable);

        //今追加した「やること」のPIDを新規作成データとして設定
        pid = dao.getPid(mGroupName);
        mNewGroupTable.setId(pid);

        //正常終了
        return NORMAL;
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

            Log.i("test", "readGroup groupName=" + groupInfo.getGroupName() + " tasksStr=" + tasksStr);

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
                        Log.i("failsafe", "group task is null. pid=" + pid);

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
     * 「やることグループ」の編集処理
     */
    private Integer editGroupName(GroupTableDao dao ){

        //プライマリーキー取得
        int pid = dao.getPid( mGroupName);
        if( pid > 0 ){
            //すでに登録済みであれば、DBには追加しない
            return REGISTERED;
        }

        //更新対象のPidを取得
        pid = dao.getPid( mPreGroupName);

        //更新
        dao.updateGroupNameByPid( pid, mGroupName);

        return NORMAL;
    }

    /*
     * 「やることグループ」の削除処理
     */
    private void deleteGroup(GroupTableDao dao ){
        //Pidを取得
        //int pid = dao.getPid( mGroupName);

        //削除
        dao.deleteByPid( mGroupPid );
    }

    /*
     * 「やることグループ」の追加処理
     */
    private Integer updateTaskInGroup(GroupTableDao dao ){

        //選択済みの「やること」を取得
        String taskPidsStr = dao.getTaskPidsStr(mGroupPid);
        if( taskPidsStr == null ){
            //--フェールセーフ
            //存在しないグループPIDが指定された場合
            Log.i("failsafe", "error getTaskPidsStr mGroupPid=" + mGroupPid);
            return -1;
        }

        if( mOperation == DB_OPERATION.ADD_TASK ){
            //「やること」Pidを文字列に追加（重複は許容する）
            mNewTaskPidsStr = TaskTableManager.addTaskPidsStrDuplicate(taskPidsStr, mAddTaskPid);

        } else {
            //「やること」Pidを文字列から削除
            mNewTaskPidsStr = TaskTableManager.deleteTaskPosInStr(taskPidsStr, mDeleteTaskPos);

        }

        Log.i("test", "updateTaskInGroup mNewTaskPidsStr=" + mNewTaskPidsStr);

        //選択済みの「やること」を更新
        dao.updateTaskPidsStrByPid(mGroupPid, mNewTaskPidsStr);

        //正常終了
        return 0;
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
                mListener.onSuccessReadGroup(mGroupList);

            } else if( mOperation == DB_OPERATION.CREATE ){
                //処理終了：新規作成
                mListener.onSuccessCreateGroup(code, mNewGroupTable);

            } else if( mOperation == DB_OPERATION.DELETE ){
                //処理終了：削除
                mListener.onSuccessDeleteGroup(mGroupName);

            } else if( mOperation == DB_OPERATION.UPDATE ){
                //処理終了：更新
                mListener.onSuccessEditGroup(code, mPreGroupName, mGroupName);

            } else if( mOperation == DB_OPERATION.ADD_TASK || mOperation == DB_OPERATION.REMOVE_TASK){
                //処理終了：やること
                mListener.onSuccessUpdateTask(mGroupPid, mNewTaskPidsStr);

            }
        }
    }

    /*
     * 処理結果通知用のインターフェース
     */
    public interface GroupOperationListener {

        /*
         * 取得完了時
         */
        void onSuccessReadGroup(GroupArrayList<GroupTable> groupList);

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
        void onSuccessEditGroup(Integer code, String preTask, String groupName);

        /*
         * やること追加／削除完了時
         */
        void onSuccessUpdateTask(int groupPid, String taskPidsStr);

    }
}
