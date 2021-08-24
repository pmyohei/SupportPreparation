package com.example.supportpreparation;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/*
 * RecyclerViewアダプター：「やること」用
 */
public class TaskRecyclerAdapter extends RecyclerView.Adapter<TaskRecyclerAdapter.TaskViewHolder> {

    //-- アダプタ設定対象
    public enum SETTING {
        CREATE,            //「やること」生成エリア
        SELECT,            //「やること」選択エリア
    }

    private List<TaskTable>             mData;
    private Context                     mContext;
    private View.OnClickListener        clickListener;
    private View.OnLongClickListener    longListener;
    private SETTING                     mSetting;
    private int                         mItemWidth;

    /*
     * ViewHolder：リスト内の各アイテムのレイアウトを含む View のラッパー
     * (固有のためインナークラスで定義)
     */
    class TaskViewHolder extends RecyclerView.ViewHolder {

        private TextView tv_pid;            //Pid
        private TextView tv_taskName;       //表示内容
        private TextView tv_taskTime;
        private LinearLayout ll_taskInfo ;  //リスナー設定ビュー

        /*
         * コンストラクタ
         */
        public TaskViewHolder(View itemView) {
            super(itemView);
            tv_pid      = (TextView) itemView.findViewById(R.id.tv_pid);
            tv_taskName = (TextView) itemView.findViewById(R.id.tv_taskName);
            tv_taskTime = (TextView) itemView.findViewById(R.id.tv_taskTime);
            ll_taskInfo = (LinearLayout)itemView.findViewById(R.id.ll_taskInfo);
        }
    }

    /*
     * コンストラクタ
     */
    public TaskRecyclerAdapter(Context context, List<TaskTable> data, SETTING setting) {
        mData     = data;
        mContext  = context;
        mSetting  = setting;

        //設定メソッドがコールされるまで、０とする
        mItemWidth = 0;
    }

    /*
     * コンストラクタ
     */
    public TaskRecyclerAdapter(Context context, List<TaskTable> data, SETTING setting, int width) {
        mData     = data;
        mContext  = context;
        mSetting  = setting;
        mItemWidth = width;
    }

    /*
     * ここで返した値が、onCreateViewHolder()の第２引数になる
     */
    @Override
    public int getItemViewType(int position) {
        //Log.i("test", "getItemViewType id=" + mData.get(position).getId());
        return mData.get(position).getTaskTime();
    }

    /*
     *　ViewHolderの生成
     */
    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        //レイアウトIDを取得
        int id = getLayoutId(viewType);

        //表示レイアウトの設定
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(id, viewGroup, false);

        //
        if( mItemWidth != 0 ){
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

            Log.i("test", "layoutParams height=" + layoutParams.height);

            layoutParams.width = mItemWidth;
            view.setLayoutParams(layoutParams);
        }

        Log.i("test", "layoutParams root=");

        return new TaskViewHolder(view);
    }


    /*
     * ViewHolderの設定
     */
    @Override
    public void onBindViewHolder(TaskViewHolder viewHolder, final int i) {

        //文字列変換
        String pidStr  = Integer.toString( mData.get(i).getId() );
        String timeStr = Integer.toString( mData.get(i).getTaskTime() );

        //データ設定
        viewHolder.tv_pid.setText(pidStr);
        viewHolder.tv_taskName.setText(mData.get(i).getTaskName());
        viewHolder.tv_taskTime.setText(timeStr);

        //クリック処理
        if( clickListener != null ){
            viewHolder.ll_taskInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onClick(view);
                }
            });
        }

        //ドラッグ処理
        if( longListener != null ){
            viewHolder.ll_taskInfo.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    longListener.onLongClick(view);
                    return true;
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
     * アイテム毎のドラッグリスナー
     */
    public void setItemWidth(int width) {
        mItemWidth = width;
    }

    /*
     * アイテム毎のタッチリスナー
     */
    public void setOnItemClickListener(View.OnClickListener listener) {
        clickListener = listener;
    }

    /*
     * アイテム毎のドラッグリスナー
     */
    public void setOnItemLongClickListener(View.OnLongClickListener listener) {
        longListener = listener;
    }

    /*
     * カラーIDの取得
     */
    private int getLayoutId(int time){

        //指定なしなら
        int id;
        if( mSetting == SETTING.CREATE ){
            id = getLayoutIdForCreate(time);
        } else {
            id = getLayoutIdForSelect(time);
        }

        return id;
    }

    /*
     * カラーIDの取得(「やること」生成エリア用)
     */
    private int getLayoutIdForCreate(int time){

        int id;

        if( time < 5 ){
            id = R.layout.item_task_very_short;
        } else if( time < 10 ){
            id = R.layout.item_task_short;
        } else if( time < 30 ){
            id = R.layout.item_task_normal;
        } else if( time < 60 ){
            id = R.layout.item_task_long;
        } else {
            id = R.layout.item_task_very_long;
        }
        return id;
    }

    /*
     * カラーIDの取得(「やること」選択エリア用)
     */
    private int getLayoutIdForSelect(int time){

        int id;

        if( time < 5 ){
            id = R.layout.item_task_for_select_very_short;
        } else if( time < 10 ){
            id = R.layout.item_task_for_select_short;
        } else if( time < 30 ){
            id = R.layout.item_task_for_select_normal;
        } else if( time < 60 ){
            id = R.layout.item_task_for_select_long;
        } else {
            id = R.layout.item_task_for_select_very_long;
        }
        return id;
    }
}

