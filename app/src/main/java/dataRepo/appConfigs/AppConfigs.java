package dataRepo.appConfigs;

import java.io.Serializable;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class AppConfigs implements Serializable {
	
	@Id(assignable = true)
	public long entityId = 0L;
	
	public boolean isLocaleConfigured = false;
	public boolean isTermsConditionsAgreed = false;
	public boolean isAppReviewCompleted = false;
	public boolean hasSkippedBatteryOptimization = false;
	public boolean hasAppCrashedRecently = false;
	public String lastProcessedClipboardText = "";
	
	public int defaultDownloadLocationType = SYSTEM_GALLERY;
	public String selectedDownloadDirectoryPath = "";
	
	public String selectedLanguageCode = "EN";
	public String selectedRegionCode = "IN";
	public int completedDownloadsSortOrder = SORT_DATE_NEWEST_FIRST;
	public int activeDownloadsSortOrder = SORT_DATE_NEWEST_FIRST;
	public boolean isDailySuggestionsEnabled = true;
	
	public int totalSuccessfulDownloadCount = 0;
	public float totalAppUsageDurationMs = 0f;
	public String formattedTotalUsageDuration = "";
	public float foregroundAppUsageDurationMs = 0f;
	public String formattedForegroundUsageDuration = "";
	
	public int pushNotificationClickCount = 0;
	public int pushNotificationReceivedCount = 0;
	public int languageChangeButtonClickCount = 0;
	public int mediaPlaybackButtonClickCount = 0;
	public int howToGuideButtonClickCount = 0;
	public int videoUrlEditorButtonClickCount = 0;
	public int homeHistoryMenuItemClickCount = 0;
	public int homeBookmarksMenuItemClickCount = 0;
	public int recentDownloadsMenuItemClickCount = 0;
	public int homeFaviconMenuItemClickCount = 0;
	public int versionCheckMenuItemClickCount = 0;
	public int interstitialAdClickCount = 0;
	public int interstitialAdImpressionCount = 0;
	public int rewardedAdClickCount = 0;
	public int rewardedAdImpressionCount = 0;
	public int userInitiatedDownloadCount = 0;
	
	public boolean isDownloadThumbnailHidden = false;
	public boolean isDownloadCompletionSoundEnabled = true;
	public boolean isDownloadNotificationHidden = false;
	public boolean isDownloadOpenedOnSingleClick = true;
	public boolean isDownloadAutoResumeEnabled = true;
	public int maxDownloadAutoResumeLimit = 35;
	public long maxDownloadSpeedLimit = 0L;
	public boolean wifiOnlyDownloadEnabled = false;
	public String httpUserAgentForDownloads = "";
	public String httpProxyServerForDownloads = "";
	public int downloadConcurrencyLimit = 1;
	
	public String browserDefaultHomepageUrl = "https://youtube.com";
	public boolean isDesktopBrowsingEnabled = false;
	public boolean isPrivateBrowsingEnabled = false;
	public boolean isAdBlockerEnabled = true;
	public boolean isJavaScriptEnabled = true;
	public boolean isImageLoadingEnabled = true;
	public boolean isPopupBlockerEnabled = true;
	public boolean isVideoGrabberEnabled = true;
	public String httpUserAgentForBrowser = "";
	
	public static final int PRIVATE_FOLDER = 1;
	public static final int SYSTEM_GALLERY = 2;
	
	public static final int SORT_DATE_NEWEST_FIRST = 3;
	public static final int SORT_DATE_OLDEST_FIRST = 4;
	public static final int SORT_NAME_A_TO_Z = 5;
	public static final int SORT_NAME_Z_TO_A = 6;
	public static final int SORT_SIZE_SMALLEST_FIRST = 7;
	public static final int SORT_SIZE_LARGEST_FIRST = 8;
	public static final int SORT_TYPE_VIDEOS_FIRST = 9;
	public static final int SORT_TYPE_MUSIC_FIRST = 10;
	
	public void save() {
		AppConfigsRepo.save(this);
	}
}