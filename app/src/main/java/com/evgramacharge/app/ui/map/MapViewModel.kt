package com.evgramacharge.app.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evgramacharge.app.EVGramaChargeApplication
import com.evgramacharge.app.data.model.ChargingHost
import com.evgramacharge.app.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MapViewModel(
    application: Application,
    repository: FirestoreRepository,
) : AndroidViewModel(application) {

    val hosts: StateFlow<List<ChargingHost>> = repository.observeHosts()
        .map { list ->
            if (list.isEmpty()) {
                // Return dummy data if Firestore is empty so user sees pins immediately
                listOf(
                    ChargingHost(
                        id = "dummy_1",
                        name = "Sample Charging Point",
                        address = "MG Road, Bengaluru",
                        latitude = 12.9716,
                        longitude = 77.5946,
                        pricePerKwh = 15.0,
                        connectorType = "Type 2"
                    ),
                    ChargingHost(
                        id = "dummy_2",
                        name = "Village Green Charger",
                        address = "Near Panchayat Office",
                        latitude = 12.9800,
                        longitude = 77.6000,
                        pricePerKwh = 12.5,
                        connectorType = "CCS"
                    )
                )
            } else list
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(app: Application): androidx.lifecycle.ViewModelProvider.Factory {
            val repo = (app as EVGramaChargeApplication).repository
            return object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return MapViewModel(app, repo) as T
                }
            }
        }
    }
}
