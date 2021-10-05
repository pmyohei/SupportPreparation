package com.example.supportpreparation;

import static android.view.View.GONE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.supportpreparation.ui.stackManager.StackManagerFragment;

import java.util.Date;
import java.util.List;


/*
 * 「やること」新規生成のダイアログ
 */
public class CreateSetAlarmDialog extends DialogFragment {

    private final StackTaskTable mStackTable;
    private final Fragment mFragment;
    private LayoutInflater mInflater;
    private final boolean mIsNew;

    /*
     * コンストラクタ
     */
    public CreateSetAlarmDialog(Fragment fragment, StackTaskTable stackTable, boolean isNew) {
        mFragment = fragment;
        mStackTable = stackTable;
        mIsNew = isNew;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //インフレータ
        mInflater = inflater;

        //ダイアログにレイアウトを設定
        return inflater.inflate(R.layout.dialog_alarm_set, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //ダイアログ
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        //背景を透明にする(デフォルトテーマに付いている影などを消す)
        //※これをしないと、画面横サイズまで拡張されない
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        //ダイアログを返す
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        //ダイアログ取得
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }

        //サイズ設定
        setupDialogSize(dialog);

        //アラーム行の追加先レイアウト
        LinearLayout ll_alarmList = dialog.findViewById(R.id.ll_alarmList);

        //アラームリスト
        setupTaskAlarm(ll_alarmList);

        //最終時刻の設定
        setupFinalLimitAlarm(ll_alarmList);

        //すべてONボタン
        Button bt_allOn = dialog.findViewById(R.id.bt_allOn);
        bt_allOn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //すべてON
                allChangeSwitch(ll_alarmList, true);
            }
        });

        //すべてOFFボタン
        Button bt_allOff = dialog.findViewById(R.id.bt_allOff);
        bt_allOff.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //すべてON
                allChangeSwitch(ll_alarmList, false);
            }
        });

        //アラーム設定ボタン
        Button bt_setAlarm = dialog.findViewById(R.id.bt_setAlarm);
        bt_setAlarm.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //本体データのアラームON／OFFをすべて更新
                updateOnAlarm(ll_alarmList);

                //呼び出し元の処理を呼びだす
                ((StackManagerFragment) mFragment).OnAlarmSetReturn(mIsNew);

                //閉じる
                dismiss();
            }
        });
    }

    /*
     * ダイアログサイズ設定
     */
    private void setupDialogSize(Dialog dialog) {

        Window window = dialog.getWindow();

        //画面メトリクスの取得
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        //レイアウトパラメータ
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = (int) ((float) metrics.widthPixels * 0.8);
        lp.height = (int) ((float) metrics.heightPixels * 0.6);

        //サイズ設定
        window.setAttributes(lp);
    }

    /*
     * やることアラームリストの設定
     */
    private void setupTaskAlarm(LinearLayout ll_alarmList) {

        TaskArrayList<TaskTable> StackTaskList = mStackTable.getStackTaskList();

        //積まれたやること分
        for (TaskTable task : StackTaskList) {
            View alarmInfo = mInflater.inflate(R.layout.element_alarm, ll_alarmList, false);

            //アラーム情報
            setupAlarmInfo(alarmInfo, task);

            ll_alarmList.addView(alarmInfo);
        }
    }

    /*
     * 最終時刻アラームの設定
     */
    private void setupFinalLimitAlarm(LinearLayout ll_alarmList) {

        View alarmInfo = mInflater.inflate(R.layout.element_alarm, ll_alarmList, false);

        //アラーム情報
        setupAlarmInfo(alarmInfo, null);

        ll_alarmList.addView(alarmInfo);
    }

    /*
     * アラーム情報の設定
     */
    private void setupAlarmInfo(View alarmInfo, TaskTable task) {

        TextView tv_taskName = (TextView) alarmInfo.findViewById(R.id.tv_taskName);
        TextView tv_alarmTime = (TextView) alarmInfo.findViewById(R.id.tv_alarmTime);
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch sw_alarmOn = (Switch) alarmInfo.findViewById(R.id.sw_alarmOn);

        String timeName;
        Date alarmDate;
        boolean onAlarm;

        if (task == null) {
            //最終時刻文字列
            Context context = tv_taskName.getContext();
            timeName = context.getString(R.string.dialog_final_time);

            //最後のやることの終了時間
            TaskArrayList<TaskTable> StackTaskList = mStackTable.getStackTaskList();
            int last = StackTaskList.getLastIdx();

            alarmDate = StackTaskList.get(last).getEndCalendar().getTime();

            //アラームON／OFF
            onAlarm = mStackTable.isOnAlarm();

        } else {

            //やること
            timeName = task.getTaskName();

            //開始時刻（アラーム時刻）
            alarmDate = task.getStartCalendar().getTime();

            //アラームON/OFF
            onAlarm = task.isOnAlarm();
        }

        //設定
        tv_taskName.setText(timeName);

        String alarmStr = ResourceManager.sdf_DateAndTime.format(alarmDate);
        tv_alarmTime.setText(alarmStr);

        sw_alarmOn.setChecked(onAlarm);

        //もし、アラームが既に経過していれば、表示を無効化
        Date now = new Date();
        if( now.after(alarmDate) ){
            //アラーム時間に取り消し線を設定
            TextPaint paint = tv_alarmTime.getPaint();
            paint.setFlags(tv_alarmTime.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            //無効化
            sw_alarmOn.setChecked(false);
            sw_alarmOn.setEnabled(false);
        }
    }


    /*
     * 全アラームのSwitch状態を変更
     */
    private void allChangeSwitch(LinearLayout ll_alarmList, boolean onAlarm) {

        int childNum = ((ViewGroup) ll_alarmList).getChildCount();

        //アラーム行数分
        for (int i = 0; i < childNum; i++) {
            //子ビューを取得
            LinearLayout ll = (LinearLayout) ((ViewGroup) ll_alarmList).getChildAt(i);

            @SuppressLint("UseSwitchCompatOrMaterialCode")
            Switch sw_alarmOn = (Switch) ll.findViewById(R.id.sw_alarmOn);
            if( sw_alarmOn.isEnabled() ){
                //有効状態であれば、設定
                sw_alarmOn.setChecked(onAlarm);
            }
        }
    }

    /*
     * 本体データのアラームON/OFF情報を更新
     */
    private void updateOnAlarm(LinearLayout ll_alarmList) {

        TaskArrayList<TaskTable> StackTaskList = mStackTable.getStackTaskList();
        List<Boolean> AlarmOnOffList = mStackTable.getAlarmOnOffList();

        int childNum = ((ViewGroup) ll_alarmList).getChildCount();
        int lastIdx = childNum - 1;

        //アラーム行数分
        for (int i = 0; i < childNum; i++) {
            //子ビューを取得
            LinearLayout ll = (LinearLayout) ((ViewGroup) ll_alarmList).getChildAt(i);

            @SuppressLint("UseSwitchCompatOrMaterialCode")
            Switch sw_alarmOn = (Switch) ll.findViewById(R.id.sw_alarmOn);
            boolean isChecked = sw_alarmOn.isChecked();

            if( i == lastIdx ){
                //最後は、最終時刻のアラーム
                mStackTable.setOnAlarm( isChecked );

            } else {
                //最後より前は、やることのアラーム
                StackTaskList.get(i).setOnAlarm( isChecked );

                //アラーム更新
                AlarmOnOffList.set(i, isChecked);
            }
        }
    }

}
