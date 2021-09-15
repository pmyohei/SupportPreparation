package com.example.supportpreparation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class TaskArrayList<E> extends ArrayList<TaskTable> {

    public static int NO_DATA = -1;        //データなし

    public TaskArrayList() {
        super();
    }

    /*
     * 「やること」取得（Pid指定）
     */
    public TaskTable getTaskByPid(int pid) {

        int size = size();
        for (int i = 0; i < size; i++) {

            int id = get(i).getId();
            if (id == pid) {
                return get(i);
            }
        }
        return null;
    }

    /*
     * 「やることPid」取得（やること、やること時間 指定）
     */
    private int getPidByTaskInfo(String taskName, int taskTime) {

        int size = size();
        for (int i = 0; i < size; i++) {
            //やること
            TaskTable task = get(i);

            //「やること」「やること時間」が一致するデータを発見した場合
            if ((taskName.equals(task.getTaskName())) && (taskTime == task.getTaskTime())) {
                return i;
            }
        }

        //データなし
        return NO_DATA;
    }

    /*
     *　先頭のアラームIndexを取得
     */
    public int getTopAlarmIndex() {
        //現在時刻
        Date nowTime = new Date();

        /*dbg
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPANESE);
        String nowStr = sdf.format(nowTime);
        Log.i("test", "nowStr=" + nowStr);
        Log.i("test", "now=" + nowTime.getTime());
        */

        int size = size();
        for (int i = 0; i < size; i++) {
            //アラーム時間取得
            Calendar alarmCalendar = get(i).getAlarmCalendar();
            if( alarmCalendar == null ){
                return NO_DATA;
            }

            Date alarmDate = alarmCalendar.getTime();

            //現在時刻 → アラーム時刻 の場合
            if (alarmDate.after(nowTime)) {
                return i;
            }
        }

        //見つからない（すべてのアラーム時間が現在時刻より前）場合
        //（いずれのアラーム時間も全て過ぎている場合）
        return NO_DATA;
    }

    /*
     *　やること時間集計
     */
    public int getTotalTaskTime(){

        int totalMinute = 0;

        //やること時間の集計
        int size = size();
        for (int i = 0; i < size; i++) {
            totalMinute += get(i).getTaskTime();
        }

        return totalMinute;
    }

}
