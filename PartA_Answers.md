# Part A. Theory Questions & Answers

All answers are based on Lecture 13 slides on Jetpack Compose Side Effects.

---

### Question 1
* **Context**: Calling `viewModel.loadProfile()` and `analytics.log("opened")` at the top of the `ProfileScreen` composable body. When an animation runs, requests and log entries multiply.
* **Answer**:
  - **Side-effect definition**: Any change to the state of the app or interaction with the outside world (network, database, analytics, logging) that happens outside the scope of a composable function's UI emission.
  - **Why composable body is dangerous**: Composable functions can run many times, in any order, concurrently, or be discarded and restarted during recomposition attempts. Placing side-effects in the body causes them to execute repeatedly and unpredictably, even for discarded composition attempts.
  - **Lecture Key Term ("Effect")**: An effect is a composable function that doesn't emit UI and causes side effects to run when a composition completes successfully.
  - **Data loading in ViewModel**: Data loading belongs in the ViewModel to maintain Unidirectional Data Flow (UDF). The UI delegates events to the ViewModel, and the ViewModel manages business logic and state, keeping composables side-effect free and responsive.

---

### Question 2
* **Context**: Matching the 8 Compose effect APIs to needs.
* **Answer**:
  1. Call a suspend function while the composable is in the Composition -> `LaunchedEffect`
  2. Launch a coroutine from a callback such as `onClick` -> `rememberCoroutineScope`
  3. Read the latest value inside an effect without restarting it -> `rememberUpdatedState`
  4. Register a listener and remove it afterwards -> `DisposableEffect`
  5. Publish Compose state to code Compose does not manage -> `SideEffect`
  6. Turn Flow, LiveData or RxJava into Compose State -> `produceState`
  7. Recompose less often than the input changes -> `derivedStateOf`
  8. Turn Compose State into a Flow -> `snapshotFlow`

---

### Question 3
* **Context**: Pulse animation with `LaunchedEffect(pulseRateMs)`.
* **Answer**:
  1. **When it starts**: The effect starts immediately when `LaunchedEffect` enters the Composition.
  2. **When pulseRateMs changes from 3000 to 1000**: The existing coroutine is cancelled, and a new coroutine is launched with the new rate (1000ms delay).
  3. **Leaving Composition & isActive**: When the composable leaves the composition, the coroutine is automatically cancelled. `isActive` checks whether the coroutine scope is still active to terminate the while loop cleanly. The `MiniComposition` model (`effect(100) cancelled`) demonstrates that changing keys or leaving cancels the old job.

---

### Question 4
* **Context**: `LaunchedEffect(true) { api.sendHello(userId) }` where `userId` can change.
* **Answer**:
  - **Why suspicious**: `LaunchedEffect(true)` never restarts when `userId` changes. If `userId` changes while the composable remains in composition, the effect continues using the old (stale) `userId`, just like an infinite `while(true)` loop ignoring external changes.
  - **Appropriate use case**: A constant key (`true`) is appropriate when work should run exactly once upon entering composition and never restart based on keys—such as a splash screen timeout delay.
  - **Fixed code**: `LaunchedEffect(userId) { api.sendHello(userId) }`.

---

### Question 5
* **Context**: Calling `LaunchedEffect(Unit)` inside an `onClick` callback.
* **Answer**:
  - **Why it doesn't compile**: `LaunchedEffect` is a composable function and can only be called directly from other composable functions in the Composition tree, not from imperative event callbacks like `onClick`.
  - **rememberCoroutineScope lifetime**: Returns a `CoroutineScope` bound to the composition point where it's called. When the composable leaves the composition, the scope is automatically cancelled.
  - **Why showSnackbar is a suspend function**: Because UI feedback like showing a snackbar is asynchronous and takes time, suspending allows non-blocking execution.
  - **API choice**: Use `LaunchedEffect` for work starting because the composable appeared; use `rememberCoroutineScope` for work starting because the user did something (callback).

---

