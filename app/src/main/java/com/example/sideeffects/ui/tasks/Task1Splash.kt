package com.example.sideeffects.ui.tasks

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/*
 * TASK 1: Splash Screen & rememberUpdatedState
 *
 * REASON FOR LaunchedEffect(onTimeout) RESTARTING:
 * When `onTimeout` is passed directly as the key to `LaunchedEffect(onTimeout)`, Compose checks key equality
 * on every recomposition. Because `version` updates every 500ms in the parent composable, a new lambda
 * instance for `onTimeout` is created every 500ms.
 * Consequently, `LaunchedEffect(onTimeout)` detects a new key every 500ms, cancels the existing coroutine
 * (which was waiting for 2000ms), and launches a brand new coroutine with a fresh 2000ms delay.
 * Because 500ms < 2000ms, the timer is repeatedly cancelled before completing 2 seconds, so `onTimeout` is NEVER called!
 *
 * SOLUTION (rememberUpdatedState):
 * `rememberUpdatedState(onTimeout)` captures the latest reference to `onTimeout` without triggering a key change in
 * `LaunchedEffect(Unit)`. `LaunchedEffect(Unit)` runs once for the full 2 seconds without restarting, and when it finishes,
 * `currentOnTimeout()` invokes the latest version of the lambda.
 */

@Composable
fun LandingScreen(onTimeout: () -> Unit) {
    // Correct variant: rememberUpdatedState ensures the latest lambda is called after 2 seconds
    val currentOnTimeout by rememberUpdatedState(onTimeout)

    LaunchedEffect(Unit) {
        delay(2000L)
        currentOnTimeout()
    }
}

@Composable
fun LandingScreenDirect(onTimeout: () -> Unit) {
    // Variant 1: Capturing lambda directly inside LaunchedEffect(Unit) without rememberUpdatedState
    LaunchedEffect(Unit) {
        delay(2000L)
        onTimeout() // Stale reference captured at initial launch time (v0)
    }
}

@Composable
fun LandingScreenRestarting(onTimeout: () -> Unit) {
    // Incorrect key variant: LaunchedEffect(onTimeout) restarts every 500ms when lambda changes
    LaunchedEffect(onTimeout) {
        delay(2000L)
        onTimeout() // Never reached because LaunchedEffect continuously restarts every 500ms
    }
}

@Composable
fun Task1SplashContent() {
    var version by remember { mutableIntStateOf(0) }
    var testKey by remember { mutableIntStateOf(0) } // Key to re-trigger splash test
    var directLogs by remember { mutableStateOf(listOf<String>()) }
    var updatedLogs by remember { mutableStateOf(listOf<String>()) }
    var restartingStatus by remember { mutableStateOf("Timer running... (restarting every 500ms)") }

    // Version counter increasing every 500ms
    LaunchedEffect(Unit) {
        while (true) {
            delay(500L)
            version++
        }
    }

    // Lambda created on each recomposition capturing current version
    val onTimeoutDirect = {
        val msg = "Direct captured v$version"
        Log.d("Task1Splash", msg)
        directLogs = directLogs + msg
    }

    val onTimeoutUpdated = {
        val msg = "UpdatedState v$version"
        Log.d("Task1Splash", msg)
        updatedLogs = updatedLogs + msg
    }

    val onTimeoutRestarting = {
        val msg = "Restarting v$version"
        Log.d("Task1Splash", msg)
        restartingStatus = msg
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Task 1: Splash Screen & rememberUpdatedState", style = MaterialTheme.typography.titleLarge)
        Text("Version counter: v$version (updates every 500ms)", style = MaterialTheme.typography.titleMedium)

        Button(onClick = {
            directLogs = emptyList()
            updatedLogs = emptyList()
            restartingStatus = "Timer running... (restarting every 500ms)"
            testKey++
        }) {
            Text("Restart Splash Screen Test")
        }

        key(testKey) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Variant 1: Direct capturing (Stale lambda)
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Variant 1: Direct", style = MaterialTheme.typography.titleSmall)
                        Text("Captures lambda at launch", style = MaterialTheme.typography.bodySmall)
                        LandingScreenDirect(onTimeout = onTimeoutDirect)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Result:")
                        if (directLogs.isEmpty()) {
                            Text("Waiting 2s...", color = MaterialTheme.colorScheme.outline)
                        } else {
                            directLogs.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }

                // Variant 2: rememberUpdatedState (Latest lambda)
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Variant 2: UpdatedState", style = MaterialTheme.typography.titleSmall)
                        Text("Uses rememberUpdatedState", style = MaterialTheme.typography.bodySmall)
                        LandingScreen(onTimeout = onTimeoutUpdated)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Result:")
                        if (updatedLogs.isEmpty()) {
                            Text("Waiting 2s...", color = MaterialTheme.colorScheme.outline)
                        } else {
                            updatedLogs.forEach { Text(it, color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            }

            // Demonstration card for LaunchedEffect(onTimeout)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Experiment: LaunchedEffect(onTimeout)", style = MaterialTheme.typography.titleSmall)
                    Text("Key changes every 500ms -> LaunchedEffect continuously restarts delay(2000ms).", style = MaterialTheme.typography.bodySmall)
                    LandingScreenRestarting(onTimeout = onTimeoutRestarting)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Status: $restartingStatus", color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Logcat Verification:", style = MaterialTheme.typography.titleSmall)
                Text("- Variant 1 prints old version: 'Direct captured v0'", style = MaterialTheme.typography.bodySmall)
                Text("- Variant 2 prints latest version: 'UpdatedState v4'", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
