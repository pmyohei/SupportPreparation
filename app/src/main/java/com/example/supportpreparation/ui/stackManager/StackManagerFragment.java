package com.example.supportpreparation.ui.stackManager;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.supportpreparation.AlarmBroadcastReceiver;
import com.example.supportpreparation.AppDatabase;
import com.example.supportpreparation.AppDatabaseSingleton;
import com.example.supportpreparation.CreateSetAlarmDialog;
import com.example.supportpreparation.GroupArrayList;
import com.example.supportpreparation.GroupSelectRecyclerAdapter;
import com.example.supportpreparation.GroupTable;
import com.example.supportpreparation.MainActivity;
import com.example.supportpreparation.R;
import com.example.supportpreparation.SelectAreaScrollListener;
import com.example.supportpreparation.StackTaskRecyclerAdapter;
import com.example.supportpreparation.StackTaskTable;
import com.example.supportpreparation.TaskArrayList;
import com.example.supportpreparation.TaskRecyclerAdapter;
import com.example.supportpreparation.TaskTable;
import com.example.supportpreparation.TaskTableManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kotlinx.coroutines.scheduling.Task;

public class StackManagerFragment extends Fragment {

    public final static float STACK_BLOCK_RATIO = 0.4f;         //スタックやることの横サイズ割合
    public final static int SELECT_TASK_AREA_DIV = 4;           //やること選択エリア-横幅分割数
    public final static int SELECT_GROUP_AREA_DIV = 4;          //やること選択エリア-横幅分割数

    public static final String NOTIFY_SEND_KEY = "notifykey";               //アラームキャンセル最大数

    public enum ALARM_RESULT {
        NEW_ALARM,                                              //アラーム新規設定
        UPDATE_ALARM,                                           //アラーム更新
    }

    private MainActivity mParentActivity;        //親アクティビティ
    private Fragment mFragment;              //本フラグメント
    private Context mContext;               //コンテキスト（親アクティビティ）
    private View mRootLayout;            //本フラグメントに設定しているレイアウト
    private AppDatabase mDB;                    //DB
    private LinearLayout mll_stackArea;          //「やること」積み上げ領域
    private StackTaskTable mStackTable;                            //スタックテーブル
    private StackTaskTable mAlarmStack;                            //アラーム設定されたスタック
    private TaskArrayList<TaskTable> mStackTaskList;             //積み上げ「やること」
    private TaskArrayList<TaskTable> mTaskList;              //「やること」
    private StackTaskRecyclerAdapter mStackAreaAdapter;      //積み上げ「やること」アダプタ
    private FloatingActionButton mfab_setAlarm;                   //フローティングボタン
    private TextView mtv_limitDate;          //リミット日のビュー
    private TextView mtv_limitTime;          //リミット時間のビュー
    private Intent mAlarmReceiverIntent;   //アラーム受信クラスのIntent
    private boolean mIsSelectTask;                          //フラグ-「やること」選択エリア表示中
    private boolean mIsLimit;                               //フラグ-リミット選択中
    private boolean mIsStackChg;                               //スタックタスク変更有無

    //BottomSheetBehavior と連動するpadding
    private int mBasicPadding;
    private int mExpandedBSHeight;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Log.i("test", "StackManagerFragment onCreateView");

        //自身のフラグメント
        mFragment = getParentFragmentManager().getFragments().get(0);
        //設定レイアウト
        mRootLayout = inflater.inflate(R.layout.fragment_stack_manager, container, false);
        //親アクティビティのコンテキスト
        mContext = mRootLayout.getContext();
        //DB操作インスタンス
        mDB = AppDatabaseSingleton.getInstance(mRootLayout.getContext());
        //親アクティビティ
        mParentActivity = (MainActivity) getActivity();
        //スタック情報
        mStackTable = mParentActivity.getStackTable();
        mAlarmStack = mParentActivity.getAlarmStack();
        //やることリスト
        mTaskList = mParentActivity.getTaskData();
        //フラグ
        mIsSelectTask = mParentActivity.isSelectTask();
        mIsLimit = mStackTable.isLimit();
        //積み上げられた「やること」
        mStackTaskList = mStackTable.getStackTaskList();


