package com.example.supportpreparation.ui.taskManager;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.supportpreparation.AppDatabase;
import com.example.supportpreparation.AppDatabaseSingleton;
import com.example.supportpreparation.AsyncTaskTableOperaion;
import com.example.supportpreparation.CreateTaskDialog;
import com.example.supportpreparation.MainActivity;
import com.example.supportpreparation.R;
import com.example.supportpreparation.TaskRecyclerAdapter;
import com.example.supportpreparation.TaskTable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class TaskManagerFragment extends Fragment implements AsyncTaskTableOperaion.TaskOperationListener {

    private final int NOT_DELETE_WAITING = -1;    //「やること」削除待ちなし

    private MainActivity            mParentActivity;            //親アクティビティ
    private View                    mRootLayout;                //本フラグメントに設定しているレイアウト
    private Fragment                mFragment;                  //本フラグメント
    private Context                 mContext;                   //コンテキスト（親アクティビティ）
    private AppDatabase             mDB;                        //DB
    private List<TaskTable>         mTaskList;                  //「やること」リスト
    private TaskRecyclerAdapter     mTaskAdapter;               //「やること」表示アダプタ
    private int                     _mDeletedTaskPos;           //削除対象のID
    private AsyncTaskTableOperaion.TaskOperationListener
            mTaskListener;              //「やること」操作リスナー


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //自身のフラグメントを保持
        mFragment = getParentFragmentManager().getFragments().get(0);
        //設定レイアウト
        mRootLayout = inflater.inflate(R.layout.fragment_task_manager, container, false);
        //親アクティビティのコンテキスト
        mContext = mRootLayout.getContext();
        //DB操作インスタンスを取得
        mDB = AppDatabaseSingleton.getInstance(mContext);
        //親アクティビティ
        mParentActivity = (MainActivity) getActivity();
        //「やること」操作リスナー
        mTaskListener = (AsyncTaskTableOperaion.TaskOperationListener) mFragment;

        //削除待ちの「やること」-リストIndex
        _mDeletedTaskPos = NOT_DELETE_WAITING;

        //現在登録されている「やること」を表示
        //displayTask();
        mTaskList = mParentActivity.getTaskData();
        this.displayTaskData();

        // FloatingActionButton
        FloatingActionButton fab = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_addTask);
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
                FragmentManager transaction = getParentFragmentManager();

                //ダイアログを生成
                DialogFragment dialog = new CreateTaskDialog(mTaskListener);
                Log.i("test", "CreateTaskDialog");
                dialog.setArguments(bundle);
                dialog.show(transaction, "CreateTask");
            }
        });


        return mRootLayout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }



    /*
     * 「やること」編集ダイアログの生成
     */
    private void createEditTaskDialog(View view) {

        //「やること」情報
        String taskName = ((TextView) view.findViewById(R.id.tv_taskName)).getText().toString();
        String taskTimeStr = ((TextView) view.findViewById(R.id.tv_taskTime)).getText().toString();

        //taskTimeStr = taskTimeStr.replace(" min", "");
        int taskTime = Integer.parseInt(taskTimeStr);

        //ダイアログへ渡すデータを設定
        Bundle bundle = new Bundle();
        bundle.putString("TaskName", taskName);
        bundle.putInt("TaskTime", taskTime);

        //FragmentManager生成
        FragmentManager transaction = getParentFragmentManager();

        //ダイアログを生成
        DialogFragment dialog = new CreateTaskDialog(mTaskListener);
        dialog.setArguments(bundle);
        dialog.show(transaction, "UpdateTask");
    }

    /*
     * 「やること」リスト検索
     */
    private int getIdTaskList(String task, int taskTime) {

        int i = 0;
        for (TaskTable taskInfo : mTaskList) {

            //「やること」「やること時間」が一致するデータを発見した場合
            if ((task == taskInfo.getTaskName()) && (taskTime == taskInfo.getTaskTime())) {
                return i;
            }
            i++;
        }

        //データなし
        return -1;
    }

    /*
     * 「やること」データを表示エリアにセット
     */
    public void displayTaskData() {

        //レイアウトからリストビューを取得
        RecyclerView rv_task  = (RecyclerView) mRootLayout.findViewById(R.id.rv_taskList);
        //グリッド表示の設定
        rv_task.setLayoutManager(new GridLayoutManager(mContext, 2));
        //アダプタの生成
        Log.i("test", "dash  pre TaskRecyclerAdapter");
        mTaskAdapter = new TaskRecyclerAdapter(mContext, mTaskList, TaskRecyclerAdapter.SETTING.LIST);
        Log.i("test", "dash TaskRecyclerAdapter");

        //リスナー設定
        mTaskAdapter.setOnItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createEditTaskDialog(view);
            }
        });

        //アダプタの設定
        rv_task.setAdapter(mTaskAdapter);

        //ドラッグアンドドロップ、スワイプの設定
        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback( ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                        ItemTouchHelper.LEFT ){
                    @Override
                    public boolean onMove(@NonNull RecyclerView            recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        //並び替えは要検討

                        //！getAdapterPosition()←非推奨
                        final int fromPos = viewHolder.getAdapterPosition();
                        final int toPos   = target.getAdapterPosition();
                        //アイテム移動を通知
                        mTaskAdapter.notifyItemMoved(fromPos, toPos);
                        Log.i("test", "onMove " + fromPos + " " + toPos);

                        return true;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                        if( _mDeletedTaskPos != NOT_DELETE_WAITING ){
                            //削除待ちのものがあるなら、何もしない
                            return;
                        }

                        //-- DBから削除
                        int i = viewHolder.getAdapterPosition();
                        String taskName = mTaskList.get(i).getTaskName();
                        int    taskTime = mTaskList.get(i).getTaskTime();

                        new AsyncTaskTableOperaion(mDB, mTaskListener, AsyncTaskTableOperaion.DB_OPERATION.DELETE, taskName, taskTime).execute();

                        //削除アイテムを保持
                        _mDeletedTaskPos = viewHolder.getAdapterPosition();

                        //※アダプタへの削除通知は、DBの削除完了後、行う
                    }
                }
        );

        //リサイクラービューをアタッチ
        helper.attachToRecyclerView(rv_task);

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
        Toast toast = new Toast(mContext);
        toast.setText(message);
        //toast.setGravity(Gravity.CENTER, 0, 0);   //E/Toast: setGravity() shouldn't be called on text toasts, the values won't be used
        toast.show();

        if( code == -1 ){
            //登録済みなら、ここで終了
            return;
        }

        //生成された「やること」をリストに追加
        mTaskList.add( taskTable );
        //アダプタに変更を通知
        mTaskAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSuccessTaskDelete(String task, int taskTime) {

        //リストから削除
        mTaskList.remove(_mDeletedTaskPos);
        //ビューにアイテム削除を通知
        mTaskAdapter.notifyItemRemoved(_mDeletedTaskPos);
        //削除待ちなしに戻す
        _mDeletedTaskPos = NOT_DELETE_WAITING;

        //トーストの生成
        Toast toast = new Toast(mContext);
        toast.setText("削除しました");
        toast.show();
    }

    /* -------------------
     * インターフェース：「やること」
     *   「やること」の編集
     */
    @Override
    public void onSuccessEditTask(String preTaskName, int preTaskTime, TaskTable updatedTask) {
        //更新されたリストのIndexを取得
        int i = getIdTaskList(preTaskName, preTaskTime);

        //フェールセーフ
        if( i == -1 ){
            //見つからなければ、何もしない
            Log.i("failsafe", "onSuccessTaskUpdate couldn't found");
            return;
        }

        //リストを更新
        mTaskList.set(i, updatedTask);
        //アダプタに変更を通知
        mTaskAdapter.notifyDataSetChanged();

        //トーストの生成
        Toast toast = new Toast(mContext);
        toast.setText("更新しました");
        toast.show();
    }


}