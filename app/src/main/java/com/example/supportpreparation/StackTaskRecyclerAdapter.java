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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
 * RecyclerViewアダプター：「やること」用
 */
public class StackTaskRecyclerAdapter extends RecyclerView.Adapter<StackTaskRecyclerAdapter.StackTaskViewHolder> {

    private final int NO_ANIMATION = -1;              //アニメーション適用なし

    private TaskArrayList<TaskTable>    mData;              //表示データ
    private Context                     mContext;           //コンテキスト
    private TextView                    mtv_limitDate;      //リミット日のTextView：ユーザー設定変更の内容反映のために保持する
    private TextView                    mtv_limitTime;      //リミット時間のTextView：ユーザー設定変更の内容反映のために保持する
    private int                         mAnimIdx;           //アニメーション有効Idx
    private boolean                     mIsLimit;           //リミット時間かどうか
    private int                         mAddAnimationID;    //やること追加時のアニメーションリソースID

    /*
     * ViewHolder：リスト内の各アイテムのレイアウトを含む View のラッパー
     * (固有のためインナークラスで定義)
     */
    class StackTaskViewHolder extends RecyclerView.ViewHolder {

        private TextView        tv_pid;
        private TextView        tv_taskName;
        private TextView        tv_taskTime;
        private LinearLayout    ll_startTime;
        private LinearLayout    ll_endTime;
        private TextView        tv_taskStartTime;
        private TextView        tv_taskEndTime;
        private LinearLayout    ll_label;
        private TextView        tv_label;

        /*
         * コンストラクタ
         */
        public StackTaskViewHolder(View itemView) {
            super(itemView);
            tv_pid = (TextView) itemView.findViewById(R.id.tv_pid);
            tv_taskName = (TextView) itemView.findViewById(R.id.tv_taskName);
            tv_taskTime = (TextView) itemView.findViewById(R.id.tv_taskTime);
            ll_startTime = (LinearLayout) itemView.findViewById(R.id.ll_startTime);
            ll_endTime   = (LinearLayout) itemView.findViewById(R.id.ll_endTime);
            tv_taskStartTime = (TextView) itemView.findViewById(R.id.tv_taskStartTime);
            tv_taskEndTime   = (TextView) itemView.findViewById(R.id.tv_taskEndTime);
            ll_label         = (LinearLayout) itemView.findViewById(R.id.ll_label);
            tv_label         = (TextView) itemView.findViewById(R.id.tv_label);
        }
    }

    /*
     * コンストラクタ
     * 　※リミット日・リミット時間のTextViewを保持するが、
     *　　 スタート設定の場合でも、設定日時は同期されるため、
     * 　  リミット関連のビューのみの保持で問題なし。
     */
    public StackTaskRecyclerAdapter(Context context, TaskArrayList<TaskTable> data, TextView limitDate, TextView limitTime) {
        mData = data;
        mContext = context;
        mtv_limitDate = limitDate;
        mtv_limitTime = limitTime;

        //セットメソッドがコールされたとき、設定する
        mAnimIdx = NO_ANIMATION;
        //リミット（起動時は、リミットモード）
        mIsLimit = true;

        mAddAnimationID = R.anim.stack_task;
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
            viewHolder.ll_endTime.setVisibility(View.INVISIBLE);

            //やること開始時間の算出と設定
            setupTaskStartTime(viewHolder, i);

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
            viewHolder.ll_startTime.setVisibility(View.INVISIBLE);
            viewHolder.ll_endTime.setVisibility(View.VISIBLE);

            //やること開始時間の算出と設定
            setupTaskEndTime(viewHolder, i);

            //アニメーション指定Idx以上の
            if ( (0 <= mAnimIdx) && (mAnimIdx <= i) ) {
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
    }

    /*
     * 「やること」開始時刻の設定
     */
    private void setupTaskStartTime(StackTaskViewHolder viewHolder, int i) {

        //設定日時をDate型として取得
        Date finalLimit = getConvertedSettingTimeDate();
        if( finalLimit == null ){
            //設定なし or 変換エラーの場合、無効文字列を設定
            String noInputStr = mContext.getString(R.string.limittime_no_input);
            viewHolder.tv_taskStartTime.setText(noInputStr);
            return;
        }

        //「やること」開始時刻のカレンダーを取得
        Calendar startCalendar = getStartTimeCalendar(i, finalLimit);

        //開始時間を取得し、文字列に変換
        Date startDate       = startCalendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.JAPANESE);
        String startTimeStr  = sdf.format(startDate);

        //ラベルの設定
        setLabel(viewHolder, i, startCalendar);

        //「開始時間」として設定
        viewHolder.tv_taskStartTime.setText(startTimeStr);
    }


    /*
     * 「やること」終了時刻の設定
     */
    private void setupTaskEndTime(StackTaskViewHolder viewHolder, int i ) {

        //設定日時をDate型として取得
        Date finalLimit = getConvertedSettingTimeDate();
        if( finalLimit == null ){
            //設定なし or 変換エラーの場合、無効文字列を設定
            String noInputStr = mContext.getString(R.string.limittime_no_input);
            viewHolder.tv_taskEndTime.setText(noInputStr);
            return;
        }

        //「やること」終了時刻のカレンダーを取得
        Calendar endCalendar = getEndTimeCalendar(i, finalLimit);

        //終了時間を取得し、文字列に変換
        Date endDate         = endCalendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.JAPANESE);
        String endTimeStr    = sdf.format(endDate);

