package com.lji.assignment.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lji.assignment.domain.model.GHRepo
import com.lji.assignment.domain.repository.GitHubRepository
import com.lji.assignment.utils.NetworkObserver
import com.lji.assignment.utils.NetworkStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * Data class representing the complete UI state of the RepoListScreen.
 * This acts as a single source of truth for the composable, making state management
 * cleaner and more predictable.
 *
 * @property repositorySearchQuery The query entered by the user for the network search.
 * @property localFilterQuery The query used to filter already-cached repositories.
 * @property allFoundRepos The complete, unfiltered list of repositories fetched from the network.
 * @property filteredRepos The list of repositories displayed to the user after local filtering.
 * @property isLoading A flag to show/hide the loading indicator.
 * @property errorMessage A user-facing message to display network or other errors.
 * @property isNetworkAvailable A flag indicating the current network status.
 * @property networkStatusMessage An optional message to display for network status (e.g., "Offline").
 */
data class RepoListUiState(
    val repositorySearchQuery: String = "",
    val localFilterQuery: String = "",
    val allFoundRepos: List<GHRepo> = emptyList(),
    val filteredRepos: List<GHRepo> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isNetworkAvailable: Boolean = true,
    val networkStatusMessage: String? = null
)

/**
 * ViewModel for the GitHub profile viewer.
 *
 * This ViewModel orchestrates data flow using Kotlin Flows, managing the UI state,
 * handling network requests, and providing an efficient in-memory filtering mechanism.
 *
 * @param repository The data repository for fetching and caching GitHub data.
 * @param networkObserver Observes the device's network connectivity status.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class GitHubViewModel @Inject constructor(
    private val repository: GitHubRepository,
    private val networkObserver: NetworkObserver
) : ViewModel() {
    private val _uiState = MutableStateFlow(RepoListUiState())
    val uiState: StateFlow<RepoListUiState> = _uiState.asStateFlow()

    // This state flow holds the last query that was attempted while offline.
    private val _lastOfflineQuery = MutableStateFlow("")

    init {
        // Observe network connectivity and update the UI state.
        viewModelScope.launch {
            networkObserver.observe().collect { status ->
                _uiState.update { currentState ->
                    val isAvailable = status is NetworkStatus.Available
                    val networkMessage = if (isAvailable) null else "Offline"
                    currentState.copy(isNetworkAvailable = isAvailable, networkStatusMessage = networkMessage)
                }

                // If the network becomes available, re-attempt the last offline search.
                if (status is NetworkStatus.Available && _lastOfflineQuery.value.isNotBlank()) {
                    searchRepositories(_lastOfflineQuery.value)
                    _lastOfflineQuery.value = "" // Clear the last offline query
                }
            }
        }
        setupFlows()
    }

    /**
     * Sets up the main search and local filter flows for the ViewModel.
     * This method is called from the init block to keep it clean.
     */
    private fun setupFlows() {
        // This flow handles the main search functionality.
        // 1. It observes changes to the repositorySearchQuery from the UI.
        // 2. The `debounce` prevents excessive API calls as the user types.
        // 3. The `distinctUntilChanged` prevents duplicate API calls for the same query.
        // 4. The `flatMapLatest` is crucial here: it cancels any previous network request
        //    if a new search query comes in, ensuring only the latest search is active.
        viewModelScope.launch {
            _uiState.map { it.repositorySearchQuery }
                .debounce(500)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .flatMapLatest { query ->
                    // Set loading and explicitly clear any previous error.
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    // Directly call the repository, the catch block will handle network errors.
                    repository.getRepositoriesBySearch(query)
                        .onStart { _uiState.update { it.copy(isLoading = true) } }
                        .catch { e ->
                            // If a network error occurs, store the current query for re-attempt.
                            val errorMessage = if (e is IOException) {
                                _lastOfflineQuery.value = query
                                "Network error. Please check your connection."
                            } else {
                                "An unknown error occurred."
                            }

                            _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
                        }
                }
                .collect { repos ->
                    // When new repos are received, update the allFoundRepos state.
                    _uiState.update { it.copy(isLoading = false, allFoundRepos = repos, filteredRepos = repos) }
                }
        }

        // This flow handles the local filtering of the displayed repos.
        // It combines the list of all found repos with the local filter query.
        // This operation is performed in-memory, so it's very fast and efficient.
        viewModelScope.launch {
            combine(
                _uiState.map { it.allFoundRepos },
                _uiState.map { it.localFilterQuery }.distinctUntilChanged()
            ) { allRepos, localQuery ->
                if (localQuery.isBlank()) {
                    allRepos
                } else {
                    allRepos.filter { repo ->
                        repo.name.contains(localQuery, ignoreCase = true) ||
                                repo.ownerLogin.contains(localQuery, ignoreCase = true) ||
                                repo.id.toString().contains(localQuery)
                    }
                }
            }.collect { filteredList ->
                // Update the filteredRepos state, which is what the UI displays.
                _uiState.update { it.copy(filteredRepos = filteredList) }
            }
        }
    }

    /**
     * Updates the repository search query. This is called by the UI.
     * It also resets the local filter to ensure a clean state for the new search.
     */
    fun searchRepositories(newQuery: String) {
        _uiState.update {
            it.copy(
                repositorySearchQuery = newQuery,
                localFilterQuery = "",
            )
        }
    }

    /**
     * Updates the local filter query to filter the displayed repositories in real-time.
     */
    fun updateLocalFilterQuery(query: String) {
        _uiState.update { it.copy(localFilterQuery = query) }
    }

    /**
     * Clears any active error message. This can be called from the UI, for example, when a snackbar is dismissed.
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
