package com.crazymen.freenote.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.crazymen.freenote.R;
import com.crazymen.freenote.ui.widget.ZoomImageView;
import com.crazymen.freenote.util.PhotoStore;

import java.util.List;

/**
 * 全屏看图：黑底、左右滑动切换、双指缩放、双击放大，单击收起/唤出顶部栏。
 *
 * 只接收文件名数组（数据库里存的就是文件名），图片本体仍从应用私有目录读取。
 */
public class PhotoViewerActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTOS = "photo_names";
    public static final String EXTRA_INDEX = "photo_index";

    private ViewPager pager;
    private TextView tvCounter;
    private View topBar;
    private TextView tvHint;

    public static void open(Activity activity, List<String> photos, int index) {
        if (photos == null || photos.isEmpty()) {
            return;
        }
        Intent intent = new Intent(activity, PhotoViewerActivity.class);
        intent.putExtra(EXTRA_PHOTOS, photos.toArray(new String[0]));
        intent.putExtra(EXTRA_INDEX, Math.max(0, Math.min(index, photos.size() - 1)));
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);

        String[] photos = getIntent().getStringArrayExtra(EXTRA_PHOTOS);
        if (photos == null || photos.length == 0) {
            finish();
            return;
        }
        int index = getIntent().getIntExtra(EXTRA_INDEX, 0);

        pager = findViewById(R.id.pager);
        tvCounter = findViewById(R.id.tvCounter);
        topBar = findViewById(R.id.viewerTop);
        tvHint = findViewById(R.id.tvViewerHint);

        pager.setAdapter(new PhotoAdapter(photos));
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateCounter(position, photos.length);
            }
        });
        pager.setCurrentItem(index, false);
        // setCurrentItem 在相同位置时不回调，这里主动刷一次
        updateCounter(pager.getCurrentItem(), photos.length);

        findViewById(R.id.btnClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        fadeHintLater();
    }

    private void updateCounter(int position, int total) {
        tvCounter.setText(getString(R.string.viewer_counter, position + 1, total));
        // 翻页后把各页的缩放状态收回去，避免带着上一张的倍数进来
        for (int i = 0; i < pager.getChildCount(); i++) {
            View child = pager.getChildAt(i);
            if (child instanceof ZoomImageView) {
                ((ZoomImageView) child).resetZoom();
            }
        }
    }

    private void toggleChrome() {
        boolean visible = topBar.getAlpha() > 0.5f;
        topBar.animate().alpha(visible ? 0f : 1f).setDuration(160).start();
        tvHint.animate().alpha(0f).setDuration(160).start();
    }

    private void fadeHintLater() {
        tvHint.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && tvHint != null) {
                    tvHint.animate().alpha(0f).setDuration(400).start();
                }
            }
        }, 2600);
    }

    /** 解码目标：按屏幕宽度取整，避免整张大图进内存 */
    private int targetPx() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return Math.max(dm.widthPixels, dm.heightPixels);
    }

    private class PhotoAdapter extends PagerAdapter {

        private final String[] photos;

        PhotoAdapter(String[] photos) {
            this.photos = photos;
        }

        @Override
        public int getCount() {
            return photos.length;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            ZoomImageView iv = new ZoomImageView(PhotoViewerActivity.this);
            iv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setTag(position);
            iv.setOnTapListener(new ZoomImageView.OnTap() {
                @Override
                public void onTap() {
                    toggleChrome();
                }
            });
            Bitmap bmp = PhotoStore.load(PhotoViewerActivity.this, photos[position], targetPx());
            if (bmp != null) {
                iv.setImageBitmap(bmp);
            }
            iv.setScaleType(ImageView.ScaleType.MATRIX);
            container.addView(iv);
            return iv;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }
    }
}
