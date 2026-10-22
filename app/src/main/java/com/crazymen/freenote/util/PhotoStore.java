package com.crazymen.freenote.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 记录配图的落盘与读取。
 *
 * 持久化策略：
 * 1. 图片压缩后存入应用私有目录 filesDir/notes_photos/，数据库只存文件名（换机/恢复后路径不失效）；
 * 2. 导入时先把 Uri 内容复制到 cacheDir 临时文件，再从文件解码——部分相册/文档提供方的流不可重复读，
 *    直接反复 openInputStream 会解码失败；
 * 3. 解码后按 EXIF 摆正、限边 1600px、JPEG 落盘；读取按目标尺寸采样并带一层 LruCache。
 */
public final class PhotoStore {

    private static final String TAG = "PhotoStore";
    private static final String DIR_NAME = "notes_photos";
    private static final int MAX_SIDE = 1600;
    private static final int JPEG_QUALITY = 86;

    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(
            (int) (Runtime.getRuntime().maxMemory() / 8)) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private PhotoStore() {
    }

    public static File dir(Context context) {
        File dir = new File(context.getFilesDir(), DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static boolean exists(Context context, String fileName) {
        return fileName != null && new File(dir(context), fileName).exists();
    }

    /**
     * 把相册/文档提供的图片导入私有目录，返回文件名；失败返回 null。
     */
    @Nullable
    public static String importFromUri(Context context, Uri uri) {
        Bitmap src = decodeScaled(context, uri, MAX_SIDE);
        if (src == null) {
            Log.w(TAG, "图片解码失败：" + uri);
            return null;
        }
        String fileName = newFileName();
        boolean ok = writeJpeg(context, src, fileName);
        src.recycle();
        if (!ok) {
            Log.w(TAG, "图片落盘失败：" + fileName);
            return null;
        }
        CACHE.evictAll();
        return fileName;
    }

    /**
     * 从 Uri 读取位图，最长边不超过 maxSide。
     *
     * 先复制到临时文件再解码：既能避免提供方流不可重复读，也能让 EXIF 直接从文件解析。
     */
    @Nullable
    public static Bitmap decodeScaled(Context context, Uri uri, int maxSide) {
        if (context == null || uri == null) {
            return null;
        }
        File temp = null;
        try {
            temp = File.createTempFile("import_", ".img", context.getCacheDir());
            if (!copyToFile(context, uri, temp)) {
                return null;
            }

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(temp.getAbsolutePath(), bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide);
            Bitmap bitmap = BitmapFactory.decodeFile(temp.getAbsolutePath(), opts);
            if (bitmap == null) {
                return null;
            }

            Bitmap upright = rotateByExif(temp.getAbsolutePath(), bitmap);
            Bitmap limited = limitSide(upright, maxSide);
            if (limited != upright) {
                upright.recycle();
            }
            return limited;
        } catch (OutOfMemoryError e) {
            Log.w(TAG, "图片过大，内存不足", e);
            return null;
        } catch (Exception e) {
            Log.w(TAG, "读取图片失败：" + uri, e);
            return null;
        } finally {
            if (temp != null) {
                // 临时文件只用于本次解码，用完即删
                temp.delete();
            }
        }
    }

    /** 直接把 Bitmap 落盘为一张配图（供测试与未来的粘贴/截图功能复用） */
    @Nullable
    public static String importBitmap(Context context, Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Bitmap limited = limitSide(bitmap, MAX_SIDE);
        String fileName = newFileName();
        boolean ok = writeJpeg(context, limited, fileName);
        if (limited != bitmap) {
            limited.recycle();
        }
        if (ok) {
            CACHE.evictAll();
        }
        return ok ? fileName : null;
    }

    /** 按目标尺寸采样读取，命中缓存直接返回 */
    public static Bitmap load(Context context, String fileName, int targetPx) {
        if (fileName == null || fileName.length() == 0) {
            return null;
        }
        String key = fileName + "@" + targetPx;
        Bitmap cached = CACHE.get(key);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }
        File file = new File(dir(context), fileName);
        if (!file.exists()) {
            return null;
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, targetPx * 2);
        Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        if (bmp != null) {
            CACHE.put(key, bmp);
        }
        return bmp;
    }

    public static void delete(Context context, String fileName) {
        if (fileName == null || fileName.length() == 0) {
            return;
        }
        new File(dir(context), fileName).delete();
        CACHE.evictAll();
    }

    /** 清空全部图片文件，返回删除的文件名（用于核对） */
    public static List<String> clearAll(Context context) {
        List<String> removed = new ArrayList<>();
        File[] files = dir(context).listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.delete()) {
                    removed.add(f.getName());
                }
            }
        }
        CACHE.evictAll();
        return removed;
    }

    // ------------------------------------------------------------------ 内部

    private static String newFileName() {
        return "p_" + System.currentTimeMillis() + "_"
                + (int) (Math.random() * 9000 + 1000) + ".jpg";
    }

    private static boolean writeJpeg(Context context, Bitmap bitmap, String fileName) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(dir(context), fileName));
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            out.flush();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            closeQuietly(out);
        }
    }

    /** 把 Uri 的内容整段复制到本地文件；失败或内容为空返回 false */
    private static boolean copyToFile(Context context, Uri uri, File target) {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            if (in == null) {
                return false;
            }
            out = new FileOutputStream(target);
            byte[] buffer = new byte[64 * 1024];
            long total = 0L;
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                total += read;
            }
            out.flush();
            return total > 0L;
        } catch (Exception e) {
            Log.w(TAG, "复制图片内容失败：" + uri, e);
            return false;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    /** 采样率：按长边算，避免超大图整张解进内存 */
    private static int sampleSize(int width, int height, int target) {
        int sample = 1;
        int longest = Math.max(width, height);
        while (longest / (sample * 2) >= target) {
            sample *= 2;
        }
        return sample;
    }

    private static Bitmap rotateByExif(String path, Bitmap src) {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try {
            ExifInterface exif = new ExifInterface(path);
            orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Exception ignored) {
            // 没有 EXIF 或格式不支持时按原图处理
        }

        int degrees = 0;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                degrees = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degrees = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degrees = 270;
                break;
            default:
                break;
        }
        if (degrees == 0) {
            return src;
        }
        Matrix m = new Matrix();
        m.postRotate(degrees);
        Bitmap rotated = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
        if (rotated != src) {
            src.recycle();
        }
        return rotated;
    }

    private static Bitmap limitSide(Bitmap src, int maxSide) {
        int max = Math.max(src.getWidth(), src.getHeight());
        if (max <= maxSide) {
            return src;
        }
        float ratio = maxSide / (float) max;
        int w = Math.max(1, Math.round(src.getWidth() * ratio));
        int h = Math.max(1, Math.round(src.getHeight() * ratio));
        return Bitmap.createScaledBitmap(src, w, h, true);
    }

    private static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }
}
