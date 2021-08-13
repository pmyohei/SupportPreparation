package com.example.supportpreparation;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/*
 * エンティティ
 *   「積み上げやること」テーブル
 *
 *     備考：本レコードの最大登録数は「１つ」のみと想定
 */
@Entity
public class StackTaskTable {
    //主キー
    @PrimaryKey(autoGenerate = true)
    private int id;

    //「スタックされたやること」
    //　 !本データの文字列データ形式は以下のように、「半角空白」で区切る形とする
    //　  "pid1 pid2 pid3"
    @ColumnInfo(name = "task_pids_string")
    private String taskPidsStr = "";

    //リミット日時："yyyy/MM/dd"
    @ColumnInfo(name = "date")
    private String date = "";

    //リミット時間："hh:mm"
    @ColumnInfo(name = "time")
    private String time = "";

    /*
     * コンストラクタ
     */
    public StackTaskTable( String taskPidsStr, String date, String time ) {
        this.taskPidsStr = taskPidsStr;
        this.date = date;
        this.time = time;
    }

    /*
     * getter and setter
     */

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public String getTaskPidsStr() {
        return taskPidsStr;
    }

    public void setTaskPidsStr(String taskPidsStr) {
        this.taskPidsStr = taskPidsStr;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}