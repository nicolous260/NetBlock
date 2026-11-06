package io.github.nicolous260.netblock

import android.content.Context
import android.content.SharedPreferences

/** Stores which apps are BLOCKED from internet access, and whether blocking is switched on. */
object Prefs {
    private const val FILE = "netblock"
    private const val KEY_BLOCKED = "blocked"
    private const val KEY_BLOCKING = "blocking"

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun blocked(c: Context): Set<String> = sp(c).getStringSet(KEY_BLOCKED, emptySet()) ?: emptySet()

    fun setBlocked(c: Context, pkg: String, block: Boolean) {
        val set = blocked(c).toMutableSet()
        if (block) set.add(pkg) else set.remove(pkg)
        sp(c).edit().putStringSet(KEY_BLOCKED, set).apply()
    }

    /** True when the user wants blocking on. Also used to restore blocking after a reboot. */
    fun isBlocking(c: Context) = sp(c).getBoolean(KEY_BLOCKING, false)
    fun setBlocking(c: Context, on: Boolean) = sp(c).edit().putBoolean(KEY_BLOCKING, on).apply()

    /**
     * Calls [onChange] on the main thread whenever a stored value changes.
     * Returns a function that unregisters the listener.
     */
    fun observe(c: Context, onChange: () -> Unit): () -> Unit {
        val prefs = sp(c)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> onChange() }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        // The returned lambda keeps a strong reference; SharedPreferences only holds listeners weakly.
        return { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
