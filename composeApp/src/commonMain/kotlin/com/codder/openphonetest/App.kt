package com.codder.openphonetest

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.codder.openphonetest.TestTags.CLICKABLE_ROW_FOR_USER
import com.codder.openphonetest.TestTags.DEFAULT
import com.codder.openphonetest.TestTags.ERROR_MESSAGE_TEXT
import com.codder.openphonetest.TestTags.INPUT_FIELD
import com.codder.openphonetest.TestTags.NUMBER_OF_REPOSITORIES_TEXT
import com.codder.openphonetest.TestTags.PROGRESS_INDICATOR
import com.codder.openphonetest.TestTags.SUBMIT_BUTTON
import com.codder.openphonetest.TestTags.USER_LOGO_IMAGE
import com.codder.openphonetest.TestTags.USER_NAME_TEXT
import com.codder.openphonetest.model.GitUserData
import com.codder.openphonetest.repositories.GitUserDataRepository
import com.codder.openphonetest.repositories.IGitUserDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import openphonetest.composeapp.generated.resources.Res
import openphonetest.composeapp.generated.resources.click_on_repos_message
import openphonetest.composeapp.generated.resources.image_content_description
import openphonetest.composeapp.generated.resources.input_field_label
import openphonetest.composeapp.generated.resources.no_repositories_loaded
import openphonetest.composeapp.generated.resources.number_of_repos_text
import openphonetest.composeapp.generated.resources.submit_button_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun App(
    repo: IGitUserDataRepository
) {

    val viewModel = viewModel<GitDataViewModel> {
        GitDataViewModel(repo)
    }
    val scope = rememberCoroutineScope()

    val userFlow = remember { viewModel.users }
    val users = userFlow.collectAsStateWithLifecycle(listOf()).value

    val uiStateDataFlow = remember { viewModel.uiState }
    val uiState = uiStateDataFlow.collectAsStateWithLifecycle().value

    GitSearchMainScreen(
        users = users,
        uiStateData = uiState,
        onRequestMoreData = {name, pageNumber, firstRequestForName ->
            scope.launch(Dispatchers.IO) {
                viewModel.getUsersFromDataSource(name, firstRequestForName, pageNumber)
            }

        },
        onRequestNumberOfRepositories = {user ->
            scope.launch(Dispatchers.IO) {
                viewModel.requestNumberOfRepositoriesForUser(user)
            }
        }
    )
}

@Composable
fun UsersListViewItem(
    data: GitUserData,
    onNumberOfRepositoriesRequested: (data: GitUserData) -> Unit
) {

    Card (
        modifier = Modifier
            .padding(4.dp)
            .wrapContentHeight()
            .fillMaxWidth()

    ){
        Row (
            modifier = Modifier
                .padding(4.dp)
                .clickable {
                   onNumberOfRepositoriesRequested(data)
                }
                .testTag(CLICKABLE_ROW_FOR_USER.plus(data.id))
                .wrapContentHeight()
                .fillMaxWidth()
        ){
            Column(modifier = Modifier.weight(7f)) {

                OpenPhoneText(
                    text = data.name,
                    testTag = USER_NAME_TEXT
                )

                Spacer(modifier = Modifier.width(20.dp))

                val text = if (data.numberOfRepositories == null) {
                    stringResource(Res.string.click_on_repos_message)
                } else if (data.numberOfRepositories > 0) {
                    stringResource(Res.string.number_of_repos_text, data.numberOfRepositories)
                } else {
                    stringResource(Res.string.no_repositories_loaded)
                }
                OpenPhoneText(
                    text = text,
                    testTag = NUMBER_OF_REPOSITORIES_TEXT
                )
            }

            AsyncImage(
                modifier = Modifier
                    .testTag(USER_LOGO_IMAGE)
                    .size(50.dp),
                model = data.imageUrl,
                contentDescription = stringResource(Res.string.image_content_description, data.name)
            )
        }
    }
}

