# Mobile Application Development – Side Effects
## Part A
### Question 1
A side effect is any change to the state of the app, or any interaction with the outside world, that happens beyond the scope of a composable function. Composables should be free of side effects, because they can be re-run unpredictably, and some recomposition attempts get thrown away.
An effect is a composable that emits no UI; it schedules side effects to run once a composition completes successfully. Loading data belongs in the ViewModel, which keeps the unidirectional data flow intact.
### Question 2
|I want to | API |
|--|--|
| call a suspend function while the composable is in the Composition | LaunchedEffect |
| launch a coroutine from a callback such as onClick | rememberCoroutineScope |
| read the latest value inside an effect without restarting it | rememberUpdatedState |
| register a listener and remove it afterwards | DisposableEffect |
| publish Compose state to code Compose does not manage | SideEffect |
| turn Flow, LiveData or RxJava into Compose State | produceState |
| recompose less often than the input changes | derivedStateOf |
| turn Compose State into a Flow | snapshotFlow |
### Question 3
1. The effect starts as soon as it enters the Composition.
2. When a key changes, LaunchedEffect cancels the running coroutine and starts a fresh one using the new key.
3. `isActive` tells us whether the coroutine is still active. Once it turns false, the while loop ends.

The lecture's MiniComposition model illustrates what happens when a key changes.
### Question 4
With `LaunchedEffect(true)` the key never changes, so the effect never restarts and keeps calling sendHello with the original userId, even after the user changes. That is acceptable only when the work should run once for the whole lifetime of the composable and must not be restarted.

    LaunchedEffect(userId) {
        api.sendHello(userId)
    }
### Question 5
LaunchedEffect is itself a composable, so it can only be called from other composable code, and an onClick lambda is not composable. rememberCoroutineScope gives us a coroutine scope that is tied to the composable's lifecycle.

    val scope = rememberCoroutineScope()
    ...
    Button(
        onClick = {
            scope.launch {
                snackbarHostState.showSnackbar("Hi")
            }
        }
    )
For **work that starts because the composable appeared**, I would pick LaunchedEffect; for **work that starts because the user did something**, I would pick rememberCoroutineScope.
### Question 6
Here `currentOnTimeout` is created with `rememberUpdatedState(onTimeout)`, so it always holds the most recent lambda. Since LaunchedEffect uses `true` as its key, the effect is not restarted when a new lambda arrives; the waiting continues, and when it ends the latest lambda is the one that gets called. If we made onTimeout the key instead, every new lambda would cancel the running coroutine and start it over, so the timer would keep resetting. The model's output shows what a plain captured lambda does: it keeps the old value and never sees the update.
### Question 7
DisposableEffect must end with an onDispose block that holds the cleanup code; without it the build fails. If there is nothing to clean up, LaunchedEffect or SideEffect is the better fit. In this example, though, cleanup is needed to remove the LifecycleEventObserver:

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> /*...*/ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
### Question 8

    A.addObserver(observer)
    A.removeObserver(observer)
    B.addObserver(observer)
    B.removeObserver(observer)
The observer is first added to owner A. The second call prints nothing, because the key did not change. When the owner switches to B, the onDispose for A runs first, and then the observer is added to B. Finally, when comp.leave() is called, the onDispose for the observer on owner B runs.
### Question 9
SideEffect runs after every successful recomposition, meaning only once the composition has been committed. A call placed directly in the composable body runs during the composition attempt. If that attempt is later discarded, the analytics call has already been made, so the library ends up with a value from a composition that was never used.

    SideEffect {
        analytics.setUserProperty("userType", user.userType)
    }
In the lecture's attempt model:
- Attempt 1 (direct call, commits=false): analytics is called even though the attempt is discarded. This is wrong.
- Attempt 2 (SideEffect, commits=false): analytics is not called, since the SideEffect stays pending and never commits.
- Attempt 3 (SideEffect, commits=true): analytics is called once, right after the commit.

