package coreUtils.library.views;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.widget.TextView;

import java.text.BreakIterator;
import java.util.Locale;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;

public class TextViewsUtils {
	private static final LoggerUtils logger = LoggerUtils.from(TextViewsUtils.class);
	
	public interface OnDoneListener {
		void onDone(Spannable spannable);
	}
	
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
}
