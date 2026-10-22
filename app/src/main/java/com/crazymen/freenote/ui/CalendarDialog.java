package com.crazymen.freenote.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.crazymen.freenote.R;
import com.crazymen.freenote.data.NoteStore;
import com.crazymen.freenote.util.Dates;
import com.crazymen.freenote.util.MonthGrid;

import java.util.Calendar;
import java.util.Collections;
import java.util.Set;

/**
 * 「选择日期」月历弹框。
 *
 * 相比周历一周一周地挪，这里一次能看清整月：可逐月、逐年翻页，有记录的日期带小圆点，
 * 点任意一天即回到时光页并筛选那一天。整块沿用纸感卡片 + 茶绿的视觉语言。
 */
public final class CalendarDialog {

    public interface OnPick {
        void onPick(long dayStart);
    }

    private static final int MIN_YEAR = 2000;
    private static final String[] WEEK_HEAD = {"一", "二", "三", "四", "五", "六", "日"};

    private final Context ctx;
    private final Dialog dialog;
    private final OnPick onPick;
    private final long selectedDay;
    private final long todayStart;
    private final int maxMonthIndex;

    private int year;
    private int month;

    private TextView tvMonth;
    private LinearLayout grid;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private ImageButton btnPrevYear;
    private ImageButton btnNextYear;
    private Set<Long> markedDays = Collections.emptySet();

    private CalendarDialog(Context ctx, long anchorDay, long selectedDay, OnPick onPick) {
        this.ctx = ctx;
        this.onPick = onPick;
        this.selectedDay = selectedDay != 0 ? Dates.startOfDay(selectedDay) : 0L;

        Calendar today = Dates.cal(System.currentTimeMillis());
        todayStart = Dates.startOfDay(today.getTimeInMillis());
        maxMonthIndex = MonthGrid.monthIndex(today.get(Calendar.YEAR), today.get(Calendar.MONTH));

        Calendar anchor = Dates.cal(anchorDay != 0 ? anchorDay : System.currentTimeMillis());
        year = anchor.get(Calendar.YEAR);
        month = anchor.get(Calendar.MONTH);

        dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_calendar);
        dialog.setCanceledOnTouchOutside(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.42f);
            window.setWindowAnimations(R.style.FreeNote_DialogAnim);
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            int width = Math.min(dm.widthPixels - dp(36), dp(356));
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        tvMonth = dialog.findViewById(R.id.tvCalMonth);
        grid = dialog.findViewById(R.id.calGrid);
        btnPrevMonth = dialog.findViewById(R.id.btnPrevMonth);
        btnNextMonth = dialog.findViewById(R.id.btnNextMonth);
        btnPrevYear = dialog.findViewById(R.id.btnPrevYear);
        btnNextYear = dialog.findViewById(R.id.btnNextYear);

        buildWeekHeader((LinearLayout) dialog.findViewById(R.id.calWeekRow));

        btnPrevMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shiftMonth(-1);
            }
        });
        btnNextMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shiftMonth(1);
            }
        });
        btnPrevYear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shiftMonth(-12);
            }
        });
        btnNextYear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shiftMonth(12);
            }
        });
        dialog.findViewById(R.id.btnCalCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.btnCalToday).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (CalendarDialog.this.onPick != null) {
                    CalendarDialog.this.onPick.onPick(todayStart);
                }
            }
        });

        renderMonth();
    }

    /** 打开月历；anchorDay 决定初始月份，selectedDay 为当前选中日（0 表示未选） */
    public static Dialog show(Context ctx, long anchorDay, long selectedDay, OnPick onPick) {
        CalendarDialog d = new CalendarDialog(ctx, anchorDay, selectedDay, onPick);
        d.dialog.show();
        return d.dialog;
    }

    // ------------------------------------------------------------------ 月历

    private void buildWeekHeader(LinearLayout row) {
        row.removeAllViews();
        for (int i = 0; i < 7; i++) {
            TextView tv = new TextView(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, dp(28), 1f);
            tv.setLayoutParams(lp);
            tv.setGravity(Gravity.CENTER);
            tv.setText(WEEK_HEAD[i]);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.text_quaternary));
            row.addView(tv);
        }
    }

    private void renderMonth() {
        tvMonth.setText(Dates.monthLabel(year, month));
        markedDays = NoteStore.get(ctx).daysWithNotes(year, month);

        grid.removeAllViews();
        MonthGrid.Cell[] cells = MonthGrid.build(year, month);
        for (int r = 0; r < MonthGrid.ROWS; r++) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            for (int c = 0; c < MonthGrid.COLS; c++) {
                row.addView(buildCell(cells[r * MonthGrid.COLS + c]));
            }
            grid.addView(row);
        }

        int index = MonthGrid.monthIndex(year, month);
        boolean canPrev = index > MonthGrid.monthIndex(MIN_YEAR, 0);
        boolean canNext = index < maxMonthIndex;
        tintNav(btnPrevMonth, canPrev);
        tintNav(btnPrevYear, canPrev);
        tintNav(btnNextMonth, canNext);
        tintNav(btnNextYear, canNext);
    }

    private void tintNav(ImageButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.26f);
    }

    private View buildCell(final MonthGrid.Cell cell) {
        final boolean active = selectedDay != 0L && cell.day == selectedDay;
        boolean today = cell.day == todayStart;
        boolean future = cell.day > todayStart;

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, dp(46), 1f));
        col.setTag(R.id.tag_day, cell.day);

        FrameLayout circle = new FrameLayout(ctx);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(32), dp(32));
        clp.topMargin = dp(2);
        circle.setLayoutParams(clp);
        if (active) {
            circle.setBackgroundResource(R.drawable.bg_week_today);
        } else if (today) {
            circle.setBackgroundResource(R.drawable.bg_day_outline);
        }

        TextView num = new TextView(ctx);
        num.setText(String.valueOf(Dates.cal(cell.day).get(Calendar.DAY_OF_MONTH)));
        num.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        num.setGravity(Gravity.CENTER);
        num.setTypeface(Typeface.create(active ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        int color;
        if (active) {
            color = R.color.on_primary;
        } else if (today) {
            color = R.color.primary;
        } else if (!cell.inMonth) {
            color = R.color.text_ghost;
        } else {
            color = R.color.ink_body;
        }
        num.setTextColor(ContextCompat.getColor(ctx, color));
        FrameLayout.LayoutParams nlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        num.setLayoutParams(nlp);
        circle.addView(num);
        col.addView(circle);

        View dot = new View(ctx);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(4), dp(4));
        dlp.topMargin = dp(3);
        dot.setLayoutParams(dlp);
        dot.setBackgroundResource(R.drawable.bg_week_today);
        dot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx,
                active ? R.color.on_primary : R.color.primary_light)));
        dot.setAlpha(markedDays.contains(cell.day) ? 1f : 0f);
        col.addView(dot);

        if (future) {
            // 未来的日子不会有记录，弱化且不可点
            col.setAlpha(0.42f);
            col.setClickable(false);
        } else {
            col.setClickable(true);
            col.setFocusable(true);
            Drawable ripple = selectableBackground();
            if (ripple != null) {
                col.setBackground(ripple);
            }
            col.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    if (onPick != null) {
                        onPick.onPick(cell.day);
                    }
                }
            });
        }
        return col;
    }

    private void shiftMonth(int delta) {
        int target = MonthGrid.monthIndex(year, month) + delta;
        if (target < MonthGrid.monthIndex(MIN_YEAR, 0) || target > maxMonthIndex) {
            return;
        }
        year = target / 12;
        month = target % 12;
        renderMonth();
    }

    // ------------------------------------------------------------------ 工具

    private Drawable selectableBackground() {
        TypedArray ta = ctx.obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackgroundBorderless});
        try {
            return ta.getDrawable(0);
        } finally {
            ta.recycle();
        }
    }

    private int dp(int value) {
        return Math.round(ctx.getResources().getDisplayMetrics().density * value);
    }
}
