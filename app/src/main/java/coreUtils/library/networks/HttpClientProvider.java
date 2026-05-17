package coreUtils.library.networks;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

public final class HttpClientProvider {
	
	private HttpClientProvider() {}
	
	private static volatile OkHttpClient okHttpClient;
	
	public static OkHttpClient getOkHttpClient(int connectTimeout, int readTimeout) {
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
		
		int finalConnectTimeout = Math.max(connectTimeout, 5);
		int finalReadTimeout = Math.max(readTimeout, 10);
		
		if (okHttpClient.connectTimeoutMillis() != (long) finalConnectTimeout * 1000 ||
			okHttpClient.readTimeoutMillis() != (long) finalReadTimeout * 1000) {
			return okHttpClient.newBuilder()
				.connectTimeout(finalConnectTimeout, TimeUnit.SECONDS)
				.readTimeout(finalReadTimeout, TimeUnit.SECONDS)
				.build();
		}

		return okHttpClient;
	}
	
	@NonNull
	private static List<Protocol> defaultProtocols() {
		return Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1);
	}
	
	@NonNull
	private static ConnectionPool getConnectionPool() {
		return new ConnectionPool(20, 5, TimeUnit.MINUTES);
	}
	
	@NonNull
	private static Dispatcher getDispatcher() {
		Dispatcher dispatcher = new Dispatcher();
		dispatcher.setMaxRequests(64);
		dispatcher.setMaxRequestsPerHost(16);
		return dispatcher;
	}
}