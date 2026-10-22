package com.crazymen.freenote;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.crazymen.freenote.reminder.ReminderScheduler;
import com.crazymen.freenote.util.Prefs;

public class FreeNoteApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        switch (Prefs.themeMode(this)) {
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
        // 应用被强行停止后系统会清掉闹钟，每次启动补排一次（未开启时内部会直接取消）
        ReminderScheduler.schedule(this);
    }
}
