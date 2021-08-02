package com.example.supportpreparation;
import androidx.room.Database;
import androidx.room.RoomDatabase;

/*
 * Database の定義
 */
@Database(
        entities = {
                TaskTable.class,        //やることテーブル
                SetTable.class          //やることセットテーブル
        },
        version = 1,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TaskTableDao    taskTableDao();            //DAO-やることテーブル
    public abstract SetTableDao     setTableDao();         //DAO-やることセットテーブル
}
