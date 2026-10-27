package com.jhw.batteryemoji.presentation.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhw.batteryemoji.core.Utils.AssetConfig
import com.jhw.batteryemoji.data.repository.BatteryRepositoryImpl
import com.jhw.batteryemoji.data.repository.PreferenceRepositoryImpl
import com.jhw.batteryemoji.data.repository.RemoteAssetRepositoryImpl
import com.jhw.batteryemoji.domain.model.AssetManifest
import com.jhw.batteryemoji.domain.model.BatteryEmojiItem
import com.jhw.batteryemoji.domain.model.CategoryItem
import com.jhw.batteryemoji.domain.model.QuickActionItem
import com.jhw.batteryemoji.domain.model.RemoteAssetItem
import com.jhw.batteryemoji.domain.repository.BatteryRepository
import com.jhw.batteryemoji.domain.repository.PreferenceRepository
import com.jhw.batteryemoji.domain.repository.RemoteAssetRepository
import com.jhw.batteryemoji.presentation.remote.RemoteAssetUiState
import kotlinx.coroutines.launch

class HomeViewModel(
    private val batteryRepository: BatteryRepository = BatteryRepositoryImpl(),
    private val preferenceRepository: PreferenceRepository = PreferenceRepositoryImpl(),
    private val remoteAssetRepository: RemoteAssetRepository = RemoteAssetRepositoryImpl()
) : ViewModel() {

    // ── Existing LiveData (unchanged) ──────────────────────────────────────────

    private val _categories = MutableLiveData<List<CategoryItem>>()
    val categories: LiveData<List<CategoryItem>> = _categories

    private val _emojis = MutableLiveData<List<BatteryEmojiItem>>()
    val emojis: LiveData<List<BatteryEmojiItem>> = _emojis

    private val _quickActions = MutableLiveData<List<QuickActionItem>>()
    val quickActions: LiveData<List<QuickActionItem>> = _quickActions

    private val _isEmojiBatteryEnabled = MutableLiveData<Boolean>()
    val isEmojiBatteryEnabled: LiveData<Boolean> = _isEmojiBatteryEnabled

    private val _isSearchMode = MutableLiveData<Boolean>(false)
    val isSearchMode: LiveData<Boolean> = _isSearchMode

    private val _recentEmojis = MutableLiveData<List<BatteryEmojiItem>>()
    val recentEmojis: LiveData<List<BatteryEmojiItem>> = _recentEmojis

    private val _topPickEmojis = MutableLiveData<List<BatteryEmojiItem>>()
    val topPickEmojis: LiveData<List<BatteryEmojiItem>> = _topPickEmojis

    private var currentCategoryId: String = "hot"

    // ── Remote asset LiveData (new) ────────────────────────────────────────────

    private val _remoteEmojiState = MutableLiveData<RemoteAssetUiState>(RemoteAssetUiState.Loading)
    val remoteEmojiState: LiveData<RemoteAssetUiState> = _remoteEmojiState

    private val _remoteEmojis = MutableLiveData<List<RemoteAssetItem>>(emptyList())
    val remoteEmojis: LiveData<List<RemoteAssetItem>> = _remoteEmojis

    // Remote emojis for the Search screen, split the same way as the local Recent / Top Picks grids
    private val _remoteRecentEmojis = MutableLiveData<List<RemoteAssetItem>>(emptyList())
    val remoteRecentEmojis: LiveData<List<RemoteAssetItem>> = _remoteRecentEmojis

    private val _remoteTopPickEmojis = MutableLiveData<List<RemoteAssetItem>>(emptyList())
    val remoteTopPickEmojis: LiveData<List<RemoteAssetItem>> = _remoteTopPickEmojis

    private var currentSearchQuery: String = ""

    // ── Init ───────────────────────────────────────────────────────────────────

    init {
        loadData()
        loadRemoteEmojis()
    }

    // ── Existing methods (unchanged) ───────────────────────────────────────────

    private fun loadData() {
        _categories.value = batteryRepository.getCategories()
        _quickActions.value = batteryRepository.getQuickActions()
        _isEmojiBatteryEnabled.value = preferenceRepository.isEmojiBatteryEnabled()

        val all = batteryRepository.getBatteryEmojis()
        _emojis.value = all
        _recentEmojis.value = all.filter { it.isRecent }
        _topPickEmojis.value = all.filter { it.isTopPick }
    }

    fun selectCategory(categoryId: String) {
        currentCategoryId = categoryId
        _emojis.value = batteryRepository.getBatteryEmojis(categoryId)
        // Also refresh remote emojis filtered by the new category
        val remote = remoteAssetRepository.getAssetsByType(AssetConfig.Type.EMOJI, categoryId)
        _remoteEmojis.value = remote
    }

    fun toggleEmojiBattery() {
        val newState = !(_isEmojiBatteryEnabled.value ?: true)
        preferenceRepository.setEmojiBatteryEnabled(newState)
        _isEmojiBatteryEnabled.value = newState
    }

    fun refreshEmojiBatteryState() {
        val current = preferenceRepository.isEmojiBatteryEnabled()
        if (_isEmojiBatteryEnabled.value != current) {
            _isEmojiBatteryEnabled.value = current
        }
    }

    fun setSearchMode(enabled: Boolean) {
        _isSearchMode.value = enabled
        if (!enabled) {
            selectCategory(currentCategoryId)
        }
    }

    fun search(query: String) {
        val results = batteryRepository.searchEmojis(query)
        _emojis.value = results
        _recentEmojis.value = results.filter { it.isRecent }
        _topPickEmojis.value = results.filter { it.isTopPick }
        currentSearchQuery = query
        updateRemoteSearch(remoteAssetRepository.getAssetsByType(AssetConfig.Type.EMOJI, null))
    }

    /** Filters remote emojis by [currentSearchQuery] with the same rules as the local search. */
    private fun updateRemoteSearch(allRemote: List<RemoteAssetItem>) {
        val trimmed = currentSearchQuery.trim().lowercase()
        val results = if (trimmed.isEmpty()) allRemote else allRemote.filter { item ->
            item.name.lowercase().contains(trimmed) ||
                    item.category.lowercase().contains(trimmed) ||
                    item.tags.any { it.lowercase().contains(trimmed) }
        }
        _remoteRecentEmojis.value = results.filter { it.isRecent }
        // Manifest emojis usually carry no flags, so anything not marked recent lands in Top Picks
        _remoteTopPickEmojis.value = results.filter { it.isTopPick || !it.isRecent }
    }

    // ── New remote methods ─────────────────────────────────────────────────────

    /**
     * Fetches the remote manifest and posts emoji assets to [remoteEmojiState].
     * Falls back to cached content on network failure.
     * Safe to call multiple times — de-duplicates in-flight requests.
     */
    fun loadRemoteEmojis() {
        viewModelScope.launch {
            // Show the manifest saved by an earlier launch right away, then refresh it
            val cached = remoteAssetRepository.loadCachedManifest()
            if (cached != null) {
                showRemoteEmojis(cached, isFromCache = true)
            } else {
                _remoteEmojiState.value = RemoteAssetUiState.Loading
            }
            remoteAssetRepository.fetchManifest().fold(
                onSuccess = { manifest -> showRemoteEmojis(manifest, isFromCache = false) },
                onFailure = { error ->
                    val items = remoteAssetRepository.getAssetsByType(
                        AssetConfig.Type.EMOJI, currentCategoryId
                    )
                    _remoteEmojiState.value = if (items.isNotEmpty()) {
                        RemoteAssetUiState.Offline(items)
                    } else {
                        RemoteAssetUiState.Error(
                            message = error.message ?: "Failed to load remote assets",
                            cause = error
                        )
                    }
                }
            )
        }
    }

    private fun showRemoteEmojis(manifest: AssetManifest, isFromCache: Boolean) {
        val items = manifest.getByType(AssetConfig.Type.EMOJI, currentCategoryId)
        // Skip identical lists so a background refresh doesn't rebind the grid
        if (_remoteEmojis.value != items) _remoteEmojis.value = items
        updateRemoteSearch(manifest.getByType(AssetConfig.Type.EMOJI))
        _remoteEmojiState.value = if (items.isEmpty()) {
            RemoteAssetUiState.Empty
        } else {
            RemoteAssetUiState.Success(items, isFromCache)
        }
    }

    /** Refreshes the manifest by invalidating the cache and re-fetching. */
    fun refreshRemoteEmojis() {
        loadRemoteEmojis()
    }
}
