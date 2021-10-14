package com.example.supportpreparation;

//import android.icu.util.Calendar;

import static java.util.Collections.swap;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
 * エンティティ
 *   「積み上げやること」テーブル
 *
 *     備考：本レコードの最大登録数は「１つ」のみと想定
 */
@Entity
public class StackTaskTable implements Cloneable {
    //主キー
    @PrimaryKey(autoGenerate = true)
    private int id;

    //「スタックされたやること」
    //　 !本データの文字列データ形式は以下のように、「半角空白」で区切る形とする
    //　  "pid1 pid2 pid3"
    @ColumnInfo(name = "task_pids_string")
    private String taskPidsStr = "";

    @ColumnInfo(name = "alarmOnOffStr")
    private String alarmOnOffStr = "";

    //リミット日："yyyy/MM/dd"
    @ColumnInfo(name = "date")
    private String date = "";

    //リミット時間："hh:mm"
    @ColumnInfo(name = "time")
    private String time = "";

    //リミット指定か否か
    @ColumnInfo(name = "isLimit")
    private boolean isLimit = true;

    //スタックとしてのフィールドか、アラームとしてのフィールドか
    @ColumnInfo(name = "isStack")
    private boolean isStack = true;

    //最終時刻のアラーム
    @ColumnInfo(name = "onAlarm")
    private boolean onAlarm = true;


    /*
     * 非レコードフィールド
     */
    @Ignore
    public static int NO_DATA = -1;        //データなし
    @Ignore
    private TaskArrayList<TaskTable> mStackTaskList = new TaskArrayList<>();
    @Ignore
    private List<Boolean> mAlarmOnOffList = new ArrayList<>();

    /*
     * コンストラクタ
     */
    public StackTaskTable(String taskPidsStr, String date, String time) {
        this.taskPidsStr = taskPidsStr;
        this.date = date;
        this.time = time;

        //初期値はアラームON
        this.onAlarm = true;
    }

    /*
     *  コンストラクタ
     *   新規生成用
     */
/*
    @Ignore
    public StackTaskTable(StackTaskTable stackTable) {
        this.taskPidsStr = stackTable.getTaskPidsStr();
        this.alarmOnOffStr = stackTable.getAlarmOnOffStr();
        this.date = stackTable.getDate();
        this.time = stackTable.getTime();
        this.isLimit = stackTable.isLimit();
        this.isStack = stackTable.isStack();
        this.onAlarm = stackTable.isOnAlarm();
    }
*/

    /*
     *  コンストラクタ
     *   空指定用
     */
    @Ignore
    public StackTaskTable( boolean isStack ) {
        //本日の日付
        Date nowDate = new Date();
        this.date = ResourceManager.sdf_Date.format(nowDate);
        this.time = "--:--";    //!文字列変更時は注意! R.string.limittime_no_input

        this.isStack = isStack;
    }


    /*
     *
     * getter and setter
     *
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

    public String getAlarmOnOffStr() {
        return alarmOnOffStr;
    }
    public void setAlarmOnOffStr(String alarmOnOffStr) {
        this.alarmOnOffStr = alarmOnOffStr;
    }

    public boolean isOnAlarm(){
        return onAlarm;
    }
    public void setOnAlarm( boolean onAlarm){
        this.onAlarm = onAlarm;
    }

    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;

        //開始・終了時間の更新
        allUpdateStartEndTime();
    }

    public String getTime() {
        return time;
    }
    public void setTime(String time) {
        this.time = time;

        //開始終了時間の更新
        allUpdateStartEndTime();
    }

    public TaskArrayList<TaskTable> getStackTaskList() {
        return mStackTaskList;
    }
    public void setStackTaskList(TaskArrayList<TaskTable> taskList) {
        this.mStackTaskList = taskList;
    }

    public List<Boolean> getAlarmOnOffList() {
        return mAlarmOnOffList;
    }
    public void setAlarmOnOffList(List<Boolean>  alarmList) {
        this.mAlarmOnOffList = alarmList;
    }

    public boolean isLimit() {
        return this.isLimit;
    }
    public void setIsLimit(boolean limit) {
        this.isLimit = limit;

        //開始・終了時間の更新
        allUpdateStartEndTime();
    }

    public boolean isStack() {
        return this.isStack;
    }
    public void setIsStack(boolean stack) {
        this.isStack = stack;
    }


    @NonNull
    public Object clone() {
        try {
            StackTaskTable cloneObject =(StackTaskTable)super.clone();

            cloneObject.taskPidsStr = new String(taskPidsStr);
            cloneObject.date = new String(date);
            cloneObject.time = new String(time);
            cloneObject.isLimit = isLimit;
            cloneObject.mStackTaskList = (TaskArrayList<TaskTable>) mStackTaskList.clone();
            cloneObject.onAlarm = onAlarm;

            //※スタック側と重複しないようにするため、pidは初期値を設定
            cloneObject.id = 0;

            //※仕様上、cloneの生成は「スタック」→「アラーム」であるため、本フィールドはfalseに設定
            cloneObject.isStack = false;

            return cloneObject;

        } catch (CloneNotSupportedException ex){
            return null;
        }
    }

    /*
     * 開始・終了時間の全更新
     */
    public void allUpdateStartEndTime() {

        //ベース時間
        Calendar baseTime = getBaseTimeCalender();
        if (baseTime == null) {
            return;
        }

        if (isLimit) {
            //最終Index
            int last = mStackTaskList.getLastIdx();

            //全やることを更新（後ろから設定）
            for (int i = last; i >= 0; i--) {
                //対象のやること
                TaskTable task = mStackTaskList.get(i);

                //更新
                setupStartEndTime(i, task, baseTime);
            }

        } else {

            //全やることを更新（先頭から設定）
            for (int i = 0; i < mStackTaskList.size(); i++) {

                //対象のやること
                TaskTable task = mStackTaskList.get(i);

                //更新
                setupStartEndTime(i, task, baseTime);
            }
        }
    }

