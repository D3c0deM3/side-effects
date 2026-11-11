package com.example.sideeffects.ui.tasks

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/*
 * TASK 3: Lifecycle Logger & DisposableEffect
 *
 * PURPOSE & REQUIREMENTS:
 * - `DisposableEffect` is used for side-effects that require cleanup when keys change or when the Composable
 *   leaves the Composition (e.g., adding and removing listeners, observers, or callbacks).
 * - Key: `lifecycleOwner` (LocalLifecycleOwner.current).
 * - Cleanup: Inside `onDispose { ... }`, remove the `LifecycleEventObserver` and log the removal.
 *
 * WHAT HAPPENS IF onDispose IS REMOVED:
 * Removing `onDispose` causes a compilation error:
 * "Target of DisposableEffect must end with an onDispose call."
 * DisposableEffect strictly requires an `onDispose` block as its final statement to guarantee proper resource cleanup.
 */

// Global counters to verify addObserver vs removeObserver line counts across toggles
var globalAddCount = 0
var globalRemoveCount = 0

@Composable
fun LifecycleLoggerComponent(onCountsUpdated: (addCount: Int, removeCount: Int) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> Log.d("LifecycleLogger", "ON_START event received")
                Lifecycle.Event.ON_STOP -> Log.d("LifecycleLogger", "ON_STOP event received")
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        globalAddCount++
        Log.d("LifecycleLogger", "addObserver called (Total adds: $globalAddCount)")
        onCountsUpdated(globalAddCount, globalRemoveCount)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            globalRemoveCount++
            Log.d("LifecycleLogger", "removeObserver called in onDispose (Total removes: $globalRemoveCount)")
            onCountsUpdated(globalAddCount, globalRemoveCount)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("LifecycleLogger is currently ACTIVE", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text("Observing ON_START and ON_STOP events. Check Logcat for details.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun Task3LifecycleContent() {
    var showLogger by remember { mutableStateOf(true) }
    var addCount by remember { mutableIntStateOf(globalAddCount) }
    var removeCount by remember { mutableIntStateOf(globalRemoveCount) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Task 3: Lifecycle Logger & DisposableEffect", style = MaterialTheme.typography.titleLarge)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Show Lifecycle Logger Component:")
            Switch(
                checked = showLogger,
                onCheckedChange = { showLogger = it }
            )
        }

        if (showLogger) {
            LifecycleLoggerComponent(onCountsUpdated = { add, remove ->
                addCount = add
                removeCount = remove
            })
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LifecycleLogger is INACTIVE (Disposed)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline)
                    Text("onDispose has removed the observer.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Verification & Observer Counts:", style = MaterialTheme.typography.titleSmall)
                Text("addObserver calls: $addCount", style = MaterialTheme.typography.bodyMedium)
                Text("removeObserver calls: $removeCount", style = MaterialTheme.typography.bodyMedium)
                if (addCount == removeCount) {
                    Text("Counts match! Equal number of addObserver and removeObserver calls.", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Component currently active (add = remove + 1). Toggle switch to balance counts.", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Build error note: Omitting onDispose causes compile error: 'Target of DisposableEffect must end with an onDispose call'.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
