package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GameEngine
import com.example.game.engine.GameStatus
import com.example.game.model.WeaponType

@Composable
fun GameHUD(
    engine: GameEngine,
    onPauseClick: () -> Unit,
    onShopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentWeapon = engine.currentWeapon
    val maxHp = engine.defenseUpgrades.maxHouseHp
    val hpRatio = (engine.houseHp / maxHp).coerceIn(0f, 1f)

    val minutes = (engine.survivedSeconds / 60).toInt()
    val seconds = (engine.survivedSeconds % 60).toInt()
    val formattedTime = "%02d:%02d".format(minutes, seconds)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // TOP HUD BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Soldier & House Status Card
            Surface(
                color = Color(0xDD1B2A1B),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF33691E)),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xFF132B20),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF76FF03)),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Soldado",
                                tint = Color(0xFF76FF03),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BASE DEFENSOR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA5D6A7)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "${engine.houseHp.toInt()}/${maxHp.toInt()} HP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF263238))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(hpRatio)
                                    .background(
                                        Brush.horizontalGradient(
                                            if (hpRatio > 0.45f) listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))
                                            else listOf(Color(0xFFE53935), Color(0xFFFF7043))
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // Wave & Survival Timer Badge
            Surface(
                color = Color(0xDD0D1F17),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB300)),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "OLEADA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F)
                        )
                        Text(
                            text = "${engine.wave}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(22.dp)
                            .background(Color(0x44FFFFFF))
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Tiempo",
                            tint = Color(0xFF80DEEA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedTime,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Coins & Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Coins Counter
                Surface(
                    color = Color(0xDD1B2A1B),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD700))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Monedas",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "$${engine.coins}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                // Armory / Shop Button (Pauses game on click)
                IconButton(
                    onClick = onShopClick,
                    modifier = Modifier
                        .testTag("hud_shop_button")
                        .size(38.dp)
                        .background(Color(0xDD3E2723), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF8D6E63), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Tienda",
                        tint = Color(0xFFFFCC80),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Pause Button
                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .testTag("hud_pause_button")
                        .size(38.dp)
                        .background(Color(0xDD263238), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF546E7A), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pausar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // WAVE CLEARED INTERMEDIATE BANNER
        AnimatedVisibility(
            visible = engine.status == GameStatus.WAVE_CLEARED,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color(0xEE14261C),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD54F)),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "¡OLEADA ${engine.wave} COMPLETADA!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD54F)
                    )
                    Text(
                        text = "Siguiente oleada en ${engine.waveClearCountdown.toInt().coerceAtLeast(1)}s...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onShopClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E2723)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ARMERÍA (PAUSAR)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { engine.continueToNextWave() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("INICIAR YA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // VERTICAL WEAPONS SELECTOR & MOLOTOV (Placed on top-left below health bar so zombies are never covered)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 52.dp)
                .width(138.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = Color(0xEE0E1C15),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2E7D32)),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 210.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    engine.weapons.forEachIndexed { index, weapon ->
                        val isSelected = engine.selectedWeaponIndex == index
                        val isUnlocked = weapon.isUnlocked
                        val canAffordUnlock = engine.coins >= weapon.type.unlockCost

                        Box(
                            modifier = Modifier
                                .testTag("weapon_slot_$index")
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> Color(0xFF2E7D32)
                                        isUnlocked -> Color(0xFF1B2E24)
                                        canAffordUnlock -> Color(0xFF3E2723)
                                        else -> Color(0x55263238)
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = when {
                                        isSelected -> Color(0xFFFFD54F)
                                        !isUnlocked && canAffordUnlock -> Color(0xFFFF9800)
                                        isUnlocked -> Color(0x4481C784)
                                        else -> Color(0x2281C784)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (weapon.isUnlocked) {
                                        engine.selectWeapon(index)
                                    } else {
                                        if (engine.coins >= weapon.type.unlockCost) {
                                            engine.unlockWeapon(weapon.type)
                                        } else {
                                            onShopClick()
                                        }
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
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
                                        },
                                        contentDescription = weapon.type.title,
                                        tint = if (isUnlocked) Color.White else if (canAffordUnlock) Color(0xFFFFD54F) else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = weapon.type.title.split(" ").first(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnlocked) Color.White else if (canAffordUnlock) Color(0xFFFFD54F) else Color.Gray
                                    )
                                }

                                if (isUnlocked) {
                                    Text(
                                        text = if (weapon.isReloading) "Recarga" else "${weapon.currentAmmo}/${weapon.maxAmmo}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (weapon.currentAmmo == 0) Color(0xFFFF5252) else Color(0xFFA5D6A7)
                                    )
                                } else {
                                    Text(
                                        text = "$${weapon.type.unlockCost}",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (canAffordUnlock) Color(0xFFFFD54F) else Color(0xFFFFB74D)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // MOLOTOV BUTTON: bottom-left, outside the shooting zone, throws at the densest zombie group
        val molotovCount = engine.defenseUpgrades.molotovCount
        val cooldown = engine.molotovCooldownRemaining
        val molotovReady = molotovCount > 0 && cooldown <= 0f && engine.status == GameStatus.PLAYING
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 6.dp, bottom = 10.dp)
                .size(68.dp)
        ) {
            Box(
                modifier = Modifier
                    .testTag("hud_molotov_button")
                    .align(Alignment.Center)
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        if (molotovReady) {
                            Brush.radialGradient(listOf(Color(0xFFFFB300), Color(0xFFE64A19)))
                        } else {
                            Brush.radialGradient(listOf(Color(0xFF455A64), Color(0xFF263238)))
                        }
                    )
                    .border(
                        2.dp,
                        if (molotovReady) Color(0xFFFFE082) else Color(0xFF546E7A),
                        CircleShape
                    )
                    .clickable(enabled = molotovReady) { engine.throwMolotovAtThreat() },
                contentAlignment = Alignment.Center
            ) {
                if (cooldown > 0f) {
                    Text(
                        text = "${cooldown.toInt() + 1}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Molotov",
                        tint = if (molotovCount > 0) Color.White else Color(0xFF78909C),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // Remaining molotovs badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B2A1B))
                    .border(1.5.dp, Color(0xFFFFD54F), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$molotovCount",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        // RELOAD BAR: appears at the bottom center only while the current weapon is reloading
        if (currentWeapon.isReloading && engine.status == GameStatus.PLAYING) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "RECARGANDO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD54F),
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC263238))
                        .border(1.dp, Color(0x66FFD54F), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(currentWeapon.reloadProgress.coerceIn(0f, 1f))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFFFEE58)))
                            )
                    )
                }
            }
        }
    }
}
