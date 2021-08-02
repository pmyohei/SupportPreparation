package com.example.supportpreparation;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;


/*
 * 「やること」新規生成のダイアログ
 */
public class CreateTaskDialog extends DialogFragment {

    private boolean updateFlg;          //更新フラグ
    private String  preTask;            //更新前-「やること」
    private int     preTaskTime;        //更新前-「やること時間」
    private AsyncTaskTableOperaion.TaskOperationListener listener;

    /*
     * コンストラクタ
     */
    public CreateTaskDialog(AsyncTaskTableOperaion.TaskOperationListener listener) {
        this.listener = listener;
        //初期値：非更新
        this.updateFlg = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //ダイアログにレイアウトを設定
        return inflater.inflate(R.layout.dialog_create_task, container, false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        //ダイアログ取得
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        //背景を透明にする(デフォルトテーマに付いている影などを消す) ※これをしないと、画面横サイズまで拡張されない
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        //アニメーションを設定
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;

        //ダイアログを返す
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        //ダイアログ取得
        Dialog dialog = getDialog();

        //-- ダイアログデザインの設定
        //画面メトリクスの取得
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        //レイアウトパラメータの取得
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.width   = metrics.widthPixels;                   //横幅=画面サイズ
        lp.gravity = Gravity.BOTTOM;                        //位置=画面下部
        //ダイアログのデザインとして設定
        dialog.getWindow().setAttributes(lp);

        //---- ビューの設定も本メソッドで行う必要あり（onCreateDialog()内だと落ちる）
        //-- NumberPicker の設定
        //ビュー取得
        NumberPicker np100th = (NumberPicker) dialog.findViewById(R.id.np_dialogTime100th);
        NumberPicker np10th  = (NumberPicker) dialog.findViewById(R.id.np_dialogTime10th);
        NumberPicker np1th   = (NumberPicker) dialog.findViewById(R.id.np_dialogTime1th);
        //値の範囲を設定
        np100th.setMaxValue(9);
        np100th.setMinValue(0);
        np10th.setMaxValue(9);
        np10th.setMinValue(0);
        np1th.setMaxValue(9);
        np1th.setMinValue(0);

        //呼び出し元から情報を取得
        this.preTask        = getArguments().getString("TaskName");
        this.preTaskTime    = getArguments().getInt("TaskTime");

        //更新であれば
        if( this.preTask != null ){
            //-- 入力済みデータの設定
            //「やること」
            EditText et_task = (EditText) dialog.findViewById(R.id.et_dialogTask);
            et_task.setText(this.preTask);

            //「やることの時間」
            np100th.setValue( this.preTaskTime / 100 );
            np10th.setValue( (this.preTaskTime / 10) % 10 );
            np1th.setValue( this.preTaskTime % 10 );

            //更新フラグを「更新」に
            this.updateFlg = true;
        }

        //-- 「保存ボタン」のリスナー設定
        Button btEntry = (Button)dialog.findViewById(R.id.bt_entryTask);
        btEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //タスク名を取得
                String task = ((EditText) dialog.findViewById(R.id.et_dialogTask)).getText().toString();

                //時間を取得
                int time;
                NumberPicker inputTime = (NumberPicker) dialog.findViewById(R.id.np_dialogTime100th);
                time = inputTime.getValue() * 100;
                inputTime = (NumberPicker) dialog.findViewById(R.id.np_dialogTime10th);
                time += inputTime.getValue() * 10;
                inputTime = (NumberPicker) dialog.findViewById(R.id.np_dialogTime1th);
                time += inputTime.getValue();

                //-- フォーマットチェック
                if ((task.isEmpty()) || (time == 0)) {
                    //未入力の場合、エラー表示
                    ((TextView) dialog.findViewById(R.id.tv_alert)).setText("未入力です");

                } else {
                    //正常入力されれば、エラー表示をクリア
                    ((TextView) dialog.findViewById(R.id.tv_alert)).setText("");

                    //-- DBへ保存
                    //DB取得
                    AppDatabase db = AppDatabaseSingleton.getInstanceNotFirst();

                    //新規作成か更新か
                    if (updateFlg) {
                        //更新
                        new AsyncTaskTableOperaion(db, listener, AsyncTaskTableOperaion.DB_OPERATION.UPDATE, preTask, preTaskTime, task, time).execute();

                    } else {
                        //新規生成
                        new AsyncTaskTableOperaion(db, listener, AsyncTaskTableOperaion.DB_OPERATION.CREATE, task, time).execute();
                    }

                    //ダイアログ閉じる
                    dismiss();
                }
            }
        });

        return;
    }

}
