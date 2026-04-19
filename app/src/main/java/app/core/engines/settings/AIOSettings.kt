package app.core.engines.settings

import androidx.documentfile.provider.*
import app.core.*
import app.core.AIOApp.Companion.AIO_DEFAULT_DOWNLOAD_PATH
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.aioSettings
import app.core.AIOLanguage.Companion.ENGLISH
import app.core.engines.settings.AIOSettingsRepo.saveInDB
import com.aio.*
import com.anggrayudi.storage.file.*
import com.anggrayudi.storage.file.DocumentFileCompat.fromFullPath
import io.objectbox.annotation.*
import lib.files.FileSystemUtility.isWritableFile
import lib.process.*
import lib.texts.CommonTextUtils.getText
import java.io.*
import kotlin.jvm.Transient

@Entity
class AIOSettings : Serializable {

	@Transient
	private val logger = LogHelperUtils.from(javaClass)

	@Id(assignable = true)
	var id: Long = 0L

	var downloadId: Long = AIOSettingsRepo.APP_SETTINGS_DB_ID

	var isLanguageSetupCompleted: Boolean = false
	var isAppReviewCompleted: Boolean = false
	var hasSkippedBatteryOptimization: Boolean = false
	var hasAppCrashedRecently: Boolean = false
	var lastClipboardTextProcessed: String = ""

	var defaultDownloadLocationType: Int = SYSTEM_GALLERY
	var selectedDownloadDirectory: String = AIO_DEFAULT_DOWNLOAD_PATH

	var whatsAppStatusFullFolderPath: String = APP_WHATSAPP_STATUS_DOWNLOADS_PATH
	var selectedUiLanguage: String = ENGLISH
	var selectedContentRegion: String = "IN"
	var selectedThemeMode: Int = THEME_AUTO
	var downloadsDefaultSortOrder: Int = SORT_DATE_NEWEST_FIRST
	var isDailyContentSuggestionEnabled: Boolean = true

	var successfulDownloadCount: Int = 0
	var totalUsageDurationMs: Float = 0.0f
	var totalUsageDurationFormatted: String = ""
	var foregroundUsageDurationMs: Float = 0.0f
	var foregroundUsageDurationFormatted: String = ""

	var pushNotificationClickCount: Int = 0
	var pushNotificationReceivedCount: Int = 0
	var languageChangeClickCount: Int = 0
	var mediaPlaybackClickCount: Int = 0
	var howToGuideClickCount: Int = 0
	var videoUrlEditorClickCount: Int = 0
	var homeHistoryClickCount: Int = 0
	var homeBookmarksClickCount: Int = 0
	var recentDownloadsClickCount: Int = 0
	var homeFaviconClickCount: Int = 0
	var versionCheckClickCount: Int = 0
	var interstitialAdClickCount: Int = 0
	var interstitialAdImpressionCount: Int = 0
	var rewardedAdClickCount: Int = 0
	var rewardedAdImpressionCount: Int = 0

	var downloadSingleUIProgress: Boolean = true
	var downloadHideVideoThumbnail: Boolean = false
	var downloadPlayNotificationSound: Boolean = true
	var downloadHideNotification: Boolean = false
	var hideDownloadProgressFromUI: Boolean = false
	var downloadAutoRemoveTasks: Boolean = false
	var downloadAutoRemoveTaskAfterNDays: Int = 0
	var openDownloadedFileOnSingleClick: Boolean = true
	var downloadAutoResume: Boolean = true
	var downloadAutoResumeMaxErrors: Int = 35
	var downloadAutoLinkRedirection: Boolean = true
	var downloadAutoFolderCatalog: Boolean = true
	var downloadAutoThreadSelection: Boolean = true
	var downloadAutoFileMoveToPrivate: Boolean = false
	var downloadAutoConvertVideosToMp3: Boolean = false
	var downloadBufferSize: Int = 1024 * 8
	var downloadMaxHttpReadingTimeout: Int = 1000 * 30
	var downloadDefaultThreadConnections: Int = 1
	var downloadDefaultParallelConnections: Int = 1
	var downloadVerifyChecksum: Boolean = false
	var downloadMaxNetworkSpeed: Long = 0
	var downloadWifiOnly: Boolean = false
	var downloadHttpUserAgent: String = APP_DEFAULT_HTTP_USER_AGENT
	var downloadHttpProxyServer: String = ""
	var numberOfMaxDownloadThreshold: Int = 1
	var numberOfDownloadsUserDid: Int = 0

