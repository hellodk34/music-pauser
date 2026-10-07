package com.musicpauser

import android.content.Context

object Prefs {
    private const val FILE = "music_pauser_prefs"
    private const val KEY_OVERLAY = "overlay_enabled"
    private const val KEY_AUTO_PLAY = "auto_play"
    private const val KEY_MUSIC_ACTIVE = "music_active"
    private const val KEY_OVERLAY_X = "overlay_x"
    private const val KEY_OVERLAY_Y = "overlay_y"

    fun setOverlayEnabled(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_OVERLAY, enabled).apply()
    }

    fun isOverlayEnabled(ctx: Context): Boolean {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_OVERLAY, false)
    }

    fun setAutoPlay(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_PLAY, enabled).apply()
    }

    fun isAutoPlay(ctx: Context): Boolean {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_PLAY, false)
    }

    fun setMusicActive(ctx: Context, active: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_MUSIC_ACTIVE, active).apply()
    }

    fun isMusicActive(ctx: Context): Boolean {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_MUSIC_ACTIVE, false)
    }

    fun setOverlayPos(ctx: Context, x: Int, y: Int) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_OVERLAY_X, x).putInt(KEY_OVERLAY_Y, y).apply()
    }

    fun getOverlayPos(ctx: Context): Pair<Int, Int>? {
        val sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (!sp.contains(KEY_OVERLAY_X) || !sp.contains(KEY_OVERLAY_Y)) return null
        return Pair(sp.getInt(KEY_OVERLAY_X, 0), sp.getInt(KEY_OVERLAY_Y, 0))
    }
}
