package com.crazymen.freenote.ui.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * 支持双指缩放、双击放大、放大后拖动的 ImageView。
 *
 * 约定：
 * - 未放大时把事件让给外层 ViewPager，左右滑动可以直接切图；
 * - 放大后接管手势（requestDisallowInterceptTouchEvent），拖动查看细节；
 * - 缩回 1 倍时自动回正，避免图片被拖出屏幕。
 */
public class ZoomImageView extends AppCompatImageView {

    public interface OnTap {
        void onTap();
    }

    private static final float MAX_SCALE = 4f;
    private static final float DOUBLE_TAP_SCALE = 2.5f;
    /** 浮点误差容忍：1 倍附近都算「未放大」 */
    private static final float IDENTITY_SLOP = 1.05f;

    private final Matrix baseMatrix = new Matrix();
    private final Matrix suppMatrix = new Matrix();
    private final Matrix drawMatrix = new Matrix();
    private final float[] values = new float[9];
    private final RectF mapped = new RectF();

    private ScaleGestureDetector scaleDetector;
    private GestureDetector tapDetector;
    private OnTap onTap;

    private float lastX;
    private float lastY;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(getContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float factor = detector.getScaleFactor();
                        suppMatrix.postScale(factor, factor,
                                detector.getFocusX(), detector.getFocusY());
                        clampScale(detector.getFocusX(), detector.getFocusY());
                        applyMatrix();
                        return true;
                    }
                });

        tapDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (currentScale() > IDENTITY_SLOP) {
                    resetZoom();
                } else {
                    zoomTo(DOUBLE_TAP_SCALE, e.getX(), e.getY());
                }
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (onTap != null) {
                    onTap.onTap();
                }
                return true;
            }
        });
    }

    public void setOnTapListener(OnTap listener) {
        this.onTap = listener;
    }

    // ------------------------------------------------------------------ 矩阵

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        updateBaseMatrix();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateBaseMatrix();
    }

    /** 按 CONTENT_FIT 方式算出基准矩阵（图片居中、完整可见） */
    private void updateBaseMatrix() {
        Drawable d = getDrawable();
        if (d == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        float dw = d.getIntrinsicWidth();
        float dh = d.getIntrinsicHeight();
        if (dw <= 0 || dh <= 0) {
            return;
        }
        float scale = Math.min(getWidth() / dw, getHeight() / dh);
        baseMatrix.reset();
        baseMatrix.postScale(scale, scale);
        baseMatrix.postTranslate((getWidth() - dw * scale) / 2f,
                (getHeight() - dh * scale) / 2f);
        suppMatrix.reset();
        applyMatrix();
    }

    private void applyMatrix() {
        drawMatrix.set(baseMatrix);
        drawMatrix.postConcat(suppMatrix);
        setImageMatrix(drawMatrix);
        invalidate();
    }

    private float currentScale() {
        suppMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    /** 限制缩放范围，并把图片拉回可视区域 */
    private void clampScale(float focusX, float focusY) {
        float scale = currentScale();
        if (scale <= 1f) {
            suppMatrix.reset();
        } else if (scale > MAX_SCALE) {
            float factor = MAX_SCALE / scale;
            suppMatrix.postScale(factor, factor, focusX, focusY);
        }
        fixTranslation();
    }

    private void fixTranslation() {
        Drawable d = getDrawable();
        if (d == null || getWidth() == 0) {
            return;
        }
        drawMatrix.set(baseMatrix);
        drawMatrix.postConcat(suppMatrix);
        mapped.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        drawMatrix.mapRect(mapped);

        float dx = 0f;
        float dy = 0f;
        if (mapped.width() <= getWidth()) {
            dx = (getWidth() - mapped.width()) / 2f - mapped.left;
        } else if (mapped.left > 0) {
            dx = -mapped.left;
        } else if (mapped.right < getWidth()) {
            dx = getWidth() - mapped.right;
        }

        if (mapped.height() <= getHeight()) {
            dy = (getHeight() - mapped.height()) / 2f - mapped.top;
        } else if (mapped.top > 0) {
            dy = -mapped.top;
        } else if (mapped.bottom < getHeight()) {
            dy = getHeight() - mapped.bottom;
        }

        if (dx != 0f || dy != 0f) {
            suppMatrix.postTranslate(dx, dy);
        }
    }

    private void zoomTo(float target, float focusX, float focusY) {
        float factor = target / currentScale();
        suppMatrix.postScale(factor, factor, focusX, focusY);
        clampScale(focusX, focusY);
        applyMatrix();
    }

    /** 回到 1 倍居中（翻页、双击还原都走这里） */
    public void resetZoom() {
        suppMatrix.reset();
        applyMatrix();
        disallowParentIntercept(false);
    }

    public boolean isZoomed() {
        return currentScale() > IDENTITY_SLOP;
    }

    /** 让/不让外层 ViewPager 抢走横向手势（View 本身没有这个方法，要问父容器） */
    private void disallowParentIntercept(boolean disallow) {
        android.view.ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
        }
    }

    // ------------------------------------------------------------------ 手势

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        tapDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                if (isZoomed()) {
                    disallowParentIntercept(true);
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // 双指缩放时不切换页面
                disallowParentIntercept(true);
                break;

            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress()) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    if (isZoomed()) {
                        suppMatrix.postTranslate(dx, dy);
                        fixTranslation();
                        applyMatrix();
                        disallowParentIntercept(true);
                    }
                    lastX = event.getX();
                    lastY = event.getY();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!isZoomed()) {
                    disallowParentIntercept(false);
                }
                break;

            default:
                break;
        }
        return true;
    }
}
