package com.codder.openphonetest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codder.openphonetest.localDatasource.DatabaseQueryParameters
import com.codder.openphonetest.model.GitUserData
import com.codder.openphonetest.networkState.NetworkStates
import com.codder.openphonetest.repositories.IGitUserDataRepository
import com.codder.openphonetest.repositories.DataFlowStates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class GitDataViewModel(
    private val repository: IGitUserDataRepository
): ViewModel() {

    private val queryParameters: MutableStateFlow<DatabaseQueryParameters?> = MutableStateFlow(
        null
    )

    private val _uiState = MutableStateFlow(
        UiStateData(UiStates.Initial)
    )

    val uiState: StateFlow<UiStateData>
        get() = _uiState

    init {
        viewModelScope.launch {
            repository.networkState.distinctUntilChanged().collect { state ->
                if (state == NetworkStates.Lost) {
                    _uiState.value = UiStateData(UiStates.NoNetwork)
                } else if (uiState.value.uiStates == UiStates.NoNetwork) {
                    _uiState.value = UiStateData(UiStates.Ok)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val users: Flow<List<GitUserData>> = queryParameters
        .flatMapLatest { param ->
            repository.getUsersFromDatabase(param)
        }

    suspend fun getUsersFromDataSource(
        partialNameString: String,
        firstRequest: Boolean,
        pageNumber: Int
    ) {
        if (uiState.value.uiStates == UiStates.NoNetwork) {
            //filtering database records by the name
            queryParameters.value = DatabaseQueryParameters(
                partialNameString = partialNameString,
                currentPageNumber = pageNumber,
                appending = false
            )
            
        } else {
            _uiState.value = UiStateData(UiStates.Loading)
            queryParameters.value = null
            val requestStatus = repository.getUsersAndOrganizationsWithNameFromRemoteDataSource(
                partialNameString = partialNameString,
                pageNumber = pageNumber,
                firstRequest = firstRequest
            )
            when (requestStatus) {
                DataFlowStates.Success -> {
                    _uiState.value = UiStateData(UiStates.Ok)
                }
                DataFlowStates.EndOfData -> {
                    _uiState.value = UiStateData(UiStates.AllDataLoaded)
                }
                DataFlowStates.NoDataFound -> {
                    _uiState.value = UiStateData(UiStates.NoDataFound)
                }
                DataFlowStates.ErrorWithRequest -> {
                    _uiState.value = UiStateData(UiStates.Error)
                }
            }
        }
    }

    suspend fun requestNumberOfRepositoriesForUser(user: GitUserData) {
        repository.requestNumberOfRepositoriesForUser(user)
    }

}

enum class UiStates {
    Initial, Loading, Ok, Error, NoNetwork, AllDataLoaded, NoDataFound
}

data class UiStateData(val uiStates: UiStates) {
    val message: String = when (uiStates) {
        UiStates.Initial, UiStates.Ok -> ""
        UiStates.Loading -> "Loading data..."
        UiStates.Error -> "Error with your request! Try again later."
        UiStates.NoNetwork -> "No network. You can search in the local cache!"
        UiStates.AllDataLoaded -> "All available data has been loaded."
        UiStates.NoDataFound -> "No data found for your request."
    }
}

