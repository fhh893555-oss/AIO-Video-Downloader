package coreUtils.library.networks;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

/**
 * A thread-safe provider class that supplies a singleton instance of {@link OkHttpClient}.
 * <p>
 * This class implements the singleton pattern using double-checked locking to ensure
 * that only one instance of {@link OkHttpClient} is created throughout the application
 * lifecycle. The client is lazily initialized on the first call to {@link #getOkHttpClient()}.
 * </p>
 * <p>
 * The provided {@link OkHttpClient} instance is pre-configured with optimal settings
 * for network operations:
 * </p>
 * <ul>
 *   <li><b>Protocols:</b> HTTP/2 with fallback to HTTP/1.1 for improved performance</li>
 *   <li><b>Redirects:</b> Both regular and SSL redirects are automatically followed</li>
 *   <li><b>Timeouts:</b> 5-second connection timeout, 10-second read timeout</li>
 *   <li><b>Connection Pool:</b> Holds up to 20 idle connections with 5-minute keep-alive</li>
 *   <li><b>Dispatcher:</b> Maximum 64 concurrent requests, 16 per host</li>
 * </ul>
 * <p>
 * This configuration is designed to balance performance, resource utilization, and
 * reliability for typical Android network operations. The class is marked as final
 * to prevent extension, and the constructor is private to enforce the singleton pattern.
 * </p>
 * <p>
 * Usage example:
 * <pre>
 * OkHttpClient client = HttpClientProvider.getOkHttpClient();
 * Request request = new Request.Builder()
 *         .url("<a href="https://api.example.com/data">https://api.example.com/data</a>")
 *         .build();
 *
 * try (Response response = client.newCall(request).execute()) {
 *     // Handle response
 * }
 * </pre>
 * </p>
 *
 * @see OkHttpClient
 * @see ConnectionPool
 * @see Dispatcher
 */
public final class HttpClientProvider {

    private HttpClientProvider() {}

    /**
     * The singleton instance of {@link OkHttpClient} used for making network requests.
     * Lazily initialized and thread-safe.
     */
    private static volatile OkHttpClient okHttpClient;

    /**
     * Returns a thread-safe, singleton instance of {@link OkHttpClient}.
     * This method uses double-checked locking to ensure that only one instance of the client
     * is created. The client is pre-configured with a connection pool, dispatcher,
     * specific protocols (HTTP/2, HTTP/1.1), and timeout settings.
     *
     * @return The shared {@link OkHttpClient} instance.
     */
    public static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            synchronized (HttpClientProvider.class) {
                if (okHttpClient == null) {
                    okHttpClient = new OkHttpClient.Builder()
                            .followRedirects(true)
                            .followSslRedirects(true)
                            .protocols(defaultProtocols())
                            .connectTimeout(5, TimeUnit.SECONDS)
                            .readTimeout(10, TimeUnit.SECONDS)
                            .connectionPool(getConnectionPool())
                            .dispatcher(getDispatcher())
                            .build();
                }
            }
        }
        return okHttpClient;
    }

    /**
     * Returns the default list of network protocols to be used by the HTTP client.
     * <p>
     * This configuration prioritizes HTTP/2 and falls back to HTTP/1.1 to ensure
     * modern performance benefits while maintaining backward compatibility.
     *
     * @return A non-null list of supported {@link Protocol}s.
     */
    @NonNull
    private static List<Protocol> defaultProtocols() {
        return Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1);
    }

    /**
     * Configures and returns a {@link ConnectionPool} for the HTTP client.
     * The pool is configured to hold a maximum of 20 idle connections with a
     * 5-minute keep-alive duration.
     *
     * @return A non-null, configured {@link ConnectionPool} instance.
     */
    @NonNull
    private static ConnectionPool getConnectionPool() {
        return new ConnectionPool(20, 5, TimeUnit.MINUTES);
    }

    /**
     * Creates and configures a {@link Dispatcher} with specific concurrency limits.
     * Sets the maximum number of requests to execute concurrently to 64, and
     * the maximum number of requests for each host to 16.
     *
     * @return A configured {@link Dispatcher} instance.
     */
    @NonNull
    private static Dispatcher getDispatcher() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(64);
        dispatcher.setMaxRequestsPerHost(16);
        return dispatcher;
    }
}