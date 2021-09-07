package com.example.supportpreparation.ui.stackManager;

import static android.content.Context.ALARM_SERVICE;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.supportpreparation.AlarmBroadcastReceiver;
import com.example.supportpreparation.AppDatabase;
import com.example.supportpreparation.AppDatabaseSingleton;
import com.example.supportpreparation.AsyncTaskTableOperaion;
import com.example.supportpreparation.GroupSelectRecyclerAdapter;
import com.example.supportpreparation.GroupTable;
import com.example.supportpreparation.MainActivity;
import com.example.supportpreparation.R;
import com.example.supportpreparation.StackTaskRecyclerAdapter;
import com.example.supportpreparation.TaskRecyclerAdapter;
import com.example.supportpreparation.TaskTable;
import com.example.supportpreparation.TaskTableManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StackManagerFragment extends Fragment {

    private final int MAX_ALARM_CANCEL_NUM = 256; //アラームキャンセル最大数

    private MainActivity                mParentActivity;        //親アクティビティ
    private Fragment                    mFragment;              //本フラグメント
    private Context                     mContext;               //コンテキスト（親アクティビティ）
    private View                        mRootLayout;            //本フラグメントに設定しているレイアウト
    private AppDatabase                 mDB;                    //DB
    private LinearLayout                mll_stackArea;          //「やること」積み上げ領域
    private List<TaskTable>             mStackTask;             //積み上げ「やること」
    private List<TaskTable>             mTaskList;              //「やること」
    private StackTaskRecyclerAdapter    mStackAreaAdapter;      //積み上げ「やること」アダプタ
    //private FloatingActionButton mFab;                        //フローティングボタン
    private TextView                    mtv_limitDate;          //リミット日のビュー
    private TextView                    mtv_limitTime;          //リミット時間のビュー
    private Intent                      mAlarmReceiverIntent;   //アラーム受信クラスのIntent


    //-- 変更有無の確認用
    //private List<TaskTable>         mInit_StackTask;            //開始時点-積み上げやること
    //private String                  mInit_LimitDate;            //開始時点-リミット日
    //private String                  mInit_LimitTime;            //開始時点-リミット時間

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //自身のフラグメントを保持
        mFragment = getParentFragmentManager().getFragments().get(0);
        //設定レイアウト
        mRootLayout = inflater.inflate(R.layout.fragment_stack_manager, container, false);
        //親アクティビティのコンテキスト
        mContext = mRootLayout.getContext();
        //DB操作インスタンスを取得
        mDB = AppDatabaseSingleton.getInstance(mRootLayout.getContext());
        //親アクティビティ
        mParentActivity = (MainActivity) getActivity();

        //やることリストを取得
        mTaskList = mParentActivity.getTaskData();

        //ビューを保持
        mtv_limitTime = (TextView) mRootLayout.findViewById(R.id.tv_limitTime);
        mtv_limitDate = (TextView) mRootLayout.findViewById(R.id.tv_limitDate);

        //アラーム受信クラスのIntent
        mAlarmReceiverIntent = new Intent(mParentActivity.getApplicationContext(), AlarmBroadcastReceiver.class);

        //「リミット日時」の設定
        setDisplayLimitDate();
        //「やること」積み上げエリアの設定
        setStackTaskArea();
        //選択エリアの設定
        setTaskSelectionArea();
        setGroupSelectionArea();

        //選択エリアの切り替え
        TextView tv_selecySwitchLabel = (TextView)mRootLayout.findViewById(R.id.tv_selecySwitchLabel);
        tv_selecySwitchLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //リサイクラービュー取得
                RecyclerView rv_task = (RecyclerView) mRootLayout.findViewById(R.id.rv_taskList);
                RecyclerView rv_group = (RecyclerView) mRootLayout.findViewById(R.id.rv_groupList);

                int taskVisibility = rv_task.getVisibility();
                if( taskVisibility == View.VISIBLE ){
                    //表示切り替え：やること → グループ
                    rv_task.setVisibility( View.GONE );
                    rv_group.setVisibility( View.VISIBLE );

                    ((TextView)view).setText( R.string.select_switch_label_group);

                } else {
                    //表示切り替え：グループ → やること
                    rv_task.setVisibility( View.VISIBLE );
                    rv_group.setVisibility( View.GONE );

                    ((TextView)view).setText( R.string.select_switch_label_task);
                }
            }
        });

        // アラーム開始ボタンの設定
        FloatingActionButton fab = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_startSupport);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast toast = new Toast(mContext);

                //時間未入力チェック
                String noInputStr = getString(R.string.limittime_no_input);
                if (mtv_limitTime.getText().toString().equals(noInputStr)) {
                    //メッセージを表示
                    toast.setText("時間を設定してください");
                    toast.show();
                    return;
                }

                //アラーム時刻の取得
                List<Calendar> alarmList = mStackAreaAdapter.getAlarmList();

                //「やること」未選択の場合
                if (mStackTask.size() == 0 || alarmList == null) {
                    //メッセージを表示
                    toast.setText("やることを選択してください");
                    toast.show();
                    return;
                }

                //AlarmManagerの取得
                AlarmManager am = (AlarmManager) mContext.getSystemService(ALARM_SERVICE);
                if (am == null) {
                    //メッセージを表示
                    toast.setText("エラーが発生しました。再度、ボタンを押してください");
                    toast.show();
                    return;
                }

                //設定中アラームの削除
                cancelAllAlarm();
                //リクエストコード
                int requestCode = 0;

                //初めに設定するアラームindexを取得
                int idx = getStartAlarmIdx(alarmList);
                if (idx == -1) {
                    //エラーの場合は、現在時刻よりも後が設定されている
                    //※過去のアラームに対して、再度アラームを設定しようとした場合のガード処理
                    toast.setText("現在時刻よりも後に設定してください");
                    toast.show();
                    return;
                }

                //各「やること」のアラームを設定
                int size = alarmList.size();
                for (; idx < size; idx++) {
                    //アラームの設定
                    PendingIntent pending = PendingIntent.getBroadcast(mParentActivity.getApplicationContext(), requestCode, mAlarmReceiverIntent, 0);
                    am.setExact(AlarmManager.RTC_WAKEUP, alarmList.get(idx).getTimeInMillis(), pending);

                    //リクエストコードを更新
                    requestCode++;
                }

                //「積み上げやること」をDBに保存
                mParentActivity.setStackTaskData(mStackTask);

                //メッセージを表示
                toast.setText("アラームを設定しました");
                toast.show();

                //-- サポート画面へ移る
                /*
                // フラグメントマネージャーの取得
                FragmentManager manager = getParentFragmentManager();
                // フラグメントトランザクションの開始
                FragmentTransaction transaction = manager.beginTransaction();
                // レイアウトをfragmentに置き換え
                transaction.replace(R.id.nav_host_fragment_activity_main, new TimeFragment());
                // 置き換えのトランザクションをバックスタックに保存する
                transaction.addToBackStack(null);
                // フラグメントトランザクションをコミット
                transaction.commit();
                 */

                //積み上げられた「やること」を保持
            }
        });

        return mRootLayout;
    }

    /*
     * onDestroyView()
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /*
     * 設定中アラームの全キャンセル
     */
    private void cancelAllAlarm() {

        //AlarmManagerの取得
        AlarmManager am = (AlarmManager) mContext.getSystemService(ALARM_SERVICE);

        for (int i = 0; i < MAX_ALARM_CANCEL_NUM; i++) {
            //PendingIntentを取得
            //※「FLAG_NO_CREATE」を指定することで、新規のPendingIntent（アラーム未生成）の場合は、nullを取得する
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mParentActivity.getApplicationContext(), i, mAlarmReceiverIntent, PendingIntent.FLAG_NO_CREATE);
            if (pendingIntent == null) {
                //未生成ならキャンセル処理終了
                break;
            }

            //アラームキャンセル
            pendingIntent.cancel();
            am.cancel(pendingIntent);
        }
    }

    /*
     * アラームリストに関して、初めに設定するアラームのIndexを取得する
     */
    private int getStartAlarmIdx(List<Calendar> alarmList) {
        //-- 現在時刻
        Date nowTime = new Date();

        /*dbg
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPANESE);
        String nowStr = sdf.format(nowTime);
        Log.i("test", "nowStr=" + nowStr);
        Log.i("test", "now=" + nowTime.getTime());
        */

        int i = 0;
        for (Calendar calendar : alarmList) {

            //アラーム時刻を取得
            Date alarmDate = calendar.getTime();

            /*dbg
            sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPANESE);
            String Str = sdf.format(alarmDate);
            Log.i("test", "alarmStr=" + Str);
            Log.i("test", "alarm getTime=" + alarmDate.getTime());
            Log.i("test", "alarm getTimeInMillis=" + calendar.getTimeInMillis());
            */

            //アラーム時刻は現在時刻よりも後か
            if (alarmDate.after(nowTime)) {
                return i;
            }

            //インデックスを加算
            i++;
        }

        //見つからない（すべて現在時刻より前の時間）場合
        return -1;
    }

    /*
     * 時間入力ダイアログの生成
     */
    private void createTimeDialog() {

        Calendar calendar = Calendar.getInstance();

        //タイムピッカーダイアログの表示
        TimePickerDialog dialog = new TimePickerDialog(
                mContext,
                R.style.TimePickerTheme,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                        //現在時刻よりも後かどうか
                        if (isAfterSetTime(hourOfDay, minute)) {
                            //入力時刻を設定
                            String limit = String.format("%02d:%02d", hourOfDay, minute);
                            mtv_limitTime.setText(limit);

                            //共通データとして保持
                            mParentActivity.setLimitTime(limit);

                            //やること開始時間を変更させる
                            //mStackAreaAdapter.;
                            mStackAreaAdapter.clearAlarmList();
                            mStackAreaAdapter.notifyDataSetChanged();

                        } else {
                            //メッセージを表示
                            Toast toast = new Toast(mContext);
                            toast.setText("現在時刻以降を設定してください");
                            toast.show();
                        }
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);
        dialog.show();
    }

    /*
     * カレンダーダイアログの生成
     */
    private void createCalendarDialog() {

        //Calendarインスタンスを取得
        Calendar calendar = Calendar.getInstance();

        //DatePickerDialogインスタンスを取得
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                mContext,
                R.style.TimePickerTheme,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

                        //現在日よりも後かどうか
                        if (isAfterSetDate(year, month, dayOfMonth)) {
                            //日付を取得して表示
                            String date = String.format(Locale.JAPANESE, "%04d/%02d/%02d", year, month + 1, dayOfMonth);
                            mtv_limitDate.setText(date);

                            //共通データとして保持
                            mParentActivity.setLimitDate(date);

                        } else {
                            //メッセージを表示
                            Toast toast = new Toast(mContext);
                            toast.setText("本日以降を設定してください");
                            toast.show();
                        }
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DATE)
        );

        //dialogを表示
        datePickerDialog.show();
    }

    /*
     * 指定年月日が現在日よりも後か
     */
    private boolean isAfterSetDate(int year, int month, int dayOfMonth) {

        //現在日時
        Date nowDate = new Date();

        //設定された日時（同じ日が入力されたとき、有効としたいため、時刻はその日の最終時刻）
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth, 23, 59, 59);
        Date setDate = calendar.getTime();

        //Log.i("test", "nowDate=" + nowDate);
        //Log.i("test", "setDate=" + setDate);
        //Log.i("test", "after=" + setDate.after(setDate));

        //設定日の方が、現在日よりも後か
        return setDate.after(nowDate);
    }

    /*
     * 指定時分が現在時分よりも後か
     */
    private boolean isAfterSetTime(int hourOfDay, int minute) {

        //現在時分
        Date nowDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String nowDateStr = sdf.format(nowDate);

        //設定中の日付
        String setLimitDate = mParentActivity.getLimitDate();

        //Log.i("test", "nowDateStr=" + nowDateStr);
        //Log.i("test", "setLimitDate=" + setLimitDate);

        //本日の時刻でなければ、後であること確定
        if (setLimitDate.compareTo(nowDateStr) != 0) {
            return true;
        }

        //現在時分
        sdf = new SimpleDateFormat("HH:mm");
        String nowStr = sdf.format(nowDate);

        //設定時分
        String setStr = String.format("%02d:%02d", hourOfDay, minute);

        //Log.i("test", "nowStr=" + nowStr);
        //Log.i("test", "setStr=" + setStr);
        //Log.i("test", "compareTo=" + nowStr.compareTo(setStr));

        if (setStr.compareTo(nowStr) >= 0) {
            //設定時分が、現在時分よりも後であれば
            return true;
        } else {
            //設定時分が、現在時分よりも前か同じであれば
            return false;
        }
    }


    /*
     * 「やること」積み上げエリアの設定
     */
    private void setStackTaskArea() {

        //積み上げられた「やること」を取得
        mStackTask = mParentActivity.getStackTaskData();

        //ドロップリスナーの設定(ドロップ先のビューにセット)
        DragListener listener = new DragListener();
        mll_stackArea = mRootLayout.findViewById(R.id.ll_stackArea);
        mll_stackArea.setOnDragListener(listener);

        //レイアウトからリストビューを取得
        RecyclerView rv_stackArea = (RecyclerView) mRootLayout.findViewById(R.id.rv_stackArea);

        //レイアウトマネージャの生成・設定（横スクロール）
        LinearLayoutManager ll_manager = new LinearLayoutManager(mContext);
        rv_stackArea.setLayoutManager(ll_manager);

        //アダプタの生成・設定
        mStackAreaAdapter = new StackTaskRecyclerAdapter(mContext, mStackTask, mtv_limitDate, mtv_limitTime);
        rv_stackArea.setAdapter(mStackAreaAdapter);

        //ドラッグアンドドロップ、スワイプの設定(リサイクラービュー)
        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                        ItemTouchHelper.LEFT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {

                        //！getAdapterPosition()←非推奨
                        final int fromPos = viewHolder.getAdapterPosition();
                        final int toPos = target.getAdapterPosition();
                        //アイテム移動を通知
                        mStackAreaAdapter.notifyItemMoved(fromPos, toPos);
                        Log.i("test", "fromPos=" + fromPos + " toPos=" + toPos);
                        //各開始時間も変更になるため、通知
                        mStackAreaAdapter.clearAlarmList();
                        mStackAreaAdapter.notifyDataSetChanged();

                        return true;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                        //スワイプされたデータ
                        final int       adapterPosition = viewHolder.getAdapterPosition();
                        final TaskTable deletedTask     = mStackTask.get(adapterPosition);

                        //下部ナビゲーションを取得
                        BottomNavigationView bnv = mParentActivity.findViewById(R.id.bnv_nav);

                        //UNDOメッセージの表示
                        Snackbar snackbar = Snackbar
                                .make(rv_stackArea, R.string.snackbar_delete, Snackbar.LENGTH_LONG)
                                //アクションボタン押下時の動作
                                .setAction(R.string.snackbar_undo, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        //UNDOが選択された場合、削除されたアイテムを元の位置に戻す
                                        mStackTask.add(adapterPosition, deletedTask);
                                        mStackAreaAdapter.notifyItemInserted(adapterPosition );
                                        rv_stackArea.scrollToPosition(adapterPosition );

                                        //各開始時間を変更させるため、アダプタへ変更を通知
                                        mStackAreaAdapter.clearAlarmList();
                                        mStackAreaAdapter.notifyDataSetChanged();
                                    }
                                })
                                //スナックバークローズ時の動作
                                .addCallback(new Snackbar.Callback() {
                                    @Override
                                    public void onDismissed(Snackbar snackbar, int event) {
                                        super.onDismissed(snackbar, event);

                                        //アクションバー押下以外で閉じられた場合
                                        if (event != DISMISS_EVENT_ACTION) {
                                            //各開始時間を変更させるため、アダプタへ変更を通知
                                            mStackAreaAdapter.clearAlarmList();
                                            mStackAreaAdapter.notifyDataSetChanged();
                                        }
                                    }
                                })
                                //下部ナビゲーションの上に表示させるための設定
                                .setAnchorView(bnv)
                                .setBackgroundTint(getResources().getColor(R.color.basic))
                                .setTextColor(getResources().getColor(R.color.white))
                                .setActionTextColor(getResources().getColor(R.color.white));

                        //表示
                        snackbar.show();

                        //リストから削除し、アダプターへ通知
                        mStackTask.remove(adapterPosition);
                        mStackAreaAdapter.notifyItemRemoved(adapterPosition);

                        //各開始時間を変更させるため、アダプタへ変更を通知
                        mStackAreaAdapter.clearAlarmList();
                        mStackAreaAdapter.notifyDataSetChanged();
                    }
                }
        );

        //リサイクラービューをアタッチ
        helper.attachToRecyclerView(rv_stackArea);
    }

    /*
     * 「リミット日時」を設定
     */
    private void setDisplayLimitDate() {

        //-- リミット時間の設定

        //親アクティビティで文字しているデータを取得・設定
        String limitTime = mParentActivity.getLimitTime();
        mtv_limitTime.setText(limitTime);

        //クリックリスナーの設定
        mtv_limitTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //-- 時刻設定ダイアログの生成
                createTimeDialog();
            }
        });

        //-- リミット日の設定
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");

        //本日の日付を取得
        Date nowDate = new Date();
        String today = sdf.format(nowDate);

        //日付を設定
        mtv_limitDate.setText(today);

        //共通データとして保持
        String now = sdf.format(nowDate);
        mParentActivity.setLimitDate(now);

        //リスナーを設定
        mtv_limitDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //-- カレンダーダイアログの生成
                createCalendarDialog();
            }
        });
    }

    /*
     * 「やること」選択エリアの設定
     */
    private void setTaskSelectionArea() {

        //登録がなければ終了
        if (mTaskList == null || mTaskList.size() == 0) {
            return;
        }

        //レイアウトからリストビューを取得
        RecyclerView rv_task = (RecyclerView) mRootLayout.findViewById(R.id.rv_taskList);

        //レイアウトマネージャの生成・設定（横スクロール）
        LinearLayoutManager ll_manager = new LinearLayoutManager(mContext);
        ll_manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        rv_task.setLayoutManager(ll_manager);

        //-- アダプタの設定は、サイズが確定してから行う
        // ビューツリー描画時に呼ばれるリスナーの設定
        rv_task.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {

                //RecyclerViewの横幅 / 2 を子アイテムの横幅とする
                int width = rv_task.getWidth() / 2;

                //RecyclerViewがGONE中は、アダプタ設定をしない
                if( width == 0 ){
                    return true;
                }

                //アダプタの生成・設定
                TaskRecyclerAdapter adapter = new TaskRecyclerAdapter(mContext, mTaskList, TaskRecyclerAdapter.SETTING.SELECT, width, 0);

                //ドラッグリスナーの設定
                adapter.setOnItemLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        //ClipData data = ClipData.newPlainText("text", "text");
                        view.startDrag(null, new View.DragShadowBuilder(view), (Object) view, 0);

                        return true;
                    }
                });

                //RecyclerViewにアダプタを設定
                rv_task.setAdapter(adapter);

                //本リスナーを削除（何度も処理する必要はないため）
                rv_task.getViewTreeObserver().removeOnPreDrawListener(this);

                //描画を中断するため、false
                return false;
            }
        });


    }

    /*
     * 「グループ」選択エリアの設定
     */
    private void setGroupSelectionArea(){

        //やることリストを取得
        List<GroupTable> groupList = mParentActivity.getGroupData();

        //登録がなければ終了
        if (groupList == null || groupList.size() == 0) {
            return;
        }

        //レイアウトからリストビューを取得
        RecyclerView rv_group = (RecyclerView) mRootLayout.findViewById(R.id.rv_groupList);

        //レイアウトマネージャの生成・設定（横スクロール）
        LinearLayoutManager ll_manager = new LinearLayoutManager(mContext);
        ll_manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        rv_group.setLayoutManager(ll_manager);

        //-- アダプタの設定は、サイズが確定してから行う
        // ビューツリー描画時に呼ばれるリスナーの設定
        rv_group.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {

                //RecyclerViewの横幅 / 2 を子アイテムの横幅とする
                int width = rv_group.getWidth() / 2;

                //RecyclerViewがGONE中は、アダプタ設定をしない
                if( width == 0 ){
                    return true;
                }

                //アダプタの生成・設定
                GroupSelectRecyclerAdapter adapter = new GroupSelectRecyclerAdapter(mContext, groupList, width, 0);

                //ドラッグリスナーの設定
                adapter.setOnItemLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        view.startDrag(null, new View.DragShadowBuilder(view), (Object) view, 0);
                        return true;
                    }
                });

                //RecyclerViewにアダプタを設定
                rv_group.setAdapter(adapter);

                //本リスナーを削除（何度も処理する必要はないため）
                rv_group.getViewTreeObserver().removeOnPreDrawListener(this);

                //描画を中断するため、false
                return false;
            }
        });
    }

    /*
     * 「やること」リストから、指定されたPidの「やること」を取得
     */
    private TaskTable getTaskByPid( int pid ){

        //保持している「やること」リスト内を検索
        for( TaskTable task: mTaskList ){
            if( task.getId() == pid ){
                return task;
            }
        }

        return null;
    }

    /*
     * ドラッグリスナー
     *　　「やること」を積み上げエリアにドラッグアンドドロップするときに使用
     */
    private class DragListener implements View.OnDragListener {
        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            switch (dragEvent.getAction()) {
                //ドラッグ開始時
                case DragEvent.ACTION_DRAG_STARTED: {
                    //色を選択中に変更
                    //View dragView = (View) dragEvent.getLocalState();
                    //dragView.setBackgroundColor(Color.LTGRAY);
                    //((TextView)dragView).setTextColor(Color.WHITE);
                    return true;
                }
                //ドロップ時(ドロップしてドラッグが終了したとき)
                case DragEvent.ACTION_DROP: {

                    //ドラッグしたビューからデータを取得
                    View dragView = (View)dragEvent.getLocalState();

                    //ドロップされたのが「やること」か「グループ」か
                    TextView tv_taskInGroup = dragView.findViewById(R.id.tv_taskInGroup);
                    if( tv_taskInGroup == null ){
                        //--「やること」がドロップ

                        TextView tv_pid      = dragView.findViewById(R.id.tv_pid);
                        TextView tv_taskName = dragView.findViewById(R.id.tv_taskName);
                        TextView tv_taskTime = dragView.findViewById(R.id.tv_taskTime);

                        int pid      = Integer.parseInt( tv_pid.getText().toString() );
                        int taskTime = Integer.parseInt( tv_taskTime.getText().toString() );

                        mStackTask.add( 0, new TaskTable( pid, tv_taskName.getText().toString(), taskTime ) );

                    } else {
                        //--「グループ」がドロップ

                        TextView tv_groupPid  = dragView.findViewById(R.id.tv_groupPid);
                        TextView tv_groupName = dragView.findViewById(R.id.tv_groupName);

                        String taskPidsStr = tv_taskInGroup.getText().toString();
                        Log.i("test", "drop taskPidsStr=" + taskPidsStr);
                        List<Integer> pids = TaskTableManager.getPidsIntArray( taskPidsStr );
                        if( pids == null ){
                            //何もないなら、何もせず終了
                            break;
                        }

                        //グループ内の「やること」を積み上げ先のリストに追加する
                        int i = pids.size() - 1;
                        for( ; i >= 0; i-- ) {
                            Integer pid = pids.get(i);
                            TaskTable task = getTaskByPid(pid);
                            if( task != null ){
                                mStackTask.add( 0, task );
                            }
                        }
                    }

                    //アダプタへ通知
                    mStackAreaAdapter.clearAlarmList();
                    mStackAreaAdapter.notifyDataSetChanged();

                    break;
                }
                //ドラッグ終了時
                case DragEvent.ACTION_DRAG_ENDED: {
                    // ドラッグ終了時
                    //Log.i(getClass().getSimpleName(), "ACTION_DRAG_ENDED");
                    //Log.i("test", "drap end=" + ((TextView)view).getText());

                    //View dragView = (View) dragEvent.getLocalState();
                    //dragView.setBackgroundColor(Color.TRANSPARENT);

                    return true;
                }
            }
            return true;
        }
    }







    /*
     * test
     */
    private static class MyDragShadowBuilder extends View.DragShadowBuilder {

        // The drag shadow image, defined as a drawable thing
        private static Drawable shadow;

        // Defines the constructor for myDragShadowBuilder
        public MyDragShadowBuilder(View v) {

            // Stores the View parameter passed to myDragShadowBuilder.
            super(v);

            // Creates a draggable image that will fill the Canvas provided by the system.
            shadow = new ColorDrawable(Color.LTGRAY);
        }

        // Defines a callback that sends the drag shadow dimensions and touch point back to the
        // system.
        @Override
        public void onProvideShadowMetrics (Point size, Point touch) {
            // Defines local variables
            int width, height;

            // Sets the width of the shadow to half the width of the original View
            width = getView().getWidth() / 2;

            // Sets the height of the shadow to half the height of the original View
            height = getView().getHeight() / 2;

            // The drag shadow is a ColorDrawable. This sets its dimensions to be the same as the
            // Canvas that the system will provide. As a result, the drag shadow will fill the
            // Canvas.
            shadow.setBounds(0, 0, width, height);

            // Sets the size parameter's width and height values. These get back to the system
            // through the size parameter.
            size.set(width, height);

            // Sets the touch point's position to be in the middle of the drag shadow
            touch.set(width / 2, height / 2);
        }

        // Defines a callback that draws the drag shadow in a Canvas that the system constructs
        // from the dimensions passed in onProvideShadowMetrics().
        @Override
        public void onDrawShadow(Canvas canvas) {

            // Draws the ColorDrawable in the Canvas passed in from the system.
            shadow.draw(canvas);
        }
    }

    /*
    @Override
    public boolean onLongClick(View v) {

        v.startDrag(null, new View.DragShadowBuilder(v), v, 0);
        return true;
    }

     */


}