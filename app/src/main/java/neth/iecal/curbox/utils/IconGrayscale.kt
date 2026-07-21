package neth.iecal.curbox.utils

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * App wide "gray the app icons" preference. A single cached flag so every icon bind site can
 * apply the filter cheaply without each screen wiring up its own DataStore read. The flag is
 * kept fresh by [start], called once from the Application in the main process.
 */
object IconGrayscale {

    private val FILTER = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })

    @Volatile
    var enabled = false
        private set

    /** Begins observing the setting so [enabled] tracks it for the process lifetime. */
    fun start(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            DataStoreManager(appContext).settings
                .map { it.appIconsGrayscale }
                .distinctUntilChanged()
                .collect { enabled = it }
        }
    }

    /** Grays [view]'s icon when the preference is on, restores full color when off. */
    fun apply(view: ImageView) {
        view.colorFilter = if (enabled) FILTER else null
    }
}
