package com.example.supportpreparation;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

/*
 * RecyclerAdapter：やること(リスト／選択エリア／グループ内)用
 */
public class TaskRecyclerAdapter extends RecyclerView.Adapter<TaskRecyclerAdapter.TaskViewHolder> {

    //表示種別
    public enum SHOW_KIND {
        LIST,                   //「やること」一覧エリア
        SELECT,                 //「やること」選択エリア
        IN_GROUP,               //「やること」グループ割り当て
    }

    //定数
    private final  int                      NOT_SIZE_SPECIFIED = 0;

    //フィールド変数
    private final TaskArrayList<TaskTable>  mData;              //やることリスト
    private final Context                   mContext;           //コンテキスト
    private final SHOW_KIND                 mShowKind;          //表示種別
    private final int                       mItemHeight;        //やること縦幅
    private int                             mItemWidth;         //やること横幅
    private View.OnClickListener            mClickListener;     //やることクリックリスナー
    private View.OnLongClickListener        mLongListener;      //やることロングタッチリスナー

    /*
     * ViewHolder：リスト内の各アイテムのレイアウトを含む View のラッパー
     * (固有のためインナークラスで定義)
     */
    class TaskViewHolder extends RecyclerView.ViewHolder {

        private final TextView      tv_pid;
        private final TextView      tv_taskName;
        private final TextView      tv_taskTime;
        private final LinearLayout  ll_taskInfo;

        /*
         * コンストラクタ
         */
        public TaskViewHolder(View itemView) {
            super(itemView);

            tv_pid      = (TextView) itemView.findViewById(R.id.tv_pid);
            tv_taskName = (TextView) itemView.findViewById(R.id.tv_taskName);
            tv_taskTime = (TextView) itemView.findViewById(R.id.tv_taskTime);
            ll_taskInfo = (LinearLayout) itemView.findViewById(R.id.ll_taskInfo);
        }
    }


    /*
     * コンストラクタ(LIST/SELECT)
     */
    public TaskRecyclerAdapter(Context context, TaskArrayList<TaskTable> data, SHOW_KIND setting, int width) {
        mData       = data;
        mContext    = context;
        mShowKind   = setting;
        mItemWidth  = width;

        //縦幅指定なし
        mItemHeight = NOT_SIZE_SPECIFIED;
    }

    /*
     * コンストラクタ(IN_GROUP)
     */
    public TaskRecyclerAdapter(Context context, TaskArrayList<TaskTable> data, SHOW_KIND setting, int width, int height) {
        mData       = data;
        mContext    = context;
        mShowKind   = setting;
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
            Log.i("failsafe", "adapter data is null. mSetting=" + mShowKind);
            return 0;
        }

