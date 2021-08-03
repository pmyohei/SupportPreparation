package com.example.supportpreparation;

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

    private List<TaskTable>         mData;
    private Context                 mContext;
    private View.OnClickListener    clickListener;
    private int                     mLayoutID;
    //private OnRecyclerListener mListener;

    /*
     * ViewHolder：リスト内の各アイテムのレイアウトを含む View のラッパー
     * (固有のためインナークラスで定義)
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        //表示内容
        TextView task;
        TextView taskTime;

        //リスナー設定ビュー
        LinearLayout ll_taskInfo ;

        /*
         * コンストラクタ
         */
        public ViewHolder(View itemView) {
            super(itemView);
            this.task     = (TextView) itemView.findViewById(R.id.tv_taskName);
            this.taskTime = (TextView) itemView.findViewById(R.id.tv_taskTime);
            this.ll_taskInfo = (LinearLayout)itemView.findViewById(R.id.ll_taskInfo);
        }
    }

    /*
     * コンストラクタ
     */
    public TaskRecyclerAdapter(Context context, int layoutID, List<TaskTable> data) {
        this.mData     = data;
        this.mContext  = context;
        this.mLayoutID = layoutID;
    }

    /*
     *　ViewHolderの生成
     */
    @Override
    public TaskRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        //表示レイアウトの設定
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        View view = inflater.inflate(this.mLayoutID, viewGroup, false);

        Log.i("test", "onCreateViewHolder i=" + i);

        return new ViewHolder(view);
    }

    /*
     * ViewHolderの設定
     */
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int i) {
        //データ表示
        viewHolder.task.setText(mData.get(i).getTaskName());
        viewHolder.taskTime.setText(mData.get(i).getTaskTime() + " min");

        //クリック処理
        viewHolder.ll_taskInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onClick(view);
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
        this.clickListener = listener;
    }


}