### Question 6
* **Context**: `LandingScreen(onTimeout)` with `rememberUpdatedState`.
* **Answer**:
  - **Stale lambda**: Without `rememberUpdatedState`, `LaunchedEffect(true)` captures the `onTimeout` lambda passed during initial composition. If the parent recomposes and passes a new lambda version, the delayed effect would execute the old captured lambda.
  - **rememberUpdatedState solution**: Stores the latest lambda reference so that when the delay finishes, it invokes the most up-to-date lambda without restarting the timer.
  - **Why making onTimeout key is wrong**: If `onTimeout` lambda instance changes on every parent recomposition, making it a key would cause `LaunchedEffect` to restart its timer from zero every time, preventing the 2-second timeout from ever completing.

---

### Question 7
* **Context**: `DisposableEffect(lifecycleOwner)` missing `onDispose`.
* **Answer**:
  - **Why cleanup & leak**: If event observers or listeners are registered without removal, when the screen or component is destroyed/recreated, lingering references cause memory leaks and duplicate event notifications.
  - **onDispose rule**: Must appear as the final statement inside `DisposableEffect`. If omitted, code will not compile.
  - **Empty onDispose**: Bad practice because it fails to clean up resources, defeating the purpose of `DisposableEffect`.
  - **Fixed code**:
    ```kotlin
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> /* ... */ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    ```

---

