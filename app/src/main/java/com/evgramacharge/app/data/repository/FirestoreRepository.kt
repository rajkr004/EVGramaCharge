package com.evgramacharge.app.data.repository

import com.evgramacharge.app.data.model.Booking
import com.evgramacharge.app.data.model.ChargingHost
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    fun observeHosts(): Flow<List<ChargingHost>> = callbackFlow {
        val registration: ListenerRegistration = db.collection(ChargingHost.COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    ChargingHost.fromDoc(doc.id, doc.data ?: emptyMap())
                }.orEmpty().sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeBookingsForUser(userId: String): Flow<List<Booking>> = callbackFlow {
        val registration: ListenerRegistration = db.collection(Booking.COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    Booking.fromDoc(doc.id, doc.data ?: emptyMap())
                }.orEmpty().sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeHostsForOwner(ownerId: String): Flow<List<ChargingHost>> = callbackFlow {
        val registration: ListenerRegistration = db.collection(ChargingHost.COLLECTION)
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    ChargingHost.fromDoc(doc.id, doc.data ?: emptyMap())
                }.orEmpty().sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveHost(host: ChargingHost): Result<String> = runCatching {
        val doc = if (host.id.isNotBlank()) {
            db.collection(ChargingHost.COLLECTION).document(host.id)
        } else {
            db.collection(ChargingHost.COLLECTION).document()
        }
        doc.set(host.copy(id = doc.id).toMap()).await()
        doc.id
    }

    suspend fun createBooking(booking: Booking): Result<String> = runCatching {
        val doc = db.collection(Booking.COLLECTION).document()
        doc.set(booking.copy(id = doc.id).toMap()).await()
        doc.id
    }
}
