package com.crazymen.freenote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.viewpager.widget.ViewPager;

import com.crazymen.freenote.data.NoteStore;
import com.crazymen.freenote.data.db.NoteDatabase;
import com.crazymen.freenote.model.Note;
import com.crazymen.freenote.reminder.ReminderNotifier;
import com.crazymen.freenote.reminder.ReminderReceiver;
import com.crazymen.freenote.reminder.ReminderScheduler;
import com.crazymen.freenote.ui.EditorActivity;
import com.crazymen.freenote.ui.NoteDetailActivity;
import com.crazymen.freenote.ui.PhotoViewerActivity;
import com.crazymen.freenote.ui.ReminderDialog;
import com.crazymen.freenote.util.Dates;
import com.crazymen.freenote.util.Encouragements;
import com.crazymen.freenote.util.MonthGrid;
import com.crazymen.freenote.util.PhotoStore;
import com.crazymen.freenote.util.Prefs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowNotificationManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.List;

/**
 * 运行冒烟测试：真实启动 Activity / Fragment，验证渲染、切页、Room 持久化、配图与深色资源。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public class UiSmokeTest {

    /** 每个用例重建数据库连接与仓库单例，避免数据串到上一个用例 */
    @Before
    public void resetStore() {
        NoteDatabase.resetForTest();
        NoteStore.resetForTest();
    }

    @Test
    public void todayScreenRendersSeededNotes() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();

        assertNotNull(activity.findViewById(R.id.tabToday));
        TextView greeting = activity.findViewById(R.id.tvGreeting);
        assertTrue("问候语应有内容", greeting.getText().length() > 0);

        LinearLayout list = activity.findViewById(R.id.listToday);
        assertNotNull(list);
        assertTrue("今日应渲染出示例记录卡", list.getChildCount() > 0);

        NoteStore store = NoteStore.get(ApplicationProvider.getApplicationContext());
        assertTrue("示例数据应已写入 Room", store.totalCount() > 0);
    }

    @Test
    public void switchingTabsRendersEachScreen() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();

        activity.findViewById(R.id.tabTimeline).performClick();
        settle(activity);
        TextView month = activity.findViewById(R.id.tvMonth);
        assertNotNull(month);
        assertTrue("月份应已生成", month.getText().toString().contains("年"));

        LinearLayout week = activity.findViewById(R.id.weekStrip);
        assertEquals("周历应有 7 天", 7, week.getChildCount());

        activity.findViewById(R.id.tabProfile).performClick();
        settle(activity);
        TextView name = activity.findViewById(R.id.tvName);
        assertEquals("随心", name.getText().toString());

        LinearLayout settings = activity.findViewById(R.id.settingsList);
        assertTrue("设置项应渲染", settings.getChildCount() >= 4);
    }

    @Test
    public void quickCaptureCreatesNote() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();

        NoteStore store = NoteStore.get(ApplicationProvider.getApplicationContext());
        int before = store.byDay(System.currentTimeMillis()).size();

        EditText quick = activity.findViewById(R.id.etQuick);
        quick.setText("测试：快速记一句");
        activity.findViewById(R.id.btnRecord).performClick();

        int after = store.byDay(System.currentTimeMillis()).size();
        assertEquals("快速记录应新增一条", before + 1, after);
        assertEquals("输入框应清空", 0, quick.getText().length());
    }

    @Test
    public void editorSavesAndPersistsText() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EditorActivity.class);
        intent.putExtra(EditorActivity.EXTRA_ID, 0L);
        ActivityController<EditorActivity> controller =
                Robolectric.buildActivity(EditorActivity.class, intent).setup();
        EditorActivity editor = controller.get();

        NoteStore store = NoteStore.get(ApplicationProvider.getApplicationContext());
        int before = store.totalCount();

        ((EditText) editor.findViewById(R.id.etTitle)).setText("傍晚的风");
        ((EditText) editor.findViewById(R.id.etBody)).setText("窗帘被风吹起来，像一面很轻的帆。");
        editor.findViewById(R.id.btnSave).performClick();

        assertEquals("保存后应新增记录", before + 1, store.totalCount());
        assertTrue("Activity 应结束", editor.isFinishing());
    }

    @Test
    public void emptyEditorDoesNotCreateNote() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EditorActivity.class);
        intent.putExtra(EditorActivity.EXTRA_ID, 0L);
        ActivityController<EditorActivity> controller =
                Robolectric.buildActivity(EditorActivity.class, intent).setup();
        EditorActivity editor = controller.get();

        NoteStore store = NoteStore.get(ApplicationProvider.getApplicationContext());
        int before = store.totalCount();

        editor.findViewById(R.id.btnSave).performClick();

        assertEquals("空记录不应写入", before, store.totalCount());
        assertTrue("空记录保存后编辑页应留在原地", !editor.isFinishing());
    }

    /**
     * 回归一：删除记录后，finish() 触发的 onPause 自动保存不能把记录写回 Room。
     * 回归二：删除只弹一个确认框，不再先经过「记录操作」菜单。
     */
    @Test
    public void deletingNoteShowsSingleDialogAndRemovesIt() {
        NoteStore store = NoteStore.get(ApplicationProvider.getApplicationContext());
        Note target = store.all().get(0);
        final long id = target.id;
        int before = store.totalCount();

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EditorActivity.class);
        intent.putExtra(EditorActivity.EXTRA_ID, id);
        ActivityController<EditorActivity> controller =
                Robolectric.buildActivity(EditorActivity.class, intent).setup();
        EditorActivity editor = controller.get();

        editor.findViewById(R.id.btnDelete).performClick();

        List<Dialog> shown = ShadowDialog.getShownDialogs();
        assertEquals("删除只应弹一个确认框", 1, shown.size());
        Dialog confirm = ShadowDialog.getLatestDialog();
        assertNotNull("应弹出删除确认弹框", confirm);
        confirm.findViewById(R.id.dialog_positive).performClick();

        // 关键复现步骤：删除后 Activity 仍会经历 onPause
        controller.pause();

        assertEquals("删除后条数应减少", before - 1, store.totalCount());
        assertNull("删除后不应再查得到", store.find(id));
        assertTrue("Activity 应结束", editor.isFinishing());

        // 重开数据库，确认删除已落盘
        NoteDatabase.resetForTest();
        NoteStore.resetForTest();
        NoteStore reopened = NoteStore.get(ApplicationProvider.getApplicationContext());
        assertNull("重启后也不应再查得到这条记录", reopened.find(id));
        assertEquals(before - 1, reopened.totalCount());
    }

    @Test
    public void timelineDayTapFiltersListAndStats() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        activity.findViewById(R.id.tabTimeline).performClick();
        settle(activity);

        long monday = Dates.weekOf(System.currentTimeMillis())[0];
        LinearLayout week = activity.findViewById(R.id.weekStrip);
        week.getChildAt(0).performClick();

        View filterBar = activity.findViewById(R.id.filterBar);
        assertEquals("选中某天后应出现筛选条", View.VISIBLE, filterBar.getVisibility());

        TextView filter = activity.findViewById(R.id.tvFilter);
        assertTrue("筛选条应写明日期", filter.getText().toString().contains("月"));

        NoteStore store = NoteStore.get(ApplicationProvider.getApplicationContext());
        int expected = store.byDay(monday).size();
        LinearLayout list = activity.findViewById(R.id.timelineList);
        assertEquals("列表应只显示这一天的记录", expected, list.getChildCount());

        TextView count = activity.findViewById(R.id.tvMonthCount);
        assertEquals(String.valueOf(expected), count.getText().toString());
        TextView countLabel = activity.findViewById(R.id.tvCountLabel);
        assertEquals("统计标题应切换为当日", "当日记录 · 条", countLabel.getText().toString());

        activity.findViewById(R.id.btnClearDay).performClick();
        assertEquals("点「整月」应回到整月视图", View.GONE, filterBar.getVisibility());
        TextView countLabelReset = activity.findViewById(R.id.tvCountLabel);
        assertEquals("统计标题应切回本月", "本月记录 · 条", countLabelReset.getText().toString());
    }

    /** Room：标签 / 心情 / 时间在仓库重启后应原样读回 */
    @Test
    public void noteFieldsPersistAcrossStoreRestart() {
        NoteStore store = NoteStore.get(ApplicationProvider.getApplicationContext());
        Note n = new Note();
        n.id = System.currentTimeMillis();
        n.createdAt = n.id;
        n.title = "拆一箱旧书";
        n.body = "扉页上还留着别人的名字。";
        n.mood = "怀念";
        n.tags.add("读书");
        n.tags.add("旧物");
        store.save(n);

        NoteStore.resetForTest();
        NoteStore reopened = NoteStore.get(ApplicationProvider.getApplicationContext());
        Note loaded = reopened.find(n.id);

        assertNotNull("重启后应能读回记录", loaded);
        assertEquals(n.title, loaded.title);
        assertEquals(n.body, loaded.body);
        assertEquals(n.mood, loaded.mood);
        assertEquals(n.tags, loaded.tags);
        assertEquals(n.createdAt, loaded.createdAt);
    }

    /** 配图：落盘 → 入库 → 读取 → 删除记录级联清文件 */
    @Test
    public void photoPersistsAndIsCleanedWithNote() {
        android.content.Context ctx = ApplicationProvider.getApplicationContext();
        Bitmap bmp = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(0xFF887766);

        String name = PhotoStore.importBitmap(ctx, bmp);
        assertNotNull("图片应成功落盘", name);
        assertTrue(PhotoStore.exists(ctx, name));
        assertNotNull("应能按尺寸读回图片", PhotoStore.load(ctx, name, 128));

        NoteStore store = NoteStore.get(ctx);
        Note n = new Note();
        n.id = System.currentTimeMillis();
        n.createdAt = n.id;
        n.body = "贴一张今天的照片。";
        n.photos.add(name);
        store.save(n);

        NoteStore.resetForTest();
        NoteStore reopened = NoteStore.get(ctx);
        Note loaded = reopened.find(n.id);
        assertNotNull(loaded);
        assertTrue("重启后配图应还在", loaded.hasPhoto());
        assertEquals(name, loaded.coverPhoto());

        reopened.delete(n.id);
        assertFalse("删除记录后图片文件应一并清理", PhotoStore.exists(ctx, name));
    }

    /** 月历网格：周一开头、6 行 42 格、整月都在网格里 */
    @Test
    public void monthGridStartsMondayAndCoversWholeMonth() {
        MonthGrid.Cell[] cells = MonthGrid.build(2026, Calendar.SEPTEMBER);
        assertEquals(MonthGrid.COLS * MonthGrid.ROWS, cells.length);
        assertEquals("首格应为周一", Calendar.MONDAY,
                Dates.cal(cells[0].day).get(Calendar.DAY_OF_WEEK));

        int inMonth = 0;
        boolean hasThird = false;
        for (MonthGrid.Cell cell : cells) {
            Calendar c = Dates.cal(cell.day);
            if (cell.inMonth) {
                inMonth++;
            }
            if (c.get(Calendar.MONTH) == Calendar.SEPTEMBER && c.get(Calendar.DAY_OF_MONTH) == 3) {
                hasThird = true;
            }
        }
        assertEquals("9 月应有 30 天", 30, inMonth);
        assertTrue("9 月 3 日必须落在网格内", hasThird);
    }

    /**
     * 核心交互回归：点日历按钮打开月历 → 翻到上个月 → 点 3 日，
     * 无需一周一周地挪，直接筛选到那一天。
     */
    @Test
    public void calendarPickerJumpsStraightToAnyPastDay() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        activity.findViewById(R.id.tabTimeline).performClick();
        settle(activity);

        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -1);
        c.set(Calendar.DAY_OF_MONTH, 3);
        c.set(Calendar.HOUR_OF_DAY, 10);
        c.set(Calendar.MINUTE, 20);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long target = c.getTimeInMillis();

        NoteStore store = NoteStore.get(ApplicationProvider.getApplicationContext());
        Note n = new Note();
        n.id = target;
        n.createdAt = target;
        n.body = "上个月的第三天，桂花开了。";
        store.save(n);

        activity.findViewById(R.id.btnPickDate).performClick();
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull("点日历按钮应弹出月历", dialog);

        dialog.findViewById(R.id.btnPrevMonth).performClick();
        TextView calMonth = dialog.findViewById(R.id.tvCalMonth);
        assertEquals("翻页后应显示上个月",
                Dates.monthLabel(c.get(Calendar.YEAR), c.get(Calendar.MONTH)),
                calMonth.getText().toString());

        View cell = findDayCell(dialog.findViewById(R.id.calGrid), Dates.startOfDay(target));
        assertNotNull("月历里应能定位到目标日期", cell);
        cell.performClick();
        settle(activity);

        View filterBar = activity.findViewById(R.id.filterBar);
        assertEquals("选完日期应立即进入当日视图", View.VISIBLE, filterBar.getVisibility());

        TextView filter = activity.findViewById(R.id.tvFilter);
        assertTrue("筛选条应显示目标日期，实际：" + filter.getText(),
                filter.getText().toString().startsWith(Dates.shortDayLabel(target)));
        assertTrue("筛选条应写明日条数，实际：" + filter.getText(),
                filter.getText().toString().endsWith("1 条"));

        TextView month = activity.findViewById(R.id.tvMonth);
        assertEquals("月份应切到目标月",
                Dates.monthLabel(c.get(Calendar.YEAR), c.get(Calendar.MONTH)),
                month.getText().toString());

        LinearLayout list = activity.findViewById(R.id.timelineList);
        assertEquals("列表应只剩这一天的记录", 1, list.getChildCount());
    }

    /** 空日子里的「补一条」应写到那一天，而不是今天 */
    @Test
    public void backdatedNoteLandsOnChosenDay() {
        android.content.Context ctx = ApplicationProvider.getApplicationContext();
        long day = Dates.startOfDay(System.currentTimeMillis() - 5L * 24 * 60 * 60 * 1000);
        NoteStore store = NoteStore.get(ctx);
        int before = store.byDay(day).size();

        Intent intent = new Intent(ctx, EditorActivity.class);
        intent.putExtra(EditorActivity.EXTRA_ID, 0L);
        intent.putExtra(EditorActivity.EXTRA_DAY, day);
        EditorActivity editor = Robolectric.buildActivity(EditorActivity.class, intent).setup().get();
        ((EditText) editor.findViewById(R.id.etBody)).setText("补写：那天的桂花。");
        editor.findViewById(R.id.btnSave).performClick();

        assertEquals("应写进选中的那一天", before + 1, store.byDay(day).size());
        boolean found = false;
        for (Note note : store.byDay(day)) {
            if ("补写：那天的桂花。".equals(note.body)) {
                found = true;
            }
        }
        assertTrue("新记录应属于该日期", found);
    }

    /**
     * 回归：导入相册图片时必须真的解码出尺寸。
     * 旧实现只 openInputStream 后直接 close，outWidth 恒为 -1，于是「这张图片读不出来」。
     */
    @Test
    public void photoImportReadsContentUri() {
        android.content.Context ctx = ApplicationProvider.getApplicationContext();
        Bitmap bmp = Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(0xFF778866);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, bytes);

        Uri uri = Uri.parse("content://freenote.test/photo/1");
        ShadowContentResolver resolver = org.robolectric.Shadows.shadowOf(ctx.getContentResolver());
        resolver.registerInputStream(uri, new ByteArrayInputStream(bytes.toByteArray()));

        String name = PhotoStore.importFromUri(ctx, uri);
        assertNotNull("content Uri 应能成功导入", name);
        assertTrue("导入后文件应存在", PhotoStore.exists(ctx, name));
        assertNotNull("导入后应能读回位图", PhotoStore.load(ctx, name, 200));
    }

    /** 时光页：搜索标题 / 正文 / 标签，结果按天分组，可退出 */
    @Test
    public void searchFindsNotesByKeyword() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        activity.findViewById(R.id.tabTimeline).performClick();
        settle(activity);

        NoteStore store = NoteStore.get(ApplicationProvider.getApplicationContext());
        Note n = new Note();
        n.id = System.currentTimeMillis();
        n.createdAt = n.id;
        n.title = "夜里的桂花";
        n.body = "楼下的桂花开了，整条街都是甜的。";
        n.tags.add("散步");
        store.save(n);

        activity.findViewById(R.id.btnSearch).performClick();
        EditText et = activity.findViewById(R.id.etSearch);
        assertEquals("搜索栏应展开", View.VISIBLE,
                activity.findViewById(R.id.searchBar).getVisibility());
        assertEquals("搜索时月份行应收起", View.GONE,
                activity.findViewById(R.id.monthRow).getVisibility());

        et.setText("桂花");
        et.onEditorAction(EditorInfo.IME_ACTION_SEARCH);
        settle(activity);

        LinearLayout list = activity.findViewById(R.id.timelineList);
        assertEquals("结果条数应与搜索一致", store.search("桂花").size(), countNoteItems(list));
        assertTrue("正文里应能找到命中记录", containsText(list, "楼下的桂花开了"));

        TextView info = activity.findViewById(R.id.tvSearchInfo);
        assertTrue("应显示命中数量，实际：" + info.getText(), info.getText().toString().contains("找到"));

        // 标签也参与搜索
        et.setText("散步");
        et.onEditorAction(EditorInfo.IME_ACTION_SEARCH);
        settle(activity);
        assertTrue("按标签应能搜到记录", countNoteItems(list) >= 1);

        // 搜不到时给出空态
        et.setText("完全不存在的词");
        et.onEditorAction(EditorInfo.IME_ACTION_SEARCH);
        settle(activity);
        assertEquals("无结果时列表应为空", 0, countNoteItems(list));
        // 三个页面都有 tvEmpty，这里限定在时光页的内容容器里找
        ViewGroup timelineRoot = (ViewGroup) list.getParent();
        assertEquals("应显示空结果提示", View.VISIBLE,
                timelineRoot.findViewById(R.id.tvEmpty).getVisibility());

        activity.findViewById(R.id.btnCancelSearch).performClick();
        assertEquals("退出后恢复月份行", View.VISIBLE,
                activity.findViewById(R.id.monthRow).getVisibility());
        assertEquals("退出后收起搜索栏", View.GONE,
                activity.findViewById(R.id.searchBar).getVisibility());
    }

    /** 本周回顾：数字与真实数据一致，柱子可点，「换一句」可切换 */
    @Test
    public void weeklyReportShowsRealDataAndInteractions() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        activity.findViewById(R.id.tabProfile).performClick();
        settle(activity);

        NoteStore store = NoteStore.get(ApplicationProvider.getApplicationContext());
        Calendar c = Dates.cal(Dates.startOfDay(System.currentTimeMillis()));
        c.add(Calendar.DAY_OF_MONTH, -6);
        int[] counts = store.dailyCounts(c.getTimeInMillis(), 7);
        int notes = 0;
        int days = 0;
        for (int count : counts) {
            notes += count;
            if (count > 0) {
                days++;
            }
        }

        activity.findViewById(R.id.btnReview).performClick();
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull("点「查看周报」应弹本周回顾", dialog);

        assertEquals("条数应与近 7 天数据一致", String.valueOf(notes),
                ((TextView) dialog.findViewById(R.id.tvReportNotes)).getText().toString());
        assertEquals("记录天数应一致", String.valueOf(days),
                ((TextView) dialog.findViewById(R.id.tvReportDays)).getText().toString());

        LinearLayout chart = dialog.findViewById(R.id.weekChart);
        assertEquals("应有 7 根柱子", 7, chart.getChildCount());

        TextView encourage = dialog.findViewById(R.id.tvEncourage);
        assertTrue("应给出鼓励语", encourage.getText().length() > 0);
        String first = encourage.getText().toString();
        dialog.findViewById(R.id.btnAnotherLine).performClick();
        assertFalse("「换一句」应切换文案", first.equals(encourage.getText().toString()));

        chart.getChildAt(6).performClick();
        TextView hint = dialog.findViewById(R.id.tvChartHint);
        assertTrue("点柱子应显示那天条数，实际：" + hint.getText(),
                hint.getText().toString().contains("条"));

        dialog.findViewById(R.id.btnReportWrite).performClick();
        assertFalse("点「写一条」后弹窗应关闭", dialog.isShowing());
    }

    // ------------------------------------------------------------------ 本轮新增

    /** 100 条鼓励语：四档各 25 条、互不重复、%d 会换成天数 */
    @Test
    public void encouragementsCoverFourTiersWithHundredLines() {
        assertEquals("总数应为 100 条", 100, Encouragements.total());
        java.util.Set<String> all = new java.util.HashSet<>();
        for (int tier = 0; tier < 4; tier++) {
            String[] pool = Encouragements.poolByTier(tier);
            assertEquals("每档应为 25 条", 25, pool.length);
            java.util.Collections.addAll(all, pool);
        }
        assertEquals("100 条文案不应有重复", 100, all.size());

        assertTrue(Encouragements.decorate("这周你来了 %d 天。", 3).contains("3 天"));

        // 「换一句」必须真的换一句
        String current = Encouragements.decorate(Encouragements.poolByTier(2)[0], 4);
        assertNotEquals(current, Encouragements.next(4, current));

        // 每次打开随机取，也不能撞上上一次
        String avoid = Encouragements.decorate(Encouragements.poolByTier(1)[0], 2);
        assertNotEquals(avoid, Encouragements.random(2, avoid));
    }

    /** 列表点一条记录 → 进详情页（不是直接落进编辑页） */
    @Test
    public void tappingNoteOpensDetailInsteadOfEditor() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        LinearLayout list = activity.findViewById(R.id.listToday);
        assertTrue("今日应有记录卡", list.getChildCount() > 0);

        list.getChildAt(0).performClick();
        Intent next = org.robolectric.Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull("应启动详情页", next);
        assertEquals(new ComponentName(activity, NoteDetailActivity.class), next.getComponent());
    }

    /** 详情页：标题 / 正文 / 心情标签 / 配图都在，点编辑才进编辑页 */
    @Test
    public void detailPageRendersNoteAndOpensEditor() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Bitmap bmp = Bitmap.createBitmap(240, 180, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(0xFF9AA07C);
        String photo = PhotoStore.importBitmap(ctx, bmp);

        NoteStore store = NoteStore.get(ctx);
        Note n = new Note();
        n.id = System.currentTimeMillis();
        n.createdAt = n.id;
        n.title = "拆一箱旧书";
        n.body = "一本 1998 年的诗集，扉页上还留着别人的名字。";
        n.mood = "怀念";
        n.tags.add("读书");
        n.photos.add(photo);
        store.save(n);

        Intent intent = new Intent(ctx, NoteDetailActivity.class);
        intent.putExtra(NoteDetailActivity.EXTRA_ID, n.id);
        NoteDetailActivity detail = Robolectric.buildActivity(NoteDetailActivity.class, intent)
                .setup().get();

        assertEquals("拆一箱旧书",
                ((TextView) detail.findViewById(R.id.tvDetailTitle)).getText().toString());
        assertTrue("正文应完整展示",
                ((TextView) detail.findViewById(R.id.tvDetailBody)).getText().toString()
                        .contains("扉页上还留着别人的名字"));
        assertEquals("心情 + 标签应两个 chip", 2,
                ((LinearLayout) detail.findViewById(R.id.chipRow)).getChildCount());
        assertEquals("配图应渲染出来", 1,
                ((LinearLayout) detail.findViewById(R.id.photoList)).getChildCount());

        detail.findViewById(R.id.btnEdit).performClick();
        Intent next = org.robolectric.Shadows.shadowOf(detail).getNextStartedActivity();
        assertNotNull("应进入编辑页", next);
        assertEquals(new ComponentName(detail, EditorActivity.class), next.getComponent());
    }

    /** 全屏看图：多张都在 pager 里，页码随翻页更新 */
    @Test
    public void photoViewerPagesThroughAllPhotos() {
        Context ctx = ApplicationProvider.getApplicationContext();
        String a = PhotoStore.importBitmap(ctx, solid(120, 160));
        String b = PhotoStore.importBitmap(ctx, solid(160, 120));
        assertNotNull(a);
        assertNotNull(b);

        Intent intent = new Intent(ctx, PhotoViewerActivity.class);
        intent.putExtra(PhotoViewerActivity.EXTRA_PHOTOS, new String[]{a, b});
        intent.putExtra(PhotoViewerActivity.EXTRA_INDEX, 0);
        PhotoViewerActivity viewer = Robolectric.buildActivity(PhotoViewerActivity.class, intent)
                .setup().get();

        ViewPager pager = viewer.findViewById(R.id.pager);
        assertEquals("两张图都应可翻", 2, pager.getAdapter().getCount());
        assertEquals("初始页码应对", "1 / 2",
                ((TextView) viewer.findViewById(R.id.tvCounter)).getText().toString());

        pager.setCurrentItem(1, false);
        assertEquals("翻页后页码应更新", "2 / 2",
                ((TextView) viewer.findViewById(R.id.tvCounter)).getText().toString());
    }

    /** 提醒：排上闹钟 → 到点发通知 → 自动续订次日 */
    @Test
    public void reminderSchedulesAlarmAndPostsNotification() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Prefs.setRemindEnabled(ctx, true);
        Prefs.setRemindTime(ctx, 7, 30);

        ReminderScheduler.schedule(ctx);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        ShadowAlarmManager alarms = org.robolectric.Shadows.shadowOf(am);
        assertNotNull("打开开关后应排上闹钟", alarms.peekNextScheduledAlarm());

        long next = ReminderScheduler.nextTrigger(7, 30);
        long now = System.currentTimeMillis();
        assertTrue("应排在将来", next > now);
        assertTrue("最迟不超过一天零一分钟", next - now <= 24L * 60 * 60 * 1000 + 60000L);

        // 模拟到点：清掉当前闹钟，再让 receiver 响一次
        ReminderScheduler.cancel(ctx);
        assertNull("取消后不应还有闹钟", alarms.peekNextScheduledAlarm());
        new ReminderReceiver().onReceive(ctx,
                new Intent(ReminderScheduler.ACTION_REMIND));

        NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        ShadowNotificationManager notifications = org.robolectric.Shadows.shadowOf(nm);
        assertEquals("应发出一条提醒", 1, notifications.size());
        Notification posted = notifications.getNotification(ReminderNotifier.NOTIFICATION_ID);
        assertNotNull(posted);
        assertEquals("该写一句了", posted.extras.getString(Notification.EXTRA_TITLE));

        assertNotNull("响过之后应自动续订下一次", alarms.peekNextScheduledAlarm());
    }

    /** 设置弹框：改时分 + 开关，写进偏好并重新排闹钟 */
    @Test
    public void reminderDialogSavesTimeAndToggle() {
        final Context ctx = ApplicationProvider.getApplicationContext();
        final boolean[] saved = {false};
        Dialog dialog = ReminderDialog.show(ctx, new ReminderDialog.OnSaved() {
            @Override
            public void onSaved(boolean enabled, int hour, int minute) {
                saved[0] = true;
            }
        });
        assertNotNull(dialog);

        ((NumberPicker) dialog.findViewById(R.id.npHour)).setValue(8);
        ((NumberPicker) dialog.findViewById(R.id.npMinute)).setValue(6);
        ((Switch) dialog.findViewById(R.id.swRemind)).setChecked(true);
        dialog.findViewById(R.id.btnRemindSave).performClick();

        assertTrue("应回调保存结果", saved[0]);
        assertEquals(8, Prefs.remindHour(ctx));
        assertEquals("60 分钟按 5 分钟一档", 30, Prefs.remindMinute(ctx));
        assertTrue(Prefs.remindEnabled(ctx));
        assertEquals("每天 08:30", Prefs.remindTime(ctx));

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        assertNotNull("保存后应排上闹钟",
                org.robolectric.Shadows.shadowOf(am).peekNextScheduledAlarm());
    }

    /** 配图入口：同时给出「拍一张」和「从相册选」 */
    @Test
    public void photoToolOffersCameraAndGallery() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                EditorActivity.class);
        intent.putExtra(EditorActivity.EXTRA_ID, 0L);
        EditorActivity editor = Robolectric.buildActivity(EditorActivity.class, intent)
                .setup().get();

        editor.findViewById(R.id.toolPhoto).performClick();
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull("点配图应弹出选择框", dialog);
        View content = dialog.findViewById(android.R.id.content);
        assertTrue("应能拍照", containsLabel(content, "拍一张照片"));
        assertTrue("应能从相册选", containsLabel(content, "从相册选择"));
    }

    /** 语音转文字入口已彻底移除 */
    @Test
    public void voiceEntryIsGone() {
        Context ctx = ApplicationProvider.getApplicationContext();
        String pkg = ctx.getPackageName();
        assertEquals("编辑页不应再有语音按钮", 0,
                ctx.getResources().getIdentifier("toolVoice", "id", pkg));
        assertEquals("今日页不应再有语音按钮", 0,
                ctx.getResources().getIdentifier("btnVoice", "id", pkg));
        assertEquals("语音图标应一并删除", 0,
                ctx.getResources().getIdentifier("ic_mic", "drawable", pkg));
    }

    private Bitmap solid(int w, int h) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(0xFF8A9B7A);
        return bmp;
    }

    /** 递归找文本（用于断言弹框里的选项文案） */
    private boolean containsLabel(View root, String label) {
        if (root instanceof TextView
                && label.contentEquals(((TextView) root).getText())) {
            return true;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsLabel(group.getChildAt(i), label)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 统计列表里真正的记录卡数量（跳过按天分组标题） */
    private int countNoteItems(ViewGroup parent) {
        int count = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i).findViewById(R.id.tvTitle) != null) {
                count++;
            }
        }
        return count;
    }

    private boolean containsText(ViewGroup parent, String text) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            TextView title = child.findViewById(R.id.tvTitle);
            if (title != null && title.getText().toString().contains(text)) {
                return true;
            }
            TextView excerpt = child.findViewById(R.id.tvExcerpt);
            if (excerpt != null && excerpt.getText().toString().contains(text)) {
                return true;
            }
        }
        return false;
    }

    /** 在月历里按时间戳找到某一天的格子 */
    private View findDayCell(View root, long day) {
        if (root == null) {
            return null;
        }
        Object tag = root.getTag(R.id.tag_day);
        if (tag instanceof Long && ((Long) tag).longValue() == day) {
            return root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findDayCell(group.getChildAt(i), day);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 让 FragmentTransaction 与主线程任务真正执行完 */
    private void settle(MainActivity activity) {
        activity.getSupportFragmentManager().executePendingTransactions();
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    }
    @Test
    @Config(sdk = 32, qualifiers = "night")
    public void darkModeUsesWarmBlackTokens() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        int bg = activity.getColor(R.color.bg);
        assertEquals("深色背景应为暖黑 #171511", 0xFF171511, bg);

        int ink = activity.getColor(R.color.ink);
        assertEquals("深色正文应为暖白 #F4EFE7", 0xFFF4EFE7, ink);

        View root = activity.findViewById(R.id.container);
        assertNotNull(root);
    }
}
