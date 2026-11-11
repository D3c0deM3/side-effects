package com.example.sideeffects.ui.tasks

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/*
 * TASK 5: Scroll, derivedStateOf & snapshotFlow
 *
 * REQUIREMENTS:
 * 1. LazyColumn with 100 items.
 * 2. "Scroll to top" button visible when firstVisibleItemIndex > 0 using `derivedStateOf`.
 * 3. Direct read variant for comparison.
 * 4. Recomposition counter for both variants using `SideEffect`.
 * 5. `snapshotFlow` + `map` + `distinctUntilChanged` + `filter` pipeline to send exactly one analytics event
 *    when passing item 0.
 *
 * WHY derivedStateOf REDUCES RECOMPOSITIONS:
 * LazyListState.firstVisibleItemIndex changes on every single item/scroll position change.
 * - In `DirectReadCounter`, reading `listState.firstVisibleItemIndex` directly causes the composable scope
 *   to recompose every time the index changes (e.g., 0 -> 1 -> 2 -> 3 -> 4).
 * - In `DerivedStateCounter`, reading `listState.firstVisibleItemIndex` inside `derivedStateOf { listState.firstVisibleItemIndex > 0 }`
 *   calculates a boolean result. As long as index stays > 0 (e.g. 1, 2, 3, 4), the boolean result remains `true`.
 *   Compose detects that the derived result has not changed and skips recomposing `DerivedStateCounter` completely!
 *
 * SNAPSHOTFLOW EXPLANATION:
 * `snapshotFlow { listState.firstVisibleItemIndex }` converts Compose State into a Kotlin Coroutines Flow.
 * .map { index -> index > 0 } transforms index to boolean.
 * .distinctUntilChanged() ignores consecutive duplicate boolean values (e.g., true, true, true).
 * .filter { it } filters out false, emitting ONLY when scrolling past item 0.
 */

@Composable
fun DirectReadCounter(listState: LazyListState) {
    var recomposeCount by remember { mutableIntStateOf(0) }

    SideEffect {
        recomposeCount++
        Log.d("Task5Scroll", "DirectReadCounter recomposed: $recomposeCount")
    }

    // Direct read: causes recomposition whenever firstVisibleItemIndex changes
    val showButtonDirect = listState.firstVisibleItemIndex > 0

    Column {
        Text("Variant 1 (Direct Read):", style = MaterialTheme.typography.titleSmall)
        Text("Recompositions: $recomposeCount (showButton = $showButtonDirect)", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun DerivedStateCounter(listState: LazyListState) {
    var recomposeCount by remember { mutableIntStateOf(0) }

    SideEffect {
        recomposeCount++
        Log.d("Task5Scroll", "DerivedStateCounter recomposed: $recomposeCount")
    }

    // derivedStateOf: only notifies observers when condition (index > 0) changes true <-> false
    val showButtonDerived by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    Column {
        Text("Variant 2 (derivedStateOf):", style = MaterialTheme.typography.titleSmall)
        Text("Recompositions: $recomposeCount (showButton = $showButtonDerived)", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Task5ScrollContent() {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var analyticsEventCount by remember { mutableIntStateOf(0) }
    var lastAnalyticsLog by remember { mutableStateOf("No analytics event yet.") }

    // SnapshotFlow pipeline: map -> distinctUntilChanged -> filter
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index -> index > 0 }
            .distinctUntilChanged()
            .filter { it } // Emits only when threshold is crossed from false -> true
            .collect {
                analyticsEventCount++
                lastAnalyticsLog = "Analytics Event #$analyticsEventCount: Scrolled past item 0!"
                Log.d("Task5SnapshotFlow", lastAnalyticsLog)
            }
    }

    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Task 5: derivedStateOf & snapshotFlow", style = MaterialTheme.typography.titleLarge)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DirectReadCounter(listState = listState)
                    HorizontalDivider()
                    DerivedStateCounter(listState = listState)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("snapshotFlow Analytics Tracker:", style = MaterialTheme.typography.titleSmall)
                    Text("Total Analytics Events logged: $analyticsEventCount", style = MaterialTheme.typography.bodyMedium)
                    Text("Last Log: $lastAnalyticsLog", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(100) { index ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "LazyColumn Item #$index",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        // Floating action button for "Scroll to Top"
        AnimatedVisibility(
            visible = showButton,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            FloatingActionButton(onClick = {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            }) {
                Text("Top")
            }
        }
    }
}
