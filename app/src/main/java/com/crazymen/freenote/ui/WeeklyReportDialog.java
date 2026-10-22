package com.crazymen.freenote.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.crazymen.freenote.R;
import com.crazymen.freenote.data.NoteStore;
import com.crazymen.freenote.util.Dates;
import com.crazymen.freenote.util.Encouragements;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

/**
 * 本周回顾弹窗：用真实数据讲清「这一周写了什么」，再给一句鼓励。
 *
 * 交互：点柱子看某天条数、「换一句」切鼓励语、「写一条 / 看看这周」直接行动。
 */
public final class WeeklyReportDialog {

    public interface OnAction {
        void onWrite();

        void onBrowse();
    }

    private WeeklyReportDialog() {
    }

    /** 上一次展示过的鼓励语，下次打开时避开，保证「每次都不一样」 */
    private static String lastLine;

    public static Dialog show(Context ctx, final OnAction action) {
        final Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_weekly_report);
        dialog.setCanceledOnTouchOutside(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.42f);
            window.setWindowAnimations(R.style.FreeNote_DialogAnim);
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            window.setLayout(Math.min(dm.widthPixels - dp(ctx, 36), dp(ctx, 380)),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }

        NoteStore store = NoteStore.get(ctx);
        long today = Dates.startOfDay(System.currentTimeMillis());
        Calendar c = Dates.cal(today);
        c.add(Calendar.DAY_OF_MONTH, -6);
        long start = c.getTimeInMillis();

        int[] counts = store.dailyCounts(start, 7);
        int notes = 0;
        int days = 0;
        for (int n : counts) {
            notes += n;
            if (n > 0) {
                days++;
            }
        }
        int words = store.wordsSince(start);

        ((TextView) dialog.findViewById(R.id.tvReportRange)).setText(String.format(Locale.CHINA,
                "%s – %s", Dates.monthDay(start), Dates.monthDay(today)));
        ((TextView) dialog.findViewById(R.id.tvReportNotes)).setText(String.valueOf(notes));
        ((TextView) dialog.findViewById(R.id.tvReportDays)).setText(String.valueOf(days));
        ((TextView) dialog.findViewById(R.id.tvReportWords)).setText(
                String.format(Locale.CHINA, "%,d", words));

        buildChart(ctx, (LinearLayout) dialog.findViewById(R.id.weekChart),
                (TextView) dialog.findViewById(R.id.tvChartHint), start, counts);
        buildInsights(ctx, (LinearLayout) dialog.findViewById(R.id.reportInsights),
                store, start, counts, notes, days, words);

