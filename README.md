# 8202530 Assignment 2

NIT3213 Final Assignment: an Android app (Kotlin, XML, Material 3) with three screens, **Login**, **Dashboard** and **Details**. It talks to the `nit3213api` to log in and load your topic's data.

As an extra feature on top of the assignment requirements, the app also includes a **Permission Manager**, opened from the Dashboard. It shows which apps on the phone can use your location, camera, microphone and other sensitive data, and lets you switch each permission on or off right in the app (through Shizuku, no root needed).

**Repository:** https://github.com/MananSharma001/8202530Assignment2

## Features

| Screen | What it does |
|---|---|
| **Login** | Student ID (username) and first name (password). Checks the form, sends `POST /footscray/auth`, and shows clear errors for wrong details, no internet or a slow server. On success, opens the Dashboard with the `keypass`. |
| **Dashboard** | Calls `GET /dashboard/{keypass}` and shows the entities in a **RecyclerView**. Each card is a summary of one entity (every field except the description). Includes pull to refresh, a retry button, an empty state and Log out. Tap a card to open Details. |
| **Details** | Shows every field of the selected entity, with readable labels, plus the full description. |
| **App permissions** (extra) | Opened from the "App permissions" card on the Dashboard. A summary of how many permissions your apps are allowed, and how many apps can use location, camera and microphone. Then every installed app in a RecyclerView, with search, filter chips, a system apps switch and pull to refresh. Rescans whenever you come back, so changes show up straight away. |
| **App detail** (extra) | Every runtime permission one app asked for, grouped like Android's Settings, each with a switch to allow or deny it in place (Shizuku runs `pm grant` / `pm revoke`). If Shizuku isn't installed, running or allowed yet, a banner shows the next step. Reads again every 2 seconds while open. |

Each topic returns different field names, so the app doesn't hard code them. It reads whatever fields come back and turns names like `artistName` into "Artist Name".

## Build and run

**You need:** a recent Android Studio with JDK 17 or newer (the bundled JDK, up to Java 25, runs the build: Gradle 9.1, AGP 8.13, Kotlin 2.2), and an emulator or phone running Android 8.0 (API 26) or newer, with internet.

1. Clone the repository:
   ```bash
   git clone https://github.com/MananSharma001/8202530Assignment2.git
   ```
2. In Android Studio, choose **File > Open** and select the `8202530Assignment2` folder.
3. Wait for **Gradle sync** to finish. It downloads every dependency automatically.
4. Pick a device and press **Run** (Shift + F10).
5. Log in with:
   * **Student ID:** `8202530`
   * **First name:** `Manan Sanjay` (capital M and S, with the space)

The API runs on a free Render server, so the first request after a while can take up to a minute while it wakes up. The app waits up to 60 seconds before showing a "try again" message.

**Different class location:** in `app/build.gradle.kts`, change `CAMPUS` from `"footscray"` to `"sydney"` or `"br"`.

## Permission Manager setup (optional)

Viewing apps and their permissions works straight away. The app uses the `QUERY_ALL_PACKAGES` permission so it can list every installed app.

To switch permissions on or off, the app needs **Shizuku**:

1. Install Shizuku from https://shizuku.rikka.app/download/
2. On the phone, turn on **Developer options**, then **Wireless debugging**.
3. Open Shizuku and tap **Start** (via Wireless debugging), following its pairing steps.
4. Open this app, go to **App permissions**, and tap **Allow** on the Shizuku banner.

Shizuku needs to be started again after the phone restarts.

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
| `PermissionsViewModelTest` | Loads apps from the mocked repository, hides system apps by default, the stats only count installed apps, and the filter chips, search, system switch and pull to refresh work. |
| `AppPermissionsViewModelTest` | Uses the package name from the Intent, shows that app's permissions, and shows "not found" after an uninstall. Switches grant a denied permission and revoke an allowed one through the mocked controller, report failures, and ask for Shizuku setup when it isn't ready. |
| `PermissionStatsTest` | The summary numbers and percentage, and permissions are grouped like Settings. |

