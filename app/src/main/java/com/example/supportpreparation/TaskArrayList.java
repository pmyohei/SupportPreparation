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
    public int getIdxByTaskInfo(String taskName, int taskTime) {

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
     *　【積まれたやることリスト用】
     *
     */
    public int getAlarmFirstArriveIdx() {
        //現在時刻
        Date nowTime = new Date();

        /*dbg
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPANESE);
        String nowStr = sdf.format(nowTime);
        Log.i("test", "nowStr=" + nowStr);
        Log.i("test", "now=" + nowTime.getTime());
        */

        //リストの先頭（時間が前側）からチェック
        //現在時刻よりも後のアラーム時間の中で、一番先に来るアラームのIndexを検索
        int size = size();
        for (int i = 0; i < size; i++) {
            //アラーム時間取得
            Calendar alarmCalendar = get(i).getAlarmCalendar();
            if (alarmCalendar == null) {
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
    public int getTotalTaskTime() {

        int totalMinute = 0;

        //やること時間の集計
        int size = size();
        for (int i = 0; i < size; i++) {
            totalMinute += get(i).getTaskTime();
        }

        return totalMinute;
    }

    /*
     *　やること開始時間の取得
     */
    public Date getStartDate(int idx, Date baseDate, boolean isLimit) {

        if (isLimit) {
            return getStartDateBaseLimit(idx, baseDate);
        } else {
            return getStartDateBaseStart(idx, baseDate);
        }
    }

    /*
     *　やること開始時間の取得（スタートベース）
     */
    public Date getStartDateBaseStart(int idx, Date startDate) {

        //やること時間の集計
        int total = 0;

        if (idx == 0) {
            return startDate;
        }

        //指定indexから先頭まで集計
        for (int i = idx; i >= 0; i--) {
            total += get(i).getTaskTime();
        }

        //リミット時間のカレンダーを生成
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        //リミット時間から集計時間を減算
        calendar.add(Calendar.MINUTE, total);

        return calendar.getTime();
    }

    /*
     *　やること開始時間の取得（リミットベース）
     */
    public Date getStartDateBaseLimit(int idx, Date limitDate) {

        //やること時間の集計
        int total = 0;
        int size = size();

        //指定indexから最後まで集計
        for (int i = idx; i < size; i++) {
            total += get(i).getTaskTime();
        }

        //リミット時間のカレンダーを生成
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(limitDate);

        //リミット時間から集計時間を減算
        calendar.add(Calendar.MINUTE, -total);

        return calendar.getTime();
    }


    /*
     *　やること終了時間の取得
     */
    public Date getEndDate(int idx, Date baseDate, boolean isLimit) {

        if (isLimit) {
            return getEndDateBaseLimit(idx, baseDate);
        } else {
            return getEndDateBaseStart(idx, baseDate);
        }
    }

    /*
     *　やること終了時間の取得（スタートベース）
     */
    public Date getEndDateBaseStart(int idx, Date startDate){

        //やること時間の集計
        int total = 0;

        //指定indexから先頭まで集計
        for (int i = idx; i >= 0; i--) {
            total += get(i).getTaskTime();
        }

        //リミット時間のカレンダーを生成
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        //リミット時間から集計時間を減算
        calendar.add(Calendar.MINUTE, +total);

        return calendar.getTime();
    }

    /*
     *　やること終了時間の取得（リミットベース）
     */
    public Date getEndDateBaseLimit(int idx, Date limitDate){

        //やること時間の集計
        int total = 0;
        int size = size();

        //最後のやることを指定された場合
        if (idx == size - 1) {
            //リミット時刻が終了時間
            return limitDate;
        }

        //「指定Indexの次のIndex」から「最後」まで集計
        for (int i = idx + 1; i < size; i++) {
            total += get(i).getTaskTime();
        }

        //リミット時間のカレンダーを生成
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(limitDate);

        //リミット時間から集計時間を減算
        calendar.add(Calendar.MINUTE, -total);

        return calendar.getTime();
    }

}
