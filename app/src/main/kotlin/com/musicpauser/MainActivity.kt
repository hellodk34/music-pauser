package com.musicpauser

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var sourceText: TextView
    private lateinit var prevButton: ImageButton
    private lateinit var toggleButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var banner: View
    private lateinit var bannerText: TextView
    private lateinit var enableButton: Button
    private lateinit var overlayButton: Button
    private lateinit var autoPlaySwitch: Switch

    private val listener: (PlaybackManager.State) -> Unit = { s ->
        runOnUiThread { render(s) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        songTitle = findViewById(R.id.song_title)
        songArtist = findViewById(R.id.song_artist)
        sourceText = findViewById(R.id.music_source)
        prevButton = findViewById(R.id.btn_prev)
        toggleButton = findViewById(R.id.btn_toggle)
        nextButton = findViewById(R.id.btn_next)
        banner = findViewById(R.id.banner)
        bannerText = findViewById(R.id.banner_text)
        enableButton = findViewById(R.id.btn_enable)
        overlayButton = findViewById(R.id.btn_overlay)
        autoPlaySwitch = findViewById(R.id.switch_autoplay)

        PlaybackManager.setContext(this)

        prevButton.setOnClickListener { PlaybackManager.previous(this) }
        toggleButton.setOnClickListener { PlaybackManager.toggle(this) }
        nextButton.setOnClickListener { PlaybackManager.next(this) }
        enableButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        overlayButton.setOnClickListener { toggleOverlay() }
        autoPlaySwitch.setOnCheckedChangeListener { _, checked ->
            Prefs.setAutoPlay(this, checked)
        }
        autoPlaySwitch.isChecked = Prefs.isAutoPlay(this)
    }

    override fun onResume() {
        super.onResume()
        PlaybackManager.addListener(listener)
        updateBanner()
        updateOverlayButton()
        PlaybackManager.recompute(this)
    }

    override fun onPause() {
        super.onPause()
        PlaybackManager.removeListener(listener)
    }

    private fun updateBanner() {
        if (isNotificationAccessEnabled()) {
            banner.visibility = View.GONE
        } else {
            banner.visibility = View.VISIBLE
            val onOldDevice = Build.VERSION.SDK_INT < 30
            bannerText.text = getString(
                if (onOldDevice) R.string.compat_mode_title else R.string.notification_hint
            )
            enableButton.visibility = if (onOldDevice) View.GONE else View.VISIBLE
        }
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val cn = ComponentName(this, MusicNotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.split(":")?.contains(cn.flattenToString()) == true
    }

    private fun toggleOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }
        if (OverlayService.running) {
            OverlayService.stop(this)
        } else {
            OverlayService.start(this)
        }
        updateOverlayButton()
    }

    private fun updateOverlayButton() {
        val canOverlay = Settings.canDrawOverlays(this)
        overlayButton.text = when {
            !canOverlay -> getString(R.string.overlay_request_permission)
            OverlayService.running -> getString(R.string.overlay_disable)
            else -> getString(R.string.overlay_enable)
        }
    }

    private fun render(s: PlaybackManager.State) {
        if (s.hasSession) {
            songTitle.text = s.title ?: s.appName ?: getString(R.string.app_name)
            songArtist.text = s.artist ?: getString(if (s.isPlaying) R.string.playing else R.string.paused)
            sourceText.text = getString(R.string.music_source, s.appName ?: s.appPackage)
            setToggleButton(true, s.isPlaying)
            prevButton.isEnabled = s.canPrev
            prevButton.alpha = if (s.canPrev) 1f else 0.35f
            nextButton.isEnabled = s.canNext
            nextButton.alpha = if (s.canNext) 1f else 0.35f
        } else if (s.canControl) {
            songTitle.text = getString(R.string.compat_mode_short)
            songArtist.text = getString(R.string.compat_toggle_hint)
            sourceText.text = getString(R.string.music_source_none)
            setToggleButton(true, s.isPlaying)
            prevButton.isEnabled = true
            prevButton.alpha = 1f
            nextButton.isEnabled = true
            nextButton.alpha = 1f
        } else {
            songTitle.text = getString(R.string.not_playing)
            songArtist.text = if (s.musicActive) getString(R.string.unknown_source)
                              else getString(R.string.not_playing_hint)
            sourceText.text = getString(R.string.music_source_none)
            setToggleButton(false, false)
            prevButton.isEnabled = false
            prevButton.alpha = 0.35f
            nextButton.isEnabled = false
            nextButton.alpha = 0.35f
        }
    }

    private fun setToggleButton(enabled: Boolean, isPlaying: Boolean) {
        if (enabled) {
            toggleButton.setBackgroundResource(
                if (isPlaying) R.drawable.btn_round_pause else R.drawable.btn_round_play
            )
            toggleButton.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            toggleButton.alpha = 1f
        } else {
            toggleButton.setBackgroundResource(R.drawable.btn_round_disabled)
            toggleButton.setImageResource(R.drawable.ic_play)
            toggleButton.alpha = 0.5f
        }
        toggleButton.isEnabled = enabled
    }
}
