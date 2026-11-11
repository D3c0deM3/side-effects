# Side Effects Lab (Jetpack Compose Side Effects)

This project contains the implementation of Part B hands-on tasks for Jetpack Compose Side Effects (Lecture 13 / Seminar 10).

## Effect APIs Chosen for Each Task & Why

### Task 1: Splash Screen
- **API Chosen**: `LaunchedEffect` & `rememberUpdatedState`
- **Why**: 
  - `LaunchedEffect(true)` runs a coroutine upon entering the composition to wait for a 2-second delay before triggering the timeout callback.
  - `rememberUpdatedState` is used to capture the changing `onTimeout` lambda (which updates with the version counter every 500ms) without restarting the coroutine delay, ensuring the splash screen calls the latest timeout callback rather than a stale captured version.

### Task 2: Snackbar Button
- **API Chosen**: `rememberCoroutineScope`
- **Why**: 
  - Showing a Snackbar is triggered by user interaction (a button click callback `onClick`), not by composition entry. Because `LaunchedEffect` cannot be called from a callback, `rememberCoroutineScope` provides a composition-aware `CoroutineScope` that allows launching coroutines (`scope.launch { snackbarHostState.showSnackbar(...) }`) while automatically canceling any active work when the composable leaves the composition.

### Task 3: Lifecycle Logger
- **API Chosen**: `DisposableEffect`
- **Why**: 
  - Registering a listener (`LifecycleEventObserver`) requires cleanup (removing the observer) when the composable leaves the composition or when the lifecycle owner changes. `DisposableEffect` guarantees that cleanup code inside the required `onDispose` block runs when the effect leaves or restarts, preventing memory leaks.

### Task 4: Analytics
- **API Chosen**: `SideEffect`
- **Why**: 
  - Passing Compose state (`userType`) to a non-Compose object (`AnalyticsTracker`) needs to happen after every successful recomposition commit. `SideEffect` runs after every successful recomposition commit, ensuring that aborted composition attempts (which may be discarded) do not prematurely update analytics.

### Task 5: Scroll (derivedStateOf & snapshotFlow)
- **API Chosen**: `derivedStateOf` & `snapshotFlow`
- **Why**: 
  - `derivedStateOf` is used to observe `listState.firstVisibleItemIndex > 0` so that the "Scroll to Top" button visibility state updates only when the boolean threshold flips (frequency reduction), preventing excessive recompositions on every pixel scrolled.
  - `snapshotFlow` converts Compose State into a cold Flow, enabling powerful Flow operators (`map`, `distinctUntilChanged`, `filter`) to trigger exactly one analytics event when crossing the first item threshold.

### Task 6: Bonus (produceState)
- **API Chosen**: `produceState`
- **Why**: 
  - `produceState` bridges non-Compose asynchronous coroutine sources (like network loading with `FakeRepository.load`) into Compose `State<ResultState>`, managing loading, success, and error states while automatically canceling the producer when the composable leaves composition.
