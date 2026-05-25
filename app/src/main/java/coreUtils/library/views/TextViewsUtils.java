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

/**
 * Utility class providing advanced text manipulation and styling methods for TextViews.
 * <p>
 * This class offers a collection of static utility methods for enhancing TextView
 * functionality beyond standard Android APIs. Features include applying color gradients
 * to text spans, normalizing tall symbols (reducing size of non-Latin scripts), and
 * other text processing utilities for improved typography and visual consistency.
 * </p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Gradient Spans:</b> Apply linear gradient color transitions to specific
 *       portions of text using {@link #applyGradientSpan(TextView, int, int, int, int)}</li>
 *   <li><b>Script Normalization:</b> Reduce font size of non-Latin scripts (Cyrillic,
 *       Arabic, CJK, Devanagari, etc.) to align visually with Latin text using
 *       {@link #normalizeTallSymbols(TextView, String, float, OnDoneListener)}</li>
 *   <li><b>Background Processing:</b> Heavy text operations are performed on background
 *       threads to avoid blocking the UI thread</li>
 * </ul>
 * </p>
 *
 * <p><b>Usage Examples:</b>
 * <pre>
 * // Apply gradient to "NextGen" in title
 * TextViewsUtils.applyGradientSpan(
 *     tvTitle,
 *     getColor(R.color.color_secondary),
 *     getColor(R.color.color_primary_variant),
 *     11, 17
 * );
 *
 * // Normalize mixed-script text
 * TextViewsUtils.normalizeTallSymbols(
 *     tvMixedText,
 *     "Hello 世界 Привет",
 *     0.85f,
 *     spannable -> tvMixedText.setText(spannable)
 * );
 * </pre>
 * </p>
 *
 * <p><b>Thread Safety:</b>
 * This utility class is thread-safe as it only contains static methods with no
 * shared mutable state. Background operations use {@link ThreadTask} for safe
 * asynchronous execution.
 * </p>
 *
 * @see TextView
 * @see Spannable
 * @see GradientSpan
 * @see ThreadTask
 */
public class TextViewsUtils {
	
	private static final LoggerUtils logger = LoggerUtils.from(TextViewsUtils.class);
	
	/**
	 * Callback interface for receiving the processed Spannable after text normalization.
	 * <p>
	 * Implement this interface to be notified when the background processing of
	 * text normalization is complete. The normalized text is delivered as a
	 * Spannable with RelativeSizeSpans applied to non-Latin characters.
	 * </p>
	 */
	public interface OnDoneListener {
		void onDone(Spannable spannable);
	}
	
