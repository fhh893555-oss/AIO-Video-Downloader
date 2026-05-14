package dataRepo.user;

import com.nextgen.R;

import org.json.JSONObject;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;
import coreUtils.library.strings.StringHelper;
import dataRepo.configs.AppConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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

    public interface OnLocationDataListener {
        void onComplete(GeoDetails geoDetails);
    }

    public static void fetch(OnLocationDataListener listener, AppConfig appConfig) {
        new ThreadTask.Builder<GeoDetails, GeoDetails>()
                .withBackgroundTask(callback -> {
                    try {
                        OkHttpClient client = new OkHttpClient();
                        String url = "http://ip-api.com/json/?fields=status,countryCode,continent,continentCode," +
                                "regionName,city,zip,isp,query";

                        Request request = new Request.Builder().url(url)
                                .header("User-Agent", StringHelper.getText(R.string.title_mobile_user_agent))
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
}