package com.example.supportpreparation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.supportpreparation.ui.stackManager.StackManagerFragment;

import java.text.SimpleDateFormat;
import java.util.Locale;

/*
 * アラームタイマ満了時のReceiver
 */
public class AlarmBroadcastReceiver extends BroadcastReceiver {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("test", "Received");

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {

            String channelId = "default";
            String title = context.getString(R.string.app_name);
            String message = intent.getExtras().getString( ResourceManager.NOTIFY_SEND_KEY );

            Log.i("test", "Received message=" + message );

            //サウンド
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            // Notification　Channel 設定
            NotificationChannel channel = new NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_DEFAULT);

            channel.setDescription(message);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            channel.setSound(defaultSoundUri, null);
            //channel.setShowBadge(true);
            //channel.canShowBadge();
            //channel.enableLights(true);
            //channel.setLightColor(Color.BLUE);

            //通知チャネル生成
            notificationManager.createNotificationChannel(channel);

            //通知ビルダー
            Notification notification = new Notification.Builder(context, channelId)
                    .setContentTitle(title)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentText(message)
                    .setAutoCancel(true)                    //ユーザーがこの通知に触れると、この通知が自動的に閉じられる
                    //.setContentIntent(pendingIntent)
                    //.setWhen(System.currentTimeMillis())
                    .build();

            // 通知
            notificationManager.notify(R.string.app_name, notification);
        }
    }
}
