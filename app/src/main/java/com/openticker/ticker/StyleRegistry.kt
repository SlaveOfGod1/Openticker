package com.openticker.ticker

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.openticker.data.TickerData
import com.openticker.data.TickerInteractions
import com.openticker.ticker.cards.CuratedArcadeTicker
import com.openticker.ticker.cards.CuratedBalloonTicker
import com.openticker.ticker.cards.CuratedBambooTicker
import com.openticker.ticker.cards.CuratedBentoTicker
import com.openticker.ticker.cards.CuratedBlueprintTicker
import com.openticker.ticker.cards.CuratedCrystalTicker
import com.openticker.ticker.cards.CuratedFlapTicker
import com.openticker.ticker.cards.CuratedFrostTicker
import com.openticker.ticker.cards.CuratedGlassTicker
import com.openticker.ticker.cards.CuratedLavaTicker
import com.openticker.ticker.cards.CuratedMapTicker
import com.openticker.ticker.cards.CuratedMarqueeTicker
import com.openticker.ticker.cards.CuratedOrigamiTicker
import com.openticker.ticker.cards.CuratedPawsTicker
import com.openticker.ticker.cards.CuratedPolaroidTicker
import com.openticker.ticker.cards.CuratedStickyTicker
import com.openticker.ticker.cards.CuratedStormTicker
import com.openticker.ticker.cards.CuratedTerminalTicker
import com.openticker.ticker.cards.CuratedTerrariumTicker
import com.openticker.ticker.cards.CuratedVaporTicker
import com.openticker.ticker.cards.CuratedVinylTicker
import com.openticker.ticker.cards.CuratedZenTicker
import com.openticker.ticker.styles.CuratedAquaticTicker
import com.openticker.ticker.styles.CuratedAuroraTicker
import com.openticker.ticker.styles.CuratedRadioDialTicker
import com.openticker.ticker.styles.CuratedSaturdayMorningTicker
import com.openticker.ticker.styles.CuratedSystemWindowTicker
import com.openticker.ticker.styles.CuratedYourImageTicker

data class TickerStyle(
    val id: String,
    val name: String,
    val description: String,
    val accent: Color,
    val icon: ImageVector,
    /** Styles with `selfIntro` run their own entrance choreography. */
    val selfIntro: Boolean = false,
    /** The style animates its own dismissal (no shared exit transform). */
    val internalExit: Boolean = false,
    val content: @Composable (TickerData, TickerInteractions?, Boolean, () -> Float) -> Unit
)

/**
 * Curated catalog. Only these six styles are enabled while we polish them;
 * the remaining implementations stay in `ticker/styles/` but are not listed.
 */
object StyleRegistry {

    private fun style(
        id: String,
        name: String,
        description: String,
        accent: Color,
        icon: ImageVector,
        selfIntro: Boolean = false,
        internalExit: Boolean = false,
        content: @Composable (TickerData, TickerInteractions?, Boolean, () -> Float) -> Unit
    ) = TickerStyle(id, name, description, accent, icon, selfIntro, internalExit, content)

