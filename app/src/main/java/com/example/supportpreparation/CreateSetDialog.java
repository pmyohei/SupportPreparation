package com.example.supportpreparation;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;


/*
 * 「やること」新規生成のダイアログ
 */
public class CreateSetDialog extends DialogFragment {

    private boolean updateFlg;          //更新フラグ
    private String  preSet;             //更新前-「やることセット名」

    /*
     * コンストラクタ
     */
    public CreateSetDialog() {
        //初期値：非更新
        this.updateFlg = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //ダイアログにレイアウトを設定
        return inflater.inflate(R.layout.dialog_create_taskset, container, false);
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

        //呼び出し元から情報を取得
        this.preSet = getArguments().getString("TaskSetName");

        //更新であれば
        if( this.preSet != null ){
            //-- 入力済みデータの設定
            //「やることセット」
            EditText et_task = (EditText) dialog.findViewById(R.id.et_dialogTaskSet);
            et_task.setText(this.preSet);

            //更新フラグを「更新」に
            this.updateFlg = true;
        }

        //-- 「保存ボタン」のリスナー設定
        Button btEntry = (Button)dialog.findViewById(R.id.bt_entryTaskSet);
        btEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //セット名を取得
                String taskSet = ((EditText) dialog.findViewById(R.id.et_dialogTaskSet)).getText().toString();

                //-- フォーマットチェック
                if (taskSet == "") {
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
                        new AsyncSetTableOperaion(db, (SetManageActivity) getActivity(), AsyncSetTableOperaion.DB_OPERATION.UPDATE, preSet, taskSet).execute();

                    } else {
                        Log.i("test", "save");
                        //新規生成
                        new AsyncSetTableOperaion(db, (SetManageActivity) getActivity(), AsyncSetTableOperaion.DB_OPERATION.CREATE, taskSet).execute();
                    }

                    //ダイアログ閉じる
                    dismiss();
                }
            }
        });

        return;
    }

}
