package com.crazymen.freenote.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.crazymen.freenote.data.db.NoteDao;
import com.crazymen.freenote.data.db.NoteDatabase;
import com.crazymen.freenote.data.db.NoteEntity;
import com.crazymen.freenote.data.db.NotePhotoEntity;
import com.crazymen.freenote.data.db.NoteWithPhotos;
import com.crazymen.freenote.model.Note;
import com.crazymen.freenote.util.Dates;
import com.crazymen.freenote.util.PhotoStore;
import com.crazymen.freenote.util.Prefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 本地记录仓库：Room/SQLite 持久化（freenote.db）。
 *
 * - notes 表存记录主体，note_photos 表存配图文件名（外键级联）；
 * - 首次启动：若存在旧版 notes.json 则自动导入，否则写入示例数据；
 * - 界面同步读写（数据量小、写入频率低），保持调用方式简单。
 */
public final class NoteStore {

    private static final String TAG = "NoteStore";
    private static final String LEGACY_FILE = "notes.json";
    private static final long DAY = 24L * 60 * 60 * 1000;

    private static NoteStore instance;

    private final Context context;
    private final NoteDao dao;
    private boolean initialized;

    private NoteStore(Context context) {
        this.context = context.getApplicationContext();
        this.dao = NoteDatabase.get(this.context).noteDao();
    }

    public static synchronized NoteStore get(Context context) {
        if (instance == null) {
            instance = new NoteStore(context);
        }
        instance.ensureInitialized();
        return instance;
    }

    /** 测试专用：丢弃单例，配合 NoteDatabase.resetForTest() 使用 */
    public static synchronized void resetForTest() {
        instance = null;
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (Prefs.dataReady(context)) {
            return;
        }
        if (!importLegacyJson()) {
            seed();
        }
        Prefs.setDataReady(context, true);
    }

