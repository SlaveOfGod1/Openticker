package com.openticker.data

import android.content.Context

/**
 * Everything the System Window hologram can be customised with.
 * Plain values so the live overlay and the in-app preview read the same thing.
 */
data class SystemWindowOptions(
    val accentArgb: Int = 0xFF6FF6FF.toInt(),
    /** Optional second/third colours for multi-colour themes (Saturday Morning). */
    val secondaryArgb: Int = 0,
    val tertiaryArgb: Int = 0,
    /** 0..100 — glow, beam and scanline strength. */
    val intensity: Int = 100,
    /** 24..48dp */
    val avatarSize: Int = 34,
    val showLogo: Boolean = true,
    val showTime: Boolean = true,
    /** 1..3 lines, or 0 for the rolling news ticker. */
    val lines: Int = 2,
    /** Projector beam, scanline and sweep. */
    val effect: Boolean = true,
    /** Preview kind: message, call, flight, music, notification. */
    val previewKind: String = "message",
    val sender: String = "Mark",
    val message: String =
        "Sir, your AI agent has successfully ordered the pizza. It ordered the large one with extra " +
            "cheese, paid with the card you left it, and the kitchen says twenty minutes. " +
            "Shall I set the table?",
    /** Your Image theme. */
    val imagePath: String = "",
    val imageX: Int = 0,
    val imageY: Int = 0,
    val imageScale: Int = 100,
    val imageOpacity: Int = 80,
    /** Dock button size for the illustrated theme pack, in percent. */
    val buttonScale: Int = 100
)

/**
 * Tiny synchronous preference store. Used from services where a suspend
 * DataStore read would be overkill.
 */
object Prefs {

    private const val FILE = "openticker_prefs"

