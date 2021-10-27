package com.example.supportpreparation;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/*
 * RecyclerViewスクロールListener
 * 　指定されたfabを、スクロール中非表示にする。
 */
public class SelectAreaScrollListener extends RecyclerView.OnScrollListener {

    //フィールド変数
    private ViewGroup               mViewGroup;         //非表示対象のfabの親ビュー
    private FloatingActionButton    mFab;               //非表示対象のfab

    /*
     * コンストラクタ
     */
    public SelectAreaScrollListener(FloatingActionButton fab){
        mFab = fab;
    }

    /*
     * コンストラクタ
     */
    public SelectAreaScrollListener(ViewGroup ll){
        mViewGroup = ll;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
        //スクロール量が一定量を超えたとき、fabを非表示にする
        int absDx = Math.abs(dx);

        //非表示スクロール量
        final int FAB_HIDE_SCROLL_DX = 12;

        if (absDx >= FAB_HIDE_SCROLL_DX) {

            if( mFab == null ){

                for (int i = 0; i < mViewGroup.getChildCount(); i++) {
                    //子ビューを取得
                    View v = mViewGroup.getChildAt(i);

                    if (v instanceof FloatingActionButton) {

                        FloatingActionButton fab = (FloatingActionButton) v;

                        //表示されていないなら、対象外
                        if( fab.getVisibility() == View.GONE ){
                            continue;
                        }

                        //非表示
                        fab.hide();
                    }
                }

            } else {
                mFab.hide();
            }
        }
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState){
        //スクロールが完全に停止したとき、fabを表示
        if( newState == SCROLL_STATE_IDLE ){

            if( mFab == null ){

                for (int i = 0; i < mViewGroup.getChildCount(); i++) {
                    //子ビューを取得
                    View v = mViewGroup.getChildAt(i);

                    if (v instanceof FloatingActionButton) {

                        FloatingActionButton fab = (FloatingActionButton) v;

                        //表示されていないなら、対象外
                        if( fab.getVisibility() == View.GONE ){
                            continue;
                        }

                        fab.show();
                    }
                }

            } else {
                mFab.show();
            }
        }
    }
}

