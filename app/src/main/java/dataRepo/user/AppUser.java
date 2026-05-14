package dataRepo.user;

import java.io.Serializable;

import coreUtils.library.process.LoggerUtils;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class AppUser implements Serializable {
    private final static LoggerUtils logger = LoggerUtils.from(AppUser.class);

    @Id(assignable = true)
    public long entityId = 0L;
    public String userServerId = "";
    public String userDeviceId = "";

    public String name = "";
    public String email = "";

    public boolean isPremiumUser = false;
    public boolean hasReferralBonus = false;
    public int totalReferralsCount = 0;
    public boolean isBanned = false;

    public String countryCode = "";
    public String languageCode = "";
    public String locationCity = "";
    public String locationRegion = "";
    public String networkIsp = "";
    public String userIpAddress = "";
    public String continent = "";
    public String continentCode = "";
    public String zipCode = "";
    public String preferredCategories = "";
    public long lastSeenTimestamp = 0L;

    public static final String POCKETBASE_COLLECTION_NAME = "users";
    public static final String POCKETBASE_REMOTE_NAME_FIELD = "name";
    public static final String POCKETBASE_REMOTE_EMAIL_FIELD = "email";
    public static final String POCKETBASE_REMOTE_DEVICE_ID_FIELD = "deviceId";
    public static final String POCKETBASE_REMOTE_PROFILE_IMAGE_FIELD = "avatar";
    public static final String POCKETBASE_REMOTE_IS_PREMIUM_FIELD = "isPremium";
    public static final String POCKETBASE_REMOTE_HAS_REFERRAL_BONUS_FIELD = "hasReferralBonus";
    public static final String POCKETBASE_REMOTE_TOTAL_REFERRALS_COUNT_FIELD = "totalReferralsCount";
    public static final String POCKETBASE_REMOTE_IS_BANNED_FIELD = "isBanned";
}
