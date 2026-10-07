package com.musicpauser

import android.app.Notification
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import android.view.KeyEvent
import android.widget.RemoteViews
import java.util.concurrent.CopyOnWriteArrayList

object PlaybackManager {

    private enum class ActionType { PLAY, PAUSE, PLAY_PAUSE, PREV, NEXT, OTHER }

    private const val SEMANTIC_PLAY = 12
    private const val SEMANTIC_PAUSE = 13
    private const val SEMANTIC_PLAY_PAUSE = 14
    private const val SEMANTIC_NEXT = 15
    private const val SEMANTIC_PREVIOUS = 16

    data class State(
        val hasSession: Boolean = false,
        val canControl: Boolean = false,
        val appPackage: String? = null,
        val appName: String? = null,
        val title: String? = null,
        val artist: String? = null,
        val isPlaying: Boolean = false,
        val canPrev: Boolean = false,
        val canNext: Boolean = false,
        val musicActive: Boolean = false
    )

    private data class MediaInfo(
        val appPackage: String,
        val lastUpdated: Long,
        var isPlaying: Boolean,
        val title: String?,
        val artist: String?,
        val playIntent: PendingIntent?,
        val pauseIntent: PendingIntent?,
        val toggleIntent: PendingIntent?,
        val prevIntent: PendingIntent?,
        val nextIntent: PendingIntent?
    )

    private val mediaMap = LinkedHashMap<String, MediaInfo>()
    private val listeners = CopyOnWriteArrayList<(State) -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var appContext: Context? = null
    private var mediaKeyPlaying: Boolean? = null
    private var playbackCallback: AudioManager.AudioPlaybackCallback? = null

    @Volatile
    var state: State = State()
        private set

    fun setContext(ctx: Context) {
        if (appContext == null) appContext = ctx.applicationContext
        registerPlaybackCallback(ctx.applicationContext)
    }

    fun addListener(l: (State) -> Unit) {
        listeners.add(l)
        l.invoke(state)
    }

    fun removeListener(l: (State) -> Unit) {
        listeners.remove(l)
    }

    fun clear() {
        mediaMap.clear()
    }

    fun onNotificationPosted(context: Context, sbn: StatusBarNotification) {
        setContext(context)
        val n = sbn.notification ?: return
        if (!isMediaNotification(n)) return

        var play: PendingIntent? = null
        var pause: PendingIntent? = null
        var toggle: PendingIntent? = null
        var prev: PendingIntent? = null
        var next: PendingIntent? = null
        val actions = n.actions ?: return
        for (a in actions) {
            when (classify(a)) {
                ActionType.PLAY -> if (play == null) play = a.actionIntent
                ActionType.PAUSE -> if (pause == null) pause = a.actionIntent
                ActionType.PLAY_PAUSE -> if (toggle == null) toggle = a.actionIntent
                ActionType.PREV -> if (prev == null) prev = a.actionIntent
                ActionType.NEXT -> if (next == null) next = a.actionIntent
                ActionType.OTHER -> Unit
            }
        }
        if (play == null && pause == null && toggle == null) return

        val playing = when {
            pause != null -> true
            play != null -> false
            else -> isMusicActive(context)
        }

        val extras = n.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
        var artist = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
        if (artist == null) {
            artist = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() }
        }

