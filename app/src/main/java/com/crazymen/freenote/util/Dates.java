package com.crazymen.freenote.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** 日期与文案格式化（全部走 Calendar，兼容 minSdk 24） */
public final class Dates {

    private static final String[] WEEK_LONG = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};

    private Dates() {
    }

    public static Calendar cal(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        return c;
    }

    public static long startOfDay(long ts) {
        Calendar c = cal(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static String dayLabel(long ts) {
        Calendar c = cal(ts);
        return String.format(Locale.CHINA, "%d月%d日 · %s",
                c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), WEEK_LONG[c.get(Calendar.DAY_OF_WEEK) - 1]);
    }

    /** 分组标题：10月21日 · 周二 */
    public static String shortDayLabel(long ts) {
        Calendar c = cal(ts);
        String[] names = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        return String.format(Locale.CHINA, "%d月%d日 · %s",
                c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), names[c.get(Calendar.DAY_OF_WEEK) - 1]);
    }

    public static String monthLabel(int year, int monthZeroBased) {
        return String.format(Locale.CHINA, "%d年%d月", year, monthZeroBased + 1);
    }

    /** 10月16日（不带星期，用于日期范围） */
    public static String monthDay(long ts) {
        Calendar c = cal(ts);
        return String.format(Locale.CHINA, "%d月%d日", c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    /** 周几：周一 … 周日 */
    public static String weekName(long ts) {
        Calendar c = cal(ts);
        String[] names = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        return names[c.get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static String time(long ts) {
        Calendar c = cal(ts);
        return String.format(Locale.CHINA, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    public static String fullTime(long ts) {
        return dayLabel(ts).replace(" · ", " ") + " · " + time(ts);
    }

    /** 周历里的星期，单字 */
    public static String weekShort(long ts) {
        Calendar c = cal(ts);
        int d = c.get(Calendar.DAY_OF_WEEK); // 1=周日
        String[] names = {"日", "一", "二", "三", "四", "五", "六"};
        return names[d - 1];
    }

    /** 以周一为一周起点，返回该周的 7 天（0:00） */
    public static long[] weekOf(long ts) {
        Calendar c = cal(startOfDay(ts));
        int dow = c.get(Calendar.DAY_OF_WEEK); // 1=周日 ... 2=周一
        int back = dow == Calendar.SUNDAY ? 6 : dow - Calendar.MONDAY;
        c.add(Calendar.DAY_OF_MONTH, -back);
        long[] days = new long[7];
        for (int i = 0; i < 7; i++) {
            days[i] = c.getTimeInMillis();
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    public static boolean isSameDay(long a, long b) {
        return startOfDay(a) == startOfDay(b);
    }

    public static boolean isToday(long ts) {
        return isSameDay(ts, System.currentTimeMillis());
    }

    /** 早/中/晚问候 */
    public static String greeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h < 5) {
            return "夜深了";
        } else if (h < 11) {
            return "早上好";
        } else if (h < 14) {
            return "中午好";
        } else if (h < 18) {
            return "下午好";
        } else if (h < 23) {
            return "晚上好";
        }
        return "夜深了";
    }

    public static String monthCountText(int count, int words) {
        return String.format(Locale.CHINA, "%d 条 · %d 字", count, words);
    }

    @SuppressWarnings("unused")
    public static String simpleDate(Date date) {
        return dayLabel(date.getTime());
    }
}
