package com.example.supportpreparation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/*
 * RecyclerViewアダプター：「やること」用
 */
public class TaskRecyclerAdapter extends RecyclerView.Adapter<TaskRecyclerAdapter.TaskViewHolder> {

    //-- アダプタ設定対象
    public enum SETTING {
        LIST,               //「やること」一覧エリア
        SELECT,             //「やること」選択エリア
        GROUP,              //「やること」グループ割り当て
    }

    private TaskArrayList<TaskTable> mData;
    private Context mContext;
    private View.OnClickListener clickListener;
    private View.OnLongClickListener longListener;
    private SETTING mSetting;
    private int mItemWidth;
    private int mItemHeight;

    /*
     * ViewHolder：リスト内の各アイテムのレイアウトを含む View のラッパー
     * (固有のためインナークラスで定義)
     */
    class TaskViewHolder extends RecyclerView.ViewHolder {

        private TextView tv_pid;            //Pid
        private TextView tv_taskName;       //表示内容
        private TextView tv_taskTime;
        private LinearLayout ll_taskInfo;  //リスナー設定ビュー

        /*
         * コンストラクタ
         */
        public TaskViewHolder(View itemView) {
            super(itemView);
            tv_pid = (TextView) itemView.findViewById(R.id.tv_pid);
            tv_taskName = (TextView) itemView.findViewById(R.id.tv_taskName);
            tv_taskTime = (TextView) itemView.findViewById(R.id.tv_taskTime);
            ll_taskInfo = (LinearLayout) itemView.findViewById(R.id.ll_taskInfo);
        }
    }

    /*
     * コンストラクタ
     */
    public TaskRecyclerAdapter(Context context, TaskArrayList<TaskTable> data, SETTING setting) {
        mData = data;
        mContext = context;
        mSetting = setting;

        //指定なしなら０とする
        mItemWidth = 0;
        mItemHeight = 0;
    }

    /*
     * コンストラクタ
     */
    public TaskRecyclerAdapter(Context context, TaskArrayList<TaskTable> data, SETTING setting, int width, int height) {
        mData = data;
        mContext = context;
        mSetting = setting;
        mItemWidth = width;
        mItemHeight = height;
    }

    /*
     * ここで返した値が、onCreateViewHolder()の第２引数になる
     */
    @Override
    public int getItemViewType(int position) {

        if (mData.get(position) == null) {
            //フェールセーフ
            Log.i("failsafe", "adapter data is null. mSetting=" + mSetting);
            return 0;
        }

        return mData.get(position).getTaskTime();
    }

    /*
     *　ViewHolderの生成
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        //レイアウトIDを取得
        int id = getLayoutId();

        //表示レイアウトビューの生成
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(id, viewGroup, false);

        //drawableファイルを適用
        applyDrawableResorce(view, viewType);

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

        return new TaskViewHolder(view);
    }


    /*
     * ViewHolderの設定
     */
    @Override
    public void onBindViewHolder(TaskViewHolder viewHolder, final int i) {

        //文字列変換
        String pidStr = Integer.toString(mData.get(i).getId());

        //データ設定
        viewHolder.tv_pid.setText(pidStr);
        viewHolder.tv_taskName.setText(mData.get(i).getTaskName());

        //グループ対応
        if (viewHolder.tv_taskTime != null) {
            String timeStr = Integer.toString(mData.get(i).getTaskTime());
            viewHolder.tv_taskTime.setText(timeStr);
        }

        //クリック処理
        if (clickListener != null) {
            viewHolder.ll_taskInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onClick(view);
                }
            });
        }

        //ドラッグ処理
        if (longListener != null) {
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
     * レイアウトIDの取得
     */
    private int getLayoutId() {

        switch (mSetting) {
            case LIST:
                return R.layout.outer_task;

            case SELECT:
                return R.layout.outer_task_for_select;

            case GROUP:
                return R.layout.outer_task_in_group;

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

        //適用対象のビューID
        int id_view;
        int id_drawable;

        //表示目的に応じて、IDを取得
        switch (mSetting) {
            case LIST:
                id_view     = R.id.ll_taskInfo;
                id_drawable = R.drawable.frame_item_task;
                break;

            case SELECT:
                id_view     = R.id.ll_taskDesign;
                id_drawable = R.drawable.frame_item_task;
                break;

            case GROUP:
                id_view     = R.id.ll_taskInfo;
                id_drawable = R.drawable.frame_item_task_in_group;
                break;

            default:
                //ありえないルート
                return;
        }

        //適用対象のビューを取得
        LinearLayout ll = view.findViewById( id_view );
        //drawableリソースを生成
        Drawable drawable = mContext.getDrawable( id_drawable );

        //時間に応じて、色を設定
        if (time <= 5) {
            drawable.setTint(mContext.getColor(R.color.bg_task_very_short));
        } else if (time <= 10) {
            drawable.setTint(mContext.getColor(R.color.bg_task_short));
        } else if (time <= 30) {
            drawable.setTint(mContext.getColor(R.color.bg_task_normal));
        } else if (time <= 60) {
            drawable.setTint(mContext.getColor(R.color.bg_task_long));
        } else {
            drawable.setTint(mContext.getColor(R.color.bg_task_very_long));
        }

        ll.setBackground(drawable);
    }


    /*
    private int getLayoutIdForCreate(int time) {

        int id;

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
        return id;
    }


    private int getLayoutIdForSelect(int time) {

        int id;

        if (time <= 5) {
            id = R.layout.outer_task_for_select_very_short;
        } else if (time <= 10) {
            id = R.layout.outer_task_for_select_short;
        } else if (time <= 30) {
            id = R.layout.outer_task_for_select_normal;
        } else if (time <= 60) {
            id = R.layout.outer_task_for_select_long;
        } else {
            id = R.layout.outer_task_for_select_very_long;
        }
        return id;
    }


    private int getLayoutIdForGroup(int time) {

        int id;

        if (time <= 5) {
            id = R.layout.outer_task_in_group_very_short;
        } else if (time <= 10) {
            id = R.layout.outer_task_in_group_short;
        } else if (time <= 30) {
            id = R.layout.outer_task_in_group_normal;
        } else if (time <= 60) {
            id = R.layout.outer_task_in_group_long;
        } else {
            id = R.layout.outer_task_in_group_very_long;
        }
        return id;
    }
     */
}