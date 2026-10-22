package com.crazymen.freenote.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.crazymen.freenote.R;

/**
 * 与「随心记」设计语言一致的弹框：纸感圆角卡片 + 衬线标题 + 茶绿主按钮。
 * 替代系统 AlertDialog，避免 Material 默认样式与整体调性冲突。
 */
public final class AppDialog {

    public interface OnConfirm {
        void run();
    }

    public interface OnChoose {
        void onChoose(int index);
    }

    public interface OnText {
        void onText(String text);
    }

    private interface Action {
        void run();
    }

    private AppDialog() {
    }

    // ------------------------------------------------------------------ 对外

    /** 单按钮提示 */
    public static Dialog info(Context ctx, String title, String message) {
        final Panel p = panel(ctx, title, message);
        addButton(p, "好", Style.PRIMARY, new Action() {
            @Override
            public void run() {
                p.dialog.dismiss();
            }
        });
        return finish(p);
    }

    /** 二次确认，danger = true 时主按钮用陶土色 */
    public static Dialog confirm(Context ctx, String title, String message, String positive,
                                 boolean danger, final OnConfirm onConfirm) {
        final Panel p = panel(ctx, title, message);
        addButton(p, "取消", Style.SECONDARY, new Action() {
            @Override
            public void run() {
                p.dialog.dismiss();
            }
        });
        addButton(p, positive, danger ? Style.DANGER : Style.PRIMARY, new Action() {
            @Override
            public void run() {
                p.dialog.dismiss();
                if (onConfirm != null) {
                    onConfirm.run();
                }
            }
        });
        return finish(p);
    }

