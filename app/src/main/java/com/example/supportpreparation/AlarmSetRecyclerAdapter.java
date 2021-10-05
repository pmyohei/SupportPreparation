package com.example.supportpreparation;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;

/*
 * RecyclerViewアダプター：「やること」用
 */
public class AlarmSetRecyclerAdapter extends RecyclerView.Adapter<AlarmSetRecyclerAdapter.AlarmViewHolder> {

    private StackTaskTable              mStackTable;                 //スタックテーブル
    private TaskArrayList<TaskTable>    mData;             //積み上げ「やること」
    private LayoutInflater              mInflater;

    /*
     * ViewHolder：リスト内の各アイテムのレイアウトを含む View のラッパー
     * (固有のためインナークラスで定義)
     */
    class AlarmViewHolder extends RecyclerView.ViewHolder {

        private final TextView    tv_taskName;
        private final TextView    tv_alarmTime;
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        private final Switch      sw_alarmOn;

        /*
         * コンストラクタ
         */
        public AlarmViewHolder(View itemView) {
            super(itemView);
            tv_taskName  = (TextView) itemView.findViewById(R.id.tv_taskName);
            tv_alarmTime = (TextView) itemView.findViewById(R.id.tv_alarmTime);
            sw_alarmOn   = (Switch) itemView.findViewById(R.id.sw_alarmOn);
        }
    }

    /*
     * コンストラクタ
     */
    public AlarmSetRecyclerAdapter( LayoutInflater inflater, StackTaskTable stackTable ) {
        mStackTable = stackTable;
        mData = stackTable.getStackTaskList();
        mInflater = inflater;

    }

    /*
     * ここで返した値が、onCreateViewHolder()の第２引数になる

    @Override
    public int getItemViewType(int position) {

        if (mData.get(position) == null) {
            //フェールセーフ
            Log.i("failsafe", "adapter data is null. mSetting=" + mSetting);
            return 0;
        }

        return mData.get(position).getTaskTime();
    }
     */

    /*
     *　ViewHolderの生成
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public AlarmViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        //アラーム１行のレイアウト
        View view = mInflater.inflate( R.layout.element_alarm, viewGroup, false);

        return new AlarmViewHolder(view);
    }


    /*
     * ViewHolderの設定
     */
    @Override
    public void onBindViewHolder(AlarmViewHolder viewHolder, final int i) {

        TaskTable task = mData.get(i);

        //やること
        viewHolder.tv_taskName.setText(task.getTaskName());

        //開始時刻（アラーム時刻）
        Date date       = task.getStartCalendar().getTime();
        String alarmStr = ResourceManager.sdf_DateAndTime.format(date);

        viewHolder.tv_alarmTime.setText(alarmStr);

        //アラームON/OFF
        boolean onAlarm = task.isOnAlarm();
        viewHolder.sw_alarmOn.setChecked(onAlarm);

        //アラームONリスナー
        //SwitchChangeListerner listerner = new SwitchChangeListerner(task);
        //viewHolder.sw_alarmOn.setOnCheckedChangeListener( listerner );
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
     * インナークラス
     *   アラームON/OFFスイッチリスナー
     */
    private class SwitchChangeListerner implements CompoundButton.OnCheckedChangeListener{

        //private final int mIdx;
        private final TaskTable mTask;

        public SwitchChangeListerner(TaskTable task){
            mTask = task;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if( isChecked ) {
                //mSwitch : Off -> On の時の処理
                //buttonView.setChecked( isChecked );
                mTask.setOnAlarm( isChecked );
            } else {
                //mSwitch : On -> Off の時の処理
                mTask.setOnAlarm( isChecked );
            }
        }
    }


}