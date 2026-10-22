package com.crazymen.freenote.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.crazymen.freenote.R;
import com.crazymen.freenote.data.NoteStore;
import com.crazymen.freenote.model.Note;
import com.crazymen.freenote.util.Dates;
import com.crazymen.freenote.util.PhotoStore;

/**
 * 记录详情：先读，再决定要不要改。
 *
 * 列表点进来是「看」这条记录，而不是直接落到编辑态；想改再点右上角的笔。
 * 配图可点开全屏，支持缩放与左右切换（{@link PhotoViewerActivity}）。
 */
public class NoteDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "note_id";

    private Note note;
    private TextView tvDate;
    private TextView tvTitle;
    private TextView tvTime;
    private TextView tvWords;
    private TextView tvBody;
    private TextView tvPhotoCount;
    private LinearLayout chipRow;
    private LinearLayout photoList;

    public static void open(Activity activity, long noteId) {
        Intent intent = new Intent(activity, NoteDetailActivity.class);
        intent.putExtra(EXTRA_ID, noteId);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        tvDate = findViewById(R.id.tvDetailDate);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvTime = findViewById(R.id.tvDetailTime);
        tvWords = findViewById(R.id.tvDetailWords);
        tvBody = findViewById(R.id.tvDetailBody);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);
        chipRow = findViewById(R.id.chipRow);
        photoList = findViewById(R.id.photoList);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.btnEdit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit();
            }
        });
        findViewById(R.id.btnMore).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMore();
            }
        });

        if (!load()) {
            finish();
            return;
        }
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从编辑页返回后可能是改了内容，也可能被删除
        if (load()) {
            render();
        } else {
            finish();
        }
    }

    /** 读取记录；不存在返回 false */
    private boolean load() {
        long id = getIntent().getLongExtra(EXTRA_ID, 0L);
        note = id == 0L ? null : NoteStore.get(this).find(id);
        return note != null;
    }

    private void render() {
        if (note == null) {
            return;
        }
        String title = note.title == null ? "" : note.title.trim();
        if (title.length() == 0) {
            tvTitle.setVisibility(View.GONE);
        } else {
            tvTitle.setVisibility(View.VISIBLE);
            tvTitle.setText(title);
        }

        tvDate.setText(Dates.dayLabel(note.createdAt));
        tvTime.setText(Dates.time(note.createdAt));
        tvWords.setText(getString(R.string.detail_words, note.wordCount()));

        String body = note.body == null ? "" : note.body.trim();
        if (body.length() == 0) {
            tvBody.setText(note.hasPhoto() ? R.string.detail_empty_body : R.string.detail_empty_note);
            tvBody.setTextColor(ContextCompat.getColor(this, R.color.text_ghost));
        } else {
            tvBody.setText(body);
            tvBody.setTextColor(ContextCompat.getColor(this, R.color.ink_body));
        }

        renderChips();
        renderPhotos();
    }

    private void renderChips() {
        chipRow.removeAllViews();
        if (note.mood != null && note.mood.trim().length() > 0) {
            chipRow.addView(chip(note.mood, R.drawable.bg_chip_primary, R.color.primary));
        }
        for (String tag : note.tags) {
            if (tag == null || tag.length() == 0) {
                continue;
            }
            chipRow.addView(chip(tag, R.drawable.bg_chip_sand, R.color.text_secondary));
        }
        chipRow.setVisibility(chipRow.getChildCount() == 0 ? View.GONE : View.VISIBLE);
    }

    private TextView chip(String text, int bgRes, int colorRes) {
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

    private void renderPhotos() {
        photoList.removeAllViews();
        if (!note.hasPhoto()) {
            tvPhotoCount.setVisibility(View.GONE);
            return;
        }
        boolean more = note.photos.size() > 1;
        tvPhotoCount.setVisibility(more ? View.VISIBLE : View.GONE);
        if (more) {
            tvPhotoCount.setText(getString(R.string.detail_photo_count, note.photos.size()));
        }

        for (int i = 0; i < note.photos.size(); i++) {
            final int index = i;
            photoList.addView(photoCard(note.photos.get(i), index));
        }
    }

    private View photoCard(final String fileName, final int index) {
        final ImageView iv = new ImageView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(220));
        lp.bottomMargin = dp(10);
        iv.setLayoutParams(lp);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackgroundResource(R.drawable.bg_photo_thumb);
        iv.setClipToOutline(true);
        iv.setClickable(true);
        iv.setFocusable(true);
        iv.setContentDescription(getString(R.string.cd_photo));

        Bitmap bmp = PhotoStore.load(this, fileName, 1024);
        if (bmp != null) {
            RoundedBitmapDrawable rounded = RoundedBitmapDrawableFactory.create(getResources(), bmp);
            rounded.setCornerRadius(dp(14));
            rounded.setAntiAlias(true);
            iv.setImageDrawable(rounded);
        }
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhotoViewerActivity.open(NoteDetailActivity.this, note.photos, index);
            }
        });
        return iv;
    }

    private void edit() {
        if (note == null) {
            return;
        }
        EditorActivity.open(this, note.id, false, false);
    }

    private void showMore() {
        final String[] items = {getString(R.string.detail_action_edit),
                getString(R.string.detail_action_delete)};
        AppDialog.actions(this, note == null ? "" : note.displayTitle(), items, 1,
                new AppDialog.OnChoose() {
                    @Override
                    public void onChoose(int index) {
                        if (index == 0) {
                            edit();
                        } else {
                            confirmDelete();
                        }
                    }
                });
    }

    private void confirmDelete() {
        AppDialog.confirm(this, "删除这条记录？", "删除后无法恢复。", "删除", true,
                new AppDialog.OnConfirm() {
                    @Override
                    public void run() {
                        if (note == null) {
                            return;
                        }
                        NoteStore.get(NoteDetailActivity.this).delete(note.id);
                        Toast.makeText(NoteDetailActivity.this,
                                R.string.detail_deleted, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