        mediaMap[sbn.packageName] = MediaInfo(
            sbn.packageName, System.currentTimeMillis(), playing,
            title, artist, play, pause, toggle, prev, next
        )
        recompute(context)
    }

    fun onNotificationRemoved(context: Context, sbn: StatusBarNotification) {
        setContext(context)
        mediaMap.remove(sbn.packageName)
        recompute(context)
    }

    fun recompute(context: Context) {
        setContext(context)
        val entries = mediaMap.values
        val s = when {
            entries.isNotEmpty() -> {
                val playingEntry = entries.filter { it.isPlaying }.maxByOrNull { it.lastUpdated }
                val active = playingEntry ?: entries.maxByOrNull { it.lastUpdated }!!
                State(
                    hasSession = true,
                    canControl = true,
                    appPackage = active.appPackage,
                    appName = friendlyName(active.appPackage),
                    title = active.title,
                    artist = active.artist,
                    isPlaying = active.isPlaying,
                    canPrev = active.prevIntent != null,
                    canNext = active.nextIntent != null,
                    musicActive = isMusicActive(context)
                )
            }
            mediaKeyAvailable() -> {
                val playing = mediaKeyPlaying ?: isActuallyPlaying(context)
                    .also { mediaKeyPlaying = it }
                persistMusicActive(context, playing)
                State(
                    hasSession = false,
                    canControl = true,
                    isPlaying = playing,
                    canPrev = true,
                    canNext = true
                )
            }
            else -> {
                State(hasSession = false, canControl = false, musicActive = isMusicActive(context))
            }
        }
        state = s
        notifyListeners(s)
        updateWidgets()
    }

    fun toggle(context: Context) {
        setContext(context)
        val s = state
        if (!effectiveCanControl(s)) return
        if (s.hasSession) {
            val info = mediaMap[s.appPackage] ?: return
            val target = if (s.isPlaying) info.pauseIntent ?: info.toggleIntent
                         else info.playIntent ?: info.toggleIntent
            if (target != null) {
                try {
                    target.send()
                } catch (e: Exception) {
                    sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    return
                }
            } else {
                sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }
            info.isPlaying = !s.isPlaying
            recompute(context)
        } else {
            sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            mediaKeyPlaying = !(mediaKeyPlaying ?: isActuallyPlaying(context))
            recompute(context)
            scheduleStateSync()
        }
    }

    fun previous(context: Context) {
        setContext(context)
        val s = state
        if (!effectiveCanControl(s)) return
        if (s.hasSession) {
            val info = mediaMap[s.appPackage] ?: return
            val intent = info.prevIntent
            if (intent != null) {
                try {
                    intent.send()
                    return
                } catch (_: Exception) {
                }
            }
        }
        sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        mediaKeyPlaying = true
        recompute(context)
        scheduleStateSync()
    }

    fun next(context: Context) {
        setContext(context)
        val s = state
        if (!effectiveCanControl(s)) return
        if (s.hasSession) {
            val info = mediaMap[s.appPackage] ?: return
            val intent = info.nextIntent
            if (intent != null) {
                try {
                    intent.send()
                    return
                } catch (_: Exception) {
                }
            }
        }
        sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
        mediaKeyPlaying = true
        recompute(context)
        scheduleStateSync()
    }

    fun play(context: Context) {
        setContext(context)
        if (Build.VERSION.SDK_INT >= 30) return
        sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY)
        mediaKeyPlaying = true
        recompute(context)
        scheduleStateSync()
    }

    private fun persistMusicActive(context: Context, playing: Boolean) {
        if (playing) {
            Prefs.setMusicActive(context, true)
        }
    }

    private fun effectiveCanControl(s: State): Boolean = s.canControl || mediaKeyAvailable()

    private fun mediaKeyAvailable(): Boolean = Build.VERSION.SDK_INT < 30

    private val syncRunnable = Runnable {
        val ctx = appContext ?: return@Runnable
        mediaKeyPlaying = isActuallyPlaying(ctx)
        recompute(ctx)
    }

    private fun scheduleStateSync() {
        mainHandler.removeCallbacks(syncRunnable)
        mainHandler.postDelayed(syncRunnable, 500)
    }

    private fun registerPlaybackCallback(context: Context) {
        if (playbackCallback != null) return
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val cb = object : AudioManager.AudioPlaybackCallback() {
                override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
                    if (mediaMap.isNotEmpty()) return
                    val ctx = appContext ?: return
                    mediaKeyPlaying = configs.isNotEmpty()
                    recompute(ctx)
                }
            }
            am.registerAudioPlaybackCallback(cb, mainHandler)
            playbackCallback = cb
        } catch (_: Exception) {
        }
    }

    private fun sendMediaKey(context: Context, keyCode: Int) {
        if (Build.VERSION.SDK_INT >= 30) return
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val down = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val up = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            val m = AudioManager::class.java.getMethod("dispatchMediaKeyEvent", KeyEvent::class.java)
            m.invoke(am, down)
            m.invoke(am, up)
        } catch (_: Exception) {
        }
    }

    private fun isMusicActive(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.isMusicActive
        } catch (_: Exception) {
            false
        }
    }

    private fun isActuallyPlaying(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.activePlaybackConfigurations?.isNotEmpty() == true
        } catch (_: Exception) {
            false
        }
    }

    private fun classify(a: Notification.Action): ActionType {
        if (Build.VERSION.SDK_INT >= 28) {
            when (a.semanticAction) {
                SEMANTIC_PLAY -> return ActionType.PLAY
                SEMANTIC_PAUSE -> return ActionType.PAUSE
                SEMANTIC_PLAY_PAUSE -> return ActionType.PLAY_PAUSE
                SEMANTIC_PREVIOUS -> return ActionType.PREV
                SEMANTIC_NEXT -> return ActionType.NEXT
            }
        }
        val title = a.title?.toString()?.lowercase() ?: ""
        if (title.contains("previous") || title.contains("上一首") || title.contains("prev")) {
            return ActionType.PREV
        }
        if (title.contains("下一首") || title.contains("next")) {
            return ActionType.NEXT
        }
        val hasPlay = title.contains("play") || title.contains("播放")
        val hasPause = title.contains("pause") || title.contains("暂停")
        return when {
            hasPlay && hasPause -> ActionType.PLAY_PAUSE
            hasPause -> ActionType.PAUSE
            hasPlay -> ActionType.PLAY
            else -> ActionType.OTHER
        }
    }

    private fun isMediaNotification(n: Notification): Boolean {
        if (n.category == Notification.CATEGORY_TRANSPORT) return true
        if (n.extras != null && n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return true
        val actions = n.actions ?: return false
        for (a in actions) {
            if (classify(a) != ActionType.OTHER) return true
        }
        return false
    }

    private fun notifyListeners(s: State) {
        mainHandler.post { for (l in listeners) l(s) }
    }

    private fun updateWidgets() {
        val ctx = appContext ?: return
        val awm = AppWidgetManager.getInstance(ctx)
        val ids = awm.getAppWidgetIds(ComponentName(ctx, PlayPauseWidgetProvider::class.java))
        if (ids.isEmpty()) return
        awm.updateAppWidget(ids, buildRemoteViews(ctx))
    }

    fun buildRemoteViews(context: Context): RemoteViews {
        val s = state
        val views = RemoteViews(context.packageName, R.layout.widget_play_pause)

        val has = s.hasSession
        val canControl = effectiveCanControl(s)
        val titleText: String?
        val artistText: String?
        when {
            has -> {
                titleText = s.title ?: s.appName
                artistText = s.artist ?: context.getString(
                    if (s.isPlaying) R.string.playing else R.string.paused
                )
            }
            canControl -> {
                titleText = context.getString(R.string.compat_mode_short)
                artistText = context.getString(R.string.compat_toggle_hint)
            }
            else -> {
                titleText = context.getString(R.string.not_playing)
                artistText = ""
            }
        }
        views.setTextViewText(R.id.widget_title, titleText)
        views.setTextViewText(R.id.widget_artist, artistText)

        val bgRes = when {
            !canControl -> R.drawable.btn_round_disabled
            s.isPlaying -> R.drawable.btn_round_pause
            else -> R.drawable.btn_round_play
        }
        val iconRes = if (canControl && s.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        views.setInt(R.id.widget_play, "setBackgroundResource", bgRes)
        views.setImageViewResource(R.id.widget_play, iconRes)
        views.setInt(R.id.widget_play, "setAlpha", if (canControl) 255 else 120)

        val prevOn = if (has) s.canPrev else canControl
        val nextOn = if (has) s.canNext else canControl
        views.setInt(R.id.widget_prev, "setAlpha", if (prevOn) 255 else 100)
        views.setInt(R.id.widget_next, "setAlpha", if (nextOn) 255 else 100)

        val togglePi = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, PlayPauseWidgetProvider::class.java)
                .setAction(PlayPauseWidgetProvider.ACTION_TOGGLE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, togglePi)
        views.setOnClickPendingIntent(R.id.widget_title, togglePi)
        views.setOnClickPendingIntent(R.id.widget_play, togglePi)

        val prevPi = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, PlayPauseWidgetProvider::class.java)
                .setAction(PlayPauseWidgetProvider.ACTION_PREV),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_prev, prevPi)

        val nextPi = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, PlayPauseWidgetProvider::class.java)
                .setAction(PlayPauseWidgetProvider.ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_next, nextPi)

        return views
    }

    private fun friendlyName(pkg: String): String {
        return when (pkg) {
            "com.spotify.music" -> "Spotify"
            "com.tencent.qqmusic" -> "QQ音乐"
            "com.tencent.qqmusiclite" -> "QQ音乐极速版"
            "com.netease.cloudmusic" -> "网易云音乐"
            "cn.kuwo.player" -> "酷我音乐"
            "com.kugou.android" -> "酷狗音乐"
            "com.kugou.android.auto" -> "酷狗音乐车机版"
            "com.apple.android.music" -> "Apple Music"
            "com.google.android.apps.youtube.music" -> "YouTube Music"
            "com.google.android.music" -> "Google Play 音乐"
            "com.miui.player" -> "小米音乐"
            "com.android.mediacenter" -> "音乐"
            "com.byd.media" -> "比亚迪音乐"
            "com.byd.mediacenter" -> "比亚迪音乐"
            "com.samsung.android.music" -> "三星音乐"
            "com.deezer.android.app" -> "Deezer"
            "com.amazon.mp3" -> "Amazon Music"
            "com.soundcloud.android" -> "SoundCloud"
            "com.shazam.android" -> "Shazam"
            "com.bilibili.app.in" -> "哔哩哔哩"
            else -> pkg
        }
    }
}
