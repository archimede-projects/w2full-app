package com.archimede.w2full.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.archimede.w2full.ui.history.PriceHistoryRoute
import com.archimede.w2full.ui.refueling.RefuelingRoute
import com.archimede.w2full.ui.settings.SettingsRoute
import com.archimede.w2full.ui.stations.NearbyStationsRoute

private enum class RootDestination {
    REFUELING,
    STATIONS,
    HISTORY,
    SETTINGS,
}

@Composable
fun W2FullRoot() {
    var destination by rememberSaveable { mutableStateOf(RootDestination.REFUELING) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = destination == RootDestination.REFUELING,
                    onClick = { destination = RootDestination.REFUELING },
                    icon = { Text("R") },
                    label = { Text("Registro") },
                )
                NavigationBarItem(
                    selected = destination == RootDestination.STATIONS,
                    onClick = { destination = RootDestination.STATIONS },
                    icon = { Text("S") },
                    label = { Text("Stazioni") },
                )
                NavigationBarItem(
                    selected = destination == RootDestination.HISTORY,
                    onClick = { destination = RootDestination.HISTORY },
                    icon = { Text("H") },
                    label = { Text("Storico") },
                )
                NavigationBarItem(
                    selected = destination == RootDestination.SETTINGS,
                    onClick = { destination = RootDestination.SETTINGS },
                    icon = { Text("⚙") },
                    label = { Text("Impostazioni") },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (destination) {
                RootDestination.REFUELING -> RefuelingRoute()
                RootDestination.STATIONS -> NearbyStationsRoute()
                RootDestination.HISTORY -> PriceHistoryRoute()
                RootDestination.SETTINGS -> SettingsRoute()
            }
        }
    }
}