    /** 单选列表，selectedIndex < 0 表示无选中项 */
    public static Dialog list(Context ctx, String title, String[] items, int selectedIndex,
                              final OnChoose onChoose) {
        final Panel p = panel(ctx, title, null);
        p.container.setVisibility(View.VISIBLE);
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            if (i > 0) {
                p.container.addView(divider(ctx));
            }
            p.container.addView(row(ctx, items[i], i == selectedIndex, false, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    p.dialog.dismiss();
                    if (onChoose != null) {
                        onChoose.onChoose(index);
                    }
                }
            }));
        }
        addButton(p, "取消", Style.SECONDARY, new Action() {
            @Override
            public void run() {
                p.dialog.dismiss();
            }
        });
        return finish(p);
    }

    /** 动作列表（无选中态），dangerIndex 指定用警示色的行 */
    public static Dialog actions(Context ctx, String title, String[] items, int dangerIndex,
                                 final OnChoose onChoose) {
        final Panel p = panel(ctx, title, null);
        p.container.setVisibility(View.VISIBLE);
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            if (i > 0) {
                p.container.addView(divider(ctx));
            }
            p.container.addView(row(ctx, items[i], false, i == dangerIndex, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    p.dialog.dismiss();
                    if (onChoose != null) {
                        onChoose.onChoose(index);
                    }
                }
            }));
        }
        addButton(p, "取消", Style.SECONDARY, new Action() {
            @Override
            public void run() {
                p.dialog.dismiss();
            }
        });
        return finish(p);
    }

    /** 单行文本输入 */
    public static Dialog input(Context ctx, String title, String message, String hint,
                               String initial, String positive, final OnText onText) {
        final Panel p = panel(ctx, title, message);

        final EditText input = new EditText(ctx);
        input.setBackgroundResource(R.drawable.bg_input);
        input.setHint(hint == null ? "" : hint);
        input.setText(initial == null ? "" : initial);
        input.setSelection(input.getText().length());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextSize(15f);
        input.setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12));
        input.setTextColor(ContextCompat.getColor(ctx, R.color.ink));
        input.setHintTextColor(ContextCompat.getColor(ctx, R.color.text_ghost));
        input.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        p.container.addView(input);
        p.container.setVisibility(View.VISIBLE);

        addButton(p, "取消", Style.SECONDARY, new Action() {
            @Override
            public void run() {
                p.dialog.dismiss();
            }
        });
        addButton(p, positive, Style.PRIMARY, new Action() {
            @Override
            public void run() {
                String text = input.getText().toString().trim();
                if (text.length() == 0) {
                    input.requestFocus();
                    return;
                }
                p.dialog.dismiss();
                if (onText != null) {
                    onText.onText(text);
                }
            }
        });
        finish(p);

        input.requestFocus();
        Window window = p.dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return p.dialog;
    }

    // ------------------------------------------------------------------ 内部

    private enum Style {
        PRIMARY, SECONDARY, DANGER
    }

    private static final class Panel {
        Dialog dialog;
        TextView title;
        TextView message;
        LinearLayout container;
        LinearLayout buttons;
    }

    private static Dialog finish(Panel p) {
        p.buttons.setVisibility(View.VISIBLE);
        p.dialog.show();
        return p.dialog;
    }

    private static Panel panel(Context ctx, String title, String message) {
        final Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_base);
        dialog.setCanceledOnTouchOutside(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.42f);
            window.setWindowAnimations(R.style.FreeNote_DialogAnim);
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            int max = dp(ctx, 440);
            int width = Math.min(dm.widthPixels - dp(ctx, 40), max);
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        Panel p = new Panel();
        p.dialog = dialog;
        p.title = dialog.findViewById(R.id.tvDialogTitle);
        p.message = dialog.findViewById(R.id.tvDialogMessage);
        p.container = dialog.findViewById(R.id.dialogContainer);
        p.buttons = dialog.findViewById(R.id.dialogButtons);

        p.title.setText(title);
        if (message == null || message.trim().length() == 0) {
            p.message.setVisibility(View.GONE);
        } else {
            p.message.setText(message);
            p.message.setVisibility(View.VISIBLE);
        }
        return p;
    }

    /** 按钮：两个时左右等宽；只有一个时占满整行 */
    private static void addButton(final Panel p, String label, Style style, final Action action) {
        Context c = p.dialog.getContext();
        TextView tv = new TextView(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(c, 44), 1f);
        if (p.buttons.getChildCount() > 0) {
            lp.setMarginStart(dp(c, 10));
        }
        tv.setLayoutParams(lp);
        tv.setId(style == Style.SECONDARY ? R.id.dialog_cancel : R.id.dialog_positive);
        tv.setText(label);
        tv.setTextSize(14f);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setClickable(true);
        tv.setFocusable(true);
        if (style == Style.PRIMARY) {
            tv.setBackgroundResource(R.drawable.bg_btn_primary);
            tv.setTextColor(ContextCompat.getColor(c, R.color.on_primary));
        } else if (style == Style.DANGER) {
            tv.setBackgroundResource(R.drawable.bg_btn_danger);
            tv.setTextColor(ContextCompat.getColor(c, R.color.on_danger));
        } else {
            tv.setBackgroundResource(R.drawable.bg_btn_secondary);
            tv.setTextColor(ContextCompat.getColor(c, R.color.text_secondary));
        }
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
        p.buttons.addView(tv);
    }

    private static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(ContextCompat.getColor(c, R.color.divider));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 1));
        lp.topMargin = dp(c, 4);
        lp.bottomMargin = dp(c, 4);
        v.setLayoutParams(lp);
        return v;
    }

    private static View row(Context c, String label, boolean selected, boolean danger,
                            View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(c, 50));
        row.setPadding(dp(c, 2), dp(c, 8), dp(c, 2), dp(c, 8));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        Drawable ripple = selectableBackground(c);
        if (ripple != null) {
            row.setBackground(ripple);
        }
        row.setClickable(true);
        row.setFocusable(true);

        TextView tv = new TextView(c);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tv.setText(label);
        tv.setTextSize(15f);
        tv.setTextColor(ContextCompat.getColor(c,
                danger ? R.color.danger : (selected ? R.color.primary : R.color.ink_body)));
        tv.setTypeface(Typeface.create(selected ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        row.addView(tv);

        if (selected) {
            ImageView check = new ImageView(c);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(c, 17), dp(c, 17));
            ilp.setMarginStart(dp(c, 10));
            check.setLayoutParams(ilp);
            check.setImageResource(R.drawable.ic_check_circle);
            check.setColorFilter(ContextCompat.getColor(c, R.color.primary));
            row.addView(check);
        }

        row.setOnClickListener(onClick);
        return row;
    }

    private static Drawable selectableBackground(Context c) {
        TypedArray ta = c.obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
        try {
            return ta.getDrawable(0);
        } finally {
            ta.recycle();
        }
    }

    private static int dp(Context c, int value) {
        return Math.round(c.getResources().getDisplayMetrics().density * value);
    }
}
