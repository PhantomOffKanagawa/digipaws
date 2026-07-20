package neth.iecal.curbox.hardcoded

import neth.iecal.curbox.R

/**
 * The pool of ASCII arts shown at random on the usage page. Each entry has a stable [key]
 * (persisted in Settings when the user hides it), a [labelRes] for the toggle in the Info
 * screen, and the [artRes] that is actually drawn. Keys must never change once shipped, or a
 * user's hidden set would silently point at the wrong art.
 */
object AsciiArts {

    data class AsciiArt(val key: String, val labelRes: Int, val artRes: Int)

    val POOL: List<AsciiArt> = listOf(
        AsciiArt("brain", R.string.ascii_label_brain, R.string.ascii_brain),
        AsciiArt("aim", R.string.ascii_label_aim, R.string.ascii_aim),
        AsciiArt("star1", R.string.ascii_label_star1, R.string.ascii_star1),
        AsciiArt("star2", R.string.ascii_label_star2, R.string.ascii_star2),
        AsciiArt("kitty", R.string.ascii_label_kitty, R.string.ascii_kitty),
        AsciiArt("star3", R.string.ascii_label_star3, R.string.ascii_star3),
        AsciiArt("star4", R.string.ascii_label_star4, R.string.ascii_star4),
        AsciiArt("star5", R.string.ascii_label_star5, R.string.ascii_star5),
        AsciiArt("coolstars", R.string.ascii_label_coolstars, R.string.ascii_coolstars),
        AsciiArt("coolflower", R.string.ascii_label_coolflower, R.string.ascii_coolflower),
        AsciiArt("chillguy", R.string.ascii_label_chillguy, R.string.ascii_chillguy),
        AsciiArt("god", R.string.ascii_label_god, R.string.ascii_god),
        AsciiArt("jellyfish", R.string.ascii_label_jellyfish, R.string.ascii_jellyfish),
        AsciiArt("lotus", R.string.ascii_label_lotus, R.string.ascii_lotus),
        AsciiArt("sharks", R.string.ascii_label_sharks, R.string.ascii_sharks),
    )

    /**
     * The arts still allowed after removing the user's hidden [disabledKeys]. Falls back to the
     * full pool when every art is hidden, so the usage page never has nothing to show.
     */
    fun enabled(disabledKeys: Collection<String>): List<AsciiArt> {
        val allowed = POOL.filter { it.key !in disabledKeys }
        return allowed.ifEmpty { POOL }
    }
}
