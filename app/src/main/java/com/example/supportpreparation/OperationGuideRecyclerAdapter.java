package com.example.supportpreparation;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/*
 * RecyclerAdapter：操作ガイド用
 */
public class OperationGuideRecyclerAdapter extends RecyclerView.Adapter<OperationGuideRecyclerAdapter.GuideViewHolder> {

    //フィールド変数
    private final List<Integer> mData;

    /*
     * ViewHolder：リスト内の各アイテムのレイアウトを含む View のラッパー
     */
    static class GuideViewHolder extends RecyclerView.ViewHolder {

        /*
         * コンストラクタ
         */
        public GuideViewHolder(View itemView) {
            super(itemView);
        }
    }

    /*
     * コンストラクタ
     */
    public OperationGuideRecyclerAdapter(List<Integer> layoutIdList) {
        mData = layoutIdList;
    }

    /*
     * ここで返した値が、onCreateViewHolder()の第２引数になる
     */
    @Override
    public int getItemViewType(int position) {
        //レイアウトIDを返す
        return mData.get(position);
    }

    /*
     *　ViewHolderの生成
     */
    @NonNull
    @Override
    public GuideViewHolder onCreateViewHolder(ViewGroup viewGroup, int layoutId) {

        //表示レイアウトの設定
        Context context = viewGroup.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(layoutId, viewGroup, false);

        return new GuideViewHolder(view);
    }

    /*
     * ViewHolderの設定
     */
    @Override
    public void onBindViewHolder(@NonNull GuideViewHolder viewHolder, final int i) {
    }

    /*
     * 表示データ数の取得
     */
    @Override
    public int getItemCount() {
        //ページ数
        return mData.size();
    }
}
