package com.example.supportpreparation;



/*
 * リソース管理
 * 　本アプリにて、共通するリソースを管理する
 */
public class ResourceManager {

    //やること時間の最高値
    private static final int TASK_TIME_VERY_SHORT  = 5;
    private static final int TASK_TIME_SHORT       = 10;
    private static final int TASK_TIME_NORMAL      = 30;
    private static final int TASK_TIME_LONG        = 60;

    /*
     * やること時間に応じたカラーIDの取得
     */
    public static int getTaskTimeColorId( int time ){

        int id;

        if (time <= TASK_TIME_VERY_SHORT) {
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
