package dataRepo.promoAds;

/**
 * Data model class representing a promotional advertisement fetched from the
 * PocketBase backend. This entity contains all necessary information to display
 * an in-app promotion, including title, description text, image URL, and click
 * destination. The {@code isActive} flag determines whether the ad should be
 * shown to users.
 *
 * <p><strong>Field descriptions:</strong>
 * <ul>
 * <li>{@code id} - Unique identifier for the promo ad in the database.</li>
 * <li>{@code title} - Short headline or title of the promotion.</li>
 * <li>{@code text} - Descriptive body text for the promotional content.</li>
 * <li>{@code imageUrl} - Fully qualified URL to the promotional image asset.</li>
 * <li>{@code clickUrl} - Destination URL opened when user taps the ad.</li>
 * <li>{@code isActive} - If {@code true}, the ad is eligible for display.</li>
 * </ul>
 *
 * <p><strong>PocketBase constants:</strong>
 * The static fields define the remote collection name and field mappings used
 * for syncing with the PocketBase server via {@link PromoAdsCloud} client.
 *
 * @see PromoAdsCloud
 * @see #POCKETBASE_COLLECTION_NAME
 */
public final class PromoAdInfo {
	public String id;
	public String title;
	public String text;
	public String imageUrl;
	public String clickUrl;
	public boolean isActive;
	
	public static final String POCKETBASE_COLLECTION_NAME = "promoAds";
	public static final String POCKETBASE_REMOTE_ID_FIELD = "id";
	public static final String POCKETBASE_REMOTE_IS_ACTIVE_FIELD = "isActive";
	public static final String POCKETBASE_REMOTE_TITLE_FIELD = "title";
	public static final String POCKETBASE_REMOTE_CLICK_URL_FIELD = "clickUrl";
	public static final String POCKETBASE_REMOTE_IMAGE_FIELD = "image";
}