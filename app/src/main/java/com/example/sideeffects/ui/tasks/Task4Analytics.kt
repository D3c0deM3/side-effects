package com.example.sideeffects.ui.tasks

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/*
 * TASK 4: SideEffect & Analytics
 *
 * REQUIREMENTS:
 * - Simple Analytics object with `var userType: String`.
 * - `SideEffect { Analytics.userType = userType }` to safely publish Compose state to external objects.
 * - Counter inside SideEffect logged to Logcat.
 *
 * WHY SideEffect RUNS AFTER RECOMPOSITION EVEN IF STATE DID NOT CHANGE:
 * `SideEffect` does not take any key parameters. Its explicit purpose is to run after EVERY successful commit
 * of the Composition to the UI tree. Whenever a recomposition occurs (for example, triggered by a parent or another
 * state change such as an internal trigger/counter), `SideEffect` will execute regardless of whether `userType`
 * itself changed.
 *
 * LECTURE ATTEMPT MODEL EXPLANATION:
 * In Compose's attempt model, composition attempts can be speculative or discarded prior to committing to the UI tree;
 * `SideEffect` guarantees that state updates to non-Compose objects only happen after a composition attempt successfully commits.
 */

object Analytics {
    var userType: String = "Guest"
        set(value) {
            field = value
            Log.d("Analytics", "Analytics.userType updated to: $value")
        }
}

@Composable
fun Task4AnalyticsContent() {
    var userType by remember { mutableStateOf("Guest") }
    var forceRecomposeTrigger by remember { mutableIntStateOf(0) }
    var sideEffectRunCount by remember { mutableIntStateOf(0) }
    var recomposeCount by remember { mutableIntStateOf(0) }

    // Reading forceRecomposeTrigger forces recomposition when clicked
    if (forceRecomposeTrigger < 0) Log.d("Task4Analytics", "never")

    recomposeCount++

    // SideEffect runs after every successful recomposition/commit
    SideEffect {
        sideEffectRunCount++
        Analytics.userType = userType
        Log.d("Task4Analytics", "SideEffect run count: $sideEffectRunCount, Analytics.userType: ${Analytics.userType}")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Task 4: SideEffect & Analytics", style = MaterialTheme.typography.titleLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("State & Analytics Tracker Info:", style = MaterialTheme.typography.titleSmall)
                Text("UI State userType: $userType", style = MaterialTheme.typography.bodyMedium)
                Text("Analytics.userType: ${Analytics.userType}", style = MaterialTheme.typography.bodyMedium)
                Text("Recomposition count: $recomposeCount", style = MaterialTheme.typography.bodyMedium)
                Text("SideEffect run count: $sideEffectRunCount", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text("Change State:")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { userType = "Guest" }) { Text("Guest") }
            Button(onClick = { userType = "Premium" }) { Text("Premium") }
            Button(onClick = { userType = "Admin" }) { Text("Admin") }
        }

        Button(
            onClick = { forceRecomposeTrigger++ },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Trigger Recomposition (No userType Change)")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Key Concepts:", style = MaterialTheme.typography.titleSmall)
                Text(
                    "1. Why SideEffect runs without state change:\n" +
                            "SideEffect executes after every successful recomposition commit. Tapping 'Trigger Recomposition' forces a recomposition, so SideEffect runs and updates Analytics again even if userType remains unchanged.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "2. Lecture Attempt Model:\n" +
                            "In Compose's attempt model, composition attempts can be speculative or discarded prior to committing to the UI tree; SideEffect guarantees that state updates to non-Compose objects only happen after a composition attempt successfully commits.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
