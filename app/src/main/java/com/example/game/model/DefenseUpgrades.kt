package com.example.game.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

class DefenseUpgrades(
    initialMaxHouseHpLevel: Int = 1,
    initialBarbedWireLevel: Int = 0,
    initialTurretLevel: Int = 0,
    initialMolotovCount: Int = 2,
    initialReloadSpeedLevel: Int = 1
) {
    var maxHouseHpLevel by mutableIntStateOf(initialMaxHouseHpLevel)
    var barbedWireLevel by mutableIntStateOf(initialBarbedWireLevel)
    var turretLevel by mutableIntStateOf(initialTurretLevel)
    var molotovCount by mutableIntStateOf(initialMolotovCount)
    var reloadSpeedLevel by mutableIntStateOf(initialReloadSpeedLevel)

    val maxHouseHp: Float
        get() = 100f + (maxHouseHpLevel - 1) * 40f

    val spikeDamagePerSec: Float
        get() = barbedWireLevel * 16f

    val spikeSlowFactor: Float
        get() = when (barbedWireLevel) {
            0 -> 1.0f
            1 -> 0.70f
            2 -> 0.55f
            3 -> 0.45f
            4 -> 0.38f
            else -> 0.30f
        }

    val turretDamage: Float
        get() = turretLevel * 14f

    val turretIntervalMs: Long
        get() = (800L - (turretLevel - 1) * 80L).coerceAtLeast(350L)

    val maxHpUpgradeCost: Int
        get() = 70 * maxHouseHpLevel

    val barbedWireUpgradeCost: Int
        get() = (barbedWireLevel + 1) * 100

    val turretUpgradeCost: Int
        get() = (turretLevel + 1) * 200

    val molotovBuyCost: Int = 70

    val repairHpCost: Int = 45 // for +50 HP repair
    val fullRepairCost: Int = 110
}
