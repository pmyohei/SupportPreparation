package com.stacktime.supportpreparation;

import java.util.ArrayList;

/*
 * ArrayList：やること用
 */
public class TaskArrayList<E> extends ArrayList<TaskTable> {

    public static final int NO_DATA = -1;        //データなし

    public TaskArrayList() {
        super();
    }

    /*
     *　最後尾のIndexの取得
     */
    public int getLastIdx() {

        int size = size();

        if (size == 0) {
            return NO_DATA;
        }

        return size - 1;
    }

/*    @Override
    public TaskTable remove(int index) {
        super.remove(index);
        return null;
    }
*/

    /*
     * 「やること」取得（Pid指定）
     */
    public TaskTable getTaskByPid(int pid) {

        int size = size();
        for (int i = 0; i < size; i++) {

            int id = get(i).getId();
            if (id == pid) {
                return get(i);
            }
        }
        return null;
    }

    /*
     * 「やることPid」取得（やること、やること時間 指定）
     */
    public int getIdxByTaskInfo(String taskName, int taskTime) {

        int size = size();
        for (int i = 0; i < size; i++) {
            //やること
            TaskTable task = get(i);

            //「やること」「やること時間」が一致するデータを発見した場合
            if ((taskName.equals(task.getTaskName())) && (taskTime == task.getTaskTime())) {
                return i;
            }
        }

        //データなし
        return NO_DATA;
    }

    /*
     * 「空データ」の追加
     *    リストが０件の場合にのみ行う。
     */
    public void addEmpty() {

        if (size() == 0) {
            //空なら追加
            add(new TaskTable("", ResourceManager.INVALID_MIN));
        }
    }


    /*
     * 「やること」追加
     */
    public void addTask( TaskTable task ) {

        //空データ削除
        removeEmpty();

        //やること追加
        add(task);
    }

    /*
     * 「やること」追加
     */
    public void addTask( int pos, TaskTable task ) {

        //空データ削除
        removeEmpty();

        //やること追加
        add(pos, task);
    }

    /*
     * 「空データ」削除
     * 　　ない場合は何もしない
     */
    public void removeEmpty() {

        //検索
        for (int i = 0; i < size(); i++) {
            int time = get(i).getTaskTime();
            if( time == ResourceManager.INVALID_MIN ){
                //削除
                TaskTable task = remove(i);
            }
        }
    }

}
