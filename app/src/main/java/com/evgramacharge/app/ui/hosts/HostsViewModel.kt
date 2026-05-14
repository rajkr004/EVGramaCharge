package com.evgramacharge.app.ui.hosts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evgramacharge.app.EVGramaChargeApplication
import com.evgramacharge.app.data.model.ChargingHost
import com.evgramacharge.app.data.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class HostsViewModel(
    application: Application,
    private val repository: FirestoreRepository,
) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()

    private val userIdFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { a ->
            trySend(a.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val myHosts: StateFlow<List<ChargingHost>> = userIdFlow
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeHostsForOwner(uid)
        }
        .map { list ->
            if (list.isEmpty()) {
                listOf(
                    ChargingHost(
                        id = "h1",
                        name = "My Sample Station",
                        address = "123 Green Lane",
                        pricePerKwh = 10.0,
                        connectorType = "Type 2"
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
                    return HostsViewModel(app, repo) as T
                }
            }
        }
    }
}
