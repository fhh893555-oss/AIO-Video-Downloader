package coreUtils.library.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.List;

public class CarouselImageView extends View {

    private final List<Bitmap> bitmaps = new ArrayList<>();
    private final List<String> imageUrls = new ArrayList<>();

    private final Paint imagePaint = new Paint(
            Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int activeDotColor = Color.parseColor("#555555");
    private final int inactiveDotColor = Color.parseColor("#CCCCCC");

    private static final long AUTO_SCROLL_DELAY = 4500;

    private int currentIndex = 0;
    private float scrollOffset = 0f;

    private final Scroller scroller;
    private final GestureDetector gestureDetector;
    private VelocityTracker velocityTracker;

    private float downX;
    private float lastX;
    private boolean isDragging = false;
    private final int touchSlop;
    private final int minFlingVelocity;

    private boolean autoScrollEnabled = true;
    private OnImageClickListener listener;

    public interface OnImageClickListener {
        void onClick(int index, String url);
    }

    public void setAutoScrollEnabled(boolean enabled) {
        autoScrollEnabled = enabled;
    }

    private final Runnable autoScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!autoScrollEnabled || bitmaps.size() <= 1) return;
            smoothScrollTo(currentIndex + 1);
            postDelayed(this, AUTO_SCROLL_DELAY);
        }
    };

    public CarouselImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        scroller = new Scroller(context, new DecelerateInterpolator());
        ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
        minFlingVelocity = vc.getScaledMinimumFlingVelocity();

        gestureDetector = new GestureDetector(
                context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                if (!isDragging && listener != null && !imageUrls.isEmpty()) {
                    listener.onClick(currentIndex, imageUrls.get(currentIndex));
                }
                return true;
            }
        });

        setClickable(true);
    }

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.listener = listener;
    }

    public void loadImages(List<String> urls) {
        if (urls == null || urls.isEmpty()) return;
        removeCallbacks(autoScrollRunnable);
        bitmaps.clear();
        imageUrls.clear();
        imageUrls.addAll(urls);
        currentIndex = 0;
        scrollOffset = 0f;
        invalidate();

        for (String url : urls) {
            Glide.with(getContext())
                    .asBitmap()
                    .load(url)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(
                                @NonNull Bitmap resource,
                                @Nullable Transition<? super Bitmap> transition) {
                            bitmaps.add(resource);
                            invalidate();
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {}
                    });
        }

        if (autoScrollEnabled) {
            postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY);
        }
    }

    private void smoothScrollTo(int index) {
        if (bitmaps.isEmpty()) return;
        int size = bitmaps.size();

        int targetIndex = index;
        if (targetIndex < 0) targetIndex = size - 1;
        else if (targetIndex >= size) targetIndex = 0;

        int startX = (int) (scrollOffset * 1000f);
        int endX = targetIndex * 1000;

        if (index >= size && scrollOffset > size - 1) {
            startX = (int) ((scrollOffset - size) * 1000f);
        } else if (index < 0 && scrollOffset < 1) {
            startX = (int) ((scrollOffset + size) * 1000f);
        }

        scroller.forceFinished(true);
        scroller.startScroll(startX, 0, endX - startX, 0, 350);
        currentIndex = targetIndex;
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollOffset = scroller.getCurrX() / 1000f;
            int size = Math.max(1, bitmaps.size());
            while (scrollOffset < 0f) scrollOffset += size;
            while (scrollOffset >= size) scrollOffset -= size;
            invalidate();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (bitmaps.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();
        int count = bitmaps.size();
        int centerIndex = Math.round(scrollOffset);

        for (int i = -1; i <= 1; i++) {
            int index = (centerIndex + i) % count;
            if (index < 0) index += count;

            float relative = index - scrollOffset;
            if (relative > count / 2f) relative -= count;
            if (relative < -count / 2f) relative += count;

            canvas.save();
            canvas.translate(relative * width, 0);
            drawCenterCropBitmap(canvas, bitmaps.get(index), width, height);
            canvas.restore();
        }
        drawDots(canvas, width, height);
    }

    private void drawCenterCropBitmap(Canvas canvas, Bitmap bitmap,
                                      float viewWidth, float viewHeight) {
        float bW = bitmap.getWidth();
        float bH = bitmap.getHeight();
        float scale = Math.max(viewWidth / bW, viewHeight / bH);
        float dx = (viewWidth - bW * scale) * 0.5f;
        float dy = (viewHeight - bH * scale) * 0.5f;

        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);
        canvas.drawBitmap(bitmap, matrix, imagePaint);
    }

    private void drawDots(Canvas canvas, float width, float height) {
        int count = bitmaps.size();
        if (count <= 1) return;
        float dotRadius = 5f;
        float spacing = 18f;
        float totalWidth = (count * (dotRadius * 2)) + ((count - 1) * spacing);
        float startX = (width - totalWidth) / 2f;

        for (int i = 0; i < count; i++) {
            float distance = Math.abs(i - scrollOffset);
            if (distance > count / 2f) distance = Math.abs(distance - count);

            dotPaint.setColor(distance < 1f ?
                    interpolateColor(inactiveDotColor,
                            activeDotColor, 1f - distance) : inactiveDotColor);
            canvas.drawCircle(startX + dotRadius, height - 35f, dotRadius, dotPaint);
            startX += (dotRadius * 2) + spacing;
        }
    }

    private int interpolateColor(int startColor, int endColor, float fraction) {
        float[] startHSV = new float[3], endHSV = new float[3];
        Color.colorToHSV(startColor, startHSV);
        Color.colorToHSV(endColor, endHSV);
        for (int i = 0; i < 3; i++)
            endHSV[i] =
                    startHSV[i] + (endHSV[i] - startHSV[i]) * fraction;
        return Color.HSVToColor(endHSV);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        if (bitmaps.size() <= 1) return true;

        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                removeCallbacks(autoScrollRunnable);
                if (!scroller.isFinished()) scroller.abortAnimation();
                downX = event.getX();
                lastX = downX;
                isDragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float currentX = event.getX();
                float dx = currentX - lastX;
                if (!isDragging && Math.abs(currentX - downX) > touchSlop) {
                    isDragging = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (isDragging) {
                    scrollOffset -= dx / getWidth();
                    int size = bitmaps.size();
                    while (scrollOffset < 0f) scrollOffset += size;
                    while (scrollOffset >= size) scrollOffset -= size;
                    invalidate();
                }
                lastX = currentX;
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    velocityTracker.computeCurrentVelocity(1000);
                    float xVelocity = velocityTracker.getXVelocity();
                    int size = bitmaps.size();
                    int targetIndex;

                    if (Math.abs(xVelocity) > minFlingVelocity) {
                        targetIndex = (xVelocity > 0) ?
                                (int) Math.floor(scrollOffset) :
                                (int) Math.ceil(scrollOffset);
                    } else {
                        targetIndex = Math.round(scrollOffset);
                    }
                    smoothScrollTo(targetIndex);
                }

                isDragging = false;
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }

                if (autoScrollEnabled) {
                    postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY);
                }

                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (autoScrollEnabled) {
            postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(autoScrollRunnable);
    }
}