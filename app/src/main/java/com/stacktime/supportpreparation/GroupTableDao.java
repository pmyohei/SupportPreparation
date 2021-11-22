package com.stacktime.supportpreparation;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/*
 * DAO の定義：グループテーブル用
 *   DB操作の仲介役
 */
@Dao
public interface GroupTableDao {
    @Query("SELECT * FROM GroupTable")
    List<GroupTable> getAll();

    @Query("SELECT * FROM GroupTable WHERE id IN (:ids)")
    List<GroupTable> loadAllByIds(int[] ids);

    /*
     * 取得：プライマリーキー
     *   ※未登録の場合、プライマリーキーは「0」が返される（実証）
     *     ＝プライマリーキーは「１」から割り当てられる
     */
    @Query("SELECT id FROM GroupTable WHERE group_name=(:groupName)")
    int getPid(String groupName);

    /*
     * 取得：選択済み「やること」文字列
     */
    @Query("SELECT task_pids_string FROM GroupTable WHERE id=(:pid)")
    String getTaskPidsStr(int pid);

    /*
     * 更新：セット名
     *   指定されたプライマリーキーのレコードを更新
     */
    @Query("UPDATE GroupTable set group_name=(:groupName) WHERE id=(:pid)")
    void updateGroupNameByPid(int pid, String groupName);

    /*
     * 更新：選択済み「やること」
     *   指定されたプライマリーキーのレコードを更新
     */
    @Query("UPDATE GroupTable set task_pids_string=(:taskPidsStr) WHERE id=(:pid)")
    void updateTaskPidsStrByPid(int pid, String taskPidsStr);

    /*
     * 削除：プライマリーキー指定
     */
    @Query("DELETE FROM GroupTable WHERE id=(:pid)")
    void deleteByPid(int pid);

    @Insert
    void insertAll(GroupTable... setTables);

    @Insert
    void insert(GroupTable setTable);

    @Delete
    void delete(GroupTable setTable);
}
