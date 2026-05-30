package dataRepo.promoAds;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import coreUtils.library.process.LoggerUtils;
import dataRepo.dbManager.PocketBaseClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Cloud repository client for fetching promotional advertisement data from the
 * PocketBase backend. This class extends {@link PocketBaseClient} and provides
 * specialized methods for retrieving active promo ads that can be displayed
 * within the application.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Fetches a list of active promotional ads via {@link #getPromoAds(String)}.</li>
 * <li>Parses raw JSON responses into typed {@link PromoAdInfo} objects.</li>
 * <li>Constructs fully accessible image URLs for each promotional asset.</li>
 * <li>Handles network errors gracefully, returning empty lists on failure.</li>
 * </ul>
 *
 * <p><strong>Usage pattern:</strong>
 * <pre>
 * PromoAdsCloud cloudClient = new PromoAdsCloud();
 * List<PromoAdInfo> ads = cloudClient.getPromoAds(deviceId);
 * for (PromoAdInfo ad : ads) {
 *     // Display ad in the UI
 * }
 * </pre>
 *
 * <p>The class uses the collection name defined in {@link PromoAdInfo#POCKETBASE_COLLECTION_NAME}
 * and follows the same error-handling pattern as the base client. All network
 * operations are synchronous and should be executed on background threads.
 *
 * @see PocketBaseClient
 * @see PromoAdInfo
 * @see #getPromoAds(String)
 * @see #parsePromoAd(JSONObject)
 * @see #buildFileUrl(String, String)
 */
public final class PromoAdsCloud extends PocketBaseClient {
	private final LoggerUtils logger = LoggerUtils.from(PromoAdsCloud.class);
	
	/**
	 * Returns the PocketBase collection name used for promotional advertisement records.
	 * This method overrides the abstract method from the parent class and provides
	 * the specific collection identifier defined in {@link PromoAdInfo#POCKETBASE_COLLECTION_NAME}.
	 *
	 * <p>The collection name is required for constructing API endpoints for querying
	 * promo ad data from the PocketBase server.
	 *
	 * @return The collection name string, always non-null. Matches the constant
	 * defined in the {@link PromoAdInfo} entity class.
	 * @see PromoAdInfo#POCKETBASE_COLLECTION_NAME
	 * @see #getPromoAds(String)
	 */
	@NonNull
	@Override
	protected String getCollectionName() {
		return PromoAdInfo.POCKETBASE_COLLECTION_NAME;
	}
	
	/**
	 * Retrieves a list of active promotional advertisements from the PocketBase server.
	 * This method constructs a filtered query URL to fetch only promo ads where the
	 * {@code isActive} field is {@code true}. The request includes the device ID
	 * in the header for analytics or targeting purposes on the server side.
	 *
	 * <p><strong>Request details:</strong>
	 * URL format: {@code /api/collections/promo_ads/records?filter=isActive=true}.
	 * The filter parameter is URL-encoded to safely transmit special characters.
	 * A successful response expects a JSON object containing an {@code items} array
	 * of promo ad records, each parsed via {@link #parsePromoAd(JSONObject)}.
	 *
	 * <p><strong>Error handling:</strong>
	 * <ul>
	 * <li>If the HTTP response is unsuccessful (non-2xx), an empty list is returned.</li>
	 * <li>If the JSON response lacks an {@code items} array, an empty list is returned.</li>
	 * <li>Any exception during network I/O, JSON parsing, or URL encoding is logged,
	 *     and an empty list is returned.</li>
	 * </ul>
	 *
	 * <p>Invalid or malformed individual ad records are skipped (parsed as {@code null})
	 * and do not interrupt processing of remaining items.
	 *
	 * @param deviceId The unique device identifier to be sent in the request header
	 *                 as {@code X-Device-Id}. Must not be null.
	 * @return A {@link List} of {@link PromoAdInfo} objects representing all active
	 * promo ads. Returns an empty list if the request fails or no active
	 * ads are available. Never returns {@code null}.
	 * @see #parsePromoAd(JSONObject)
	 * @see #getRecordsUrl()
	 * @see PromoAdInfo#POCKETBASE_REMOTE_IS_ACTIVE_FIELD
	 */
	@NonNull
	public List<PromoAdInfo> getPromoAds(@NonNull String deviceId) {
		List<PromoAdInfo> ads = new ArrayList<>();
		
		try {
			String filter = URLEncoder.encode(
				PromoAdInfo.POCKETBASE_REMOTE_IS_ACTIVE_FIELD + "=true",
				StandardCharsets.UTF_8);
			
			String url = getRecordsUrl() + "?filter=" + filter;
			Request request = new Request.Builder().url(url)
				.addHeader("X-Device-Id", deviceId)
				.get().build();
			
			try (Response response = httpClient.newCall(request).execute()) {
				if (!response.isSuccessful()) return ads;
				String body = response.body().string();
				JSONObject json = new JSONObject(body);
				JSONArray items = json.optJSONArray("items");
				
				if (items == null) return ads;
				for (int i = 0; i < items.length(); i++) {
					JSONObject item = items.getJSONObject(i);
					
					PromoAdInfo ad = parsePromoAd(item);
					if (ad != null) ads.add(ad);
				}
			}
			
		} catch (Exception error) {
			logger.error("Failed to get promo ads", error);
		}
		
		return ads;
	}
	
	/**
	 * Parses a JSON object from the PocketBase API response into a {@link PromoAdInfo}
	 * entity. This method extracts each field using the constant keys defined in
	 * {@link PromoAdInfo}, constructs a new ad object, and builds the full image
	 * URL if a valid image filename is present in the response.
	 *
	 * <p><strong>Parsing behavior:</strong>
	 * <ul>
	 * <li>Extracts id, title, clickUrl, and isActive using {@link JSONObject#optString}
	 *     and {@link JSONObject#optBoolean} (defaults to empty string/false).</li>
	 * <li>If the image filename is non-empty, calls {@link #buildFileUrl(String, String)}
	 *     to construct the complete accessible image URL.</li>
	 * <li>Any exception during parsing (e.g., malformed JSON) is caught and logged,
	 *     and the method returns {@code null}.</li>
	 * </ul>
	 *
	 * @param item The JSON object representing a single promo ad record from
	 *             the PocketBase server. Must not be null.
	 * @return A populated {@link PromoAdInfo} object, or {@code null} if parsing
	 * failed due to missing required fields or an exception.
	 * @see #buildFileUrl(String, String)
	 * @see PromoAdInfo
	 */
	@Nullable
	private PromoAdInfo parsePromoAd(@NonNull JSONObject item) {
		try {
			PromoAdInfo promoAdInfo = new PromoAdInfo();
			promoAdInfo.id = item.optString(PromoAdInfo.POCKETBASE_REMOTE_ID_FIELD);
			promoAdInfo.title = item.optString(PromoAdInfo.POCKETBASE_REMOTE_TITLE_FIELD);
			promoAdInfo.clickUrl = item.optString(PromoAdInfo.POCKETBASE_REMOTE_CLICK_URL_FIELD);
			promoAdInfo.isActive = item.optBoolean(PromoAdInfo.POCKETBASE_REMOTE_IS_ACTIVE_FIELD);
			String imageFile = item.optString(PromoAdInfo.POCKETBASE_REMOTE_IMAGE_FIELD);
			if (!imageFile.isEmpty()) promoAdInfo.imageUrl = buildFileUrl(promoAdInfo.id, imageFile);
			return promoAdInfo;
		} catch (Exception error) {
			logger.error("Failed to parse promo ad", error);
			return null;
		}
	}
	
	/**
	 * Constructs the full publicly accessible URL for a promo ad image stored on
	 * the PocketBase server. The URL follows the PocketBase file serving pattern:
	 * {@code /api/files/{collection}/{recordId}/{fileName}}.
	 *
	 * <p>The collection name is obtained via {@link #getCollectionName()}, which
	 * returns {@link PromoAdInfo#POCKETBASE_COLLECTION_NAME} for this client class.
	 *
	 * @param recordId The PocketBase record identifier of the promo ad. Must not be null.
	 * @param fileName The stored filename of the image returned by the server.
	 *                 Must not be null and should be non-empty.
	 * @return The complete file URL as a non-null string.
	 * @see #parsePromoAd(JSONObject)
	 * @see PromoAdInfo#POCKETBASE_COLLECTION_NAME
	 */
	@NonNull
	private String buildFileUrl(@NonNull String recordId,
	                            @NonNull String fileName) {
		return API_ENDPOINT
			+ "/api/files/"
			+ getCollectionName()
			+ "/"
			+ recordId
			+ "/"
			+ fileName;
	}
}