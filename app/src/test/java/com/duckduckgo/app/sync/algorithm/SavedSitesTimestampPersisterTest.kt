/*
 * Copyright (c) 2023 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.sync.algorithm

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.duckduckgo.app.global.db.AppDatabase
import com.duckduckgo.app.sync.FakeDisplayModeSettingsRepository
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.common.utils.formatters.time.DatabaseDateFormatter
import com.duckduckgo.savedsites.api.SavedSitesRepository
import com.duckduckgo.savedsites.api.models.BookmarkFolder
import com.duckduckgo.savedsites.api.models.SavedSite.Bookmark
import com.duckduckgo.savedsites.api.models.SavedSite.Favorite
import com.duckduckgo.savedsites.api.models.SavedSitesNames
import com.duckduckgo.savedsites.impl.RealFavoritesDelegate
import com.duckduckgo.savedsites.impl.RealSavedSitesRepository
import com.duckduckgo.savedsites.impl.sync.RealSyncSavedSitesRepository
import com.duckduckgo.savedsites.impl.sync.SyncSavedSitesRepository
import com.duckduckgo.savedsites.impl.sync.algorithm.SavedSitesTimestampPersister
import com.duckduckgo.savedsites.impl.sync.store.SavedSitesSyncMetadataDao
import com.duckduckgo.savedsites.impl.sync.store.SavedSitesSyncMetadataDatabase
import com.duckduckgo.savedsites.store.SavedSitesEntitiesDao
import com.duckduckgo.savedsites.store.SavedSitesRelationsDao
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

@RunWith(AndroidJUnit4::class)
class SavedSitesTimestampPersisterTest {

    @get:Rule
    @Suppress("unused")
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private lateinit var db: AppDatabase
    private lateinit var savedSitesDatabase: SavedSitesSyncMetadataDatabase

    private lateinit var repository: SavedSitesRepository
    private lateinit var syncRepository: SyncSavedSitesRepository
    private lateinit var savedSitesEntitiesDao: SavedSitesEntitiesDao
    private lateinit var savedSitesRelationsDao: SavedSitesRelationsDao
    private lateinit var savedSitesMetadataDao: SavedSitesSyncMetadataDao

    private lateinit var persister: SavedSitesTimestampPersister

    private val threeHoursAgo = DatabaseDateFormatter.iso8601(OffsetDateTime.now(ZoneOffset.UTC).minusHours(3))
    private val twoHoursAgo = DatabaseDateFormatter.iso8601(OffsetDateTime.now(ZoneOffset.UTC).minusHours(2))

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        savedSitesEntitiesDao = db.syncEntitiesDao()
        savedSitesRelationsDao = db.syncRelationsDao()

        savedSitesDatabase = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            SavedSitesSyncMetadataDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
        savedSitesMetadataDao = savedSitesDatabase.syncMetadataDao()

        val favoritesDelegate = RealFavoritesDelegate(
            savedSitesEntitiesDao,
            savedSitesRelationsDao,
            FakeDisplayModeSettingsRepository(),
            coroutinesTestRule.testDispatcherProvider,
        )
        syncRepository = RealSyncSavedSitesRepository(savedSitesEntitiesDao, savedSitesRelationsDao, savedSitesMetadataDao)
        repository = RealSavedSitesRepository(
            savedSitesEntitiesDao,
            savedSitesRelationsDao,
            favoritesDelegate,
            coroutinesTestRule.testDispatcherProvider,
        )

        persister = SavedSitesTimestampPersister(repository, syncRepository)
    }

    @Test
    fun whenProcessingBookmarkNotPresentLocallyThenBookmarkIsInserted() {
        val bookmark = Bookmark("bookmark1", "title", "www.example.com", "folder2", threeHoursAgo)
        assertTrue(repository.getBookmarkById(bookmark.id) == null)

        persister.processBookmark(bookmark, SavedSitesNames.BOOKMARKS_ROOT)

        assertTrue(repository.getBookmarkById(bookmark.id) != null)
    }

    @Test
    fun whenProcessingDeletedBookmarkNotPresentLocallyThenBookmarkIsNotInserted() {
        val bookmark = Bookmark("bookmark1", "title", "www.example.com", "folder2", threeHoursAgo, deleted = "1")
        assertTrue(repository.getBookmarkById(bookmark.id) == null)

        persister.processBookmark(bookmark, SavedSitesNames.BOOKMARKS_ROOT)

        assertTrue(repository.getBookmarkById(bookmark.id) == null)
    }

    @Test
    fun whenProcessingRemoteBookmarkModifiedAfterThenBookmarkIsReplaced() {
        val bookmark = Bookmark("bookmark1", "title", "www.example.com", "folder2", threeHoursAgo)
        repository.insert(bookmark)

        assertTrue(repository.getBookmarkById(bookmark.id) != null)

        val remoteBookmark = bookmark.copy(title = "title replaced", lastModified = twoHoursAgo)
        persister.processBookmark(remoteBookmark, SavedSitesNames.BOOKMARKS_ROOT)

        val replacedBookmark = repository.getBookmarkById(remoteBookmark.id)
        assertTrue(replacedBookmark != null)
        assertTrue(replacedBookmark!!.title == remoteBookmark.title)
    }

    @Test
    fun whenProcessingRemoteBookmarkModifiedBeforeThenBookmarkIsNotReplaced() {
        val bookmark = Bookmark("bookmark1", "title", "www.example.com", "folder2", twoHoursAgo)
        repository.insert(bookmark)

        assertTrue(repository.getBookmarkById(bookmark.id) != null)

        val remoteBookmark = bookmark.copy(title = "title replaced", lastModified = threeHoursAgo)
        persister.processBookmark(remoteBookmark, SavedSitesNames.BOOKMARKS_ROOT)

        val storedBookmark = repository.getBookmarkById(bookmark.id)
        assertTrue(storedBookmark != null)
        assertTrue(storedBookmark!!.title == bookmark.title)
    }

    @Test
    fun whenProcessingDeletedRemoteBookmarkThenBookmarkIsDeleted() {
        val bookmark = Bookmark("bookmark1", "title", "www.example.com", "folder2", threeHoursAgo)
        repository.insert(bookmark)

        assertTrue(repository.getBookmarkById(bookmark.id) != null)

        val remoteBookmark = bookmark.copy(deleted = "1", lastModified = threeHoursAgo)
        persister.processBookmark(remoteBookmark, SavedSitesNames.BOOKMARKS_ROOT)

        val replacedBookmark = repository.getBookmarkById(remoteBookmark.id)
        assertTrue(replacedBookmark == null)
    }

    @Test
    fun whenProcessingFavouriteNotPresentLocallyThenBookmarkIsInserted() {
        val favourite = Favorite("bookmark1", "title", "www.example.com", "timestamp", 0)
        assertTrue(repository.getFavoriteById(favourite.id) == null)

        persister.processFavourite(favourite, SavedSitesNames.FAVORITES_ROOT)

        assertTrue(repository.getFavoriteById(favourite.id) != null)
    }

    @Test
    fun whenProcessingFavoritePresentOnDifferentFormFactorFolderThenFavouriteIsInserted() {
        val favourite1 = Favorite("bookmark1", "title", "www.example.com", twoHoursAgo, 0)
        repository.insert(favourite1)
        assertTrue(syncRepository.getFavoriteById(favourite1.id, SavedSitesNames.FAVORITES_ROOT) != null)
        Assert.assertNull(syncRepository.getFavoriteById(favourite1.id, SavedSitesNames.FAVORITES_DESKTOP_ROOT))

        persister.processFavourite(favourite1, SavedSitesNames.FAVORITES_DESKTOP_ROOT)

        val storedFavourite = syncRepository.getFavoriteById(favourite1.id, SavedSitesNames.FAVORITES_DESKTOP_ROOT)
        assertTrue(storedFavourite != null)
        assertTrue(storedFavourite!!.position == favourite1.position)
    }

    @Test
    fun whenProcessingDeletedFavouriteThenLocalFavouriteIsDeleted() {
        val favourite = Favorite("bookmark1", "title", "www.example.com", threeHoursAgo, 0)
        repository.insert(favourite)
        assertTrue(repository.getFavoriteById(favourite.id) != null)

        val remoteFavourite = favourite.copy(deleted = "1")
        persister.processFavourite(remoteFavourite, SavedSitesNames.FAVORITES_ROOT)

        assertTrue(repository.getFavoriteById(favourite.id) == null)
        assertTrue(repository.getBookmarkById(favourite.id) != null)
    }

    @Test
    fun whenProcessingDeletedFavouritePresentInMultipleFactoryFolderThenOnlyDeleteFromSpecificFolder() {
        val favourite = Favorite("bookmark1", "title", "www.example.com", twoHoursAgo, 0)
        syncRepository.insert(favourite, SavedSitesNames.FAVORITES_ROOT)
        syncRepository.insert(favourite, SavedSitesNames.FAVORITES_DESKTOP_ROOT)
        assertTrue(syncRepository.getFavoriteById(favourite.id, SavedSitesNames.FAVORITES_ROOT) != null)
        assertTrue(syncRepository.getFavoriteById(favourite.id, SavedSitesNames.FAVORITES_DESKTOP_ROOT) != null)

        val remoteFavourite = favourite.copy(deleted = "1")
        persister.processFavourite(remoteFavourite, SavedSitesNames.FAVORITES_DESKTOP_ROOT)

        assertTrue(syncRepository.getFavoriteById(favourite.id, SavedSitesNames.FAVORITES_ROOT) != null)
        Assert.assertFalse(syncRepository.getFavoriteById(favourite.id, SavedSitesNames.FAVORITES_DESKTOP_ROOT) != null)
        assertTrue(repository.getBookmarkById(favourite.id) != null)
    }

    @Test
    fun whenProcessingDeletedFavouriteNotPresentLocallyThenFavouriteIsNotAdded() {
        val favourite = Favorite("bookmark1", "title", "www.example.com", threeHoursAgo, 0, deleted = "1")

        assertTrue(repository.getFavoriteById(favourite.id) == null)
        persister.processFavourite(favourite, SavedSitesNames.FAVORITES_ROOT)

        assertTrue(repository.getFavoriteById(favourite.id) == null)
        assertTrue(repository.getBookmarkById(favourite.id) == null)
    }

    @Test
    fun whenProcessingFavouriteModifiedAfterThenLocalFavouriteIsReplaced() {
        val favourite1 = Favorite("bookmark1", "title", "www.example.com", threeHoursAgo, 0)
        val favourite2 = Favorite("bookmark2", "title2", "www.example2.com", threeHoursAgo, 1)
        repository.insert(favourite1)
        repository.insert(favourite2)
        assertTrue(repository.getFavoriteById(favourite1.id) != null)

        val remoteFavourite = favourite1.copy(lastModified = twoHoursAgo, position = 1)
        persister.processFavourite(remoteFavourite, SavedSitesNames.FAVORITES_ROOT)

        val storedFavourite = repository.getFavoriteById(favourite1.id)

        assertTrue(storedFavourite != null)
        assertTrue(storedFavourite!!.position == remoteFavourite.position)
    }

    @Test
    fun whenProcessingFavouriteModifiedBeforeThenLocalFavouriteIsNotReplaced() {
        val favourite1 = Favorite("bookmark1", "title", "www.example.com", twoHoursAgo, 0)
        val favourite2 = Favorite("bookmark2", "title2", "www.example2.com", twoHoursAgo, 1)
        repository.insert(favourite1)
        repository.insert(favourite2)
        assertTrue(repository.getFavoriteById(favourite1.id) != null)

        val remoteFavourite = favourite1.copy(lastModified = threeHoursAgo, position = 1)
        persister.processFavourite(remoteFavourite, SavedSitesNames.FAVORITES_ROOT)

        val storedFavourite = repository.getFavoriteById(favourite1.id)

        assertTrue(storedFavourite != null)
        assertTrue(storedFavourite!!.position == favourite1.position)
    }

    @Test
    fun whenProcessingFavouriteSameTimestampThenLocalFavouriteIsNotReplaced() {
        val timestamp = twoHoursAgo
        val favourite1 = Favorite("bookmark1", "title", "www.example.com", timestamp, 0)
        val favourite2 = Favorite("bookmark2", "title2", "www.example2.com", timestamp, 1)
        repository.insert(favourite1)
        repository.insert(favourite2)
        assertTrue(repository.getFavoriteById(favourite1.id) != null)

        val remoteFavourite = favourite1.copy(lastModified = timestamp, position = 1)
        persister.processFavourite(remoteFavourite, SavedSitesNames.FAVORITES_ROOT)

        val storedFavourite = repository.getFavoriteById(favourite1.id)

        assertTrue(storedFavourite != null)
        assertTrue(storedFavourite!!.position == remoteFavourite.position)
    }

    @Test
    fun whenProcessingFolderNotPresentLocallyThenFolderIsInserted() {
        val folder = BookmarkFolder("folder1", "title", SavedSitesNames.BOOKMARKS_ROOT, 0, 0)
        assertTrue(repository.getFolder(folder.id) == null)

        persister.processBookmarkFolder(folder, emptyList())

        assertTrue(repository.getFolder(folder.id) != null)
    }

    @Test
    fun whenProcessingDeletedFolderPresentLocallyThenFolderIsDeleted() {
        val folder = BookmarkFolder("folder1", "title", SavedSitesNames.BOOKMARKS_ROOT, 0, 0)
        repository.insert(folder)
        assertTrue(repository.getFolder(folder.id) != null)

        val deletedFolder = folder.copy(deleted = "1")
        persister.processBookmarkFolder(deletedFolder, emptyList())

        assertTrue(repository.getFolder(folder.id) == null)
    }

    @Test
    fun whenProcessingFolderModifiedAfterThenFolderIsReplaced() {
        val folder = BookmarkFolder("folder1", "title", SavedSitesNames.BOOKMARKS_ROOT, 0, 0, lastModified = threeHoursAgo)
        repository.insert(folder)
        assertTrue(repository.getFolder(folder.id) != null)

        val updatedFolder = folder.copy(name = "remoteFolder1", lastModified = twoHoursAgo)
        persister.processBookmarkFolder(updatedFolder, emptyList())

        assertTrue(repository.getFolder(folder.id) != null)
        assertTrue(repository.getFolder(folder.id)!!.name == updatedFolder.name)
    }

    @Test
    fun whenProcessingFolderModifiedBeforeThenFolderIsNotReplaced() {
        val folder = BookmarkFolder("folder1", "title", SavedSitesNames.BOOKMARKS_ROOT, 0, 0, lastModified = twoHoursAgo)
        repository.insert(folder)
        assertTrue(repository.getFolder(folder.id) != null)

        val updatedFolder = folder.copy(name = "remoteFolder1", lastModified = threeHoursAgo)
        persister.processBookmarkFolder(updatedFolder, emptyList())

        assertTrue(repository.getFolder(folder.id) != null)
        assertTrue(repository.getFolder(folder.id)!!.name == folder.name)
    }

    @Test
    fun whenProcessingFolderSameTimestampThenFolderIsReplaced() {
        val timestamp = twoHoursAgo
        val folder = BookmarkFolder("folder1", "title", SavedSitesNames.BOOKMARKS_ROOT, 0, 0, lastModified = timestamp)
        repository.insert(folder)
        assertTrue(repository.getFolder(folder.id) != null)

        val updatedFolder = folder.copy(name = "remoteFolder1", lastModified = timestamp)
        persister.processBookmarkFolder(updatedFolder, emptyList())

        assertTrue(repository.getFolder(folder.id) != null)
        assertTrue(repository.getFolder(folder.id)!!.name == updatedFolder.name)
    }
}
