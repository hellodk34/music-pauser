package com.musicpauser

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class PlayPauseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        PlaybackManager.setContext(context)
        PlaybackManager.recompute(context)
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, PlaybackManager.buildRemoteViews(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE -> PlaybackManager.toggle(context)
            ACTION_PREV -> PlaybackManager.previous(context)
            ACTION_NEXT -> PlaybackManager.next(context)
            else -> super.onReceive(context, intent)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.musicpauser.widget.TOGGLE"
        const val ACTION_PREV = "com.musicpauser.widget.PREV"
        const val ACTION_NEXT = "com.musicpauser.widget.NEXT"
    }
}
