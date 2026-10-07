package com.musicpauser

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: ImageView? = null
    private lateinit var layoutParams: WindowManager.LayoutParams
    private val mainHandler = Handler(Looper.getMainLooper())

    private val stateListener: (PlaybackManager.State) -> Unit = { s ->
        overlayView?.let { v ->
            val playing = s.canControl && s.isPlaying
            v.setBackgroundResource(
                when {
                    !s.canControl -> R.drawable.btn_round_disabled
                    playing -> R.drawable.btn_round_pause
                    else -> R.drawable.btn_round_play
                }
            )
            v.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        startForeground(NOTIF_ID, buildNotification())
        addOverlay()
        PlaybackManager.setContext(this)
        PlaybackManager.addListener(stateListener)
        if (intent?.getBooleanExtra(EXTRA_AUTOPLAY, false) == true) {
            scheduleAutoPlay()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        PlaybackManager.removeListener(stateListener)
        overlayView?.let { try { windowManager.removeView(it) } catch (_: Exception) { } }
        overlayView = null
        running = false
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val channelId = "overlay"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                channelId, getString(R.string.overlay_channel), NotificationManager.IMPORTANCE_MIN
            )
            nm.createNotificationChannel(channel)
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_play)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_running))
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun addOverlay() {
        if (overlayView != null) return
        val iv = ImageView(this)
        iv.setImageResource(R.drawable.ic_play)
        iv.setBackgroundResource(R.drawable.btn_round_play)
        iv.scaleType = ImageView.ScaleType.CENTER
        iv.alpha = 0.5f

        val size = dp(78)
        layoutParams = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.START
        val maxX = (resources.displayMetrics.widthPixels - size).coerceAtLeast(0)
        val maxY = (resources.displayMetrics.heightPixels - size).coerceAtLeast(0)
        val saved = Prefs.getOverlayPos(this)
        layoutParams.x = saved?.first?.coerceIn(0, maxX) ?: dp(8)
        layoutParams.y = saved?.second?.coerceIn(0, maxY) ?: dp(160)

        iv.setOnTouchListener(object : View.OnTouchListener {
            var initialX = 0
            var initialY = 0
            var touchX = 0f
            var touchY = 0f
            var moved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        touchX = event.rawX
                        touchY = event.rawY
                        moved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchX).toInt()
                        val dy = (event.rawY - touchY).toInt()
                        if (Math.abs(dx) > 8 || Math.abs(dy) > 8) moved = true
                        layoutParams.x = initialX + dx
                        layoutParams.y = initialY + dy
                        try {
                            windowManager.updateViewLayout(v, layoutParams)
                        } catch (_: Exception) {
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (moved) {
                            Prefs.setOverlayPos(this@OverlayService, layoutParams.x, layoutParams.y)
                        } else {
                            PlaybackManager.toggle(this@OverlayService)
                        }
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        if (moved) {
                            Prefs.setOverlayPos(this@OverlayService, layoutParams.x, layoutParams.y)
                        }
                        return true
                    }
                }
                return false
            }
        })

        iv.setOnLongClickListener {
            val intent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        }

        try {
            windowManager.addView(iv, layoutParams)
            overlayView = iv
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun scheduleAutoPlay() {
        if (!Prefs.isAutoPlay(this)) return
        if (!Prefs.isMusicActive(this)) return
        Prefs.setMusicActive(this, false)
        mainHandler.postDelayed({
            PlaybackManager.play(this)
        }, 2000)
    }

    companion object {
        private const val NOTIF_ID = 1001
        private const val EXTRA_AUTOPLAY = "extra_autoplay"

        @Volatile
        var running = false
            private set

        fun start(context: Context, autoPlay: Boolean = false) {
            running = true
            Prefs.setOverlayEnabled(context, true)
            val i = Intent(context, OverlayService::class.java)
                .putExtra(EXTRA_AUTOPLAY, autoPlay)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i)
            else context.startService(i)
        }

        fun stop(context: Context) {
            running = false
            Prefs.setOverlayEnabled(context, false)
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
