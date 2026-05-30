package userInterface.userFeedback;

/**
 * Represents the possible user sentiment reactions for feedback submission.
 * This enumeration defines five reaction levels ranging from highly positive to
 * highly negative, allowing users to quickly express their overall satisfaction.
 *
 * <p><strong>Reaction meanings:</strong>
 * <ul>
 * <li><b>Excellent</b> - Highly positive, user is very satisfied</li>
 * <li><b>Good</b> - Positive, user is generally satisfied</li>
 * <li><b>Average</b> - Neutral, acceptable but not outstanding</li>
 * <li><b>Poor</b> - Negative, user is dissatisfied</li>
 * <li><b>Angry</b> - Highly negative, user is frustrated</li>
 * </ul>
 *
 * @see #name()
 * @see FeedbackPocketbase
 */
public enum FeedbackReactions {
	Excellent, Good, Average, Poor, Angry
}