package com.stacktime.supportpreparation.ui.taskManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.stacktime.supportpreparation.AppDatabase;
import com.stacktime.supportpreparation.AppDatabaseSingleton;
import com.stacktime.supportpreparation.AsyncTaskTableOperaion;
import com.stacktime.supportpreparation.CreateTaskDialog;
import com.stacktime.supportpreparation.MainActivity;
import com.stacktime.supportpreparation.R;
import com.stacktime.supportpreparation.ResourceManager;
import com.stacktime.supportpreparation.TaskArrayList;
import com.stacktime.supportpreparation.TaskRecyclerAdapter;
import com.stacktime.supportpreparation.TaskTable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

/*
 * タスク画面フラグメント
 */
public class TaskManagerFragment extends Fragment implements AsyncTaskTableOperaion.TaskOperationListener {

    //定数
    public final static int             TASK_COLUMN = 2;            //やること表示列数

    //フィールド変数
    private MainActivity                mParentActivity;            //親アクティビティ
    private View                        mRootLayout;                //本フラグメントに設定しているレイアウト
    private Context                     mContext;                   //コンテキスト（親アクティビティ）
    private AppDatabase                 mDB;                        //DB
    private TaskArrayList<TaskTable>    mTaskList;                  //「やること」リスト
    private TaskRecyclerAdapter         mTaskAdapter;               //「やること」表示アダプタ
    private AsyncTaskTableOperaion.TaskOperationListener
                                        mTaskListener;              //「やること」操作リスナー

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //設定レイアウト
        mRootLayout = inflater.inflate(R.layout.fragment_task_manager, container, false);
        //親アクティビティのコンテキスト
        mContext = mRootLayout.getContext();
        //DB操作インスタンスを取得
        mDB = AppDatabaseSingleton.getInstance(mContext);
        //親アクティビティ
        mParentActivity = (MainActivity) getActivity();
        //「やること」操作リスナー
        Fragment fragment = getParentFragmentManager().getFragments().get(0);
        mTaskListener = (AsyncTaskTableOperaion.TaskOperationListener) fragment;

        //ガイドクローズ
        mParentActivity.closeGuide();

        //snackbarクローズ
        mParentActivity.dismissSnackbar();

        //Admod非表示
        mParentActivity.setVisibilityAdmod( View.GONE );
        //ヘルプボタン表示(タイマ画面でグラフを閉じずに画面移動された時の対策)
        mParentActivity.setVisibilityHelpBtn(View.VISIBLE);

        //現在登録されている「やること」
        mTaskList = mParentActivity.getTaskData();

        //やること表示
        setupTaskList();

        //「やること」追加Fab
        FloatingActionButton fab = mRootLayout.findViewById(R.id.fab_addTask);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //-- 「やること」追加ダイアログの生成
                //Bundle生成
                Bundle bundle = new Bundle();
                //FragmentManager生成
                FragmentManager transaction = getParentFragmentManager();

                //ダイアログを生成
                DialogFragment dialog = new CreateTaskDialog(mTaskListener);
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
        String taskName    = ((TextView) view.findViewById(R.id.tv_taskName)).getText().toString();
        String taskTimeStr = ((TextView) view.findViewById(R.id.tv_taskTime)).getText().toString();

        int taskTime = Integer.parseInt(taskTimeStr);

        //ダイアログへ渡すデータを設定
        Bundle bundle = new Bundle();
        bundle.putString(ResourceManager.KEY_TASK_NAME, taskName);
        bundle.putInt(ResourceManager.KEY_TASK_TIME, taskTime);

        //FragmentManager生成
        FragmentManager transaction = getParentFragmentManager();