    /*
     * 開始・終了時間の設定
     */
    private void setupStartEndTime(int idx, TaskTable taskTable, Calendar baseTime) {

        int taskTime = taskTable.getTaskTime();

        //開始・終了時間
        Calendar start;
        Calendar end;

        if (isLimit) {

            int last = mStackTaskList.getLastIdx();

            if ( idx == last ) {
                //最後尾の場合
                //終了時間：ベース時間
                end = baseTime;

            } else {
                //最後尾より前の場合
                //終了時間：１つ前の開始時間
                end = mStackTaskList.get(idx + 1).getStartCalendar();
            }

            //開始時間：終了時間ーやること時間
            start = (Calendar) end.clone();
            start.add(Calendar.MINUTE, -taskTime);

        } else {

            if ( idx == 0 ) {
                //先頭の場合
                //開始時間：ベース時間
                start = baseTime;

            } else {
                //先頭より後の場合
                //開始時間：１つ前の終了時間
                start = mStackTaskList.get(idx - 1).getEndCalendar();
            }

            //終了時間：開始時間＋やること時間
            end = (Calendar) start.clone();
            end.add(Calendar.MINUTE, taskTime);
        }

        //開始・終了時間を設定
        taskTable.setStartCalendar(start);
        taskTable.setEndCalendar(end);

        Log.i("test", "setupStartEndTime start=" + start);
    }

    /*
     * 「やること」追加
     */
    public void addTask(TaskTable taskTable) {

        //追加されたやることのIndex
        int addedIdx;

        boolean onoff = taskTable.isOnAlarm();

        if (isLimit) {
            //リミット指定なら、先頭に追加
            mStackTaskList.add(0, taskTable);

            //アラームOn/Off
            mAlarmOnOffList.add(0, onoff);

            addedIdx = 0;
        } else {
            //スタート指定なら、最後尾に追加
            mStackTaskList.add(taskTable);

            //アラームOn/Off
            mAlarmOnOffList.add(onoff);

            addedIdx = mStackTaskList.getLastIdx();
        }

        //時間設定あれば、開始or終了 時間を設定
        if ( !time.equals("--:--") ) {

            //ベース時間
            Calendar baseTime = getBaseTimeCalender();

            //開始・終了時間を設定
            setupStartEndTime(addedIdx, taskTable, baseTime);
        }
    }

    /*
     * 「やること」挿入
     */
    public void insertTask(int idx, TaskTable taskTable) {

        //挿入
        mStackTaskList.add(idx, taskTable);
        mAlarmOnOffList.add(idx, taskTable.isOnAlarm() );

        //開始・終了時間の更新
        allUpdateStartEndTime();
    }

    /*
     * 「やること」削除
     */
    public void removeTask(int idx) {

        //リストから削除
        mStackTaskList.remove(idx);
        mAlarmOnOffList.remove(idx);

        //開始・終了時間の更新
        allUpdateStartEndTime();
    }