        //ビューを保持
        mtv_limitTime = (TextView) mRootLayout.findViewById(R.id.tv_limitTime);
        mtv_limitDate = (TextView) mRootLayout.findViewById(R.id.tv_limitDate);
        mfab_setAlarm = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_setAlarm);

        //スタックタスク未変更
        mIsStackChg = false;

        //スタックやること情報の同期
        syncStackTaskData();

        //「リミット日時」の設定
        setupBaseTimeDate();

        //「やること」積み上げエリアの設定
        setupStackTaskArea();

        //選択エリアの設定
        setupTaskSelectionArea();
        setupGroupSelectionArea();

        //選択エリア切り替えアイコンの設定
        setupIvSwitchSelectArea();

        //FABの設定
        setupFabParent();
        setupFabSwitchDirection();
        setupFabSetAlarm();
        setupFabStackTaskClear();

        //BottomSheetの設定
        setupBottomSheet();

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
     * 時間入力ダイアログの生成
     */
    private void createTimeDialog(TextView touchView) {

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
                            touchView.setText(limit);

                            if (!mIsLimit) {
                                //リミット指定でないなら、リミット側にも設定(スタックアダプタ参照用)
                                mtv_limitTime.setText(limit);
                            }

                            //共通データとして保持
                            mStackTable.setTime(limit);
                            //mParentActivity.setStackTable(mStackTable);
                            mIsStackChg = true;

                            //やること開始時間を変更
                            mStackAreaAdapter.notifyDataSetChanged();

                        } else {
                            //メッセージを表示
                            Toast toast = new Toast(mContext);
                            toast.setText(R.string.toast_input_time_previous);
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
    private void createCalendarDialog(TextView touchView) {

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
                            touchView.setText(date);

                            if (!mIsLimit) {
                                //リミット指定でないなら、リミット側にも設定(スタックアダプタ参照用)
                                mtv_limitDate.setText(date);
                            }

                            //共通データとして保持
                            mStackTable.setDate(date);
                            //mParentActivity.setStackTable(mStackTable);
                            mIsStackChg = true;

                            //やること開始時間を変更
                            mStackAreaAdapter.notifyDataSetChanged();

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
        String setDate = mStackTable.getDate();

        //Log.i("test", "nowDateStr=" + nowDateStr);
        //Log.i("test", "setDate=" + setDate);

        //本日の時刻でなければ、後であること確定
        if (setDate.compareTo(nowDateStr) != 0) {
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
    private void setupStackTaskArea() {

        //ドロップリスナーの設定(ドロップ先のビューにセット)
        //DragListener listener = new DragListener();
        //RecyclerView mll_stackArea = mRootLayout.findViewById(R.id.rv_stackArea);
        //mll_stackArea.setOnDragListener(listener);

        //レイアウトからリストビューを取得
        RecyclerView rv_stackArea = (RecyclerView) mRootLayout.findViewById(R.id.rv_stackArea);

        //レイアウトマネージャの生成・設定（横スクロール）、下寄り表示
        LinearLayoutManager ll_manager = new LinearLayoutManager(mContext);
        ll_manager.setStackFromEnd(mIsLimit);    //表示方向はリミットかスタートかで切り分け
        rv_stackArea.setLayoutManager(ll_manager);

        //-- アダプタの設定は、サイズが確定してから行う
        // ビューツリー描画時に呼ばれるリスナーの設定
        rv_stackArea.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {

                //RecyclerViewのブロックサイズ
                int width = (int) (rv_stackArea.getWidth() * STACK_BLOCK_RATIO);

                //アダプタの生成・設定
                //※高さはビューに依存「wrap_contents」
                mStackAreaAdapter = new StackTaskRecyclerAdapter(mContext, mStackTable, width);

                //RecyclerViewにアダプタを設定
                rv_stackArea.setAdapter(mStackAreaAdapter);

                //本リスナーを削除（何度も処理する必要はないため）
                rv_stackArea.getViewTreeObserver().removeOnPreDrawListener(this);

                //描画を中断するため、false
                return false;
            }
        });

        //スタート指定の場合
        if (!mIsLimit) {
            //アニメーションを変更
            rv_stackArea.setLayoutAnimation(
                    AnimationUtils.loadLayoutAnimation(mContext, R.anim.layout_anim_que_task)
            );
        }

        //((SimpleItemAnimator) rv_stackArea.getItemAnimator()).setSupportsChangeAnimations(false);

        //ドラッグアンドドロップ、スワイプの設定(リサイクラービュー)
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT) {

            //入れ替えフラグ
            private boolean isMove = false;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {

                //★備考★getAdapterPosition()←非推奨
                final int fromPos = viewHolder.getAdapterPosition();
                final int toPos = target.getAdapterPosition();

                //リスト入れ替え
                mStackTable.swapTask(fromPos, toPos);

                //アイテム移動を通知
                mStackAreaAdapter.notifyItemMoved(fromPos, toPos);
                Log.i("test", "fromPos=" + fromPos + " toPos=" + toPos);

                //フラグON
                isMove = true;

                return true;
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                //スワイプされたデータ
                final int adapterPos = viewHolder.getAdapterPosition();
                final TaskTable deletedTask = mStackTaskList.get(adapterPos);

                //下部ナビゲーションを取得
                BottomNavigationView bnv = mParentActivity.findViewById(R.id.bnv_nav);

                //スナックバーを保持する親ビュー
                ConstraintLayout cl_mainContainer = mParentActivity.findViewById(R.id.cl_mainContainer);

                //UNDOメッセージの表示
                Snackbar snackbar = Snackbar
                        .make(cl_mainContainer, R.string.snackbar_delete, Snackbar.LENGTH_LONG)
                        //アクションボタン押下時の動作
                        .setAction(R.string.snackbar_undo, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //UNDOが選択された場合、削除されたアイテムを元の位置に戻す
                                mStackTable.insertTask(adapterPos, deletedTask);
                                mStackAreaAdapter.notifyItemInserted(adapterPos);

                                //表示位置を、元に戻したアイテム位置へ移動
                                rv_stackArea.scrollToPosition(adapterPos);

                                //各時間を変更させるため、アダプタへ変更を通知
                                mStackAreaAdapter.notifyDataSetChanged();
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
                mStackTable.removeTask(adapterPos);

                //各開始時間を変更させるため、アダプタへ変更を通知
                mStackAreaAdapter.notifyDataSetChanged();

                //フラグOFF
                isMove = false;
            }

            /*
             * 最終的な処理終了時にコールされる
             */
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {

                //※superをしないと、onSwipで削除したとき、同じデータが非表示になる
                super.clearView(recyclerView, viewHolder);

                //スタックエリア変更ON
                //※変更した後、元に戻して結果的に変化なしの可能性もあるが、このメソッドがコールされた時点で変更ありとみなす
                mIsStackChg = false;

                if (!isMove) {
                    //onMove終了後でないなら、なにもしない
                    return;
                }

                //各開始時間も変更になるため、通知
                mStackAreaAdapter.notifyDataSetChanged();

                isMove = false;
            }
        });

        //リサイクラービューをアタッチ
        helper.attachToRecyclerView(rv_stackArea);
    }

    /*
     * スタック中のやることを、登録されているやること情報と同期させる
     */
    private void syncStackTaskData() {

        //削除キュー
        List<Integer> delList = new ArrayList<>();

        int i = 0;
        for( TaskTable task: mStackTaskList ){

            int pid = task.getId();

            TaskTable orgTask = mTaskList.getTaskByPid(pid);
            if( orgTask == null ){
                //削除済みなら、リストに追加
                delList.add(i);

            } else {
                //データ同期（他のフィールドは本フラグメント以外で変更になることはないため、対象外）
                task.setTaskName( orgTask.getTaskName() );
                task.setTaskTime( orgTask.getTaskTime() );
            }

            i++;
        }

        //削除対象があれば、削除
        for( Integer idx: delList ){
            mStackTaskList.remove(idx.intValue() );
        }
    }


    /*
     * 「リミット日時」を設定
     */
    private void setupBaseTimeDate() {

        //リミット・スタートのビュー
        LinearLayout ll_startGroup = (LinearLayout) mRootLayout.findViewById(R.id.ll_startGroup);
        LinearLayout ll_limitGroup = (LinearLayout) mRootLayout.findViewById(R.id.ll_limitGroup);

        //選択中の方向に応じた表示
        if (mIsLimit) {
            //リミットを表示
            ll_startGroup.setVisibility(View.GONE);
            ll_limitGroup.setVisibility(View.VISIBLE);
        } else {
            //スタートを表示
            ll_startGroup.setVisibility(View.VISIBLE);
            ll_limitGroup.setVisibility(View.GONE);
        }

        //親アクティビティで保持しているリミット時間を取得
        String limitTime = mStackTable.getTime();

        //時間設定-リミット
        mtv_limitTime.setText(limitTime);
        mtv_limitTime.setOnClickListener(new BaseTimeListener());

        //時間設定-スタート
        TextView tv_startTime = (TextView) mRootLayout.findViewById(R.id.tv_alarmTime);
        tv_startTime.setText(limitTime);
        tv_startTime.setOnClickListener(new BaseTimeListener());

        //親アクティビティで保持しているリミット日を取得
        String today = mStackTable.getDate();

        //日付設定-リミット
        mtv_limitDate.setText(today);
        mtv_limitDate.setOnClickListener(new BaseDateListener());

        //日付設定-スタート
        TextView tv_startDate = (TextView) mRootLayout.findViewById(R.id.tv_startDate);
        tv_startDate.setText(today);
        tv_startDate.setOnClickListener(new BaseDateListener());
    }

    /*
     * 「やること」選択エリアの設定
     */
    private void setupTaskSelectionArea() {

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

                //RecyclerViewのアイテムサイズ
                int width = rv_task.getWidth() / SELECT_TASK_AREA_DIV;
                if (width == 0) {
                    //RecyclerViewがGONE中は、アダプタ設定をしない
                    return true;
                }

                //アダプタの生成・設定
                //※高さはビューに依存「wrap_contents」
                TaskRecyclerAdapter adapter = new TaskRecyclerAdapter(mContext, mTaskList, TaskRecyclerAdapter.SETTING.SELECT, width, 0);

                //クリックリスナー（クリック時、スタックエリアにクリックアイテムを積み上げる）
                adapter.setOnItemClickListener(new SelectItemClickListener());

                //RecyclerViewにアダプタを設定
                rv_task.setAdapter(adapter);

                //--FAB 分と重ならないように、最後のアイテムの右に空白を入れる
                rv_task.addItemDecoration(new SelectAreaItemDecoration());

                //本リスナーを削除（何度も処理する必要はないため）
                rv_task.getViewTreeObserver().removeOnPreDrawListener(this);

                //描画を中断するため、false
                return false;
            }
        });

        //スクロールリスナーの設定（スクロール中はfabを非表示）
        //※対応しない方針とする
        //LinearLayout ll_fabGroup = (LinearLayout) mRootLayout.findViewById(R.id.ll_fabGroup);
        //rv_task.addOnScrollListener(new SelectAreaScrollListener( (ViewGroup)ll_fabGroup));
    }

    /*
     * 「グループ」選択エリアの設定
     */
    private void setupGroupSelectionArea() {

        //やることリストを取得
        GroupArrayList<GroupTable> groupList = mParentActivity.getGroupData();

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

                //子アイテムの横幅
                int width = rv_group.getWidth() / SELECT_GROUP_AREA_DIV;

                //RecyclerViewがGONE中は、アダプタ設定をしない
                if (width == 0) {
                    return true;
                }

                //アダプタの生成・設定
                GroupSelectRecyclerAdapter adapter = new GroupSelectRecyclerAdapter(mContext, groupList, width, 0);

                //クリックリスナー（クリック時、スタックエリアにクリックアイテムを積み上げる）
                adapter.setOnItemClickListener(new SelectItemClickListener());

                //RecyclerViewにアダプタを設定
                rv_group.setAdapter(adapter);

                //FAB 分と重ならないように、最後のアイテムの右に空白を入れる
                rv_group.addItemDecoration(new SelectAreaItemDecoration());

                //本リスナーを削除（何度も処理する必要はないため）
                rv_group.getViewTreeObserver().removeOnPreDrawListener(this);

                //描画を中断するため、false
                return false;
            }
        });

        //スクロールリスナーの設定
        rv_group.addOnScrollListener(new SelectAreaScrollListener(mfab_setAlarm));
    }

    /*
     * 選択エリア切り替えアイコンの設定
     */
    private void setupIvSwitchSelectArea() {
        ImageView iv_selectSwitch = (ImageView) mRootLayout.findViewById(R.id.iv_selectSwitch);

        //リサイクラービュー取得
        RecyclerView rv_task = (RecyclerView) mRootLayout.findViewById(R.id.rv_taskList);
        RecyclerView rv_group = (RecyclerView) mRootLayout.findViewById(R.id.rv_groupList);

        //設定アイコンの取得
        if (mIsSelectTask) {
            //やること表示の場合

            //やること選択エリアを表示
            rv_task.setVisibility(View.VISIBLE);
            rv_group.setVisibility(View.GONE);

        } else {
            //グループ表示の場合

            //グループ選択エリアを表示
            rv_task.setVisibility(View.GONE);
            rv_group.setVisibility(View.VISIBLE);

            //アイコン設定
            iv_selectSwitch.setBackgroundResource(R.drawable.ic_switch_group);
        }

        //クリックリスナーの設定
        iv_selectSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //リサイクラービュー取得
                RecyclerView rv_task = (RecyclerView) mRootLayout.findViewById(R.id.rv_taskList);
                RecyclerView rv_group = (RecyclerView) mRootLayout.findViewById(R.id.rv_groupList);

                //フラグ反転し、親側データと同期
                mIsSelectTask = !mIsSelectTask;
                mParentActivity.setFlgSelectTask(mIsSelectTask);

                if (mIsSelectTask) {
                    //表示切り替え：グループ → やること
                    rv_task.setVisibility(View.VISIBLE);
                    rv_group.setVisibility(View.GONE);

                    //layoutAnimation　を再度適用
                    rv_task.requestLayout();
                    rv_task.scheduleLayoutAnimation();

                    //アイコンのアニメーション
                    view.setBackgroundResource(R.drawable.avd_group_to_task);

                } else {
                    //表示切り替え：やること → グループ
                    rv_task.setVisibility(View.GONE);
                    rv_group.setVisibility(View.VISIBLE);

                    //layoutAnimation　を再度適用
                    rv_group.requestLayout();
                    rv_group.scheduleLayoutAnimation();

                    //アイコンのアニメーション
                    view.setBackgroundResource(R.drawable.avd_task_to_group);
                }

                //アイコンアニメーション開始
                AnimatedVectorDrawable rocketAnimation = (AnimatedVectorDrawable) view.getBackground();
                rocketAnimation.start();
            }
        });
    }

    /*
     * Fab(親)の設定
     */
    private void setupFabParent() {

        FloatingActionButton fab_parent = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_parent);
        fab_parent.setOnClickListener(new ParentFabOnClickListener());
    }

    /*
     * Fab(積み上げ方向変更)の設定
     */
    private void setupFabSwitchDirection() {

        FloatingActionButton fab_switchDirection = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_switchDirection);

        //設定アイコンの取得
        if (!mIsLimit) {
            //スタート指定の場合、初期アイコンを変更
            fab_switchDirection.setImageResource(R.drawable.ic_switch_direction_limit_32);
        }

        fab_switchDirection.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View view) {
                //スタート側のビュー
                LinearLayout ll_startGroup = mRootLayout.findViewById(R.id.ll_startGroup);
                TextView tv_startTime = ll_startGroup.findViewById(R.id.tv_alarmTime);
                TextView tv_startDate = ll_startGroup.findViewById(R.id.tv_startDate);

                //リミット側のビュー
                LinearLayout ll_limitGroup = mRootLayout.findViewById(R.id.ll_limitGroup);
                TextView tv_limitTime = ll_limitGroup.findViewById(R.id.tv_limitTime);
                TextView tv_limitDate = ll_limitGroup.findViewById(R.id.tv_limitDate);

                //適用するアニメーション
                LayoutAnimationController anim;
                int anim_limit;
                int anim_start;
                int anim_fab;

                //フラグ反転
                mIsLimit = !mIsLimit;
                //親側データと同期
                mStackTable.setIsLimit(mIsLimit);
                //mParentActivity.setStackTable(mStackTable);
                mIsStackChg = true;

                int iconResId;

                if (mIsLimit) {
                    //--スタート(false) → リミット(true) へ変更された

                    //表示切り替え
                    ll_startGroup.setVisibility(View.GONE);
                    ll_limitGroup.setVisibility(View.VISIBLE);

                    //設定日時を同期
                    tv_limitTime.setText(tv_startTime.getText());
                    tv_limitDate.setText(tv_startDate.getText());

                    //アニメーション：スタックエリア
                    anim = AnimationUtils.loadLayoutAnimation(mContext, R.anim.layout_anim_stack_task);
                    //アニメーション：ベース時間
                    anim_limit = R.anim.limit_down_appear;
                    anim_start = R.anim.start_down_disappear;
                    //アニメーション：切り替えアイコン
                    anim_fab = R.anim.rotation_from_180_to_360;

                    iconResId = R.drawable.ic_switch_direction_start_32;

                } else {
                    //--リミット(true) → スタート(false) へ変更

                    //表示切り替え
                    ll_startGroup.setVisibility(View.VISIBLE);
                    ll_limitGroup.setVisibility(View.GONE);

                    //設定日時を同期
                    tv_startTime.setText(tv_limitTime.getText());
                    tv_startDate.setText(tv_limitDate.getText());

                    //アニメーション：スタックエリア
                    anim = AnimationUtils.loadLayoutAnimation(mContext, R.anim.layout_anim_que_task);
                    //アニメーション：ベース時間
                    anim_limit = R.anim.limit_up_disappear;
                    anim_start = R.anim.start_up_appear;
                    //アニメーション：切り替えアイコン
                    anim_fab = R.anim.rotation_from_180_to_360;

                    iconResId = R.drawable.ic_switch_direction_limit_32;
                }

                //基準の時間を反転し、積み上げエリアアダプタへ変更通知
                mStackAreaAdapter.reverseTime();
                mStackAreaAdapter.notifyDataSetChanged();

                //積み上げ方向（リサイクラービューアイテムの表示位置 上or下）
                RecyclerView rv_stackArea = mRootLayout.findViewById(R.id.rv_stackArea);
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) rv_stackArea.getLayoutManager();

                //現在の表示方向を逆にする
                boolean isStackFromEnd = !(linearLayoutManager.getStackFromEnd());
                linearLayoutManager.setStackFromEnd(isStackFromEnd);

                //アニメーションを実行
                rv_stackArea.setLayoutAnimation(anim);

                Animation animation = AnimationUtils.loadAnimation(mContext, anim_limit);
                ll_limitGroup.startAnimation(animation);
                animation = AnimationUtils.loadAnimation(mContext, anim_start);
                ll_startGroup.startAnimation(animation);

                animation = AnimationUtils.loadAnimation(mContext, anim_fab);
                view.startAnimation(animation);

                //アイコン変更
                fab_switchDirection.setImageResource( iconResId );
            }
        });
    }

    /*
     * FAB(アラーム設定)の設定
     */
    private void setupFabSetAlarm() {
        // アラーム開始ボタンの設定
        mfab_setAlarm.setOnClickListener(new View.OnClickListener() {
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

                //「やること」未選択の場合
                if (mStackTaskList.size() == 0) {
                    //メッセージを表示
                    toast.setText("やることを選択してください");
                    toast.show();
                    return;
                }

                //ダイアログの生成
                createSetAlarmDialog(mStackTable);

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
    }

    /*
     * FAB(スタックタスク全削除)の設定
     */
    private void setupFabStackTaskClear() {

        FloatingActionButton fab_refAlarm = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_stackTaskClear);

        fab_refAlarm.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View view) {

                if( mStackTaskList.size() == 0 ){
                    //やることが空の場合、何もしない旨のメッセージを表示して終了
                    Toast toast = new Toast(mContext);
                    toast.setText(R.string.toast_no_stack_task);
                    toast.show();

                    return;
                }

                //スタックされたやることを全て保持
                TaskArrayList<TaskTable> deletedTaskList = (TaskArrayList<TaskTable>) mStackTaskList.clone();

                //全削除
                mStackTaskList.clear();

                //下部ナビゲーションを取得
                BottomNavigationView bnv = mParentActivity.findViewById(R.id.bnv_nav);

                //スナックバーを保持する親ビュー
                ConstraintLayout cl_mainContainer = mParentActivity.findViewById(R.id.cl_mainContainer);

                //UNDOメッセージの表示
                //★備考★他のとまとめられるならまとめたい
                Snackbar snackbar = Snackbar
                        .make(cl_mainContainer, R.string.snackbar_delete, Snackbar.LENGTH_LONG)
                        //アクションボタン押下時の動作
                        .setAction(R.string.snackbar_undo, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                //UNDOが選択されたため、削除されたアイテムを元に戻す
                                int size = deletedTaskList.size();
                                for( int i = 0; i < size; i++ ){
                                    mStackTable.insertTask(i, deletedTaskList.get(i));
                                    mStackAreaAdapter.notifyItemInserted(i);
                                }

                                //各時間を変更させるため、アダプタへ変更を通知
                                mStackAreaAdapter.notifyDataSetChanged();

                                //削除リストをクリア
                                deletedTaskList.clear();
                            }
                        })

                        //下部ナビゲーションの上に表示させるための設定
                        .setAnchorView(bnv)
                        .setBackgroundTint(getResources().getColor(R.color.basic))
                        .setTextColor(getResources().getColor(R.color.white))
                        .setActionTextColor(getResources().getColor(R.color.white));

                //表示
                snackbar.show();

                //各開始時間を変更させるため、アダプタへ変更を通知
                mStackAreaAdapter.notifyDataSetChanged();
            }
        });
    }

    /*
     * BottomSheetの設定
     */
    private void setupBottomSheet() {

        //BottomSheetBehavior と連動するpadding
        LinearLayout ll_manageStack = mRootLayout.findViewById(R.id.ll_manageStack);

        //BottomSheet
        View ll_bottomSheet = mRootLayout.findViewById(R.id.ll_bottomSheet);

        //BottomSheetBehavior
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(ll_bottomSheet);

        //レイアウト確定後、ビューに合わせてサイズ設定
        ViewTreeObserver observer = ll_bottomSheet.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onGlobalLayout() {

                        //レイアウト確定後は不要なので、本リスナー削除
                        ll_bottomSheet.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        //画面上に残したいサイズ
                        View ll_peek = ll_bottomSheet.findViewById(R.id.ll_peek);
                        int peekHeight = ll_peek.getHeight();

                        //初期PaddingBottom
                        mBasicPadding = ll_manageStack.getPaddingBottom() + peekHeight;

                        //BottomSheetの高さ
                        mExpandedBSHeight = ll_bottomSheet.getHeight();

                        //Padding設定
                        ll_manageStack.setPadding(
                                ll_manageStack.getPaddingLeft(),
                                ll_manageStack.getPaddingTop(),
                                ll_manageStack.getPaddingRight(),
                                mBasicPadding + mExpandedBSHeight
                        );

                        //元々のpadding分（下部ナビゲーション分設定済み）を加味した分をPeekHeightとする
                        peekHeight += behavior.getPeekHeight();

                        behavior.setPeekHeight(peekHeight);
                    }
                }
        );

        //開いた状態で開始
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        //スライド時の設定
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                //case BottomSheetBehavior.STATE_DRAGGING:
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

                //Log.i("test", "slideOffset=" + slideOffset);

                //padding調整値
                int offset = (int) (mExpandedBSHeight * slideOffset);

                ll_manageStack.setPadding(
                        ll_manageStack.getPaddingLeft(),
                        ll_manageStack.getPaddingTop(),
                        ll_manageStack.getPaddingRight(),
                        (int) (mBasicPadding + offset)
                );
            }
        });
    }

    /*
     * アラームダイアログの生成
     */
    private void createSetAlarmDialog(StackTaskTable stack) {

        //FragmentManager生成
        FragmentManager transaction = getParentFragmentManager();

        //ダイアログを生成
        CreateSetAlarmDialog dialog = new CreateSetAlarmDialog(stack);

        //設定ボタン押下時リスナー
        dialog.setOnSetBtnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //設定をアラーム情報としてコピー
                mAlarmStack = (StackTaskTable) mStackTable.clone();

                //スタック情報変更ON
                mIsStackChg = true;

                //DB更新
                mParentActivity.setAlarmStack(mAlarmStack);

                //アラーム設定
                mParentActivity.setupAlarm(mStackTable);
            }
        });

        dialog.show(transaction, "alarm");
    }


    /*
     * 選択エリアのやること／グループクリックリスナー
     */
    private class SelectItemClickListener implements View.OnClickListener {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onClick(View view) {

            //スタックタスク変更ON
            mIsStackChg = true;

            //ドラッグしたビューからデータを取得
            //View dragView = (View) dragEvent.getLocalState();

            //やること積み上げアニメーションの適用Idx
            int animIdx;

            //ドロップされたのが「やること」か「グループ」か
            TextView tv_taskInGroup = view.findViewById(R.id.tv_taskInGroup);
            if (tv_taskInGroup == null) {
                //「やること」がクリックされた場合

                //「やること」をリストへ追加
                animIdx = addTaskToStackList(view);

            } else {
                //--「グループ」がクリックされた場合

                //グループ内の「やること」をリストへ追加
                animIdx = addGroupToStackList(view);
                if (animIdx == -1) {
                    //何も追加されてなければ、アダプタへの通知はなし
                    return;
                }
            }

            //アダプタへ通知
            mStackAreaAdapter.setInsertAnimationIdx(animIdx);
            mStackAreaAdapter.notifyDataSetChanged();
            //mStackAreaAdapter.notifyItemInserted(0);
        }

        /*
         * 積まれた「やること」リストに「やること」を追加
         * ★備忘★animを返すのは微妙
         */
        private int addTaskToStackList(View dragView) {

            //ドロップされたビューからデータを取得
            TextView tv_pid      = dragView.findViewById(R.id.tv_pid);
            //TextView tv_taskName = dragView.findViewById(R.id.tv_taskName);
            //TextView tv_taskTime = dragView.findViewById(R.id.tv_taskTime);

            int pid      = Integer.parseInt(tv_pid.getText().toString());
            //int taskTime = Integer.parseInt(tv_taskTime.getText().toString());

            //スタックにやることを追加
            //※同じものが積まれたとき、開始時間が同じになるのを防ぐため、cloneを追加
            TaskTable task = mTaskList.getTaskByPid(pid);
            //TaskTable task = new TaskTable(pid, tv_taskName.getText().toString(), taskTime);
            mStackTable.addTask( (TaskTable)task.clone() );

            //追加アニメーションを適用するIndex を返す
            return ( mIsLimit ? 0 : (mStackTaskList.size() - 1) );
        }

        /*
         * 積まれた「やること」リストに「グループ」を追加
         */
        private int addGroupToStackList(View dragView) {

            //グループ内やることがあるかチェック
            TextView tv_taskInGroup = dragView.findViewById(R.id.tv_taskInGroup);
            String   taskPidsStr    = tv_taskInGroup.getText().toString();
            List<Integer> pids      = TaskTableManager.convertIntArray(taskPidsStr);
            if (pids == null) {
                //何もないなら、何もせず終了
                return -1;
            }

            //追加アニメーションを適用するIndex
            int animIdx = 0;

            //グループ内の「やること」を積み上げ先のリストに追加する
            int taskNum = pids.size();

            if (mIsLimit) {
                //グループ内やることを、「逆」から追加
                int i = taskNum - 1;
                for (; i >= 0; i--) {
                    Integer pid = pids.get(i);
                    TaskTable task = mTaskList.getTaskByPid(pid);
                    if (task != null) {
                        //リスト追加
                        //※同じものが積まれたとき、開始時間が同じになるのを防ぐため、cloneを追加
                        mStackTable.addTask( (TaskTable)task.clone() );

                        //積み上げ数を加算
                        animIdx++;
                    }
                }

                //積み上げられた最後のIndexを指定するため、-1して調整
                animIdx -= 1;

            } else {
                //適用アニメーションは、初めに追加するIndex
                animIdx = mStackTaskList.size();

                //グループ内やることを、「頭」から追加
                for (int i = 0; i < taskNum; i++) {
                    Integer pid = pids.get(i);
                    TaskTable task = mTaskList.getTaskByPid(pid);
                    if (task != null) {
                        //リスト追加
                        //※同じものが積まれたとき、開始時間が同じになるのを防ぐため、cloneを追加
                        mStackTable.addTask( (TaskTable)task.clone() );
                    }
                }
            }

            return animIdx;
        }

    }



    /*
     * ドラッグリスナー（ビューがドロップされた時の動作）
     *　　「やること」を積み上げエリアにドラッグアンドドロップするときに使用
     */
