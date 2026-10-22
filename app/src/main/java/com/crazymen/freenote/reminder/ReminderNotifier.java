package com.crazymen.freenote.reminder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.crazymen.freenote.MainActivity;
import com.crazymen.freenote.R;

import java.util.Calendar;

/**
 * 提醒通知：一条轻轻的便签，点开回到「今日」。
 *
 * 文案按一年中的第几天轮换，同一个人不会每天看到完全一样的一句。
 */
public final class ReminderNotifier {

    public static final String CHANNEL_ID = "freenote_daily_reminder";
    public static final int NOTIFICATION_ID = 4101;

    private static final String[] LINES = {
            "今天想记下什么？一句也算数。",
            "此刻有什么在你心里停留？",
            "花一分钟，把今天留住。",
            "天气、心情、一顿饭，都可以写。",
            "不必完整，写一句就好。",
            "今天有值得记下的小事吗？",
            "把刚才想到的那句话写下来吧。"
    };

    private ReminderNotifier() {
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "记录提醒", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("每天到你设定的时间提醒写一句");
        channel.enableVibration(false);
        channel.setShowBadge(true);
        nm.createNotificationChannel(channel);
    }

    public static void showReminder(Context context) {
        ensureChannel(context);
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return;
        }

        String line = LINES[dayOfYear() % LINES.length];
        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent content = PendingIntent.getActivity(context, 0, open, flags);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle("该写一句了")
                .setContentText(line)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(line))
                .setContentIntent(content)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification);
        } catch (SecurityException ignored) {
            // Android 13 上用户关掉了通知权限，静默跳过
        }
    }

    private static int dayOfYear() {
        return Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
    }
}
