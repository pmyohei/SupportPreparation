package com.example.supportpreparation.ui.groupManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.supportpreparation.AppDatabase;
import com.example.supportpreparation.AppDatabaseSingleton;
import com.example.supportpreparation.AsyncGroupTableOperaion;
import com.example.supportpreparation.CreateGroupDialog;
import com.example.supportpreparation.GroupArrayList;
import com.example.supportpreparation.GroupTable;
import com.example.supportpreparation.MainActivity;
import com.example.supportpreparation.R;
import com.example.supportpreparation.GroupRecyclerAdapter;
import com.example.supportpreparation.SelectAreaScrollListener;
import com.example.supportpreparation.TaskArrayList;
import com.example.supportpreparation.TaskRecyclerAdapter;
import com.example.supportpreparation.TaskTable;
import com.example.supportpreparation.ui.stackManager.StackManagerFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class GroupManagerFragment extends Fragment implements AsyncGroupTableOperaion.GroupOperationListener {

    private final int           NO_SEARCH = -1;                        //未発見
    public  final static int    DIV_GROUP_IN_TASK = 3;                 //グループ内やること-横幅分割数
    public  final static int    DIV_GROUP_IN_TASK_WIDTH = 4;           //グループ内やることブロックの横幅算出値

    private MainActivity                   mParentActivity;            //親アクティビティ
    private View                           mRootLayout;                //本フラグメントに設定しているレイアウト
    private Fragment                       mFragment;                  //本フラグメント
    private Context                        mContext;                   //コンテキスト（親アクティビティ）
    private AppDatabase                    mDB;                        //DB
    private TaskArrayList<TaskTable>       mTaskList;                  //「やること」リスト
    private GroupArrayList<GroupTable>     mGroupList;                 //「グループ」リスト
    private GroupRecyclerAdapter           mGroupAdapter;              //「グループ」表示アダプタ
    private FloatingActionButton           mFab;                       //フローティングボタン
    private AsyncGroupTableOperaion.GroupOperationListener
                                           mGroupDBListener;           //「グループ」DB操作リスナー


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //自身のフラグメントを保持
        mFragment = getParentFragmentManager().getFragments().get(0);
        //設定レイアウト
        mRootLayout = inflater.inflate(R.layout.fragment_group_manager, container, false);
        //親アクティビティのコンテキスト
        mContext = mRootLayout.getContext();
        //親アクティビティ
        mParentActivity = (MainActivity) getActivity();
        //DB操作インスタンスを取得
        mDB = AppDatabaseSingleton.getInstance(mContext);
        //「グループ」DB操作リスナー
        mGroupDBListener = (AsyncGroupTableOperaion.GroupOperationListener) mFragment;

        //「やること」リスト
        mTaskList = mParentActivity.getTaskData();
        //「グループ」リスト
        mGroupList = mParentActivity.getGroupData();

        //ビュー
        mFab = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_addSet);

        //現在登録されている「グループ」表示
        setupGroupList();

        //選択エリアのやること
        setupTaskSelectionArea();

        //BottomSheetの設定
        setupBottomSheet();

        // FloatingActionButton
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //-- 「グループ」追加ダイアログの生成
                createNewGroupDialog();
            }
        });

        return mRootLayout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


    /*
     * 「やること」データを選択エリアにセット
     */
    private void setupTaskSelectionArea() {

        //登録がなければ終了
        if (mTaskList == null || mTaskList.size() == 0) {
            return;
        }

        //-- 「やること」の表示（セットへ追加の選択用）
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
                int width = rv_task.getWidth() / StackManagerFragment.SELECT_TASK_AREA_DIV;

                //アダプタの生成・設定
                TaskRecyclerAdapter adapter = new TaskRecyclerAdapter(mContext, mTaskList, TaskRecyclerAdapter.SETTING.SELECT, width, 0);

                //ドラッグリスナーの設定
                adapter.setOnItemLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        view.startDrag(null, new View.DragShadowBuilder(view), (Object) view, 0);
                        return true;
                    }
                });

                //RecyclerViewにアダプタを設定
                rv_task.setAdapter(adapter);

                //FAB 分と重ならないように、最後のアイテムの右に空白を入れる
                rv_task.addItemDecoration( new RecyclerView.ItemDecoration(){
                    @Override
                    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                        int position = parent.getChildAdapterPosition(view);
                        if (position == state.getItemCount() - 1) {
                            //最後の要素の右に空間を設定
                            outRect.right = mFab.getWidth();
                        }
                    }
                });

                //本リスナーを削除（何度も処理する必要はないため）
                rv_task.getViewTreeObserver().removeOnPreDrawListener(this);

                //描画を中断するため、false
                return false;
            }
        });

        //スクロールリスナーの設定
        rv_task.addOnScrollListener(new SelectAreaScrollListener( mFab ));
    }

    /*
     * 「グループ」の表示
     *    登録済みの「グループ」を全て表示する。
     */
    private void setupGroupList() {

        //グループ内のやることの同期
        syncTaskInGroupData();

        //-- 「グループ」の表示
        //レイアウトからリストビューを取得
        RecyclerView rv_group = (RecyclerView) mRootLayout.findViewById(R.id.rv_groupList);

        //レイアウトマネージャの生成・設定（横スクロール）
        LinearLayoutManager l_manager = new LinearLayoutManager(mContext);
        rv_group.setLayoutManager(l_manager);

        //-- アダプタの設定は、サイズが確定してから行う
        // ビューツリー描画時に呼ばれるリスナーの設定
        rv_group.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {

                //グループのサイズ
                int height = (int)(rv_group.getHeight() * 0.5);

                //グループのリサイクラービューを基準に、横幅を決定
                //※グループ内やることのリサイクラービューは現時点では取得不可のため
                int width = rv_group.getWidth() / DIV_GROUP_IN_TASK_WIDTH;

                //グループ内「やること」のアダプタを設定
                for( GroupTable group: mGroupList ){
                    //アダプタ生成
                    //※高さはビューに依存「wrap_contents」
                    TaskArrayList<TaskTable> taskInGroupList = group.getTaskInGroupList();
                    TaskRecyclerAdapter adapter = new TaskRecyclerAdapter(mContext, taskInGroupList, TaskRecyclerAdapter.SETTING.IN_GROUP, width, 0);

                    //設定
                    group.setTaskAdapter(adapter);
                }

                //下部ナビゲーション
                BottomNavigationView bnv = mParentActivity.findViewById(R.id.bnv_nav);

                //スナックバーを保持する親ビュー
                ConstraintLayout cl_mainContainer = mParentActivity.findViewById(R.id.cl_mainContainer);

                //アダプタの生成・設定
                AsyncGroupTableOperaion.GroupOperationListener dbListener
                        = (AsyncGroupTableOperaion.GroupOperationListener) mFragment;
                mGroupAdapter = new GroupRecyclerAdapter(mContext, mGroupList, dbListener, height, bnv, cl_mainContainer);

                //リスナー設定(グループ名編集)
                mGroupAdapter.setOnGroupNameClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        createEditGroupDialog(view);
                    }
                });

                //リスナー設定(やることスクロール)
                mGroupAdapter.setOnTaskTouchListener(new View.OnTouchListener() {
                    @SuppressLint("ClickableViewAccessibility")
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        //グループの「やること」RecyclerViewがタッチされた時、親であるグループのRecyclerViewのスクロールを停止する
                        //※「やること」側をスクロールさせるため

                        Log.i("timming", "task setOnTaskTouchListener");

                        //アクションを取得
                        int action = event.getAction() & MotionEvent.ACTION_MASK;

                        if (action == MotionEvent.ACTION_DOWN) {
                            //タッチ検知されたら、グループ側のリサイクラービューのスクロールを無効化
                            rv_group.requestDisallowInterceptTouchEvent(true);

                        } else if (action == MotionEvent.ACTION_UP) {
                            //タッチアップが検知されたら、グループ側のリサイクラービューのスクロールを有効化
                            rv_group.requestDisallowInterceptTouchEvent(false);
                        }

                        return false;
                    }
                });

                //アダプタ設定
                rv_group.setAdapter(mGroupAdapter);

                //FAB 分と重ならないように、最後のアイテムの右に空白を入れる
                rv_group.addItemDecoration( new RecyclerView.ItemDecoration(){
                    //★備考★クラス化できそう
                    @Override
                    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                        int position = parent.getChildAdapterPosition(view);
                        if (position == state.getItemCount() - 1) {
                            //最後の要素の右に空間を設定
                            outRect.bottom = height;
                        }
                    }
                });

                //本リスナーを削除（何度も処理する必要はないため）
                rv_group.getViewTreeObserver().removeOnPreDrawListener(this);

                //描画を中断するため、false
                return false;
            }
        });

        //ドラッグ、スワイプの設定
        ItemTouchHelper helper = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback( 0, ItemTouchHelper.LEFT ){
                @Override
                public boolean onMove(@NonNull RecyclerView            recyclerView,
                                      @NonNull RecyclerView.ViewHolder viewHolder,
                                      @NonNull RecyclerView.ViewHolder target) {
                            /*
                            //！getAdapterPosition()←非推奨
                            final int fromPos = viewHolder.getAdapterPosition();
                            final int toPos   = target.getAdapterPosition();
                            //アイテム移動を通知
                            mGroupAdapter.notifyItemMoved(fromPos, toPos);
                            Log.i("test", "onMove " + fromPos + " " + toPos);
                            */
                    return true;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                    //スワイプされたデータ
                    final int        adapterPosition = viewHolder.getAdapterPosition();
                    final GroupTable deletedGroup    = mGroupList.get(adapterPosition);

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
                                    mGroupList.add(adapterPosition, deletedGroup);
                                    mGroupAdapter.notifyItemInserted(adapterPosition );
                                    rv_group.scrollToPosition(adapterPosition );
                                }
                            })
                            //スナックバークローズ時の動作
                            .addCallback(new Snackbar.Callback() {
                                @Override
                                public void onDismissed(Snackbar snackbar, int event) {
                                    super.onDismissed(snackbar, event);

                                    //アクションバー押下以外で閉じられた場合
                                    if (event != DISMISS_EVENT_ACTION) {
                                        //DBから削除
                                        int gPid = deletedGroup.getId();
                                        new AsyncGroupTableOperaion(mDB, mGroupDBListener, AsyncGroupTableOperaion.DB_OPERATION.DELETE, gPid).execute();
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
                    mGroupList.remove(adapterPosition);
                    mGroupAdapter.notifyItemRemoved(adapterPosition);
                }
            }
        );

        //リサイクラービューをヘルパーにアタッチ
        helper.attachToRecyclerView(rv_group);

        //スクロール時、ビューが画面中央に固定されるようにする
        //LinearSnapHelper snapHelper = new LinearSnapHelper();
        //snapHelper.attachToRecyclerView(rv_group);
    }

    /*
     * スタック中のやることを、登録されているやること情報と同期させる
     */
    private void syncTaskInGroupData() {

        //削除キュー
        List<Integer> delList = new ArrayList<>();

        for( GroupTable group: mGroupList ){

            int i = 0;
            TaskArrayList<TaskTable> taskInGroupList = group.getTaskInGroupList();
            for( TaskTable task: taskInGroupList ){

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
                taskInGroupList.remove(idx.intValue() );
            }

            //削除キュークリア
            delList.clear();
        }
    }

    /*
     * タスク生成ダイアログの生成
     */
    private void createNewGroupDialog() {
        //Bundle生成
        Bundle bundle = new Bundle();
        //FragmentManager生成
        FragmentManager transaction = getParentFragmentManager();

        //ダイアログを生成
        DialogFragment dialog = new CreateGroupDialog((AsyncGroupTableOperaion.GroupOperationListener) mFragment, false);
        dialog.setArguments(bundle);
        dialog.show(transaction, "NewGroup");
    }

    /*
     * 「グループ」編集ダイアログを生成
     */
    private void createEditGroupDialog(View view) {
        //「グループ」情報
        String groupName = ((TextView) view.findViewById(R.id.tv_groupName)).getText().toString();

        //ダイアログへ渡すデータを設定
        Bundle bundle = new Bundle();
        bundle.putString("EditGroupName", groupName);

        //FragmentManager生成
        FragmentManager transaction = getParentFragmentManager();

        //ダイアログを生成
        DialogFragment dialog = new CreateGroupDialog((AsyncGroupTableOperaion.GroupOperationListener) mFragment, true);
        dialog.setArguments(bundle);
        dialog.show(transaction, "EditGroup");
    }

    /*
     * BottomSheetの設定
     */
    private void setupBottomSheet() {

        //BottomSheet
        View ll_bottomSheet = mRootLayout.findViewById(R.id.ll_bottomSheet);

        //PeekHeight領域に対して、空のタッチリスナーを設定
        //※これをしないと、「グループ内やること」とかぶった時、BottomSheetのスクロールが無効になる
        //※スクロールが無効になるのは、「requestDisallowInterceptTouchEvent」にて制御をしているため。
        View ll_peek = ll_bottomSheet.findViewById(R.id.ll_peek);
        ll_peek.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                Log.i("timming", "ll_peek.setOnTouchListener");
                return true;
            }
        });

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
                        int peekHeight = ll_peek.getHeight();

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
                Log.i("timming", "group onStateChanged");
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

                Log.i("timming", "group onSlide");
            }
        });
    }

    /* --------------------------------------
     * 「グループ」
     */
    @Override
    public void onSuccessReadGroup(GroupArrayList<GroupTable> groupList) {
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onSuccessCreateGroup(Integer code, GroupTable group) {

        //戻り値に応じてトースト表示
        if( code == -1 ){
            //エラーメッセージを表示
            String message = "登録済みです";

            //トーストの生成
            Toast toast = new Toast(mContext);
            toast.setText(message);
            toast.show();

            return;
        }

        //空データ削除
        mGroupList.removeEmpty();

        //生成された「グループ」情報をリストに追加
        mGroupList.add( group );

        RecyclerView rv_group = (RecyclerView) mRootLayout.findViewById(R.id.rv_groupList);

        //グループのリサイクラービューを基準に、横幅を決定
        //※グループ内やることのリサイクラービューは現時点では取得不可のため
        int size = rv_group.getWidth() / DIV_GROUP_IN_TASK_WIDTH;

        //対応するアダプタを生成して、設定
        TaskArrayList<TaskTable> taskInGroupList = group.getTaskInGroupList();
        TaskRecyclerAdapter adapter = new TaskRecyclerAdapter(mContext, taskInGroupList, TaskRecyclerAdapter.SETTING.IN_GROUP, size, 0);
        group.setTaskAdapter(adapter);

        //アダプタに変更を通知
        mGroupAdapter.notifyDataSetChanged();

        //追加された位置へスクロール
        rv_group.scrollToPosition( mGroupList.size() - 1 );
    }

    @Override
    public void onSuccessDeleteGroup(String groupName) {

        //０件なら、空のデータをリストに入れておく
        //※選択エリアのサイズを確保するため
        mGroupList.addEmpty();
    }

    @Override
    public void onSuccessEditGroup(String preGroupName, String groupName) {
        //更新されたリストのIndexを取得
        int i = mGroupList.searchIdxByGroupName(preGroupName);
        if( i == NO_SEARCH ){
            //--フェールセーフ
            //見つからなければ、何もしない
            Log.i("failsafe", "onSuccessEditGroup couldn't found");
            return;
        }

        //リストの該当データを更新
        mGroupList.get(i).setGroupName(groupName);

        //アダプタに変更を通知
        mGroupAdapter.notifyItemChanged(i);

        //追加された位置へスクロール
        RecyclerView rv_group = (RecyclerView) mRootLayout.findViewById(R.id.rv_groupList);
        rv_group.scrollToPosition(i);

        //トーストの生成
        Toast toast = new Toast(mContext);
        toast.setText("更新しました");
        toast.show();
    }

    @Override
    public void onSuccessUpdateTask(int groupPid, String taskPidsStr){
        //更新されたグループを取得
        GroupTable group = mGroupList.getGroupByPid(groupPid);
        if( group == null ){
            //--フェールセーフ
            return;
        }

        //やること文字列を更新
        group.setTaskPidsStr(taskPidsStr);
    }

}