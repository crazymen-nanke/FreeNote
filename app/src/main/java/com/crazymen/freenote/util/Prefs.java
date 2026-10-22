package com.crazymen.freenote.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

/** 轻量偏好：昵称、主题、提醒时间 */
public final class Prefs {

    private static final String FILE = "freenote_prefs";
    private static final String KEY_NAME = "nickname";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_REMIND_ENABLED = "remind_enabled";
    private static final String KEY_REMIND_HOUR = "remind_hour";
    private static final String KEY_REMIND_MINUTE = "remind_minute";
    /** 数据库初始化（种子数据 / 旧 JSON 迁移）是否已完成 */
    private static final String KEY_DATA_READY = "data_ready";

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String name(Context c) {
        return sp(c).getString(KEY_NAME, "随心");
    }

    public static void setName(Context c, String name) {
        sp(c).edit().putString(KEY_NAME, name).apply();
    }

    /** 0 跟随系统 / 1 浅色 / 2 深色 */
    public static int themeMode(Context c) {
        return sp(c).getInt(KEY_THEME, 0);
    }

    public static void setThemeMode(Context c, int mode) {
        sp(c).edit().putInt(KEY_THEME, mode).apply();
    }

    // ------------------------------------------------------------------ 记录提醒

    public static boolean remindEnabled(Context c) {
        return sp(c).getBoolean(KEY_REMIND_ENABLED, false);
    }

    public static void setRemindEnabled(Context c, boolean enabled) {
        sp(c).edit().putBoolean(KEY_REMIND_ENABLED, enabled).apply();
    }

    public static int remindHour(Context c) {
        return sp(c).getInt(KEY_REMIND_HOUR, 21);
    }

    public static int remindMinute(Context c) {
        return sp(c).getInt(KEY_REMIND_MINUTE, 30);
    }

    public static void setRemindTime(Context c, int hour, int minute) {
        sp(c).edit()
                .putInt(KEY_REMIND_HOUR, Math.max(0, Math.min(23, hour)))
                .putInt(KEY_REMIND_MINUTE, Math.max(0, Math.min(59, minute)))
                .apply();
    }

    /** 21:30 */
    public static String remindClock(Context c) {
        return String.format(Locale.CHINA, "%02d:%02d", remindHour(c), remindMinute(c));
    }

    /** 设置页右侧的展示文案：每天 21:30 / 未开启 */
    public static String remindTime(Context c) {
        return remindEnabled(c) ? "每天 " + remindClock(c) : "未开启";
    }

    public static boolean dataReady(Context c) {
        return sp(c).getBoolean(KEY_DATA_READY, false);
    }

    public static void setDataReady(Context c, boolean ready) {
        sp(c).edit().putBoolean(KEY_DATA_READY, ready).apply();
    }
}
