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

package com.duckduckgo.app.bookmarks

import com.duckduckgo.common.utils.formatters.time.DatabaseDateFormatter
import com.duckduckgo.savedsites.api.models.BookmarkFolder
import com.duckduckgo.savedsites.api.models.SavedSite
import com.duckduckgo.savedsites.api.models.SavedSite.Bookmark
import com.duckduckgo.savedsites.api.models.SavedSite.Favorite
import com.duckduckgo.savedsites.impl.sync.SyncBookmarkPage
import com.duckduckgo.savedsites.impl.sync.SyncFolderChildren
import com.duckduckgo.savedsites.impl.sync.SyncSavedSiteRequestFolder
import com.duckduckgo.savedsites.impl.sync.SyncSavedSiteResponseFolder
import com.duckduckgo.savedsites.impl.sync.SyncSavedSitesRequestEntry
import com.duckduckgo.savedsites.impl.sync.SyncSavedSitesResponseEntry
import com.duckduckgo.savedsites.store.Entity
import com.duckduckgo.savedsites.store.EntityType.BOOKMARK
import com.duckduckgo.savedsites.store.EntityType.FOLDER
import com.duckduckgo.savedsites.store.Relation

object BookmarkTestUtils {

    fun givenSomeBookmarks(
        total: Int,
        lastModified: String = DatabaseDateFormatter.iso8601(),
    ): List<Entity> {
        val entities = mutableListOf<Entity>()
        for (index in 1..total) {
            entities.add(
                Entity(title = "entity$index", url = "https://testUrl$index", type = BOOKMARK, lastModified = lastModified),
            )
        }
        return entities
    }

    fun givenSomeFolders(
        total: Int,
    ): List<Entity> {
        val entities = mutableListOf<Entity>()
        for (index in 1..total) {
            entities.add(
                Entity(title = "folder$index", url = "https://testUrl$index", type = FOLDER),
            )
        }
        return entities
    }

    fun givenFolderWithContent(
        folderId: String,
        entities: List<Entity>,
    ): List<Relation> {
        val relations = mutableListOf<Relation>()
        entities.forEach {
            relations.add(Relation(folderId = folderId, entityId = it.entityId))
        }
        return relations
    }

    fun aBookmarkFolder(
        id: String,
        name: String,
        parentId: String,
        timestamp: String = "2023-05-10T16:10:32.338Z",
    ): BookmarkFolder {
        return BookmarkFolder(id = id, name = name, parentId = parentId, lastModified = timestamp)
    }

    fun aFavorite(
        id: String,
        title: String,
        url: String,
        position: Int,
        timestamp: String = "2023-05-10T16:10:32.338Z",
    ): Favorite {
        return Favorite(id, title, url, lastModified = timestamp, position)
    }

    fun aBookmark(
        id: String,
        title: String,
        url: String,
        timestamp: String = "2023-05-10T16:10:32.338Z",
    ): Bookmark {
        return Bookmark(id, title, url, lastModified = timestamp)
    }

    fun getRequestEntryFromBookmarkFolder(
        bookmarkFolder: BookmarkFolder,
        children: List<String>,
    ): SyncSavedSitesRequestEntry {
        return SyncSavedSitesRequestEntry(
            id = bookmarkFolder.id,
            title = bookmarkFolder.name,
            folder = SyncSavedSiteRequestFolder(SyncFolderChildren(current = children, insert = children, remove = emptyList())),
            page = null,
            deleted = null,
            client_last_modified = bookmarkFolder.lastModified ?: DatabaseDateFormatter.iso8601(),
        )
    }

    fun getRequestEntryFromSavedSite(savedSite: SavedSite): SyncSavedSitesRequestEntry {
        return SyncSavedSitesRequestEntry(
            id = savedSite.id,
            title = savedSite.title,
            page = SyncBookmarkPage(savedSite.url),
            folder = null,
            deleted = null,
            client_last_modified = savedSite.lastModified ?: DatabaseDateFormatter.iso8601(),
        )
    }

    fun getResponseEntryFromSavedSite(
        savedSite: SavedSite,
        deleted: Boolean = false,
    ): SyncSavedSitesResponseEntry {
        if (deleted) {
            return SyncSavedSitesResponseEntry(
                id = savedSite.id,
                title = savedSite.title,
                page = SyncBookmarkPage(savedSite.url),
                folder = null,
                deleted = "1",
                last_modified = savedSite.lastModified ?: DatabaseDateFormatter.iso8601(),
            )
        } else {
            return SyncSavedSitesResponseEntry(
                id = savedSite.id,
                title = savedSite.title,
                page = SyncBookmarkPage(savedSite.url),
                folder = null,
                deleted = null,
                last_modified = savedSite.lastModified ?: DatabaseDateFormatter.iso8601(),
            )
        }
    }

    fun getResponseEntryFromBookmarkFolder(
        bookmarkFolder: BookmarkFolder,
        children: List<String>,
        deleted: Boolean = false,
    ): SyncSavedSitesResponseEntry {
        if (deleted) {
            return SyncSavedSitesResponseEntry(
                id = bookmarkFolder.id,
                title = bookmarkFolder.name,
                folder = SyncSavedSiteResponseFolder(children),
                page = null,
                deleted = "1",
                last_modified = bookmarkFolder.lastModified ?: DatabaseDateFormatter.iso8601(),
            )
        } else {
            return SyncSavedSitesResponseEntry(
                id = bookmarkFolder.id,
                title = bookmarkFolder.name,
                folder = SyncSavedSiteResponseFolder(children),
                page = null,
                deleted = null,
                last_modified = bookmarkFolder.lastModified ?: DatabaseDateFormatter.iso8601(),
            )
        }
    }
}