SideEffect takes no keys. It runs after each successful recomposition, so the value is always current. For that reason it has to be cheap: no heavy work and no suspend calls.
### Question 10
produceState starts a coroutine scoped to the Composition and lets it push values into a State it returns. It is used to turn non-Compose sources (Flow, LiveData, RxJava) into Compose state. The producer starts when produceState enters the Composition and is cancelled when it leaves. If a key (url, imageRepository) changes, the running producer is cancelled and launched again with the new inputs.
The returned State is conflated, so assigning the same value again does not trigger a recomposition. In the lecture's model, `state.value = state.value` does nothing, which is why a.png and b.txt each recompose only once (Loading to Success, or Loading to Error).
loadNetworkImage begins with a lowercase letter because a composable that returns a value is named like an ordinary Kotlin function.

    @Composable
    fun loadNetworkImage(
        url: String,
        imageRepository: ImageRepository = ImageRepository()
    ): State<Result<Image>> {
        return produceState<Result<Image>>(initialValue = Result.Loading, url, imageRepository) {
            val image = imageRepository.load(url)
            value = if (image == null) {
                Result.Error
            } else {
                Result.Success(image)
            }
        }
    }
### Question 11
There is no magic in produceState. Internally it keeps a result variable created with `remember { mutableStateOf(initialValue) }` and runs the producer inside a LaunchedEffect. Each time the producer sets value, the result state is updated.

    @Composable
    fun <T> myProduceState(
        initialValue: T,
        key: Any?,
        producer: suspend MutableState<T>.() -> Unit
    ): State<T> {
        val result = remember { mutableStateOf(initialValue) }
        LaunchedEffect(key) {
            result.producer()
        }
        return result
    }
It behaves like LaunchedEffect because it is built on top of it: the producer starts when it enters the Composition and is cancelled when it leaves. If the key changes, the old coroutine is cancelled and the producer runs again in a new one. The remembered state holds on to the last value in the meantime.
### Question 12
Reading listState.firstVisibleItemIndex directly makes the composable recompose every time the index changes (0, 1, 2, 3...), even though the UI only needs to change once the index goes above 0. This is a mismatch in update frequency. derivedStateOf produces a state that updates only when its result (a Boolean) changes, much like distinctUntilChanged. It is wrapped in remember so that the derived state is not recreated on each recomposition and keeps its value.

    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
Counting for the indexes [0, 0, 1, 2, 3, 4, 5, 3, 1, 0]:
- Direct read: a recomposition happens whenever the index differs from the previous one. 0 (first, prev = -1) counts as 1, the second 0 is unchanged, then 1 (2), 2 (3), 3 (4), 4 (5), 5 (6), 3 (7), 1 (8), 0 (9). That gives 9 recompositions.
- derivedStateOf: show = index > 0 produces false, false, true, true, true, true, true, true, true, false. The value changes at false (first, 1), true (2) and false (3), giving 3 recompositions.
### Question 13
Here fullName has to be updated exactly as often as firstName and lastName, so there is no extra recomposition to avoid, and derivedStateOf is just overhead (it is not cheap). The correct version:

    val fullNameCorrect = "$firstName $lastName"
The rule of thumb: use derivedStateOf only when the inputs change more often than the result and we want to skip pointless recompositions (as with a scroll position and a threshold). If the result changes as often as the inputs, it is not needed.
The lecture also warns that derivedStateOf is costly, so it should be reserved for cases where it prevents recompositions that would produce an unchanged result.
### Question 14
derivedStateOf makes sense when the waveform depends on some other state, but we do not want to recompose each time that base state changes, only when the derived value does.
It is not needed in these cases:
1. The amplitudes are already precomputed (in the ViewModel or repository). There is nothing to derive, so derivedStateOf would only add overhead.
2. The waveform is static and does not depend on rapidly changing state (such as a playback position updated every frame). The input rarely changes, so there are no extra recompositions to cut.
3. The waveform is updated manually via LaunchedEffect or animate* APIs. Those APIs already control the updates, so derivedStateOf adds nothing.
### Question 15
snapshotFlow turns State objects into a cold Flow. It is cold because its block runs only when someone collects it, and it emits whatever the State objects read inside the block evaluate to. When one of those states changes, the Flow emits the new value only if it differs from the last emitted one (similar to distinctUntilChanged).
- map: converts the index into a Boolean (index > 0).
- distinctUntilChanged: drops repeated values, so only changes in the Boolean get through.
- filter: keeps only true, so an event fires only when the user moves past the first item.

collect is called inside LaunchedEffect because it is a suspend function and needs a coroutine. In addition, the collection is cancelled together with the effect when it leaves the Composition (and restarts if listState changes).

For the stream 0, 0, 1, 2, 5, 3, 0, 0, 1, 4:
1. map: false, false, true, true, true, true, false, false, true, true
2. distinctUntilChanged: false, true, false, true
3. filter (true only): true, true

