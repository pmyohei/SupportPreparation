package com.example.supportpreparation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class GroupArrayList<E> extends ArrayList<GroupTable> {

    public static int NO_DATA = -1;        //データなし

    public GroupArrayList() {
        super();
    }

    /*
     * 「グループ」Index検索（グループ名指定）
     */
    private int searchIdxByGroupName(String groupName){

        int size = size();
        for (int i = 0; i < size; i++) {
            //グループ名が一致した場合
            if (get(i).getGroupName().equals( groupName )) {
                return i;
            }
        }
        return NO_DATA;
    }

    /*
     * 「グループ」Index検索（グループ名指定）
     */
    private int getIdxByPid(int groupPid){

        int size = size();
        for (int i = 0; i < size; i++) {
            //グループPidが一致した場合
            int pid = get(i).getId();
            if ( pid == groupPid ) {
                return i;
            }
        }
        return NO_DATA;
    }
}
