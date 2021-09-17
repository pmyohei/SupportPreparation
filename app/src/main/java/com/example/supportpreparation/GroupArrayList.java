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
    public int searchIdxByGroupName(String groupName){

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
     * 「グループ」取得（pid指定）
     */
    public GroupTable getGroupByPid(int groupPid){

        int size = size();
        for (int i = 0; i < size; i++) {
            GroupTable group = get(i);
            //グループPidが一致した場合
            if ( groupPid == group.getId() ) {
                return group;
            }
        }
        return null;
    }

}