	var browserDefaultHomepage: String =
		getText(R.string.text_https_youtube_com)
	var browserDesktopBrowsing: Boolean = false
	var browserPrivateBrowsing: Boolean = false
	var browserEnableAdblocker: Boolean = true
	var browserEnableJavascript: Boolean = true
	var browserEnableImages: Boolean = true
	var browserEnablePopupBlocker: Boolean = true
	var browserEnableVideoGrabber: Boolean = true
	var browserHttpUserAgent: String = APP_DEFAULT_MOBILE_HTTP_USER_AGENT

	fun updateInDB() {
		ThreadsUtility.executeInBackground(codeBlock = {
			runCatching {
				ensureDownloadDirExists()
				saveInDB(this@AIOSettings)
			}.onFailure { error ->
				logger.e("Settings save error: ${error.message}", error)
			}
		})
	}

	suspend fun validateUserSelectedFolder() {
		withIOContext {
			runCatching {
				if (!isWritableFile(getUserSelectedDir())) {
					val created = setupDefaultDownloadDir()
					if (created) aioSettings.updateInDB()
				}
			}.onFailure { error ->
				logger.e("Folder validation error: ${error.message}", error)
			}
		}
	}

	private suspend fun getUserSelectedDir(): DocumentFile? {
		return withIOContext {
			when (aioSettings.defaultDownloadLocationType) {
				PRIVATE_FOLDER -> getDirectory(INSTANCE.dataDir.absolutePath)
				SYSTEM_GALLERY -> getDefaultDirectory()
				else -> null
			}
		}
	}

	private suspend fun getDefaultDirectory(): DocumentFile? {
		return withIOContext {
			ensureDownloadDirExists()
			return@withIOContext getDirectory(selectedDownloadDirectory)
		}
	}

	private suspend fun getDirectory(internalDir: String): DocumentFile? {
		return withIOContext {
			fromFullPath(INSTANCE, internalDir, requiresWriteAccess = true)
		}
	}

	private suspend fun ensureDownloadDirExists() {
		withIOContext {
			if (selectedDownloadDirectory.isEmpty()) {
				selectedDownloadDirectory = AIO_DEFAULT_DOWNLOAD_PATH
			}
		}
	}

	private suspend fun setupDefaultDownloadDir(): Boolean {
		return withIOContext {
			runCatching {
				val dirName = APP_DOWNLOADS
				val dir = INSTANCE.getPublicDownloadDir()?.makeFolder(INSTANCE, dirName)
				dir?.exists() == true
			}.getOrDefault(false)
		}
	}

	companion object {
		const val DARK_MODE_INDICATOR_FIE: String = "darkmode.on"
		const val THEME_AUTO = -1
		const val THEME_DARK = 1
		const val THEME_LIGHT = 2
		const val PRIVATE_FOLDER = 1
		const val SYSTEM_GALLERY = 2
		const val SORT_DATE_NEWEST_FIRST = 3
		const val SORT_DATE_OLDEST_FIRST = 4
		const val SORT_NAME_A_TO_Z = 5
		const val SORT_NAME_Z_TO_A = 6
		const val SORT_SIZE_SMALLEST_FIRST = 7
		const val SORT_SIZE_LARGEST_FIRST = 8
		const val SORT_TYPE_VIDEOS_FIRST = 9
		const val SORT_TYPE_MUSIC_FIRST = 10
	}
}