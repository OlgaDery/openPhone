1. This is a Kotlin Multiplatform project targeting Android and iOS. To run on both platforms, insert the Git private token (fine-grained) value to RemoteDataSource class -> token in the "companion" object.
   In the production app, the token should be stored in the appropriate platform-specific mechanism (Android Keystore/Keychain).
2. Libraries
   I tried using libraries that are commonly used, regularly supported, and, ideally, officially recommended for KMP (Ktor for network calls, SqlDelight for local storage, Kotlin coroutines/flows, Koin, Coil) and, ideally, those I have some experience with.
   For testing (mocking), I used the "dev.mokkery" library for the first time because "io.mockative," which I have more experience with, didn't compile with the current configuration.

3. Project structure for iOS
   Since the project didn't require any iOS-specific dependencies that couldn't be set via Gradle configuration, I configured the iOS project as a regular framework (without using CocoaPods or similar tools).

4. UI sharing
   While deciding whether to share the UI layer between platforms or design a shared composable layer, I considered the following points:

- The app's UI should support pagination (considering the specifics of the given API). I didn't find any reliable CMP implementations for pagination. (There are rumors that the "androidx.paging" library is now multiplatform, but I wasn't able to build the project when using these dependencies in the shared configuration.)
- Custom pagination (if I decide to design it myself) may lack the UX advantages of native libraries and may not be well-optimized.
- Platform-specific UI components with pagination may take too long to implement. Additionally, the "androidx.paging" library has platform-specific dependencies on the lower layers (data and business logic), which would make the work even longer since I would need to abstract these components into platform-specific modules and design iOS equivalents.

Considering the potential risks, I decided to proceed with the second option (custom shared UI code).

5. Overall design
   Since there was an optional requirement to integrate local storage for data caching, I decided to use the "repository" pattern as it's a convenient way to coordinate multiple data sources. The SqlDelight database was used as the single source of truth, and the data was streamed to the UI directly from there (via the ViewModel). This aligns with Android's official guidelines and is a simple, easy-to-test implementation.
   To observe changes in the network state, I built a number of classes to consume callback flows from platform-specific listeners.

6. Dependencies distribution
   Since the number of classes is relatively small, I mostly used manual dependency injection, providing dependencies as constructor parameters. This creates a clear hierarchy of dependencies and is very unit-test-friendly. Additionally, I used Koin in the native Android and iOS entry points (in the Application classes) to ensure the required classes are initialized as singletons per the app's instance lifecycle.

7. Paginator
   I implemented standard logic for custom pagination, where new pages are requested based on user activity (by tracking the state of the lazy column displaying data items and making the next call when the user is close to the end). This allows new data to be appended to the list smoothly, providing a good user experience. I also experimented with offsetting display data from the database or querying only certain pages to keep a reasonable amount of data in memory. However, I haven't finalized the solution, as I tried a few algorithms and haven't achieved the desired combination of smooth UX and simplicity.

8. Unit tests
   The functions of the repository and ViewModel classes are unit-tested. I used various capabilities from "kotlinx.coroutines.test" for running coroutines in tests and setting up test dispatchers, including one for the ViewModel scope. For mocking, I used "dev.mokkery." Due to time constraints, I haven't tested lower-level functionality from the remote and local data sources or the "NetworkStateCallbacksManager," but this can be easily done by wrapping their inner dependencies into mockable interfaces.

9. Integration/UI tests
   Some samples of the UI tests for the composables can be found in commonTest -> kotlin -> screen -> ComposeTests.
   Unfortunately, due to a known issue (https://slack-chats.kotlinlang.org/t/18784429/hi-there-i-m-trying-to-run-an-ui-test-in-shared-commontest-a), they can't be run from Android Studio, but they can be run from the terminal:

Android target: ./gradlew :composeApp:connectedAndroidTest
iOS target: ./gradlew :composeApp:iosSimulatorArm64Test
10. Issues
   While experimenting with displaying a number of repositories for each user, I found that a separate API call is required for each user/organization. With a high volume of data, I was exceeding my token limits in minutes. I had to redesign the initial approach so that the number of repositories is displayed only upon clicking the user/organization item.
   When searching the remote database, the final results don't always look relevant (the "login" field doesn't include the search string). I suspect the organization name might include it, but the organization name is not in the JSON schema. When searching locally, the database query looks for names containing the search string, so the results of local searches differ from those of remote Git database searches.