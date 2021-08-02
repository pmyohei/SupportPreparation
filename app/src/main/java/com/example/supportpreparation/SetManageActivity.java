package com.example.supportpreparation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/*
 * 「やることセット」管理画面
 */
public class SetManageActivity extends AppCompatActivity implements AsyncSetTableOperaion.SetOperationListener,
                                                                    AsyncTaskTableOperaion.TaskOperationListener {

    private AppDatabase     db;                     //DB
//    private LinearLayout    ll_rootDisplay;         //「やること」表示元のビューID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_manage);
        //-- レイアウト関係

        //「やること」表示元のビューIDを保持
        //this.ll_rootDisplay = (LinearLayout) findViewById(R.id.ll_rootCreatedTask);

        //DB操作インスタンスを取得
        this.db = AppDatabaseSingleton.getInstance(getApplicationContext());

        //-- test
        findViewById(R.id.bt_createTask).setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //やることセット登録ダイアログの生成
                createTaskSetDialog();
            }
        });

        findViewById(R.id.testaddbt).setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //セットへやることを追加

            }
        });
        //--

        //現在登録されている「やること」を表示
        this.displaySetList();
    }

    /*
     * タスク生成ダイアログの生成
     */
    private void createTaskSetDialog(){
        //Bundle生成
        Bundle bundle = new Bundle();
        //FragmentManager生成
        FragmentManager transaction = getSupportFragmentManager();

        //ダイアログを生成
        DialogFragment dialog = new CreateSetDialog();
        dialog.setArguments(bundle);
        dialog.show(transaction, "CreateTask");
    }

    /*
     * 「やること」の表示
     *    登録済みの「やること」データを全て表示する。
     */
    private void displaySetList(){

        //-- 非同期スレッドにて、読み込み開始
        //「やること」
        new AsyncTaskTableOperaion(this.db, this, AsyncTaskTableOperaion.DB_OPERATION.READ).execute();
        //「やることセット」
        new AsyncSetTableOperaion(this.db, this, AsyncSetTableOperaion.DB_OPERATION.READ).execute();
    }

    /*
     * 「やること」ビューの作成・レイアウトへの追加
     *    「やること」データ単体のレイアウトを作り、ルートレイアウトへ追加する。
     */
    private void addDisplayUnitTask(TaskTable task){
        /*
        //--「やること」のレイアウトインフレータを取得
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //「やること」データのビュー
        View taskLayout = inflater.inflate(R.layout.unit_task_for_taskset, null);

        //-- 「やること」にリスナーを登録
        //リスナー設定ビューの取得
        LinearLayout ll_taskInfo = taskLayout.findViewById(R.id.ll_taskInfo);
        //リスナー設定
        ll_taskInfo.setOnClickListener(
                new TaskSelectListener()
        );

        ll_taskInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i("test", "is=" + v.isEnabled());

                if(v.isEnabled()){
                    //有効 → 無効
                    Log.i("test", "false");
                    //v.setEnabled(false);
                } else {
                    //無効 → 有効
                    Log.i("test", "true");
                    v.setEnabled(true);
                }
            }
        });

        //-- 「やること」データの表示内容を設定
        //「やること」
        TextView tv_data = taskLayout.findViewById(R.id.tv_taskName);
        tv_data.setText(task.getTaskName());
        //「やること」の時間
        tv_data = taskLayout.findViewById(R.id.tv_taskTime);
        tv_data.setText(task.getTaskTime() + " min");

        //「やること」データを表示先のビューに追加
        this.ll_rootDisplay.addView( taskLayout );
        */
    }

    /*
     * 「やること」選択リスナー
     */
    private class TaskSelectListener implements View.OnClickListener{

        boolean isDisplay;              //操作アイコンの表示状態
        LinearLayout ll_targetView;     //操作対象ビュー
        Animation animation_in;         //アニメーション

        TaskSelectListener() {
            this.isDisplay = false;
            //this.ll_targetView = view;
            this.animation_in = AnimationUtils.loadAnimation(SetManageActivity.this, R.anim.unit_open_ctrl);
        }
        TaskSelectListener(LinearLayout view) {
            this.isDisplay = false;
            //this.ll_targetView = view;
            this.animation_in = AnimationUtils.loadAnimation(SetManageActivity.this, R.anim.unit_open_ctrl);
        }

        @Override
        public void onClick(View view) {
            /*
            //-- 操作UIの表示/非表示
            //表示中
            if(isDisplay){
                //-- 隠す
                this.ll_targetView.setVisibility(View.GONE);
                //control.startAnimation(animation_out);
                this.isDisplay = false;

                //非表示中
            }else{
                //-- 表示する
                this.ll_targetView.setVisibility(View.VISIBLE);
                this.ll_targetView.startAnimation(this.animation_in);
                this.isDisplay = true;
            }
             */
        }
    }


    /*
     *  -------------------------------------------------
     *  インターフェース
     *  -------------------------------------------------
     */

    /* -------------------
     * 「やることセット」
     */

    @Override
    public void onSuccessSetRead(List<SetTable> setList, List<List<TaskTable>> tasksList) {

        Log.i("test", "start onSuccessSetRead");

        //-- 「やることセット」の表示
        //レイアウトからリストビューを取得
        RecyclerView rv  = (RecyclerView) findViewById(R.id.rv_setList);

        //レイアウトマネージャの生成・設定（横スクロール）
        LinearLayoutManager l_manager = new LinearLayoutManager(this);
        l_manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        rv.setLayoutManager(l_manager);

        Log.i("test", "onSuccessSetRead");

        //アダプタの生成・設定
        SetRecyclerAdapter adapter = new SetRecyclerAdapter(this, setList, tasksList);
        rv.setAdapter(adapter);
    }

    @Override
    public void onSuccessSetCreate(Integer code, String task) {

        //-- 作成結果をトーストで表示
        //結果メッセージ
        String message;

        //戻り値に応じてトースト表示
        if( code == -1 ){
            //エラーメッセージを表示
            message = "登録済みです";
        } else {
            //正常メッセージを表示
            message = "登録しました";
        }

        //トーストの生成
        Toast toast = new Toast(getApplicationContext());
        toast.setText(message);
        //toast.setGravity(Gravity.CENTER, 0, 0);   //E/Toast: setGravity() shouldn't be called on text toasts, the values won't be used
        toast.show();

        if( code == -1 ){
            //登録済みなら、ここで終了
            return;
        }


        return;
    }

    @Override
    public void onSuccessSetDelete(String task) {

    }

    @Override
    public void onSuccessSetUpdate(String preTask, String task) {

    }

    /* -------------------
     * 「やること」
     */

    @Override
    public void onSuccessTaskRead(List<TaskTable> taskList) {

        //-- 「やること」の表示（セットへ追加の選択用）
        //レイアウトからリストビューを取得
        RecyclerView rv  = (RecyclerView) findViewById(R.id.rv_taskList);

        //レイアウトマネージャの生成・設定（横スクロール）
        LinearLayoutManager ll_manager = new LinearLayoutManager(this);
        ll_manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        rv.setLayoutManager(ll_manager);

        //アダプタの生成・設定
        TaskRecyclerAdapter adapter = new TaskRecyclerAdapter(this, taskList);
        rv.setAdapter(adapter);
    }

    @Override
    public void onSuccessTaskCreate(Integer code, TaskTable taskTable) {
        //do nothing
    }
    @Override
    public void onSuccessTaskDelete(String task, int taskTime) {
        //do nothing
    }
    @Override
    public void onSuccessTaskUpdate(String preTask, int preTaskTime, TaskTable updatedTask) {
        //do nothing
    }

    /*
     *  -------------------------------------------------
     *  インナークラス
     *  -------------------------------------------------
     */




}