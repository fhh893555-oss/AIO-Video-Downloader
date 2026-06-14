package coreUtils.library.views;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.ReplacementSpan;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.BreakIterator;
import java.util.Locale;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;

public class TextViewsUtils {
	
	private static final LoggerUtils logger = LoggerUtils.from(TextViewsUtils.class);
	
	public interface OnDoneListener {
		void onDone(Spannable spannable);
	}
	
	public static void normalizeTallSymbols(@NonNull final TextView textView,
	                                        @NonNull final String originalText,
	                                        final float reductionFactor,
	                                        @NonNull final OnDoneListener onDone) {
		if (originalText.isEmpty()) return;
		new ThreadTask.Builder<Spannable, Void>()
			.withBackgroundTask(callback -> {
				final SpannableStringBuilder spannable =
					new SpannableStringBuilder(originalText);
				
				BreakIterator boundaryIterator =
					BreakIterator.getCharacterInstance(Locale.getDefault());
				boundaryIterator.setText(originalText);
				
				int currentStart = boundaryIterator.first();
				int currentEnd = boundaryIterator.next();
				
				while (currentEnd != BreakIterator.DONE) {
					String cluster = originalText.substring(currentStart, currentEnd);
					boolean shouldReduce = false;
					
					for (int index = 0; index < cluster.length(); index++) {
						char c = cluster.charAt(index);
						int codePoint = Character.codePointAt(cluster, index);
						
						if (!Character.isSpaceChar(c) &&
							Character.UnicodeScript.of(codePoint)
								!= Character.UnicodeScript.LATIN) {
							shouldReduce = true;
							break;
						}
					}
					
					if (shouldReduce) {
						try {
							spannable.setSpan(
								new RelativeSizeSpan(reductionFactor),
								currentStart,
								currentEnd,
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
							);
						} catch (Exception error) {
							logger.error("Error normalizing textview: ", error);
						}
					}
					
					currentStart = currentEnd;
					currentEnd = boundaryIterator.next();
				}
				return spannable;
			})
			.withResultTask(spannable -> {
				if (textView.getText().toString().equals(originalText)) {
					if (onDone != null) {
						onDone.onDone(spannable);
					}
				}
			})
			.build()
			.start();
	}
	
	public static void applyGradientSpan(@NonNull TextView textView, int startColor,
	                                     int endColor, int startIndex, int endIndex) {
		CharSequence text = textView.getText();
		if (text == null || text.length() == 0) return;
		if (!(text instanceof Spannable)) text = new SpannableStringBuilder(text);
		
		Spannable spannable = (Spannable) text;
		int exclusive = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE;
		GradientSpan gradientSpan = new GradientSpan(startColor, endColor);
		spannable.setSpan(gradientSpan, startIndex, endIndex, exclusive);
		textView.setText(spannable);
	}
	
	private static class GradientSpan extends ReplacementSpan {
		private final int startColor;
		private final int endColor;
		
		GradientSpan(int startColor, int endColor) {
			this.startColor = startColor;
			this.endColor = endColor;
		}
		
		@Override
		public int getSize(@NonNull Paint paint, CharSequence text,
		                   int start, int end, @Nullable Paint.FontMetricsInt fm) {
			return (int) paint.measureText(text, start, end);
		}
		
		@Override
		public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
		                 float x, int top, int y, int bottom, @NonNull Paint paint) {
			float textWidth = paint.measureText(text, start, end);
			Shader shader = new LinearGradient(x, top, x + textWidth, top,
				startColor, endColor, Shader.TileMode.CLAMP);
			paint.setShader(shader);
			canvas.drawText(text, start, end, x, bottom - paint.descent(), paint);
			paint.setShader(null);
		}
	}
}