    private fun sp(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = sp(context).getBoolean("enabled", true)

    fun setEnabled(context: Context, value: Boolean) {
        sp(context).edit().putBoolean("enabled", value).apply()
    }

    fun styleId(context: Context): String =
        sp(context).getString("style", "system_window") ?: "system_window"

    fun setStyleId(context: Context, id: String) {
        sp(context).edit().putString("style", id).apply()
    }

    /** The language the app speaks: a tag like "es", or "system". */
    fun language(context: Context): String =
        sp(context).getString("language", "system") ?: "system"

    fun setLanguage(context: Context, tag: String) {
        sp(context).edit().putString("language", tag).apply()
    }

    /** 0.5x .. 3.0x. Affects how long a ticker stays on screen. */
    fun speed(context: Context): Float = sp(context).getFloat("speed", 1f)

    fun setSpeed(context: Context, value: Float) {
        sp(context).edit().putFloat("speed", value).apply()
    }

    /**
     * How fast the theme animations play. 1.0 is the designed pace, 2.0 is
     * twice as fast and 0.5 is half speed.
     */
    fun animationSpeed(context: Context): Float =
        sp(context).getFloat("animation_speed", 1f).coerceIn(0.25f, 4f)

    fun setAnimationSpeed(context: Context, value: Float) {
        sp(context).edit().putFloat("animation_speed", value.coerceIn(0.25f, 4f)).apply()
    }

    /** Base time a ticker stays visible, in seconds. */
    fun duration(context: Context): Int = sp(context).getInt("duration", 8)

    fun setDuration(context: Context, value: Int) {
        sp(context).edit().putInt("duration", value).apply()
    }

    /** Width of the ticker card as a percentage of the screen. */
    fun widthPercent(context: Context): Int = sp(context).getInt("width", 94)

    fun setWidthPercent(context: Context, value: Int) {
        sp(context).edit().putInt("width", value).apply()
    }

    fun blockedApps(context: Context): Set<String> =
        sp(context).getStringSet("blocked_apps", emptySet())?.toSet() ?: emptySet()

    fun isAppAllowed(context: Context, pkg: String): Boolean =
        !blockedApps(context).contains(pkg)

    fun setAppAllowed(context: Context, pkg: String, allowed: Boolean) {
        val next = blockedApps(context).toMutableSet()
        if (allowed) next.remove(pkg) else next.add(pkg)
        sp(context).edit().putStringSet("blocked_apps", next).apply()
    }

    fun tickerDurationMs(context: Context): Long {
        val seconds = duration(context).coerceIn(3, 20)
        val speed = speed(context).coerceIn(0.5f, 3f)
        return (seconds * 1000L / speed).toLong()
    }

    /** Vertical offset of the floating ticker from the default position, in dp. */
    fun overlayOffsetY(context: Context): Int =
        sp(context).getInt("overlay_offset_y", 10).coerceIn(-20, 140)

    /** Body (message) text size of every theme, in sp. */
    fun fontSize(context: Context): Float =
        sp(context).getFloat("font_size", 13f).coerceIn(10f, 18f)

    fun setFontSize(context: Context, value: Float) {
        sp(context).edit().putFloat("font_size", value.coerceIn(10f, 18f)).apply()
    }

    fun setOverlayOffsetY(context: Context, value: Int) {
        sp(context).edit()
            .putInt("overlay_offset_y", value.coerceIn(-20, 140))
            .apply()
    }

    // ---- Customisable styles (System Window + Aurora) ----------------

    fun systemWindow(context: Context): SystemWindowOptions =
        readOptions(context, "sw_", 0xFF6FF6FF.toInt())

    fun saveSystemWindow(context: Context, value: SystemWindowOptions) =
        writeOptions(context, "sw_", value)

    fun aurora(context: Context): SystemWindowOptions =
        readOptions(context, "au_", 0xFF5CE8AE.toInt())

    fun saveAurora(context: Context, value: SystemWindowOptions) =
        writeOptions(context, "au_", value)

    fun saturdayMorning(context: Context): SystemWindowOptions =
        readOptions(
            context = context,
            prefix = "sm_",
            defaultAccent = 0xFF7A3BD9.toInt(),
            defaultSecondary = 0xFFFFCF3F.toInt(),
            defaultTertiary = 0xFFE8453C.toInt(),
            defaultAvatar = 44
        )

    fun saveSaturdayMorning(context: Context, value: SystemWindowOptions) =
        writeOptions(context, "sm_", value)

    fun yourImage(context: Context): SystemWindowOptions =
        readOptions(
            context = context,
            prefix = "yi_",
            defaultAccent = 0xFFFF7A1A.toInt(),
            defaultAvatar = 44
        )

    fun saveYourImage(context: Context, value: SystemWindowOptions) =
        writeOptions(context, "yi_", value)

    fun radioDial(context: Context): SystemWindowOptions =
        readOptions(
            context = context,
            prefix = "rd_",
            defaultAccent = 0xFFE8B33A.toInt(),
            defaultSecondary = 0xFFD8402C.toInt(),
            defaultTertiary = 0xFF8A5A2E.toInt(),
            defaultAvatar = 32
        )

    fun saveRadioDial(context: Context, value: SystemWindowOptions) =
        writeOptions(context, "rd_", value)

    fun aquatic(context: Context): SystemWindowOptions =
        readOptions(
            context = context,
            prefix = "aq_",
            defaultAccent = 0xFFB57BD9.toInt(),
            defaultSecondary = 0xFFFF7A5C.toInt(),
            defaultTertiary = 0xFF1E8C74.toInt(),
            defaultAvatar = 42
        )

    fun saveAquatic(context: Context, value: SystemWindowOptions) =
        writeOptions(context, "aq_", value)

    /** Options for one of the twenty illustrated card themes. */
    fun themeOptions(
        context: Context,
        id: String,
        defaultAccent: Int,
        defaultAvatar: Int = 22,
        /**
         * Themes that show a colour picker for their ink keep whatever the
         * user picked, even a pale one — the washed-out restore below would
         * otherwise undo the choice every time the card is drawn.
         */
        keepAnyAccent: Boolean = false
    ): SystemWindowOptions {
        val stored = readOptions(
            context = context,
            prefix = "card_${id}_",
            defaultAccent = defaultAccent,
            defaultAvatar = defaultAvatar
        )
        if (keepAnyAccent) return stored
        // The card themes keep their designed palette and have no colour picker,
        // so a light/white accent can only be a leftover from an older build.
        // Left as-is it painted the sender, the clock and the dock buttons white
        // instead of the theme's own accent, so restore the designed colour.
        if (!isWashedOut(stored.accentArgb)) return stored
        return stored.copy(accentArgb = defaultAccent)
    }

    /** Every designed card accent is far more saturated than this. */
    private fun isWashedOut(argb: Int): Boolean {
        val red = (argb shr 16) and 0xFF
        val green = (argb shr 8) and 0xFF
        val blue = argb and 0xFF
        return minOf(red, green, blue) >= 176
    }

    fun saveThemeOptions(context: Context, id: String, value: SystemWindowOptions) =
        writeOptions(context, "card_${id}_", value)

    private fun readOptions(
        context: Context,
        prefix: String,
        defaultAccent: Int,
        defaultSecondary: Int = 0,
        defaultTertiary: Int = 0,
        defaultAvatar: Int = 34
    ): SystemWindowOptions {
        val p = sp(context)
        // Older installs stored the first demo line; treat it as unset so the
        // new one takes over. Anything the user typed themselves is kept.
        val storedSender = p.getString("${prefix}sender", "") ?: ""
        val storedMessage = p.getString("${prefix}message", "") ?: ""
        val demoSender = storedSender.takeUnless { it.isBlank() || it == "Ava" } ?: "Mark"
        val demoMessage = storedMessage.takeUnless {
            it.isBlank() || it == "Dinner table booked for 8pm tonight! Running a little late, start without me?"
        } ?: ("Sir, your AI agent has successfully ordered the pizza. It ordered the large one " +
            "with extra cheese, paid with the card you left it, and the kitchen says " +
            "twenty minutes. Shall I set the table?")
        return SystemWindowOptions(
            accentArgb = p.getInt("${prefix}accent", defaultAccent),
            secondaryArgb = p.getInt("${prefix}secondary", defaultSecondary),
            tertiaryArgb = p.getInt("${prefix}tertiary", defaultTertiary),
            intensity = p.getInt("${prefix}intensity", 100).coerceIn(0, 100),
            avatarSize = p.getInt("${prefix}avatar", defaultAvatar).coerceIn(24, 48),
            showLogo = p.getBoolean("${prefix}logo", true),
            showTime = p.getBoolean("${prefix}time", true),
            lines = p.getInt("${prefix}lines", 2).coerceIn(0, 3),
            effect = p.getBoolean("${prefix}effect", true),
            previewKind = p.getString("${prefix}kind", "message") ?: "message",
            sender = demoSender,
            message = demoMessage,
            imagePath = p.getString("${prefix}image_path", "") ?: "",
            imageX = p.getInt("${prefix}image_x", 0).coerceIn(-10000, 10000),
            imageY = p.getInt("${prefix}image_y", 0).coerceIn(-10000, 10000),
            imageScale = p.getInt("${prefix}image_scale", 100).coerceIn(100, 10000),
            imageOpacity = p.getInt("${prefix}image_opacity", 80).coerceIn(0, 100),
            buttonScale = p.getInt("${prefix}button_scale", 100).coerceIn(70, 150)
        )
    }

    private fun writeOptions(context: Context, prefix: String, value: SystemWindowOptions) {
        sp(context).edit()
            .putInt("${prefix}accent", value.accentArgb)
            .putInt("${prefix}secondary", value.secondaryArgb)
            .putInt("${prefix}tertiary", value.tertiaryArgb)
            .putInt("${prefix}intensity", value.intensity.coerceIn(0, 100))
            .putInt("${prefix}avatar", value.avatarSize.coerceIn(24, 48))
            .putBoolean("${prefix}logo", value.showLogo)
            .putBoolean("${prefix}time", value.showTime)
            .putInt("${prefix}lines", value.lines.coerceIn(0, 3))
            .putBoolean("${prefix}effect", value.effect)
            .putString("${prefix}kind", value.previewKind)
            .putString("${prefix}sender", value.sender)
            .putString("${prefix}message", value.message)
            .putString("${prefix}image_path", value.imagePath)
            .putInt("${prefix}image_x", value.imageX.coerceIn(-10000, 10000))
            .putInt("${prefix}image_y", value.imageY.coerceIn(-10000, 10000))
            .putInt("${prefix}image_scale", value.imageScale.coerceIn(100, 10000))
            .putInt("${prefix}image_opacity", value.imageOpacity.coerceIn(0, 100))
            .putInt("${prefix}button_scale", value.buttonScale.coerceIn(70, 150))
            .apply()
    }
}