    /** 旧版 JSON 文件迁移：成功导入返回 true（无论条数多少） */
    private boolean importLegacyJson() {
        File f = new File(context.getFilesDir(), LEGACY_FILE);
        if (!f.exists()) {
            return false;
        }
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(f));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } finally {
                reader.close();
            }
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                Note n = new Note();
                n.id = o.optLong("id");
                n.title = o.optString("title", "");
                n.body = o.optString("body", "");
                n.createdAt = o.optLong("createdAt");
                n.mood = o.isNull("mood") ? null : o.optString("mood", null);
                JSONArray tags = o.optJSONArray("tags");
                if (tags != null) {
                    for (int j = 0; j < tags.length(); j++) {
                        String t = tags.optString(j, null);
                        if (t != null && t.length() > 0) {
                            n.tags.add(t);
                        }
                    }
                }
                dao.insert(toEntity(n));
            }
            f.renameTo(new File(context.getFilesDir(), LEGACY_FILE + ".migrated"));
            Log.i(TAG, "旧版 JSON 已导入 Room：" + arr.length() + " 条");
            return true;
        } catch (Exception e) {
            Log.w(TAG, "旧版 JSON 迁移失败，改用示例数据", e);
            return false;
        }
    }

    // ------------------------------------------------------------------ 查询

    public List<Note> all() {
        return toModels(dao.all());
    }

    public List<Note> byDay(long dayTs) {
        long start = Dates.startOfDay(dayTs);
        return toModels(dao.between(start, start + DAY));
    }

    public List<Note> byMonth(int year, int monthZeroBased) {
        Calendar c = Calendar.getInstance();
        c.set(year, monthZeroBased, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();
        c.add(Calendar.MONTH, 1);
        return toModels(dao.between(start, c.getTimeInMillis()));
    }

    /** 某月哪些天有记录（0:00 时间戳集合），用于月历上的圆点 */    public Set<Long> daysWithNotes(int year, int monthZeroBased) {
        Calendar c = Calendar.getInstance();
        c.set(year, monthZeroBased, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        long start = c.getTimeInMillis();
        c.add(Calendar.MONTH, 1);
        Set<Long> days = new HashSet<>();
        for (Long ts : dao.createdAtBetween(start, c.getTimeInMillis())) {
            days.add(Dates.startOfDay(ts));
        }
        return days;
    }

    /** 关键词搜索（标题 / 正文 / 标签），空关键词返回空列表 */
    public List<Note> search(String keyword) {
        String q = keyword == null ? "" : keyword.trim();
        if (q.isEmpty()) {
            return new ArrayList<>();
        }
        // 用户输入的 % 和 _ 按普通字符匹配，不当通配符
        String escaped = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return toModels(dao.search(escaped));
    }

    /** 从 startDay 起连续 days 天，每天各自的记录条数（按时间正序） */
    public int[] dailyCounts(long startDay, int days) {
        int[] counts = new int[Math.max(days, 0)];
        Calendar c = Dates.cal(Dates.startOfDay(startDay));
        for (int i = 0; i < counts.length; i++) {
            long from = c.getTimeInMillis();
            c.add(Calendar.DAY_OF_MONTH, 1);
            counts[i] = dao.createdAtBetween(from, c.getTimeInMillis()).size();
        }
        return counts;
    }

    @Nullable
    public Note find(long id) {        NoteWithPhotos row = dao.byId(id);
        return row == null ? null : toModel(row);
    }

    public int totalCount() {
        return dao.count();
    }

    public int totalWords() {
        return sumWords(dao.allBodies());
    }

    public int wordsSince(long from) {
        return sumWords(dao.bodiesSince(from));
    }

    /** 连续记录天数（今天没写则从昨天往前算） */
    public int streakDays() {
        Set<Long> days = new HashSet<>();
        for (Long ts : dao.allCreatedAt()) {
            days.add(Dates.startOfDay(ts));
        }
        if (days.isEmpty()) {
            return 0;
        }
        long day = Dates.startOfDay(System.currentTimeMillis());
        if (!days.contains(day)) {
            day -= DAY;
            if (!days.contains(day)) {
                return 0;
            }
        }
        int count = 0;
        while (days.contains(day)) {
            count++;
            day -= DAY;
        }
        return count;
    }

    public int daysWithNotesSince(long from) {
        Set<Long> days = new HashSet<>();
        for (Long ts : dao.createdAtSince(from)) {
            days.add(Dates.startOfDay(ts));
        }
        return days.size();
    }

    /** 常用标签（含心情），按出现次数降序 */
    public Map<String, Integer> topTags(long since, int limit) {
        Map<String, Integer> counter = new HashMap<>();
        for (String mood : dao.moodsSince(since)) {
            bump(counter, mood);
        }
        for (String csv : dao.tagsSince(since)) {
            if (csv == null || csv.isEmpty()) {
                continue;
            }
            for (String t : csv.split(",")) {
                if (!t.isEmpty()) {
                    bump(counter, t);
                }
            }
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counter.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });
        Map<String, Integer> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : entries) {
            if (result.size() >= limit) {
                break;
            }
            result.put(e.getKey(), e.getValue());
        }
        return result;
    }

    // ------------------------------------------------------------------ 写入

    public Note create(String body) {
        Note n = new Note();
        newId(n);
        n.body = body == null ? "" : body;
        n.title = "";
        dao.insert(toEntity(n));
        return n;
    }

    public Note createEmpty() {
        Note n = new Note();
        newId(n);
        dao.insert(toEntity(n));
        return n;
    }

    public void save(Note note) {
        if (note == null) {
            return;
        }
        NoteEntity e = toEntity(note);
        if (dao.update(e) == 0) {
            dao.insert(e);
        }
        syncPhotos(note);
    }

    public void delete(long id) {
        NoteWithPhotos row = dao.byId(id);
        if (row == null) {
            return;
        }
        List<String> files = new ArrayList<>();
        if (row.photos != null) {
            for (NotePhotoEntity p : row.photos) {
                files.add(p.fileName);
            }
        }
        dao.deleteById(id);
        for (String f : files) {
            PhotoStore.delete(context, f);
        }
    }

    public void clearAll() {
        dao.clear();
        PhotoStore.clearAll(context);
    }

    public String exportText() {
        StringBuilder sb = new StringBuilder();
        for (Note n : all()) {
            sb.append(Dates.fullTime(n.createdAt)).append("\n");
            if (n.title != null && n.title.trim().length() > 0) {
                sb.append(n.title.trim()).append("\n");
            }
            sb.append(n.body == null ? "" : n.body).append("\n\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ 映射

    private static void newId(Note n) {
        n.id = System.currentTimeMillis() + (long) (Math.random() * 900);
        n.createdAt = n.id;
    }

    private static NoteEntity toEntity(Note n) {
        NoteEntity e = new NoteEntity();
        e.id = n.id;
        e.title = n.title == null ? "" : n.title;
        e.body = n.body == null ? "" : n.body;
        e.createdAt = n.createdAt;
        e.updatedAt = System.currentTimeMillis();
        e.mood = n.mood;
        e.tagsCsv = joinTags(n.tags);
        return e;
    }

    private static Note toModel(NoteWithPhotos row) {
        Note n = new Note();
        NoteEntity e = row.note;
        n.id = e.id;
        n.title = e.title == null ? "" : e.title;
        n.body = e.body == null ? "" : e.body;
        n.createdAt = e.createdAt;
        n.mood = e.mood;
        if (e.tagsCsv != null && !e.tagsCsv.isEmpty()) {
            Collections.addAll(n.tags, e.tagsCsv.split(","));
        }
        if (row.photos != null && !row.photos.isEmpty()) {
            List<NotePhotoEntity> ordered = new ArrayList<>(row.photos);
            Collections.sort(ordered, new Comparator<NotePhotoEntity>() {
                @Override
                public int compare(NotePhotoEntity a, NotePhotoEntity b) {
                    return Integer.compare(a.position, b.position);
                }
            });
            for (NotePhotoEntity p : ordered) {
                n.photos.add(p.fileName);
            }
        }
        return n;
    }

    private static List<Note> toModels(List<NoteWithPhotos> rows) {
        List<Note> result = new ArrayList<>(rows.size());
        for (NoteWithPhotos row : rows) {
            result.add(toModel(row));
        }
        return result;
    }

    /** 配图落库：先清理移除的文件，再整体重写顺序 */
    private void syncPhotos(Note note) {
        List<String> previous = dao.photoFilesOf(note.id);
        for (String f : previous) {
            if (!note.photos.contains(f)) {
                PhotoStore.delete(context, f);
            }
        }
        dao.deletePhotosOf(note.id);
        if (!note.photos.isEmpty()) {
            List<NotePhotoEntity> rows = new ArrayList<>();
            for (int i = 0; i < note.photos.size(); i++) {
                NotePhotoEntity p = new NotePhotoEntity();
                p.noteId = note.id;
                p.fileName = note.photos.get(i);
                p.position = i;
                rows.add(p);
            }
            dao.insertPhotos(rows);
        }
    }

    private static String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String t : tags) {
            if (t == null || t.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(t);
        }
        return sb.toString();
    }

    private static int sumWords(List<String> bodies) {
        int sum = 0;
        for (String b : bodies) {
            if (b != null) {
                sum += b.replaceAll("\\s", "").length();
            }
        }
        return sum;
    }

    private static void bump(Map<String, Integer> counter, String key) {
        Integer c = counter.get(key);
        counter.put(key, c == null ? 1 : c + 1);
    }

    // ------------------------------------------------------------------ 首次启动示例

    private void seed() {
        long now = System.currentTimeMillis();
        long today = Dates.startOfDay(now);

        addSeed(today + hours(9) + minutes(12), "", "早起煮了一壶茶，把上周没读完的书翻到最后三章。窗外起了雾。", "平静", null);
        addSeed(today + hours(14) + minutes(30), "", "楼下的桂花开了，风一过，整条街都是甜的。", "愉悦", new String[]{"桂花"});
        addSeed(today + hours(21) + minutes(6), "", "把今天的照片洗出来贴上，纸的味道很好闻。", "平静", new String[]{"散步"});

        long yesterday = today - DAY;
        addSeed(yesterday + hours(21) + minutes(12), "桂花开了", "风一过，整条街都是甜的。", null, new String[]{"散步"});
        addSeed(yesterday + hours(18) + minutes(5), "拆一箱旧书", "一本 1998 年的诗集，扉页上还留着别人的名字。", "怀念", new String[]{"读书"});
        addSeed(yesterday + hours(9) + minutes(12), "早起的三章书", "一壶茶，三章书，雾很厚。", null, new String[]{"读书"});

        String[] fillers = {
                "下班绕远路走回家，路灯刚亮起来。",
                "给窗台的薄荷浇了水，长出新叶了。",
                "听了半小时的老唱片，什么也没做。",
                "地铁上看见有人在读纸质书，忽然很高兴。",
                "随手画了一张草图，线条歪得很可爱。",
                "煮了一锅粥，等它慢慢变稠。",
                "把桌面收拾干净，心里也空出来一块。",
                "傍晚的风把窗帘吹起来了。",
                "写了一封不打算寄出去的信。"
        };
        for (int i = 2; i <= 11; i++) {
            long day = today - DAY * i;
            addSeed(day + hours(20) + minutes(10 + i), "", fillers[(i - 2) % fillers.length],
                    i % 3 == 0 ? "平静" : null, null);
        }
    }

    private void addSeed(long ts, String title, String body, String mood, String[] tags) {
        Note n = new Note();
        n.id = ts + (long) (Math.random() * 900);
        n.createdAt = ts;
        n.title = title;
        n.body = body;
        n.mood = mood;
        if (tags != null) {
            Collections.addAll(n.tags, tags);
        }
        dao.insert(toEntity(n));
    }

    private static long hours(int h) {
        return h * 60L * 60 * 1000;
    }

    private static long minutes(int m) {
        return m * 60L * 1000;
    }
}
