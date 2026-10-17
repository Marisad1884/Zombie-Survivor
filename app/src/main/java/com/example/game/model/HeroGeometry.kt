package com.example.game.model

import androidx.compose.ui.geometry.Offset

/**
 * Where things are inside the heroine's sprites (measured in sprite pixels, 1 px = Balance.PX units).
 * The engine uses it to find the muzzle; the renderer uses it to line the arms and gun up with the body.
 */
object HeroGeometry {
    /** Body sprite: the column that sits on the heroine's x position, and the row of her feet. */
    const val BODY_ANCHOR_COL = 14
    const val GROUND_ROW = 53

    /** Where the front shoulder is in the body sprite. The arm + gun sprite is pinned here and rotates around it. */
    const val SHOULDER_COL = 15
    const val SHOULDER_ROW = 21

    /** Pin point of each arm sprite (shoulder end), in that sprite's pixels. */
    fun armAnchor(type: WeaponType): Pair<Int, Int> = when (type) {
        WeaponType.REVOLVER -> 34 to 5
        WeaponType.SHOTGUN -> 47 to 8
        WeaponType.GRENADE -> 23 to 13
    }

    /** Muzzle (barrel tip, or the grenade in her hand), in the arm sprite's pixels. */
    private fun muzzlePx(type: WeaponType): Pair<Int, Int> = when (type) {
        WeaponType.REVOLVER -> 1 to 3
        WeaponType.SHOTGUN -> 1 to 7
        WeaponType.GRENADE -> 6 to 9
    }

    /** Muzzle position relative to the shoulder when she is aiming straight left, in world units. */
    fun muzzleOffset(type: WeaponType): Offset {
        val (ax, ay) = armAnchor(type)
        val (mx, my) = muzzlePx(type)
        return Offset((mx - ax) * Balance.PX, (my - ay) * Balance.PX)
    }
}
