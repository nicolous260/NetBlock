package com.example.netblock

import android.content.Context

/** Stores which apps are BLOCKED from internet access. */
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

    fun isBlocking(c: Context) = sp(c).getBoolean(KEY_BLOCKING, false)
    fun setBlocking(c: Context, on: Boolean) = sp(c).edit().putBoolean(KEY_BLOCKING, on).apply()

    fun packagesToBlock(c: Context): Set<String> {
        return blocked(c)
    }
}
