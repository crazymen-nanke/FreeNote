package com.crazymen.freenote.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.crazymen.freenote.R;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 用户头像：存应用私有目录 avatar/avatar.jpg。
 * 支持从相册选择（居中裁方 + 压缩）与恢复默认。
 */
public final class AvatarStore {

    private static final int MAX_SIZE = 512;
    private static final int JPEG_QUALITY = 90;

    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(4) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private AvatarStore() {
    }

    public static File file(Context context) {
        File dir = new File(context.getFilesDir(), "avatar");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "avatar.jpg");
    }

    public static boolean has(Context context) {
        return file(context).exists();
    }

    /** 从相册选中的图片生成方形头像，成功返回 true */
    public static boolean saveFromUri(Context context, Uri uri) {
        Bitmap src = PhotoStore.decodeScaled(context, uri, MAX_SIZE);
        if (src == null) {
            return false;
        }
        try {
            // 居中裁方
            int side = Math.min(src.getWidth(), src.getHeight());
            int x = (src.getWidth() - side) / 2;
            int y = (src.getHeight() - side) / 2;
            Bitmap square = Bitmap.createBitmap(src, x, y, side, side);
            Bitmap scaled = side > MAX_SIZE
                    ? Bitmap.createScaledBitmap(square, MAX_SIZE, MAX_SIZE, true)
                    : square;

            boolean ok = writeJpeg(context, scaled);
            if (scaled != square) {
                scaled.recycle();
            }
            if (square != src) {
                square.recycle();
            }
            if (ok) {
                CACHE.evictAll();
            }
            return ok;
        } catch (Exception e) {
            return false;
        } finally {
            src.recycle();
        }
    }

    private static boolean writeJpeg(Context context, Bitmap bitmap) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file(context));
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            out.flush();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static Bitmap load(Context context, int sizePx) {
        File file = file(context);
        if (!file.exists()) {
            return null;
        }
        String key = "avatar@" + sizePx;
        Bitmap cached = CACHE.get(key);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 1;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        while (bounds.outWidth / (opts.inSampleSize * 2) >= sizePx) {
            opts.inSampleSize *= 2;
        }
        Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        if (bmp != null && bmp.getWidth() > sizePx * 2) {
            Matrix m = new Matrix();
            m.postScale(sizePx * 2f / bmp.getWidth(), sizePx * 2f / bmp.getHeight());
            Bitmap scaled = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            bmp.recycle();
            bmp = scaled;
        }
        if (bmp != null) {
            CACHE.put(key, bmp);
        }
        return bmp;
    }

    public static void clear(Context context) {
        file(context).delete();
        CACHE.evictAll();
    }

    /** 把头像绘制到 ImageView：有自定头像画圆形，否则回退到渐变底 */
    public static void apply(Context context, ImageView target, int sizePx) {
        Bitmap bmp = load(context, sizePx);
        if (bmp == null) {
            target.setImageDrawable(null);
            target.setBackgroundResource(R.drawable.bg_avatar);
            return;
        }
        target.setBackground(null);
        RoundedBitmapDrawable circle =
                RoundedBitmapDrawableFactory.create(context.getResources(), bmp);
        circle.setCircular(true);
        circle.setAntiAlias(true);
        target.setImageDrawable(circle);
    }
}
