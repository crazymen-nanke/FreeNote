package com.crazymen.freenote.util;

import java.util.Calendar;

/**
 * 月历网格：把某个月摊成 6 行 × 7 列（周一开头），首尾补上相邻月份的日期。
 *
 * 纯计算、不依赖 Android，便于单元测试；月历弹框直接消费这个结果。
 */
public final class MonthGrid {

    public static final int COLS = 7;
    public static final int ROWS = 6;

    private MonthGrid() {
    }

    /** 格子：day 为当天 0:00 的时间戳，inMonth 标记是否属于当前月 */
    public static final class Cell {
        public final long day;
        public final boolean inMonth;

        Cell(long day, boolean inMonth) {
            this.day = day;
            this.inMonth = inMonth;
        }
    }

    public static Cell[] build(int year, int monthZeroBased) {
        Calendar c = Calendar.getInstance();
        c.set(year, monthZeroBased, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);

        int dow = c.get(Calendar.DAY_OF_WEEK);           // 1 = 周日
        int back = dow == Calendar.SUNDAY ? 6 : dow - Calendar.MONDAY;  // 距本周一几天
        c.add(Calendar.DAY_OF_MONTH, -back);

        Cell[] cells = new Cell[COLS * ROWS];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new Cell(c.getTimeInMillis(), c.get(Calendar.MONTH) == monthZeroBased);
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return cells;
    }

    /** 月份序号（年 * 12 + 月），用于比较大小 */
    public static int monthIndex(int year, int monthZeroBased) {
        return year * 12 + monthZeroBased;
    }
}
