package com.example.supportpreparation;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Calendar;
import java.util.List;

/*
 * エンティティ
 *   テーブルに相当
 */
@Entity
public class TaskTable {
    //主キー
    @PrimaryKey(autoGenerate = true)
    private int id;

    //「やること」
    @ColumnInfo(name = "task_name")
    private String taskName;

    //「やること」の時間
    @ColumnInfo(name = "task_time")
    private int taskTime;



    /*
     * 非レコードフィールド
     */
    @Ignore
    private Calendar mAlarmCalendar;       //アラーム時刻のカレンダー

    /*
     * コンストラクタ
     */
    public TaskTable(int pid, String taskName, int taskTime) {
        this.id       = pid;
        this.taskName = taskName;
        this.taskTime = taskTime;
    }

    /*
     * コンストラクタ
     */
    public TaskTable(String taskName, int taskTime) {
        this.taskName = taskName;
        this.taskTime = taskTime;
    }

    public void setId(int id) {
        this.id = id;
    }
    public int getId() {
        return this.id;
    }

    public String getTaskName() {
        return this.taskName;
    }
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public int getTaskTime() {
        return this.taskTime;
    }
    public void setTaskTime(int taskTime) {
        this.taskTime = taskTime;
    }

    public Calendar getAlarmCalendar() {
        return mAlarmCalendar;
    }
    public void setAlarmCalendar(Calendar mAlarmCalendar) {
        this.mAlarmCalendar = mAlarmCalendar;
    }

}