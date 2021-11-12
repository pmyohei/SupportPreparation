package com.example.supportpreparation;


import static android.text.format.DateUtils.FORMAT_NUMERIC_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_YEAR;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/*
 * 共通リソース管理
 */
public class ResourceManager {

    //やること無効時間（空データとして設定）
    public static final int INVALID_MIN = -1;

    //やること時間の色変更閾値
    private static final int TASK_TIME_VERY_SHORT   = 5;
    private static final int TASK_TIME_SHORT        = 10;
    private static final int TASK_TIME_NORMAL       = 30;
    private static final int TASK_TIME_LONG         = 60;

    //アラームキャンセル最大数
    public static final int MAX_STACK_TASK_NUM      = 100;

    //文字列
    public static final String STR_NO_INPUT_BASETIME = "--:--";         //!文字列変更時は注意! R.string.limittime_no_inputs

    public static final String STR_DATE_AND_TIME    = "yyyy/MM/dd HH:mm";
    public static final String STR_DATE             = "yyyy/MM/dd";
    public static final String STR_GRAGH_LAST_TIME  = "HH:mm (MM/dd)";
    public static final String STR_HOUR_MIN         = "HH:mm";
    public static final String STR_HOUR_MIN_HM      = "HH'h'mm'm'";
    public static final String STR_HOUR_MIN_SEC     = "HH:mm:ss";

    public static final String SAVE_FORMAT_STR_DATE = "%04d/%02d/%02d";
    public static final String SAVE_FORMAT_STR_TIME = "%02d:%02d";

    //キー文字列
    public static final String KEY_TASK_NAME        = "TaskName";
    public static final String KEY_TASK_TIME        = "TaskTime";
    public static final String KEY_GROUP_NAME       = "EditGroupName";

    //デリミタ文字
    public static final String DELIMITER_DATA_AND_TIME = " ";

    //！Localeは任意の指定で問題ない場面で使用すること
    public static final SimpleDateFormat sdf_DateAndTime = new SimpleDateFormat(STR_DATE_AND_TIME, Locale.US);
    public static final SimpleDateFormat sdf_Date        = new SimpleDateFormat(STR_DATE, Locale.US);
    public static final SimpleDateFormat sdf_Date_jp     = new SimpleDateFormat(STR_DATE, Locale.JAPAN);

    //通知キー
    public static final String NOTIFY_SEND_KEY = "notifykey";


    /*
     * やること時間に応じたカラーIDの取得
     */
    public static int getTaskTimeColorId(int time) {

        int id;

        if (time == INVALID_MIN) {
            id = R.color.clear;

        } else if (time <= TASK_TIME_VERY_SHORT) {
            id = R.color.bg_task_very_short;

        } else if (time <= TASK_TIME_SHORT) {
            id = R.color.bg_task_short;

        } else if (time <= TASK_TIME_NORMAL) {
            id = R.color.bg_task_normal;

        } else if (time <= TASK_TIME_LONG) {
            id = R.color.bg_task_long;

        } else {
            id = R.color.bg_task_very_long;
        }

        return id;
    }

    /*
     * 国際化対応した年月日を取得
     *    イギリス英語: 18/11/2016
     *    アメリカ英語: 11/18/2016
     *    日本語　　　: 2016/11/18
     */
    public static String getInternationalizationDate(Context context, long mills) {

        //ローカル対応した年月日を返す
        return DateUtils.formatDateTime(context, mills, FORMAT_SHOW_YEAR | FORMAT_SHOW_DATE | FORMAT_NUMERIC_DATE);
    }

    /*
     * 国際化対応した年月日時分を取得
     *    イギリス英語: 18/11/2016 12:34
     *    アメリカ英語: 11/18/2016 12:34
     *    日本語　　　: 2016/11/18 12:34
     */
    public static String getInternationalizationDateTime(Context context, long mills) {

        String dateStr = DateUtils.formatDateTime(context, mills, FORMAT_SHOW_YEAR | FORMAT_SHOW_DATE | FORMAT_NUMERIC_DATE);

        String timeStr = (String) DateFormat.format(STR_HOUR_MIN, mills);

        //ローカル対応した年月日を返す
        return (dateStr + DELIMITER_DATA_AND_TIME + timeStr);
    }

    /*
     * 時分秒のsdfフォーマットを取得
     *   ※カウントダウンでの使用を想定しているため、
     * 　　タイムゾーンはUTCを指定
     */
    public static SimpleDateFormat getSdfHMS(){

        //タイムゾーン-UTC
        SimpleDateFormat sdf = new SimpleDateFormat(STR_HOUR_MIN_SEC);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return sdf;
    }

    /*
     * 時分のsdfフォーマットを取得
     *   ※カウントダウンでの使用を想定しているため、
     * 　　タイムゾーンはUTCを指定
     */
    public static SimpleDateFormat getSdfHM(){

        //タイムゾーン-UTC
        SimpleDateFormat sdf = new SimpleDateFormat(STR_HOUR_MIN_HM);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return sdf;
    }


}
