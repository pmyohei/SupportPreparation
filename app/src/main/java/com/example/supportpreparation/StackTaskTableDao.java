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
     * ！仕様上、取得できるのは1件のみ
     * 　(登録数を1件としているため)
     */
    @Query("SELECT * FROM stackTaskTable")
    List<StackTaskTable> getAll();

    /*
     * 取得：プライマリーキー
     *   ※未登録の場合、プライマリーキーは「0」が返される（実証）
     *     ＝プライマリーキーは「１」から割り当てられる
     */
    //@Query("SELECT id FROM stackTaskTable WHERE set_name=(:setName)")
    //int getPid(String setName);

    /*
     * 取得：選択済み「やること」文字列
     */
    @Query("SELECT task_pids_string FROM stackTaskTable WHERE id=(:pid)")
    String getTaskPidsStr(int pid);

    /*
     * 削除：プライマリーキー指定
     */
    @Query("DELETE FROM stackTaskTable")
    void deleteAll();

    @Insert
    void insert(StackTaskTable stackTaskTable);

    @Delete
    void delete(StackTaskTable stackTaskTable);
}
