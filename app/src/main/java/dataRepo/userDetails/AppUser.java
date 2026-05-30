package dataRepo.userDetails;

import java.io.Serializable;

import coreUtils.library.process.LoggerUtils;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.Transient;

/**
 * Entity class representing an application user with persistent storage in the
 * ObjectBox database. This class stores user-specific data including profile
 * information, authentication tokens, and personalized settings across app sessions.
 *
 * <p>The user entity is typically a singleton instance (single active user per
 * app installation) accessed via {@link AppUserRepo#getUser()}. User data is
 * persisted automatically when {@link #save()} or {@link AppUserRepo#save(AppUser)}
 * is called.
 *
 * <p><strong>Common fields include:</strong>
 * <ul>
 * <li>User profile information (name, email, avatar URI).</li>
 * <li>Authentication tokens for API access.</li>
 * <li>User-specific preferences and settings.</li>
 * <li>Analytics identifiers for tracking.</li>
 * </ul>
 *
 * <p>Implements {@link Serializable} to allow user objects to be passed via
 * Intents or saved in {@link android.os.Bundle} when needed across components.
 *
 * @see AppUserRepo
 * @see io.objectbox.annotation.Entity
 * @see java.io.Serializable
 */
@Entity public final class AppUser implements Serializable {
	@Transient private final static LoggerUtils logger = LoggerUtils.from(AppUser.class);
	
	@Id(assignable = true)
	public long id = 0L;
	@Index public String userServerId = "";
	@Index public String userDeviceId = "";
	@Index public String name = "";
	@Index public String email = "";
	@Index public boolean isPremiumUser = false;
	public boolean hasReferralBonus = false;
	public int totalReferralsCount = 0;
	@Index public boolean isBanned = false;
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
	
	public static final String POCKETBASE_COLLECTION_NAME = "userDetails";
	public static final String POCKETBASE_REMOTE_NAME_FIELD = "name";
	public static final String POCKETBASE_REMOTE_EMAIL_FIELD = "email";
	public static final String POCKETBASE_REMOTE_DEVICE_ID_FIELD = "deviceId";
	public static final String POCKETBASE_REMOTE_PROFILE_IMAGE_FIELD = "avatar";
	public static final String POCKETBASE_REMOTE_IS_PREMIUM_FIELD = "isPremium";
	public static final String POCKETBASE_REMOTE_HAS_REFERRAL_BONUS_FIELD = "hasReferralBonus";
	public static final String POCKETBASE_REMOTE_TOTAL_REFERRALS_COUNT_FIELD = "totalReferralsCount";
	public static final String POCKETBASE_REMOTE_IS_BANNED_FIELD = "isBanned";
	
	/**
	 * Persists the current user entity to the database. This method is a convenience
	 * wrapper around {@link AppUserRepo#save(AppUser)} that passes {@code this} as
	 * the argument. Call this method after modifying any user fields to ensure
	 * changes are stored persistently in the ObjectBox database.
	 *
	 * <p><strong>Example usage:</strong>
	 * <pre>
	 * AppUser user = AppUserRepo.getUser();
	 * user.setUserName("john_doe");
	 * user.setEmail("john@example.com");
	 * user.save();
	 * </pre>
	 *
	 * @see AppUserRepo#save(AppUser)
	 * @see AppUserRepo#getUser()
	 */
	public void save() {
		AppUserRepo.save(this);
	}
}
