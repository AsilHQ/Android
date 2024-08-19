/*
 * Copyright (c) 2024 DuckDuckGo
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

package com.duckduckgo.app.browser.menu.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.SimpleItemAnimator
import com.duckduckgo.anvil.annotations.InjectWith
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.browser.databinding.FragmentBookmarkBinding
import com.duckduckgo.app.browser.favicon.FaviconManager
import com.duckduckgo.app.tabs.BrowserNav
import com.duckduckgo.common.ui.DuckDuckGoFragment
import com.duckduckgo.common.utils.DispatcherProvider
import com.duckduckgo.common.utils.FragmentViewModelFactory
import com.duckduckgo.di.scopes.FragmentScope
import com.duckduckgo.savedsites.api.models.SavedSitesNames
import com.duckduckgo.savedsites.api.service.ImportSavedSitesResult
import com.duckduckgo.savedsites.impl.bookmarks.BookmarkItemTouchHelperCallback
import com.duckduckgo.savedsites.impl.bookmarks.BookmarksActivity.Companion.SAVED_SITE_URL_EXTRA
import com.duckduckgo.savedsites.impl.bookmarks.BookmarksAdapter
import com.duckduckgo.savedsites.impl.bookmarks.BookmarksQueryListener
import com.duckduckgo.savedsites.impl.bookmarks.BookmarksViewModel
import com.duckduckgo.savedsites.impl.bookmarks.BookmarksViewModel.Command.ImportedSavedSites
import com.duckduckgo.savedsites.impl.bookmarks.BookmarksViewModel.Command.OpenSavedSite
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import javax.inject.Inject

@InjectWith(FragmentScope::class)
class BookmarkFragment : DuckDuckGoFragment(R.layout.fragment_bookmark) {
    @Inject
    lateinit var viewModelFactory: FragmentViewModelFactory

    @Inject
    lateinit var dispatchers: DispatcherProvider

    @Inject
    lateinit var faviconManager: FaviconManager

    @Inject
    lateinit var browserNav: BrowserNav

    private lateinit var viewModel: BookmarksViewModel
    private lateinit var binding: FragmentBookmarkBinding
    private lateinit var bookmarksAdapter: BookmarksAdapter
    private lateinit var searchListener: BookmarksQueryListener

    private var deleteDialog: AlertDialog? = null
    private var searchMenuItem: MenuItem? = null
    private var exportMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this, viewModelFactory)[BookmarksViewModel::class.java]
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookmarkBinding.inflate(inflater)
        setupBookmarksRecycler()

        binding.bBtnAdd.setOnClickListener {
            Timber.d("aLog clicking")
        }

        observeViewModel()
        viewModel.fetchBookmarksAndFolders(getParentFolderId())

        return binding.root
    }

    private fun getParentFolderId() = SavedSitesNames.BOOKMARKS_ROOT

    private fun observeViewModel() {
        viewModel.viewState.observe(viewLifecycleOwner) { viewState ->
            viewState?.let { state ->
                val items = state.bookmarkItems ?: emptyList()
                Timber.d("aLog bookmark items ${items.size}")
                bookmarksAdapter.setItems(
                    items,
                    state.bookmarkItems != null && state.bookmarkItems?.isEmpty() == true && getParentFolderId() == SavedSitesNames.BOOKMARKS_ROOT,
                    false,
                )
                // setSearchMenuItemVisibility()
                exportMenuItem?.isEnabled = items.isNotEmpty()
            }
        }

        viewModel.command.observe(viewLifecycleOwner) {
            when (it) {
                // is ConfirmDeleteSavedSite -> confirmDeleteSavedSite(it.savedSite)
                is OpenSavedSite -> openSavedSite(it.savedSiteUrl)
                // is ShowEditSavedSite -> showEditSavedSiteDialog(it.savedSite)
                is ImportedSavedSites -> showImportedSavedSites(it.importSavedSitesResult)
                // is ExportedSavedSites -> showExportedSavedSites(it.exportSavedSitesResult)
                // is OpenBookmarkFolder -> openBookmarkFolder(it.bookmarkFolder)
                // is ShowEditBookmarkFolder -> editBookmarkFolder(it.bookmarkFolder)
                // is DeleteBookmarkFolder -> deleteBookmarkFolder(it.bookmarkFolder)
                // is ConfirmDeleteBookmarkFolder -> confirmDeleteBookmarkFolder(it.bookmarkFolder)
                // is LaunchBookmarkImport -> launchBookmarkImport()
                // is ShowFaviconsPrompt -> showFaviconsPrompt()
                else -> {}
            }
        }
    }

    private fun setupBookmarksRecycler() {
        bookmarksAdapter = BookmarksAdapter(layoutInflater, viewModel, this, faviconManager)
        binding.recycler.adapter = bookmarksAdapter

        val callback = BookmarkItemTouchHelperCallback(bookmarksAdapter)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.recycler)

        (binding.recycler.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    }

    private fun showImportedSavedSites(result: ImportSavedSitesResult) {
        when (result) {
            is ImportSavedSitesResult.Error -> {
                showMessage(getString(com.duckduckgo.saved.sites.impl.R.string.importBookmarksError))
            }

            is ImportSavedSitesResult.Success -> {
                if (result.savedSites.isEmpty()) {
                    showMessage(getString(com.duckduckgo.saved.sites.impl.R.string.importBookmarksEmpty))
                } else {
                    showMessage(getString(com.duckduckgo.saved.sites.impl.R.string.importBookmarksSuccess, result.savedSites.size))
                }
            }
        }
    }

    private fun openSavedSite(url: String) {
        val intent = Intent()
        intent.putExtra("open_in_browser", url)

        requireActivity().apply {
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun showMessage(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG,
        ).show()
    }
}
