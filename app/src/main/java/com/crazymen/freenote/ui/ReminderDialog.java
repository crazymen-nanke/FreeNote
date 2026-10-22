package com.crazymen.freenote.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.NumberPicker;
import android.widget.Switch;

import com.crazymen.freenote.R;
import com.crazymen.freenote.reminder.ReminderNotifier;
import com.crazymen.freenote.reminder.ReminderScheduler;
import com.crazymen.freenote.util.Prefs;

import java.util.Locale;

/**
 * 记录提醒设置：一个纸感的时分选择器 + 开关。
 *
 * 分钟按 5 分钟一档——日记提醒不需要精确到分，档位少一点反而好选。
 */
public final class ReminderDialog {

    public interface OnSaved {
        void onSaved(boolean enabled, int hour, int minute);
    }

    private static final int MINUTE_STEP = 5;

    private ReminderDialog() {
    }

    public static Dialog show(Context ctx, final OnSaved onSaved) {
        final Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_reminder);
        dialog.setCanceledOnTouchOutside(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.42f);
            window.setWindowAnimations(R.style.FreeNote_DialogAnim);
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            window.setLayout(Math.min(dm.widthPixels - dp(ctx, 36), dp(ctx, 360)),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }

        final NumberPicker npHour = dialog.findViewById(R.id.npHour);
        final NumberPicker npMinute = dialog.findViewById(R.id.npMinute);
        final Switch swRemind = dialog.findViewById(R.id.swRemind);

        npHour.setMinValue(0);
        npHour.setMaxValue(23);
        npHour.setValue(Prefs.remindHour(ctx));
        npHour.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format(Locale.CHINA, "%02d", value);
            }
        });

        npMinute.setMinValue(0);
        npMinute.setMaxValue(59 / MINUTE_STEP);
        npMinute.setValue(Math.min(59 / MINUTE_STEP, Prefs.remindMinute(ctx) / MINUTE_STEP));
        npMinute.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format(Locale.CHINA, "%02d", value * MINUTE_STEP);
            }
        });

        swRemind.setChecked(Prefs.remindEnabled(ctx));

        dialog.findViewById(R.id.btnRemindCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.btnRemindSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = npHour.getValue();
                int minute = npMinute.getValue() * MINUTE_STEP;
                boolean enabled = swRemind.isChecked();

                Prefs.setRemindTime(v.getContext(), hour, minute);
                Prefs.setRemindEnabled(v.getContext(), enabled);
                if (enabled) {
                    // 顺手把通知通道建出来：Android 13 会在这一步询问通知权限，时机正好
                    ReminderNotifier.ensureChannel(v.getContext());
                    ReminderScheduler.schedule(v.getContext(), hour, minute);
                } else {
                    ReminderScheduler.cancel(v.getContext());
                }

                dialog.dismiss();
                if (onSaved != null) {
                    onSaved.onSaved(enabled, hour, minute);
                }
            }
        });

        dialog.show();
        return dialog;
    }

    private static int dp(Context ctx, int value) {
        return Math.round(ctx.getResources().getDisplayMetrics().density * value);
    }
}
