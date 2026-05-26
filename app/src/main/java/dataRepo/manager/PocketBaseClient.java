package dataRepo.manager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import coreUtils.library.networks.HttpClientProvider;
import coreUtils.library.process.LoggerUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
/**
 * Serves as the abstract foundation for communicating with a PocketBase backend
 * API, encapsulating HTTP client configuration and CRUD operation templates.
 * <p>
 * This abstract class provides a reusable base for concrete client implementations
 * targeting specific PocketBase collections. It manages an {@link OkHttpClient}
 * instance for network operations, defines common REST endpoint construction logic,
 * and offers protected methods for executing POST, PATCH, and filtered GET requests
 * with automatic JSON parsing. Subclasses must implement {@link #getCollectionName()}
 * to specify the target collection for all operations.
 * </p>
 * <ul>
 * <li>Uses a shared {@code API_ENDPOINT} constant for the base server URL</li>
 * <li>Requests are automatically closed via try-with-resources to prevent leaks</li>
 * <li>All network errors are caught and logged; methods return {@code null} on
 *     failure instead of throwing checked exceptions</li>
 * <li>Supports custom {@link OkHttpClient} injection via
 *     {@link #setCustomOKHttpClient(OkHttpClient)}</li>
 * </ul>
 *
 * @see OkHttpClient
 * @see JSONObject
 * @see RequestBody
 */
public abstract class PocketBaseClient {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	protected OkHttpClient httpClient = HttpClientProvider.getOkHttpClient(10, 10);
	protected final MediaType jsonMedia = MediaType.parse("application/json");
	
	public static final String API_ENDPOINT = "https://cloud.tubeaio.com";
	
	/**
	 * Constructs the fully qualified REST endpoint URL for the target collection's
	 * records.
	 * <p>
	 * This method builds a URL by concatenating the base API endpoint constant
	 * with a fixed path template and the collection name obtained from
	 * {@link #getCollectionName()}. The resulting URL follows the pattern:
	 * <pre>{@code {API_ENDPOINT}/api/collections/{collectionName}/records}</pre>
	 * The method is marked {@code protected} to allow subclasses to override the
	 * URL construction logic if the default pattern does not apply.
	 * </p>
	 * <ul>
	 * <li>Assumes {@code API_ENDPOINT} is a class-level constant (e.g.,
	 *     {@code "https://api.example.com"})</li>
	 * <li>Does not perform validation on the concatenated result</li>
	 * <li>Returned URL is never {@code null} when {@link #getCollectionName()}
	 *     returns a non-null value</li>
	 * </ul>
	 *
	 * @return the complete record endpoint URL as a non-null string
	 * @see #getCollectionName()
	 */
	@NonNull
	protected String getRecordsUrl() {
		return API_ENDPOINT + "/api/collections/" + getCollectionName() + "/records";
	}
	
	/**
	 * Returns the name of the remote collection or database table to be queried.
	 * <p>
	 * This abstract method must be implemented by concrete subclasses to specify
	 * the target collection name used in constructing the full records URL via
	 * {@link #getRecordsUrl()}. The returned value should be URL-safe and
	 * typically corresponds to a logical grouping of records (e.g., "users",
	 * "transactions", "devices").
	 * </p>
	 *
	 * @return the collection name string, never {@code null}
	 * @see #getRecordsUrl()
	 */
	@NonNull
	protected abstract String getCollectionName();
	