The ViewModel tests follow the pattern from the Testing lecture: the repository is mocked with **MockK** (`mockk()`, `coEvery { } returns`, `coVerify { }`), `Dispatchers.setMain` swaps in a `StandardTestDispatcher` in `@Before` (reset in `@After`), and `advanceUntilIdle()` runs the queued coroutines before each check. Mocking is easy here because ViewModels only depend on repository **interfaces**, which Hilt binds to the real classes.

## Architecture

MVVM with a repository layer and Hilt for dependency injection.

```
Assignment screens
Activity ─► ViewModel ─► AuthRepository / DashboardRepository ─► Retrofit API ─► nit3213api

Permission Manager (extra)
Activity ─► ViewModel ─► PermissionRepository ─► Android PackageManager       (read)
                      ─► PermissionController (ShizukuController) ─► Shizuku ─► pm grant / revoke   (write)
```

Activities observe a `StateFlow` from their ViewModel inside `repeatOnLifecycle(STARTED)`. ViewModels call suspend functions in `viewModelScope`, and network and PackageManager work runs on the IO dispatcher.

### Dependency injection (Hilt)

| Piece | Role |
|---|---|
| `Assignment2App` (`@HiltAndroidApp`) | Starts Hilt and generates the app wide container. |
| `di/NetworkModule` | `@Provides` OkHttp, Retrofit, `Nit3213Api`, the campus name (`@CampusLocation`) and the IO dispatcher (`@IoDispatcher`), all as `@Singleton` where shared. |
| `di/RepositoryModule` | `@Binds` each interface to its implementation: `AuthRepository` → `AuthRepositoryImpl`, `DashboardRepository` → `DashboardRepositoryImpl`, `PermissionRepository` → `PermissionRepositoryImpl` (reads PackageManager), and `PermissionController` → `ShizukuController`. |
| Repositories | Get their dependencies through `@Inject constructor`. |
| `LoginViewModel`, `DashboardViewModel`, `PermissionsViewModel`, `AppPermissionsViewModel` | `@HiltViewModel` with `@Inject constructor`. The Dashboard gets the keypass, and App detail the package name, through `SavedStateHandle`. |
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
│   │   ├── EntityMapper.kt           Raw JSON to Entity, readable labels
│   │   └── PermissionModels.kt       Permission groups, apps, stats
│   ├── remote/
│   │   ├── Nit3213Api.kt             Retrofit endpoints
│   │   └── dto/ApiDtos.kt            Request and response classes
│   ├── repository/
│   │   ├── AuthRepository.kt         Interface + implementation
│   │   ├── DashboardRepository.kt    Interface + implementation
│   │   └── PermissionRepository.kt   Interface + PackageManager implementation
│   └── shizuku/
│       ├── ShizukuController.kt      PermissionController: grant / revoke through Shizuku
│       └── UserService.kt            Runs `pm` with ADB rights in Shizuku's process
├── di/
│   ├── NetworkModule.kt
│   ├── RepositoryModule.kt
│   └── Qualifiers.kt
└── ui/
    ├── common/                       Error messages, edge to edge insets, app icon loader
    ├── login/                        LoginActivity, LoginViewModel, LoginValidator
    ├── dashboard/                    DashboardActivity, DashboardViewModel, EntityAdapter
    ├── details/                      DetailsActivity
    └── permissions/                  PermissionsActivity, AppPermissionsActivity, their ViewModels and adapters
```

## Libraries

| Library | Used for |
|---|---|
| Hilt 2.57 (KSP) | Dependency injection |
| Retrofit 2.11 + Gson | HTTP calls and JSON |
| OkHttp logging interceptor | Shows requests in Logcat (debug builds only) |
| Kotlin Coroutines + StateFlow | Background work and UI state |
| AndroidX Lifecycle ViewModel | Screen state that survives rotation |
| RecyclerView, SwipeRefreshLayout | Dashboard and app lists |
| Material Components 1.12 | Material 3 UI |
| Kotlin Parcelize | Passing an entity from the Dashboard to Details |
| Shizuku API + provider | Granting and revoking permissions with ADB rights, without root |
| JUnit 4, MockK, Coroutines Test, MockWebServer | Unit tests |

## Git

The project history is split into clear steps: project setup, API layer and Hilt modules, Login, Dashboard and Details, unit tests, README, then the Permission Manager screens and their tests.
