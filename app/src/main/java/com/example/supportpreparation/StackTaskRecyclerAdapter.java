package com.example.supportpreparation;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/*
 * RecyclerViewアダプター：「やること」用
 */
public class StackTaskRecyclerAdapter extends RecyclerView.Adapter<StackTaskRecyclerAdapter.StackTaskViewHolder> {

    private final int NO_ANIMATION = -1;        //アニメーション適用なし

    private StackTaskTable mStackTable;     //積み立てやること
    private TaskArrayList<TaskTable> mData;     //積み立てやること
    private boolean mIsLimit;                   //リミット時間かどうか
    private Context mContext;                   //コンテキスト
    private int mAnimIdx;                       //アニメーション有効Idx
    private int mAddAnimationID;                //やること追加時のアニメーションリソースID
    private int mItemWidth;

    /*
     * ViewHolder：リスト内の各アイテムのレイアウトを含む View のラッパー
     * (固有のためインナークラスで定義)
     */
    class StackTaskViewHolder extends RecyclerView.ViewHolder {

        private TextView tv_pid;
        private TextView tv_taskName;
        private TextView tv_taskTime;
        private LinearLayout ll_taskInfo;
        private LinearLayout ll_startTime;
        private LinearLayout ll_endTime;
        private TextView tv_taskStartTime;
        private TextView tv_taskEndTime;
        private LinearLayout ll_label;
        private TextView tv_label;

        /*
         * コンストラクタ
         */
        public StackTaskViewHolder(View itemView) {
            super(itemView);
            tv_pid = (TextView) itemView.findViewById(R.id.tv_pid);
            tv_taskName = (TextView) itemView.findViewById(R.id.tv_taskName);
            tv_taskTime = (TextView) itemView.findViewById(R.id.tv_taskTime);
            ll_taskInfo = (LinearLayout) itemView.findViewById(R.id.ll_taskInfo);
            ll_startTime = (LinearLayout) itemView.findViewById(R.id.ll_startTime);
            ll_endTime = (LinearLayout) itemView.findViewById(R.id.ll_endTime);
            tv_taskStartTime = (TextView) itemView.findViewById(R.id.tv_taskStartTime);
            tv_taskEndTime = (TextView) itemView.findViewById(R.id.tv_taskEndTime);
            ll_label = (LinearLayout) itemView.findViewById(R.id.ll_label);
            tv_label = (TextView) itemView.findViewById(R.id.tv_label);
        }
    }

    /*
     * コンストラクタ
     * 　※リミット日・リミット時間のTextViewを保持するが、
     *　　 スタート設定の場合でも、設定日時は同期されるため、
     * 　  リミット関連のビューのみの保持で問題なし。
     */
    public StackTaskRecyclerAdapter(Context context, StackTaskTable stackTable, int width) {
        mStackTable = stackTable;
        mData       = stackTable.getStackTaskList();
        mContext    = context;
        mItemWidth  = width;

        //セットメソッドがコールされたとき、設定する
        mAnimIdx = NO_ANIMATION;
        //リミット（起動時は、リミットモード）
        mIsLimit = stackTable.isLimit();

        //適用するアニメーション
        mAddAnimationID = ( mIsLimit ? R.anim.stack_task : R.anim.que_task );
    }

    /*
     * 表示データ数の取得
     */
    @Override
    public int getItemCount() {
        Log.i("test", "stack getItemCount");

        //表示データ数を返す
        return mData.size();
    }

    /*
     * ここで返した値が、onCreateViewHolder()の第２引数になる
     */
    @Override
    public int getItemViewType(int position) {
        Log.i("test", "stack getItemViewType position=" + position);

        return mData.get(position).getTaskTime();
    }

