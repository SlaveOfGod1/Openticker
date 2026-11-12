package com.openticker.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import org.json.JSONObject

/**
 * Translations live in `assets/i18n/<language>.json`. Each file lists the
 * translation first and the English meaning after it, so a translator only
 * has to copy a line and put their own word in front:
 *
 * ```json
 * { "Estilos": "Styles", "Aplicaciones": "Apps" }
 * ```
 *
 * Adding a language is a matter of dropping a new file in that folder — no
 * code changes. The English file (`en.json`) is the reference list.
 */
object I18n {

    /** Every language the app ships with, plus the system default. */
    val languages = listOf(
        Language("system", "System default"),
        Language("en", "English"),
        Language("es", "Español"),
        Language("fr", "Français"),
        Language("de", "Deutsch"),
        Language("pt", "Português"),
        Language("ru", "Русский"),
        Language("zh", "简体中文"),
        Language("ja", "日本語"),
        Language("ko", "한국어"),
        Language("ar", "العربية"),
        Language("hi", "हिन्दी")
    )

    /** A language the app can speak. */
    data class Language(val tag: String, val nativeName: String)

    /**
     * Reads a language file and turns it into a meaning -> translation map.
     * Anything the file does not cover falls back to the English meaning.
     *
     * A file may list its entries either way round — the translation first
     * (`{ "Estilos": "Styles" }`) or the meaning first (`{ "Styles": "Estilos" }`,
     * the i18next style) — and both are understood.
     */
    fun load(context: Context, tag: String): Map<String, String> {
        if (tag == "system") return emptyMap()
        val meanings = englishMeanings(context)
        val raw = readAsset(context, "i18n/$tag.json") ?: return emptyMap()
        val out = HashMap<String, String>()
        runCatching {
            val json = JSONObject(raw)
            for (key in json.keys()) {
                val value = json.getString(key)
                if (value in meanings) {
                    // the translation comes first
                    out[value] = key
                } else {
                    // the meaning comes first
                    out[key] = value
                }
            }
        }
        return out
    }

    /** Every English meaning the app knows, taken from the reference file. */
    private fun englishMeanings(context: Context): Set<String> {
        val raw = readAsset(context, "i18n/en.json") ?: return emptySet()
        val set = HashSet<String>()
        runCatching {
            val json = JSONObject(raw)
            for (key in json.keys()) set.add(key)
        }
        return set
    }

    private fun readAsset(context: Context, path: String): String? = runCatching {
        context.assets.open(path).bufferedReader().use { it.readText() }
    }.getOrNull()

    /** The language code of the device, if we ship that language. */
    fun systemTag(context: Context): String {
        val code = context.resources.configuration.locales[0].language
        return if (languages.any { it.tag == code }) code else "en"
    }

    /** Languages that read right to left. */
    fun isRtl(tag: String): Boolean = tag in rtlLanguages

    private val rtlLanguages = setOf("ar", "he", "fa", "ur")
}

/** The strings for the language currently in use. */
val LocalStrings = androidx.compose.runtime.compositionLocalOf<Map<String, String>> { emptyMap() }

/**
 * The same lookup without needing a composable scope — for the few places
 * that run outside the UI, like a toast.
 */
object Loc {
    @Volatile
    var strings: Map<String, String> = emptyMap()

    fun t(meaning: String): String = strings[meaning] ?: meaning
}

/** The meaning, translated into the current language. */
@Composable
fun t(meaning: String): String {
    val strings = LocalStrings.current
    return strings[meaning] ?: meaning
}

/**
 * Wraps the app in the chosen language. The strings come from the JSON
 * files; the layout direction follows the language too, so Arabic reads
 * right to left.
 */
@Composable
fun ProvideStrings(tag: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    // "System default" follows the device; everything else is picked by hand.
    val effective = remember(tag) {
        if (tag == "system") I18n.systemTag(context) else tag
    }
    val strings = remember(effective) { I18n.load(context, effective) }
    SideEffect { Loc.strings = strings }
    CompositionLocalProvider(LocalStrings provides strings) {
        // Only a right-to-left language flips the layout. Every other
        // language keeps whatever direction the device already uses.
        if (I18n.isRtl(effective)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                content()
            }
        } else {
            content()
        }
    }
}
