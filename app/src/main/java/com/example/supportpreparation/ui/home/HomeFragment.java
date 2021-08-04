package com.example.supportpreparation.ui.home;

import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.supportpreparation.AppDatabase;
import com.example.supportpreparation.AppDatabaseSingleton;
import com.example.supportpreparation.AsyncSetTableOperaion;
import com.example.supportpreparation.CreateSetDialog;
import com.example.supportpreparation.R;
import com.example.supportpreparation.TaskManageActivity;
import com.example.supportpreparation.databinding.FragmentHomeBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;

public class HomeFragment extends Fragment {

    private View                    mRootLayout;                //本フラグメントに設定しているレイアウト
    private Fragment                mFragment;                  //本フラグメント
    private Context                 mContext;                   //コンテキスト（親アクティビティ）
    private AppDatabase             mDB;                        //DB



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

        //-- test
        TextView drop = mRootLayout.findViewById(R.id.tv_drop);
        TextView drag = mRootLayout.findViewById(R.id.tv_drag);



        //-- test

        // FloatingActionButton
        FloatingActionButton fab = (FloatingActionButton) mRootLayout.findViewById(R.id.fab_startSupport);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //-- 時刻設定ダイアログの生成
                createInputTimeDialog();
            }
        });

        return mRootLayout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /*
     * タスク生成ダイアログの生成
     */
    private void createInputTimeDialog(){

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        //タイムピッカーダイアログの表示
        TimePickerDialog dialog = new TimePickerDialog(
                mContext,
                new TimePickerDialog.OnTimeSetListener(){
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        Log.i("test", String.format("%02d:%02d", hourOfDay,minute) );
                    }
                },
                hour,minute,true);
        dialog.show();
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


}