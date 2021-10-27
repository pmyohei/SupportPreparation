package com.example.supportpreparation;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;


/*
 * ダイアログ：「グループ」生成・更新
 */
public class CreateGroupDialog extends DialogFragment {

    //フィールド変数
    private final boolean   mIsEdit;                            //更新フラグ
    private String          mPreGroupName;                      //更新前-「やることグループ名」
    private final AsyncGroupTableOperaion.GroupOperationListener
                            mListener;                          //呼び出し元リスナー

    /*
     * コンストラクタ
     */
    public CreateGroupDialog(AsyncGroupTableOperaion.GroupOperationListener listener, boolean editFlg) {
        mListener = listener;
        mIsEdit = editFlg;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //ダイアログにレイアウトを設定
        return inflater.inflate(R.layout.dialog_create_group, container, false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        //ダイアログ取得
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        //背景を透明にする(デフォルトテーマに付いている影などを消す)
        //※これをしないと、画面横サイズまで拡張されない
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
        if( dialog == null ){
            return;
        }

        //ダイアログデザインの設定
        setupDialogSize(dialog);

        //---- ビューの設定も本メソッドで行う必要あり（onCreateDialog()内だと落ちる）
        //編集の場合
        if(mIsEdit){
            //呼び出し元から情報を取得
            mPreGroupName = getArguments().getString(ResourceManager.KEY_GROUP_NAME);

            //「やることグループ」を更新
            EditText et_task = (EditText) dialog.findViewById(R.id.et_groupName);
            et_task.setText(mPreGroupName);
        }

        //「保存ボタン」のリスナー設定
        Button bt_entry = (Button)dialog.findViewById(R.id.bt_entryGroup);
        bt_entry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //グループ名を取得
                String groupName = ((EditText) dialog.findViewById(R.id.et_groupName)).getText().toString();

                //-- フォーマットチェック
                if (groupName.isEmpty()) {
                    //未入力の場合、エラー表示
                    ((TextView) dialog.findViewById(R.id.tv_alert)).setText( R.string.no_input );

                } else {
                    //正常入力されれば、エラー表示をクリア
                    ((TextView) dialog.findViewById(R.id.tv_alert)).setText("");

                    //DB取得
                    AppDatabase db = AppDatabaseSingleton.getInstanceNotFirst();

                    if (mIsEdit) {
                        //更新
                        new AsyncGroupTableOperaion(db, mListener, AsyncGroupTableOperaion.DB_OPERATION.UPDATE, mPreGroupName, groupName).execute();
                    } else {
                        //新規生成
                        new AsyncGroupTableOperaion(db, mListener, AsyncGroupTableOperaion.DB_OPERATION.CREATE, groupName).execute();
                    }

                    //ダイアログ閉じる
                    dismiss();
                }
            }
        });
    }

    /*
     * ダイアログサイズ設定
     */
    private void setupDialogSize( Dialog dialog ){

        Window window= dialog.getWindow();

        //画面メトリクスの取得
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        //レイアウトパラメータ
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width   = metrics.widthPixels;
        lp.gravity = Gravity.BOTTOM;

        //サイズ設定
        window.setAttributes(lp);
    }


}
