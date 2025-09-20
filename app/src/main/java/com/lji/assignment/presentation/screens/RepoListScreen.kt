package com.lji.assignment.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lji.assignment.R
import com.lji.assignment.domain.model.GHRepo
import com.lji.assignment.presentation.viewmodel.GitHubViewModel
import kotlinx.coroutines.launch

/**
 * Main composable screen for searching GitHub repositories.
 *
 * This screen manages the entire UI state, including the search bar, loading indicators,
 * error messages via snackbars, and the list of repositories. It observes a single
 * UI state from the ViewModel to render the correct view.
 *
 * @param viewModel The Hilt-injected ViewModel that holds the UI state and business logic.
 * @param onRepoClick A callback function to handle clicks on a repository item, typically for navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoListScreen(
    viewModel: GitHubViewModel = hiltViewModel(),
    onRepoClick: (String) -> Unit
) {
    // Observe the single UI state flow from the ViewModel.
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    // LaunchedEffect to show a snackbar when an API-related error message is present.
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "Dismiss",
                    duration = SnackbarDuration.Short
                )
                viewModel.clearErrorMessage()
            }
        }
    }

    // LaunchedEffect to show a snackbar for continuous network status messages.
    LaunchedEffect(uiState.networkStatusMessage) {
        uiState.networkStatusMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            }
        } ?: run {
            // If networkStatusMessage becomes null (network available again), dismiss any active network snackbar
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("GitHub Repositories") },
                navigationIcon = {
                    Image(
                        painter = painterResource(R.drawable.github_icon),
                        contentDescription = "github_icon"
                    )
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // GitHub Repository Search Bar
            OutlinedTextField(
                value = uiState.repositorySearchQuery,
                onValueChange = { newQuery ->
                    // Allow clearing the query even offline
                    viewModel.searchRepositories(newQuery)
                    if(!uiState.isNetworkAvailable) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Cannot search for new repositories while offline.",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                },
                label = { Text("Search GitHub Repositories") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Repos") },
                trailingIcon = {
                    if (uiState.repositorySearchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.searchRepositories("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Repo Search")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp), // Allow clearing existing query
            )

            // Local Repository Filter Search Bar
            AnimatedVisibility(
                visible = uiState.allFoundRepos.isNotEmpty(), // Only show if we have some repos to filter
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                OutlinedTextField(
                    value = uiState.localFilterQuery,
                    onValueChange = { viewModel.updateLocalFilterQuery(it) },
                    label = { Text("Filter Results by Name or ID") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Filter Results") },
                    trailingIcon = {
                        if (uiState.localFilterQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateLocalFilterQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Local Filter")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Searching repositories...", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    // Handle specific messages based on network and search state
                    !uiState.isNetworkAvailable && uiState.allFoundRepos.isEmpty() && uiState.repositorySearchQuery.isBlank() -> {
                        Text(
                            "You are offline. Cannot fetch new repositories.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    !uiState.isNetworkAvailable && uiState.allFoundRepos.isNotEmpty() && uiState.filteredRepos.isEmpty() -> {
                        Text(
                            "You are offline. No matching offline results for '${uiState.localFilterQuery}'.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    uiState.repositorySearchQuery.isBlank() && uiState.allFoundRepos.isEmpty() -> {
                        Text(
                            "Enter a search query to find GitHub repositories.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    uiState.filteredRepos.isEmpty() && !uiState.isLoading -> {
                        Text(
                            "No repositories found for query '${uiState.repositorySearchQuery}' or filter '${uiState.localFilterQuery}'.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.filteredRepos, key = { it.id }) { repo ->
                                RepoListItem(
                                    repo = repo,
                                    onClick = {
                                        if (uiState.isNetworkAvailable) {
                                            onRepoClick(repo.repoURL)
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "No network connection. Cannot open repository link.",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A composable function to display a single repository item in a list.
 *
 * @param repo The repository data class to display.
 * @param onClick The callback function to be invoked when the item is clicked.
 */
@Composable
fun RepoListItem(repo: GHRepo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = repo.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ID: ${repo.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Owner: ${repo.ownerLogin}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = repo.repoURL,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}