	/**
	 * Executes a filtered GET query against the remote collection and returns the
	 * first matching record as a JSON object.
	 * <p>
	 * This method constructs a URL-encoded filter parameter, appends optional
	 * field selectors, and includes a device identifier as a custom HTTP header.
	 * The response is expected to be a JSON object containing an {@code items}
	 * array. Only the first element of this array is returned; subsequent items
	 * are ignored. If the {@code items} array is empty, missing, or the response
	 * is unsuccessful, {@code null} is returned.
	 * </p>
	 * <ul>
	 * <li>Filter string is URL-encoded using {@link URLEncoder#encode(String, String)}</li>
	 * <li>Device ID is transmitted in the {@code X-Device-Id} header for request
	 *     routing or authentication purposes</li>
	 * <li>Optional {@code fields} parameter can limit which fields are returned
	 *     (implementation-dependent syntax)</li>
	 * <li>All exceptions are caught and logged; no exceptions propagate to caller</li>
	 * </ul>
	 *
	 * @param filter   a query filter string (e.g., "name='John'") that will be
	 *                 URL-encoded before transmission
	 * @param fields   an optional comma-separated list of fields to include in the
	 *                 response, or {@code null} to request all fields
	 * @param deviceId the device identifier sent in the {@code X-Device-Id} header
	 *                 for request contextualization
	 * @return a {@link JSONObject} representing the first matching record, or
	 * {@code null} if no records match, the query fails, or an error
	 * occurs during network or JSON processing
	 */
	@Nullable
	protected JSONObject query(@NonNull String filter,
	                           @Nullable String fields, @NonNull String deviceId) {
		try {
			String encodedFilter = URLEncoder.encode(filter, StandardCharsets.UTF_8);
			StringBuilder url = new StringBuilder(getRecordsUrl())
				.append("?filter=")
				.append(encodedFilter);
			
			if (fields != null && !fields.isEmpty()) {
				url.append("&fields=").append(fields);
			}
			
			Request request = new Request.Builder()
				.url(url.toString())
				.addHeader("X-Device-Id", deviceId)
				.get()
				.build();
			
			try (Response response = httpClient.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					logger.debug("Query failed code=" + response.code());
					return null;
				}
				
				String body = response.body().string();
				JSONArray items = new JSONObject(body).optJSONArray("items");
				if (items == null || items.length() == 0) return null;
				return items.getJSONObject(0);
			}
			
		} catch (Exception error) {
			logger.error("Query failed.", error);
			return null;
		}
	}
	
	/**
	 * Executes an HTTP PATCH request to partially update an existing record.
	 * <p>
	 * This method constructs a PATCH request targeting a specific record endpoint
	 * (base URL + {@code "/"} + {@code recordId}), serializes the provided
	 * {@link JSONObject} into a string, and sends it using the configured
	 * {@link OkHttpClient} with a JSON media type. The response body is parsed
	 * as a {@link JSONObject} upon successful completion. Any network failure,
	 * non-successful HTTP status code, or JSON parsing error results in a
	 * {@code null} return and an error log entry.
	 * </p>
	 * <ul>
	 * <li>Uses {@link RequestBody#create(String, okhttp3.MediaType)} with
	 *     {@code jsonMedia} as the content type</li>
	 * <li>Record ID is appended directly to the base URL path</li>
	 * <li>All exceptions are caught and logged silently</li>
	 * </ul>
	 *
	 * @param recordId the unique identifier of the record to patch (appended to
	 *                 the base URL path)
	 * @param data     a {@link JSONObject} containing the partial fields to update
	 * @return a {@link JSONObject} parsed from the successful response body, or
	 * {@code null} if the request failed or response was invalid
	 * @see #getRecordsUrl()
	 * @see #post(JSONObject)
	 */
	@Nullable
	protected JSONObject patch(@NonNull String recordId,
	                           @NonNull JSONObject data) {
		try {
			RequestBody requestBody = RequestBody.create(data.toString(), jsonMedia);
			Request request = new Request.Builder()
				.url(getRecordsUrl() + "/" + recordId)
				.patch(requestBody).build();
			
			try (Response response = httpClient.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					logger.debug("Patch failed code=" + response.code());
					return null;
				}
				
				return new JSONObject(response.body().string());
			}
		} catch (Exception error) {
			logger.error("Patch failed.", error);
			return null;
		}
	}
	
	/**
	 * Executes an HTTP POST request with a JSON payload to create a new record.
	 * <p>
	 * This overloaded version accepts a {@link JSONObject} directly, serializes it
	 * to a string, and sends it as the request body with a JSON media type. The
	 * request targets the base URL returned by {@link #getRecordsUrl()}. Upon a
	 * successful response (HTTP status code 2xx), the response body is parsed and
	 * returned as a {@link JSONObject}. All exceptions are caught and logged,
	 * returning {@code null} to the caller without propagating the error.
	 * </p>
	 * <ul>
	 * <li>Internally uses {@link RequestBody#create(String, okhttp3.MediaType)}</li>
	 * <li>Response is automatically closed via try-with-resources</li>
	 * <li>Logs debug-level messages for HTTP errors and error-level for exceptions</li>
	 * </ul>
	 *
	 * @param data a {@link JSONObject} containing the data to post (e.g., new
	 *             record fields in JSON format)
	 * @return a {@link JSONObject} parsed from the successful response body
	 * (typically the created record including server-generated fields),
	 * or {@code null} if the request failed
	 * @see #patch(String, JSONObject)
	 * @see RequestBody
	 */
	@Nullable
	protected JSONObject post(@NonNull JSONObject data) {
		try {
			RequestBody requestBody = RequestBody.create(data.toString(), jsonMedia);
			Request request = new Request.Builder()
				.url(getRecordsUrl()).post(requestBody).build();
			
			try (Response response = httpClient.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					logger.debug("Post failed code=" + response.code());
					return null;
				}
				
				return new JSONObject(response.body().string());
			}
		} catch (Exception error) {
			logger.error("Post failed.", error);
			return null;
		}
	}
	
	/**
	 * Executes an HTTP POST request with the provided body and returns the parsed
	 * JSON response.
	 * <p>
	 * This method constructs a POST request to the URL returned by
	 * {@link #getRecordsUrl()}, sends it using the configured {@link OkHttpClient},
	 * and attempts to parse the response body as a {@link JSONObject}. The operation
	 * uses a try-with-resources block to ensure the response body is properly
	 * closed. Any network failure, non-successful HTTP status code (2xx), or JSON
	 * parsing error results in a {@code null} return and an error log entry.
	 * </p>
	 * <ul>
	 * <li>Uses the {@link OkHttpClient} instance managed by this class</li>
	 * <li>Response body is consumed as a string via {@link ResponseBody#string()}</li>
	 * <li>All exceptions are caught and logged; no exceptions propagate to caller</li>
	 * <li>Returns {@code null} for any unsuccessful response or exception</li>
	 * </ul>
	 *
	 * @param requestBody the HTTP POST body to send (typically JSON or form-encoded)
	 * @return a {@link JSONObject} parsed from the successful response body, or
	 * {@code null} if the request failed or response was invalid
	 * @see RequestBody
	 * @see OkHttpClient
	 * @see JSONObject
	 */
	@Nullable
	protected JSONObject post(@NonNull RequestBody requestBody) {
		try {
			Request request = new Request.Builder()
				.url(getRecordsUrl()).post(requestBody).build();
			
			try (Response response = httpClient.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					logger.debug("Post failed code=" + response.code());
					return null;
				}
				
				return new JSONObject(response.body().string());
			}
		} catch (Exception error) {
			logger.error("Post failed.", error);
			return null;
		}
	}
	
	/**
	 * Replaces the internal HTTP client used for all network requests.
	 * <p>
	 * This setter allows external configuration of the {@link OkHttpClient}
	 * instance, enabling customization of timeouts, interceptors, cache settings,
	 * or connection specifications. The provided client must not be {@code null},
	 * as subsequent calls to {@link #post(RequestBody)} depend on a valid client
	 * reference. No validation is performed on the client's configuration state.
	 * </p>
	 *
	 * @param httpClient the {@link OkHttpClient} instance to use for all future
	 *                   POST requests; must not be {@code null}
	 */
	public void setCustomOKHttpClient(@NonNull OkHttpClient httpClient) {
		this.httpClient = httpClient;
	}
	
	/**
	 * Returns the currently configured HTTP client instance.
	 * <p>
	 * The returned client is guaranteed to be non-null, as the field is initialized
	 * at construction time (presumably with a default client) before any calls to
	 * this method. Callers may inspect or modify the client's configuration, though
	 * direct modifications may affect concurrent request behavior.
	 * </p>
	 *
	 * @return the {@link OkHttpClient} instance used for executing network requests
	 */
	@NonNull
	public OkHttpClient getHttpClient() {
		return this.httpClient;
	}
	
}