        return mData.get(position).getTaskTime();
    }

    /*
     *　ViewHolderの生成
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        //レイアウトIDを取得
        int id = getLayoutId();

        //表示レイアウトビューの生成
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(id, viewGroup, false);

        //drawableファイルを適用
        applyDrawableResorce(view, viewType);


        //レイアウトパラメータを取得
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

        //表示目的に応じて、IDを取得
        switch (mShowKind) {
            case LIST:
                //リストに対しては、追加レイアウト全体に対してサイズを設定

                //サイズ割合
                final float SIZE_RATIO_OVERALL = 0.8f;

                //追加レイアウトそのものの大きさ
                layoutParams.width  = (int)(mItemWidth * SIZE_RATIO_OVERALL);
                layoutParams.height = (int)(mItemHeight * SIZE_RATIO_OVERALL);
                view.setLayoutParams(layoutParams);

                break;

            case SELECT:
            case IN_GROUP:
                //選択エリア／グループ内に対しては、追加レイアウト全体、ブロックに対してサイズを設定

                //横幅
                //アイテムレイアウトそのものの大きさを設定
                layoutParams.width = mItemWidth;
                view.setLayoutParams(layoutParams);

                //サイズ割合
                final float SIZE_RATIO_BLOCK = 0.6f;

                //正方形のサイズを設定
                LinearLayout ll_taskDesign = view.findViewById(R.id.ll_taskDesign);

                //アイテムレイアウト全体サイズに対して、一定割合をブロックの大きさとする
                ViewGroup.LayoutParams blockLayoutParams = ll_taskDesign.getLayoutParams();
                blockLayoutParams.width  = (int)(mItemWidth * SIZE_RATIO_BLOCK);
                blockLayoutParams.height = (int)(mItemWidth * SIZE_RATIO_BLOCK);

                ll_taskDesign.setLayoutParams(blockLayoutParams);

                break;

            default:
                //ありえないルート
                break;
        }

        return new TaskViewHolder(view);
    }


    /*
     * ViewHolderの設定
     */
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder viewHolder, final int i) {

        int taskTime = mData.get(i).getTaskTime();
        if( taskTime == ResourceManager.INVALID_MIN ){
            //空データなら設定不要
            return;
        }

        //文字列変換
        String pidStr = Integer.toString(mData.get(i).getId());

        //データ設定
        viewHolder.tv_pid.setText(pidStr);
        viewHolder.tv_taskName.setText(mData.get(i).getTaskName());

        String timeStr = Integer.toString( taskTime );
        viewHolder.tv_taskTime.setText(timeStr);

        //クリック時の処理
        if (mClickListener != null) {
            viewHolder.ll_taskInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mClickListener.onClick(view);
                }
            });
        }

        //ロングクリック時の処理
        if ( mLongListener != null ) {
            viewHolder.ll_taskInfo.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    //リスナーとして設定されたメソッドをコール
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
     * クリックリスナーの設定
     */
    public void setOnItemClickListener(View.OnClickListener listener) {
        mClickListener = listener;
    }

    /*
     * ドラッグ（長押し）リスナーの設定
     */
    public void setOnItemLongClickListener(View.OnLongClickListener listener) {
        //長押しされた時の動作
        mLongListener = listener;
    }

    /*
     * レイアウトIDの取得
     */
    private int getLayoutId() {

        switch (mShowKind) {
            case LIST:
                return R.layout.outer_task;

            case SELECT:
                return R.layout.outer_task_for_select;

            case IN_GROUP:
                return R.layout.outer_task_for_select;

            default:
                //ありえないルート
                return R.layout.outer_task_in_group;
        }

    }

    /*
     * ビューにdrawableを適用
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void applyDrawableResorce(View view, int time) {

        //適用対象のビューID（角丸四角のビュー）
        int id_view;
        //drawableファイルID
        int id_drawable;

        //表示目的に応じて、IDを取得
        switch (mShowKind) {
            case LIST:
                id_view     = R.id.ll_taskInfo;
                id_drawable = R.drawable.frame_item_task;
                break;

            case SELECT:
                id_view     = R.id.ll_taskDesign;
                id_drawable = R.drawable.frame_item_task_for_select;
                break;

            case IN_GROUP:
                id_view     = R.id.ll_taskDesign;
                id_drawable = R.drawable.frame_item_task_for_select;
                break;

            default:
                //ありえないルート
                return;
        }

        //適用対象のビューを取得
        LinearLayout ll = view.findViewById( id_view );
        //drawableリソースを生成
        Drawable drawable = mContext.getDrawable( id_drawable );

        if( time == ResourceManager.INVALID_MIN ){

            //空データのため、非表示
            LinearLayout ll_taskInfo = view.findViewById( R.id.ll_taskInfo );
            ll_taskInfo.setVisibility( View.INVISIBLE );

        } else {

            //時間に応じて、色を設定
            int colorId = ResourceManager.getTaskTimeColorId(time);;
            drawable.setTint(mContext.getColor( colorId ));
        }

        ll.setBackground(drawable);
    }

}