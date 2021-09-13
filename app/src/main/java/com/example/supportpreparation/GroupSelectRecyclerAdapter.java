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
public class GroupSelectRecyclerAdapter extends RecyclerView.Adapter<GroupSelectRecyclerAdapter.GroupViewHolder> {

    private List<GroupTable>            mData;
    private Context                     mContext;
    private View.OnClickListener        mClickListener;
    private View.OnLongClickListener    mLongListener;
    private int                         mItemWidth;
    private int                         mItemHeight;

    /*
     * ViewHolder：リスト内の各アイテムのレイアウトを含む View のラッパー
     * (固有のためインナークラスで定義)
     */
    class GroupViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout    ll_groupInfo;        //リスナー設定ビュー
        private TextView        tv_groupPid;        //Pid
        private TextView        tv_groupName;       //グループ名
        private TextView        tv_taskInGroup;     //やること

        /*
         * コンストラクタ
         */
        public GroupViewHolder(View itemView) {
            super(itemView);
            //ルートレイアウト
            ll_groupInfo = (LinearLayout) itemView.findViewById(R.id.ll_groupInfo);

            //各要素のビュー
            tv_groupPid = (TextView) itemView.findViewById(R.id.tv_groupPid);
            tv_groupName = (TextView) itemView.findViewById(R.id.tv_groupName);
            tv_taskInGroup = (TextView) itemView.findViewById(R.id.tv_taskInGroup);
        }
    }

    /*
     * コンストラクタ
     */
    public GroupSelectRecyclerAdapter(Context context, List<GroupTable> data) {
        mData = data;
        mContext = context;

        //指定なしなら０とする
        mItemWidth = 0;
        mItemHeight = 0;
    }

    /*
     * コンストラクタ
     */
    public GroupSelectRecyclerAdapter(Context context, List<GroupTable> data, int width, int height) {
        mData = data;
        mContext = context;
        mItemWidth = width;
        mItemHeight = height;
    }

    /*
     * ここで返した値が、onCreateViewHolder()の第２引数になる
     */
    @Override
    public int getItemViewType(int position) {

        return 0;
    }

    /*
     *　ViewHolderの生成
     */
    @Override
    public GroupViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        //レイアウトIDを取得
        int id = getLayoutId(viewType);

        //表示レイアウトの設定
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(id, viewGroup, false);

        //-- サイズ指定があれば、サイズを設定
        //レイアウトパラメータを取得
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

        //横幅
        if (mItemWidth != 0) {
            layoutParams.width = mItemWidth;
            view.setLayoutParams(layoutParams);
        }
        //高さ
        if (mItemHeight != 0) {

            layoutParams.height = mItemHeight;
            view.setLayoutParams(layoutParams);
        }

        return new GroupViewHolder(view);
    }


    /*
     * ViewHolderの設定
     */
    @Override
    public void onBindViewHolder(GroupViewHolder viewHolder, final int i) {

        //文字列変換
        String pidStr = Integer.toString(mData.get(i).getId());

        //データ設定
        viewHolder.tv_groupPid.setText(pidStr);
        viewHolder.tv_groupName.setText( mData.get(i).getGroupName() );
        viewHolder.tv_taskInGroup.setText( mData.get(i).getTaskPidsStr() );

        Log.i("test", "getTaskPidsStr=" + mData.get(i).getTaskPidsStr());

        //ドラッグ処理
        if (mLongListener != null) {
            viewHolder.ll_groupInfo.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    mLongListener.onLongClick(view);
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
    public void setOnItemLongClickListener(View.OnLongClickListener listener) {
        mLongListener = listener;
    }

    /*
     * カラーIDの取得
     */
    private int getLayoutId(int time) {

        int id;

        id = R.layout.outer_group_for_select;
        /*
        if (time <= 5) {
            id = R.layout.outer_task_very_short;
        } else if (time <= 10) {
            id = R.layout.outer_task_short;
        } else if (time <= 30) {
            id = R.layout.outer_task_normal;
        } else if (time <= 60) {
            id = R.layout.outer_task_long;
        } else {
            id = R.layout.outer_task_very_long;
        }
         */
        return id;
    }


}