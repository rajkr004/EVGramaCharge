package com.evgramacharge.app.data.model

data class Booking(
    val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val userId: String = "",
    val startEpochMs: Long = 0L,
    val endEpochMs: Long = 0L,
    val estimatedEnergyKwh: Double = 0.0,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "hostId" to hostId,
        "hostName" to hostName,
        "userId" to userId,
        "startEpochMs" to startEpochMs,
        "endEpochMs" to endEpochMs,
        "estimatedEnergyKwh" to estimatedEnergyKwh,
        "status" to status,
        "createdAt" to createdAt,
    )

    companion object {
        const val COLLECTION = "bookings"

        fun fromDoc(id: String, data: Map<String, Any?>): Booking {
            return Booking(
                id = id,
                hostId = data["hostId"] as? String ?: "",
                hostName = data["hostName"] as? String ?: "",
                userId = data["userId"] as? String ?: "",
                startEpochMs = (data["startEpochMs"] as? Number)?.toLong() ?: 0L,
                endEpochMs = (data["endEpochMs"] as? Number)?.toLong() ?: 0L,
                estimatedEnergyKwh = (data["estimatedEnergyKwh"] as? Number)?.toDouble() ?: 0.0,
                status = data["status"] as? String ?: "PENDING",
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            )
        }
    }
}
