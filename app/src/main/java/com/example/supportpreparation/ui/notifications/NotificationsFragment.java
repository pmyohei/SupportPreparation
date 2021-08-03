package com.example.supportpreparation.ui.notifications;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.supportpreparation.AppDatabase;
import com.example.supportpreparation.AppDatabaseSingleton;
import com.example.supportpreparation.AsyncSetTableOperaion;
import com.example.supportpreparation.AsyncTaskTableOperaion;
import com.example.supportpreparation.CreateSetDialog;
import com.example.supportpreparation.R;
import com.example.supportpreparation.SetRecyclerAdapter;
import com.example.supportpreparation.SetTable;
import com.example.supportpreparation.TaskRecyclerAdapter;
import com.example.supportpreparation.TaskTable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class NotificationsFragment extends Fragment  implements AsyncSetTableOperaion.SetOperationListener,
                                                                AsyncTaskTableOperaion.TaskOperationListener {

    private View                    mRootLayout;                //本フラグメントに設定しているレイアウト
    private Fragment                mFragment;                  //本フラグメント
    private Context                 mContext;                   //コンテキスト（親アクティビティ）
    private AppDatabase             mDB;                        //DB
    private List<SetTable>          mSetList;                   //「やることセット」リスト
    private List<List<TaskTable>>   mTasksList;                 //「やることセット」リスト
    private SetRecyclerAdapter      mSetAdapter;                //「やることセット」表示アダプタ

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //自身のフラグメントを保持
        this.mFragment = getParentFragmentManager().getFragments().get(0);
        //設定レイアウト
        this.mRootLayout = inflater.inflate(R.layout.fragment_notifications, container, false);
        //親アクティビティのコンテキスト
        this.mContext = this.mRootLayout.getContext();
        //DB操作インスタンスを取得
        this.mDB = AppDatabaseSingleton.getInstance(this.mRootLayout.getContext());

        //現在登録されている「やること」「やることセット」を表示
        this.displayData();

        // FloatingActionButton
        FloatingActionButton fab = (FloatingActionButton) this.mRootLayout.findViewById(R.id.fab_addSet);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //-- 「やることセット」追加ダイアログの生成
                createTaskSetDialog();
            }
        });


        return mRootLayout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


    /*
     * タスク生成ダイアログの生成
     */
    private void createTaskSetDialog(){
        //Bundle生成
        Bundle bundle = new Bundle();
        //FragmentManager生成
        FragmentManager transaction = getParentFragmentManager();

        //ダイアログを生成
        DialogFragment dialog = new CreateSetDialog((AsyncSetTableOperaion.SetOperationListener)mFragment);
        dialog.setArguments(bundle);
        dialog.show(transaction, "CreateSet");
    }

    /*
     * 「やること」の表示
     *    登録済みの「やること」データを全て表示する。
     */
    private void displayData(){

        //-- 非同期スレッドにて、読み込み開始
        //「やること」
        new AsyncTaskTableOperaion(this.mDB, this, AsyncTaskTableOperaion.DB_OPERATION.READ).execute();
        //「やることセット」
        new AsyncSetTableOperaion(this.mDB, this, AsyncSetTableOperaion.DB_OPERATION.READ).execute();
    }

    /*
     * 「やること」ビューの作成・レイアウトへの追加
     *    「やること」データ単体のレイアウトを作り、ルートレイアウトへ追加する。
     */
    private void addDisplayUnitTask(TaskTable task){
        /*
        //--「やること」のレイアウトインフレータを取得
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //「やること」データのビュー
        View taskLayout = inflater.inflate(R.layout.unit_task_for_taskset, null);

        //-- 「やること」にリスナーを登録
        //リスナー設定ビューの取得
        LinearLayout ll_taskInfo = taskLayout.findViewById(R.id.ll_taskInfo);
        //リスナー設定
        ll_taskInfo.setOnClickListener(
                new TaskSelectListener()
        );

        ll_taskInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i("test", "is=" + v.isEnabled());

                if(v.isEnabled()){
                    //有効 → 無効
                    Log.i("test", "false");
                    //v.setEnabled(false);
                } else {
                    //無効 → 有効
                    Log.i("test", "true");
                    v.setEnabled(true);
                }
            }
        });

        //-- 「やること」データの表示内容を設定
        //「やること」
        TextView tv_data = taskLayout.findViewById(R.id.tv_taskName);
        tv_data.setText(task.getTaskName());
        //「やること」の時間
        tv_data = taskLayout.findViewById(R.id.tv_taskTime);
        tv_data.setText(task.getTaskTime() + " min");

        //「やること」データを表示先のビューに追加
        this.ll_rootDisplay.addView( taskLayout );
        */
    }


    /*
     *  -------------------------------------------------
     *  インターフェース
     *  -------------------------------------------------
     */

    /* -------------------
     * 「やることセット」
     */

    @Override
    public void onSuccessSetRead(List<SetTable> setList, List<List<TaskTable>> tasksList) {

        Log.i("test", "start onSuccessSetRead");

        //「やることセット」「紐づいたやること」を保持
        this.mSetList   = setList;
        this.mTasksList = tasksList;

        //-- 「やることセット」の表示
        //レイアウトからリストビューを取得
        RecyclerView rv_set  = (RecyclerView) mRootLayout.findViewById(R.id.rv_setList);

        //レイアウトマネージャの生成・設定（横スクロール）
        LinearLayoutManager l_manager = new LinearLayoutManager(mContext);
        l_manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        rv_set.setLayoutManager(l_manager);

        Log.i("test", "onSuccessSetRead");

        //アダプタの生成・設定
        this.mSetAdapter = new SetRecyclerAdapter(mContext, setList, tasksList);
        rv_set.setAdapter(this.mSetAdapter);
    }

    @Override
    public void onSuccessSetCreate(Integer code, String setName) {

        //-- 作成結果をトーストで表示
        //結果メッセージ
        String message;

        //戻り値に応じてトースト表示
        if( code == -1 ){
            //エラーメッセージを表示
            message = "登録済みです";
        } else {
            //正常メッセージを表示
            message = "登録しました";
        }

        //トーストの生成
        Toast toast = new Toast(mContext);
        toast.setText(message);
        //toast.setGravity(Gravity.CENTER, 0, 0);   //E/Toast: setGravity() shouldn't be called on text toasts, the values won't be used
        toast.show();

        if( code == -1 ){
            //登録済みなら、ここで終了
            return;
        }

        //生成された「やることセット」情報をリストに追加
        this.mSetList.add( new SetTable(setName) );
        this.mTasksList.add( new ArrayList<>() );

        //アダプタに変更を通知
        this.mSetAdapter.notifyDataSetChanged();
        Log.i("test", "notifyDataSetChanged");
    }

    @Override
    public void onSuccessSetDelete(String task) {

    }

    @Override
    public void onSuccessSetUpdate(String preTask, String task) {

    }

    /* -------------------
     * 「やること」
     */

    @Override
    public void onSuccessTaskRead(List<TaskTable> taskList) {

        //-- 「やること」の表示（セットへ追加の選択用）
        //レイアウトからリストビューを取得
        RecyclerView rv_task  = (RecyclerView) mRootLayout.findViewById(R.id.rv_taskList);

        //レイアウトマネージャの生成・設定（横スクロール）
        LinearLayoutManager ll_manager = new LinearLayoutManager(mContext);
        ll_manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        rv_task.setLayoutManager(ll_manager);

        //アダプタの生成・設定
        TaskRecyclerAdapter adapter = new TaskRecyclerAdapter(mContext, R.layout.item_task_for_set, taskList);
        rv_task.setAdapter(adapter);
    }

    @Override
    public void onSuccessTaskCreate(Integer code, TaskTable taskTable) {
        //do nothing
    }
    @Override
    public void onSuccessTaskDelete(String task, int taskTime) {
        //do nothing
    }
    @Override
    public void onSuccessTaskUpdate(String preTask, int preTaskTime, TaskTable updatedTask) {
        //do nothing
    }

    /*
     *  -------------------------------------------------
     *  インナークラス
     *  -------------------------------------------------
     */

}