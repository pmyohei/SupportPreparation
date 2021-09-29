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
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.supportpreparation.AlarmBroadcastReceiver;
import com.example.supportpreparation.AppDatabase;
import com.example.supportpreparation.AppDatabaseSingleton;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StackManagerFragment extends Fragment {

    public final static int SELECT_TASK_AREA_DIV = 4;           //やること選択エリア-横幅分割数
    public final static int SELECT_GROUP_AREA_DIV = 3;          //やること選択エリア-横幅分割数

    private final int MAX_ALARM_CANCEL_NUM = 256;               //アラームキャンセル最大数

    private MainActivity mParentActivity;        //親アクティビティ
    private Fragment mFragment;              //本フラグメント
    private Context mContext;               //コンテキスト（親アクティビティ）
    private View mRootLayout;            //本フラグメントに設定しているレイアウト
    private AppDatabase mDB;                    //DB
    private LinearLayout mll_stackArea;          //「やること」積み上げ領域
    private StackTaskTable mStackTable;                            //スタックテーブル
    private TaskArrayList<TaskTable> mStackTaskList;             //積み上げ「やること」
    private TaskArrayList<TaskTable> mTaskList;              //「やること」
    private StackTaskRecyclerAdapter mStackAreaAdapter;      //積み上げ「やること」アダプタ
    private FloatingActionButton mfab_setAlarm;                   //フローティングボタン
    private TextView mtv_limitDate;          //リミット日のビュー
    private TextView mtv_limitTime;          //リミット時間のビュー
    private Intent mAlarmReceiverIntent;   //アラーム受信クラスのIntent
    private boolean mIsSelectTask;                          //フラグ-「やること」選択エリア表示中
    private boolean mIsLimit;                               //フラグ-リミット選択中

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Log.i("test", "StackManagerFragment onCreateView");

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
        //スタック情報取得
        mStackTable = mParentActivity.getStackTable();
        //やることリストを取得
        mTaskList = mParentActivity.getTaskData();
        //フラグ
        mIsSelectTask = mParentActivity.isSelectTask();
        mIsLimit = mStackTable.isLimit();

        //ビューを保持
        mtv_limitTime = (TextView) mRootLayout.findViewById(R.id.tv_limitTime);
        mtv_limitDate = (TextView) mRootLayout.findViewById(R.id.tv_limitDate);
        mfab_setAlarm = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_setAlarm);

        //アラーム受信クラスのIntent
        mAlarmReceiverIntent = new Intent(mParentActivity.getApplicationContext(), AlarmBroadcastReceiver.class);

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
                            mParentActivity.setStackTable(mStackTable);

                            //やること開始時間を変更
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
                            mParentActivity.setStackTable(mStackTable);

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

        //積み上げられた「やること」を取得
        mStackTaskList = mStackTable.getStackTaskList();

        //ドロップリスナーの設定(ドロップ先のビューにセット)
        DragListener listener = new DragListener();
        RecyclerView mll_stackArea = mRootLayout.findViewById(R.id.rv_stackArea);
        mll_stackArea.setOnDragListener(listener);

        //レイアウトからリストビューを取得
        RecyclerView rv_stackArea = (RecyclerView) mRootLayout.findViewById(R.id.rv_stackArea);

        //レイアウトマネージャの生成・設定（横スクロール）、下寄り表示
        LinearLayoutManager ll_manager = new LinearLayoutManager(mContext);
        ll_manager.setStackFromEnd(mIsLimit);    //表示方向はリミットかスタートかで切り分け
        rv_stackArea.setLayoutManager(ll_manager);

        //アダプタの生成・設定
        mStackAreaAdapter = new StackTaskRecyclerAdapter(mContext, mStackTable);
        rv_stackArea.setAdapter(mStackAreaAdapter);

        //スタート指定の場合
        if (!mIsLimit) {
            //アニメーションを変更
            rv_stackArea.setLayoutAnimation(
                    AnimationUtils.loadLayoutAnimation(mContext, R.anim.layout_anim_que_task)
            );
        }

        //((SimpleItemAnimator) rv_stackArea.getItemAnimator()).setSupportsChangeAnimations(false);

        //ドラッグアンドドロップ、スワイプの設定(リサイクラービュー)
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
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
/*
                        int size = mStackTask.size();
                        for( int i = 0; i < size; i++ ){
                            mStackAreaAdapter.notifyItemChanged(i);
                        }
*/

                //各開始時間も変更になるため、通知
                //mStackAreaAdapter.notifyDataSetChanged();

                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                //スワイプされたデータ
                final int adapterPosition = viewHolder.getAdapterPosition();
                final TaskTable deletedTask = mStackTaskList.get(adapterPosition);

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
                                mStackTable.insertTask(adapterPosition, deletedTask);
                                mStackAreaAdapter.notifyItemInserted(adapterPosition);

                                //表示位置を元に戻した対象の位置へ移動
                                rv_stackArea.scrollToPosition(adapterPosition);

                                //各時間を変更させるため、アダプタへ変更を通知
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
                                    //mStackAreaAdapter.clearAlarmList();
                                    //mStackAreaAdapter.notifyDataSetChanged();
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
                mStackTable.removeTask(adapterPosition);
                //mStackAreaAdapter.notifyItemRemoved(adapterPosition);

                //各開始時間を変更させるため、アダプタへ変更を通知
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
    private void setupBaseTimeDate() {

        //リミット・スタートのビュー
        LinearLayout ll_startGroup = (LinearLayout) mRootLayout.findViewById(R.id.ll_startGroup);
        LinearLayout ll_limitGroup = (LinearLayout) mRootLayout.findViewById(R.id.ll_limitGroup);

        //選択中の方向に応じた表示
        if (mIsLimit) {
            //リミットを表示
            ll_startGroup.setVisibility(View.INVISIBLE);
            ll_limitGroup.setVisibility(View.VISIBLE);
        } else {
            //スタートを表示
            ll_startGroup.setVisibility(View.VISIBLE);
            ll_limitGroup.setVisibility(View.INVISIBLE);
        }

        //親アクティビティで保持しているリミット時間を取得
        String limitTime = mStackTable.getTime();

        //時間設定-リミット
        mtv_limitTime.setText(limitTime);
        mtv_limitTime.setOnClickListener(new BaseTimeListener());

        //時間設定-スタート
        TextView tv_startTime = (TextView) mRootLayout.findViewById(R.id.tv_startTime);
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

                //RecyclerViewの横幅分割
                int width = rv_task.getWidth() / SELECT_TASK_AREA_DIV;
                if (width == 0) {
                    //RecyclerViewがGONE中は、アダプタ設定をしない
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

                //--FAB 分と重ならないように、最後のアイテムの右に空白を入れる
                rv_task.addItemDecoration(new SelectAreaItemDecoration());

                //本リスナーを削除（何度も処理する必要はないため）
                rv_task.getViewTreeObserver().removeOnPreDrawListener(this);

                //描画を中断するため、false
                return false;
            }
        });

        //スクロールリスナーの設定
        //★備忘★
        //rv_task.addOnScrollListener(new SelectAreaScrollListener(mfab_setAlarm));
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

                //--FAB 分と重ならないように、最後のアイテムの右に空白を入れる
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
    private void setupFabSwitchDirection(){

        FloatingActionButton fab_switchDirection = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_switchDirection);

        //設定アイコンの取得
        if( !mIsLimit){
            //スタート指定の場合、初期アイコンを変更
            fab_switchDirection.setImageResource(R.drawable.ic_switch_direction_limit_32);
        }

        fab_switchDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //スタート側のビュー
                LinearLayout ll_startGroup = (LinearLayout) mRootLayout.findViewById(R.id.ll_startGroup);
                TextView tv_startTime = (TextView) ll_startGroup.findViewById(R.id.tv_startTime);
                TextView tv_startDate = (TextView) ll_startGroup.findViewById(R.id.tv_startDate);

                //リミット側のビュー
                LinearLayout ll_limitGroup = (LinearLayout) mRootLayout.findViewById(R.id.ll_limitGroup);
                TextView tv_limitTime = (TextView) ll_limitGroup.findViewById(R.id.tv_limitTime);
                TextView tv_limitDate = (TextView) ll_limitGroup.findViewById(R.id.tv_limitDate);

                //適用するアニメーション
                LayoutAnimationController anim;
                int anim_limit;
                int anim_start;
                int anim_fab;

                //フラグ反転
                mIsLimit = !mIsLimit;
                //親側データと同期
                mStackTable.setIsLimit(mIsLimit);
                mParentActivity.setStackTable(mStackTable);

                if(mIsLimit){
                    //--スタート(false) → リミット(true) へ変更された

                    //表示切り替え
                    ll_startGroup.setVisibility( View.INVISIBLE );
                    ll_limitGroup.setVisibility( View.VISIBLE );

                    //設定日時を同期
                    tv_limitTime.setText( tv_startTime.getText() );
                    tv_limitDate.setText( tv_startDate.getText() );

                    //アニメーション：スタックエリア
                    anim = AnimationUtils.loadLayoutAnimation(mContext, R.anim.layout_anim_stack_task);
                    //アニメーション：ベース時間
                    anim_limit = R.anim.limit_down_appear;
                    anim_start = R.anim.start_down_disappear;
                    //アニメーション：切り替えアイコン
                    anim_fab = R.anim.switch_to_que;

                } else {
                    //--リミット(true) → スタート(false) へ変更

                    //表示切り替え
                    ll_startGroup.setVisibility( View.VISIBLE );
                    ll_limitGroup.setVisibility( View.INVISIBLE );

                    //設定日時を同期
                    tv_startTime.setText( tv_limitTime.getText() );
                    tv_startDate.setText( tv_limitDate.getText() );

                    //アニメーション：スタックエリア
                    anim = AnimationUtils.loadLayoutAnimation(mContext, R.anim.layout_anim_que_task);
                    //アニメーション：ベース時間
                    anim_limit = R.anim.limit_up_disappear;
                    anim_start = R.anim.start_up_appear;
                    //アニメーション：切り替えアイコン
                    anim_fab = R.anim.switch_to_stack;
                }

                //基準の時間を反転し、積み上げエリアアダプタへ変更通知
                mStackAreaAdapter.reverseTime();
                mStackAreaAdapter.notifyDataSetChanged();

                //積み上げ方向（リサイクラービューアイテムの表示位置 上or下）
                RecyclerView rv_stackArea = (RecyclerView) mRootLayout.findViewById(R.id.rv_stackArea);
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager)rv_stackArea.getLayoutManager();

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
            }
        });
    }

    /*
     * FAB(アラーム設定)の設定
     */
    private void setupFabSetAlarm(){
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
                int idx = mStackTaskList.getAlarmFirstArriveIdx();
                if (idx == TaskArrayList.NO_DATA) {
                    //すべてのアラームが過ぎてしまっている場合
                    //※過去のアラームに対して、再度アラームを設定しようとした場合のガード処理
                    toast.setText("現在時刻よりも後に設定してください");
                    toast.show();
                    return;
                }

                //各「やること」のアラームを設定
                int size = mStackTaskList.size();
                for (; idx < size; idx++) {
                    //★備忘★アラームは、ここだけではなく、ベース時間も追加する必要がある
                    long millis = mStackTable.getAlarmCalender(idx).getTimeInMillis();

                    //アラームの設定
                    PendingIntent pending
                            = PendingIntent.getBroadcast(mParentActivity.getApplicationContext(), requestCode, mAlarmReceiverIntent, 0);
                    am.setExact(AlarmManager.RTC_WAKEUP, millis, pending);

                    //リクエストコードを更新
                    requestCode++;
                }

                //「積み上げやること」をDBに保存
                //mParentActivity.setStackTaskData(mStackTaskList);

                //メッセージを表示
                //★備忘★
                //toast.setText("アラームを設定しました");
                toast.setText("アラーム実装中");
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
                    View dragView = (View) dragEvent.getLocalState();

                    //やること積み上げアニメーションの適用Idx
                    int animIdx;

                    //ドロップされたのが「やること」か「グループ」か
                    TextView tv_taskInGroup = dragView.findViewById(R.id.tv_taskInGroup);
                    if (tv_taskInGroup == null) {
                        //--「やること」がドロップされた場合

                        //「やること」をリストへ追加
                        animIdx = addTaskToStackList(dragView);

                    } else {
                        //--「グループ」がドロップされた場合

                        //グループ内の「やること」をリストへ追加
                        animIdx = addGroupToStackList(dragView);
                        if( animIdx == -1 ){
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

        /*
         * 積まれた「やること」リストに「やること」を追加
         * ★備忘★animを返すのは微妙
         */
        private int addTaskToStackList(View dragView) {

            //ドロップされたビューからデータを取得
            TextView tv_pid = dragView.findViewById(R.id.tv_pid);
            TextView tv_taskName = dragView.findViewById(R.id.tv_taskName);
            TextView tv_taskTime = dragView.findViewById(R.id.tv_taskTime);

            int pid = Integer.parseInt(tv_pid.getText().toString());
            int taskTime = Integer.parseInt(tv_taskTime.getText().toString());

            //スタックにやることを追加
            TaskTable task = new TaskTable(pid, tv_taskName.getText().toString(), taskTime);
            mStackTable.addTask(task);

            //追加アニメーションを適用するIndex
            int animIdx = (mIsLimit ? 0: (mStackTaskList.size() - 1) );

            return animIdx;
        }

        /*
         * 積まれた「やること」リストに「グループ」を追加
         */
        private int addGroupToStackList( View dragView ){

            //グループ内やることがあるかチェック
            TextView tv_taskInGroup = dragView.findViewById(R.id.tv_taskInGroup);
            String taskPidsStr = tv_taskInGroup.getText().toString();
            List<Integer> pids = TaskTableManager.getPidsIntArray(taskPidsStr);
            if (pids == null) {
                //何もないなら、何もせず終了
                return -1;
            }

            //追加アニメーションを適用するIndex
            int animIdx = 0;

            //グループ内の「やること」を積み上げ先のリストに追加する
            int taskNum = pids.size();

            if(mIsLimit){
                //グループ内やることを、「逆」から追加
                int i = taskNum - 1;
                for (; i >= 0; i--) {
                    Integer pid = pids.get(i);
                    TaskTable task = mTaskList.getTaskByPid(pid);
                    if (task != null) {
                        //リスト追加
                        mStackTable.addTask(task);

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
                for ( int i = 0; i < taskNum; i++) {
                    Integer pid = pids.get(i);
                    TaskTable task = mTaskList.getTaskByPid(pid);
                    if (task != null) {
                        //リスト追加
                        mStackTable.addTask(task);
                    }
                }
            }

            return animIdx;
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
                outRect.right = mfab_setAlarm.getWidth();
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
        public ParentFabOnClickListener(){

            isShow = false;

            fab_switchDirection = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_switchDirection);
            fab_cancelAlarm = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_cancelAlarm);
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

            if( isShow ){
                //非表示（上から）
                fab_switchDirection.hide();
                fab_cancelAlarm.hide();
                fab_setAlarm.hide();

                fab_switchDirection.startAnimation( hideAnimation1 );
                fab_cancelAlarm.startAnimation( hideAnimation2 );
                fab_setAlarm.startAnimation( hideAnimation3 );

                //フラグ切り替え
                isShow = false;

            } else {

                //表示（下から）
                fab_setAlarm.show();
                fab_cancelAlarm.show();
                fab_switchDirection.show();

                fab_setAlarm.startAnimation( showAnimation1 );
                fab_cancelAlarm.startAnimation( showAnimation2 );
                fab_switchDirection.startAnimation( showAnimation3 );

                //フラグ切り替え
                isShow = true;
            }
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