        //ダイアログを生成
        DialogFragment dialog = new CreateTaskDialog(mTaskListener);
        dialog.setArguments(bundle);
        dialog.show(transaction, "UpdateTask");
    }

    /*
     * 「やること」データを表示エリアにセット
     */
    public void setupTaskList() {

        //レイアウトからリストビューを取得
        RecyclerView rv_task = mRootLayout.findViewById(R.id.rv_taskList);
        //グリッド表示の設定
        rv_task.setLayoutManager(new GridLayoutManager(mContext, TASK_COLUMN));

        //-- アダプタの設定は、サイズが確定してから行う
        // ビューツリー描画時に呼ばれるリスナーの設定
        rv_task.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {

                //本リスナーを削除（何度も処理する必要はないため）
                rv_task.getViewTreeObserver().removeOnPreDrawListener(this);

                //RecyclerViewの横幅分割
                int size = rv_task.getWidth() / TASK_COLUMN;

                //アダプタの生成
                mTaskAdapter = new TaskRecyclerAdapter(mContext, mTaskList, TaskRecyclerAdapter.SHOW_KIND.LIST, size, size);

                //リスナー設定
                mTaskAdapter.setOnItemClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        createEditTaskDialog(view);
                    }
                });

                //アダプタの設定
                rv_task.setAdapter(mTaskAdapter);

                //レイアウト調整（1行目の上部にスペースを設定）
                rv_task.addItemDecoration(new TaskListItemDecoration());

                //描画を中断するため、false
                return false;
            }
        });

        //ドラッグアンドドロップ、スワイプの設定
        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback( ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                        ItemTouchHelper.LEFT ){
                    @Override
                    public boolean onMove(@NonNull RecyclerView            recyclerView,
                                          @NonNull RecyclerView.ViewHolder dragged,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return true;
                    }

                    @SuppressLint("ResourceAsColor")
                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                        //スワイプされたデータ
                        final int       adapterPosition = viewHolder.getAbsoluteAdapterPosition();
                        final TaskTable deletedTask     = mTaskList.get(adapterPosition);

                        //スナックバー
                        mParentActivity.showSnackbar(
                                //para1:UNDO押下時の動作
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        //UNDOが選択された場合、削除されたアイテムを元の位置に戻す
                                        mTaskList.addTask(adapterPosition, deletedTask);

                                        //アダプタに変更を通知
                                        if( mTaskList.size() == 1 ){
                                            //空のデータがあるため、1件目の場合は変更通知
                                            mTaskAdapter.notifyItemChanged(0);
                                        } else {
                                            mTaskAdapter.notifyItemInserted(adapterPosition);
                                            rv_task.scrollToPosition(adapterPosition );
                                        }
                                    }
                                },
                                //para2:スナックバー消失時の動作
                                new Snackbar.Callback() {
                                    @Override
                                    public void onDismissed(Snackbar snackbar, int event) {
                                        super.onDismissed(snackbar, event);

                                        //アクションバー押下以外で閉じられた場合
                                        if (event != DISMISS_EVENT_ACTION) {
                                            //DBから削除
                                            int pid = deletedTask.getId();
                                            new AsyncTaskTableOperaion(mDB, mTaskListener, AsyncTaskTableOperaion.DB_OPERATION.DELETE, pid).execute();
                                        }
                                    }
                                }
                        );

                        //リストから削除し、アダプターへ通知
                        mTaskList.remove(adapterPosition);
                        mTaskAdapter.notifyItemRemoved(adapterPosition);

                        //０件なら、空のデータをリストに入れておく
                        //※選択エリアのサイズを確保するため
                        mTaskList.addEmpty();
                    }
                }
        );

        //リサイクラービューをアタッチ
        helper.attachToRecyclerView(rv_task);
    }

    /*
     * やることリスト レイアウト調整用
     */
    private class TaskListItemDecoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position < TASK_COLUMN) {
                //1行目の上部にスペースを設定
                outRect.top = (mParentActivity.getHelpButtonHeight() * 2);
            }
        }
    }

    /* -------------------
     * インターフェース：「やること」
     *   「やること」の表示
     */
    @Override
    public void onSuccessTaskRead(TaskArrayList<TaskTable> taskList) {
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onSuccessTaskCreate(Integer code, TaskTable taskTable) {
        //-- 生成した「やること」を表示

        //戻り値に応じてトースト表示
        if(code.equals(AsyncTaskTableOperaion.REGISTERED)){
            Toast.makeText(mContext, R.string.toast_data_registered , Toast.LENGTH_SHORT).show();
            return;
        }

        //生成された「やること」をリストに追加
        mTaskList.addTask( taskTable );

        int addIdx = mTaskList.getLastIdx();

        //アダプタに変更を通知
        if( addIdx == 0 ){
            //空のデータがあるため、1件目の場合は変更通知
            mTaskAdapter.notifyItemChanged(0);
        } else {
            mTaskAdapter.notifyItemInserted(addIdx);
        }

        //追加された位置へスクロール
        //RecyclerView rv_task  = mRootLayout.findViewById(R.id.rv_taskList);
        //rv_task.scrollToPosition( addIdx );
    }

    @Override
    public void onSuccessTaskDelete(String task, int taskTime) {

        //０件なら、空のデータをリストに入れておく
        //※選択エリアのサイズを確保するため
        //mTaskList.addEmpty();
    }

    /* -------------------
     * インターフェース：「やること」
     *   「やること」の編集
     */
    @Override
    public void onSuccessEditTask(Integer code, String preTaskName, int preTaskTime, TaskTable updatedTask) {
        //更新されたリストのIndexを取得
        int i = mTaskList.getIdxByTaskInfo(preTaskName, preTaskTime);
        if( i == TaskArrayList.NO_DATA ){
            //--フェールセーフ
            //見つからなければ、何もしない
            Log.i("failsafe", "onSuccessTaskUpdate couldn't found");
            return;
        }

        //戻り値に応じてトースト表示
        if(code.equals(AsyncTaskTableOperaion.REGISTERED)){
            Toast.makeText(mContext, R.string.toast_data_registered , Toast.LENGTH_SHORT).show();
            return;
        }

        //リストを更新
        mTaskList.set(i, updatedTask);
        //アダプタに変更を通知
        mTaskAdapter.notifyItemChanged(i);

        //トーストの生成
        Toast.makeText(mContext, R.string.toast_updated , Toast.LENGTH_SHORT).show();
    }


}