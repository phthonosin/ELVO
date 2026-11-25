package com.elvo.app.enforce

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.elvo.app.R

class OverlayController(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var videoOverlayView: View? = null
    private var reflectionOverlayView: View? = null
    private var exoPlayer: ExoPlayer? = null

    /**
     * Accessibility overlays are allowed to extend over the status bar and
     * display cutout, so the block screen covers the whole device screen.
     * Falls back to a normal app overlay when not hosted by the service.
     */
    private fun overlayParams(fullscreen: Boolean): WindowManager.LayoutParams {
        val type = when {
            context is AccessibilityService -> WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else -> WindowManager.LayoutParams.TYPE_PHONE
        }
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (fullscreen) flags = flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun statusBarHeight(): Int {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else 0
    }

    /** The same faint bubbles of light the app screens use, drawn behind the overlay copy. */
    private fun addGlows(container: FrameLayout, strength: Float) {
        val density = context.resources.displayMetrics.density
        fun bubble(sizeDp: Int, centerColor: Int, gravity: Int, xOffset: Int, yOffset: Int) {
            val size = (sizeDp * density).toInt()
            val view = View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setGradientType(GradientDrawable.RADIAL_GRADIENT)
                    setGradientRadius(size * 0.5f)
                    colors = intArrayOf(centerColor, Color.TRANSPARENT)
                }
            }
            container.addView(
                view,
                FrameLayout.LayoutParams(size, size).apply {
                    this.gravity = gravity
                    leftMargin = xOffset
                    topMargin = yOffset
                }
            )
        }

        val rose = Color.argb((42 * strength).toInt().coerceIn(0, 255), 216, 167, 182)
        val blush = Color.argb((26 * strength).toInt().coerceIn(0, 255), 232, 188, 201)
        bubble(620, rose, Gravity.TOP or Gravity.END, (180 * density).toInt(), (-180 * density).toInt())
        bubble(520, blush, Gravity.BOTTOM or Gravity.START, (-180 * density).toInt(), (150 * density).toInt())
    }

    @OptIn(UnstableApi::class)
    fun showRickrollVideoOverlay(onFinished: () -> Unit) {
        if (videoOverlayView != null) return

        val params = overlayParams(fullscreen = true)

        val container = FrameLayout(context)
        videoOverlayView = container

        val playerView = PlayerView(context).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        }
        container.addView(playerView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        addGlows(container, 0.6f)

        val indicatorBadge = TextView(context).apply {
            text = "Double tap to turn off video"
            setTextColor(android.graphics.Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(36, 16, 36, 16)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 32f
                setColor(android.graphics.Color.parseColor("#CC000000"))
            }
        }

        val indicatorParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = statusBarHeight() + 24
        }
        container.addView(indicatorBadge, indicatorParams)

        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                dismissVideoOverlay()
                onFinished()
                return true
            }
        })

        container.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let {
                val maxVol = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val currentVol = it.getStreamVolume(AudioManager.STREAM_MUSIC)
                val boostedVol = (currentVol * 1.2f).toInt().coerceIn(1, maxVol)
                it.setStreamVolume(AudioManager.STREAM_MUSIC, boostedVol, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val player = ExoPlayer.Builder(context).build().apply {
            val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.rickroll}")
            setMediaItem(MediaItem.fromUri(videoUri))
            volume = 1.0f
            prepare()
            playWhenReady = true
        }
        exoPlayer = player
        playerView.player = player

        try {
            windowManager.addView(container, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            dismissVideoOverlay()
            onFinished()
        }, 10_000)
    }

    fun dismissVideoOverlay() {
        videoOverlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        videoOverlayView = null
        exoPlayer?.release()
        exoPlayer = null
    }

    fun showReflectionOverlay(onFinished: () -> Unit) {
        if (reflectionOverlayView != null) return

        val params = overlayParams(fullscreen = false)

        // Warm near-black background, matching the rest of the app.
        val container = FrameLayout(context).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#0A0808"))
        }
        reflectionOverlayView = container
        addGlows(container, 1f)

        val contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
        }

        // Title View in Dusty Rose
        val titleView = TextView(context).apply {
            text = "Take a breath"
            setTextColor(android.graphics.Color.parseColor("#D8A7B6"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        // Subtitle View in Dusty Rose
        val subtitleView = TextView(context).apply {
            text = "Is opening this app what you want right now?"
            setTextColor(android.graphics.Color.parseColor("#D8A7B6"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 48)
        }

        // Action Button in Dusty Rose
        val actionBtn = Button(context).apply {
            text = "Close app"
            setTextColor(android.graphics.Color.parseColor("#000000"))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28f
                setColor(android.graphics.Color.parseColor("#D8A7B6"))
            }
            setPadding(52, 28, 52, 28)
            setOnClickListener {
                dismissReflectionOverlay()
                onFinished()
            }
        }

        contentLayout.addView(titleView)
        contentLayout.addView(subtitleView)
        contentLayout.addView(actionBtn)

        container.addView(
            contentLayout,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
        )

        try {
            windowManager.addView(container, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            dismissReflectionOverlay()
            onFinished()
        }, 8000)
    }

    fun dismissReflectionOverlay() {
        reflectionOverlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        reflectionOverlayView = null
    }
}
