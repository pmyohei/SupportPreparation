package com.example.supportpreparation;


import java.text.SimpleDateFormat;
import java.util.Locale;

/*
 * リソース管理
 * 　本アプリにて、共通するリソースを管理する
 */
public class ResourceManager {

    //やること無効時間
    public static final int INVALID_MIN = -1;                      //空データとして設定

    //やること時間の最高値
    private static final int TASK_TIME_VERY_SHORT  = 5;
    private static final int TASK_TIME_SHORT       = 10;
    private static final int TASK_TIME_NORMAL      = 30;
    private static final int TASK_TIME_LONG        = 60;


    public static final int MAX_ALARM_CANCEL_NUM = 256;               //アラームキャンセル最大数

    public static final SimpleDateFormat sdf_DateAndTime = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPANESE);
    public static final SimpleDateFormat sdf_Date = new SimpleDateFormat("yyyy/MM/dd", Locale.JAPANESE);
    public static final SimpleDateFormat sdf_Time = new SimpleDateFormat("HH:mm", Locale.JAPANESE);

    /*
     * やること時間に応じたカラーIDの取得
     */
    public static int getTaskTimeColorId( int time ){

        int id;

        if( time == INVALID_MIN ){
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






}