    /*
     *　ViewHolderの生成
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public StackTaskViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        Log.i("test", "stack onCreateViewHolder viewType=" + viewType);

        //レイアウトIDを取得
        int id = R.layout.outer_task_for_stack;

        //表示レイアウトの設定
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(id, viewGroup, false);

        //正方形のサイズを設定
        LinearLayout ll_taskDesign = view.findViewById(R.id.ll_taskInfo);

        //レイアウト全体サイズに対して、一定割合をブロックの大きさとする
        ViewGroup.LayoutParams blockLayoutParams = ll_taskDesign.getLayoutParams();
        blockLayoutParams.width = mItemWidth;

        //drawableファイルを適用
        applyDrawableResorce(view, viewType);

        return new StackTaskViewHolder(view);
    }

    /*
     * ViewHolderの設定
     */
    @Override
    public void onBindViewHolder(StackTaskViewHolder viewHolder, final int i) {

        Log.i("test", "stack onBindViewHolder i=" + i);

        //viewHolder.getItemViewType();

        //文字列変換
        int taskTime = mData.get(i).getTaskTime();
        String timeStr = Integer.toString(taskTime);
        String pidStr = Integer.toString(mData.get(i).getId());

        //データ設定
        viewHolder.tv_pid.setText(pidStr);
        viewHolder.tv_taskName.setText(mData.get(i).getTaskName());
        viewHolder.tv_taskTime.setText(timeStr);

        //リミットベースかスタートベースか
        if (mIsLimit) {
            //開始時間を表示
            viewHolder.ll_startTime.setVisibility(View.VISIBLE);
            viewHolder.ll_endTime.setVisibility(View.GONE);

            //やること開始時間の算出と設定
            //setupTaskStartTime(viewHolder, i);

            //アニメーション指定Idxの範囲内の場合
            if (mAnimIdx >= i) {
                //やること追加時のアニメーション開始
                Animation animation = AnimationUtils.loadAnimation(mContext, mAddAnimationID);
                viewHolder.itemView.startAnimation(animation);

                //先頭のIdxまで適用した場合
                //！setStackFromEnd(true)を設定している場合、リスト最後尾から表示処理が入るため、
                //　先頭要素の設定が最後になる
                if (i == 0) {
                    //アニメーション無効化
                    mAnimIdx = NO_ANIMATION;
                }
            }

        } else {
            //終了時間を表示
            viewHolder.ll_startTime.setVisibility(View.GONE);
            viewHolder.ll_endTime.setVisibility(View.VISIBLE);

            //やること開始時間の算出と設定
            //setupTaskEndTime(viewHolder, i);

            //アニメーション指定Idx以上の
            if ((0 <= mAnimIdx) && (mAnimIdx <= i)) {
                //やること追加時のアニメーション開始
                Animation animation = AnimationUtils.loadAnimation(mContext, mAddAnimationID);
                viewHolder.itemView.startAnimation(animation);

                //最後のIdxまで適用した場合
                if (i == mData.size() - 1) {
                    //アニメーション無効化
                    mAnimIdx = NO_ANIMATION;
                }
            }
        }

        //開始・終了時刻の設定
        setupTaskStartEndTime(viewHolder, i);
    }

    /*
     * 「やること」開始時刻の設定
     */
    private void setupTaskStartEndTime(StackTaskViewHolder viewHolder, int i) {

        TextView tv_target;

        //「やること」開始・終了時刻
        Calendar startCalendar = mData.get(i).getStartCalendar();
        Calendar endCalendar   = mData.get(i).getEndCalendar();
        Calendar setCalendar;

        if( mIsLimit ){
            tv_target = viewHolder.tv_taskStartTime;
            setCalendar = startCalendar;

        } else {
            tv_target = viewHolder.tv_taskEndTime;
            setCalendar = endCalendar;
        }

        //設定日時をDate型として取得
        Date baseDate = mStackTable.getBaseTimeDate();
        if( baseDate == null ){
            //設定なし or 変換エラーの場合、無効文字列を設定
            String noInputStr = mContext.getString(R.string.limittime_no_input);
            tv_target.setText(noInputStr);
            return;
        }

        //ラベルの設定
        if( mIsLimit ){
            setLabel(viewHolder, startCalendar, endCalendar);
        } else {
            //スタートベースの場合、ラベル付与なし
            viewHolder.ll_label.setVisibility(View.INVISIBLE);
        }

        //Dateを文字列変換
        Date setDate = setCalendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.JAPANESE);
        String timeStr  = sdf.format(setDate);

        //時間設定
        tv_target.setText(timeStr);
    }

    /*
     * 「やること」開始時刻の設定
     */
    /*
    private void setupTaskStartTime(StackTaskViewHolder viewHolder, int i) {

        //設定日時をDate型として取得
        Date finalLimit = getConvertedDateBaseTime();
        if( finalLimit == null ){
            //設定なし or 変換エラーの場合、無効文字列を設定
            String noInputStr = mContext.getString(R.string.limittime_no_input);
            viewHolder.tv_taskStartTime.setText(noInputStr);
            return;
        }

        //「やること」開始時刻
        Calendar startCalendar = getStartTimeCalendar(i, finalLimit);

        //「やること」終了時刻の計算
        Calendar endCalendar = getStartTimeCalendar(i, finalLimit);
        int time = mData.get(i).getTaskTime();
        endCalendar.add(Calendar.MINUTE, time);

        //ラベルの設定
        setLabel(viewHolder, i, startCalendar, endCalendar);

        //開始時間を文字列に変換
        Date startDate       = startCalendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.JAPANESE);
        String startTimeStr  = sdf.format(startDate);

        //「開始時間」として設定
        viewHolder.tv_taskStartTime.setText(startTimeStr);

        //やることにカレンダーを設定
        mData.get(i).setStartCalendar(startCalendar);
        mData.get(i).setEndCalendar(endCalendar);
    }
     */


    /*
     * 「やること」終了時刻の設定
     */
    /*
    private void setupTaskEndTime(StackTaskViewHolder viewHolder, int i ) {

        //設定日時をDate型として取得
        Date finalLimit = getConvertedDateBaseTime();
        if( finalLimit == null ){
            //設定なし or 変換エラーの場合、無効文字列を設定
            String noInputStr = mContext.getString(R.string.limittime_no_input);
            viewHolder.tv_taskEndTime.setText(noInputStr);
            return;
        }

        //「やること」終了時刻
        Calendar endCalendar = getEndTimeCalendar(i, finalLimit);

        //「やること」開始時刻の計算
        Calendar startCalendar = getEndTimeCalendar(i, finalLimit);
        int time = mData.get(i).getTaskTime();
        startCalendar.add(Calendar.MINUTE, -time);

        //ラベルの設定（スタートベースの場合、ラベル付与なし）
        viewHolder.ll_label.setVisibility(View.INVISIBLE);

        //終了時間を取得し、文字列に変換
        Date endDate         = endCalendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.JAPANESE);
        String endTimeStr    = sdf.format(endDate);

        //「終了時間」として設定
        viewHolder.tv_taskEndTime.setText(endTimeStr);

        //やることにカレンダーを設定
        mData.get(i).setStartCalendar(startCalendar);
        mData.get(i).setEndCalendar(endCalendar);
    }
     */

    /*
     * 「やること」の開始時刻（カレンダー）の取得
     *   取得に伴い、アラーム時刻のリストに開始時刻を追加する
     */
