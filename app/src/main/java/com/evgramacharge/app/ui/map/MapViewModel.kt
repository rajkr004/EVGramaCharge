package com.evgramacharge.app.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evgramacharge.app.EVGramaChargeApplication
import com.evgramacharge.app.data.model.ChargingHost
import com.evgramacharge.app.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MapViewModel(
    application: Application,
    repository: FirestoreRepository,
) : AndroidViewModel(application) {

    val hosts: StateFlow<List<ChargingHost>> = repository.observeHosts()
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
