package com.ishara.app.core.designsystem.asset

import com.ishara.app.R

/**
 * Centralized catalog of Ishaara Brand and Vehicle visual assets.
 * References verified static drawables and provides guidelines for their usage.
 */
object IshaaraAssets {
    /**
     * Primary Bus Photographic / Editorial Visual.
     * Use exclusively for Onboarding, Hero Cards, and Brand moments.
     * NOT to be used as live GPS map marker icons.
     */
    val BusHeroDrawable: Int = R.drawable.ishara_bus

    /**
     * Launcher branding drawables.
     */
    val LauncherForeground: Int = R.drawable.ic_launcher_foreground
    val LauncherBackground: Int = R.drawable.ic_launcher_background

    /**
     * Relative path constants to repository asset images for dynamic loading / documentation.
     */
    const val ASSET_PATH_PRIMARY_LOGO = "assets/images/ishara-primary-logo.png"
    const val ASSET_PATH_COMPACT_LOGO = "assets/images/compact_logo.png"
    const val ASSET_PATH_BUS_SIDEVIEW = "assets/images/bus_sideview.png"
    const val ASSET_PATH_BUS_BACKVIEW = "assets/images/bus_backview.png"
    const val ASSET_PATH_BUS_BLACK_WHITE = "assets/images/bus_black&white.png"
}