	/**
	 * Normalizes tall symbols (non-Latin scripts) in a TextView by reducing their font size.
	 * <p>
	 * This method processes text in a background thread to identify non-Latin characters
	 * (such as Cyrillic, Arabic, Chinese, Japanese, Korean, Devanagari, etc.) and applies
	 * a {@link RelativeSizeSpan} to reduce their size relative to Latin characters. This
	 * helps achieve visual alignment when mixed scripts have different inherent heights.
	 * </p>
	 *
	 * <p><b>How It Works:</b>
	 * <ol>
	 *   <li>Iterates through the text using {@link BreakIterator} to handle Unicode grapheme clusters</li>
	 *   <li>Checks each cluster for any non-Latin script characters</li>
	 *   <li>Applies a RelativeSizeSpan with the specified reduction factor to matching clusters</li>
	 *   <li>Returns the processed Spannable via the OnDoneListener on the main thread</li>
	 *   <li>Verifies the TextView text hasn't changed before applying the result</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Scripts Affected:</b>
	 * This method targets all Unicode scripts except LATIN. This includes Cyrillic,
	 * Greek, Arabic, Hebrew, Devanagari, Chinese, Japanese, Korean, Thai, and many others.
	 * </p>
	 *
	 * @param textView        the TextView containing the text to normalize
	 * @param originalText    the original text string (should match TextView's current text)
	 * @param reductionFactor the factor to reduce font size by (e.g., 0.8f for 80% of original size)
	 * @param onDone          callback listener to receive the processed Spannable, or null to ignore
	 */
	public static void normalizeTallSymbols(final TextView textView, final String originalText,
	                                        final float reductionFactor, final OnDoneListener onDone) {
		if (originalText == null || originalText.isEmpty()) return;
		new ThreadTask.Builder<Spannable, Void>()
			.withBackgroundTask(callback -> {
				final SpannableStringBuilder spannable = new SpannableStringBuilder(originalText);
				BreakIterator boundaryIterator = BreakIterator.getCharacterInstance(Locale.getDefault());
				boundaryIterator.setText(originalText);
				
				int currentStart = boundaryIterator.first();
				int currentEnd = boundaryIterator.next();
				
				while (currentEnd != BreakIterator.DONE) {
					String cluster = originalText.substring(currentStart, currentEnd);
					boolean shouldReduce = false;
					
					for (int index = 0; index < cluster.length(); index++) {
						char c = cluster.charAt(index);
						int codePoint = Character.codePointAt(cluster, index);
						if (!Character.isSpaceChar(c) && Character.UnicodeScript.of(codePoint)
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
	
	/**
	 * Applies a color gradient span to a specific portion of text within a TextView.
	 * <p>
	 * This method creates a gradient effect (color transition from startColor to endColor)
	 * over a substring defined by startIndex and endIndex. If the TextView's text is not
	 * already a Spannable, it is converted to one. The gradient is applied using a custom
	 * GradientSpan that extends ReplacementSpan and draws the text with a linear gradient shader.
	 * </p>
	 *
	 * <p><b>Usage Example:</b>
	 * <pre>
	 * TextView title = findViewById(R.id.tvTitle);
	 * int fullText = "Welcome to NextGen App";
	 * applyGradientSpan(title, Color.BLUE, Color.CYAN, 11, 17); // Applies gradient to "NextGen"
	 * </pre>
	 * </p>
	 *
	 * @param textView   the TextView containing the text to which the gradient will be applied
	 * @param startColor the starting color of the gradient (e.g., Color.RED or resource color)
	 * @param endColor   the ending color of the gradient (e.g., Color.BLUE or resource color)
	 * @param startIndex the starting index of the substring to apply the gradient to (inclusive)
	 * @param endIndex   the ending index of the substring to apply the gradient to (exclusive)
	 */
	public static void applyGradientSpan(@NonNull TextView textView, int startColor,
	                                     int endColor, int startIndex, int endIndex) {
		CharSequence text = textView.getText();
		if (text == null || text.length() == 0) return;
		if (!(text instanceof Spannable)) text = new SpannableStringBuilder(text);
		
		Spannable spannable = (Spannable) text;
		int exclusive = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE;
		spannable.setSpan(new GradientSpan(startColor, endColor), startIndex, endIndex, exclusive);
		textView.setText(spannable);
	}
	
	/**
	 * A custom ReplacementSpan that draws text with a linear gradient shader.
	 * <p>
	 * This span overrides the default text drawing behavior to apply a horizontal gradient
	 * (from left to right) across the spanned text portion. The gradient transitions smoothly
	 * from the startColor to the endColor over the width of the text. This is used internally
	 * by {@link #applyGradientSpan} to create colorful text effects.
	 * </p>
	 *
	 * <p><b>How It Works:</b>
	 * <ol>
	 *   <li>{@link #getSize} returns the width of the text to be drawn</li>
	 *   <li>{@link #draw} creates a LinearGradient shader from startColor to endColor</li>
	 *   <li>The shader is applied to the paint, then the text is drawn</li>
	 *   <li>The shader is cleared after drawing to avoid affecting other spans</li>
	 * </ol>
	 * </p>
	 */
	private static class GradientSpan extends ReplacementSpan {
		private final int startColor;
		private final int endColor;
		
		/**
		 * Constructs a new GradientSpan with the specified start and end colors.
		 * <p>
		 * This constructor initializes the span with the colors that will be used to create
		 * the linear gradient across the text. The gradient will transition smoothly from
		 * startColor to endColor over the width of the spanned text.
		 * </p>
		 *
		 * @param startColor the color at the beginning (left side) of the gradient
		 * @param endColor   the color at the end (right side) of the gradient
		 */
		GradientSpan(int startColor, int endColor) {
			this.startColor = startColor;
			this.endColor = endColor;
		}
		
		/**
		 * Measures the width of the text portion that this span will replace.
		 * <p>
		 * This method calculates and returns the width of the text from the start index
		 * to the end index using the provided paint. This width determines how much
		 * horizontal space the spanned text will occupy in the layout.
		 * </p>
		 *
		 * @param paint the paint used to measure the text (contains font, size, etc.)
		 * @param text  the full text containing the spanned portion
		 * @param start the start index of the spanned portion (inclusive)
		 * @param end   the end index of the spanned portion (exclusive)
		 * @param fm    font metrics (unused, may be null)
		 * @return the width in pixels of the spanned text
		 */
		@Override
		public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
		                   @Nullable Paint.FontMetricsInt fm) {
			return (int) paint.measureText(text, start, end);
		}
		
		/**
		 * Draws the text with a linear gradient shader applied.
		 * <p>
		 * This method creates a LinearGradient shader from startColor to endColor over the
		 * width of the text, applies it to the paint, draws the text, then clears the shader
		 * to prevent affecting other spans. The text is drawn at the baseline position.
		 * </p>
		 *
		 * @param canvas the canvas to draw on
		 * @param text   the full text containing the spanned portion
		 * @param start  the start index of the spanned portion (inclusive)
		 * @param end    the end index of the spanned portion (exclusive)
		 * @param x      the left position where the text should be drawn
		 * @param top    the top position of the line (unused in baseline calculation)
		 * @param y      the baseline y-coordinate (standard drawText uses y, but bottom is used here)
		 * @param bottom the bottom position of the line, used with descent() for baseline alignment
		 * @param paint  the paint to use for drawing (shader will be applied)
		 */
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
