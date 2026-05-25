package userInterface.userFeedback;

/**
 * Represents the possible user sentiment reactions for feedback submission.
 * <p>
 * This enumeration defines the five available reaction levels that users can select
 * when providing feedback about their app experience. Each reaction corresponds to
 * a specific sentiment ranging from highly positive to highly negative, allowing
 * users to quickly express their overall satisfaction level.
 * </p>
 *
 * <p><b>Reaction Meanings:</b>
 * <ul>
 *   <li><b>Excellent</b> - Highly positive, user is very satisfied with the app</li>
 *   <li><b>Good</b> - Positive, user is generally satisfied with minor suggestions</li>
 *   <li><b>Average</b> - Neutral, user finds the app acceptable but not outstanding</li>
 *   <li><b>Poor</b> - Negative, user is dissatisfied with certain aspects</li>
 *   <li><b>Angry</b> - Highly negative, user is frustrated or angry with the app</li>
 * </ul>
 * </p>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * FeedbackReactions selected = FeedbackReactions.Good;
 * viewModel.setSelectedReaction(selected.name());
 *
 * switch (selected) {
 *     case Excellent:
 *         showHappyIcon();
 *         break;
 *     case Poor:
 *         showSadIcon();
 *         break;
 * }
 * </pre>
 * </p>
 */
public enum FeedbackReactions {
	Excellent, Good, Average, Poor, Angry
}