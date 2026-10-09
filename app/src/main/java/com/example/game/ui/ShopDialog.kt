package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.engine.GameEngine
import com.example.game.engine.GameStatus
import com.example.game.model.Weapon
import com.example.game.model.WeaponType

@Composable
fun ShopDialog(
    engine: GameEngine,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBetweenWaves = engine.wasBetweenWavesWhenShopOpened || engine.status == GameStatus.WAVE_CLEARED

    Dialog(onDismissRequest = {
        if (isBetweenWaves) {
            engine.continueToNextWave()
        }
        engine.closeShop()
        onDismiss()
    }) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF14241B),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF33691E)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isBetweenWaves) "¡OLEADA ${engine.wave} SUPERADA!" else "ARMERÍA DEL PANTANO",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isBetweenWaves) Color(0xFFFFD54F) else Color.White
                        )
                        Text(
                            text = if (isBetweenWaves) "Juego pausado. Equípate para la siguiente oleada"
                            else "Juego pausado: Mejora tus defensas y armas con calma",
                            fontSize = 11.sp,
                            color = Color(0xFFA5D6A7)
                        )
                    }

                    // Coins Badge
                    Surface(
                        color = Color(0xFF1E3526),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD700))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Monedas",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "$${engine.coins}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Shop Items
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "DEFENSAS DE LA CABAÑA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFCC80)
                        )
                    }

                    // Quick Repair
                    item {
                        val maxHp = engine.defenseUpgrades.maxHouseHp
                        val canRepair = engine.houseHp < maxHp
                        ShopItemCard(
                            icon = Icons.Default.Healing,
                            title = "Reparación Rápida (+50 HP)",
                            subtitle = "Salud actual: ${engine.houseHp.toInt()} / ${maxHp.toInt()} HP",
                            cost = engine.defenseUpgrades.repairHpCost,
                            canAfford = engine.coins >= engine.defenseUpgrades.repairHpCost && canRepair,
                            isMaxed = !canRepair,
                            buttonText = if (canRepair) "REPARAR" else "COMPLETO",
                            onBuy = { engine.repairHouse() }
                        )
                    }

                    // Full Repair
                    item {
                        val maxHp = engine.defenseUpgrades.maxHouseHp
                        val canRepair = engine.houseHp < maxHp
                        ShopItemCard(
                            icon = Icons.Default.Build,
                            title = "Reparación Total (100% HP)",
                            subtitle = "Restaura la barricada por completo",
                            cost = engine.defenseUpgrades.fullRepairCost,
                            canAfford = engine.coins >= engine.defenseUpgrades.fullRepairCost && canRepair,
                            isMaxed = !canRepair,
                            buttonText = if (canRepair) "RESTAURAR" else "COMPLETO",
                            onBuy = { engine.repairHouseFull() }
                        )
                    }

                    // Fortify Max HP
                    item {
                        val level = engine.defenseUpgrades.maxHouseHpLevel
                        val isMax = level >= 10
                        ShopItemCard(
                            icon = Icons.Default.Security,
                            title = "Fortificar Cabaña (Niv. $level/10)",
                            subtitle = "Aumenta la vida máxima de la base a ${(100 + level * 40)} HP",
                            cost = engine.defenseUpgrades.maxHpUpgradeCost,
                            canAfford = engine.coins >= engine.defenseUpgrades.maxHpUpgradeCost && !isMax,
                            isMaxed = isMax,
                            buttonText = if (isMax) "MÁXIMO" else "MEJORAR",
                            onBuy = { engine.upgradeMaxHp() }
                        )
                    }

                    // Barbed Wire Spikes
                    item {
                        val level = engine.defenseUpgrades.barbedWireLevel
                        val isMax = level >= 5
                        ShopItemCard(
                            icon = Icons.Default.Shield,
                            title = if (level == 0) "Instalar Alambre de Púas" else "Reforzar Púas (Niv. $level/5)",
                            subtitle = "Ralentiza y desgarra a los zombis que tocan la barricada",
                            cost = engine.defenseUpgrades.barbedWireUpgradeCost,
                            canAfford = engine.coins >= engine.defenseUpgrades.barbedWireUpgradeCost && !isMax,
                            isMaxed = isMax,
                            buttonText = if (isMax) "MÁXIMO" else if (level == 0) "COMPRAR" else "MEJORAR",
                            onBuy = { engine.upgradeBarbedWire() }
                        )
                    }

                    // Roof Sentry Turret
                    item {
                        val level = engine.defenseUpgrades.turretLevel
                        val isMax = level >= 5
                        ShopItemCard(
                            icon = Icons.Default.PrecisionManufacturing,
                            title = if (level == 0) "Torreta Centinela de Techo" else "Mejorar Torreta (Niv. $level/5)",
                            subtitle = "Ametralladora automática en el techo que dispara a los zombis",
                            cost = engine.defenseUpgrades.turretUpgradeCost,
                            canAfford = engine.coins >= engine.defenseUpgrades.turretUpgradeCost && !isMax,
                            isMaxed = isMax,
                            buttonText = if (isMax) "MÁXIMO" else if (level == 0) "COMPRAR" else "MEJORAR",
                            onBuy = { engine.upgradeTurret() }
                        )
                    }

                    // Buy Molotov
                    item {
                        val count = engine.defenseUpgrades.molotovCount
                        val isMax = count >= 5
                        ShopItemCard(
                            icon = Icons.Default.LocalFireDepartment,
                            title = "Cóctel Molotov Táctico ($count/5)",
                            subtitle = "Fuego que quema el pantano durante 5 segundos",
                            cost = engine.defenseUpgrades.molotovBuyCost,
                            canAfford = engine.coins >= engine.defenseUpgrades.molotovBuyCost && !isMax,
                            isMaxed = isMax,
                            buttonText = if (isMax) "LLENO" else "COMPRAR",
                            onBuy = { engine.buyMolotov() }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ARSENAL DE ARMAS (HASTA NIVEL 10)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFCC80)
                        )
                    }

                    // Weapons list
                    items(engine.weapons.size) { index ->
                        val weapon = engine.weapons[index]
                        WeaponShopCard(
                            weapon = weapon,
                            engine = engine,
                            onUnlock = { engine.unlockWeapon(weapon.type) },
                            onUpgrade = { engine.upgradeWeapon(weapon.type) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action
                Button(
                    onClick = {
                        if (isBetweenWaves) {
                            engine.continueToNextWave()
                        }
                        engine.closeShop()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("shop_continue_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (isBetweenWaves) Icons.Default.PlayArrow else Icons.Default.Check,
                        contentDescription = "Continuar"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBetweenWaves) "¡INICIAR OLEADA ${engine.wave + 1} AHORA!" else "CONTINUAR COMBATE",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    cost: Int,
    canAfford: Boolean,
    isMaxed: Boolean,
    buttonText: String,
    onBuy: () -> Unit
) {
    Surface(
        color = Color(0xFF1B3024),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B4D3A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color(0xFF274333),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color(0xFFA5D6A7),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }
            }

            Button(
                onClick = onBuy,
                enabled = canAfford && !isMaxed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF388E3C),
                    disabledContainerColor = Color(0xFF263238)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (!isMaxed) {
                    Text(
                        text = "$buttonText ($$cost)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canAfford) Color.White else Color.Gray
                    )
                } else {
                    Text(
                        text = buttonText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
private fun WeaponShopCard(
    weapon: Weapon,
    engine: GameEngine,
    onUnlock: () -> Unit,
    onUpgrade: () -> Unit
) {
    val isUnlocked = weapon.isUnlocked
    val isMaxed = weapon.level >= weapon.maxLevel
    val cost = if (!isUnlocked) weapon.type.unlockCost else weapon.upgradeCost
    val canAfford = engine.coins >= cost
    val isEquipped = engine.currentWeapon.type == weapon.type

    Surface(
        color = Color(0xFF1B3024),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isEquipped) 2.dp else 1.dp,
            color = if (isEquipped) Color(0xFFFFD54F) else Color(0xFF2B4D3A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = if (isEquipped) Color(0xFF2E7D32) else if (isUnlocked) Color(0xFF1E3526) else Color(0xFF37474F),
                    shape = RoundedCornerShape(10.dp),
                    border = if (isEquipped) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F)) else null,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (weapon.type) {
                                WeaponType.PISTOL -> Icons.Default.Adjust
                                WeaponType.CROSSBOW -> Icons.Default.Navigation
                                WeaponType.SHOTGUN -> Icons.Default.FlashOn
                                WeaponType.RIFLE -> Icons.Default.Speed
                                WeaponType.FLAMETHROWER -> Icons.Default.Whatshot
                                WeaponType.LAUNCHER -> Icons.Default.RocketLaunch
                                WeaponType.MINIGUN -> Icons.Default.Cyclone
                                WeaponType.SNIPER -> Icons.Default.GpsFixed
                                else -> Icons.Default.Security
                            },
                            contentDescription = weapon.type.title,
                            tint = if (isEquipped) Color(0xFFFFD54F) else if (isUnlocked) Color.White else Color(0xFFB0BEC5),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(end = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = weapon.type.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (isUnlocked) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF2E7D32),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Niv. ${weapon.level}/10",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (isEquipped) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFFF8F00),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "EN USO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (!isUnlocked) weapon.type.description
                        else "Daño: ${weapon.damage.toInt()} | Cargador: ${weapon.maxAmmo} | Cadencia: ${(1000f / weapon.cooldownMs).toInt()} d/s",
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Equip button if unlocked but not equipped
                if (isUnlocked && !isEquipped) {
                    OutlinedButton(
                        onClick = {
                            val idx = engine.weapons.indexOf(weapon)
                            if (idx != -1) engine.selectWeapon(idx)
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA5D6A7)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784))
                    ) {
                        Text(
                            text = "EQUIPAR",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Unlock or Upgrade Button
                Button(
                    onClick = {
                        if (!isUnlocked) onUnlock() else onUpgrade()
                    },
                    enabled = canAfford && !isMaxed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isUnlocked) Color(0xFFE65100) else Color(0xFF388E3C),
                        disabledContainerColor = Color(0xFF263238)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when {
                            isMaxed -> "MÁXIMO"
                            !isUnlocked -> "DESBLOQUEAR ($$cost)"
                            else -> "MEJORAR ($$cost)"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canAfford && !isMaxed) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}
