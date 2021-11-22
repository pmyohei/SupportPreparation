package com.stacktime.supportpreparation;
import android.util.Log;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/*
 * エンティティ
 *   テーブルに相当
 */
@Entity
public class SetTable {
    //主キー
    @PrimaryKey(autoGenerate = true)
    private int id;

    //「やることセット」名
    @ColumnInfo(name = "set_name")
    private String setName;

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
    public SetTable(String setName) {
        this.setName = setName;
    }

    /*
     * コンストラクタ
     */
    public SetTable(String setName, List<Integer> taskPids) {
        this.setName = setName;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public String getSetName() {
        return this.setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public String getTaskPidsStr() {
        return taskPidsStr;
    }

    public void setTaskPidsStr(String taskPidsStr) {
        this.taskPidsStr = taskPidsStr;
    }

}