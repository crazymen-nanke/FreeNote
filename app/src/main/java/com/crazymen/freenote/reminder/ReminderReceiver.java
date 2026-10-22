package com.crazymen.freenote.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 到点响铃：发一条通知，再把下一次排到明天。
 *
 * 用一次性闹钟 + 响后重新注册，避免重复闹钟在用户改时间后仍按旧时间打扰。
 */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ReminderNotifier.showReminder(context);
        ReminderScheduler.schedule(context);
    }
}
