package com.example.supportpreparation;
import android.content.Context;
import androidx.room.Room;

/*
 * AppDatabaseオブジェクトのシングルトン生成
 */
public class AppDatabaseSingleton {
    private static AppDatabase instance = null;

    /*
     * インスタンス取得
     */
    public static AppDatabase getInstance(Context context) {
        if (instance != null) {
            //インスタンスを生成済みなら、それを返す
            return instance;
        }

        //Roomクラスからインスタンスを生成
        instance = Room.databaseBuilder(context, AppDatabase.class, "database-task").build();
        return instance;
    }

    /*
     * インスタンス取得(ダイアログ用)
     *   ※使用していいのは、getInstance()がコールされたあと
     */
    public static AppDatabase getInstanceNotFirst() {

        return instance;
    }

}