        // 100 条文案里随机取一条，并避开上一次打开时显示的那句
        final int dayCount = days;
        final TextView tvEncourage = dialog.findViewById(R.id.tvEncourage);
        String line = Encouragements.random(dayCount, lastLine);
        lastLine = line;
        tvEncourage.setText(line);
        dialog.findViewById(R.id.btnAnotherLine).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String next = Encouragements.next(dayCount, tvEncourage.getText().toString());
                lastLine = next;
                tvEncourage.setText(next);
                tvEncourage.setAlpha(0f);
                tvEncourage.setTranslationY(dp(v.getContext(), 4));
                tvEncourage.animate().alpha(1f).translationY(0f).setDuration(180).start();
            }
        });

        dialog.findViewById(R.id.btnReportWrite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (action != null) {
                    action.onWrite();
                }
            }
        });
        dialog.findViewById(R.id.btnReportBrowse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (action != null) {
                    action.onBrowse();
                }
            }
        });

        dialog.show();
        return dialog;
    }

    // ------------------------------------------------------------------ 图表

    private static void buildChart(Context ctx, LinearLayout chart, final TextView hint,
                                   long start, int[] counts) {
        chart.removeAllViews();
        int max = 1;
        for (int n : counts) {
            max = Math.max(max, n);
        }

        Calendar c = Dates.cal(start);
        for (int i = 0; i < counts.length; i++) {
            final long day = c.getTimeInMillis();
            final int count = counts[i];
            c.add(Calendar.DAY_OF_MONTH, 1);

            final boolean isToday = Dates.isToday(day);
            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            col.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            col.setPadding(0, dp(ctx, 2), 0, 0);

            TextView number = new TextView(ctx);
            number.setText(count == 0 ? "" : String.valueOf(count));
            number.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
            number.setGravity(Gravity.CENTER);
            number.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            number.setTextColor(ContextCompat.getColor(ctx,
                    isToday ? R.color.primary : R.color.text_quaternary));
            number.setHeight(dp(ctx, 14));
            col.addView(number);

            View bar = new View(ctx);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(ctx, 16),
                    count == 0 ? dp(ctx, 3)
                            : Math.max(dp(ctx, 7), Math.round(dp(ctx, 44) * count / (float) max)));
            blp.topMargin = dp(ctx, 3);
            bar.setLayoutParams(blp);
            bar.setBackgroundResource(R.drawable.bg_chart_bar);
            bar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx,
                    count == 0 ? R.color.stroke
                            : isToday ? R.color.primary : R.color.primary_light)));
            col.addView(bar);

            TextView week = new TextView(ctx);
            week.setText(Dates.weekShort(day));
            week.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
            week.setGravity(Gravity.CENTER);
            week.setTypeface(Typeface.create(isToday ? "sans-serif-medium" : "sans-serif",
                    Typeface.NORMAL));
            week.setTextColor(ContextCompat.getColor(ctx,
                    isToday ? R.color.primary : R.color.text_ghost));
            LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            wlp.topMargin = dp(ctx, 5);
            week.setLayoutParams(wlp);
            col.addView(week);

            col.setClickable(true);
            col.setFocusable(true);
            col.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hint.setText(String.format(Locale.CHINA, "%s · %s · %d 条",
                            Dates.monthDay(day), Dates.weekName(day), count));
                }
            });
            chart.addView(col);
        }
    }

    // ------------------------------------------------------------------ 洞察

    private static void buildInsights(Context ctx, LinearLayout box, NoteStore store, long start,
                                      int[] counts, int notes, int days, int words) {
        box.removeAllViews();
        if (notes == 0) {
            addInsight(ctx, box, "这 7 天还没有记录，从今天写一句也可以。");
            return;
        }

        addInsight(ctx, box, String.format(Locale.CHINA,
                "这 7 天有 %d 天写下东西，一共 %d 条", days, notes));

        int best = 0;
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] > counts[best]) {
                best = i;
            }
        }
        if (counts[best] > 0) {
            Calendar c = Dates.cal(start);
            c.add(Calendar.DAY_OF_MONTH, best);
            addInsight(ctx, box, String.format(Locale.CHINA, "%s写得最多，%d 条",
                    Dates.weekName(c.getTimeInMillis()), counts[best]));
        }

        Map<String, Integer> tags = store.topTags(start, 2);
        if (!tags.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e : tags.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("、");
                }
                sb.append(e.getKey());
            }
            addInsight(ctx, box, "常写的标签：" + sb);
        }

        if (notes > 0) {
            addInsight(ctx, box, String.format(Locale.CHINA, "平均每条 %d 字", words / notes));
        }
    }

    private static void addInsight(Context ctx, LinearLayout box, String text) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 7);
        row.setLayoutParams(lp);

        View dot = new View(ctx);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(ctx, 5), dp(ctx, 5));
        dlp.setMarginEnd(dp(ctx, 8));
        dot.setLayoutParams(dlp);
        dot.setBackgroundResource(R.drawable.bg_week_today);
        dot.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(ctx, R.color.primary_light)));
        row.addView(dot);

        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tv.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary));
        tv.setLineSpacing(dp(ctx, 2), 1f);
        row.addView(tv);
        box.addView(row);
    }

    // ------------------------------------------------------------------ 文案

    private static int dp(Context ctx, int value) {
        return Math.round(ctx.getResources().getDisplayMetrics().density * value);
    }
}
