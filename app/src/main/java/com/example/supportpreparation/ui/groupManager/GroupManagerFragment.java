package com.example.supportpreparation.ui.groupManager;

import android.content.Context;
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
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.supportpreparation.AppDatabase;
import com.example.supportpreparation.AppDatabaseSingleton;
import com.example.supportpreparation.AsyncGroupTableOperaion;
import com.example.supportpreparation.AsyncTaskTableOperaion;
import com.example.supportpreparation.CreateGroupDialog;
import com.example.supportpreparation.GroupTable;
import com.example.supportpreparation.MainActivity;
import com.example.supportpreparation.R;
import com.example.supportpreparation.GroupRecyclerAdapter;
import com.example.supportpreparation.TaskRecyclerAdapter;
import com.example.supportpreparation.TaskTable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class GroupManagerFragment extends Fragment implements AsyncGroupTableOperaion.GroupOperationListener {

    private final int NO_SEARCH = -1;                   //未発見
    private final int NOT_DELETE_WAITING = -1;          //「グループ」削除待ちなし

    private MainActivity mParentActivity;            //親アクティビティ
    private View mRootLayout;                //本フラグメントに設定しているレイアウト
    private Fragment mFragment;                  //本フラグメント
    private Context mContext;                   //コンテキスト（親アクティビティ）
    private AppDatabase mDB;                        //DB
    private List<TaskTable> mTaskList;                  //「やること」リスト
    private List<GroupTable> mGroupList;                 //「グループ」リスト
    private List<List<TaskTable>> mTaskListInGroup;           //「グループ」に割り当てられた「やること」リスト
    private List<TaskRecyclerAdapter> mTaskInGroupAdapter;      //グループ内「やること」のアダプタ
    private GroupRecyclerAdapter mGroupAdapter;                //「グループ」表示アダプタ
    private AsyncGroupTableOperaion.GroupOperationListener
            mGroupDBListener;                   //「グループ」DB操作リスナー
    private int                     _mDeletedGroupPos;           //削除対象グループのID



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

        //「やること」リストを取得
        mTaskList = mParentActivity.getTaskData();

        //「グループ」リストを取得
        mGroupList       = mParentActivity.getGroupData();
        mTaskListInGroup = mParentActivity.getTaskListInGroup();

        //グループ内「やること」のアダプタを保持
        mTaskInGroupAdapter = new ArrayList<>();
        for( List<TaskTable> taskList: mTaskListInGroup){
            mTaskInGroupAdapter.add(
                    new TaskRecyclerAdapter(mContext, taskList, TaskRecyclerAdapter.SETTING.GROUP, 0, 0)
            );
        }
        
        //削除待ちの「グループ」-リストIndex
        _mDeletedGroupPos = NOT_DELETE_WAITING;

        //現在登録されている「やること」「グループ」を表示
        displayTask();
        displayGroup();

        // FloatingActionButton
        FloatingActionButton fab = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_addSet);
        fab.setOnClickListener(new View.OnClickListener() {
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
     * 「やること」データを表示エリアにセット
     */
    private void displayTask() {

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

                //RecyclerViewの横幅 / 2 を子アイテムの横幅とする
                int width = rv_task.getWidth() / 2;

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

                //本リスナーを削除（何度も処理する必要はないため）
                rv_task.getViewTreeObserver().removeOnPreDrawListener(this);

                //描画を中断するため、false
                return false;
            }
        });
    }

    /*
     * 「グループ」の表示
     *    登録済みの「グループ」を全て表示する。
     */
    private void displayGroup() {

        //-- 「グループ」の表示
        //レイアウトからリストビューを取得
        RecyclerView rv_group = (RecyclerView) mRootLayout.findViewById(R.id.rv_groupList);

        //レイアウトマネージャの生成・設定（横スクロール）
        LinearLayoutManager l_manager = new LinearLayoutManager(mContext);
        //l_manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        rv_group.setLayoutManager(l_manager);

        //-- アダプタの設定は、サイズが確定してから行う
        // ビューツリー描画時に呼ばれるリスナーの設定
        rv_group.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {

                //「RecyclerViewの高さ」の 3 / 4 を子アイテムの高さとする
                int height = rv_group.getHeight() * 3 / 4;

                //アダプタの生成・設定
                AsyncGroupTableOperaion.GroupOperationListener dbListener
                        = (AsyncGroupTableOperaion.GroupOperationListener) mFragment;
                mGroupAdapter = new GroupRecyclerAdapter(mContext, mGroupList, mTaskListInGroup, mTaskInGroupAdapter, dbListener, height);
                //リスナー設定(グループ名編集)
                mGroupAdapter.setOnGroupNameClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        createEditGroupDialog(view);
                    }
                });
                //リスナー設定(やることスクロール)
                mGroupAdapter.setOnTaskTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        //グループの「やること」RecyclerViewがタッチされた時、親であるグループのRecyclerViewのスクロールを停止する
                        //※「やること」側をスクロールさせるため

                        //アクションを取得
                        int action = event.getAction() & MotionEvent.ACTION_MASK;

                        if (action == MotionEvent.ACTION_DOWN) {
                            //タッチ検知されたら、スクロール無効
                            rv_group.requestDisallowInterceptTouchEvent(true);
                        }
                        else if (action == MotionEvent.ACTION_UP) {
                            //タッチアップが検知されたら、スクロール無効
                            rv_group.requestDisallowInterceptTouchEvent(false);
                        }

                        return false;
                    }
                });

                //アダプタ設定
                rv_group.setAdapter(mGroupAdapter);

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

                    if( _mDeletedGroupPos != NOT_DELETE_WAITING ){
                        //削除待ちのものがあるなら、何もしない
                        return;
                    }

                    //-- DBから削除
                    int i = viewHolder.getAdapterPosition();
                    String groupName = mGroupList.get(i).getGroupName();

                    new AsyncGroupTableOperaion(mDB, mGroupDBListener, AsyncGroupTableOperaion.DB_OPERATION.DELETE, groupName).execute();

                    //削除アイテムを保持
                    _mDeletedGroupPos = viewHolder.getAdapterPosition();

                    //※アダプタへの削除通知は、DBの削除完了後、行う
                }
            }
        );

        //リサイクラービューをヘルパーにアタッチ
        helper.attachToRecyclerView(rv_group);

        //ビューが画面中央に固定されるようにする
        LinearSnapHelper snapHelper = new LinearSnapHelper();
        //snapHelper.attachToRecyclerView(rv_group);
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
     * 「グループ」リストIndex検索
     */
    private int searchIdxGroupList(String groupName){

        int i = 0;
        for( GroupTable group: mGroupList ){
            if( group.getGroupName().equals( groupName ) ){
                return i;
            }
            i++;
        }
        return NO_SEARCH;
    }


    /* --------------------------------------
     * 「グループ」
     */
    @Override
    public void onSuccessReadGroup(List<GroupTable> groupList, List<List<TaskTable>> taskListInGroup) {

    }

    @Override
    public void onSuccessCreateGroup(Integer code, GroupTable group) {
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
        Toast toast = new Toast(mContext);
        toast.setText(message);
        toast.show();

        if( code == -1 ){
            //登録済みなら、ここで終了
            return;
        }

        //生成された「グループ」情報をリストに追加
        mGroupList.add( group );
        mTaskListInGroup.add( new ArrayList<>() );

        //対応するアダプタを生成してリストに追加
        int last = mTaskListInGroup.size() - 1;
        List<TaskTable> taskList = mTaskListInGroup.get( last );
        mTaskInGroupAdapter.add(
                new TaskRecyclerAdapter(mContext, taskList, TaskRecyclerAdapter.SETTING.GROUP, 0, 0)
        );

        //アダプタに変更を通知
        mGroupAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSuccessDeleteGroup(String groupName) {
        //リストから削除
        mGroupList.remove(_mDeletedGroupPos);
        mTaskListInGroup.remove(_mDeletedGroupPos);
        mTaskInGroupAdapter.remove(_mDeletedGroupPos);

        //ビューにアイテム削除を通知
        mGroupAdapter.notifyItemRemoved(_mDeletedGroupPos);

        //削除待ちなしに戻す
        _mDeletedGroupPos = NOT_DELETE_WAITING;

        //トーストの生成
        Toast toast = new Toast(mContext);
        toast.setText("削除しました");
        toast.show();
    }

    @Override
    public void onSuccessEditGroup(String preGroupName, String groupName) {
        //更新されたリストのIndexを取得
        int i = searchIdxGroupList(preGroupName);

        //フェールセーフ
        if( i == NO_SEARCH ){
            //見つからなければ、何もしない
            Log.i("failsafe", "onSuccessEditGroup couldn't found");
            return;
        }

        //リストの該当データを更新
        mGroupList.get(i).setGroupName(groupName);
        //mGroupList.set(i, group);

        //アダプタに変更を通知
        mGroupAdapter.notifyDataSetChanged();

        //トーストの生成
        Toast toast = new Toast(mContext);
        toast.setText("更新しました");
        toast.show();
    }
}