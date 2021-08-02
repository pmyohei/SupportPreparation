package com.example.supportpreparation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/*
 * 「やること」管理画面
 */
public class TaskManageActivity extends AppCompatActivity implements AsyncTaskTableOperaion.TaskOperationListener {

    private AppDatabase         db;                 //DB
    private List<TaskTable>     taskList;           //「やること」リスト
    private TaskRecyclerAdapter taskAdapter;        //「やること」表示アダプタ
    private AsyncTaskTableOperaion.TaskOperationListener
                                taskListener;       //「やること」操作リスナー
    private int                 _deleted_taskPos;   //削除対象のID

    //「やること」削除待ちなし
    private final int           NOT_DELETE_WAITING = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_manage);

        //DB操作インスタンスを取得
        this.db = AppDatabaseSingleton.getInstance(getApplicationContext());
        //「やること」操作リスナー
        this.taskListener = (AsyncTaskTableOperaion.TaskOperationListener)TaskManageActivity.this;
        //削除待ちの「やること」-リストIndex
        this._deleted_taskPos = NOT_DELETE_WAITING;

        //現在登録されている「やること」を表示
        this.displayTask();

        // FloatingActionButton
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_addTask);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                CoordinatorLayout coordinatorLayout
                        = (CoordinatorLayout) findViewById(R.id.cl_taskManage);
                Snackbar
                        .make(coordinatorLayout, "Hello, Snackbar!", Snackbar.LENGTH_SHORT)
                        .show();
                 */
                //-- 「やること」追加ダイアログの生成
                //Bundle生成
                Bundle bundle = new Bundle();
                //FragmentManager生成
                FragmentManager transaction = getSupportFragmentManager();

                //ダイアログを生成
                DialogFragment dialog = new CreateTaskDialog((AsyncTaskTableOperaion.TaskOperationListener) view.getContext());
                dialog.setArguments(bundle);
                dialog.show(transaction, "CreateTask");
            }
        });
    }

    /*
     * 「やること」の表示
     *    登録済みの「やること」データを全て表示する。
     */
    private void displayTask(){

        //-- 非同期スレッドにて、読み込み
        //「やること」
        new AsyncTaskTableOperaion(this.db, this, AsyncTaskTableOperaion.DB_OPERATION.READ).execute();
    }


    /*
     * 「やること」編集ダイアログの生成
     */
    private void createUpdateTaskDialog(View view ) {

        //「やること」情報
        String taskName = ((TextView)view.findViewById(R.id.tv_taskName)).getText().toString();
        String taskTimeStr = ((TextView)view.findViewById(R.id.tv_taskTime)).getText().toString();

        taskTimeStr = taskTimeStr.replace(" min", "");
        int taskTime = Integer.parseInt(taskTimeStr);

        //ダイアログへ渡すデータを設定
        Bundle bundle = new Bundle();
        bundle.putString("TaskName", taskName);
        bundle.putInt("TaskTime", taskTime);

        //FragmentManager生成
        FragmentManager transaction = getSupportFragmentManager();

        //ダイアログを生成
        DialogFragment dialog = new CreateTaskDialog((AsyncTaskTableOperaion.TaskOperationListener) view.getContext());
        dialog.setArguments(bundle);
        dialog.show(transaction, "UpdateTask");
    }

    /*
     * 「やること」リスト検索
     */
    private int getIdTaskList( String task, int taskTime ) {

        int i = 0;
        for( TaskTable taskInfo: this.taskList ){

            //「やること」「やること時間」が一致するデータを発見した場合
            if( ( task == taskInfo.getTaskName() ) && (taskTime == taskInfo.getTaskTime()) ){
                return i;
            }

            i++;
        }

        //データなし
        return -1;
    }

    /* -------------------
     * リスナークラス
     */


    /* -------------------
     * インターフェース：「やること」
     *   「やること」の表示
     */
    @Override
    public void onSuccessTaskRead(List<TaskTable> taskList) {

        //「やること」を保持
        this.taskList = taskList;

        //レイアウトからリストビューを取得
        RecyclerView rv_task  = (RecyclerView) findViewById(R.id.rv_taskList);

        //レイアウトマネージャの生成・設定（横スクロール）
        LinearLayoutManager ll_manager = new LinearLayoutManager(this);
        ll_manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        rv_task.setLayoutManager(ll_manager);

        //グリッド表示の設定
        rv_task.setLayoutManager(new GridLayoutManager(this, 2));

        //アダプタの生成
        this.taskAdapter = new TaskRecyclerAdapter(this, this.taskList);

        //リスナー設定
        this.taskAdapter.setOnItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createUpdateTaskDialog(view);
            }
        });

        //アダプタの設定
        rv_task.setAdapter(this.taskAdapter);

        //ドラッグアンドドロップ、スワイプの設定
        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback( ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                                                    ItemTouchHelper.LEFT ){
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        //並び替えは要検討
                        /*
                        //！getAdapterPosition()←非推奨
                        final int fromPos = viewHolder.getAdapterPosition();
                        final int toPos   = target.getAdapterPosition();
                        //アイテム移動を通知
                        taskAdapter.notifyItemMoved(fromPos, toPos);
                        Log.i("test", "onMove " + fromPos + " " + toPos);
                         */
                        return true;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                        if( _deleted_taskPos != NOT_DELETE_WAITING ){
                            //削除待ちのものがあるなら、何もしない
                            return;
                        }

                        //-- DBから削除
                        int i = viewHolder.getAdapterPosition();
                        String taskName = taskList.get(i).getTaskName();
                        int    taskTime = taskList.get(i).getTaskTime();

                        new AsyncTaskTableOperaion(db, taskListener, AsyncTaskTableOperaion.DB_OPERATION.DELETE, taskName, taskTime).execute();

                        //アイテム削除を通知
                        _deleted_taskPos = viewHolder.getAdapterPosition();
                    }
                }
        );

        //リサイクラービューをアタッチ
        helper.attachToRecyclerView(rv_task);
    }

    @Override
    public void onSuccessTaskCreate(Integer code, TaskTable taskTable) {
        //-- 生成した「やること」を表示

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
        Toast toast = new Toast(getApplicationContext());
        toast.setText(message);
        //toast.setGravity(Gravity.CENTER, 0, 0);   //E/Toast: setGravity() shouldn't be called on text toasts, the values won't be used
        toast.show();

        if( code == -1 ){
            //登録済みなら、ここで終了
            return;
        }

        //生成された「やること」をリストに追加
        this.taskList.add( taskTable );
        //アダプタに変更を通知
        this.taskAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSuccessTaskDelete(String task, int taskTime) {

        //リストから削除
        this.taskList.remove(this._deleted_taskPos);
        //ビューにアイテム削除を通知
        this.taskAdapter.notifyItemRemoved(_deleted_taskPos);
        //削除待ちなしに戻す
        this._deleted_taskPos = NOT_DELETE_WAITING;

        //トーストの生成
        Toast toast = new Toast(getApplicationContext());
        toast.setText("削除しました");
        toast.show();
    }

    /* -------------------
     * インターフェース：「やること」
     *   「やること」の編集
     */
    @Override
    public void onSuccessTaskUpdate(String preTask, int preTaskTime, TaskTable updatedTask) {
        //更新されたリストのIndexを取得
        int i = this.getIdTaskList(preTask, preTaskTime);

        //フェールセーフ
        if( i == -1 ){
            //見つからなければ、何もしない
            Log.i("failsafe", "onSuccessTaskUpdate couldn't found");
            return;
        }

        //リストを更新
        this.taskList.set(i, updatedTask);
        //アダプタに変更を通知
        this.taskAdapter.notifyDataSetChanged();

        //トーストの生成
        Toast toast = new Toast(getApplicationContext());
        toast.setText("更新しました");
        toast.show();
    }


}