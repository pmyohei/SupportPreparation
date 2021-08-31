package com.example.supportpreparation;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

    private List<TaskTable>             mData;
    private Context                     mContext;

    private TextView                    mtv_limitDate;          //リミット日のTextView：ユーザー設定変更の内容反映のために保持する
    private TextView                    mtv_limitTime;          //リミット時間のTextView：ユーザー設定変更の内容反映のために保持する

    private List<Calendar>              mAlarmList;

    /*
     * ViewHolder：リスト内の各アイテムのレイアウトを含む View のラッパー
     * (固有のためインナークラスで定義)
     */
    class StackTaskViewHolder extends RecyclerView.ViewHolder {

        private TextView tv_pid;            //Pid
        private TextView tv_taskName;       //表示内容
        private TextView tv_taskTime;
        private TextView tv_taskStartTime;


        /*
         * コンストラクタ
         */
        public StackTaskViewHolder(View itemView) {
            super(itemView);
            tv_pid      = (TextView) itemView.findViewById(R.id.tv_pid);
            tv_taskName = (TextView) itemView.findViewById(R.id.tv_taskName);
            tv_taskTime = (TextView) itemView.findViewById(R.id.tv_taskTime);
            tv_taskStartTime = (TextView) itemView.findViewById(R.id.tv_taskStartTime);
        }
    }

    /*
     * コンストラクタ
     */
    public StackTaskRecyclerAdapter(Context context, List<TaskTable> data, TextView limitDate, TextView limitTime) {
        mData         = data;
        mContext      = context;
        mtv_limitDate = limitDate;
        mtv_limitTime = limitTime;

        //アラーム時間リスト
        mAlarmList = new ArrayList<>();
    }

    /*
     * ここで返した値が、onCreateViewHolder()の第２引数になる
     */
    @Override
    public int getItemViewType(int position) {
        Log.i("test", "stack getItemViewType id=" + mData.get(position).getId());
        return mData.get(position).getTaskTime();
    }

    /*
     *　ViewHolderの生成
     */
    @Override
    public StackTaskViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        //レイアウトIDを取得
        int id = getLayoutId(viewType);

        //表示レイアウトの設定
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(id, viewGroup, false);

        return new StackTaskViewHolder(view);
    }


    /*
     * ViewHolderの設定
     */
    @Override
    public void onBindViewHolder(StackTaskViewHolder viewHolder, final int i) {

        //文字列変換
        int    taskTime = mData.get(i).getTaskTime();
        String timeStr  = Integer.toString( taskTime );
        String pidStr   = Integer.toString( mData.get(i).getId() );

        //データ設定
        viewHolder.tv_pid.setText(pidStr);
        viewHolder.tv_taskName.setText(mData.get(i).getTaskName());
        viewHolder.tv_taskTime.setText(timeStr);

        //-- やること開始時間の算出と設定
        Log.i("test", "mTest=" + mtv_limitTime.getText().toString());

        //文字列-リミット時間
        String limitTimeStr = mtv_limitTime.getText().toString();

        String noInputStr = mContext.getString(R.string.limittime_no_input);
        if ( limitTimeStr.equals(noInputStr) ) {
            //未設定なら、無効値を表示
            viewHolder.tv_taskStartTime.setText(noInputStr);
            return;
        }

        //-- 時間が設定されているなら、開始時間を計算

        //期限日と期限時間を連結
        String limitStr = mtv_limitDate.getText().toString() + " " + limitTimeStr;
        //Date型へ変換
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPANESE);

        Date finalLimit;
        try {
            //文字列をDate型に変換
            finalLimit = sdf.parse(limitStr);
        } catch (ParseException e) {
            e.printStackTrace();

            //例外発生なら、無効値を表示
            viewHolder.tv_taskStartTime.setText(noInputStr);
            return;
        }

        //リミットから引く時間を計算
        int totalTaskMin = getTotalTaskTime(i);

        //開始時間を計算
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(finalLimit);
        calendar.add(Calendar.MINUTE, -totalTaskMin);

        //アラーム時間として追加
        mAlarmList.add(calendar);

        //開始時間を取得し、文字列に変換
        Date startDate = calendar.getTime();
        sdf = new SimpleDateFormat("HH:mm", Locale.JAPANESE);
        String startTimeStr = sdf.format(startDate);

        //「開始時間」として設定
        viewHolder.tv_taskStartTime.setText(startTimeStr);
    }

    /*
     * 表示データ数の取得
     */
    @Override
    public int getItemCount() {
        //表示データ数を返す
        Log.i("test", "stack getItemCount");
        return mData.size();
    }

    /*
     * アラーム時間リストクリア
     */
    public void clearAlarmList() {
        //アラームリストをクリア
        mAlarmList.clear();
    }

    /*
     * アラーム時間リスト取得
     */
    public List<Calendar> getAlarmList() {
        //アラームリストを取得
        return mAlarmList;
    }

    /*
     * リミットから引く時間を計算
     *   指定したIndexより後ろのやること時間を累計する
     */
    private int getTotalTaskTime(int idx){

        int minute = 0;

        int size = mData.size();
        for( int i = idx; i < size; i++ ){
            //やること時間を累算
            minute += mData.get(i).getTaskTime();
        }

        return minute;
    }


    /*
     * カラーIDの取得
     */
    private int getLayoutId(int time){

        int id;

        if( time <= 5 ){
            id = R.layout.outer_task_for_stack_very_short;
        } else if( time <= 10 ){
            id = R.layout.outer_task_for_stack_short;
        } else if( time <= 30 ){
            id = R.layout.outer_task_for_stack_normal;
        } else if( time <= 60 ){
            id = R.layout.outer_task_for_stack_long;
        } else {
            id = R.layout.outer_task_for_stack_very_long;
        }
        return id;
    }

}

