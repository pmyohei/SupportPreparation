package com.example.supportpreparation;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Calendar;

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
    private Calendar mStartCalendar;        //開始時間のカレンダー
    @Ignore
    private Calendar mEndCalendar;          //終了時間のカレンダー
    @Ignore
    private boolean mOnAlarm;               //アラームON
                                            //※本データはDB保存しない。保存はStackTable側で行う

    /*
     * コンストラクタ
     */
    public TaskTable(int pid, String taskName, int taskTime) {
        this.id       = pid;
        this.taskName = taskName;
        this.taskTime = taskTime;

        //設定されるまでOFF
        mOnAlarm = false;
    }

    /*
     * コンストラクタ
     */
    public TaskTable(String taskName, int taskTime) {
        this.taskName = taskName;
        this.taskTime = taskTime;

        //設定されるまでOFF
        mOnAlarm = false;
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

    public Calendar getStartCalendar() {
        return mStartCalendar;
    }
    public void setStartCalendar(Calendar calender) {
        this.mStartCalendar = calender;
    }

    public Calendar getEndCalendar() {
        return this.mEndCalendar;
    }
    public void setEndCalendar(Calendar calender) {
        this.mEndCalendar = calender;
    }

    public boolean isOnAlarm(){
        return mOnAlarm;
    }
    public void setOnAlarm( boolean onAlarm){
        mOnAlarm = onAlarm;
    }

}