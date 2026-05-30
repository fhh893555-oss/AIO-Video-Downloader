package dataRepo.userDetails;

import com.nextgen.R;

import org.json.JSONObject;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;
import coreUtils.library.strings.StringHelper;
import dataRepo.appConfigs.AppConfigs;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Data model class representing geolocation and network information for the
 * current device user. This object is populated by fetching data from the
 * ip-api.com service and contains details such as country code, city, region,
 * ISP, IP address, continent, and postal code.
 *
 * <p>Instances are typically created asynchronously via
 * {@link GeoDetails#fetch(OnLocationDataListener, AppConfigs)} and returned
 * to the callback listener. The language code field is populated from the
 * application's current configuration rather than from the geolocation API,
 * ensuring consistency with the user's selected app language.
 *
 * <p><strong>Field descriptions:</strong>
 * <ul>
 * <li>{@code countryCode} - Two-letter ISO country code (e.g., "IN", "US").</li>
 * <li>{@code languageCode} - App-selected language code from config.</li>
 * <li>{@code locationCity} - Name of the city (e.g., "Mumbai").</li>
 * <li>{@code locationRegion} - State or region name (e.g., "Maharashtra").</li>
 * <li>{@code networkIsp} - Internet service provider name.</li>
 * <li>{@code userIpAddress} - Public IP address of the device.</li>
 * <li>{@code continent} - Full continent name (e.g., "Asia").</li>
 * <li>{@code continentCode} - Two-letter continent code (e.g., "AS").</li>
 * <li>{@code zipCode} - Postal or zip code of the location.</li>
 * </ul>
 *
 * @see #fetch(OnLocationDataListener, AppConfigs)
 * @see OnLocationDataListener
 */
public final class GeoDetails {
	private static final LoggerUtils logger = LoggerUtils.from(GeoDetails.class);
	
	public String countryCode = "";
	public String languageCode = "";
	public String locationCity = "";
	public String locationRegion = "";
	public String networkIsp = "";
	public String userIpAddress = "";
	public String continent = "";
	public String continentCode = "";
	public String zipCode = "";
	
	/**
	 * Asynchronously fetches geolocation and network data from the ip-api.com service.
	 * This method executes an HTTP GET request to retrieve the user's country code,
	 * city, region, ISP, IP address, continent information, and postal code. The
	 * request includes a mobile user agent header and expects a JSON response with
	 * the status field indicating success.
	 *
	 * <p><strong>Request details:</strong>
	 * URL format: {@code http://ip-api.com/json/?fields=status,countryCode,continent,
	 * continentCode,regionName,city,zip,isp,query}. The API returns the client's
	 * public IP address and associated geolocation data. A default country code of
	 * "IN" is applied if the response does not contain a valid country code.
	 *
	 * <p><strong>Error handling:</strong>
	 * Any network exception, non-successful HTTP response, or JSON parsing error
	 * is logged. The method returns {@code null} to the callback in all failure
	 * scenarios, ensuring the listener receives no invalid data.
	 *
	 * @param listener  Callback interface to receive the fetched {@link GeoDetails}
	 *                  object on the main thread. May be {@code null}, in which
	 *                  case the result is ignored.
	 * @param appConfig The current application configuration, used to obtain the
	 *                  selected language code for the {@link GeoDetails} object.
	 *                  Must not be null.
	 * @see GeoDetails
	 * @see OnLocationDataListener
	 * @see AppUserRepo#syncUserGeoData()
	 */
	public static void fetch(OnLocationDataListener listener, AppConfigs appConfig) {
		new ThreadTask.Builder<GeoDetails, GeoDetails>()
			.withBackgroundTask(callback -> {
				try {
					OkHttpClient client = new OkHttpClient();
					String url = "http://ip-api.com/json/?fields=status,countryCode,continent," +
						"continentCode,regionName,city,zip,isp,query";
					
					Request request = new Request.Builder().url(url)
						.header("User-Agent", StringHelper.getText(R.string.label_mobile_user_agent))
						.build();
					
					try (Response response = client.newCall(request).execute()) {
						if (response.isSuccessful()) {
							JSONObject json = new JSONObject(response.body().string());
							if ("success".equals(json.optString("status"))) {
								GeoDetails data = new GeoDetails();
								data.countryCode = json.optString("countryCode", "IN");
								data.languageCode = appConfig.selectedLanguageCode;
								data.locationCity = json.optString("city");
								data.locationRegion = json.optString("regionName");
								data.networkIsp = json.optString("isp");
								data.userIpAddress = json.optString("query");
								data.continent = json.optString("continent");
								data.continentCode = json.optString("continentCode");
								data.zipCode = json.optString("zip");
								logger.debug("GeoDetails Fetched: " + json);
								return data;
							}
						}
					}
				} catch (Exception error) {
					logger.error("ExtendedLocationData: Fetch failed", error);
				}
				return null;
			})
			.withResultTask(result -> {
				if (result != null && listener != null) {
					listener.onComplete(result);
				}
			})
			.build()
			.start();
	}
	
	/**
	 * Callback interface for receiving asynchronously fetched geolocation data.
	 * Implement this interface and pass it to {@link #fetch(OnLocationDataListener, AppConfigs)}
	 * to be notified when the geo-details request completes.
	 *
	 * <p>The callback is invoked on the main thread after the background network
	 * request finishes, making it safe to update UI components directly within
	 * the implementation.
	 *
	 * @see #fetch(OnLocationDataListener, AppConfigs)
	 * @see GeoDetails
	 */
	public interface OnLocationDataListener {
		void onComplete(GeoDetails geoDetails);
	}
}