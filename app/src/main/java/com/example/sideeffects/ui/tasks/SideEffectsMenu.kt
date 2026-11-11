package com.example.sideeffects.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

sealed class Screen(val title: String) {
    object Menu : Screen("Side Effects Lab Menu")
    object Task1 : Screen("Task 1: Splash Screen")
    object Task2 : Screen("Task 2: Snackbar & CoroutineScope")
    object Task3 : Screen("Task 3: Lifecycle Logger")
    object Task4 : Screen("Task 4: SideEffect Analytics")
    object Task5 : Screen("Task 5: Scroll & snapshotFlow")
    object Task6 : Screen("Task 6: Bonus produceState")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideEffectsApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Menu) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentScreen.title) },
                navigationIcon = {
                    if (currentScreen !is Screen.Menu) {
                        TextButton(onClick = { currentScreen = Screen.Menu }) {
                            Text("Back")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                is Screen.Menu -> MenuScreen(onSelectScreen = { currentScreen = it })
                is Screen.Task1 -> Task1SplashContent()
                is Screen.Task2 -> Task2SnackbarContent()
                is Screen.Task3 -> Task3LifecycleContent()
                is Screen.Task4 -> Task4AnalyticsContent()
                is Screen.Task5 -> Task5ScrollContent()
                is Screen.Task6 -> Task6ProduceStateContent()
            }
        }
    }
}

@Composable
fun MenuScreen(onSelectScreen: (Screen) -> Unit) {
    val tasks = listOf(
        Screen.Task1 to "Task 1: Splash Screen (rememberUpdatedState)",
        Screen.Task2 to "Task 2: Snackbar (rememberCoroutineScope)",
        Screen.Task3 to "Task 3: Lifecycle Logger (DisposableEffect)",
        Screen.Task4 to "Task 4: Analytics (SideEffect)",
        Screen.Task5 to "Task 5: Scroll (derivedStateOf & snapshotFlow)",
        Screen.Task6 to "Task 6: Bonus (produceState image loading)"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Side Effects Lab - Part B Hands-on Tasks", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select a task below to test and inspect behavior:")
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(tasks.size) { index ->
            val (screen, description) = tasks[index]
            Button(
                onClick = { onSelectScreen(screen) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(description)
            }
        }
    }
}