/*
    private class DragListener implements View.OnDragListener {
        @SuppressLint("NotifyDataSetChanged")
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

                    //スタックタスク変更ON
                    mIsStackChg = true;

                    //ドラッグしたビューからデータを取得
                    View dragView = (View) dragEvent.getLocalState();

                    //やること積み上げアニメーションの適用Idx
                    int animIdx;

                    //ドロップされたのが「やること」か「グループ」か
                    TextView tv_taskInGroup = dragView.findViewById(R.id.tv_taskInGroup);
                    if (tv_taskInGroup == null) {
                        //「やること」がドロップされた場合

                        //「やること」をリストへ追加
                        animIdx = addTaskToStackList(dragView);

                    } else {
                        //--「グループ」がドロップされた場合

                        //グループ内の「やること」をリストへ追加
                        animIdx = addGroupToStackList(dragView);
                        if (animIdx == -1) {
                            //何も追加されてなければ、アダプタへの通知はなし
                            break;
                        }
                    }

                    //アダプタへ通知
                    mStackAreaAdapter.setInsertAnimationIdx(animIdx);
                    mStackAreaAdapter.notifyDataSetChanged();
                    //mStackAreaAdapter.notifyItemInserted(0);

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

        */
/*
         * 積まれた「やること」リストに「やること」を追加
         * ★備忘★animを返すのは微妙
         *//*

        private int addTaskToStackList(View dragView) {

            //ドロップされたビューからデータを取得
            TextView tv_pid      = dragView.findViewById(R.id.tv_pid);
            //TextView tv_taskName = dragView.findViewById(R.id.tv_taskName);
            //TextView tv_taskTime = dragView.findViewById(R.id.tv_taskTime);

            int pid      = Integer.parseInt(tv_pid.getText().toString());
            //int taskTime = Integer.parseInt(tv_taskTime.getText().toString());

            //スタックにやることを追加
            //※同じものが積まれたとき、開始時間が同じになるのを防ぐため、cloneを追加
            TaskTable task = mTaskList.getTaskByPid(pid);
            //TaskTable task = new TaskTable(pid, tv_taskName.getText().toString(), taskTime);
            mStackTable.addTask( (TaskTable)task.clone() );

            //追加アニメーションを適用するIndex
            int animIdx = (mIsLimit ? 0 : (mStackTaskList.size() - 1));

            return animIdx;
        }

        */
