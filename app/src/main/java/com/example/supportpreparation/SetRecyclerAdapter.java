package com.example.supportpreparation;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/*
 * RecyclerViewアダプター：「やることセット」用
 */
public class SetRecyclerAdapter extends RecyclerView.Adapter<SetRecyclerAdapter.ViewHolder> {

    private List<SetTable>          mSetList;
    private List<List<TaskTable>>   mTasksList;
    private Context                 mContext;
    //private OnRecyclerListener mListener;

    /*
     * ViewHolder：リスト内の各アイテムのレイアウトを含む View のラッパー
     * (固有のためインナークラスで定義)
     */
    class ViewHolder extends RecyclerView.ViewHolder {

        //レイアウト内のビュー
        TextView        tv_setName;
        RecyclerView    rv_tasks;

        /*
         * コンストラクタ
         */
        public ViewHolder(View itemView) {
            super(itemView);

            //ビュー取得
            this.tv_setName = (TextView) itemView.findViewById(R.id.tv_setName);
            this.rv_tasks   = (RecyclerView) itemView.findViewById(R.id.rv_selectedtaskList);
        }
    }


    /*
     * コンストラクタ
     */
    public SetRecyclerAdapter(Context context, List<SetTable> setList, List<List<TaskTable>> tasksList) {
        this.mSetList   = setList;
        this.mTasksList = tasksList;
        this.mContext   = context;
        //mListener = listener;
        Log.i("test", "set mData=" + this.mSetList);
    }

    /*
     *　ViewHolderの生成
     */
    @Override
    public SetRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        //表示レイアウトの設定
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        View view = inflater.inflate(R.layout.item_set, viewGroup, false);

        return new ViewHolder(view);
    }

    /*
     * ViewHolderの設定
     */
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int i) {
        //セット名の設定
        viewHolder.tv_setName.setText(this.mSetList.get(i).getSetName());

        //-- セットに紐づいた「やること」
        Log.i("test", "set adapter pre onBindViewHolder");

        //紐づいた「やること」があるなら
        if( this.mTasksList.get(i).size() > 0 ){

            //レイアウトマネージャの生成・設定
            LinearLayoutManager ll_manager = new LinearLayoutManager(this.mContext);
            viewHolder.rv_tasks.setLayoutManager(ll_manager);

            Log.i("test", "set adapter onBindViewHolder");

            //アダプタの生成・設定
            TaskRecyclerAdapter adapter = new TaskRecyclerAdapter(this.mContext, this.mTasksList.get(i), TaskRecyclerAdapter.SETTING.SELECT);
            viewHolder.rv_tasks.setAdapter(adapter);
        }

        /*
         * クリック処理
         */
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mListener.onRecyclerClicked(v, i);
            }
        });
    }

    /*
     * 表示データ数の取得
     */
    @Override
    public int getItemCount() {
        //表示データ数を返す
        return this.mSetList.size();
    }


}

