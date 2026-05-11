package com.evgramacharge.app.ui.calculator

import androidx.lifecycle.ViewModel
import kotlin.math.max

data class BatteryCalcResult(
    val energyKwh: Double,
    val extraRangeKm: Double,
)

class BatteryCalculatorViewModel : ViewModel() {

    fun calculate(
        batteryKwh: Double,
        currentSocPercent: Double,
        targetSocPercent: Double,
        consumptionKwhPer100km: Double,
    ): BatteryCalcResult? {
        if (batteryKwh <= 0 || consumptionKwhPer100km <= 0) return null
        val current = currentSocPercent.coerceIn(0.0, 100.0)
        val target = targetSocPercent.coerceIn(0.0, 100.0)
        if (target <= current) return null
        val deltaPercent = target - current
        val energy = batteryKwh * (deltaPercent / 100.0)
        val range = energy / consumptionKwhPer100km * 100.0
        return BatteryCalcResult(energyKwh = energy, extraRangeKm = max(0.0, range))
    }
}