/*
         * 積まれた「やること」リストに「グループ」を追加
         *//*

        private int addGroupToStackList(View dragView) {

            //グループ内やることがあるかチェック
            TextView tv_taskInGroup = dragView.findViewById(R.id.tv_taskInGroup);
            String   taskPidsStr    = tv_taskInGroup.getText().toString();
            List<Integer> pids      = TaskTableManager.convertIntArray(taskPidsStr);
            if (pids == null) {
                //何もないなら、何もせず終了
                return -1;
            }

            //追加アニメーションを適用するIndex
            int animIdx = 0;

            //グループ内の「やること」を積み上げ先のリストに追加する
            int taskNum = pids.size();

            if (mIsLimit) {
                //グループ内やることを、「逆」から追加
                int i = taskNum - 1;
                for (; i >= 0; i--) {
                    Integer pid = pids.get(i);
                    TaskTable task = mTaskList.getTaskByPid(pid);
                    if (task != null) {
                        //リスト追加
                        //※同じものが積まれたとき、開始時間が同じになるのを防ぐため、cloneを追加
                        mStackTable.addTask( (TaskTable)task.clone() );

                        //積み上げ数を加算
                        animIdx++;
                    }
                }

                //積み上げられた最後のIndexを指定するため、-1して調整
                animIdx -= 1;

            } else {
                //適用アニメーションは、初めに追加するIndex
                animIdx = mStackTaskList.size();

                //グループ内やることを、「頭」から追加
                for (int i = 0; i < taskNum; i++) {
                    Integer pid = pids.get(i);
                    TaskTable task = mTaskList.getTaskByPid(pid);
                    if (task != null) {
                        //リスト追加
                        //※同じものが積まれたとき、開始時間が同じになるのを防ぐため、cloneを追加
                        mStackTable.addTask( (TaskTable)task.clone() );
                    }
                }
            }

            return animIdx;
        }

    }
*/


    @Override
    public void onPause() {
        super.onPause();

        //フラグメント停止タイミングで、変更があればDB更新
        if( mIsStackChg ){
            mParentActivity.setStackTable(mStackTable);
        }
    }

    /*
     * 選択エリアレイアウト調整用
     */
    private class SelectAreaItemDecoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position == state.getItemCount() - 1) {
                //最後の要素の右に、FAB分の空間を設定
                FloatingActionButton fab_parent = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_parent);
                outRect.right = fab_parent.getWidth();
            }
        }
    }

    /*
     * ベース時間クリックリスナー
     */
    private class BaseTimeListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            //-- 時刻設定ダイアログの生成
            createTimeDialog((TextView) view);
        }
    }

    /*
     * ベース日クリックリスナー
     */
    private class BaseDateListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            //-- 時刻設定ダイアログの生成
            createCalendarDialog((TextView) view);
        }
    }

    /*
     * 親Fabクリックリスナー
     */
    private class ParentFabOnClickListener implements View.OnClickListener {

        //表示フラグ
        private boolean isShow;

        //子Fab
        private final FloatingActionButton fab_switchDirection;
        private final FloatingActionButton fab_cancelAlarm;
        private final FloatingActionButton fab_setAlarm;

        //アニメーション(表示)
        Animation showAnimation1;
        Animation showAnimation2;
        Animation showAnimation3;

        //アニメーション(非表示)
        Animation hideAnimation1;
        Animation hideAnimation2;
        Animation hideAnimation3;

        /*
         * コンストラクタ
         */
        public ParentFabOnClickListener() {

            isShow = false;

            fab_switchDirection = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_switchDirection);
            fab_cancelAlarm = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_stackTaskClear);
            fab_setAlarm = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_setAlarm);

            showAnimation1 = AnimationUtils.loadAnimation(mContext, R.anim.show_child_fab_1);
            showAnimation2 = AnimationUtils.loadAnimation(mContext, R.anim.show_child_fab_2);
            showAnimation3 = AnimationUtils.loadAnimation(mContext, R.anim.show_child_fab_3);

            hideAnimation1 = AnimationUtils.loadAnimation(mContext, R.anim.hide_child_fab_1);
            hideAnimation2 = AnimationUtils.loadAnimation(mContext, R.anim.hide_child_fab_2);
            hideAnimation3 = AnimationUtils.loadAnimation(mContext, R.anim.hide_child_fab_3);
        }

        @Override
        public void onClick(View view) {

            int iconId;

            if (isShow) {
                //（上から）非表示にする
                fab_switchDirection.hide();
                fab_cancelAlarm.hide();
                fab_setAlarm.hide();

                fab_switchDirection.startAnimation(hideAnimation1);
                fab_cancelAlarm.startAnimation(hideAnimation2);
                fab_setAlarm.startAnimation(hideAnimation3);

                iconId = R.drawable.ic_up_32;

            } else {

                //（下から）表示する
                fab_setAlarm.show();
                fab_cancelAlarm.show();
                fab_switchDirection.show();

                fab_setAlarm.startAnimation(showAnimation1);
                fab_cancelAlarm.startAnimation(showAnimation2);
                fab_switchDirection.startAnimation(showAnimation3);

                iconId = R.drawable.ic_down_32;
            }

            //フラグ切り替え
            isShow = !isShow;

            //アイコン切り替え
            ((FloatingActionButton)view).setImageResource(iconId);
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