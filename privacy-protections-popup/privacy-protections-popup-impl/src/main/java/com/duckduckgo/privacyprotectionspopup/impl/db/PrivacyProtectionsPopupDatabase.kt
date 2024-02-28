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

package com.duckduckgo.privacyprotectionspopup.impl.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.Instant

@Database(
    exportSchema = true,
    version = 1,
    entities = [
        PopupDismissDomain::class,
    ],
)
@TypeConverters(Converters::class)
abstract class PrivacyProtectionsPopupDatabase : RoomDatabase() {
    abstract fun popupDismissDomainDao(): PopupDismissDomainsDao
}

class Converters {
    @TypeConverter
    fun epochMilliToInstant(millis: Long?): Instant? =
        millis?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun instantToEpochMilli(instant: Instant?): Long? =
        instant?.toEpochMilli()
}
