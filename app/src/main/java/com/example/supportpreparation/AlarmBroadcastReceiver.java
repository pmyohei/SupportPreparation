package com.example.supportpreparation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;


/*
 * アラームタイマ満了時のReceiver
 */
public class AlarmBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("test", "Received");

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {

            String channelId = "default";
            String title = context.getString(R.string.app_name);
            String message = intent.getExtras().getString(ResourceManager.NOTIFY_SEND_KEY);

            Log.i("test", "Received message=" + message);

            //サウンド
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            //APIレベル対応（通知チャネルは26から）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //通知チャネル設定
                NotificationChannel channel = new NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_HIGH);

                channel.setDescription(message);
                channel.enableVibration(true);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                channel.setSound(defaultSoundUri, null);
                //channel.setShowBadge(false);
                //channel.canShowBadge();
                //channel.enableLights(true);
                //channel.setLightColor(Color.BLUE);

                //通知チャネル生成
                notificationManager.createNotificationChannel(channel);
            }

            //通知ビルダー
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
            builder.setContentTitle(title)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentText(message)
                    .setAutoCancel(true);                    //ユーザーがこの通知に触れると、この通知が自動的に閉じられる
                    //.setContentIntent(pendingIntent)
                    //.setWhen(System.currentTimeMillis())

            //APIレベル対応（ヘッドアップレイアウトの設定は24から）
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ){

                //ヘッドアップレイアウト
                RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.notification_head_up);
                rv.setTextViewText(R.id.tv_appName, title);
                rv.setTextViewText(R.id.tv_message, message);

                //ヘッドアップレイアウトの設定
                builder.setCustomHeadsUpContentView(rv);
            }

            //通知
            notificationManager.notify(R.string.app_name, builder.build());
        }
    }
}
