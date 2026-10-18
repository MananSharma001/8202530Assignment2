# 8202530Assignment2

NIT3213 Final Assignment: an Android app (Kotlin, XML, Material 3) with three screens: **Login**, **Dashboard** and **Details**. It talks to the `nit3213api` to log in and load your topic's data.

## Features

| Screen | What it does |
|---|---|
| **Login** | Student ID (username) and first name (password). Checks the form, sends `POST /footscray/auth`, and shows clear errors for wrong details, no internet or a slow server. On success, opens the Dashboard with the `keypass`. |
| **Dashboard** | Calls `GET /dashboard/{keypass}` and shows the entities in a **RecyclerView**. Each card is a summary of one entity (every field except the description). Includes pull to refresh, a retry button, an empty state and Log out. Tap a card to open Details. |
| **Details** | Shows every field of the selected entity, with readable labels, plus the full description. |

Each topic returns different field names, so the app doesn't hard code them. It reads whatever fields come back and turns names like `artistName` into "Artist Name".

## Build and run

**You need:** a recent Android Studio with JDK 17 or newer (the bundled JDK, up to Java 25, runs the build: Gradle 9.1, AGP 8.13, Kotlin 2.2), and an emulator or phone running Android 8.0 (API 26) or newer, with internet.

1. Clone the repository:
   ```bash
   git clone <your repository link>
   ```
2. In Android Studio, choose **File > Open** and select the `8202530Assignment2` folder.
3. Wait for **Gradle sync** to finish. It downloads every dependency automatically.
4. Pick a device and press **Run** (Shift + F10).
5. Log in with:
   * **Student ID:** your ID without the "s", e.g. `8202530`
   * **First name:** exactly as registered, capitals included, e.g. `Manan`

The API runs on a free Render server, so the first request after a while can take up to a minute while it wakes up. The app waits up to 60 seconds before showing a "try again" message.

**Different class location:** in `app/build.gradle.kts`, change `CAMPUS` from `"footscray"` to `"sydney"` or `"br"`.

## Run the unit tests

* In Android Studio: right click `app/src/test` and choose **Run Tests**, or
* From a terminal: `./gradlew test` (Windows: `gradlew test`)

| Test class | What it checks |
|---|---|
| `LoginViewModelTest` | A successful login sends the keypass. Wrong details and no internet show the right message. Empty or "s" prefixed IDs are rejected without calling the API. |
| `DashboardViewModelTest` | Uses the keypass from Login, shows entities on success, shows an error on failure, and the retry works. |
| `RepositoryTest` | Real Retrofit against a fake server (MockWebServer): the correct URL and request body, keypass parsing, entity mapping, and HTTP error mapping. |
| `EntityMapperTest` | The description is separated out, field order is kept, labels are readable, and lists and nulls are handled. |
| `LoginValidatorTest` | The form rules. |

The ViewModel tests follow the pattern from the Testing lecture: the repository is mocked with **MockK** (`mockk()`, `coEvery { } returns`, `coVerify { }`), `Dispatchers.setMain` swaps in a `StandardTestDispatcher` in `@Before` (reset in `@After`), and `advanceUntilIdle()` runs the queued coroutines before each check. Mocking is easy here because ViewModels only depend on repository **interfaces**, which Hilt binds to the real classes.

## Architecture

MVVM with a repository layer and Hilt for dependency injection.

```
Activity (UI)  ─►  ViewModel (@HiltViewModel)  ─►  Repository interface  ─►  Retrofit API  ─►  nit3213api
   observes StateFlow        calls suspend fun       (bound by Hilt)
```

### Dependency injection (Hilt)

| Piece | Role |
|---|---|
| `Assignment2App` (`@HiltAndroidApp`) | Starts Hilt and generates the app wide container. |
| `di/NetworkModule` | `@Provides` OkHttp, Retrofit, `Nit3213Api`, the campus name (`@CampusLocation`) and the IO dispatcher (`@IoDispatcher`), all as `@Singleton` where shared. |
| `di/RepositoryModule` | `@Binds` `AuthRepository` to `AuthRepositoryImpl` and `DashboardRepository` to `DashboardRepositoryImpl`. |
| Repositories | Get their dependencies through `@Inject constructor`. |
| `LoginViewModel`, `DashboardViewModel` | `@HiltViewModel` with `@Inject constructor`. The Dashboard gets the keypass through `SavedStateHandle`. |
| Activities | `@AndroidEntryPoint`, so `by viewModels()` returns Hilt built ViewModels. |

No class creates its own dependencies, so each one can be tested on its own.

### Project structure

```
app/src/main/java/com/s8202530/assignment2/
├── Assignment2App.kt                 Hilt application
├── data/
│   ├── ApiResult.kt                  Success / Error result and error mapping
│   ├── model/
│   │   ├── Entity.kt                 Entity, EntityField (Parcelable), DashboardData
│   │   └── EntityMapper.kt           Raw JSON to Entity, readable labels
│   ├── remote/
│   │   ├── Nit3213Api.kt             Retrofit endpoints
│   │   └── dto/ApiDtos.kt            Request and response classes
│   └── repository/
│       ├── AuthRepository.kt         Interface + implementation
│       └── DashboardRepository.kt    Interface + implementation
├── di/
│   ├── NetworkModule.kt
│   ├── RepositoryModule.kt
│   └── Qualifiers.kt
└── ui/
    ├── common/                       Error messages, edge to edge insets
    ├── login/                        LoginActivity, LoginViewModel, LoginValidator
    ├── dashboard/                    DashboardActivity, DashboardViewModel, EntityAdapter
    └── details/                      DetailsActivity
```

## Libraries

| Library | Used for |
|---|---|
| Hilt 2.57 (KSP) | Dependency injection |
| Retrofit 2.11 + Gson | HTTP calls and JSON |
| OkHttp logging interceptor | Shows requests in Logcat (debug builds only) |
| Kotlin Coroutines + StateFlow | Background work and UI state |
| AndroidX Lifecycle ViewModel | Screen state that survives rotation |
| RecyclerView, SwipeRefreshLayout | Dashboard list |
| Material Components 1.12 | Material 3 UI |
| Kotlin Parcelize | Passing an entity from the Dashboard to Details |
| JUnit 4, MockK, Coroutines Test, MockWebServer | Unit tests |

## Git

The project history is split into steps (setup, API layer, Login, Dashboard and Details, tests, docs). To publish it:

```bash
# create an empty repository on GitHub first, then:
git remote add origin https://github.com/<your-username>/8202530Assignment2.git
git push -u origin main
```
