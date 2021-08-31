package com.example.supportpreparation;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/*
 * 「やること」関連のテーブルレコードを管理するメソッドを提供する
 */
public class TaskTableManager {

    //「セットに追加されたやること」の区切文字
    private final static String DELIMITER = " ";

    /*
     * 「やること」追加
     *   指定された文字列に追加する
     */
    public static String addTaskPidsStr(String toStr, int pid) {

        //指定IDが既に含まれているかチェック
        if( hasPidInStr(toStr, pid) ){
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
    private static boolean hasPidInStr(String str, int pid){

        //半角スペースで分割
        String[] pidsStr = str.split(DELIMITER);

        //PID分ループ
        for( String pidStr: pidsStr ){
            //一致するか
            if( pidStr.equals( Integer.toString(pid) ) ){
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
    public static String getPidsStr( List<Integer> list) {

        String ret = "";

        for( Integer i: list ){

            if( ret.isEmpty() ){
                //1つ目
                ret = Integer.toString(i);
            } else {
                //2つ目は、デリミタ付き。
                ret = ret + TaskTableManager.DELIMITER + Integer.toString(i);
            }
        }

        return ret;
    }

    /*
     * 「選択済みやること文字列」のPidをint型配列として返す
     */
    public static List<Integer> getPidsIntArray(String str) {

        //pidなしなら、終了
        if( str.isEmpty() ){
            return null;
        }

        //半角スペースで分割
        String[] pidsStr = str.split(DELIMITER);

        Log.i("test", "getPidsIntArray pidsStr=" + pidsStr);

        //pidリスト
        List<Integer> pids = new ArrayList<>();

        //PID分ループ
        for( String pidStr: pidsStr ){
            Log.i("test", "getPidsIntArray loop pidStr=" + pidStr);
            //pidを整数に変換して、リストに追加
            pids.add( Integer.parseInt(pidStr) );
        }

        return pids;
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