### Question 8
* **Context**: `MiniDisposable` simulation model.
* **Answer**:
  - **Console output**:
    1. A.addObserver(observer)
    2. (Second screen("A") prints nothing because owner didn't change).
    3. A.removeObserver(observer) followed by B.addObserver(observer) (when B arrives).
    4. B.removeObserver(observer) (when comp.leave() is called).
  - **Order when B arrives**: The old effect is cleaned up first (`removeObserver(observer)` for A), then the new effect is set up (`addObserver(observer)` for B).

---

### Question 9
* **Context**: Setting analytics `userType` in the composable body vs `SideEffect`.
* **Answer**:
  - **When SideEffect runs**: After every successful recomposition commit.
  - **Direct call danger**: Touches the library even during composition attempts that are discarded (`commits=false`), leading to false tracking.
  - **Attempt model**: Direct calls execute on every attempt (even discarded ones). `SideEffect` only executes when `commits=true`.
  - **Does SideEffect take keys**: No, `SideEffect` does not take keys because it runs after every successful commit.

---

### Question 10
* **Context**: `loadNetworkImage(url)` with `produceState`.
* **Answer**:
  - **What produceState does**: Converts non-Compose asynchronous data sources (suspend functions, flows) into Compose `State`. Started on enter, cancelled on leave or when keys change.
  - **Conflation**: Setting the same state value multiple times does not trigger unnecessary recompositions.
  - **Lowercase naming rule**: In Compose, functions that return a remembered state object (like `produceState` or custom state producers) start with a lowercase letter by convention.

---

### Question 11
* **Context**: Implementing `myProduceState` yourself.
* **Answer**:
  - **Implementation**:
    ```kotlin
    @Composable
    fun <T> myProduceState(
        initialValue: T,
        key: Any?,
        producer: suspend MutableState<T>.() -> Unit
    ): State<T> {
        val state = remember { mutableStateOf(initialValue) }
        LaunchedEffect(key) {
            state.producer()
        }
        return state
    }
    ```
  - **Behavior**: Behaves like `LaunchedEffect` because it uses `LaunchedEffect` under the hood to launch the coroutine block, cancelling and restarting when the key changes.

---

### Question 12
* **Context**: Scroll-to-top button with `derivedStateOf`.
* **Answer**:
  - **Why direct read causes recomposition**: `firstVisibleItemIndex` changes on every pixel/item scroll (0, 1, 2, 3...), causing the parent composable to recompose on every index change even though the button visibility only changes at `index > 0`.
  - **derivedStateOf role**: Filters out high-frequency input changes, emitting a state change only when the derived result (Boolean) changes.
  - **Why wrapped in remember**: To retain the derived state object across recompositions so its calculation cache isn't recreated.

---

### Question 13
* **Context**: Combining `firstName` and `lastName` with `derivedStateOf`.
* **Answer**:
  - **Why pure overhead**: `firstName` and `lastName` change at the exact same frequency as the combined `fullName` string. There is no frequency mismatch, so `derivedStateOf` adds unnecessary overhead.
  - **Correct version**: `val fullName = "$firstName $lastName"`
  - **Criterion**: Use `derivedStateOf` only when inputs change much more frequently than the output state you care about.

---

### Question 14
* **Context**: Voice-message waveform and `derivedStateOf` usage.
* **Answer**:
  - **When to use**: When the waveform state depends on a high-frequency base state (like playback position every frame) but you only want updates when a threshold or derived value changes.
  - **Three cases NOT to use**:
    1. Amplitudes are precomputed in ViewModel/repository (no high-frequency source).
    2. Waveform is static.
    3. Waveform is updated manually with `LaunchedEffect` or `animate*` APIs.

---

### Question 15
* **Context**: `snapshotFlow` with list scroll analytics.
* **Answer**:
  - **snapshotFlow**: Converts Compose State reads into a cold Flow that emits whenever read states mutate.
  - **Operators**: `map` transforms index to Boolean; `distinctUntilChanged` drops consecutive duplicates; `filter` keeps only `true` (crosses threshold).
  - **collect in LaunchedEffect**: Ties the flow collection lifecycle to the composable's composition lifecycle.
  - **Event count**: For input `0, 0, 1, 2, 5, 3, 0, 0, 1, 4`, events are sent 2 times (crossing 0 -> >0 twice).

---

### Question 16
* **Context**: Effect model keys (`keys = []`, `keys = [owner]`, `keys = [owner, tick]`).
* **Answer**:
  - `keys = []`: Restarts 1 time. Wrong (uses stale owner value after owner changes to "B").
  - `keys = [owner]`: Restarts 2 times. Right (restarts exactly when `owner` changes).
  - `keys = [owner, tick]`: Restarts 4 times. Inefficient (restarts on every tick even when owner didn't change).

---

### Question 17
* **Context**: `HomeScreen` with `lifecycleOwner`, `onStart`, `onStop`.
* **Answer**:
  - **Keys**: `lifecycleOwner` must be a key because it can change and the effect must follow it.
  - **Not keys**: `currentOnStart` and `currentOnStop` are not keys because `rememberUpdatedState` ensures their reference never changes in composition.
  - **Problem if lifecycleOwner is not a key**: If `lifecycleOwner` changes and is not a key, the effect is not restarted, leaving the observer attached to the old lifecycle owner (bug).
  - **Key Point rule**: Variables used in an effect should be added as a parameter of the effect composable, or use `rememberUpdatedState`.

---

### Question 18
* **Context**: Code review of `ChatScreen`.
* **Answer**:
  - **Bug 1**: `viewModel.markOpened(userId)` directly in composable body -> side-effect during composition; should be in `LaunchedEffect(userId)`.
  - **Bug 2**: `LaunchedEffect(true)` used for an effect relying on `onClose` -> should use `LaunchedEffect(onClose)` or `rememberUpdatedState`.
  - **Bug 3**: `DisposableEffect(owner)` missing `onDispose` block -> compiler error.
  - **Bug 4**: `LaunchedEffect(Unit)` called inside `onClick` callback -> illegal; should use `rememberCoroutineScope` and `scope.launch`.

---

### Question 19
* **Context**: Summary table of the 8 APIs.
* **Answer**:
  - `LaunchedEffect`: Enters Composition | Coroutine cancelled | Keys
  - `rememberCoroutineScope`: When you call launch | Scope cancelled on leave | No keys
  - `rememberUpdatedState`: Not an effect | None | Never
  - `DisposableEffect`: Enters Composition | onDispose (required) | Keys
  - `SideEffect`: After every successful recomposition | None | No keys
  - `produceState`: Enters Composition | Producer cancelled | Keys
  - `derivedStateOf`: When read inputs change | None | Wrap in remember
  - `snapshotFlow`: When collected | Collector cancelled | No keys