/*    private Calendar getStartTimeCalendar(int i, Date finalLimit) {

        //リミットから引く時間を計算
        int totalTaskMin = getTotalTaskTimeToLast(i);

        //開始時間を計算
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(finalLimit);
        calendar.add(Calendar.MINUTE, -totalTaskMin);

        return calendar;
    }*/

    /*
     * 「やること」の終了時刻（カレンダー）の取得
     *   取得に伴い、アラーム時刻のリストに終了時刻を追加する
     */
/*    private Calendar getEndTimeCalendar(int i, Date startTime) {

        //スタート時間に加算する値を取得
        int totalTaskMin = getTotalTaskTimeFromTop(i);

        //開始時間を計算
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime);
        calendar.add(Calendar.MINUTE, totalTaskMin);

        return calendar;
    }*/

    /*
     * 「やること」ラベルの設定
     *   ラベルを設定する必要がない場合、何もしない
     */
    private void setLabel(StackTaskViewHolder viewHolder, Calendar startCalendar, Calendar endCalendar) {

        if( startCalendar == null ){
            return;
        }

        //現在時刻
        Date nowTime = new Date();
        //開始時刻
        Date startTime = startCalendar.getTime();

        //現在時刻が開始時刻よりも前の場合
        if (nowTime.before(startTime)) {
            //ラベル設定はなし
            viewHolder.ll_label.setVisibility(View.INVISIBLE);
            return;
        }

        //終了時刻を取得
        Date endTime = endCalendar.getTime();

        //ラべルの設定
        String label;
        if( nowTime.before(endTime) ){
            //--現在時刻が終了時刻の前の場合

            //「やること」進行中
            label = mContext.getString(R.string.label_now);
        } else {
            //「やること」終了済み
            label = mContext.getString(R.string.label_finish);
        }

        //ラベルを可視化し、文字列を設定
        viewHolder.ll_label.setVisibility(View.VISIBLE);
        viewHolder.tv_label.setText(label);
    }

    /*
     * 追加時のアニメーション設定
     */
    public void setInsertAnimationIdx(int idx) {
        //リミット指定：0からこのIndexまで、アイテム追加アニメーションを適用
        //スタート指定：このIndexから最後のIndexまで、アイテム追加アニメーションを適用
        mAnimIdx = idx;
    }

    /*
     * やること時間の累計（指定Index～LastIndex）
     */
/*    private int getTotalTaskTimeToLast(int idx){

        int minute = 0;

        int size = mData.size();
        for( int i = idx; i < size; i++ ){
            //やること時間を累算
            minute += mData.get(i).getTaskTime();
        }

        return minute;
    }*/

    /*
     * やること時間の累計（TopIndex～指定Index）
     */
/*    private int getTotalTaskTimeFromTop(int idx){

        int minute = 0;

        for( int i = 0; i <= idx; i++ ){
            //やること時間を累算
            minute += mData.get(i).getTaskTime();
        }

        return minute;
    }*/

    /*
     * リミットか否かを反転
     */
    public void reverseTime(){
        mIsLimit = mStackTable.isLimit();

        //適用アニメーションを変更
        mAddAnimationID = ( mIsLimit ? R.anim.stack_task : R.anim.que_task );
    }

    /*
     * ビューにdrawableを適用
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void applyDrawableResorce(View view, int time) {

        //適用対象のビューを取得
        LinearLayout ll = view.findViewById(R.id.ll_taskInfo);

        //表示目的に応じて、drawableリソースを取得
        Drawable drawable = mContext.getDrawable(R.drawable.frame_item_task_for_stack);

        //時間に応じて、色を設定
        int colorId = ResourceManager.getTaskTimeColorId(time);;
        drawable.setTint(mContext.getColor( colorId ));

        ll.setBackground(drawable);
    }

}

