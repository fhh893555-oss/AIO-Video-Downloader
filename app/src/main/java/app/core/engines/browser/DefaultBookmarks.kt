package app.core.engines.browser

import com.google.gson.*
import kotlinx.coroutines.*
import lib.process.*
import java.net.*

data class RemoteBookmarkJson(val categories: List<Category>) {
	data class Category(val title: String, val sites: List<Site>)
	data class Site(val name: String, val url: String)
}

private val logger = LogHelperUtils.from(RemoteBookmarkJson::class.java)

private const val REMOTE_BOOKMARKS_URL =
	"https://raw.githubusercontent.com/shibaFoss/AIO-Video-Downloader/refs/heads/master/others/bookmark_sites.json"

private suspend fun fetchBookmarksFromJson(remoteUrl: String = REMOTE_BOOKMARKS_URL):
	List<AIOWebRecords> = withContext(Dispatchers.IO) {
	try {
		val jsonText = URL(remoteUrl).readText()
		val gson = Gson()
		val remoteSites = gson.fromJson(jsonText, RemoteBookmarkJson::class.java)

		val bookmarks = ArrayList<AIOWebRecords>()
		for (category in remoteSites.categories) {
			for (site in category.sites) {
				bookmarks.add(
					AIOWebRecords(
						name = site.name,
						url = site.url,
						isBookmark = true,
					)
				)
			}
		}
		bookmarks
	} catch (error: Exception) {
		logger.e("Error fetching bookmarks: ${error.message}", error)
		emptyList()
	}
}

suspend fun syncDefaultBookmarks() {
	withContext(Dispatchers.IO) {
		if (AIOWebRecordsRepo.getAllRecordsLazy().isEmpty()) {
			fetchBookmarksFromJson().let { detailedBookmarks ->
				detailedBookmarks.forEach { item ->
					AIOWebRecordsRepo.getWebRecordsBox().put(item)
				}
			}
		}
	}
}