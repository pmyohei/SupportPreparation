package com.example.supportpreparation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class AlarmBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("test", "Received");

        // toast で受け取りを確認
        Toast toast = new Toast(context);
        toast.setText("時間がきました");
        toast.show();
    }
}
