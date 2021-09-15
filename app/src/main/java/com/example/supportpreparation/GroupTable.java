package com.example.supportpreparation;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

/*
 * エンティティ
 *   テーブルに相当
 */
@Entity
public class GroupTable {
    //主キー
    @PrimaryKey(autoGenerate = true)
    private int id;

    //「やることセット」名
    @ColumnInfo(name = "group_name")
    private String groupName;

    //「セットに追加されたやること」
    //　 !本データの文字列データ形式は以下のように、「半角空白」で区切る形とする
    //　  "pid1 pid2 pid3"
    @ColumnInfo(name = "task_pids_string")
    private String taskPidsStr = "";

    /*
     * 非レコードフィールド
     */
    private final static String DELIMITER = " ";    //「セットに追加されたやること」の区切文字

    @Ignore
    private List<TaskTable> mTaskInGroupList;       //グループ内の「やること」

    /*
     * コンストラクタ
     */
    public GroupTable(String groupName) {
        this.groupName          = groupName;
        mTaskInGroupList   = new ArrayList<>();
    }

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

    public List<TaskTable> getTaskInGroupList() {
        return mTaskInGroupList;
    }

    public void setTaskInGroupList(List<TaskTable> mTaskInGroupList) {
        this.mTaskInGroupList = mTaskInGroupList;
    }
}