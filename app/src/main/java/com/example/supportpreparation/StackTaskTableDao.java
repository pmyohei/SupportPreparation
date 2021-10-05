package com.example.supportpreparation;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/*
 * DAO の定義：「積み上げやること」テーブル
 *   DB操作の仲介役
 */
@Dao
public interface StackTaskTableDao {
    /*
     * 全レコード取得
     */
    @Query("SELECT * FROM stackTaskTable")
    List<StackTaskTable> getAll();

    /*
     * 取得：プライマリーキー
     *   ※指定されたスタックorアラームのプライマリーキーを取得
     *   ※未登録の場合、プライマリーキーは「0」が返される（実証）＝プライマリーキーは「１」から割り当てられる
     */
    @Query("SELECT id FROM stackTaskTable WHERE isStack=(:isStack)")
    int getPid(boolean isStack);

    /*
     * 取得：選択済み「やること」文字列
     */
    @Query("SELECT task_pids_string FROM stackTaskTable WHERE id=(:pid)")
    String getTaskPidsStr(int pid);

    /*
     * 更新（全フィールド更新）
     */
    @Query("UPDATE stackTaskTable set task_pids_string=(:taskPidsStr), alarmOnOffStr=(:alarmOnOffStr), date=(:date), time=(:time), isLimit=(:isLimit), isStack=(:isStack), onAlarm=(:onAlarm) WHERE id=(:pid)")
    void update( int pid,
                 String taskPidsStr,
                 String alarmOnOffStr,
                 String date,
                 String time,
                 boolean isLimit,
                 boolean isStack,
                 boolean onAlarm  );

    /*
     * 削除：指定「スタック or アラーム」に該当するすべてのレコードを削除
     */
    @Query("DELETE FROM stackTaskTable WHERE isStack=(:isStack)")
    void deleteWithCategory( boolean isStack );

    /*
     * 削除：すべて
     */
    @Query("DELETE FROM stackTaskTable")
    void deleteAll();

    @Insert
    void insert(StackTaskTable stackTaskTable);

    @Delete
    void delete(StackTaskTable stackTaskTable);
}