        //ラベルの設定（スタートベースの場合、ラベル設置なし）
        viewHolder.ll_label.setVisibility(View.INVISIBLE);

        //「終了時間」として設定
        viewHolder.tv_taskEndTime.setText(endTimeStr);
    }

    /*
     * 設定時刻の取得（Date型）
     */
    private Date getConvertedSettingTimeDate() {

        //リミット時刻
        String limitTimeStr = mtv_limitTime.getText().toString();
        //時刻指定なし文字列
        String noInputStr = mContext.getString(R.string.limittime_no_input);
        if ( limitTimeStr.equals(noInputStr) ) {
            return null;
        }

        //期限日と期限時間を連結
        String limitStr = mtv_limitDate.getText().toString() + " " + limitTimeStr;

        //リミット日時をDate型へ変換
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPANESE);

        Date finalLimit;
        try {
            //文字列をDate型に変換
            finalLimit = sdf.parse(limitStr);

        } catch (ParseException e) {
            e.printStackTrace();

            //例外発生なら、nullを返す
            return null;
        }

        //Date型で返す
        return finalLimit;
    }

    /*
     * 「やること」の開始時刻（カレンダー）の取得
     *   取得に伴い、アラーム時刻のリストに開始時刻を追加する
     */
    private Calendar getStartTimeCalendar(int i, Date finalLimit) {

        //リミットから引く時間を計算
        int totalTaskMin = getTotalTaskTimeToLast(i);

        //開始時間を計算
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(finalLimit);
        calendar.add(Calendar.MINUTE, -totalTaskMin);

        //アラーム時間を設定
        mData.get(i).setAlarmCalendar(calendar);

        return calendar;
    }

    /*
     * 「やること」の終了時刻（カレンダー）の取得
     *   取得に伴い、アラーム時刻のリストに終了時刻を追加する
     */
    private Calendar getEndTimeCalendar(int i, Date startTime) {

        //スタート時間に加算する値を取得
        int totalTaskMin = getTotalTaskTimeFromTop(i);

        //開始時間を計算
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime);
        calendar.add(Calendar.MINUTE, totalTaskMin);

        //アラーム時間を設定
        mData.get(i).setAlarmCalendar(calendar);

        return calendar;
    }

    /*
     * 「やること」ラベルの設定
     *   ラベルを設定する必要がない場合、何もしない
     */
    private void setLabel(StackTaskViewHolder viewHolder, int i, Calendar calendar) {

        //現在時刻
        Date nowTime = new Date();
        //開始時刻
        Date startTime = calendar.getTime();

        //現在時刻が開始時刻よりも前の場合
        if (nowTime.before(startTime)) {
            //ラベル設定はなし
            viewHolder.ll_label.setVisibility(View.INVISIBLE);
            return;
        }

        //「やること時間」取得
        int taskTime = mData.get(i).getTaskTime();

        //終了時刻を取得
        calendar.add(Calendar.MINUTE, taskTime);
        Date endTime = calendar.getTime();

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
    public void setInsertAnimation(int idx) {
        //リミット指定：0からこのIndexまで、アイテム追加アニメーションを適用
        //スタート指定：このIndexから最後のIndexまで、アイテム追加アニメーションを適用
        mAnimIdx = idx;
    }

    /*
     * やること時間の累計（指定Index～LastIndex）
     */
    private int getTotalTaskTimeToLast(int idx){

        int minute = 0;

        int size = mData.size();
        for( int i = idx; i < size; i++ ){
            //やること時間を累算
            minute += mData.get(i).getTaskTime();
        }

        return minute;
    }

    /*
     * やること時間の累計（TopIndex～指定Index）
     */
    private int getTotalTaskTimeFromTop(int idx){

        int minute = 0;

        for( int i = 0; i <= idx; i++ ){
            //やること時間を累算
            minute += mData.get(i).getTaskTime();
        }

        return minute;
    }

    /*
     * リミットか否かを反転
     */
    public void reverseTime(){
        mIsLimit = !mIsLimit;

        if( mIsLimit ){
            //スタックアニメーション
            mAddAnimationID = R.anim.stack_task;

        } else {
            //キューアニメーション
            mAddAnimationID = R.anim.que_task;
        }
    }

    /*
     * ビューにdrawableを適用
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void applyDrawableResorce(View view, int time) {

        //適用対象のビューを取得
        LinearLayout ll = view.findViewById(R.id.ll_taskInfo);

        //表示目的に応じて、drawableリソースを取得
        Drawable drawable = mContext.getDrawable(R.drawable.frame_item_task);;

        //時間に応じて、色を設定
        int colorId = ResourceManager.getTaskTimeColorId(time);;
        drawable.setTint(mContext.getColor( colorId ));

        ll.setBackground(drawable);
    }

}

