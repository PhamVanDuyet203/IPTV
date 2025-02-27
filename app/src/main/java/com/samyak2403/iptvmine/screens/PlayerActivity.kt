package com.samyak2403.iptvmine.screens

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.model.Channel
import com.samyak2403.iptvmine.provider.ChannelsProvider
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.mediarouter.media.MediaRouteSelector
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.samyak2403.iptvmine.databinding.ActivityPlayerBinding
import com.samyak2403.iptvmine.databinding.CustomControllerChannelBinding
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.*
import com.google.android.gms.cast.framework.*
import androidx.mediarouter.app.MediaRouteChooserDialog
import com.google.android.gms.cast.framework.media.RemoteMediaClient

class PlayerActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var btnBack: ImageView
    private lateinit var exoPlay: ImageView
    private lateinit var exoPause: ImageView
    private lateinit var exoRew: ImageView
    private lateinit var exoFfwd: ImageView
    private lateinit var linearLayoutControlUp: LinearLayout
    private lateinit var linearLayoutControlBottom: LinearLayout
    private lateinit var controlButtonsTop: LinearLayout
    private lateinit var exoPosition: TextView
    private lateinit var exoDuration: TextView
    private lateinit var imageViewFullScreen: ImageView
    private lateinit var btnPip: LinearLayout
    private lateinit var channel: Channel
    private lateinit var channelsProvider: ChannelsProvider
    private var playbackPosition = 0L
    private var isPlayerReady = false
    private var isFullScreen = false
    private var isLock = false
    private var isInPictureInPictureMode = false
    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "PlayerActivity"
    private lateinit var tvTitle: TextView
    private val binding by lazy { CustomControllerChannelBinding.inflate(layoutInflater) }
    private var isControlVisible = false
    private val controlHideHandler = Handler(Looper.getMainLooper())
    private val hideControlRunnable = Runnable { hideControlsInFullscreen() }
    private val CONTROL_HIDE_DELAY = 3000L
    private lateinit var touchOverlay: View
    private lateinit var castContext: CastContext

    companion object {
        private const val INCREMENT_MILLIS = 5000L
        private const val MIN_PIP_API = Build.VERSION_CODES.O

        fun start(context: Context, channel: Channel) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra("channel", channel)
            }
            context.startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting PlayerActivity for channel: ${intent.getParcelableExtra<Channel>("channel")?.name}")
        setContentView(binding.root)

        channel = savedInstanceState?.getParcelable("channel") ?: intent.getParcelableExtra("channel") ?: run {
            Log.e(TAG, "onCreate: Channel not found in Intent, finishing activity")
            finish()
            return
        }

        channelsProvider = ViewModelProvider(this).get(ChannelsProvider::class.java)
        Log.d(TAG, "onCreate: Initializing ChannelsProvider")
        channelsProvider.init(this)

        // Khởi tạo CastContext
        try {
            castContext = CastContext.getSharedInstance(this)
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Failed to initialize CastContext - ${e.message}")
            Toast.makeText(this, "Casting not available", Toast.LENGTH_SHORT).show()
        }

        setFindViewById()
        setupPlayer()
        setLockScreen()
        setFullScreen()
        setupFavorite()

        setupMirroring()

        if (Build.VERSION.SDK_INT >= MIN_PIP_API) {
            setupPip()
        }

        Log.d(TAG, "onCreate: Adding channel to recent: ${channel.name}")
        channelsProvider.addToRecent(channel)
        controlButtonsTop.visibility = View.VISIBLE

        /* Quan sát thay đổi từ ChannelsProvider để đồng bộ favorite và tên */
        channelsProvider.channels.observe(this) { channels ->
            channels.find { it.streamUrl == channel.streamUrl }?.let { updatedChannel ->
                channel = updatedChannel
                updateFavoriteIcon()
                tvTitle.text = channel.name
                tvTitle.isSelected = true
                Log.d(TAG, "Channels observed: Synced Favorite: ${channel.isFavorite}, Name: ${channel.name}")
            }
        }

        // Tải dữ liệu ban đầu từ Room
        channelsProvider.fetchChannelsFromRoom()
    }

    private fun setFindViewById() {
        Log.d(TAG, "setFindViewById: Initializing UI components")
        playerView = findViewById(R.id.playerView)
        touchOverlay = findViewById(R.id.touchOverlay)
        loadingProgress = findViewById(R.id.loadingProgress)
        btnBack = findViewById(R.id.btnBack)
        exoPlay = findViewById(R.id.exo_play)
        exoPause = findViewById(R.id.exo_pause)
        exoRew = findViewById(R.id.exo_rew)
        exoFfwd = findViewById(R.id.exo_ffwd)
        linearLayoutControlUp = findViewById(R.id.linearLayoutControlUp)
        linearLayoutControlBottom = findViewById(R.id.linearLayoutControlBottom)
        controlButtonsTop = findViewById(R.id.controlButtonsTop)
        exoPosition = findViewById(R.id.exo_position)
        exoDuration = findViewById(R.id.exo_duration)
        imageViewFullScreen = findViewById(R.id.imageViewFullScreen)
        tvTitle = findViewById(R.id.tvTitle)
        btnPip = findViewById(R.id.btn_pip)
        tvTitle.text = channel.name
        tvTitle.isSelected = true
        Log.d(TAG, "setFindViewById: Set tvTitle to ${channel.name}")

        btnBack.setOnClickListener {
            Log.d(TAG, "btnBack clicked")
            if (isLock) {
                Log.d(TAG, "btnBack: Activity locked, ignoring back press")
                return@setOnClickListener
            }
            if (isFullScreen && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Log.d(TAG, "btnBack: Exiting full screen mode")
                imageViewFullScreen.performClick()
            } else if (Build.VERSION.SDK_INT >= MIN_PIP_API && isInPictureInPictureMode) {
                Log.d(TAG, "btnBack: In PiP mode, moving to background")
                moveTaskToBack(true)
            } else {
                Log.d(TAG, "btnBack: Finishing activity")
                finish()
            }
        }
    }

    // mirrroring
    private fun setupMirroring() {
        Log.d(TAG, "setupMirroring: Setting up mirroring functionality")
        val mirroringButton = findViewById<LinearLayout>(R.id.btn_mirroring)
        val mediaRouteButton = findViewById<androidx.mediarouter.app.MediaRouteButton>(R.id.media_route_button)

        // Gán MediaRouteSelector cho MediaRouteButton
        val selector = MediaRouteSelector.Builder()
            .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
            .build()
        mediaRouteButton.routeSelector = selector

        mirroringButton.setOnClickListener {
            Log.d(TAG, "setupMirroring: Mirroring button clicked")
            if (!isPlayerReady) {
                Toast.makeText(this, "Player not ready yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Hiển thị dialog chọn thiết bị Cast
            MediaRouteChooserDialog(this).apply {
                setRouteSelector(selector)
                show()
            }
        }

        // Lắng nghe sự kiện khi chọn thiết bị Cast
        castContext.sessionManager.addSessionManagerListener(object :
            SessionManagerListener<CastSession> {
            override fun onSessionStarting(session: CastSession) {
                Log.d(TAG, "onSessionStarting: Cast session starting")
            }

            override fun onSessionStarted(session: CastSession, sessionId: String) {
                Log.d(TAG, "onSessionStarted: Cast session started with ID: $sessionId")
                loadMediaToCast(session)
            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                Log.e(TAG, "onSessionStartFailed: Error code $error")
                Toast.makeText(this@PlayerActivity, "Failed to start casting", Toast.LENGTH_SHORT).show()
            }

            override fun onSessionEnding(session: CastSession) {
                Log.d(TAG, "onSessionEnding: Cast session ending")
            }

            override fun onSessionEnded(session: CastSession, error: Int) {
                Log.d(TAG, "onSessionEnded: Cast session ended with error: $error")
                player.seekTo(session.remoteMediaClient?.approximateStreamPosition ?: playbackPosition)
                player.playWhenReady = true
            }

            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {}
            override fun onSessionResumeFailed(session: CastSession, error: Int) {}
            override fun onSessionSuspended(session: CastSession, reason: Int) {}
        }, CastSession::class.java)
    }

    // loadmedia mirroring
    private fun loadMediaToCast(session: CastSession) {
        Log.d(TAG, "loadMediaToCast: Loading media to Cast device")
        val remoteMediaClient = session.remoteMediaClient ?: run {
            Log.e(TAG, "loadMediaToCast: RemoteMediaClient is null")
            Toast.makeText(this, "Casting failed: No remote media client", Toast.LENGTH_SHORT).show()
            return
        }

        val mediaInfo = MediaInfo.Builder(channel.streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(
                when {
                    channel.streamUrl.endsWith(".mp4") -> "video/mp4"
                    channel.streamUrl.endsWith(".m3u8") -> "application/x-mpegURL"
                    else -> "video/mp4" // Giá trị mặc định
                }
            )
            .setMetadata(getMediaMetadata())
            .build()

        val mediaLoadOptions = MediaLoadOptions.Builder()
            .setAutoplay(true)
            .setPlayPosition(playbackPosition)
            .build()

        // Sử dụng Callback thay vì addOnSuccessListener
        remoteMediaClient.load(mediaInfo, mediaLoadOptions).setResultCallback { result ->
            if (result.status.isSuccess) {
                Log.d(TAG, "loadMediaToCast: Media loaded successfully")
                player.playWhenReady = false // Tạm dừng ExoPlayer trên thiết bị
            } else {
                Log.e(TAG, "loadMediaToCast: Failed to load media - ${result.status.statusMessage}")
                Toast.makeText(this, "Failed to cast media: ${result.status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getMediaMetadata(): MediaMetadata {
        return MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, channel.name)
        }
    }

    private fun setupFavorite() {
        Log.d(TAG, "setupFavorite: Setting up favorite button listener for channel: ${channel.name}")
        val favoriteLayout = controlButtonsTop.getChildAt(2) as? LinearLayout
        val favoriteIcon = favoriteLayout?.findViewById<ImageView>(R.id.img_fav)
        val favoriteBtn = favoriteLayout?.findViewById<LinearLayout>(R.id.btn_fav)
        // Hiển thị trạng thái ban đầu
        updateFavoriteIcon(favoriteIcon)

        favoriteBtn?.setOnClickListener {
            Log.d(TAG, "setupFavorite: Toggling favorite for channel: ${channel.name}")
            channelsProvider.toggleFavorite(channel) // Gọi toggleFavorite từ ChannelsProvider
            // Cập nhật ngay lập tức UI trong PlayerActivity sau khi toggle
            updateFavoriteIcon(favoriteIcon)
        }
    }

    private fun updateFavoriteIcon(favoriteIcon: ImageView? = controlButtonsTop.getChildAt(2)?.findViewById(R.id.img_fav)) {
        val updatedChannel = channelsProvider.channels.value?.find { it.streamUrl == channel.streamUrl }
        channel = updatedChannel ?: channel // Cập nhật channel trong PlayerActivity
        favoriteIcon?.setImageResource(
            if (channel.isFavorite) R.drawable.fav_on_channel else R.drawable.ic_fav
        )
        Log.d(TAG, "updateFavoriteIcon: Updated icon to Favorite: ${channel.isFavorite}")
    }

    private fun showErrorDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_unavailable, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.show()

        val width = (312 * resources.displayMetrics.density).toInt()
        dialog.window?.apply {
            setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
            attributes?.width = width
            attributes?.height = WindowManager.LayoutParams.WRAP_CONTENT
            setBackgroundDrawableResource(R.drawable.bg_confirm_del)
        }

        val btnOk = dialogView.findViewById<TextView>(R.id.btn_ok)
        btnOk?.setOnClickListener {
            dialog.dismiss()
            finish()
        } ?: Log.e(TAG, "showErrorDialog: btn_ok not found in dialog_unavailable.xml")
    }

    private fun setupPlayer() {
        Log.d(TAG, "setupPlayer: Setting up ExoPlayer for stream URL: ${channel.streamUrl}")
        player = ExoPlayer.Builder(this)
            .setSeekBackIncrementMs(INCREMENT_MILLIS)
            .setSeekForwardIncrementMs(INCREMENT_MILLIS)
            .build().also { exoPlayer ->
                playerView.player = exoPlayer

                val uri = Uri.parse(channel.streamUrl)
                if (uri == null || uri.toString().isEmpty()) {
                    Log.e(TAG, "setupPlayer: Invalid URI: ${channel.streamUrl}")
                    Toast.makeText(this, "Invalid video URL", Toast.LENGTH_SHORT).show()
                    return
                }

                val mediaItem = MediaItem.fromUri(uri)
                val mediaSource = when {
                    uri.scheme == "content" || uri.scheme == "file" -> {
                        Log.d(TAG, "setupPlayer: Detected local file URI (MP4), using ProgressiveMediaSource")
                        ProgressiveMediaSource.Factory(
                            DefaultDataSource.Factory(this)
                        ).createMediaSource(mediaItem)
                    }
                    uri.scheme == "http" || uri.scheme == "https" -> {
                        Log.d(TAG, "setupPlayer: Detected streaming URL, using HlsMediaSource")
                        HlsMediaSource.Factory(DefaultHttpDataSource.Factory()).createMediaSource(mediaItem)
                    }
                    else -> {
                        Log.w(TAG, "setupPlayer: Unsupported URI scheme: ${uri.scheme}, defaulting to HlsMediaSource")
                        HlsMediaSource.Factory(DefaultHttpDataSource.Factory()).createMediaSource(mediaItem)
                    }
                }

                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.seekTo(playbackPosition)
                exoPlayer.playWhenReady = true
                exoPlayer.prepare()

                loadingProgress.visibility = View.VISIBLE
                if (!isFullScreen) {
                    linearLayoutControlUp.visibility = View.GONE
                    linearLayoutControlBottom.visibility = View.GONE
                    controlButtonsTop.visibility = View.GONE
                }

                exoPlayer.addListener(object : Player.Listener {
                    override fun onIsLoadingChanged(isLoading: Boolean) {
                        Log.d(TAG, "onIsLoadingChanged: isLoading = $isLoading")
                        loadingProgress.visibility = if (isLoading && !exoPlayer.isPlaying) View.VISIBLE else View.GONE
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "onPlayerError: Error playing video - ${error.errorCodeName}, ${error.message}")
                        loadingProgress.visibility = View.GONE
                        showErrorDialog()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d(TAG, "onPlaybackStateChanged: State = $playbackState")
                        when (playbackState) {
                            Player.STATE_READY -> {
                                isPlayerReady = true
                                Log.d(TAG, "onPlaybackStateChanged: Player ready")
                            }
                            Player.STATE_BUFFERING -> {
                                Log.d(TAG, "onPlaybackStateChanged: Buffering")
                                loadingProgress.visibility = View.VISIBLE
                            }
                            Player.STATE_ENDED -> {
                                Log.d(TAG, "onPlaybackStateChanged: Playback ended")
                                loadingProgress.visibility = View.GONE
                            }
                            Player.STATE_IDLE -> {
                                Log.d(TAG, "onPlaybackStateChanged: Player idle")
                                loadingProgress.visibility = View.VISIBLE
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        Log.d(TAG, "onIsPlayingChanged: isPlaying = $isPlaying")
                        updatePlayPauseIcons(isPlaying)
                        if (isPlaying && !isFullScreen) {
                            loadingProgress.visibility = View.GONE
                            linearLayoutControlUp.visibility = View.VISIBLE
                            linearLayoutControlBottom.visibility = View.VISIBLE
                            controlButtonsTop.visibility = View.VISIBLE
                        }
                    }
                })
            }
    }

    private fun updatePlayPauseIcons(isPlaying: Boolean) {
        Log.d(TAG, "updatePlayPauseIcons: Updating play/pause icons, isPlaying = $isPlaying")
        exoPlay.visibility = if (isPlaying) View.GONE else View.VISIBLE
        exoPause.visibility = if (isPlaying) View.VISIBLE else View.GONE
    }

    private val originalLayoutParams = mutableMapOf<View, LinearLayout.LayoutParams>()

    private fun lockScreen(lock: Boolean) {
        Log.d(TAG, "lockScreen: Setting lock state to $lock")
        isLock = lock

        val lockLayout = findViewById<LinearLayout>(R.id.btn_lock)
        val lockIcon = findViewById<ImageView>(R.id.img_lock)
        val lockText = findViewById<TextView>(R.id.txt_lock)

        if (lock) {
            // Khi khóa màn hình
            linearLayoutControlUp.visibility = View.INVISIBLE
            linearLayoutControlBottom.visibility = View.INVISIBLE
            controlButtonsTop.visibility = View.VISIBLE

            for (i in 0 until controlButtonsTop.childCount) {
                val child = controlButtonsTop.getChildAt(i)
                if (child != lockLayout) {
                    // Lưu lại layoutParams ban đầu trước khi ẩn
                    originalLayoutParams[child] = child.layoutParams as LinearLayout.LayoutParams
                    child.visibility = View.GONE
                }
            }

            lockIcon.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_lock_screen))
            lockText.text = "Screen lock. Long press to unlock"
            lockLayout.setBackgroundColor(Color.WHITE)
            lockText.setTextColor(Color.parseColor("#000000"))
        } else {
            // Khi thoát khóa
            linearLayoutControlUp.visibility = if (isFullScreen) View.GONE else View.VISIBLE
            linearLayoutControlBottom.visibility = if (isFullScreen) View.GONE else View.VISIBLE
            controlButtonsTop.visibility = View.VISIBLE

            val margin4dp = resources.getDimensionPixelSize(R.dimen.button_margin)

            for (i in 0 until controlButtonsTop.childCount) {
                val child = controlButtonsTop.getChildAt(i)
                child.visibility = View.VISIBLE

                // Khôi phục layoutParams gốc nếu có
                originalLayoutParams[child]?.let {
                    child.layoutParams = it
                } ?: run {
                    // Nếu không có layoutParams gốc, thiết lập margin mặc định
                    val params = child.layoutParams as LinearLayout.LayoutParams
                    params.setMargins(margin4dp, params.topMargin, margin4dp, params.bottomMargin)
                    child.layoutParams = params
                }
            }

            lockIcon.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_lock))
            lockText.text = "Lock"
            lockLayout.setBackgroundResource(R.drawable.bg_menu_playcontrol)
            lockText.setTextColor(Color.parseColor("#3F484A"))

            // Yêu cầu bố trí lại để áp dụng layout mới
            controlButtonsTop.requestLayout()

            // Nếu đang ở chế độ toàn màn hình, kiểm tra và điều chỉnh UI
            if (isFullScreen && !isControlVisible) {
                hideControlsInFullscreen()
            }
        }

        Log.d(TAG, "lockScreen: UI state - controlButtonsTop childCount=${controlButtonsTop.childCount}")
    }


    private fun setLockScreen() {
        Log.d(TAG, "setLockScreen: Setting up lock button listener")
        val lockButton = findViewById<LinearLayout>(R.id.btn_lock)
        val lockIcon = findViewById<ImageView>(R.id.img_lock)

        lockButton.setOnClickListener {
            if (!isLock) {
                isLock = true
                Log.d(TAG, "setLockScreen: Lock enabled")
                lockIcon.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_lock_screen))
                lockScreen(true)
            }
        }

        lockButton.setOnLongClickListener {
            if (isLock) {
                Log.d(TAG, "setLockScreen: Long press detected, unlocking")
                isLock = false
                lockIcon.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_lock))
                lockScreen(false)
                true
            } else {
                false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SourceLockedOrientationActivity")
    private fun setFullScreen() {
        Log.d(TAG, "setFullScreen: Setting up full screen button listener")
        imageViewFullScreen.setOnClickListener {
            isFullScreen = !isFullScreen
            Log.d(TAG, "setFullScreen: Full screen toggled to $isFullScreen")
            imageViewFullScreen.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext,
                    if (isFullScreen) R.drawable.ic_baseline_fullscreen_exit else R.drawable.ic_baseline_fullscreen
                )
            )
            requestedOrientation = if (isFullScreen) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            if (isFullScreen) {
                binding.controlButtonsTop1.visibility = View.VISIBLE
                binding.root.setBackgroundColor(getColor(R.color.black))
                btnBack.visibility = View.GONE
                tvTitle.visibility = View.GONE
                controlButtonsTop.visibility = View.GONE
                loadingProgress.visibility = View.GONE
                playerView.useController = true
                playerView.hideController()

                val params = playerView.layoutParams as ConstraintLayout.LayoutParams
                params.height = ConstraintLayout.LayoutParams.MATCH_PARENT
                params.topToBottom = ConstraintLayout.LayoutParams.UNSET
                params.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                params.topMargin = 0
                params.bottomMargin = 100
                playerView.layoutParams = params

                touchOverlay.visibility = View.VISIBLE
                val overlayParams = touchOverlay.layoutParams as ConstraintLayout.LayoutParams
                overlayParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT
                overlayParams.topToBottom = ConstraintLayout.LayoutParams.UNSET
                overlayParams.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                overlayParams.bottomMargin = 100
                touchOverlay.layoutParams = overlayParams

                touchOverlay.setOnClickListener {
                    Log.d(TAG, "Touch overlay clicked in fullscreen")
                    if (isFullScreen) {
                        toggleControlsInFullscreen()
                    }
                }
            } else {
                loadingProgress.visibility = View.GONE
                binding.controlButtonsTop1.visibility = View.GONE
                binding.root.setBackgroundColor(getColor(R.color.white))
                btnBack.visibility = View.VISIBLE
                tvTitle.visibility = View.VISIBLE
                linearLayoutControlUp.visibility = View.VISIBLE
                linearLayoutControlBottom.visibility = View.VISIBLE
                controlButtonsTop.visibility = View.VISIBLE
                loadingProgress.visibility = if (player.isLoading && !player.isPlaying) View.VISIBLE else View.GONE
                imageViewFullScreen.visibility = View.VISIBLE
                playerView.useController = true
                playerView.showController()

                val params = playerView.layoutParams as ConstraintLayout.LayoutParams
                params.height = resources.getDimensionPixelSize(R.dimen.player_height)
                params.topToBottom = R.id.tvTitle
                params.bottomToTop = R.id.linearLayoutControlUp
                params.topMargin = resources.getDimensionPixelSize(R.dimen.player_margin_top)
                params.bottomMargin = 0
                playerView.layoutParams = params

                val overlayParams = touchOverlay.layoutParams as ConstraintLayout.LayoutParams
                overlayParams.height = resources.getDimensionPixelSize(R.dimen.player_height)
                overlayParams.topToBottom = R.id.tvTitle
                overlayParams.bottomToTop = R.id.linearLayoutControlUp
                touchOverlay.layoutParams = overlayParams
                touchOverlay.visibility = View.GONE
                touchOverlay.setOnClickListener(null)

                tvTitle.setTextColor(Color.parseColor("#000000"))
                btnBack.clearColorFilter()

                val controlButtonsParams = controlButtonsTop.layoutParams as ConstraintLayout.LayoutParams
                controlButtonsParams.topToBottom = R.id.playerView
                controlButtonsParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                controlButtonsParams.bottomMargin = 0
                controlButtonsParams.topMargin = resources.getDimensionPixelSize(R.dimen.control_margin_default)
                controlButtonsTop.layoutParams = controlButtonsParams

                findViewById<LinearLayout>(R.id.btn_mirroring)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
                findViewById<LinearLayout>(R.id.btn_pip)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
                controlButtonsTop.getChildAt(2)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
                controlButtonsTop.getChildAt(3)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
                findViewById<TextView>(R.id.txt_mirroring)?.setTextColor(Color.parseColor("#3F484A"))
                findViewById<TextView>(R.id.txt_pip)?.setTextColor(Color.parseColor("#3F484A"))
                findViewById<TextView>(R.id.txt_fav)?.setTextColor(Color.parseColor("#3F484A"))
                findViewById<TextView>(R.id.txt_lock)?.setTextColor(Color.parseColor("#3F484A"))
                findViewById<ImageView>(R.id.img_mirroring)?.clearColorFilter()
                findViewById<ImageView>(R.id.img_pip)?.clearColorFilter()
                findViewById<ImageView>(R.id.img_fav)?.clearColorFilter()
                findViewById<ImageView>(R.id.img_lock)?.clearColorFilter()

                isControlVisible = false
                controlHideHandler.removeCallbacks(hideControlRunnable)
            }
        }
    }

    private fun toggleControlsInFullscreen() {
        if (isControlVisible) {
            hideControlsInFullscreen()
        } else {
            showControlsInFullscreen()
        }
    }

    private fun showControlsInFullscreen() {
        Log.d(TAG, "showControlsInFullscreen: Showing controls")
        isControlVisible = true

        btnBack.visibility = if (isLock) View.GONE else View.VISIBLE
        tvTitle.visibility = if (isLock) View.GONE else View.VISIBLE
        controlButtonsTop.visibility = View.GONE

        playerView.useController = true
        playerView.showController()

        tvTitle.setTextColor(Color.parseColor("#FFFFFF"))
        findViewById<TextView>(R.id.txt_mirroring)?.setTextColor(Color.parseColor("#FFFFFF"))
        findViewById<TextView>(R.id.txt_pip)?.setTextColor(Color.parseColor("#FFFFFF"))
        findViewById<TextView>(R.id.txt_fav)?.setTextColor(Color.parseColor("#FFFFFF"))
        findViewById<TextView>(R.id.txt_lock)?.setTextColor(Color.parseColor("#FFFFFF"))

        exoPosition.setTextColor(Color.parseColor("#FFFFFF"))
        exoDuration.setTextColor(Color.parseColor("#FFFFFF"))

        findViewById<ImageView>(R.id.img_mirroring)?.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.img_pip)?.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.img_fav)?.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.img_lock)?.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        btnBack.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)

        exoPlay.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        exoPause.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        exoRew.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        exoFfwd.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        imageViewFullScreen.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)

        findViewById<LinearLayout>(R.id.btn_mirroring)?.setBackgroundColor(Color.parseColor("#111111"))
        findViewById<LinearLayout>(R.id.btn_pip)?.setBackgroundColor(Color.parseColor("#111111"))
        controlButtonsTop.getChildAt(2)?.setBackgroundColor(Color.parseColor("#111111"))
        controlButtonsTop.getChildAt(3)?.setBackgroundColor(Color.parseColor("#111111"))

        val controlButtonsParams = controlButtonsTop.layoutParams as ConstraintLayout.LayoutParams
        controlButtonsParams.topToBottom = ConstraintLayout.LayoutParams.UNSET
        controlButtonsParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        controlButtonsParams.bottomMargin = resources.getDimensionPixelSize(R.dimen.fullscreen_control_margin_bottom) * 2
        controlButtonsTop.layoutParams = controlButtonsParams

        val timeBar = linearLayoutControlBottom.findViewById<com.google.android.exoplayer2.ui.DefaultTimeBar>(R.id.exo_progress)
        timeBar?.visibility = View.VISIBLE

        touchOverlay.isClickable = false

        controlHideHandler.removeCallbacks(hideControlRunnable)
        controlHideHandler.postDelayed(hideControlRunnable, CONTROL_HIDE_DELAY)
    }

    private fun hideControlsInFullscreen() {
        Log.d(TAG, "hideControlsInFullscreen: Hiding controls")
        isControlVisible = false

        btnBack.visibility = View.GONE
        tvTitle.visibility = View.GONE
        controlButtonsTop.visibility = View.GONE
        loadingProgress.visibility = View.GONE

        playerView.useController = false
        playerView.hideController()

        tvTitle.setTextColor(Color.parseColor("#000000"))
        findViewById<TextView>(R.id.txt_mirroring)?.setTextColor(Color.parseColor("#3F484A"))
        findViewById<TextView>(R.id.txt_pip)?.setTextColor(Color.parseColor("#3F484A"))
        findViewById<TextView>(R.id.txt_fav)?.setTextColor(Color.parseColor("#3F484A"))
        findViewById<TextView>(R.id.txt_lock)?.setTextColor(Color.parseColor("#3F484A"))

        exoPosition.setTextColor(Color.parseColor("#FFFFFF"))
        exoDuration.setTextColor(Color.parseColor("#CBCDC8"))

        findViewById<ImageView>(R.id.img_mirroring)?.clearColorFilter()
        findViewById<ImageView>(R.id.img_pip)?.clearColorFilter()
        findViewById<ImageView>(R.id.img_fav)?.clearColorFilter()
        findViewById<ImageView>(R.id.img_lock)?.clearColorFilter()
        btnBack.clearColorFilter()

        exoPlay.clearColorFilter()
        exoPause.clearColorFilter()
        exoRew.clearColorFilter()
        exoFfwd.clearColorFilter()
        imageViewFullScreen.clearColorFilter()

        findViewById<LinearLayout>(R.id.btn_mirroring)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
        findViewById<LinearLayout>(R.id.btn_pip)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
        controlButtonsTop.getChildAt(2)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
        controlButtonsTop.getChildAt(3)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)

        val controlButtonsParams = controlButtonsTop.layoutParams as ConstraintLayout.LayoutParams
        controlButtonsParams.topToBottom = R.id.playerView
        controlButtonsParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
        controlButtonsParams.bottomMargin = 0
        controlButtonsParams.topMargin = resources.getDimensionPixelSize(R.dimen.control_margin_default)
        controlButtonsTop.layoutParams = controlButtonsParams

        touchOverlay.isClickable = true

        controlHideHandler.removeCallbacks(hideControlRunnable)
    }

    private fun setupPip() {
        Log.d(TAG, "setupPip: Setting up PiP button listener")
        if (Build.VERSION.SDK_INT >= MIN_PIP_API) {
            btnPip.setOnClickListener {
                Log.d(TAG, "setupPip: Attempting to enter Picture-in-Picture mode")
                enterPictureInPictureModeIfAvailable()
            }
            binding.btnPip1.setOnClickListener {
                Log.d(TAG, "setupPip: Attempting to enter Picture-in-Picture mode")
                enterPictureInPictureModeIfAvailable()
            }
        } else {
            btnPip.visibility = View.GONE
            Log.w(TAG, "setupPip: PiP not supported on this device (API < 26)")
        }
    }

    private fun enterPictureInPictureModeIfAvailable() {
        if (Build.VERSION.SDK_INT >= MIN_PIP_API) {
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                Log.w(TAG, "enterPictureInPictureModeIfAvailable: PiP not supported on this device")
                Toast.makeText(this, "Picture-in-Picture mode not supported on this device", Toast.LENGTH_SHORT).show()
                return
            }

            if (isInPictureInPictureMode) {
                Log.d(TAG, "enterPictureInPictureModeIfAvailable: Already in PiP mode")
                return
            }

            try {
                playerView.post {
                    val width = playerView.measuredWidth
                    val height = playerView.measuredHeight

                    if (width <= 0 || height <= 0) {
                        Log.e(TAG, "enterPictureInPictureModeIfAvailable: Invalid playerView dimensions ($width x $height)")
                        Toast.makeText(this, "Cannot enter PiP mode: Player not ready", Toast.LENGTH_SHORT).show()
                        return@post
                    }

                    val aspectRatio = Rational(width, height)
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                        .build()

                    if (enterPictureInPictureMode(params)) {
                        isInPictureInPictureMode = true
                        Log.d(TAG, "enterPictureInPictureModeIfAvailable: Entered PiP mode successfully")
                    } else {
                        Log.e(TAG, "enterPictureInPictureModeIfAvailable: Failed to enter PiP mode")
                        Toast.makeText(this, "Failed to enter PiP mode", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "enterPictureInPictureModeIfAvailable: Error - ${e.message}")
                Toast.makeText(this, "Error entering PiP mode: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        @NonNull newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        Log.d(TAG, "onPictureInPictureModeChanged: PiP mode changed to $isInPictureInPictureMode")
        this.isInPictureInPictureMode = isInPictureInPictureMode

        if (isInPictureInPictureMode) {

            // Ẩn tất cả các thành phần trừ PlayerView
            btnBack.visibility = View.GONE
            tvTitle.visibility = View.GONE
            linearLayoutControlUp.visibility = View.GONE
            controlButtonsTop.visibility = View.GONE
            loadingProgress.visibility = View.GONE
            playerView.useController = false // Ẩn controller mặc định
            playerView.visibility = View.VISIBLE
            // Tạm thời điều chỉnh PlayerView để chiếm toàn bộ không gian trong PiP
            val params = playerView.layoutParams as ConstraintLayout.LayoutParams
            params.height = ConstraintLayout.LayoutParams.MATCH_PARENT
            params.topToBottom = ConstraintLayout.LayoutParams.UNSET
            params.bottomToTop = ConstraintLayout.LayoutParams.UNSET // Bỏ constraint bottom
            params.topMargin = 0
            params.bottomMargin = 0
            playerView.layoutParams = params
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT


        } else {
            // Khôi phục giao diện ban đầu
            btnBack.visibility = View.VISIBLE
            tvTitle.visibility = View.VISIBLE
            linearLayoutControlUp.visibility = View.VISIBLE
            controlButtonsTop.visibility = View.VISIBLE
            loadingProgress.visibility = if (player.isLoading) View.VISIBLE else View.GONE

            playerView.useController = true
            playerView.visibility = View.VISIBLE

            // Khôi phục constraint ban đầu của PlayerView
            val params = playerView.layoutParams as ConstraintLayout.LayoutParams
            params.width = ConstraintLayout.LayoutParams.MATCH_PARENT
            params.height = resources.getDimensionPixelSize(R.dimen.player_height)
            params.topToBottom = R.id.tvTitle
            params.bottomToTop = R.id.linearLayoutControlUp
            params.topMargin = resources.getDimensionPixelSize(R.dimen.player_margin_top)
            params.bottomMargin = 0
            params.leftMargin = 0
            params.rightMargin = 0
            playerView.layoutParams = params

            binding.controlButtonsTop1.visibility = View.VISIBLE
            binding.root.setBackgroundColor(getColor(R.color.white))

            imageViewFullScreen.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext,
                    R.drawable.ic_baseline_fullscreen
                )
            )
            isFullScreen = false
        }
    }


    private fun updateTime() {
        Log.d(TAG, "updateTime: Updating position and duration")
        player?.let {
            val position = it.currentPosition
            val duration = it.duration
            exoPosition.text = formatTime(position)
            exoDuration.text = formatTime(duration)
        }
    }

    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged: Orientation changed to ${newConfig.orientation}")
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && isFullScreen) {
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            supportActionBar?.hide()
            Log.d(TAG, "onConfigurationChanged: Entered full screen mode")
            if (!isControlVisible) {
                hideControlsInFullscreen() // Đảm bảo ẩn điều khiển khi vào fullscreen
                touchOverlay.visibility = View.VISIBLE // Đảm bảo touchOverlay hiển thị
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            supportActionBar?.show()
            Log.d(TAG, "onConfigurationChanged: Returned to portrait mode")
            isControlVisible = false // Reset trạng thái khi thoát fullscreen
            touchOverlay.visibility = View.GONE // Ẩn touchOverlay khi thoát fullscreen
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState: Saving state with playbackPosition = $playbackPosition")
        outState.putParcelable("channel", channel)
        player?.let { playbackPosition = it.currentPosition }
        outState.putLong("playbackPosition", playbackPosition)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: Initializing/resuming player")
        if (!::player.isInitialized) {
            setupPlayer()
        }
        if (Util.SDK_INT > 23) {
            player.playWhenReady = true
        }
    }

    override fun onResume() {
        super.onResume()
        channelsProvider.fetchChannelsFromRoom() // Tải lại danh sách kênh để đảm bảo đồng bộ
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: Pausing player if necessary")
        if (Util.SDK_INT <= 23 && ::player.isInitialized) {
            player.playWhenReady = false
            playbackPosition = player.currentPosition
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: Handling player stop")
        if (::player.isInitialized) {
            if (Util.SDK_INT > 23) {
                player.playWhenReady = false
                playbackPosition = player.currentPosition
            }
            if (!isInPictureInPictureMode) {
                player.release()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Cleaning up")
        if (::player.isInitialized && !isInPictureInPictureMode) {
            player.release()
        }
        controlHideHandler.removeCallbacks(hideControlRunnable)

        // Yêu cầu làm mới dữ liệu khi thoát
        channelsProvider.requestRefresh()
        Log.d(TAG, "onDestroy: Requested ChannelsProvider to refresh")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isLock) {
            Log.d(TAG, "onBackPressed: Activity locked, ignoring back press")
            return
        }
        if (isFullScreen && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "onBackPressed: Exiting full screen mode")
            imageViewFullScreen.performClick()
        } else if (Build.VERSION.SDK_INT >= MIN_PIP_API && isInPictureInPictureMode) {
            Log.d(TAG, "onBackPressed: In PiP mode, moving to background")
            moveTaskToBack(true)
        } else {
            Log.d(TAG, "onBackPressed: Finishing activity")
            super.onBackPressed()
        }
    }
}