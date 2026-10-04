package com.shilapi.xcertplay

import android.view.Surface

/**
 * The surfaces that show a copy of the dashboard map (CarPlay stream 111) outside the dashboard,
 * currently the centre card ([CARD]). The CarPlay screen hands them to its media sink, now and after
 * every reconnect. Main thread.
 */
internal object MapMirrors {
    const val CARD = "card"

    /** The dashboard stream's shape (1920x720, sent scaled to 1600x600 by default). */
    const val STREAM_ASPECT = 8.0 / 3

    private val surfaces = LinkedHashMap<String, Surface>()

    /** Set by the CarPlay screen: applies one mirror to its current media sink. */
    var sink: ((String, Surface?) -> Unit)? = null

    /** The CarPlay screen has a new media sink: give it every mirror. */
    fun reapply() {
        val apply = sink ?: return
        surfaces.forEach { (key, surface) -> apply(key, surface) }
    }

    fun set(key: String, surface: Surface?) {
        if (surface == null) {
            if (surfaces.remove(key) == null) return
        } else {
            surfaces[key] = surface
        }
        sink?.invoke(key, surface)
    }
}
