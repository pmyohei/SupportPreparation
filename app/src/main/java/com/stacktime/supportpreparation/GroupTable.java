package com.stacktime.supportpreparation;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/*
 * テーブル：グループ
 */
@Entity
public class GroupTable {
    //主キー
    @PrimaryKey(autoGenerate = true)
    private int id;

    //グループ名
    @ColumnInfo(name = "group_name")
    private String groupName;

    //「セットに追加されたやること」
    //　 !本データの文字列データ形式は以下のように、「半角空白」で区切る形とする
    //　  "pid1 pid2 pid3"
    @ColumnInfo(name = "task_pids_string")
    private String taskPidsStr = "";

    //やること合計時間
    @ColumnInfo(name = "total_time")
    private int totalTime = 0;

    /*
     * 定数
     */
    private final static String DELIMITER = " ";    //「グループ内やること」の区切文字

    /*
     * 非レコードフィールド
     */
    @Ignore
    private TaskArrayList<TaskTable>    mTaskInGroupList;   //グループ内「やること」
    @Ignore
    private TaskRecyclerAdapter         mTaskAdapter;       //グループ内「やること」用アダプタ

    /*
     * コンストラクタ
     */
    public GroupTable(String groupName) {
        this.groupName     = groupName;
        mTaskInGroupList   = new TaskArrayList<>();
    }

    @Ignore
    public GroupTable(String groupName, int totalTime) {
        this.groupName     = groupName;
        this.totalTime     = totalTime;
        mTaskInGroupList   = new TaskArrayList<>();
    }

    /*
     * getter/setter
     */

    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return this.id;
    }

    public String getGroupName() {
        return this.groupName;
    }
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getTaskPidsStr() {
        return taskPidsStr;
    }
    public void setTaskPidsStr(String taskPidsStr) {
        this.taskPidsStr = taskPidsStr;
    }

    public void setTotalTime(int totalTime) {
        this.totalTime = totalTime;
    }
    public int getTotalTime() {
        return this.totalTime;
    }

    /*
     * グループ内「やること」取得・設定
     */
    public TaskArrayList<TaskTable> getTaskInGroupList() {
        return mTaskInGroupList;
    }
    public void setTaskInGroupList(TaskArrayList<TaskTable> mTaskInGroupList) {
        this.mTaskInGroupList = mTaskInGroupList;
    }

    /*
     * グループ内「やること」アダプタ 取得・設定
     */
    public TaskRecyclerAdapter getTaskAdapter() {
        return mTaskAdapter;
    }
    public void setTaskAdapter(TaskRecyclerAdapter taskAdapter) {
        this.mTaskAdapter = taskAdapter;
    }

}