    /*
     * 「やること」入れ替え
     */
    public void swapTask(int fromPos, int toPos) {

        swap( mStackTaskList, fromPos, toPos );
        swap( mAlarmOnOffList, fromPos, toPos );

        //開始・終了時間の更新
        allUpdateStartEndTime();
    }


    /*
     * ベース時間をDateに変換
     */
    public Date getBaseTimeDate() {

        Date baseDate;
        try {
            //期限日と期限時間を連結
            String baseStr = date + " " + time;
            //文字列をDate型に変換
            baseDate = ResourceManager.sdf_DateAndTime.parse(baseStr);

        } catch (ParseException e) {
            e.printStackTrace();
            //例外発生なら、nullを返す
            return null;
        }

        return baseDate;
    }

    /*
     * ベース時間をCalendarに変換
     */
    public Calendar getBaseTimeCalender() {
        //保持中のベース時間を、Dateに変換
        Date baseDate = getBaseTimeDate();
        if (baseDate == null) {
            return null;
        }

        //Calendarに変換
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(baseDate);

        return calendar;
    }

    /*
     * 「やること」のアラーム時間を取得
     */
    public Calendar getAlarmCalender(int idx) {

        if (isLimit) {
            //リミットなら、開始時間
            return mStackTaskList.get(idx).getEndCalendar();
        } else {
            //スタートなら、終了時間
            return mStackTaskList.get(idx).getStartCalendar();
        }
    }

    /*
     *　現在時刻から、一番初めのやることindexを取得
     *  　※既にやることに割り込んでいる場合は、そのIndexを返す
     */
    public int getFirstArriveIdx() {
        //現在時刻
        Date nowTime = new Date();

        /*dbg
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPANESE);
        String nowStr = sdf.format(nowTime);
        Log.i("test", "nowStr=" + nowStr);
        Log.i("test", "now=" + nowTime.getTime());
        */

        //リストの先頭（時間が前側）からチェック
        //現在時刻よりも後の終了時間の中で、一番先に来るIndexを検索
        int size = mStackTaskList.size();
        for (int i = 0; i < size; i++) {
            //アラーム時間取得
            Calendar calendar = mStackTaskList.get(i).getEndCalendar();
            if (calendar == null) {
                return NO_DATA;
            }

            Date endDate = calendar.getTime();

            //現在時刻 → 開始時刻 の場合
            if (endDate.after(nowTime)) {
                //※先頭からチェックしているため、見つけた時点で一番早いIndexになる
                return i;
            }
        }

        //見つからない（すべてのアラーム時間が現在時刻より前）場合
        //（いずれのアラーム時間も全て過ぎている場合）
        return NO_DATA;
    }

    /*
     *　現在時刻がやることに割り込んでいるか否か
     */
    public boolean isInterruptTask() {

        Calendar calender = mStackTaskList.get(0).getStartCalendar();
        if( calender == null ){
            return false;
        }

        //現在時刻
        Date nowTime = new Date();
        //一番先頭のやることの開始時間
        Date firstStartDate = calender.getTime();

        //現在時刻 が 開始時刻 より「前」
        if (nowTime.before(firstStartDate)) {
            //現在時間は未到達
            return false;
        }

        //割り込み
        return true;
    }

    /*
     * すべてのやることが過ぎているか否か
     *   ※ベース時間未指定の場合は、true（過ぎている）を返す
     */
    public boolean isAllTaskPassed() {

        //最後のやることの終了時間
        int last = mStackTaskList.getLastIdx();
        if( last < 0 ){
            //ベース時間未設定の場合、trueを返却
            return true;
        }

        Calendar finalCal = mStackTaskList.get(last).getEndCalendar();
        if( finalCal == null ){
            //ベース時間未設定の場合、trueを返却
            return true;
        }

        Date finalDate = finalCal.getTime();
        Date nowTime = new Date();

        //現在時刻が後の場合、true。そうでないならfalse
        return nowTime.after(finalDate);
    }

    /*
     * 時間が設定されているか否か
     */
    public boolean isSettingTime() {

        int size = mStackTaskList.size();
        if( size > 0 ){
            Calendar calender = mStackTaskList.get(0).getStartCalendar();
            if( calender == null ){
                //未設定
                return false;
            }
        }

        Date baseDate = getBaseTimeDate();
        if (baseDate == null) {
            //未設定
            return false;
        }

        //設定あり
        return true;
    }
}