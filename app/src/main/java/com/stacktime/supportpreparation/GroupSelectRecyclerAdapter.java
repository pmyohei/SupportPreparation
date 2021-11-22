package com.stacktime.supportpreparation;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stacktime.supportpreparation.R;

/*
 * RecyclerAdapter：グループ（選択エリア）用
 */
public class GroupSelectRecyclerAdapter extends RecyclerView.Adapter<GroupSelectRecyclerAdapter.GroupViewHolder> {

    //定数
    private final int                         NOT_SIZE_SPECIFIED = 0;

    //フィールド変数
    private final GroupArrayList<GroupTable>  mData;
    private final Context                     mContext;
    private final int                         mItemWidth;
    private final int                         mItemHeight;
    private View.OnClickListener              mClickListener;

    /*
     * ViewHolder：リスト内の各アイテムのレイアウトを含む View のラッパー
     * (固有のためインナークラスで定義)
     */
    class GroupViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout    ll_groupInfo;       //リスナー設定ビュー
        private final TextView        tv_groupPid;        //Pid
        private final TextView        tv_groupName;       //グループ名
        private final TextView        tv_taskInGroup;     //やること

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
    public GroupSelectRecyclerAdapter(Context context, GroupArrayList<GroupTable> data) {
        mData    = data;
        mContext = context;

        //指定なしならサイズ指定なしを設定
        mItemWidth  = NOT_SIZE_SPECIFIED;
        mItemHeight = NOT_SIZE_SPECIFIED;
    }

    /*
     * コンストラクタ
     */
    public GroupSelectRecyclerAdapter(Context context, GroupArrayList<GroupTable> data, int width, int height) {
        mData       = data;
        mContext    = context;
        mItemWidth  = width;
        mItemHeight = height;
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
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        //レイアウトIDを取得
        int id = getLayoutId(viewType);

        //表示レイアウトの設定
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(id, viewGroup, false);

        //-- サイズ指定があれば、サイズを設定
        //レイアウトパラメータを取得
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

        //横幅
        if (mItemWidth != NOT_SIZE_SPECIFIED) {
            layoutParams.width = mItemWidth;
            view.setLayoutParams(layoutParams);

            //正方形のサイズを設定
            LinearLayout ll_groupDesign = view.findViewById(R.id.ll_groupDesign);

            //サイズ割合
            final float SIZE_RATIO = 0.6f;

            //レイアウト全体サイズに対して、一定割合をブロックの大きさとする
            ViewGroup.LayoutParams blockLayoutParams = ll_groupDesign.getLayoutParams();
            blockLayoutParams.width  = (int)(mItemWidth * SIZE_RATIO);
            blockLayoutParams.height = (int)(mItemWidth * SIZE_RATIO);

            ll_groupDesign.setLayoutParams(blockLayoutParams);

        }
        //高さ
        if (mItemHeight != NOT_SIZE_SPECIFIED) {

            layoutParams.height = mItemHeight;
            view.setLayoutParams(layoutParams);
        }
        
        //空データの場合、非表示
        if( viewType == ResourceManager.INVALID_MIN ){
            LinearLayout ll_taskInfo = view.findViewById( R.id.ll_groupInfo );
            ll_taskInfo.setVisibility( View.INVISIBLE );
        }

        return new GroupViewHolder(view);
    }


    /*
     * ViewHolderの設定
     */
    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder viewHolder, final int i) {

        int totalTime = mData.get(i).getTotalTime();
        if( totalTime == ResourceManager.INVALID_MIN ){
            //空データなら設定不要
            return;
        }

        //文字列変換
        String pidStr = Integer.toString(mData.get(i).getId());

        //データ設定
        viewHolder.tv_groupPid.setText(pidStr);
        viewHolder.tv_groupName.setText( mData.get(i).getGroupName() );
        viewHolder.tv_taskInGroup.setText( mData.get(i).getTaskPidsStr() );

        Log.i("test", "getTaskPidsStr=" + mData.get(i).getTaskPidsStr());

        //クリック処理
        if ( mClickListener != null ) {
            viewHolder.ll_groupInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mClickListener.onClick(view);
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
     * クリックリスナーの設定
     */
    public void setOnItemClickListener(View.OnClickListener listener) {
        mClickListener = listener;
    }

    /*
     * カラーIDの取得
     *   ※色の動的変更に備え、残しておく
     */
    private int getLayoutId(int time) {

        int id;

        id = R.layout.outer_group_for_select;

        return id;
    }


}