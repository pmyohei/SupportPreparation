package com.example.supportpreparation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/*
 * RecyclerViewアダプター：「やることグループ」用
 */
public class GroupRecyclerAdapter extends RecyclerView.Adapter<GroupRecyclerAdapter.ViewHolder> {

    private GroupArrayList<GroupTable>                      mGroupList;
    private Context                                         mContext;
    private int                                             mItemHeight;
    private BottomNavigationView                            mBNV;
    private ConstraintLayout                                mcl_mainContainer;
    private View.OnClickListener                            mGroupNameClickListener;
    private View.OnTouchListener                            mTaskTouchListener;
    private View.OnDragListener                             mDragListener;
    private AsyncGroupTableOperaion.GroupOperationListener  mDBListener;

    /*
     * ViewHolder：リスト内の各アイテムのレイアウトを含む View のラッパー
     * (固有のためインナークラスで定義)
     */
    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView        tv_groupPid;
        private LinearLayout    ll_group;
        private TextView        tv_groupName;
        private RecyclerView    rv_taskInGroup;

        /*
         * コンストラクタ
         */
        public ViewHolder(View itemView) {
            super(itemView);

            //ビュー取得
            tv_groupPid = (TextView) itemView.findViewById(R.id.tv_pid);
            ll_group = (LinearLayout) itemView.findViewById(R.id.ll_group);
            tv_groupName = (TextView) itemView.findViewById(R.id.tv_groupName);
            rv_taskInGroup = (RecyclerView) itemView.findViewById(R.id.rv_taskInGroup);
        }
    }


    /*
     * コンストラクタ
     */
    public GroupRecyclerAdapter(Context context, GroupArrayList<GroupTable> groupList,
                                AsyncGroupTableOperaion.GroupOperationListener dbListener, int height,
                                BottomNavigationView bnv, ConstraintLayout cl_mainContainer) {
        mContext                = context;
        mGroupList              = groupList;
        mItemHeight             = height;
        mDBListener             = dbListener;
        mBNV                    = bnv;
        mcl_mainContainer       = cl_mainContainer;
    }

    /*
     *　ViewHolderの生成
     */
    @Override
    public GroupRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        //表示レイアウトの設定
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.outer_group, viewGroup, false);

        //高さの設定
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = mItemHeight;
        view.setLayoutParams(layoutParams);

        return new ViewHolder(view);
    }

    /*
     * ViewHolderの設定
     */
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int i) {

        Log.i("test", "group adapter onBindViewHolder i=" + i);

        //文字列変換
        int groupPid = mGroupList.get(i).getId();
        String pidStr = Integer.toString(groupPid);

        //ビューの設定
        viewHolder.tv_groupPid.setText(pidStr);
        viewHolder.tv_groupName.setText(mGroupList.get(i).getGroupName());

        //-- グループに紐づいた「やること」

        //グループ内「やること」の表示設定
        setTaskInGroup(viewHolder, i, groupPid);

        //グループ名クリックリスナー設定
        if (mGroupNameClickListener != null) {
            viewHolder.tv_groupName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mGroupNameClickListener.onClick(view);
                }
            });
        }
    }

    /*
     * 表示データ数の取得
     */
    @Override
    public int getItemCount() {
        //表示データ数を返す
        return mGroupList.size();
    }

    /*
     * グループのやることを設定
     */
    @SuppressLint("ClickableViewAccessibility")
    public void setTaskInGroup(ViewHolder viewHolder, int idx, int groupPid) {

        //レイアウトマネージャの生成・設定
        LinearLayoutManager ll_manager = new LinearLayoutManager(mContext);
        viewHolder.rv_taskInGroup.setLayoutManager(ll_manager);

        //「やること」の高さ
        int taskHeight = mItemHeight / 3;

        //グループ内のやること／アダプタ
        TaskRecyclerAdapter adapter = mGroupList.get(idx).getTaskAdapter();
        TaskArrayList<TaskTable> taskInGroupList = mGroupList.get(idx).getTaskInGroupList();

        Log.i("test", "idx=" + idx);

        //アダプタの設定
        viewHolder.rv_taskInGroup.setAdapter(adapter);

        //ドラッグ、スワイプの設定
        ItemTouchHelper helper = new ItemTouchHelper( new SimpleCallback(viewHolder.rv_taskInGroup, adapter, taskInGroupList, groupPid) );
        //リサイクラービューをヘルパーにアタッチ
        helper.attachToRecyclerView(viewHolder.rv_taskInGroup);

        //「やること」タッチリスナー設定
        if (mTaskTouchListener != null) {
            viewHolder.rv_taskInGroup.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    mTaskTouchListener.onTouch(view, event);
                    return false;
                }
            });
        }

        //「やること」ドロップ時のリスナー設定
        DragGroupListener listener = new DragGroupListener(adapter, taskInGroupList, groupPid);
        viewHolder.ll_group.setOnDragListener(listener);
    }

    /*
     * リスナー設定：タッチリスナー
     */
    public void setOnGroupNameClickListener(View.OnClickListener listener) {
        mGroupNameClickListener = listener;
    }

    /*
     * リスナー設定：タッチリスナー
     */
    public void setOnTaskTouchListener(View.OnTouchListener listener) {
        mTaskTouchListener = listener;
    }


    /*
     * リスナー設定：ドラッグリスナー（ドロップされた時の動作）
     */
    public void setOnItemDragListener(View.OnDragListener listener) {
        mDragListener = listener;
    }


    /*
     * ドラッグ＆ドロップリスナー
     *　　グループ内への「やること」のドロップを検知する
     */
    private class DragGroupListener implements View.OnDragListener {

        private TaskRecyclerAdapter         mAdapter;       //本コールバックのアタッチ先のRecyclerView
        private int                         mGroupPid;      //グループのプライマリーキー
        private TaskArrayList<TaskTable>    mTaskInGroup;   //グループに割り当てられた「やること」

        /*
         * コンストラクタ
         */
        public DragGroupListener(TaskRecyclerAdapter adapter, TaskArrayList<TaskTable> taskInGroup, int groupPid) {
            mAdapter     = adapter;
            mGroupPid    = groupPid;
            mTaskInGroup = taskInGroup;
        }

        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            switch (dragEvent.getAction()) {
                //ドロップ
                case DragEvent.ACTION_DROP: {
                    //--グループに「やること」がドロップされたとき

                    //ドラッグしたビューからデータを取得
                    View dragView = (View) dragEvent.getLocalState();
                    TextView tv_pid      = dragView.findViewById(R.id.tv_pid);
                    TextView tv_taskName = dragView.findViewById(R.id.tv_taskName);
                    TextView tv_taskTime = dragView.findViewById(R.id.tv_taskTime);

                    int pid      = Integer.parseInt(tv_pid.getText().toString());
                    int taskTime = Integer.parseInt(tv_taskTime.getText().toString());
                    mTaskInGroup.add(new TaskTable(pid, tv_taskName.getText().toString(), taskTime));

                    //DB取得
                    AppDatabase db = AppDatabaseSingleton.getInstanceNotFirst();
                    //グループに「やること」を追加
                    new AsyncGroupTableOperaion(db, mDBListener, AsyncGroupTableOperaion.DB_OPERATION.ADD_TASK, mGroupPid, pid).execute();

                    //アダプタへ通知
                    mAdapter.notifyDataSetChanged();

                    break;
                }
                //ドラッグ終了時
                case DragEvent.ACTION_DRAG_ENDED: {
                    return true;
                }
            }
            return true;
        }
    }

    /*
     * ドラッグ／スワイプ リスナー
     *　　グループ内「やること」のドラッグ／スワイプ処理
     */
    private class SimpleCallback extends ItemTouchHelper.SimpleCallback {

        private RecyclerView                mRecyclerView;  //本コールバックのアタッチ先のRecyclerView
        private TaskRecyclerAdapter         mAdapter;       //本コールバックのアタッチ先のRecyclerView
        private int                         mGroupPid;      //グループのプライマリーキー
        private TaskArrayList<TaskTable>    mTaskInGroup;   //グループに割り当てられた「やること」

        public SimpleCallback(RecyclerView recyclerView, TaskRecyclerAdapter adapter, TaskArrayList<TaskTable> taskInGroup, int groupPid) {
            super(0, ItemTouchHelper.LEFT);

            mRecyclerView   = recyclerView;
            mAdapter        = adapter;
            mTaskInGroup    = taskInGroup;
            mGroupPid       = groupPid;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
                            /*
                            //！getAdapterPosition()←非推奨
                            final int fromPos = viewHolder.getAdapterPosition();
                            final int toPos   = target.getAdapterPosition();
                            //アイテム移動を通知
                            mGroupAdapter.notifyItemMoved(fromPos, toPos);
                            Log.i("test", "onMove " + fromPos + " " + toPos);
                            */
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            //スワイプされたデータ
            final int       adapterPosition = viewHolder.getAdapterPosition();
            final TaskTable deletedTask     = mTaskInGroup.get(adapterPosition);

            //UNDOメッセージの表示
            Snackbar snackbar = Snackbar
                    .make(mcl_mainContainer, R.string.snackbar_delete, Snackbar.LENGTH_LONG)
                    //アクションボタン押下時の動作
                    .setAction(R.string.snackbar_undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //UNDOが選択された場合、削除されたアイテムを元の位置に戻す
                            mTaskInGroup.add(adapterPosition, deletedTask);
                            mAdapter.notifyItemInserted(adapterPosition );
                            mRecyclerView.scrollToPosition(adapterPosition );
                        }
                    })
                    //スナックバークローズ時の動作
                    .addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            super.onDismissed(snackbar, event);

                            //アクションバー押下以外で閉じられた場合
                            if (event != DISMISS_EVENT_ACTION) {
                                //DBから削除
                                AppDatabase db = AppDatabaseSingleton.getInstance(mContext);
                                new AsyncGroupTableOperaion(db, mDBListener, AsyncGroupTableOperaion.DB_OPERATION.REMOVE_TASK, mGroupPid, adapterPosition).execute();
                            }
                        }
                    })
                    //下部ナビゲーションの上に表示させるための設定
                    .setAnchorView(mBNV)
                    .setBackgroundTint(mContext.getResources().getColor(R.color.basic))
                    .setTextColor(mContext.getResources().getColor(R.color.white))
                    .setActionTextColor(mContext.getResources().getColor(R.color.white));

            //表示
            snackbar.show();

            //リストから削除し、アダプターへ通知
            mTaskInGroup.remove(adapterPosition);
            mAdapter.notifyItemRemoved(adapterPosition);
        }
    }
}
