package dataRepo.promo;

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

public class PromoAdsCloud extends PocketBaseClient {
    private final LoggerUtils logger = LoggerUtils.from(PromoAdsCloud.class);

    @NonNull
    @Override
    protected String getCollectionName() {
        return PromoAdInfo.POCKETBASE_COLLECTION_NAME;
    }

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

    @Nullable
    private PromoAdInfo parsePromoAd(@NonNull JSONObject item) {
        try {
            PromoAdInfo ad = new PromoAdInfo();
            ad.id = item.optString(PromoAdInfo.POCKETBASE_REMOTE_ID_FIELD);
            ad.title = item.optString(PromoAdInfo.POCKETBASE_REMOTE_TITLE_FIELD);
            ad.clickUrl = item.optString(PromoAdInfo.POCKETBASE_REMOTE_CLICK_URL_FIELD);
            ad.isActive = item.optBoolean(PromoAdInfo.POCKETBASE_REMOTE_IS_ACTIVE_FIELD);
            String imageFile = item.optString(PromoAdInfo.POCKETBASE_REMOTE_IMAGE_FIELD);
            if (!imageFile.isEmpty()) ad.imageUrl = buildFileUrl(ad.id, imageFile);
            return ad;
        } catch (Exception error) {
            logger.error("Failed to parse promo ad", error);
            return null;
        }
    }

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