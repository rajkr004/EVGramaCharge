package com.evgramacharge.app.data.model

data class ChargingHost(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val pricePerKwh: Double = 0.0,
    val connectorType: String = "Type 2",
    val ownerId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "address" to address,
        "latitude" to latitude,
        "longitude" to longitude,
        "pricePerKwh" to pricePerKwh,
        "connectorType" to connectorType,
        "ownerId" to ownerId,
        "createdAt" to createdAt,
    )

    companion object {
        const val COLLECTION = "charging_hosts"

        fun fromDoc(id: String, data: Map<String, Any?>): ChargingHost {
            return ChargingHost(
                id = id,
                name = data["name"] as? String ?: "",
                address = data["address"] as? String ?: "",
                latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                pricePerKwh = (data["pricePerKwh"] as? Number)?.toDouble() ?: 0.0,
                connectorType = data["connectorType"] as? String ?: "Type 2",
                ownerId = data["ownerId"] as? String ?: "",
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            )
        }
    }
}
