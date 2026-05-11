package com.evgramacharge.app.ui.hosts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evgramacharge.app.EVGramaChargeApplication
import com.evgramacharge.app.data.model.ChargingHost
import com.evgramacharge.app.data.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class HostRegistrationViewModel(
    application: Application,
    private val repository: FirestoreRepository,
) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events

    fun saveHost(
        name: String,
        address: String,
        lat: Double,
        lng: Double,
        price: Double,
        connector: String,
    ) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _events.tryEmit(UiEvent.Error("Not signed in"))
            return
        }
        if (name.isBlank() || address.isBlank()) {
            _events.tryEmit(UiEvent.Error("Name and address are required"))
            return
        }
        val host = ChargingHost(
            name = name.trim(),
            address = address.trim(),
            latitude = lat,
            longitude = lng,
            pricePerKwh = price,
            connectorType = connector.ifBlank { "Type 2" },
            ownerId = uid,
        )
        viewModelScope.launch {
            val result = repository.saveHost(host)
            result.onSuccess {
                _events.emit(UiEvent.Saved)
            }.onFailure { e ->
                _events.emit(UiEvent.Error(e.message ?: "Save failed"))
            }
        }
    }

    sealed class UiEvent {
        data object Saved : UiEvent()
        data class Error(val message: String) : UiEvent()
    }

    companion object {
        fun factory(app: Application): androidx.lifecycle.ViewModelProvider.Factory {
            val repo = (app as EVGramaChargeApplication).repository
            return object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return HostRegistrationViewModel(app, repo) as T
                }
            }
        }
    }
}
