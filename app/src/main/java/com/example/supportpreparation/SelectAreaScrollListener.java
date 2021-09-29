package com.example.supportpreparation;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/*
 * RecyclerViewスクロールListener
 * 　指定されたfabを、スクロール中非表示にする。
 */
public class SelectAreaScrollListener extends RecyclerView.OnScrollListener {

    private final int FAB_HIDE_SCROLL_DX   = 12;        //非表示スクロール量

    private FloatingActionButton mFab;                  //非表示対象のfab

    /*
     * コンストラクタ
     */
    public SelectAreaScrollListener(FloatingActionButton fab){
        mFab = fab;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
        //スクロール量が一定量を超えたとき、fabを非表示にする
        int absDx = Math.abs(dx);
        if (absDx >= FAB_HIDE_SCROLL_DX) {
            mFab.hide();
        }

        Log.d("test", "dx=" + dx);
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState){
        //スクロールが完全に停止したとき、fabを表示
        if( newState == SCROLL_STATE_IDLE ){
            mFab.show();
        }

        Log.d("test", "newState=" + newState);
    }
}