@Composable
fun GitSearchMainScreen(
    users: List<GitUserData>,
    uiStateData: UiStateData,
    onRequestMoreData: (name: String, pageNumber: Int, firstRequestForName: Boolean) -> Unit,
    onRequestNumberOfRepositories: (GitUserData) -> Unit
) {

    MaterialTheme {

        var inputName by rememberSaveable { mutableStateOf("") }

        var previousVisibleIndex by rememberSaveable { mutableStateOf(0) }
        var previousVisibleItemOffset by rememberSaveable { mutableStateOf(0) }

        var lastTriggeredIndexDown by remember { mutableStateOf(-1) }
        var lastTriggeredIndexUp by remember { mutableStateOf(-1) }

        var pageNumber by remember { mutableStateOf(1) }

        val lazyListState = rememberLazyListState()

        LaunchedEffect(lazyListState) {

            snapshotFlow { lazyListState.layoutInfo }
                .distinctUntilChanged()
                .collectLatest { layoutInfo ->

                    val firstVisibleInfoOffset = lazyListState.firstVisibleItemScrollOffset
                    val firstIndexFromLazyListState = lazyListState.firstVisibleItemIndex

                    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    val firstVisibleItemIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index
                    val totalItemsCount = layoutInfo.totalItemsCount

                    val isScrollingUp = isScrollingUp(previousVisibleIndex, previousVisibleItemOffset, firstIndexFromLazyListState, firstVisibleInfoOffset)
                    previousVisibleItemOffset = firstVisibleInfoOffset
                    previousVisibleIndex = firstIndexFromLazyListState

                    if (lastVisibleItemIndex != null && firstVisibleItemIndex != null) {
                        if (!isScrollingUp) {
                            //scrolling down
                            if (lastVisibleItemIndex >= totalItemsCount-2) {
                                //scrolling down
                                if (inputName.isNotEmpty()) {
                                    pageNumber += 1
                                    onRequestMoreData(
                                        inputName, pageNumber, false
                                    )
                                    lastTriggeredIndexDown = lastVisibleItemIndex
                                }
                            }

                        } else {

                            if (firstVisibleItemIndex < 2) {
                                if (inputName.isNotEmpty()) {
                                    pageNumber -= 1
                                    onRequestMoreData(inputName, pageNumber, false)
                                    lastTriggeredIndexUp = lastVisibleItemIndex
                                }
                            }
                        }
                    }
                }
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            OutlinedTextField(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .testTag(INPUT_FIELD),
                value = inputName,
                onValueChange = { value ->
                    inputName = value
                },
                label = {
                    OpenPhoneText(
                        text = stringResource(Res.string.input_field_label),
                        testTag = DEFAULT
                    )
                }
            )

            Button(
                modifier = Modifier
                    .testTag(SUBMIT_BUTTON)
                    .wrapContentSize(),
                onClick = {
                    onRequestMoreData(inputName, pageNumber, true)

                }) {
                OpenPhoneText(
                    text = stringResource(Res.string.submit_button_title),
                    testTag = DEFAULT
                )
            }

            if (uiStateData.uiStates != UiStates.Initial && uiStateData.uiStates != UiStates.Ok) {
                OpenPhoneText(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .wrapContentSize(),
                    fontWeight = FontWeight.Bold,
                    text = uiStateData.message,
                    testTag = ERROR_MESSAGE_TEXT
                )
            }

            if (uiStateData.uiStates == UiStates.Loading && users.isEmpty()) {

                CustomCircularProgressIndicator(
                    modifier = Modifier
                        .testTag(PROGRESS_INDICATOR)
                        .size(200.dp)
                )
            } else {
                if (uiStateData.uiStates != UiStates.Initial) {

                    LazyColumn(state = lazyListState) {
                        itemsIndexed(items = users) { index, item ->
                            UsersListViewItem(
                                data = item,
                                onNumberOfRepositoriesRequested = { user ->
                                    onRequestNumberOfRepositories(user)
                                }
                            )
                        }

                    }
                }
            }
        }
    }

}

@Composable
fun OpenPhoneText(
    text: String,
    testTag: String,
    modifier: Modifier = Modifier
        .wrapContentSize()
        .testTag(testTag),
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 16.sp,
        fontWeight = fontWeight
    )
}

fun isScrollingUp(
    previousFirstIndex: Int,
    previousFirstOffset: Int,
    currentFirstIndex: Int,
    currentFirstOffset: Int
): Boolean {
    return if (previousFirstIndex != currentFirstIndex) {
        previousFirstIndex > currentFirstIndex
    } else {
        previousFirstOffset > currentFirstOffset
    }
}

@Composable
fun CustomCircularProgressIndicator(
    modifier: Modifier = Modifier
        .background(Color.Transparent)
        .fillMaxSize()
) {

    val transition = rememberInfiniteTransition()

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(150.dp)
        ) {
            androidx.compose.material.CircularProgressIndicator(
                progress = 0.5f, // Set your progress here
                modifier = Modifier
                    .size(120.dp)
                    .rotate(rotation)
            )
        }

    }
}