So 2 events are sent, one each time the user scrolls past the first item.
### Question 16
| Keys | Restarts (prediction) | Why |
|--|--|--|
| keys = [] | 1 | The keys never change, so the effect runs only once at the start. Even after owner B arrives, it still uses A. |
| keys = [owner] | 2 | It restarts exactly when the owner changes (A at the start, B at tick 3). |
| keys = [owner, tick] | 4 | tick changes every time (1 to 4), so the effect restarts on every tick. |

keys = [] is wrong: it works with a stale value (restarting less than needed causes bugs). keys = [owner] is correct. keys = [owner, tick] is inefficient (restarting more than needed). The output of the lecture's model:

    keys = []            restarts: 1
    keys = [owner]       restarts: 2
    keys = [owner, tick] restarts: 4
### Question 17
lifecycleOwner has to be a key of DisposableEffect, because when it changes the old observer must be removed and a new one attached to the new owner. currentOnStart and currentOnStop are not keys: they come from rememberUpdatedState, so their State object never changes in the Composition, and they always provide the latest lambda without restarting the effect.
If lifecycleOwner is not a key and it changes, HomeScreen recomposes but the DisposableEffect is neither disposed nor restarted. The observer stays attached to the old owner, and from then on the wrong lifecycleOwner is in use.

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) currentOnStart()
            else if (event == Lifecycle.Event.ON_STOP) currentOnStop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
Key point: any variable used inside an effect should be passed as a parameter of the effect, or be wrapped with rememberUpdatedState.
### Question 18
Bugs:
1. `viewModel.markOpened(userId)` is called straight in the composable body. This is a side effect that runs on every recomposition, including discarded ones. It should live in an effect keyed on userId: LaunchedEffect(userId).
2. `LaunchedEffect(true) { delay(5_000); onClose() }` uses onClose, which can change, so it may call an outdated lambda. Making onClose a key would restart the delay with every new lambda. The right fix is rememberUpdatedState(onClose).
3. DisposableEffect has no onDispose, so it does not compile, and the observer would never be removed (a leak). onDispose must be the last statement and must call removeObserver.
4. LaunchedEffect is called inside onClick. It is a composable and cannot be used from a callback. We should use scope.launch with rememberCoroutineScope (send() is a suspend function).

Corrected version:

    @Composable
    fun ChatScreen(userId: String, onClose: () -> Unit) {
        val scope = rememberCoroutineScope()
        val owner = LocalLifecycleOwner.current
        val observer = remember { LifecycleEventObserver { _, _ -> } }
        val currentOnClose by rememberUpdatedState(onClose)

        LaunchedEffect(userId) {                           // (1)
            viewModel.markOpened(userId)
        }
        LaunchedEffect(true) {                             // (2)
            delay(5_000)
            currentOnClose()
        }
        DisposableEffect(owner) {                          // (3)
            owner.lifecycle.addObserver(observer)
            onDispose {
                owner.lifecycle.removeObserver(observer)
            }
        }
        Button(onClick = {                                 // (4)
            scope.launch { send() }
        }) { Text("Send") }
    }
### Question 19
| API | Runs | Cleanup | Restarts on |
|--|--|--|--|
| LaunchedEffect | enters Composition | coroutine cancelled | keys |
| rememberCoroutineScope | when you call launch | scope cancelled on leave | no keys |
| rememberUpdatedState | not an effect | none | never |
| DisposableEffect | enters Composition | onDispose (required) | keys |
| SideEffect | after every successful recomposition | none | no keys |
| produceState | enters Composition | producer cancelled | keys |
| derivedStateOf | when the read inputs change | none | wrap in remember |
| snapshotFlow | when collected | collector cancelled | no keys |

Real examples from the SideEffectsLab tasks:
- LaunchedEffect: a splash screen that waits 2 seconds and then calls onTimeout.
- rememberCoroutineScope: a button that shows a snackbar via scope.launch { showSnackbar(...) }.
- rememberUpdatedState: currentOnTimeout in the splash screen, so the wait does not restart when the parent passes a new lambda.
- DisposableEffect: a lifecycle logger that adds a LifecycleEventObserver and removes it in onDispose.
- SideEffect: handing the userType state to the Analytics object after each successful recomposition.
- produceState: loadNetworkImage(url) with Loading, Success and Error states.
- derivedStateOf: a "scroll to top" button that appears when firstVisibleItemIndex > 0.
- snapshotFlow: sending a single analytics event when the user scrolls past item 0.