    val styles: List<TickerStyle> = listOf(
        style(
            id = "system_window",
            name = "System Window",
            description = "The blue bordered system window: corner brackets, scanlines and a NOTIFICATION strip.",
            accent = Color(0xFF8FB7FF),
            icon = Icons.Filled.Visibility,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedSystemWindowTicker(data, actions, animate, exit) },
        style(
            id = "aurora",
            name = "Aurora",
            description = "Green and teal light curtains breathing over a polar night sky.",
            accent = Color(0xFF5CE8AE),
            icon = Icons.Filled.NightsStay,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedAuroraTicker(data, actions, animate, exit) },
        style(
            id = "saturday_morning",
            name = "Saturday Morning",
            description = "Cartoon yellow, purple bubbles and bouncing dots. Cereal-notification energy.",
            accent = Color(0xFFFFCF3F),
            icon = Icons.Filled.Celebration,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedSaturdayMorningTicker(data, actions, animate, exit) },
        style(
            id = "radio_dial",
            name = "Radio Dial",
            description = "Vintage wooden radio: mesh screen, red tuning needle and gold knob rings.",
            accent = Color(0xFFE8B33A),
            icon = Icons.Filled.Radio,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedRadioDialTicker(data, actions, animate, exit) },
        style(
            id = "aquatic",
            name = "Aquatic",
            description = "Open coral reef: roaming fish, a peeking octopus, god rays and endless bubbles.",
            accent = Color(0xFF8FE8EE),
            icon = Icons.Filled.Waves,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedAquaticTicker(data, actions, animate, exit) },
        style(
            id = "your_image",
            name = "Your Image",
            description = "Use any photo as the notification background, with position and scale controls.",
            accent = Color(0xFFFF7A1A),
            icon = Icons.Filled.Image
        ) { data, actions, _, _ -> CuratedYourImageTicker(data, actions) },
        style(
            id = "arcade",
            name = "Neon Arcade",
            description = "Coin-op cabinet on midnight violet: pixel scanlines, twinkling stars and a flipping coin.",
            accent = Color(0xFF7DF9FF),
            icon = Icons.Filled.SportsEsports,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedArcadeTicker(data, actions, animate, exit) },
        style(
            id = "flap",
            name = "Split-Flap",
            description = "Amber airport departures board with seam lines, flap cells and a blinking status LED.",
            accent = Color(0xFFFFB627),
            icon = Icons.Filled.ConfirmationNumber,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedFlapTicker(data, actions, animate, exit) },
        style(
            id = "vinyl",
            name = "Vinyl Turntable",
            description = "Walnut-plinth deck with a spinning record and a tonearm that drops onto it.",
            accent = Color(0xFFFF8A5C),
            icon = Icons.Filled.Album,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedVinylTicker(data, actions, animate, exit) },
        style(
            id = "polaroid",
            name = "Polaroid",
            description = "Tilted instant-photo frame taped to a wall; the shot ejects and develops before your eyes.",
            accent = Color(0xFFC0653E),
            icon = Icons.Filled.PhotoCamera,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedPolaroidTicker(data, actions, animate, exit) },
        style(
            id = "sticky",
            name = "Corkboard Sticky",
            description = "Speckled corkboard with a slap-on yellow lined note, push pin and two mini notes.",
            accent = Color(0xFFC0392B),
            icon = Icons.Filled.PushPin,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedStickyTicker(data, actions, animate, exit) },
        style(
            id = "terminal",
            name = "Green Terminal",
            description = "Phosphor CRT shell session: scanlines, drifting refresh band and a blinking prompt.",
            accent = Color(0xFF7CFF9B),
            icon = Icons.Filled.Terminal,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedTerminalTicker(data, actions, animate, exit) },
        style(
            id = "blueprint",
            name = "Blueprint",
            description = "Prussian-blue inspection sheet with a binding strip, ruler, bearing detail and a rolled-sheet reveal.",
            accent = Color(0xFF9FD0FF),
            icon = Icons.Filled.Architecture,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedBlueprintTicker(data, actions, animate, exit) },
        style(
            id = "origami",
            name = "Origami",
            description = "White paper folded into a triangle: it opens out to the left and the right, then every piece shivers like stop-motion paper.",
            accent = Color(0xFF14161A),
            icon = Icons.Filled.Send,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedOrigamiTicker(data, actions, animate, exit) },
        style(
            id = "glass",
            name = "Stained Glass",
            description = "One thick pane of leaded glass: the note is sandblasted into 182 jewel panes that dissolve away on exit.",
            accent = Color(0xFFD9B45B),
            icon = Icons.Filled.Diamond,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedGlassTicker(data, actions, animate, exit) },
        style(
            id = "lava",
            name = "Lava Lamp",
            description = "A lava lamp on the desk beside the note: ribbed brass foot, chrome collars and wax that climbs, lingers and drops.",
            accent = Color(0xFFFFB36B),
            icon = Icons.Filled.Whatshot,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedLavaTicker(data, actions, animate, exit) },
        style(
            id = "vapor",
            name = "Vaporwave",
            description = "A synthwave sunset: slatted sun, wireframe peaks and a grid floor rushing forward, with a chromatic glitch before the note lands.",
            accent = Color(0xFFFF71CE),
            icon = Icons.Filled.Videocam,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedVaporTicker(data, actions, animate, exit) },
        style(
            id = "crystal",
            name = "Crystal Ball",
            description = "A velvet room that wakes: stars ignite, moonlight sweeps, mist gathers and a leaded orb grows with the app icon burning as the flame inside.",
            accent = Color(0xFFC9A227),
            icon = Icons.Filled.AutoAwesome,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedCrystalTicker(data, actions, animate, exit) },
        style(
            id = "map",
            name = "Treasure Map",
            description = "Aged parchment with fold creases, a wobbling compass and a dotted route marching to a red X.",
            accent = Color(0xFFA9402E),
            icon = Icons.Filled.Map,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedMapTicker(data, actions, animate, exit) },
        style(
            id = "terrarium",
            name = "Terrarium",
            description = "A bell jar on a lamplit workbench: the jar drops in and seats itself, then moss spreads, a fern unfurls and glowing mushrooms wake inside.",
            accent = Color(0xFF4C8A5A),
            icon = Icons.Filled.Eco,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedTerrariumTicker(data, actions, animate, exit) },
        style(
            id = "frost",
            name = "Frost Pane",
            description = "A winter night outside the house: spruces with little stoves burning on them, three layers of snow and buttons carved from packed ice.",
            accent = Color(0xFF3A6E94),
            icon = Icons.Filled.AcUnit,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedFrostTicker(data, actions, animate, exit) },
        style(
            id = "storm",
            name = "Thunderstorm",
            description = "A storm over the moor at night: cloud bank, driving rain, hills, bending reeds and a bolt that splits the sky.",
            accent = Color(0xFFFFD84D),
            icon = Icons.Filled.Thunderstorm,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedStormTicker(data, actions, animate, exit) },
        style(
            id = "zen",
            name = "Zen Garden",
            description = "Raked sand circles around a smooth stone, swaying bamboo and scattered pebbles.",
            accent = Color(0xFF8A8272),
            icon = Icons.Filled.Spa,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedZenTicker(data, actions, animate, exit) },
        style(
            id = "bento",
            name = "Bento Box",
            description = "The note served in a black lacquer bento — the box is set down from above, the lid slides off and every compartment fills.",
            accent = Color(0xFFC9A227),
            icon = Icons.Filled.Restaurant,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedBentoTicker(data, actions, animate, exit) },
        style(
            id = "balloon",
            name = "Balloon Party",
            description = "Pastel sky with swaying bunting, drifting confetti and bobbing balloons on strings.",
            accent = Color(0xFFE8788F),
            icon = Icons.Filled.Celebration,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedBalloonTicker(data, actions, animate, exit) },
        style(
            id = "marquee",
            name = "Carnival Marquee",
            description = "Red velvet theatre with gold trim, chasing bulbs, a spotlight and parting curtains.",
            accent = Color(0xFFE8BC4E),
            icon = Icons.Filled.TheaterComedy,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedMarqueeTicker(data, actions, animate, exit) },
        style(
            id = "paws",
            name = "Cat Paws",
            description = "Crisp white card with a pink rim: a ball of yarn drops and unrolls, paw prints sprint across and wool-ball buttons pop in.",
            accent = Color(0xFFF27FA5),
            icon = Icons.Filled.Pets,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedPawsTicker(data, actions, animate, exit) },
        style(
            id = "bamboo",
            name = "Bamboo",
            description = "A grove of thin canes: the rods fall and plant themselves one by one, the green field fills in and three leaf buttons sprout.",
            accent = Color(0xFFDFF3B2),
            icon = Icons.Filled.Eco,
            selfIntro = true,
            internalExit = true
        ) { data, actions, animate, exit -> CuratedBambooTicker(data, actions, animate, exit) }
    )

    fun byId(id: String): TickerStyle = styles.firstOrNull { it.id == id } ?: styles.first()
}
