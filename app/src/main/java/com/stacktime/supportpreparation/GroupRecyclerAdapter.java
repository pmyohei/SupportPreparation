package com.stacktime.supportpreparation;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.stacktime.supportpreparation.R;
import com.stacktime.supportpreparation.ui.groupManager.GroupManagerFragment;
import com.google.android.material.snackbar.Snackbar;

/*
 * RecyclerAdapter：グループ用
 */
public class GroupRecyclerAdapter extends RecyclerView.Adapter<GroupRecyclerAdapter.ViewHolder> {

    //フィールド変数
    private final GroupArrayList<GroupTable>                      mData;
    private final Context                                         mContext;
    private final int                                             mItemHeight;
    private final MainActivity                                    mParentActivity;
    private final AsyncGroupTableOperaion.GroupOperationListener  mDBListener;
    private View.OnClickListener                                  mGroupNameClickListener;
    private View.OnTouchListener                                  mTaskTouchListener;

    /*
     * ViewHolder：リスト内の各アイテムのレイアウトを含む View のラッパー
     * (固有のためインナークラスで定義)
     */
    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView        tv_groupPid;
        private final LinearLayout    ll_group;
        private final TextView        tv_groupName;
        private final RecyclerView    rv_taskInGroup;

        /*
         * コンストラクタ
         */
        public ViewHolder(View itemView) {
            super(itemView);

            //ビュー取得
            tv_groupPid    = itemView.findViewById(R.id.tv_pid);
            ll_group       = itemView.findViewById(R.id.ll_group);
            tv_groupName   = itemView.findViewById(R.id.tv_groupName);
            rv_taskInGroup = itemView.findViewById(R.id.rv_taskInGroup);
        }
    }

    /*
     * コンストラクタ
     */
    public GroupRecyclerAdapter(Context context, GroupArrayList<GroupTable> groupList,
                                AsyncGroupTableOperaion.GroupOperationListener dbListener, int height,
                                MainActivity activity) {
        mContext          = context;
        mData             = groupList;
        mItemHeight       = height;
        mDBListener       = dbListener;
        mParentActivity   = activity;
    }


    /*
     * ここで返した値が、onCreateViewHolder()の第２引数になる
     */
    @Override
    public int getItemViewType(int position) {

        if (mData.get(position) == null) {
            //フェールセーフ
            return 0;
        }

        return mData.get(position).getTotalTime();
    }

    /*
     *　ViewHolderの生成
     */
    @Override
    public GroupRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        //表示レイアウトの設定
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.outer_group, viewGroup, false);

        //高さの設定
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = mItemHeight;
        view.setLayoutParams(layoutParams);

        //空データなら、非表示
        if( viewType == ResourceManager.INVALID_MIN ){
            LinearLayout ll_group = view.findViewById( R.id.ll_group );
            ll_group.setVisibility( View.INVISIBLE );
        }

        return new ViewHolder(view);
    }

    /*
     * ViewHolderの設定
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int i) {

        int totalTime = mData.get(i).getTotalTime();
        if( totalTime == ResourceManager.INVALID_MIN ){
            //空データなら設定不要
            return;
        }

        //文字列変換
        int groupPid = mData.get(i).getId();
        String pidStr = Integer.toString(groupPid);

        //ビューの設定
        viewHolder.tv_groupPid.setText(pidStr);
        viewHolder.tv_groupName.setText(mData.get(i).getGroupName());

        //グループ内「やること」の表示設定
        setupTaskInGroup(viewHolder, i, groupPid);

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
        return mData.size();
    }

    /*
     * グループのやることを設定
     */
    @SuppressLint("ClickableViewAccessibility")
    public void setupTaskInGroup(ViewHolder viewHolder, int idx, int groupPid) {

        //グループ内のやること／アダプタ
        TaskRecyclerAdapter adapter = mData.get(idx).getTaskAdapter();
        TaskArrayList<TaskTable> taskInGroupList = mData.get(idx).getTaskInGroupList();

        //レイアウトマネージャの生成・設定
        viewHolder.rv_taskInGroup.setLayoutManager( new GridLayoutManager(mContext, GroupManagerFragment.DIV_GROUP_IN_TASK) );

        //アダプタの設定
        viewHolder.rv_taskInGroup.setAdapter(adapter);

        //ドラッグ、スワイプの設定（グループ内のやること）
        SimpleCallbackTaskInGroup callBack = new SimpleCallbackTaskInGroup(viewHolder.rv_taskInGroup);
        ItemTouchHelper helper             = new ItemTouchHelper( callBack );

        //リサイクラービューをヘルパーにアタッチ
        helper.attachToRecyclerView(viewHolder.rv_taskInGroup);

        //「やること」タッチリスナー設定
        if (mTaskTouchListener != null) {
            viewHolder.rv_taskInGroup.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {

                    //設定されたリスナー処理を行う
                    return mTaskTouchListener.onTouch(view, event);
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
     * ドラッグ＆ドロップリスナー
     *　　グループ内への「やること」のドロップを検知する
     */
    private class DragGroupListener implements View.OnDragListener {

        private final TaskRecyclerAdapter         mAdapter;       //本コールバックのアタッチ先のRecyclerView
        private final int                         mGroupPid;      //グループのプライマリーキー
        private final TaskArrayList<TaskTable>    mTaskInGroup;   //グループに割り当てられた「やること」

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
                    View dragView        = (View) dragEvent.getLocalState();
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
    private class SimpleCallbackTaskInGroup extends ItemTouchHelper.SimpleCallback {

        private final RecyclerView mrv_taskInGroup;         //本コールバックのアタッチ先RecyclerView

        public SimpleCallbackTaskInGroup(RecyclerView recyclerView) {
            super(0, ItemTouchHelper.LEFT);

            mrv_taskInGroup = recyclerView;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            //対象アダプタ
            TaskRecyclerAdapter adapter = (TaskRecyclerAdapter)mrv_taskInGroup.getAdapter();

            //対象のアダプタと同じデータを検索
            int i = 0;
            for( GroupTable group: mData ){
                if( adapter == group.getTaskAdapter() ){
                    break;
                }
                i++;
            }

            //見つからなければ何もしない
            if( i == mData.size() ){
                Log.i( "failsafe", "not found swiped data" );
                return;
            }

            //対象データ
            TaskArrayList<TaskTable> taskInGroup = mData.get(i).getTaskInGroupList();
            int gPid = mData.get(i).getId();

            //スワイプされたデータ
            final int       adapterPosition = viewHolder.getAbsoluteAdapterPosition();
            final TaskTable deletedTask = taskInGroup.get(adapterPosition);

            //スナックバー
            mParentActivity.showSnackbar(
                    //UNDO押下時の動作
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //UNDOが選択された場合、削除されたアイテムを元の位置に戻す
                            taskInGroup.add(adapterPosition, deletedTask);
                            adapter.notifyItemInserted(adapterPosition );
                            mrv_taskInGroup.scrollToPosition(adapterPosition );
                        }
                    },
                    new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            super.onDismissed(snackbar, event);

                            //アクションバー押下以外で閉じられた場合
                            if (event != DISMISS_EVENT_ACTION) {

                                //DBから削除
                                AppDatabase db = AppDatabaseSingleton.getInstance(mContext);
                                new AsyncGroupTableOperaion(db, mDBListener, AsyncGroupTableOperaion.DB_OPERATION.REMOVE_TASK, gPid, adapterPosition).execute();
                            }
                        }
                    }
            );

            //リストから削除し、アダプターへ通知
            taskInGroup.remove(adapterPosition);
            adapter.notifyItemRemoved(adapterPosition);
        }
    }
}
