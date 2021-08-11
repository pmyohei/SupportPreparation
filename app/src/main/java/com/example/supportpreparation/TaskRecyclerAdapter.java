package com.example.supportpreparation;

import static com.example.supportpreparation.R.color.a_blue_1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/*
 * RecyclerViewアダプター：「やること」用
 */
public class TaskRecyclerAdapter extends RecyclerView.Adapter<TaskRecyclerAdapter.ViewHolder> {

    private List<TaskTable>             mData;
    private Context                     mContext;
    private View.OnClickListener        clickListener;
    private View.OnLongClickListener    longListener;
    private int                         mLayoutID;
    //private OnRecyclerListener mListener;

    /*
     * ViewHolder：リスト内の各アイテムのレイアウトを含む View のラッパー
     * (固有のためインナークラスで定義)
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        //表示内容
        private TextView taskName;
        private TextView taskTime;

        //リスナー設定ビュー
        private LinearLayout ll_taskInfo ;

        /*
         * コンストラクタ
         */
        public ViewHolder(View itemView) {
            super(itemView);
            taskName = (TextView) itemView.findViewById(R.id.tv_taskName);
            taskTime = (TextView) itemView.findViewById(R.id.tv_taskTime);
            ll_taskInfo = (LinearLayout)itemView.findViewById(R.id.ll_taskInfo);
        }
    }

    /*
     * コンストラクタ
     */
    public TaskRecyclerAdapter(Context context, int layoutID, List<TaskTable> data) {
        mData     = data;
        mContext  = context;
        mLayoutID = layoutID;
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
    public TaskRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        //レイアウトIDを取得
        int id = mLayoutID;
        if( id == -1 ){
            id = getLayoutId(viewType);
        }

        //表示レイアウトの設定
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(id, viewGroup, false);

        return new ViewHolder(view);
    }


    /*
     * ViewHolderの設定
     */
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int i) {

        //やること時間を文字列に変換
        String timeStr = Integer.toString( mData.get(i).getTaskTime() );

        //データ表示
        viewHolder.taskName.setText(mData.get(i).getTaskName());
        viewHolder.taskTime.setText(timeStr);

        //クリック処理
        viewHolder.ll_taskInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onClick(view);
            }
        });

        //ドラッグ処理
        viewHolder.ll_taskInfo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                longListener.onLongClick(view);
                return true;
            }
        });
    }

    /*
     * 表示データ数の取得
     */
    @Override
    public int getItemCount() {
        //表示データ数を返す
        Log.i("test", "getItemCount =" + mData.size());
        return mData.size();
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

}

