package com.crazymen.freenote.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.crazymen.freenote.MainActivity;
import com.crazymen.freenote.R;
import com.crazymen.freenote.data.NoteStore;
import com.crazymen.freenote.model.Note;
import com.crazymen.freenote.util.AvatarStore;
import com.crazymen.freenote.util.Dates;
import com.crazymen.freenote.util.Prefs;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 04 我的：习惯与设置 */
public class ProfileFragment extends Fragment {

    private TextView tvName;
    private TextView tvStats;
    private TextView tvReview;
    private LinearLayout tagContainer;
    private LinearLayout settingsList;
    private ImageView avatar;

    private ActivityResultLauncher<String> avatarPicker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvName = view.findViewById(R.id.tvName);
        tvStats = view.findViewById(R.id.tvStats);
        tvReview = view.findViewById(R.id.tvReview);
        tagContainer = view.findViewById(R.id.tagContainer);
        settingsList = view.findViewById(R.id.settingsList);
        avatar = view.findViewById(R.id.avatar);

        avatarPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri == null) {
                            return;
                        }
                        if (AvatarStore.saveFromUri(requireContext(), uri)) {
                            render();
                            Toast.makeText(requireContext(), "头像已更新", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "这张图片读取失败，换一张 JPG / PNG 试试",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRenameDialog();
            }
        });

        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAvatarDialog();
            }
        });

        view.findViewById(R.id.btnReview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WeeklyReportDialog.show(requireContext(), new WeeklyReportDialog.OnAction() {
                    @Override
                    public void onWrite() {
                        EditorActivity.open(requireActivity(), 0L, false, false);
                    }

                    @Override
                    public void onBrowse() {
                        if (requireActivity() instanceof MainActivity) {
                            ((MainActivity) requireActivity())
                                    .selectTab(MainActivity.TAB_TIMELINE);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        render();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            render();
        }
    }

    private void render() {
        if (!isAdded() || getView() == null) {
            return;
        }
        Context c = requireContext();
        NoteStore store = NoteStore.get(c);

        tvName.setText(Prefs.name(c));
        tvStats.setText(String.format(Locale.CHINA, "%d 条记录 · 连续 %d 天 · %s 字",
                store.totalCount(), store.streakDays(), compact(store.totalWords())));
        AvatarStore.apply(c, avatar, dp(62));

        long from = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        int days = store.daysWithNotesSince(from);
        int words = store.wordsSince(from);
        if (days == 0) {
            tvReview.setText("这 7 天还没有记录。今晚写一句，明早会感谢自己。");
        } else {
            tvReview.setText(String.format(Locale.CHINA,
                    "这 7 天你记录了 %d 天，写下 %d 字。慢一点，也写得长一点。", days, words));
        }

        renderTags(store);
        renderSettings();
    }

    private void renderTags(NoteStore store) {
        tagContainer.removeAllViews();
        long monthStart = startOfMonth();
        Map<String, Integer> tags = store.topTags(monthStart, 5);
        if (tags.isEmpty()) {
            tags = store.topTags(0, 5);
        }
        if (tags.isEmpty()) {
            addChip("平静", R.drawable.bg_chip_primary, R.color.primary);
            return;
        }
        for (String tag : tags.keySet()) {
            addChip(tag, R.drawable.bg_chip_sand, R.color.text_secondary);
        }
    }

    private void addChip(String text, int bgRes, int colorRes) {
        TextView chip = new TextView(requireContext());
        chip.setText(text);
        chip.setTextSize(11f);
        chip.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        chip.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
        chip.setBackgroundResource(bgRes);
        chip.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        chip.setLayoutParams(lp);
        tagContainer.addView(chip);
    }

    private void renderSettings() {
        settingsList.removeAllViews();
        addSetting(R.drawable.ic_bell, "记录提醒", Prefs.remindTime(requireContext()), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRemindDialog();
            }
        });
        addSetting(R.drawable.ic_moon, "主题外观", themeName(Prefs.themeMode(requireContext())),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showThemeDialog();
                    }
                });
        addSetting(R.drawable.ic_cloud, "数据与备份", null, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDataDialog();
            }
        });
        addSetting(R.drawable.ic_info, "关于随心记", "v1.0.0", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppDialog.info(requireContext(), "关于随心记",
                        "随心记 v1.0.0\n想记就记，不必完整。\n\n所有记录仅保存在本机。");
            }
        });
    }

    private void addSetting(int iconRes, String name, String value, View.OnClickListener click) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_setting, settingsList, false);
        ((ImageView) row.findViewById(R.id.ivIcon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.tvName)).setText(name);
        TextView tvValue = row.findViewById(R.id.tvValue);
        if (value == null || value.length() == 0) {
            tvValue.setVisibility(View.GONE);
        } else {
            tvValue.setText(value);
        }
        row.setOnClickListener(click);

        if (settingsList.getChildCount() > 0) {
            View divider = new View(requireContext());
            divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.divider));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            dlp.leftMargin = dp(46);
            divider.setLayoutParams(dlp);
            settingsList.addView(divider);
        }
        settingsList.addView(row);
    }

    private void showAvatarDialog() {
        AppDialog.actions(requireContext(), "头像", new String[]{"从相册选择", "恢复默认头像"}, -1,
                new AppDialog.OnChoose() {
                    @Override
                    public void onChoose(int index) {
                        if (index == 0) {
                            avatarPicker.launch("image/*");
                        } else {
                            AvatarStore.clear(requireContext());
                            render();
                            Toast.makeText(requireContext(), "已恢复默认头像", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showRenameDialog() {
        AppDialog.input(requireContext(), "怎么称呼你", null, "你的昵称",
                Prefs.name(requireContext()), "保存", new AppDialog.OnText() {
                    @Override
                    public void onText(String name) {
                        Prefs.setName(requireContext(), name);
                        render();
                    }
                });
    }

    private void showRemindDialog() {
        ReminderDialog.show(requireContext(), new ReminderDialog.OnSaved() {
            @Override
            public void onSaved(boolean enabled, int hour, int minute) {
                render();
                if (!enabled) {
                    Toast.makeText(requireContext(), R.string.remind_closed, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(requireContext(),
                        getString(R.string.remind_saved, Prefs.remindClock(requireContext())),
                        Toast.LENGTH_SHORT).show();
                // 提醒排上了，但系统层面关掉了通知也响不出来，这里直说
                if (!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                    AppDialog.info(requireContext(), "通知被关闭了",
                            getString(R.string.remind_blocked));
                }
            }
        });
    }

    private void showThemeDialog() {
        final String[] items = {"跟随系统", "浅色", "深色"};
        AppDialog.list(requireContext(), "主题外观", items, Prefs.themeMode(requireContext()),
                new AppDialog.OnChoose() {
                    @Override
                    public void onChoose(int which) {
                        Prefs.setThemeMode(requireContext(), which);
                        applyTheme(which);
                    }
                });
    }

    private void applyTheme(int mode) {
        switch (mode) {
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
    }

    private void showDataDialog() {
        final NoteStore store = NoteStore.get(requireContext());
        String[] items = {"复制全部记录为文本", "清空所有记录"};
        AppDialog.actions(requireContext(), "数据与备份", items, 1, new AppDialog.OnChoose() {
            @Override
            public void onChoose(int which) {
                if (which == 0) {
                    String text = store.exportText();
                    ClipboardManager cm = (ClipboardManager) requireContext()
                            .getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("随心记", text));
                        Toast.makeText(requireContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    confirmClear();
                }
            }
        });
    }

    private void confirmClear() {
        AppDialog.confirm(requireContext(), "清空所有记录？",
                "包括示例数据在内的全部记录都会被删除，且无法恢复。",
                "清空", true, new AppDialog.OnConfirm() {
                    @Override
                    public void run() {
                        NoteStore.get(requireContext()).clearAll();
                        render();
                    }
                });
    }

    private String themeName(int mode) {
        switch (mode) {
            case 1:
                return "浅色";
            case 2:
                return "深色";
            default:
                return "跟随系统";
        }
    }

    private long startOfMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private String compact(int value) {
        if (value >= 10000) {
            return String.format(Locale.CHINA, "%.1f 万", value / 10000f);
        }
        return String.valueOf(value);
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
