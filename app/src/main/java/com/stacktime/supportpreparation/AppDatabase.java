package com.stacktime.supportpreparation;
import androidx.room.Database;
import androidx.room.RoomDatabase;

/*
 * Database定義
 */
@Database(
        entities = {
                TaskTable.class,        //やることテーブル
                GroupTable.class,       //やることセットテーブル
                StackTaskTable.class    //積み上げやることテーブル
        },
        version = 1,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    //DAO
    public abstract TaskTableDao        taskTableDao();             //やることテーブル
    public abstract GroupTableDao       groupTableDao();            //やることグループテーブル
    public abstract StackTaskTableDao   stackTaskTableDao();        //積み上げられたやることテーブル
}
