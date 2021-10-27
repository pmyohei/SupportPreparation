package com.example.supportpreparation;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/*
 * 「やること」関連のテーブルレコードを管理するメソッドを提供する
 */
public class TaskTableManager {

    //pid間のデリミタ
    private final static String DELIMITER      = " ";

    //アラームON/OFF文字
    private final static String ALARM_ON_CHAR  = "1";
    private final static String ALARM_OFF_CHAR = "0";

    /*
     * 「やること」追加
     *   指定された文字列に追加する
     */
    public static String addTaskPidsStr(String toStr, int pid) {

        //指定IDが既に含まれているかチェック
        if (hasPidInStr(toStr, pid)) {
            //あるなら、追加せず終了
            return null;
        }

        //区切り文字として空白を入れて追加
        // ！最後に空白が入った状態で問題なし
        // ！split()で分割しても、最後の要素が空にはならない
        toStr += pid + DELIMITER;

        return toStr;
    }

    /*
     * 「やること」追加（重複許容）
     *   指定された文字列に追加する
     */
    public static String addTaskPidsStrDuplicate(String toStr, int pid) {

        //区切り文字として空白を入れて追加
        // ！最後に空白が入った状態で問題なし
        // ！split()で分割しても、最後の要素が空にはならない
        toStr += pid + DELIMITER;

        return toStr;
    }


    /*
     * PID有無判定
     *   指定された「選択済みやること文字列」内に、
     *   指定されたPIDが含まれているか判定する
     */
    private static boolean hasPidInStr(String str, int pid) {

        //半角スペースで分割
        String[] pidsStr = str.split(DELIMITER);

        //PID分ループ
        for (String pidStr : pidsStr) {
            //一致するか
            if (pidStr.equals(Integer.toString(pid))) {
                //あり
                return true;
            }
        }

        //なし
        return false;
    }

    /*
     * 「選択済みやること」リストを文字列として返す。
     * 　※各値の区切りは、指定されたデリミタにて行う。
     */
    public static String getPidsStr(TaskArrayList<TaskTable> list) {

        StringBuilder ret = new StringBuilder();

        for (TaskTable task : list) {

            //pidを文字列に
            String tmp = Integer.toString(task.getId());

            if (ret.length() == 0) {
                //1つ目
                ret = new StringBuilder(tmp);
            } else {
                //2つ目は、デリミタ付き。
                ret.append(DELIMITER).append(tmp);
            }
        }

        return ret.toString();
    }

    /*
     * 数値文字列（空白区切り）をIntegerのListに変換
     */
    public static List<Integer> convertIntArray(String str) {

        //pidなしなら、終了
        if (str.isEmpty()) {
            return null;
        }

        //半角スペースで分割
        String[] pidsStr = str.split(DELIMITER);

        //pidリスト
        List<Integer> pids = new ArrayList<>();

        //PID分ループ
        for (String pidStr : pidsStr) {
            //pidを整数に変換して、リストに追加
            pids.add(Integer.parseInt(pidStr));
        }

        return pids;
    }

    /*
     * 数値文字列（空白区切り）をIntegerのListに変換
     */
    public static void convertAlarmList(String str, List<Boolean> list ) {

        //文字列なしなら終了
        if (str.isEmpty()) {
            return;
        }

        //半角スペースで分割
        String[] pidsStr = str.split(DELIMITER);

        //PID分ループ
        for (String pidStr : pidsStr) {
            //On/Off
            boolean onoff = pidStr.equals( TaskTableManager.ALARM_ON_CHAR );

            //リストに追加
            list.add( onoff );
        }
    }

    /*
     * 「選択済みやること」リストを文字列として返す。
     * 　※各値の区切りは、指定されたデリミタにて行う。
     */
    public static String getAlarmStr( List<Boolean> list) {

        StringBuilder ret = new StringBuilder();

        for( boolean onoff: list ){
            //設定文字
            String tmp = ( onoff ? ALARM_ON_CHAR : ALARM_OFF_CHAR);

            if (ret.length() == 0) {
                //1つ目
                ret = new StringBuilder( tmp );
            } else {
                //2つ目は、デリミタ付き。
                ret.append( DELIMITER ).append( tmp );
            }
        }

        return ret.toString();
    }

    /*
     * 「やること」削除
     *   指定されたPIDのやることを削除する
     */
    public static String deleteTaskPidInStr(String str, int pid) {

        //半角スペースで分割
        String[] pidsStr = str.split(DELIMITER);

        //新文字列
        String newStr = "";

        //PID分ループ
        for( String pidStr: pidsStr ){
            //あれば、文字列へ追加しない
            if( pidStr.equals( Integer.toString(pid) ) ){
                continue;
            }

            //文字列へ追加
            newStr += pid + DELIMITER;
        }

        return newStr;
    }

    /*
     * 「やること」削除
     *   指定された位置のやることを削除する
     */
    public static String deleteTaskPosInStr(String str, int pos) {

        //半角スペースで分割
        String[] pidsStr = str.split(DELIMITER);

        //新文字列
        String newStr = "";

        //PID分ループ
        for( int i = 0; i < pidsStr.length; i++  ){

            //指定位置のPIDは、文字列に追加しない
            if( i == pos ){
                continue;
            }

            //文字列へ追加
            newStr += pidsStr[i] + DELIMITER;
        }

        return newStr;
    }


}