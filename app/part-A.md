# Mobile application development - Side-effects
## Part A
### Question 1
A side-effect is a change in the app state that happens outside of the composable function scope. Composable functions should be side-effect free because of unpredictable recompositions or recompositions that can be discarded.
An effect is a composable function that doesn't emit UI and causes side effects to run when a composition completes. Data loading belongs to ViewModel to keep unidirectional data flow intact.
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
(1) Effect starts when enters composition.
(2) When key changes LaunchedEffect will stop previous coroutine and start new one with new keys.
(3) isActive check is coroutine active or not, if it is not while loop stops.
In lecture's MiniComposition model shows what would be when key changes.
### Question 4
LaunchedEffect(true) means its key never changes and it will always sendHello with the same userId, even when user changes. It is okay, if we want to have an effect that shouldn't be changed across the lifecycle of the composable.

    LaunchedEffect(userId) {
		api.sendHello(userId)
	}
### Question 5
As LaunchedEffect is composable it should be called inside of composable and lambda is not composable. rememberCoroutineScope creates coroutine and it will be tied to composable's lifecycle

    val scope = rememberCoroutineScope()
    ...
    Button(
	    onclick = {
		    scope.launch {
			    snackbarHostState.showSnackbar("Hi")
		    }
	    }
	)
For the **“work that starts because the composable appeared”** I would choose LaunchedEffect and for **“work that starts because the user did something”** I would choose rememberCoroutineScope.
### Question 6
As in this code currentOnTimeout is rememberUpdatedState(onTimeout) it will save the state of the captured lambda and as LaunchedEffect is used with true as a key when new state of onTimeout appears previous state will continue till it finishes. If we gave onTimeout as a key to LaunchedEffect it would destroy previous coroutine and start new one each time. Model output shows how captured works and proves that captured state won't change.
### Question 7
DisposableEffect should have onDispose at the end, where we write cleanup code, otherwise IDE will display build-time error. If we don't have anything to clean up LaunchedEffect or SideEffect-s are better choice. But in code example for this task we need cleanup to remove LifeCycleEventObserver:

    DisposableEffect(lifecycleOwner) {
	    val observer = LifecycleEventObserver{_, event -> /*...*/}
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
First it adds observer with owner A, then in second call it doesn't run anything because key is not changed. When owner changed to B, first onDispose of observer with owner A runs then B is added. At the end when comp.leave() runs onDispose of observer with owner A runs.
### Question 9
SideEffect runs after every successful recomposition, so only after the composition is committed. If we write the call directly in the composable body, it runs during the composition attempt. The attempt can be discarded, but the call to the analytics library has already happened, so the library gets a value from a composition that never was used.

    SideEffect {
	    analytics.setUserProperty("userType", user.userType)
	}
In the lecture's attempt model:
- Attempt 1 (direct call, commits=false) - analytics is called, even if the attempt is discarded. This is wrong.
- Attempt 2 (SideEffect, commits=false) - analytics is not called, because SideEffect is only pending and never commits.
- Attempt 3 (SideEffect, commits=true) - analytics is called once, after the commit.

SideEffect does not take keys. It runs after every successful recomposition, so the value is always up to date. Because of this it must be cheap, no heavy work and no suspend calls.
### Question 10
produceState launches a coroutine scoped to the Composition that can push values into a returned State. It is used to convert non-Compose state (Flow, LiveData, RxJava) to Compose state. Producer is launched when produceState enters the Composition and it is cancelled when it leaves the Composition. If keys (url, imageRepository) change, the running producer is cancelled and launched again with new inputs.
The returned State conflates, it means setting the same value doesn't trigger recomposition. In the lecture's model `state.value = state.value` changes nothing, so a.png and b.txt both have only 1 recomposition (Loading to Success or Loading to Error).
loadNetworkImage starts with a lowercase letter because composables that return a value should be named like normal Kotlin functions.

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
produceState is not magic, under the hood it holds a result variable with remember { mutableStateOf(initialValue) } and runs the producer in a LaunchedEffect. Whenever value is updated in the producer, the result state is updated.

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
It behaves like LaunchedEffect because it is built on top of it: the producer starts when it enters the Composition and is cancelled when it leaves. When the key changes, the old coroutine is cancelled and the producer is launched again in a new coroutine. The remembered state keeps the last value.
### Question 12
If we read listState.firstVisibleItemIndex directly, the composable recomposes every time the index changes (0, 1, 2, 3...), but we need the UI to change only when the index is greater than 0. This is the mismatch of update frequency. derivedStateOf creates a new state that only updates when the result (Boolean) changes, it works like distinctUntilChanged. It is wrapped in remember so the derived state is not created again on every recomposition and it keeps its state.

    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
Calculation for indexes [0, 0, 1, 2, 3, 4, 5, 3, 1, 0]:
- Direct read: it recomposes when index is different from previous one. 0 (first, prev = -1) 1, 0 same, 1 (2), 2 (3), 3 (4), 4 (5), 5 (6), 3 (7), 1 (8), 0 (9). So 9 recompositions.
- derivedStateOf: show = index > 0 gives false, false, true, true, true, true, true, true, true, false. It changes: false (first, 1), true (2), false (3). So 3 recompositions.
### Question 13
In this code fullName needs to update as often as firstName and lastName, so no excess recomposition happens and derivedStateOf is pure overhead (it is expensive). Correct version:

    val fullNameCorrect = "$firstName $lastName"
The criterion: use derivedStateOf only when the inputs change more often than the result and we want to avoid unnecessary recomposition (like scroll position and threshold). If the result changes as often as the inputs, we don't need it.
Caution from the lecture: derivedStateOf is expensive, so use it only to avoid unnecessary recomposition when the result hasn't changed.
### Question 14
We use derivedStateOf when the waveform depends on another state, but we don't want recompositions every time that base state changes, only when the derived value changes.
We don't use it in these cases:
1. Amplitudes are already precomputed (from ViewModel or repository) - there is nothing to derive, so derivedStateOf only adds overhead.
2. The waveform is static and doesn't depend on fast-updating states (like playback position every frame) - the input doesn't change often, so there is no extra recomposition to reduce.
3. The waveform is updated manually with LaunchedEffect or animate* APIs - the updates are already controlled by those APIs, derivedStateOf is not needed.
### Question 15
snapshotFlow converts State objects into a cold Flow. It is cold because its block runs only when it is collected, and it emits the result of State objects read inside the block. When one of the states changes, the Flow emits the new value only if it is not equal to the previous emitted value (like distinctUntilChanged).
- map - turns the index into Boolean (index > 0).
- distinctUntilChanged - drops repeated values, passes only when the Boolean changes.
- filter - keeps only true, so only passing the first item sends an event.

collect is inside LaunchedEffect because collect is a suspend function and it needs a coroutine. Also the collecting is cancelled together with the effect when it leaves the Composition (and restarts if listState changes).

Stream 0, 0, 1, 2, 5, 3, 0, 0, 1, 4:
1. map: false, false, true, true, true, true, false, false, true, true
2. distinctUntilChanged: false, true, false, true
3. filter (true only): true, true

So 2 events are sent, one each time the user crosses the first item downwards.
### Question 16
| Keys | Restarts (prediction) | Why |
|--|--|--|
| keys = [] | 1 | Keys never change, so it runs only once at start. After owner B arrives it still uses A. |
| keys = [owner] | 2 | Restarts exactly when owner changes (A at start, B at tick 3). |
| keys = [owner, tick] | 4 | tick changes every time (1 to 4), so it restarts on every tick. |

keys = [] is wrong, it works with a stale value (restarting less than it should causes bugs). keys = [owner] is right. keys = [owner, tick] is inefficient (restarting more than it should). Output of the lecture's model:

    keys = []            restarts: 1
    keys = [owner]       restarts: 2
    keys = [owner, tick] restarts: 4
### Question 17
lifecycleOwner must be a key of DisposableEffect, because when it changes the old observer must be removed and a new one added to the new owner. currentOnStart and currentOnStop are not keys, because they are made with rememberUpdatedState and their State never changes in Composition, they always give the latest lambda without restarting the effect.
If lifecycleOwner is not a key and it changes, HomeScreen recomposes, but DisposableEffect isn't disposed and restarted. The observer stays on the old owner and the wrong lifecycleOwner is used from that point onward.

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
Key Point: variables used in an effect should be added as a parameter of the effect composable, or use rememberUpdatedState.
### Question 18
Bugs:
1. `viewModel.markOpened(userId)` is called directly in the composable body. It is a side-effect, it runs on every recomposition, even discarded ones. It should be inside an effect with userId as a key: LaunchedEffect(userId).
2. `LaunchedEffect(true) { delay(5_000); onClose() }` uses onClose which can change, so it can call a stale lambda. If we make onClose a key, the delay starts over on every new lambda. We should use rememberUpdatedState(onClose).
3. DisposableEffect has no onDispose, so it doesn't compile and the observer is never removed (leak). onDispose must be the last statement with removeObserver.
4. LaunchedEffect is called inside onClick. It is a composable function and cannot be called from callback. We should use scope.launch with rememberCoroutineScope (and send() is suspend).

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

Real usage examples (from the SideEffectsLab tasks):
- LaunchedEffect - splash screen that waits 2 seconds and then calls onTimeout.
- rememberCoroutineScope - button that shows a snackbar with scope.launch { showSnackbar(...) }.
- rememberUpdatedState - currentOnTimeout in the splash screen, so the wait does not restart when the parent gives a new lambda.
- DisposableEffect - lifecycle logger that adds LifecycleEventObserver and removes it in onDispose.
- SideEffect - passing userType state to the Analytics object after every successful recomposition.
- produceState - loadNetworkImage(url) with Loading, Success and Error states.
- derivedStateOf - "scroll to top" button that appears when firstVisibleItemIndex > 0.
- snapshotFlow - sending one analytics event when the user scrolls past item 0.
