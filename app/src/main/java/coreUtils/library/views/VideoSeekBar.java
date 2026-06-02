package coreUtils.library.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nextgen.R;

public class VideoSeekBar extends View {
	
	private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	
	private final RectF trackRect = new RectF();
	
	private float progress = 0f;
	private float bufferProgress = 0f;
	
	private float trackHeight;
	private float thumbRadius;
	private float thumbPadding;
	
	private int trackColor;
	private int bufferColor;
	private int progressColor;
	private int thumbColor;
	
	private OnProgressChangedListener listener;
	
	public VideoSeekBar(Context context) {
		super(context);
		init(null);
	}
	
	public VideoSeekBar(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}
	
	public VideoSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}
	
	private void init(@Nullable AttributeSet attrs) {
		
		trackHeight = dp(8);
		thumbRadius = dp(12);
		thumbPadding = dp(8);
		
		trackColor = Color.parseColor("#222222");
		bufferColor = Color.parseColor("#666666");
		progressColor = Color.WHITE;
		thumbColor = Color.WHITE;
		
		if (attrs != null) {
			
			TypedArray ta = getContext().obtainStyledAttributes(
				attrs,
				R.styleable.VideoSeekBar
			);
			
			trackHeight = ta.getDimension(
				R.styleable.VideoSeekBar_vsb_trackHeight,
				trackHeight
			);
			
			float thumbSize = ta.getDimension(
				R.styleable.VideoSeekBar_vsb_thumbSize,
				dp(24)
			);
			
			thumbRadius = thumbSize / 2f;
			
			thumbPadding = ta.getDimension(
				R.styleable.VideoSeekBar_vsb_thumbPadding,
				thumbPadding
			);
			
			trackColor = ta.getColor(
				R.styleable.VideoSeekBar_vsb_trackColor,
				trackColor
			);
			
			bufferColor = ta.getColor(
				R.styleable.VideoSeekBar_vsb_bufferColor,
				bufferColor
			);
			
			progressColor = ta.getColor(
				R.styleable.VideoSeekBar_vsb_progressColor,
				progressColor
			);
			
			thumbColor = ta.getColor(
				R.styleable.VideoSeekBar_vsb_thumbColor,
				thumbColor
			);
			
			ta.recycle();
		}
		
		thumbPaint.setShadowLayer(
			dp(3),
			0,
			dp(1),
			0x33000000
		);
		
		setLayerType(LAYER_TYPE_SOFTWARE, null);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		int desiredHeight = (int) Math.max(
			dp(40),
			thumbRadius * 2 + dp(12)
		);
		
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = resolveSize(desiredHeight, heightMeasureSpec);
		
		setMeasuredDimension(width, height);
	}
	
	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		super.onDraw(canvas);
		
		float centerY = getHeight() / 2f;
		
		float startX = thumbRadius + thumbPadding;
		float endX = getWidth() - thumbRadius - thumbPadding;
		
		float radius = trackHeight / 2f;
		
		trackRect.set(
			startX,
			centerY - trackHeight / 2f,
			endX,
			centerY + trackHeight / 2f
		);
		
		// Base Track
		trackPaint.setColor(trackColor);
		
		canvas.drawRoundRect(
			trackRect,
			radius,
			radius,
			trackPaint
		);
		
		// Buffer Progress
		if (bufferProgress > 0f) {
			
			trackPaint.setColor(bufferColor);
			
			RectF bufferRect = new RectF(
				trackRect.left,
				trackRect.top,
				trackRect.left + (trackRect.width() * bufferProgress),
				trackRect.bottom
			);
			
			canvas.drawRoundRect(
				bufferRect,
				radius,
				radius,
				trackPaint
			);
		}
		
		// Played Progress
		if (progress > 0f) {
			
			trackPaint.setColor(progressColor);
			
			RectF progressRect = new RectF(
				trackRect.left,
				trackRect.top,
				trackRect.left + (trackRect.width() * progress),
				trackRect.bottom
			);
			
			canvas.drawRoundRect(
				progressRect,
				radius,
				radius,
				trackPaint
			);
		}
		
		float thumbX = startX + ((endX - startX) * progress);
		
		// Thumb
		thumbPaint.setColor(thumbColor);
		
		canvas.drawCircle(
			thumbX,
			centerY,
			thumbRadius,
			thumbPaint
		);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		switch (event.getAction()) {
			
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				
				updateProgressFromTouch(event.getX());
				
				if (listener != null) {
					listener.onProgressChanged(progress);
				}
				
				return true;
			
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				performClick();
				return true;
		}
		
		return super.onTouchEvent(event);
	}
	
	@Override
	public boolean performClick() {
		return super.performClick();
	}
	
	private void updateProgressFromTouch(float x) {
		
		float startX = thumbRadius + thumbPadding;
		float endX = getWidth() - thumbRadius - thumbPadding;
		
		progress = (x - startX) / (endX - startX);
		progress = Math.max(0f, Math.min(1f, progress));
		
		invalidate();
	}
	
	public void setProgress(float progress) {
		this.progress = Math.max(0f, Math.min(1f, progress));
		invalidate();
	}
	
	public float getProgress() {
		return progress;
	}
	
	public void setBufferProgress(float bufferProgress) {
		this.bufferProgress = Math.max(0f, Math.min(1f, bufferProgress));
		invalidate();
	}
	
	public float getBufferProgress() {
		return bufferProgress;
	}
	
	public void setOnProgressChangedListener(OnProgressChangedListener listener) {
		this.listener = listener;
	}
	
	public interface OnProgressChangedListener {
		void onProgressChanged(float progress);
	}
	
	private float dp(float value) {
		return value * getResources().getDisplayMetrics().density;
	}
}