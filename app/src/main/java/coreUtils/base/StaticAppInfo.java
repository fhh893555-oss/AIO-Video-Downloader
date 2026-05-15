package coreUtils.base;

import java.util.List;

public final class StaticAppInfo {
    private StaticAppInfo() {}

    public static final String APP_OFFICIAL_SITE = "https://tubeaio.com/";
    public static final String APP_WHATSAPP_STATUS_DOWNLOADS_PATH = "/storage/emulated/0/Android/media/com.whatsapp" +
            "/WhatsApp/Media/.Statuses/";

    public static final String APP_ALL_MEDIA_TYPES = "text/html,application/xhtml+xml,application/xml;q=0.9," +
            "image/webp,image/apng,image/avif,image/jpeg,image/png,image/gif,image/svg+xml,image/*," +
            "*/*;q=0.8";

   

    public static final String APP_GITHUB_RAW_URL = "https://github.com/shibaFoss/AIO-Video-Downloader/raw/refs/" +
            "heads/master/others/adblock_host.txt";

    public static final String APP_FULL_NUMBERS = "0123456789";
    public static final String APP_FULL_APPLETS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static final String APP_DEFAULT_HTTP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/97.0.4692.99 Safari/537.36";

    public static final String APP_DEFAULT_MOBILE_HTTP_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/91.0.4472.77 Mobile Safari/537.36";

    public static final List<String> APP_DEFAULT_MOBILE_AGENTS =
            List.of("Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_5 like Mac OS X) AppleWebKit/601.1.46 (KHTML, " +
                            "like Gecko) Version/9.0 Mobile/13G36 Safari/601.1",
                    "Mozilla/5.0 (Linux; Android 4.4.2; Nexus 5 Build/KOT49H) AppleWebKit/537.36 (KHTML, " +
                            "like Gecko) Chrome/34.0.1847.114 Mobile Safari/537.36",
                    "Mozilla/5.0 (Linux; U; Android 2.3.6; en-us; GT-I9000 Build/GINGERBREAD) AppleWebKit/533.1 " +
                            "(KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");

}
