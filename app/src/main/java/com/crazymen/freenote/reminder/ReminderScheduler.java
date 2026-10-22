package com.crazymen.freenote.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.crazymen.freenote.util.Prefs;

import java.util.Calendar;

/**
 * 记录提醒的调度：每天固定时刻响一次。
 *
 * 只注册「下一次」而不做重复闹钟，由 {@link ReminderReceiver} 响过之后再续订明天，
 * 这样用户改时间、关开关、重启手机都能自然对齐，不会留下过期的重复闹钟。
 *
 * 用 setAndAllowWhileIdle 而不是精确闹钟：日记提醒差几分钟无所谓，
 * 但精确闹钟在 Android 12+ 需要额外授权，得不偿失。
 */
public final class ReminderScheduler {

    public static final String ACTION_REMIND = "com.crazymen.freenote.action.REMIND";
    private static final int REQUEST_CODE = 20301;

    private ReminderScheduler() {
    }

    /** 开关打开时按偏好时间排一次，否则取消 */
    public static void schedule(Context context) {
        if (!Prefs.remindEnabled(context)) {
            cancel(context);
            return;
        }
        schedule(context, Prefs.remindHour(context), Prefs.remindMinute(context));
    }

    /** 指定时分排下一次 */
    public static void schedule(Context context, int hour, int minute) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        long triggerAt = nextTrigger(hour, minute);
        PendingIntent pi = pendingIntent(context);
        am.cancel(pi);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 打盹模式下也能响，且不需要精确闹钟权限
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static void cancel(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(pendingIntent(context));
        }
    }

    /** 今天这个时刻若已过，就顺延到明天 */
    public static long nextTrigger(int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return c.getTimeInMillis();
    }

    public static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class).setAction(ACTION_REMIND);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags);
    }
}
