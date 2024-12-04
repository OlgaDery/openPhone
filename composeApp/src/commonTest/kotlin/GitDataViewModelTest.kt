import GitUserDataRepositoryTest.Companion.listOfUsersFromDatabase
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codder.openphonetest.GitDataViewModel
import com.codder.openphonetest.UiStates
import com.codder.openphonetest.localDatasource.DatabaseQueryParameters
import com.codder.openphonetest.networkState.NetworkStates
import com.codder.openphonetest.repositories.IGitUserDataRepository
import com.codder.openphonetest.repositories.DataFlowStates
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


class GitDataViewModelTest {

    val scheduler = TestCoroutineScheduler()
    @OptIn(ExperimentalCoroutinesApi::class)
    val testDispatcher = UnconfinedTestDispatcher(scheduler)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGetUsersFromDataSourceAssertUiStateOk() {
        val viewModel = initViewModelAndTestDependencies(
            serverSideCallReturns = DataFlowStates.Success
        )

        runTest {
            viewModel.getUsersFromDataSource(
                firstRequest = false,
                partialNameString = "abc",
                pageNumber = 3
            )
            assertEquals(UiStates.Ok, viewModel.uiState.value.uiStates)
        }
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGetUsersFromDataSourceAssertUiStateError() {
        val viewModel = initViewModelAndTestDependencies(
            serverSideCallReturns = DataFlowStates.ErrorWithRequest
        )

        runTest {
            viewModel.getUsersFromDataSource(
                firstRequest = false,
                partialNameString = "abc",
                pageNumber = 3
            )
            assertEquals(UiStates.Error, viewModel.uiState.value.uiStates)
        }
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGetUsersFromDataSourceAssertInitialUiState() {
        val viewModel =  initViewModelAndTestDependencies(
            serverSideCallReturns = null
        )
        assertEquals(UiStates.Initial, viewModel.uiState.value.uiStates)
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGetUsersFromDataSourceAssertUiStateAllDataLoaded() {
        val viewModel = initViewModelAndTestDependencies(
            serverSideCallReturns = DataFlowStates.EndOfData
        )
        runTest {
            viewModel.getUsersFromDataSource(
                firstRequest = false,
                partialNameString = "abc",
                pageNumber = 3
            )
            assertEquals(UiStates.AllDataLoaded, viewModel.uiState.value.uiStates)
        }
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testNetworkStateAssertUiStateNoNetwork() {
        val viewModel = initViewModelAndTestDependencies(
            networkStates = NetworkStates.Lost,
            serverSideCallReturns = null
        )

        runTest {
            assertEquals(UiStates.NoNetwork, viewModel.uiState.value.uiStates)
        }

        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testValidateGetUsersAndOrganizationsWithNameFromRemoteDataSourceNotCalledWithNoNetwork() {

        val partialName = "asd"
        val firstRequest = false
        val pageNumber = 1

        Dispatchers.setMain(testDispatcher)

        val returnFlow = flowOf(NetworkStates.Lost)

        val repository = mock<IGitUserDataRepository> {

            everySuspend { networkState } returns returnFlow
        }
        val viewModel = GitDataViewModel(repository)

        runTest {
            viewModel.getUsersFromDataSource(
                firstRequest = firstRequest,
                pageNumber = pageNumber,
                partialNameString = partialName
            )
            verifySuspend(exactly(0)) {
                repository.getUsersAndOrganizationsWithNameFromRemoteDataSource(
                    any(),
                    any(),
                    any()
                )
            }
        }

        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testValidateExpectedUsersFlowReturnedWithNonNullableQueryParameters() {

        val partialName = "asd"
        val firstRequest = false
        val pageNumber = 1

        val queryParams = DatabaseQueryParameters(
            currentPageNumber = pageNumber,
            partialNameString = partialName,
            appending = false
        )

        Dispatchers.setMain(testDispatcher)

        val returnFlow = flowOf(NetworkStates.Lost)

        val repository = mock<IGitUserDataRepository> {

            everySuspend { networkState } returns returnFlow

            everySuspend {
                getUsersFromDatabase(null)
            } returns flowOf(listOfUsersFromDatabase)

            everySuspend {
                getUsersFromDatabase(queryParams)
            } returns flowOf(listOfUsersFromDatabase.subList(0, 1))
        }
        val viewModel = GitDataViewModel(repository)

        runTest {
            viewModel.getUsersFromDataSource(
                firstRequest = firstRequest,
                pageNumber = pageNumber,
                partialNameString = partialName
            )
            assertEquals(listOfUsersFromDatabase.subList(0, 1), viewModel.users.first())
        }

        Dispatchers.resetMain()
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initViewModelAndTestDependencies(
        serverSideCallReturns: DataFlowStates?,
        networkStates: NetworkStates = NetworkStates.Connected
    ): GitDataViewModel {
        Dispatchers.setMain(testDispatcher)

        val returnFlow = flowOf(networkStates)

        val repository = mock<IGitUserDataRepository>() {

            serverSideCallReturns?.let {
                everySuspend {
                    getUsersAndOrganizationsWithNameFromRemoteDataSource(
                    "abc",
                    3,
                    false
                ) } returns serverSideCallReturns
            }

            everySuspend { networkState } returns returnFlow
        }
        return GitDataViewModel(repository)
    }
}