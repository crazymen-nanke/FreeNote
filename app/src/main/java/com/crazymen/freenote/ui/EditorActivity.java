package com.crazymen.freenote.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.crazymen.freenote.R;
import com.crazymen.freenote.data.NoteStore;
import com.crazymen.freenote.model.Note;
import com.crazymen.freenote.util.Dates;
import com.crazymen.freenote.util.PhotoStore;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;

/** 02 写记录：沉浸编辑 + 自动保存 + 配图 */
public class EditorActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "note_id";
    public static final String EXTRA_PHOTO = "attach_photo";
    public static final String EXTRA_TAG = "ask_tag";
    /** 新建时指定记录所属的那一天（0 表示今天） */
    public static final String EXTRA_DAY = "note_day";

    private static final String[] MOODS = {"平静", "愉悦", "怀念", "疲惫"};

    private Note note;
    private EditText etTitle;
    private EditText etBody;
    private TextView tvMeta;
    private TextView tvAutosave;
    private TextView tvDraft;
    private LinearLayout tagRow;
    private HorizontalScrollView photoScroller;
    private LinearLayout photoRow;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<String> photoPicker;
    private ActivityResultLauncher<Uri> takePicture;
    /** 拍照时先建好的目标文件 Uri（系统相机把照片写进这里） */
    private Uri pendingCaptureUri;
    private boolean largeText;
    /** 已删除：阻止 onPause / 自动保存把这条记录又写回去 */
    private boolean deleted;

    private final Runnable autosaveTask = new Runnable() {
        @Override
        public void run() {
            saveDraft();
        }
    };

    public static void open(Activity activity, long noteId, boolean attachPhoto, boolean askTag) {
        Intent intent = new Intent(activity, EditorActivity.class);
        intent.putExtra(EXTRA_ID, noteId);
        intent.putExtra(EXTRA_PHOTO, attachPhoto);
        intent.putExtra(EXTRA_TAG, askTag);
        activity.startActivity(intent);
    }

    /** 补写在指定日期上（时光页选了一个空日子时用） */
    public static void openOnDay(Activity activity, long dayStart) {
        Intent intent = new Intent(activity, EditorActivity.class);
        intent.putExtra(EXTRA_ID, 0L);
        intent.putExtra(EXTRA_DAY, Dates.startOfDay(dayStart));
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        long id = getIntent().getLongExtra(EXTRA_ID, 0L);
        Note existing = id == 0L ? null : NoteStore.get(this).find(id);
        final boolean isNew = existing == null;
        if (isNew) {
            note = new Note();
            long day = getIntent().getLongExtra(EXTRA_DAY, 0L);
            if (day != 0L) {
                // 补写：日期用选中的那一天，时分沿用此刻，保证记录落在那一天
                Calendar now = Calendar.getInstance();
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(day);
                c.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
                c.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                note.createdAt = c.getTimeInMillis();
                note.id = note.createdAt + (long) (Math.random() * 900);
            } else {
                note.id = System.currentTimeMillis();
                note.createdAt = note.id;
            }
        } else {
            note = existing;
        }

        photoPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            addPhoto(uri);
                        }
                    }
                });

        takePicture = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean saved) {
                        Uri uri = pendingCaptureUri;
                        pendingCaptureUri = null;
                        if (saved != null && saved && uri != null) {
                            addPhoto(uri);
                        }
                    }
                });

        etTitle = findViewById(R.id.etTitle);
        etBody = findViewById(R.id.etBody);
        tvMeta = findViewById(R.id.tvMeta);
        tvAutosave = findViewById(R.id.tvAutosave);
        tvDraft = findViewById(R.id.tvDraft);
        tagRow = findViewById(R.id.tagRow);
        photoScroller = findViewById(R.id.photoScroller);
        photoRow = findViewById(R.id.photoRow);

        etTitle.setText(note.title);
        etBody.setText(note.body);
        if (note.body != null) {
            etBody.setSelection(note.body.length());
        }
        tvMeta.setText(Dates.fullTime(note.createdAt));
        tvAutosave.setText(String.format(Locale.CHINA, "已自动保存 · %d 字", note.wordCount()));
        tvDraft.setText(isNew ? "新记录 · 自动保存" : "草稿 · 自动保存");

        renderPhotos();

        boolean attachPhoto = getIntent().getBooleanExtra(EXTRA_PHOTO, false);
        if (attachPhoto) {
            photoRow.post(new Runnable() {
                @Override
                public void run() {
                    showPhotoSource();
                }
            });
        }

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tvDraft.setText("编辑中 · 自动保存");
                handler.removeCallbacks(autosaveTask);
                handler.postDelayed(autosaveTask, 700);
            }
        };
        etTitle.addTextChangedListener(watcher);
        etBody.addTextChangedListener(watcher);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDraft();
                finish();
            }
        });

        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEmpty()) {
                    Toast.makeText(EditorActivity.this, "写点什么再保存吧", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveDraft();
                Toast.makeText(EditorActivity.this, "已保存", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        findViewById(R.id.toolPhoto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPhotoSource();
            }
        });

        findViewById(R.id.toolList).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int start = Math.max(etBody.getSelectionStart(), 0);
                etBody.getText().insert(start, "• ");
                etBody.requestFocus();
            }
        });

        // 直接弹删除确认：只有一步，不再先经过操作菜单
        findViewById(R.id.btnDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });

        toolText().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                largeText = !largeText;
                etBody.setTextSize(largeText ? 17f : 15f);
                toolText().setBackgroundResource(
                        largeText ? R.drawable.bg_chip_primary : R.drawable.bg_icon_btn_soft);
                Toast.makeText(EditorActivity.this,
                        largeText ? "正文字号：大" : "正文字号：标准", Toast.LENGTH_SHORT).show();
            }
        });

        renderTags();

        if (isNew && note.body.length() == 0) {
            etBody.requestFocus();
        }

        if (getIntent().getBooleanExtra(EXTRA_TAG, false)) {
            tagRow.post(new Runnable() {
                @Override
                public void run() {
                    showTagDialog();
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveDraft();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(autosaveTask);
        super.onDestroy();
    }

    // ------------------------------------------------------------------ 配图

    /** 「拍一张」还是「从相册选」——两条路径都落进同一条导入与持久化链路 */
    private void showPhotoSource() {
        AppDialog.actions(this, getString(R.string.photo_pick_title),
                new String[]{getString(R.string.photo_capture), getString(R.string.photo_gallery)},
                -1, new AppDialog.OnChoose() {
                    @Override
                    public void onChoose(int index) {
                        if (index == 0) {
                            capturePhoto();
                        } else {
                            photoPicker.launch("image/*");
                        }
                    }
                });
    }

    /** 用系统相机拍照：先建目标文件，再把这个文件的 content Uri 交给相机写入 */
    private void capturePhoto() {
        try {
            File dir = new File(getCacheDir(), "captures");
            if (!dir.exists() && !dir.mkdirs()) {
                Toast.makeText(this, "暂时无法拍照，请检查存储空间", Toast.LENGTH_SHORT).show();
                return;
            }
            cleanStaleCaptures(dir);
            File file = new File(dir, "cap_" + System.currentTimeMillis() + ".jpg");
            pendingCaptureUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
            takePicture.launch(pendingCaptureUri);
        } catch (Exception e) {
            pendingCaptureUri = null;
            Toast.makeText(this, "没有找到可用的相机", Toast.LENGTH_SHORT).show();
        }
    }

    /** 取消拍照会留下空文件，顺手清掉一天前的残留 */
    private void cleanStaleCaptures(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        long limit = System.currentTimeMillis() - 24L * 60 * 60 * 1000;
        for (File f : files) {
            if (f.isFile() && f.lastModified() < limit) {
                f.delete();
            }
        }
    }

    private void addPhoto(Uri uri) {
        Toast.makeText(this, "正在读取图片…", Toast.LENGTH_SHORT).show();
        final boolean fromCamera = "file".equals(uri.getScheme())
                || uri.toString().contains("/captures/");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String name = PhotoStore.importFromUri(EditorActivity.this, uri);
                // 相机产生的原图只是临时原料，导入后即删
                if (fromCamera && "content".equals(uri.getScheme())) {
                    deleteCapture(uri);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (name == null) {
                            Toast.makeText(EditorActivity.this,
                                    "这张图片读取失败，换一张 JPG / PNG 试试",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        note.photos.add(name);
                        renderPhotos();
                        scheduleAutosave();
                        Toast.makeText(EditorActivity.this,
                                fromCamera ? "照片已添加" : "已加入配图", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }, "photo-import").start();
    }

    /** 相机写进 cacheDir/captures 的原图用完就清掉，避免缓存越攒越大 */
    private void deleteCapture(Uri uri) {
        try {
            File dir = new File(getCacheDir(), "captures");
            String path = uri.getPath();
            if (path == null) {
                return;
            }
            File target = new File(path);
            // FileProvider 的 path 形如 /captures/xxx.jpg，直接用文件名定位到自己的目录
            File file = new File(dir, target.getName());
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception ignored) {
            // 清理失败不影响记录本身
        }
    }

    private void removePhoto(String name) {
        note.photos.remove(name);
        PhotoStore.delete(this, name);
        renderPhotos();
        scheduleAutosave();
    }

    private void renderPhotos() {
        photoRow.removeAllViews();
        photoScroller.setVisibility(note.photos.isEmpty() ? View.GONE : View.VISIBLE);
        for (final String name : note.photos) {
            photoRow.addView(photoTile(name));
        }
        if (!note.photos.isEmpty()) {
            photoRow.addView(addPhotoTile());
        }
    }

    private View photoTile(final String name) {
        final int side = dp(96);
        FrameLayout tile = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(side, side);
        lp.rightMargin = dp(10);
        tile.setLayoutParams(lp);

        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        iv.setBackgroundResource(R.drawable.bg_photo_thumb);
        iv.setClipToOutline(true);
        Bitmap bmp = PhotoStore.load(this, name, side * 2);
        if (bmp != null) {
            RoundedBitmapDrawable rounded =
                    RoundedBitmapDrawableFactory.create(getResources(), bmp);
            rounded.setCornerRadius(dp(12));
            rounded.setAntiAlias(true);
            iv.setImageDrawable(rounded);
        }
        iv.setContentDescription(name);
        iv.setClickable(true);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 先看大图再决定删不删，比在小缩略图上判断可靠
                PhotoViewerActivity.open(EditorActivity.this, note.photos, note.photos.indexOf(name));
            }
        });
        tile.addView(iv);

        TextView badge = new TextView(this);
        badge.setText("×");
        badge.setTextSize(13f);
        badge.setGravity(Gravity.CENTER);
        badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        badge.setTextColor(ContextCompat.getColor(this, R.color.on_danger));
        badge.setBackgroundResource(R.drawable.bg_photo_remove);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(dp(22), dp(22));
        blp.gravity = Gravity.TOP | Gravity.END;
        blp.rightMargin = dp(5);
        blp.topMargin = dp(5);
        badge.setLayoutParams(blp);
        badge.setClickable(true);
        badge.setFocusable(true);
        badge.setContentDescription(getString(R.string.cd_remove_photo));
        badge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removePhoto(name);
            }
        });
        tile.addView(badge);
        return tile;
    }

    private View addPhotoTile() {
        final int side = dp(96);
        FrameLayout tile = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(side, side);
        tile.setLayoutParams(lp);
        tile.setBackgroundResource(R.drawable.bg_photo_add);
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setContentDescription(getString(R.string.action_add_photo));
        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPhotoSource();
            }
        });

        TextView plus = new TextView(this);
        plus.setText("＋");
        plus.setTextSize(20f);
        plus.setGravity(Gravity.CENTER);
        plus.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        tile.addView(plus, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        return tile;
    }

    private void scheduleAutosave() {
        tvDraft.setText("编辑中 · 自动保存");
        handler.removeCallbacks(autosaveTask);
        handler.postDelayed(autosaveTask, 400);
    }

    // ------------------------------------------------------------------ 保存

    private View toolText() {
        return findViewById(R.id.toolText);
    }

    private boolean isEmpty() {
        return etTitle.getText().toString().trim().length() == 0
                && etBody.getText().toString().trim().length() == 0
                && note.photos.isEmpty();
    }

    private void saveDraft() {
        if (deleted) {
            return;
        }
        String title = etTitle.getText().toString().trim();
        String body = etBody.getText().toString().trim();
        if (title.length() == 0 && body.length() == 0 && note.photos.isEmpty()) {
            return;
        }
        note.title = title;
        note.body = body;
        NoteStore.get(this).save(note);
        tvAutosave.setText(String.format(Locale.CHINA, "已自动保存 · %d 字", note.wordCount()));
        tvDraft.setText("草稿 · 自动保存");
    }

    private void renderTags() {
        tagRow.removeAllViews();

        if (note.mood != null && note.mood.length() > 0) {
            TextView moodChip = makeChip(note.mood, R.drawable.bg_chip_primary, R.color.primary);
            moodChip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMoodDialog();
                }
            });
            tagRow.addView(moodChip);
        } else {
            TextView addMood = makeChip("＋ 心情", R.drawable.bg_chip_sand, R.color.text_tertiary);
            addMood.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMoodDialog();
                }
            });
            tagRow.addView(addMood);
        }

        for (final String tag : note.tags) {
            TextView chip = makeChip(tag, R.drawable.bg_chip_sand, R.color.text_secondary);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppDialog.confirm(EditorActivity.this,
                            "移除标签？",
                            "「" + tag + "」会从这条记录上移除，记录本身不受影响。",
                            "移除", false, new AppDialog.OnConfirm() {
                                @Override
                                public void run() {
                                    note.tags.remove(tag);
                                    renderTags();
                                }
                            });
                }
            });
            tagRow.addView(chip);
        }

        View addBtn = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(26), dp(26));
        lp.rightMargin = dp(8);
        addBtn.setLayoutParams(lp);
        addBtn.setBackgroundResource(R.drawable.bg_tag_add);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTagDialog();
            }
        });
        tagRow.addView(addBtn);
    }

    private TextView makeChip(String text, int bgRes, int colorRes) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(11f);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        chip.setTextColor(ContextCompat.getColor(this, colorRes));
        chip.setBackgroundResource(bgRes);
        chip.setPadding(dp(10), dp(5), dp(10), dp(5));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        chip.setLayoutParams(lp);
        return chip;
    }

    private void showTagDialog() {
        AppDialog.input(this, "添加标签", null, "例如：散步、读书、桂花", "", "添加",
                new AppDialog.OnText() {
                    @Override
                    public void onText(String tag) {
                        if (!note.tags.contains(tag)) {
                            note.tags.add(tag);
                            renderTags();
                        }
                    }
                });
    }

    private void showMoodDialog() {
        final String[] items = new String[MOODS.length + 1];
        System.arraycopy(MOODS, 0, items, 0, MOODS.length);
        items[MOODS.length] = "清除心情";

        int selected = -1;
        for (int i = 0; i < MOODS.length; i++) {
            if (MOODS[i].equals(note.mood)) {
                selected = i;
                break;
            }
        }
        AppDialog.list(this, "此刻的心情", items, selected, new AppDialog.OnChoose() {
            @Override
            public void onChoose(int index) {
                note.mood = index >= MOODS.length ? null : MOODS[index];
                renderTags();
            }
        });
    }

    private void confirmDelete() {
        AppDialog.confirm(this, "删除这条记录？", "删除后无法恢复。", "删除", true,
                new AppDialog.OnConfirm() {
                    @Override
                    public void run() {
                        deleteNote();
                    }
                });
    }

    private void deleteNote() {
        // 先立起删除标记：finish() 触发的 onPause 不能再把记录写回去
        deleted = true;
        handler.removeCallbacks(autosaveTask);
        NoteStore.get(this).delete(note.id);
        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
        finish();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
