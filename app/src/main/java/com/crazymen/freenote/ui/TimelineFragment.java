package com.crazymen.freenote.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.crazymen.freenote.R;
import com.crazymen.freenote.data.NoteStore;
import com.crazymen.freenote.model.Note;
import com.crazymen.freenote.util.Dates;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 03 时光：按月回看。
 * 点击周历上的某一天 → 下面的列表与统计切到那一天；点「整月」回到整月视图。
 * 周历左右滑动可切换上一周 / 下一周。
 */
public class TimelineFragment extends Fragment {

    /** 当前展示的锚点日（决定月份与周历所在周） */
    private long anchorDay = Dates.startOfDay(System.currentTimeMillis());
    /** 选中的具体某一天，0 = 未选（整月视图） */
    private long selectedDay = 0L;

    private TextView tvMonth;
    private TextView tvStreak;
    private TextView tvMonthCount;
    private TextView tvTotalWords;
    private TextView tvCountLabel;
    private TextView tvWordsLabel;
    private TextView tvEmpty;
    private TextView tvFilter;
    private LinearLayout weekStrip;
    private LinearLayout timelineList;
    private LinearLayout filterBar;
    private View btnEarlier;
    private NestedScrollView scrollView;

    /** 搜索态：隐藏月份 / 周历 / 统计，只留搜索栏与结果 */
    private boolean searchMode;
    private String query = "";
    private LinearLayout searchBar;
    private LinearLayout monthRow;
    private View statsCard;
    private TextView tvSearchInfo;
    private EditText etSearch;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final Runnable searchTask = new Runnable() {
        @Override
        public void run() {
            render();
        }
    };

    private GestureDetector weekDetector;
    private long lastSwipeAt;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timeline, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvMonth = view.findViewById(R.id.tvMonth);
        tvStreak = view.findViewById(R.id.tvStreak);
        tvMonthCount = view.findViewById(R.id.tvMonthCount);
        tvTotalWords = view.findViewById(R.id.tvTotalWords);
        tvCountLabel = view.findViewById(R.id.tvCountLabel);
        tvWordsLabel = view.findViewById(R.id.tvWordsLabel);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvFilter = view.findViewById(R.id.tvFilter);
        weekStrip = view.findViewById(R.id.weekStrip);
        timelineList = view.findViewById(R.id.timelineList);
        filterBar = view.findViewById(R.id.filterBar);
        btnEarlier = view.findViewById(R.id.btnEarlier);
        scrollView = view.findViewById(R.id.timelineScroll);
        searchBar = view.findViewById(R.id.searchBar);
        monthRow = view.findViewById(R.id.monthRow);
        statsCard = view.findViewById(R.id.statsCard);
        tvSearchInfo = view.findViewById(R.id.tvSearchInfo);
        etSearch = view.findViewById(R.id.etSearch);

        weekDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) {
                    return false;
                }
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dx) > dp(36) && Math.abs(dx) > Math.abs(dy) * 1.4f) {
                    lastSwipeAt = SystemClock.uptimeMillis();
                    shiftWeek(dx < 0 ? 1 : -1);
                    return true;
                }
                return false;
            }
        });

        view.findViewById(R.id.btnPrevMonth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shiftMonth(-1);
            }
        });

        view.findViewById(R.id.btnNextMonth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shiftMonth(1);
            }
        });

        view.findViewById(R.id.btnToday).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                anchorDay = Dates.startOfDay(System.currentTimeMillis());
                selectedDay = 0L;
                render();
            }
        });

        View.OnClickListener openCalendar = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCalendar();
            }
        };
        view.findViewById(R.id.btnMonthPicker).setOnClickListener(openCalendar);
        view.findViewById(R.id.btnPickDate).setOnClickListener(openCalendar);
        view.findViewById(R.id.btnChangeDay).setOnClickListener(openCalendar);

        view.findViewById(R.id.btnEarlier).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shiftMonth(-1);
            }
        });

        view.findViewById(R.id.btnSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchMode) {
                    exitSearch();
                } else {
                    enterSearch();
                }
            }
        });

        view.findViewById(R.id.btnCancelSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSearch();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString();
                searchHandler.removeCallbacks(searchTask);
                searchHandler.postDelayed(searchTask, 200);
            }
        });
        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                searchHandler.removeCallbacks(searchTask);
                query = etSearch.getText().toString();
                render();
                hideKeyboard();
                return true;
            }
        });

        // 搜索态下先退搜索，而不是直接退出 App
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (searchMode && !isHidden()) {
                            exitSearch();
                            return;
                        }
                        setEnabled(false);
                        requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        setEnabled(true);
                    }
                });

        view.findViewById(R.id.btnClearDay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedDay = 0L;
                render();
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
        if (hidden) {
            hideKeyboard();
        } else {
            render();
        }
    }

    @Override
    public void onDestroyView() {
        searchHandler.removeCallbacks(searchTask);
        super.onDestroyView();
    }

    // ------------------------------------------------------------------ 渲染

    private void render() {
        if (!isAdded() || getView() == null) {
            return;
        }
        Context c = requireContext();
        NoteStore store = NoteStore.get(c);

        boolean searching = searchMode;
        String keyword = query == null ? "" : query.trim();
        boolean hasQuery = keyword.length() > 0;

        Calendar ac = Dates.cal(anchorDay);
        int year = ac.get(Calendar.YEAR);
        int month = ac.get(Calendar.MONTH);
        tvMonth.setText(Dates.monthLabel(year, month));

        boolean dayMode = !searching && selectedDay != 0L;
        if (!searching) {
            buildWeekStrip(anchorDay);
        }

        List<Note> notes;
        if (searching) {
            notes = hasQuery ? store.search(keyword) : new ArrayList<Note>();
        } else if (dayMode) {
            notes = store.byDay(selectedDay);
        } else {
            notes = store.byMonth(year, month);
        }

        int words = 0;
        for (Note n : notes) {
            words += n.wordCount();
        }

        // 搜索时收起月份、周历与统计，只留搜索栏和结果
        monthRow.setVisibility(searching ? View.GONE : View.VISIBLE);
        weekStrip.setVisibility(searching ? View.GONE : View.VISIBLE);
        statsCard.setVisibility(searching ? View.GONE : View.VISIBLE);
        searchBar.setVisibility(searching ? View.VISIBLE : View.GONE);
        tvSearchInfo.setVisibility(searching ? View.VISIBLE : View.GONE);

        tvStreak.setText(String.valueOf(store.streakDays()));
        tvMonthCount.setText(String.valueOf(notes.size()));
        if (dayMode) {
            tvCountLabel.setText("当日记录 · 条");
            tvWordsLabel.setText("当日字数");
            tvTotalWords.setText(compactNumber(words));
        } else {
            tvCountLabel.setText("本月记录 · 条");
            tvWordsLabel.setText("累计字数");
            tvTotalWords.setText(compactNumber(store.totalWords()));
        }

        if (searching) {
            tvSearchInfo.setText(hasQuery
                    ? String.format(Locale.CHINA, "找到 %d 条 · 含「%s」", notes.size(), keyword)
                    : "输入关键词，搜标题、正文和标签");
        }

        filterBar.setVisibility(dayMode ? View.VISIBLE : View.GONE);
        if (dayMode) {
            tvFilter.setText(String.format(Locale.CHINA, "%s · %d 条",
                    Dates.shortDayLabel(selectedDay), notes.size()));
        }
        btnEarlier.setVisibility(!searching && !dayMode ? View.VISIBLE : View.GONE);

        timelineList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(c);
        if (searching) {
            Map<Long, Integer> perDay = countByDay(notes);
            long lastDayTs = 0L;
            for (Note n : notes) {
                long dayStart = Dates.startOfDay(n.createdAt);
                if (dayStart != lastDayTs) {
                    Integer count = perDay.get(dayStart);
                    timelineList.addView(groupLabel(String.format(Locale.CHINA, "%s · %d 条",
                            Dates.shortDayLabel(n.createdAt), count == null ? 0 : count)));
                    lastDayTs = dayStart;
                }
                timelineList.addView(bindItem(inflater, n));
            }
        } else if (dayMode) {
            for (Note n : notes) {
                timelineList.addView(bindItem(inflater, n));
            }
        } else {
            long lastDayTs = 0L;
            for (Note n : notes) {
                long dayStart = Dates.startOfDay(n.createdAt);
                if (dayStart != lastDayTs) {
                    timelineList.addView(groupLabel(Dates.shortDayLabel(n.createdAt)));
                    lastDayTs = dayStart;
                }
                timelineList.addView(bindItem(inflater, n));
            }
        }

        if (searching) {
            boolean empty = hasQuery && notes.isEmpty();
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            tvEmpty.setText(String.format(Locale.CHINA,
                    "没有找到「%s」相关的记录\n换个词试试，比如「桂花」「读书」", keyword));
            tvEmpty.setClickable(false);
            tvEmpty.setOnClickListener(null);
        } else if (dayMode) {
            tvEmpty.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
            tvEmpty.setText("这一天还没有记录\n点这里，补一条到这一天");
            tvEmpty.setClickable(true);
            tvEmpty.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditorActivity.openOnDay(requireActivity(), selectedDay);
                }
            });
        } else {
            tvEmpty.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
            tvEmpty.setText("这个月还没有记录\n去「今日」写一条吧");
            tvEmpty.setClickable(false);
            tvEmpty.setOnClickListener(null);
        }
    }

    private Map<Long, Integer> countByDay(List<Note> notes) {
        Map<Long, Integer> perDay = new HashMap<>();
        for (Note n : notes) {
            long day = Dates.startOfDay(n.createdAt);
            Integer old = perDay.get(day);
            perDay.put(day, old == null ? 1 : old + 1);
        }
        return perDay;
    }

    // ------------------------------------------------------------------ 搜索

    private void enterSearch() {
        searchMode = true;
        searchBar.setVisibility(View.VISIBLE);
        monthRow.setVisibility(View.GONE);
        weekStrip.setVisibility(View.GONE);
        statsCard.setVisibility(View.GONE);
        etSearch.setText("");
        query = "";
        etSearch.requestFocus();
        etSearch.post(new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || etSearch == null) {
                    return;
                }
                InputMethodManager imm = (InputMethodManager)
                        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
        render();
    }

    private void exitSearch() {
        if (!searchMode) {
            return;
        }
        searchMode = false;
        query = "";
        searchHandler.removeCallbacks(searchTask);
        etSearch.setText("");
        etSearch.clearFocus();
        hideKeyboard();
        render();
    }

    private void hideKeyboard() {
        if (!isAdded()) {
            return;
        }
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && etSearch != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
    }

    /** 在结果文本里把命中的关键词标成茶绿加粗 */
    private void highlight(TextView tv, String text, String keyword) {
        if (text == null || text.length() == 0) {
            tv.setText(text);
            return;
        }
        if (keyword == null || keyword.length() == 0) {
            tv.setText(text);
            return;
        }
        String lowerText = text.toLowerCase(Locale.CHINA);
        String lowerKey = keyword.toLowerCase(Locale.CHINA);
        SpannableString span = new SpannableString(text);
        int from = 0;
        boolean hit = false;
        while (true) {
            int index = lowerText.indexOf(lowerKey, from);
            if (index < 0) {
                break;
            }
            int end = Math.min(index + keyword.length(), text.length());
            span.setSpan(new ForegroundColorSpan(
                            ContextCompat.getColor(requireContext(), R.color.primary)),
                    index, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new StyleSpan(Typeface.BOLD), index, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            from = index + keyword.length();
            hit = true;
        }
        if (hit) {
            tv.setText(span);
        } else {
            tv.setText(text);
        }
    }

    private void buildWeekStrip(long reference) {
        weekStrip.removeAllViews();
        final long[] week = Dates.weekOf(reference);
        for (int i = 0; i < week.length; i++) {
            final long day = week[i];
            boolean today = Dates.isToday(day);
            boolean active = selectedDay != 0L ? Dates.isSameDay(day, selectedDay) : today;
            boolean inMonth = monthIndex(day) == monthIndex(reference);

            LinearLayout col = new LinearLayout(requireContext());
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            col.setPadding(0, dp(4), 0, dp(4));
            col.setClickable(true);
            col.setFocusable(true);
            Drawable ripple = selectableBackground();
            if (ripple != null) {
                col.setBackground(ripple);
            }

            TextView label = new TextView(requireContext());
            label.setText(Dates.weekShort(day));
            label.setTextSize(10f);
            label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            label.setTextColor(ContextCompat.getColor(requireContext(),
                    active ? R.color.primary : R.color.text_quaternary));
            col.addView(label);

            col.addView(dayCircle(day, active, today, inMonth));

            col.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectDay(day);
                }
            });
            col.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (weekDetector != null) {
                        weekDetector.onTouchEvent(event);
                    }
                    return false;
                }
            });
            weekStrip.addView(col);
        }
    }

    private View dayCircle(long day, boolean active, boolean today, boolean inMonth) {
        Calendar dc = Dates.cal(day);
        String num = String.valueOf(dc.get(Calendar.DAY_OF_MONTH));

        FrameLayout circle = new FrameLayout(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(28), dp(28));
        lp.topMargin = dp(4);
        circle.setLayoutParams(lp);
        if (active) {
            circle.setBackgroundResource(R.drawable.bg_week_today);
        } else if (today) {
            circle.setBackgroundResource(R.drawable.bg_day_outline);
        }

        TextView tv = new TextView(requireContext());
        tv.setText(num);
        tv.setTextSize(13f);
        tv.setTypeface(Typeface.create(active ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        tv.setTextColor(ContextCompat.getColor(requireContext(),
                active ? R.color.on_primary
                        : today ? R.color.primary
                        : inMonth ? R.color.text_secondary : R.color.text_ghost));
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        tlp.gravity = Gravity.CENTER;
        tv.setLayoutParams(tlp);
        circle.addView(tv);
        return circle;
    }

    private View groupLabel(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(12f);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setLetterSpacing(0.04f);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_quaternary));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        tv.setLayoutParams(lp);
        return tv;
    }

    private View bindItem(LayoutInflater inflater, final Note note) {
        View v = inflater.inflate(R.layout.item_timeline, timelineList, false);
        ((TextView) v.findViewById(R.id.tvTime)).setText(Dates.time(note.createdAt));

        TextView title = v.findViewById(R.id.tvTitle);
        String titleText = note.displayTitle();
        title.setText(titleText);

        TextView excerpt = v.findViewById(R.id.tvExcerpt);
        String text = note.body == null ? "" : note.body.trim().replace("\n", " ");
        if (text.length() == 0 && note.hasPhoto()) {
            text = String.format(Locale.CHINA, "配图 %d 张", note.photos.size());
        }
        String excerptText = text.length() == 0 ? "（无正文）" : text;
        excerpt.setText(excerptText);

        if (searchMode && query != null && query.trim().length() > 0) {
            highlight(title, titleText, query.trim());
            highlight(excerpt, excerptText, query.trim());
        }

        View dot = v.findViewById(R.id.dot);
        int colorRes = R.color.primary_light;
        if (note.mood != null) {
            if (note.mood.contains("愉悦")) {
                colorRes = R.color.clay;
            } else if (note.mood.contains("怀念")) {
                colorRes = R.color.gold;
            }
        }
        dot.setBackgroundResource(R.drawable.bg_week_today);
        dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), colorRes)));

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v2) {
                NoteDetailActivity.open(requireActivity(), note.id);
            }
        });
        return v;
    }

    // ------------------------------------------------------------------ 交互

    private void selectDay(long day) {
        // 刚滑动过的那一下不当作点击
        if (lastSwipeAt != 0L && SystemClock.uptimeMillis() - lastSwipeAt < 350) {
            return;
        }
        jumpToDay(day);
    }

    /** 打开月历选日：整月一览，可逐月 / 逐年翻，有记录的日子带圆点 */
    private void showCalendar() {
        if (!isAdded()) {
            return;
        }
        CalendarDialog.show(requireContext(), anchorDay, selectedDay, new CalendarDialog.OnPick() {
            @Override
            public void onPick(long dayStart) {
                jumpToDay(dayStart);
            }
        });
    }

    /** 跳到某一天并筛选当天记录（周历点击与月历选日共用） */
    private void jumpToDay(long day) {
        long start = Dates.startOfDay(day);
        selectedDay = start;
        anchorDay = start;
        render();
        scrollToFilter();
    }

    /** 选中后把筛选条滚到视野顶部，立刻看到这一天的记录 */
    private void scrollToFilter() {
        if (scrollView == null || filterBar.getVisibility() != View.VISIBLE) {
            return;
        }
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.smoothScrollTo(0, Math.max(0, filterBar.getTop() - dp(12)));
            }
        });
    }

    private void shiftMonth(int delta) {
        if (delta > 0 && monthIndex(anchorDay) >= monthIndex(System.currentTimeMillis())) {
            Toast.makeText(requireContext(), "已经是本月了", Toast.LENGTH_SHORT).show();
            return;
        }
        Calendar c = Dates.cal(anchorDay);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.MONTH, delta);
        anchorDay = Dates.startOfDay(c.getTimeInMillis());
        selectedDay = 0L;
        render();
    }

    private void shiftWeek(int delta) {
        Calendar c = Dates.cal(anchorDay);
        c.add(Calendar.DAY_OF_MONTH, delta * 7);
        long target = Dates.startOfDay(c.getTimeInMillis());
        if (monthIndex(target) > monthIndex(System.currentTimeMillis())) {
            return;
        }
        anchorDay = target;
        selectedDay = 0L;
        render();
    }

    private int monthIndex(long ts) {
        Calendar c = Dates.cal(ts);
        return c.get(Calendar.YEAR) * 12 + c.get(Calendar.MONTH);
    }

    private Drawable selectableBackground() {
        android.content.res.TypedArray ta = requireContext()
                .obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
        try {
            return ta.getDrawable(0);
        } finally {
            ta.recycle();
        }
    }

    private String compactNumber(int value) {
        if (value >= 10000) {
            return String.format(Locale.CHINA, "%.1f万", value / 10000f);
        }
        return String.valueOf(value);
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
