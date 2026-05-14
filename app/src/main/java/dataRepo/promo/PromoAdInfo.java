package dataRepo.promo;

public class PromoAdInfo {
    public String id;
    public String title;
    public String text;
    public String imageUrl;
    public String clickUrl;
    public boolean isActive;

    public static final String POCKETBASE_COLLECTION_NAME = "promo_ads";
    public static final String POCKETBASE_REMOTE_ID_FIELD = "id";
    public static final String POCKETBASE_REMOTE_IS_ACTIVE_FIELD = "isActive";
    public static final String POCKETBASE_REMOTE_TITLE_FIELD = "title";
    public static final String POCKETBASE_REMOTE_CLICK_URL_FIELD = "clickUrl";
    public static final String POCKETBASE_REMOTE_IMAGE_FIELD = "image";
}