package screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.codder.openphonetest.GitSearchMainScreen
import com.codder.openphonetest.TestTags.CLICKABLE_ROW_FOR_USER
import com.codder.openphonetest.TestTags.INPUT_FIELD
import com.codder.openphonetest.TestTags.NUMBER_OF_REPOSITORIES_TEXT
import com.codder.openphonetest.TestTags.PROGRESS_INDICATOR
import com.codder.openphonetest.TestTags.SUBMIT_BUTTON
import com.codder.openphonetest.TestTags.USER_NAME_TEXT
import com.codder.openphonetest.UiStateData
import com.codder.openphonetest.UiStates
import com.codder.openphonetest.model.GitUserData
import kotlin.test.Test

class ComposeTest {

    private val users = listOf(
        GitUserData(
            id = "123",
            name = "Abc",
            imageUrl = "",
            urlForRepositories = "",
            numberOfRepositories = 5
        ),
        GitUserData(
            id = "1234",
            name = "Abcd",
            imageUrl = "",
            urlForRepositories = "",
            numberOfRepositories = 4
        ),
        GitUserData(
            id = "12345",
            name = "Abcdee",
            imageUrl = "",
            urlForRepositories = "",
            numberOfRepositories = null
        )

    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testGitSearchMainScreenOkStatus() = runComposeUiTest {

        setContent {
            GitSearchMainScreen(
                users = users,
                uiStateData = UiStateData(uiStates = UiStates.Ok),
                onRequestMoreData = {_, _, _ ->},
                onRequestNumberOfRepositories = {}
            )
        }

        onNodeWithTag(SUBMIT_BUTTON).assertHasClickAction()
        onNodeWithTag(INPUT_FIELD).assertIsDisplayed()
        onNodeWithTag(INPUT_FIELD).assertIsEnabled()

        users.forEach { user ->
            onNodeWithText(user.name).assertIsDisplayed()

            onNodeWithTag(CLICKABLE_ROW_FOR_USER.plus(user.id)).assertHasClickAction()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testGitSearchMainScreenNoNetwork() = runComposeUiTest {

        setContent {
            GitSearchMainScreen(
                users = users,
                uiStateData = UiStateData(uiStates = UiStates.NoNetwork),
                onRequestMoreData = {_, _, _ ->},
                onRequestNumberOfRepositories = {}
            )
        }

        onNodeWithTag(SUBMIT_BUTTON).assertHasClickAction()
        onNodeWithTag(INPUT_FIELD).assertIsDisplayed()
        onNodeWithTag(INPUT_FIELD).assertIsEnabled()
        onNodeWithText(UiStateData(UiStates.NoNetwork).message).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testGitSearchMainScreenInitState() = runComposeUiTest {

        setContent {
            GitSearchMainScreen(
                users = users,
                uiStateData = UiStateData(uiStates = UiStates.Initial),
                onRequestMoreData = {_, _, _ ->},
                onRequestNumberOfRepositories = {}
            )
        }

        onNodeWithTag(SUBMIT_BUTTON).assertHasClickAction()
        onNodeWithTag(INPUT_FIELD).assertIsDisplayed()
        onNodeWithTag(INPUT_FIELD).assertIsEnabled()


        users.forEach { user ->
            onNodeWithText(user.name).assertIsNotDisplayed()
            onNodeWithContentDescription("Image for the repository of ${user.name}").assertIsNotDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testGitSearchMainScreenLoadingState() = runComposeUiTest {

        setContent {
            GitSearchMainScreen(
                users = listOf(),
                uiStateData = UiStateData(uiStates = UiStates.Loading),
                onRequestMoreData = {_, _, _ ->},
                onRequestNumberOfRepositories = {}
            )
        }

        onNodeWithTag(SUBMIT_BUTTON).assertHasClickAction()
        onNodeWithTag(INPUT_FIELD).assertIsDisplayed()
        onNodeWithTag(INPUT_FIELD).assertIsEnabled()
        onNodeWithTag(PROGRESS_INDICATOR).assertIsEnabled()

    }
}