import com.codder.openphonetest.api.IRemoteDataSource
import com.codder.openphonetest.api.ResponseState
import com.codder.openphonetest.api.StateOfGitResponse
import com.codder.openphonetest.api.UserDataInResponse
import com.codder.openphonetest.localDatasource.IGitUserLocalDatasource
import com.codder.openphonetest.model.GitUserData
import com.codder.openphonetest.networkState.INetworkStateCallbacksManager
import com.codder.openphonetest.networkState.NetworkStates
import com.codder.openphonetest.repositories.DataFlowStates
import com.codder.openphonetest.repositories.GitUserDataRepository
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GitUserDataRepositoryTest {

    val scheduler = TestCoroutineScheduler()
    @OptIn(ExperimentalCoroutinesApi::class)
    val testDispatcher = UnconfinedTestDispatcher(scheduler)

    private val listOfUsersFromServer = listOf(
        UserDataInResponse(
            id = 125,
            login = basePartialString+2,
            avatar_url = "imageUrl1",
            repos_url = "urlForRepositories1"
        ),
        UserDataInResponse(
            id = 124,
            login = basePartialString+1,
            avatar_url = "imageUrl1",
            repos_url = "urlForRepositories1"
        ),
    )

    @Test
    fun testGetUsersFromDatabaseReturnsFlow() {

        val localDatasource = mock<IGitUserLocalDatasource> {
            everySuspend { selectAllUsers() } returns flowOf(listOfUsersFromDatabase)
        }

        val remoteDataSource = mock<IRemoteDataSource> {}

        val networkStateCallbackManager = mock<INetworkStateCallbacksManager> {
            everySuspend { networkStateFlow() } returns flowOf(NetworkStates.Connected)
        }

        val repository = GitUserDataRepository(
            gitUserLocalDatasource = localDatasource,
            remoteDataSource = remoteDataSource,
            scope = CoroutineScope(testDispatcher),
            networkStateCallbackManager = networkStateCallbackManager
        )

        runTest {
            val flow = repository.getUsersFromDatabase()
            assertEquals(listOfUsersFromDatabase, flow.first())
        }
    }

    @Test
    fun testGetUsersAndOrganizationsWithNameFromRemoteDataSourceReturnSuccess() {

        val localDatasource = mock<IGitUserLocalDatasource>(MockMode.autoUnit) {}

        val networkStateCallbackManager = mock<INetworkStateCallbacksManager> {
            everySuspend { networkStateFlow() } returns flowOf(NetworkStates.Connected)
        }

       val remoteDataSource = mock<IRemoteDataSource> {
            everySuspend {
                getListOfUsersAndOrganizations(
                    partialName = basePartialString,
                    pageCounter = pageNumber2ToCall
                )
            } returns StateOfGitResponse(
                responseState = ResponseState.OK,
                hasNextLink = true,
                listOfUsers = listOfUsersFromServer
            )
        }

        val repository = GitUserDataRepository(
            gitUserLocalDatasource = localDatasource,
            remoteDataSource = remoteDataSource,
            scope = CoroutineScope(testDispatcher),
            networkStateCallbackManager = networkStateCallbackManager
        )

        runTest {
            val result = repository.getUsersAndOrganizationsWithNameFromRemoteDataSource(
                partialNameString = basePartialString,
                pageNumber = pageNumber2ToCall,
                firstRequest = false
            )

            assertEquals(DataFlowStates.Success, result)
        }
    }

    @Test
    fun testGetUsersAndOrganizationsWithNameFromRemoteDataSourceReturnEndOfData() {

        val localDatasource = mock<IGitUserLocalDatasource>(MockMode.autoUnit) {}

        val networkStateCallbackManager = mock<INetworkStateCallbacksManager> {
            everySuspend { networkStateFlow() } returns flowOf(NetworkStates.Connected)
        }

        val remoteDataSource = mock<IRemoteDataSource> {
            everySuspend {
                getListOfUsersAndOrganizations(
                    partialName = basePartialString,
                    pageCounter = pageNumber1ToCall
                )
            } returns StateOfGitResponse(
                responseState = ResponseState.OK,
                hasNextLink = false,
                listOfUsers = listOfUsersFromServer.subList(0, 1)
            )
        }

        val repository = GitUserDataRepository(
            gitUserLocalDatasource = localDatasource,
            remoteDataSource = remoteDataSource,
            scope = CoroutineScope(testDispatcher),
            networkStateCallbackManager = networkStateCallbackManager
        )

        runTest {
            val result = repository.getUsersAndOrganizationsWithNameFromRemoteDataSource(
                partialNameString = basePartialString,
                pageNumber = pageNumber1ToCall,
                firstRequest = true
            )

            assertEquals(DataFlowStates.EndOfData, result)
        }
    }

    @Test
    fun testGetUsersAndOrganizationsWithNameFromRemoteDataSourceReturnErrorWithRequest() {

        val localDatasource = mock<IGitUserLocalDatasource>(MockMode.autoUnit) {}

        val networkStateCallbackManager = mock<INetworkStateCallbacksManager> {
            everySuspend { networkStateFlow() } returns flowOf(NetworkStates.Connected)
        }

        val remoteDataSource = mock<IRemoteDataSource> {
            everySuspend {
                getListOfUsersAndOrganizations(
                    partialName = basePartialString,
                    pageCounter = pageNumber1ToCall
                )
            } returns StateOfGitResponse(
                responseState = ResponseState.Error,
                hasNextLink = false,
                listOfUsers = listOfUsersFromServer.subList(0, 1)
            )
        }

        val repository = GitUserDataRepository(
            gitUserLocalDatasource = localDatasource,
            remoteDataSource = remoteDataSource,
            scope = CoroutineScope(testDispatcher),
            networkStateCallbackManager = networkStateCallbackManager
        )

        runTest {
            val result = repository.getUsersAndOrganizationsWithNameFromRemoteDataSource(
                partialNameString = basePartialString,
                pageNumber = pageNumber1ToCall,
                firstRequest = true
            )
            verifySuspend(exactly(1)) {
                remoteDataSource.getListOfUsersAndOrganizations(partialName = "aaa", pageCounter = 1)
            }
            verifySuspend(exactly(0)) {
                localDatasource.saveOrUpdateUserData(any(), any(), any(), any(), any())
            }

            assertEquals(DataFlowStates.ErrorWithRequest, result)
        }
    }

    @Test
    fun testGetUsersAndOrganizationsWithNameFromRemoteDataSourceReturnNoDataFound() {

        val localDatasource = mock<IGitUserLocalDatasource>(MockMode.autoUnit) {}

        val networkStateCallbackManager = mock<INetworkStateCallbacksManager> {
            everySuspend { networkStateFlow() } returns flowOf(NetworkStates.Connected)
        }

        val remoteDataSource = mock<IRemoteDataSource> {
            everySuspend {
                getListOfUsersAndOrganizations(
                    partialName = basePartialString,
                    pageCounter = pageNumber1ToCall
                )
            } returns StateOfGitResponse(
                responseState = ResponseState.OK,
                hasNextLink = true,
                listOfUsers = listOf()
            )
        }

        val repository = GitUserDataRepository(
            gitUserLocalDatasource = localDatasource,
            remoteDataSource = remoteDataSource,
            scope = CoroutineScope(testDispatcher),
            networkStateCallbackManager = networkStateCallbackManager
        )

        runTest {
            val result = repository.getUsersAndOrganizationsWithNameFromRemoteDataSource(
                partialNameString = basePartialString,
                pageNumber = pageNumber1ToCall,
                firstRequest = true
            )

            assertEquals(DataFlowStates.NoDataFound, result)
        }
    }


    @Test
    fun testGetUsersAndOrganizationsWithNameFromRemoteDataSourceConfirmDatabaseCallsMadeWhenDataPresented() {

        val localDatasource = mock<IGitUserLocalDatasource>(MockMode.autoUnit) {}

        val networkStateCallbackManager = mock<INetworkStateCallbacksManager> {
            everySuspend { networkStateFlow() } returns flowOf(NetworkStates.Connected)
        }

        val remoteDataSource = mock<IRemoteDataSource> {
            everySuspend {
                getListOfUsersAndOrganizations(
                    partialName = basePartialString,
                    pageCounter = pageNumber1ToCall
                )
            } returns StateOfGitResponse(
                responseState = ResponseState.OK,
                hasNextLink = false,
                listOfUsers = listOfUsersFromServer
            )
        }

        val repository = GitUserDataRepository(
            gitUserLocalDatasource = localDatasource,
            remoteDataSource = remoteDataSource,
            scope = CoroutineScope(testDispatcher),
            networkStateCallbackManager = networkStateCallbackManager
        )

        runTest {
            repository.getUsersAndOrganizationsWithNameFromRemoteDataSource(
                partialNameString = basePartialString,
                pageNumber = pageNumber1ToCall,
                firstRequest = true
            )
            verifySuspend(exactly(2)) {
                localDatasource.saveOrUpdateUserData(any(), any(), any(), any(), any())
            }
        }
    }

    @Test
    fun testGetUsersAndOrganizationsWithNameFromRemoteDataSourceVerifyDataDeletionCalledIfFirstRequestForName() {

        val localDatasource = mock<IGitUserLocalDatasource>(MockMode.autoUnit) {}

        val networkStateCallbackManager = mock<INetworkStateCallbacksManager> {
            everySuspend { networkStateFlow() } returns flowOf(NetworkStates.Connected)
        }

        val remoteDataSource = mock<IRemoteDataSource> {
            everySuspend {
                getListOfUsersAndOrganizations(
                    partialName = basePartialString,
                    pageCounter = pageNumber2ToCall
                )
            } returns StateOfGitResponse(
                responseState = ResponseState.OK,
                hasNextLink = true,
                listOfUsers = listOfUsersFromServer
            )
        }

        val repository = GitUserDataRepository(
            gitUserLocalDatasource = localDatasource,
            remoteDataSource = remoteDataSource,
            scope = CoroutineScope(testDispatcher),
            networkStateCallbackManager = networkStateCallbackManager
        )

        runTest {
            repository.getUsersAndOrganizationsWithNameFromRemoteDataSource(
                partialNameString = basePartialString,
                pageNumber = pageNumber2ToCall,
                firstRequest = true
            )

            verifySuspend(exactly(1)) {
                localDatasource.deleteAllUsers()
            }

        }
    }

    @Test
    fun testRequestNumberOfRepositoriesForUserVerifyUpdateNumberOfRepositoriesCalled() {
        val numberOfRepositoriesToReturn = 4

        val localDatasource = mock<IGitUserLocalDatasource>(MockMode.autoUnit) {}

        val networkStateCallbackManager = mock<INetworkStateCallbacksManager> {
            everySuspend { networkStateFlow() } returns flowOf(NetworkStates.Connected)
        }

        val remoteDataSource = mock<IRemoteDataSource> {
            everySuspend {
                getNumberOfRepositoriesForOrganization(
                   listOfUsersFromDatabase[0].urlForRepositories
                )
            } returns numberOfRepositoriesToReturn
        }

        val repository = GitUserDataRepository(
            gitUserLocalDatasource = localDatasource,
            remoteDataSource = remoteDataSource,
            scope = CoroutineScope(testDispatcher),
            networkStateCallbackManager = networkStateCallbackManager
        )

        runTest {
            repository.requestNumberOfRepositoriesForUser(
                listOfUsersFromDatabase[0]
            )

            verifySuspend(exactly(1)) {
                localDatasource.updateNumberOfRepositories(
                    listOfUsersFromDatabase[0].id,
                    numberOfRepositoriesToReturn
                )
            }
        }
    }

    companion object {
        val basePartialString = "aaa"

        val listOfUsersFromDatabase = listOf(
            GitUserData(
                id = "123",
                name = basePartialString,
                imageUrl = "imageUrl",
                urlForRepositories = "urlForRepositories",
                numberOfRepositories = 6
            ),
            GitUserData(
                id = "124",
                name = basePartialString+1,
                imageUrl = "imageUrl1",
                urlForRepositories = "urlForRepositories1",
                numberOfRepositories = null
            ),
            GitUserData(
                id = "125",
                name = basePartialString+2,
                imageUrl = "imageUrl1",
                urlForRepositories = "urlForRepositories1",
                numberOfRepositories = null
            )
        )

        val pageNumber1ToCall = 1
        val pageNumber2ToCall = 2
    }

}