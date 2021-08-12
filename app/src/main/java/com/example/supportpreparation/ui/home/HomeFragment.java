package com.example.supportpreparation.ui.home;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
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

import com.example.supportpreparation.AppDatabase;
import com.example.supportpreparation.AppDatabaseSingleton;
import com.example.supportpreparation.MainActivity;
import com.example.supportpreparation.R;
import com.example.supportpreparation.TaskRecyclerAdapter;
import com.example.supportpreparation.TaskTable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HomeFragment extends Fragment {

    private MainActivity            mParentActivity;            //
    private Fragment                mFragment;                  //本フラグメント
    private Context                 mContext;                   //コンテキスト（親アクティビティ）
    private View                    mRootLayout;                //本フラグメントに設定しているレイアウト
    private AppDatabase             mDB;                        //DB
    private LinearLayout            mll_stackArea;              //「やること」積み上げ領域
    private List<TaskTable>         mStackTask;                 //積み上げ「やること」
    private TaskRecyclerAdapter     mStackAreaAdapter;          //積み上げ「やること」アダプタ
    private FloatingActionButton    mFab;                       //フローティングボタン


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //自身のフラグメントを保持
        mFragment = getParentFragmentManager().getFragments().get(0);
        //設定レイアウト
        mRootLayout = inflater.inflate(R.layout.fragment_home, container, false);
        //親アクティビティのコンテキスト
        mContext = mRootLayout.getContext();
        //DB操作インスタンスを取得
        mDB = AppDatabaseSingleton.getInstance(mRootLayout.getContext());
        //親アクティビティ
        mParentActivity = (MainActivity) getActivity();

        //積み上げられた「やること」を取得
        mStackTask = mParentActivity.getStackTaskData();

        //「やること」積み上げエリア
        this.setStackTaskArea();
        //「リミット日時」を設定
        this.setDisplayLimitDate();
        //「やること」を表示
        this.setDisplayTaskData();

        //-- リミット時間の設定
        TextView tv_limit = (TextView) mRootLayout.findViewById(R.id.tv_limitTime);

        //文字列の設定
        String limitTime = mParentActivity.getLimitTime();
        tv_limit.setText(limitTime);

        tv_limit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //-- 時刻設定ダイアログの生成
                createTimeDialog();
            }
        });

        // FloatingActionButton
        mFab = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_startSupport);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //時間未入力
                String noInputStr = getString(R.string.limittime_no_input);
                if (tv_limit.getText().toString().equals(noInputStr)) {
                    //メッセージを表示
                    Toast toast = new Toast(mContext);
                    toast.setText("時間を設定してください");
                    toast.show();
                    return;
                }

                //「やること」未選択の場合
                if (mStackTask.size() == 0) {
                    //メッセージを表示
                    Toast toast = new Toast(mContext);
                    toast.setText("やることを選択してください");
                    toast.show();
                    return;
                }

                //「積み上げやること」をDBに保存
                mParentActivity.setStackTaskData( mStackTask );

                //メッセージを表示
                Toast toast = new Toast(mContext);
                toast.setText("保存しました");
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /*
     * 時間入力ダイアログの生成
     */
    private void createTimeDialog() {

        Calendar calendar = Calendar.getInstance();

        //タイムピッカーダイアログの表示
        TimePickerDialog dialog = new TimePickerDialog(
                mContext,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                        //現在時刻よりも後かどうか
                        if( isAfterSetTime(hourOfDay, minute) ){
                            //入力時刻を設定
                            String limit = String.format("%02d:%02d", hourOfDay, minute);
                            TextView tv_limit = mRootLayout.findViewById(R.id.tv_limitTime);
                            tv_limit.setText(limit);

                            //共通データとして保持
                            mParentActivity.setLimitTime(limit);

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
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

                        //現在日よりも後かどうか
                        if( isAfterSetDate( year, month, dayOfMonth) ){
                            //日付を取得して表示
                            TextView tv_limitDate = mRootLayout.findViewById(R.id.tv_limitDate);
                            tv_limitDate.setText(String.format("%02d/%02d", month + 1, dayOfMonth));

                            //共通データとして保持
                            mParentActivity.setLimitDate(String.format("%04d/%02d/%02d", year, month + 1, dayOfMonth));

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
    private boolean isAfterSetDate(int year, int month, int dayOfMonth){

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
    private boolean isAfterSetTime(int hourOfDay, int minute){

        //現在時分
        Date nowDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String nowDateStr = sdf.format(nowDate);

        //設定中の日付
        String setLimitDate = mParentActivity.getLimitDate();

        //Log.i("test", "nowDateStr=" + nowDateStr);
        //Log.i("test", "setLimitDate=" + setLimitDate);

        //本日の時刻でなければ、後であること確定
        if( setLimitDate.compareTo(nowDateStr) != 0 ){
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

        if( setStr.compareTo(nowStr) >= 0 ){
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

        //ドロップリスナーの設定
        DragListener listener = new DragListener();
        mll_stackArea = mRootLayout.findViewById(R.id.ll_stackArea);
        mll_stackArea.setOnDragListener(listener);

        //レイアウトからリストビューを取得
        RecyclerView rv_stackArea = (RecyclerView) mRootLayout.findViewById(R.id.rv_stackArea);
        //レイアウトマネージャの生成・設定（横スクロール）
        LinearLayoutManager ll_manager = new LinearLayoutManager(mContext);
        rv_stackArea.setLayoutManager(ll_manager);
        //アダプタの生成・設定
        Log.i("test", "home pre TaskRecyclerAdapter");
        mStackAreaAdapter = new TaskRecyclerAdapter(mContext, R.layout.item_task_for_stack, mStackTask);
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
                        Log.i("test", "onMove " + fromPos + " " + toPos);

                        return true;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                    /*
                    if (_mDeletedTaskPos != NOT_DELETE_WAITING) {
                        //削除待ちのものがあるなら、何もしない
                        return;
                    }
                     */

                        //-- DBから削除
                        int i = viewHolder.getAdapterPosition();
                        //String taskName = taskList.get(i).getTaskName();
                        //int taskTime = taskList.get(i).getTaskTime();

                        //new AsyncTaskTableOperaion(mDB, mTaskListener, AsyncTaskTableOperaion.DB_OPERATION.DELETE, taskName, taskTime).execute();

                        //アイテム削除を通知
                        //_mDeletedTaskPos = viewHolder.getAdapterPosition();

                        //リストから削除
                        mStackTask.remove(i);
                        //ビューにアイテム削除を通知
                        mStackAreaAdapter.notifyItemRemoved(i);
                    }
                }
        );

        //リサイクラービューをアタッチ
        helper.attachToRecyclerView(rv_stackArea);
    }

    /*
     * 「リミット日時」を設定
     */
    private void setDisplayLimitDate(){

        //本日の日付を取得
        Date nowDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd");
        String today = sdf.format(nowDate);

        //日付を設定
        TextView tv_limitDate = (TextView)mRootLayout.findViewById(R.id.tv_limitDate);
        tv_limitDate.setText(today);

        //共通データとして保持
        sdf = new SimpleDateFormat("yyyy/MM/dd");
        String now = sdf.format(nowDate);
        mParentActivity.setLimitDate(now);

        //リスナーを設定
        tv_limitDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //-- カレンダーダイアログの生成
                createCalendarDialog();
            }
        });
    }

    /*
     * 「やること」データを表示エリアにセット
     */
    private void setDisplayTaskData(){

        //やることリストを取得
        List<TaskTable> taskList = mParentActivity.getTaskData();

        //登録があれば
        if( taskList != null && taskList.size() > 0 ){
            //レイアウトからリストビューを取得
            RecyclerView rv_task  = (RecyclerView) mRootLayout.findViewById(R.id.rv_taskList);

            //レイアウトマネージャの生成・設定（横スクロール）
            LinearLayoutManager ll_manager = new LinearLayoutManager(mContext);
            ll_manager.setOrientation(LinearLayoutManager.HORIZONTAL);
            rv_task.setLayoutManager(ll_manager);

            //アダプタの生成・設定
            Log.i("test", "home displayTaskData pre TaskRecyclerAdapter");
            TaskRecyclerAdapter adapter = new TaskRecyclerAdapter(mContext, R.layout.item_task_for_set, taskList);
            Log.i("test", "home displayTaskData TaskRecyclerAdapter");

            //-- ドラッグ&ドロップ リスナー設定
            adapter.setOnItemLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    //ClipData data = ClipData.newPlainText("text", "text");
                    view.startDrag(null, new View.DragShadowBuilder(view), (Object) view, 0);

                    return true;
                }
            });

            rv_task.setAdapter(adapter);
        }

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
                    View dragView = (View) dragEvent.getLocalState();
                    dragView.setBackgroundColor(Color.LTGRAY);
                    //((TextView)dragView).setTextColor(Color.WHITE);
                    return true;
                }
                case DragEvent.ACTION_DRAG_ENTERED: {
                    break;
                }
                case DragEvent.ACTION_DRAG_LOCATION: {
                    break;
                }
                case DragEvent.ACTION_DRAG_EXITED: {
                    break;
                }
                //ドロップ時(ドロップしてドラッグが終了したとき)
                case DragEvent.ACTION_DROP: {
                    Log.i("test", "drop=" + view);
                    /*
                    View dragView = (View) dragEvent.getLocalState();
                    dragView.setBackgroundColor(Color.TRANSPARENT);
                    ((TextView)dragView).setTextColor(Color.BLACK);

                    ((LinearLayout) dragView.getParent()).removeView(dragView);
                    ((LinearLayout) view).addView(dragView);
                    */

                    //ドラッグしたビューからデータを取得
                    View dragView = (View)dragEvent.getLocalState();
                    TextView tv_taskName = dragView.findViewById(R.id.tv_taskName);
                    TextView tv_taskTime = dragView.findViewById(R.id.tv_taskTime);

                    Log.i("test", "drop task=" + tv_taskName.getText());

                    int taskTime = Integer.parseInt( tv_taskTime.getText().toString() );
                    mStackTask.add( 0, new TaskTable( tv_taskName.getText().toString(), taskTime ) );

                    //アダプタへ通知
                    mStackAreaAdapter.notifyDataSetChanged();

                    break;
                }
                //ドラッグ終了時
                case DragEvent.ACTION_DRAG_ENDED: {
                    // ドラッグ終了時
                    //Log.i(getClass().getSimpleName(), "ACTION_DRAG_ENDED");
                    //Log.i("test", "drap end=" + ((TextView)view).getText());

                    View dragView = (View) dragEvent.getLocalState();
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