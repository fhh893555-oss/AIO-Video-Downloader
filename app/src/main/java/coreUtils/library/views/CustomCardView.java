package coreUtils.library.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.nextgen.R;

import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

public class CustomCardView extends FrameLayout {

    private int cardBackgroundColor = Color.WHITE;
    private float cornerRadius = 0f;
    private float cardElevation = 0f;
    private int shadowColor = Color.BLACK;
    private float maxShadowAlpha = 0.3f;

    private Paint cardPaint;
    private Paint shadowPaint;
    private Paint clearPaint;

    private Path cardPath;
    private Path shadowPath;
    private RectF cardRect;
    private RectF shadowRect;

    private Bitmap shadowBitmap;
    private Canvas shadowCanvas;

    private boolean needsShadowUpdate = true;

    public CustomCardView(Context context) {
        this(context, null);
    }

    public CustomCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setClipToPadding(false);
        setClipChildren(false);

        cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardPaint.setStyle(Paint.Style.FILL);

        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setStyle(Paint.Style.FILL);

        clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        cardPath = new Path();
        shadowPath = new Path();
        cardRect = new RectF();
        shadowRect = new RectF();

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.CustomCardView,
                    0, 0
            );

            try {
                cardBackgroundColor = a.getColor(
                        R.styleable.CustomCardView_cardBackgroundColor,
                        Color.WHITE
                );

                cornerRadius = a.getDimension(
                        R.styleable.CustomCardView_cardCornerRadius,
                        0f
                );

                cardElevation = a.getDimension(
                        R.styleable.CustomCardView_cardElevation,
                        0f
                );

                shadowColor = a.getColor(
                        R.styleable.CustomCardView_cardShadowColor,
                        Color.BLACK
                );

                maxShadowAlpha = a.getFloat(
                        R.styleable.CustomCardView_cardMaxShadowAlpha,
                        0.3f
                );
            } finally {
                a.recycle();
            }
        }

        cardPaint.setColor(cardBackgroundColor);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            updateCardBounds();
            generateShadow();
        }
    }

    private void updateCardBounds() {
        int shadowPadding = (int) (cardElevation * 2);

        cardRect.set(
                getPaddingLeft() + shadowPadding,
                getPaddingTop() + shadowPadding,
                getWidth() - getPaddingRight() - shadowPadding,
                getHeight() - getPaddingBottom() - shadowPadding
        );

        shadowRect.set(
                cardRect.left - cardElevation,
                cardRect.top - cardElevation,
                cardRect.right + cardElevation,
                cardRect.bottom + cardElevation * 2
        );

        cardPath.reset();
        cardPath.addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW);

        shadowPath.reset();
        shadowPath.addRoundRect(shadowRect, cornerRadius + cardElevation,
                cornerRadius + cardElevation, Path.Direction.CW);

        needsShadowUpdate = true;
    }

    private void generateShadow() {
        if (cardElevation <= 0) return;

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) return;

        if (shadowBitmap != null) {
            shadowBitmap.recycle();
        }

        shadowBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        shadowCanvas = new Canvas(shadowBitmap);

        // Draw shadow layers
        int layers = Math.min(5, Math.max(1, (int)(cardElevation / 2)));

        for (int i = 0; i < layers; i++) {
            float offset = (i + 1) * cardElevation * 0.2f;
            float alpha = maxShadowAlpha * (1f - (i * 0.15f)) / layers;
            int shadowAlpha = (int)(alpha * 255);

            shadowPaint.setColor(Color.argb(
                    shadowAlpha,
                    Color.red(shadowColor),
                    Color.green(shadowColor),
                    Color.blue(shadowColor)
            ));

            shadowPaint.setShadowLayer(
                    cardElevation * (1 + i * 0.5f),
                    offset,
                    offset + (i * cardElevation * 0.1f),
                    Color.argb(
                            shadowAlpha,
                            Color.red(shadowColor),
                            Color.green(shadowColor),
                            Color.blue(shadowColor)
                    )
            );

            shadowCanvas.drawRoundRect(
                    cardRect.left + offset * 0.5f,
                    cardRect.top + offset * 0.5f,
                    cardRect.right + offset * 0.5f,
                    cardRect.bottom + offset * 0.5f,
                    cornerRadius + i * 2,
                    cornerRadius + i * 2,
                    shadowPaint
            );
        }

        // Clear the card area from shadow bitmap
        shadowCanvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, clearPaint);
        needsShadowUpdate = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw shadow from bitmap
        if (shadowBitmap != null && !shadowBitmap.isRecycled()) {
            canvas.drawBitmap(shadowBitmap, 0, 0, null);
        }

        // Draw card background
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardPaint);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // Clip children to card bounds
        int saveCount = canvas.save();
        canvas.clipPath(cardPath);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(saveCount);
    }

    public void setCardBackgroundColor(int color) {
        cardBackgroundColor = color;
        cardPaint.setColor(color);
        invalidate();
    }

    public int getCardBackgroundColor() {
        return cardBackgroundColor;
    }

    public void setCardElevation(float elevation) {
        cardElevation = Math.max(0, elevation);
        updateCardBounds();
        generateShadow();
        invalidate();
    }

    public float getCardElevation() {
        return cardElevation;
    }

    public void setCornerRadius(float radius) {
        cornerRadius = Math.max(0, radius);
        updateCardBounds();
        generateShadow();
        invalidate();
    }

    public float getCornerRadius() {
        return cornerRadius;
    }

    public void setShadowColor(int color) {
        shadowColor = color;
        generateShadow();
        invalidate();
    }

    public void setMaxShadowAlpha(float alpha) {
        maxShadowAlpha = Math.max(0, Math.min(1, alpha));
        generateShadow();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (shadowBitmap != null && !shadowBitmap.isRecycled()) {
            shadowBitmap.recycle();
            shadowBitmap = null;
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        // Add extra padding for shadow
        int shadowPadding = (int) (cardElevation * 2);
        super.setPadding(
                left + shadowPadding,
                top + shadowPadding,
                right + shadowPadding,
                bottom + shadowPadding
        );
    }
}