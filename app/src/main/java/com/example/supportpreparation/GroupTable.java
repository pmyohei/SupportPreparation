package com.example.supportpreparation;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

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

    //「セットに追加されたやること」の区切文字
    private final static String DELIMITER = " ";

    /*
     * コンストラクタ
     */
    public GroupTable(String groupName) {
        this.groupName = groupName;
    }

    /*
     * コンストラクタ
     */
    public GroupTable(String groupName, List<Integer> taskPids) {
        this.groupName = groupName;
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

}