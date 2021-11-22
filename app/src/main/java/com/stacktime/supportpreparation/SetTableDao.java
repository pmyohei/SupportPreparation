package com.stacktime.supportpreparation;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/*
 * DAO の定義：やることセットテーブル
 *   DB操作の仲介役
 */
@Dao
public interface SetTableDao {
    @Query("SELECT * FROM SetTable")
    List<SetTable> getAll();

    @Query("SELECT * FROM SetTable WHERE id IN (:ids)")
    List<SetTable> loadAllByIds(int[] ids);

    /*
     * 取得：プライマリーキー
     *   ※未登録の場合、プライマリーキーは「0」が返される（実証）
     *     ＝プライマリーキーは「１」から割り当てられる
     */
    @Query("SELECT id FROM SetTable WHERE set_name=(:setName)")
    int getPid(String setName);

    /*
     * 取得：選択済み「やること」文字列
     */
    @Query("SELECT task_pids_string FROM SetTable WHERE id=(:pid)")
    String getTaskPidsStr(int pid);

    /*
     * 更新：セット名
     *   指定されたプライマリーキーのレコードを更新
     */
    @Query("UPDATE SetTable set set_name=(:setName) WHERE id=(:pid)")
    int updateSetNameByPid(int pid, String setName);

    /*
     * 更新：選択済み「やること」
     *   指定されたプライマリーキーのレコードを更新
     */
    @Query("UPDATE SetTable set task_pids_string=(:taskPidsStr) WHERE id=(:pid)")
    int updateTaskPidsStrByPid(int pid, String taskPidsStr);

    /*
     * 削除：プライマリーキー指定
     */
    @Query("DELETE FROM SetTable WHERE id=(:pid)")
    void deleteByPid(int pid);

    @Insert
    void insertAll(SetTable... setTables);

    @Insert
    void insert(SetTable setTable);

    @Delete
    void delete(SetTable setTable);
}
