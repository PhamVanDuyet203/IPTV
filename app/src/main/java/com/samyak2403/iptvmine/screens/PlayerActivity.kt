package com.samyak2403.iptvmine.screens

import android.annotation.SuppressLint
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
import android.view.ViewGroup

class PlayerActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var btnBack: ImageView
    private lateinit var exoPlay: ImageView
    private lateinit var exoPause: ImageView
    private lateinit var exoRew: ImageView
    private lateinit var exoFfwd: ImageView
    private lateinit var imageViewLock: ImageView
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

    private var isControlVisible = false
    private val controlHideHandler = Handler(Looper.getMainLooper())
    private val hideControlRunnable = Runnable { hideControlsInFullscreen() }
    private val CONTROL_HIDE_DELAY = 3000L // Ẩn sau 3 giây
    private lateinit var touchOverlay: View

    companion object {
        private const val INCREMENT_MILLIS = 5000L
        private const val MIN_PIP_API = Build.VERSION_CODES.O // API 26

        fun start(context: Context, channel: Channel) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra("channel", channel)
            }
            context.startActivity(intent)
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting PlayerActivity for channel: ${intent.getParcelableExtra<Channel>("channel")?.name}")
        setContentView(R.layout.custom_controller_channel)

        channel = savedInstanceState?.getParcelable("channel") ?: intent.getParcelableExtra("channel") ?: run {
            Log.e(TAG, "onCreate: Channel not found in Intent, finishing activity")
            finish()
            return
        }

        channelsProvider = ViewModelProvider(this).get(ChannelsProvider::class.java)
        Log.d(TAG, "onCreate: Initializing ChannelsProvider")
        channelsProvider.init(this)

        setFindViewById()
        setupPlayer()
        setLockScreen()
        setFullScreen()
        setupFavorite()

        if (Build.VERSION.SDK_INT >= MIN_PIP_API) {
            setupPip()
        }

        Log.d(TAG, "onCreate: Adding channel to recent: ${channel.name}")
        channelsProvider.addToRecent(channel)
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
    }
    private fun setupPlayer() {
        Log.d(TAG, "setupPlayer: Setting up ExoPlayer for stream URL: ${channel.streamUrl}")
        player = ExoPlayer.Builder(this)
            .setSeekBackIncrementMs(INCREMENT_MILLIS)
            .setSeekForwardIncrementMs(INCREMENT_MILLIS)
            .build().also { exoPlayer ->
                playerView.player = exoPlayer

                val mediaItem = MediaItem.fromUri(Uri.parse(channel.streamUrl))
                val mediaSource = HlsMediaSource.Factory(DefaultHttpDataSource.Factory()).createMediaSource(mediaItem)
                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.seekTo(playbackPosition)
                exoPlayer.playWhenReady = true
                exoPlayer.prepare()

                // Đặt loading ban đầu là visible
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
                        Toast.makeText(this@PlayerActivity, "Error playing video: ${error.message}", Toast.LENGTH_LONG).show()
                        playerView.visibility = View.GONE
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

    private fun lockScreen(lock: Boolean) {
        Log.d(TAG, "lockScreen: Setting lock state to $lock")
        linearLayoutControlUp.visibility = if (lock) View.INVISIBLE else View.VISIBLE
        linearLayoutControlBottom.visibility = if (lock) View.INVISIBLE else View.VISIBLE
        controlButtonsTop.visibility = if (lock) View.INVISIBLE else View.VISIBLE
    }

    private fun setLockScreen() {
        Log.d(TAG, "setLockScreen: Setting up lock button listener")
        val lockButton = findViewById<LinearLayout>(R.id.btn_lock)
        val lockIcon = findViewById<ImageView>(R.id.img_lock)
        lockButton.setOnClickListener {
            isLock = !isLock
            Log.d(TAG, "setLockScreen: Lock toggled to $isLock")
            lockIcon.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext,
                    if (isLock) R.drawable.ic_baseline_lock else R.drawable.ic_baseline_lock_open
                )
            )
            lockScreen(isLock)
        }
    }

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
                // Ẩn tất cả, chỉ giữ PlayerView
                btnBack.visibility = View.GONE
                tvTitle.visibility = View.GONE
                controlButtonsTop.visibility = View.GONE
                loadingProgress.visibility = View.GONE
                playerView.useController = true // Giữ controller bật nhưng ẩn ban đầu
                playerView.hideController() // Ẩn controller ban đầu để chỉ hiển thị video

                // Điều chỉnh PlayerView chiếm toàn màn hình
                val params = playerView.layoutParams as ConstraintLayout.LayoutParams
                params.height = ConstraintLayout.LayoutParams.MATCH_PARENT
                params.topToBottom = ConstraintLayout.LayoutParams.UNSET
                params.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                params.topMargin = 0
                playerView.layoutParams = params

                // Kích hoạt touchOverlay trong fullscreen
                touchOverlay.visibility = View.VISIBLE
                val overlayParams = touchOverlay.layoutParams as ConstraintLayout.LayoutParams
                overlayParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT
                overlayParams.topToBottom = ConstraintLayout.LayoutParams.UNSET
                overlayParams.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                touchOverlay.layoutParams = overlayParams

                // Thêm listener chạm vào touchOverlay
                touchOverlay.setOnClickListener {
                    Log.d(TAG, "Touch overlay clicked in fullscreen")
                    if (isFullScreen) {
                        toggleControlsInFullscreen()
                    }
                }
            } else {
                // Khôi phục giao diện ban đầu hoàn toàn
                btnBack.visibility = View.VISIBLE
                tvTitle.visibility = View.VISIBLE
                linearLayoutControlUp.visibility = View.VISIBLE
                linearLayoutControlBottom.visibility = View.VISIBLE
                controlButtonsTop.visibility = View.VISIBLE
                loadingProgress.visibility = if (player.isLoading) View.VISIBLE else View.GONE
                imageViewFullScreen.visibility = View.VISIBLE
                playerView.useController = true // Đảm bảo controller mặc định bật
                playerView.showController() // Hiển thị controller khi thoát fullscreen

                // Khôi phục kích thước và vị trí PlayerView
                val params = playerView.layoutParams as ConstraintLayout.LayoutParams
                params.height = resources.getDimensionPixelSize(R.dimen.player_height) // 200dp
                params.topToBottom = R.id.tvTitle
                params.bottomToTop = R.id.linearLayoutControlUp
                params.topMargin = resources.getDimensionPixelSize(R.dimen.player_margin_top) // 120dp
                playerView.layoutParams = params

                // Khôi phục touchOverlay về trạng thái ban đầu
                val overlayParams = touchOverlay.layoutParams as ConstraintLayout.LayoutParams
                overlayParams.height = resources.getDimensionPixelSize(R.dimen.player_height) // 200dp
                overlayParams.topToBottom = R.id.tvTitle
                overlayParams.bottomToTop = R.id.linearLayoutControlUp
                touchOverlay.layoutParams = overlayParams
                touchOverlay.visibility = View.GONE
                touchOverlay.setOnClickListener(null)

                // Khôi phục trạng thái ban đầu cho custom_controller_channel.xml
                tvTitle.setTextColor(Color.parseColor("#000000")) // Màu gốc của tvTitle
                btnBack.clearColorFilter() // Khôi phục màu gốc btnBack

                // Khôi phục trạng thái ban đầu cho controlButtonsTop
                val controlButtonsParams = controlButtonsTop.layoutParams as ConstraintLayout.LayoutParams
                controlButtonsParams.topToBottom = R.id.playerView
                controlButtonsParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                controlButtonsParams.bottomMargin = 0
                controlButtonsParams.topMargin = resources.getDimensionPixelSize(R.dimen.control_margin_default) // 24dp
                controlButtonsTop.layoutParams = controlButtonsParams

                findViewById<LinearLayout>(R.id.btn_mirroring)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
                findViewById<LinearLayout>(R.id.btn_pip)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
                controlButtonsTop.getChildAt(2)?.setBackgroundResource(R.drawable.bg_menu_playcontrol) // Favorite
                controlButtonsTop.getChildAt(3)?.setBackgroundResource(R.drawable.bg_menu_playcontrol) // Lock
                findViewById<TextView>(R.id.txt_mirroring)?.setTextColor(Color.parseColor("#3F484A"))
                findViewById<TextView>(R.id.txt_pip)?.setTextColor(Color.parseColor("#3F484A"))
                findViewById<TextView>(R.id.txt_fav)?.setTextColor(Color.parseColor("#3F484A"))
                findViewById<TextView>(R.id.txt_lock)?.setTextColor(Color.parseColor("#3F484A"))
                findViewById<ImageView>(R.id.img_mirroring)?.clearColorFilter()
                findViewById<ImageView>(R.id.img_pip)?.clearColorFilter()
                findViewById<ImageView>(R.id.img_fav)?.clearColorFilter()
                findViewById<ImageView>(R.id.img_lock)?.clearColorFilter()

                // Reset isControlVisible và hủy lệnh ẩn
                isControlVisible = false
                controlHideHandler.removeCallbacks(hideControlRunnable)
            }
        }
    }

    private fun toggleControlsInFullscreen() {
        if (isControlVisible) {
            hideControlsInFullscreen() // Ẩn cả hai giao diện khi nhấn lần thứ hai
        } else {
            showControlsInFullscreen() // Hiển thị cả hai giao diện khi nhấn lần đầu
        }
    }

    private fun showControlsInFullscreen() {
        Log.d(TAG, "showControlsInFullscreen: Showing controls")
        isControlVisible = true

        // Hiển thị các thành phần từ custom_controller_channel.xml
        btnBack.visibility = if (isLock) View.GONE else View.VISIBLE // Ẩn btnBack khi khóa
        tvTitle.visibility = if (isLock) View.GONE else View.VISIBLE // Ẩn tvTitle khi khóa
        controlButtonsTop.visibility = View.VISIBLE

        // Hiển thị controller từ custom_controller.xml
        playerView.useController = true
        playerView.showController() // Buộc hiển thị controller ngay lập tức

        // Đổi màu text sang #FFFFFF cho custom_controller_channel.xml
        tvTitle.setTextColor(Color.parseColor("#FFFFFF"))
        findViewById<TextView>(R.id.txt_mirroring)?.setTextColor(Color.parseColor("#FFFFFF"))
        findViewById<TextView>(R.id.txt_pip)?.setTextColor(Color.parseColor("#FFFFFF"))
        findViewById<TextView>(R.id.txt_fav)?.setTextColor(Color.parseColor("#FFFFFF"))
        findViewById<TextView>(R.id.txt_lock)?.setTextColor(Color.parseColor("#FFFFFF"))

        // Đổi màu text trong custom_controller.xml
        exoPosition.setTextColor(Color.parseColor("#FFFFFF"))
        exoDuration.setTextColor(Color.parseColor("#FFFFFF"))

        // Đổi màu icon sang #FFFFFF cho custom_controller_channel.xml
        findViewById<ImageView>(R.id.img_mirroring)?.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.img_pip)?.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.img_fav)?.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.img_lock)?.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        btnBack.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)

        // Đổi màu icon trong custom_controller.xml
        exoPlay.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        exoPause.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        exoRew.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        exoFfwd.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        imageViewFullScreen.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)

        // Đổi màu nền của các LinearLayout con trong controlButtonsTop thành #111111
        findViewById<LinearLayout>(R.id.btn_mirroring)?.setBackgroundColor(Color.parseColor("#111111"))
        findViewById<LinearLayout>(R.id.btn_pip)?.setBackgroundColor(Color.parseColor("#111111"))
        controlButtonsTop.getChildAt(2)?.setBackgroundColor(Color.parseColor("#111111")) // Favorite
        controlButtonsTop.getChildAt(3)?.setBackgroundColor(Color.parseColor("#111111")) // Lock

        // Điều chỉnh constraint của controlButtonsTop để neo vào đáy màn hình
        val controlButtonsParams = controlButtonsTop.layoutParams as ConstraintLayout.LayoutParams
        controlButtonsParams.topToBottom = ConstraintLayout.LayoutParams.UNSET // Bỏ ràng buộc top cũ
        controlButtonsParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID // Neo vào đáy parent
        controlButtonsParams.bottomMargin = resources.getDimensionPixelSize(R.dimen.fullscreen_control_margin_bottom) * 2 // 32dp
        controlButtonsTop.layoutParams = controlButtonsParams

        // Hiển thị DefaultTimeBar từ linearLayoutControlBottom
        val timeBar = linearLayoutControlBottom.findViewById<com.google.android.exoplayer2.ui.DefaultTimeBar>(R.id.exo_progress)
        timeBar?.visibility = View.VISIBLE

        // Đảm bảo touchOverlay không chặn sự kiện
        touchOverlay.isClickable = false // Tạm thời vô hiệu hóa để không chặn controller

        // Ẩn controls sau 3 giây nếu không thao tác
        controlHideHandler.removeCallbacks(hideControlRunnable)
        controlHideHandler.postDelayed(hideControlRunnable, CONTROL_HIDE_DELAY)
    }

    private fun hideControlsInFullscreen() {
        Log.d(TAG, "hideControlsInFullscreen: Hiding controls")
        isControlVisible = false

        // Ẩn tất cả các thành phần từ custom_controller_channel.xml
        btnBack.visibility = View.GONE
        tvTitle.visibility = View.GONE
        controlButtonsTop.visibility = View.GONE
        loadingProgress.visibility = View.GONE

        // Ẩn controller từ custom_controller.xml
        playerView.useController = false // Tắt controller hoàn toàn
        playerView.hideController() // Đảm bảo controller ẩn

        // Khôi phục màu gốc cho custom_controller_channel.xml
        tvTitle.setTextColor(Color.parseColor("#000000")) // Màu gốc của tvTitle
        findViewById<TextView>(R.id.txt_mirroring)?.setTextColor(Color.parseColor("#3F484A"))
        findViewById<TextView>(R.id.txt_pip)?.setTextColor(Color.parseColor("#3F484A"))
        findViewById<TextView>(R.id.txt_fav)?.setTextColor(Color.parseColor("#3F484A"))
        findViewById<TextView>(R.id.txt_lock)?.setTextColor(Color.parseColor("#3F484A"))

        // Khôi phục màu gốc cho custom_controller.xml
        exoPosition.setTextColor(Color.parseColor("#FFFFFF")) // Màu gốc trong custom_controller.xml
        exoDuration.setTextColor(Color.parseColor("#CBCDC8")) // Màu gốc trong custom_controller.xml

        // Xóa màu tint của icon từ custom_controller_channel.xml
        findViewById<ImageView>(R.id.img_mirroring)?.clearColorFilter()
        findViewById<ImageView>(R.id.img_pip)?.clearColorFilter()
        findViewById<ImageView>(R.id.img_fav)?.clearColorFilter()
        findViewById<ImageView>(R.id.img_lock)?.clearColorFilter()
        btnBack.clearColorFilter()

        // Xóa màu tint của icon từ custom_controller.xml
        exoPlay.clearColorFilter()
        exoPause.clearColorFilter()
        exoRew.clearColorFilter()
        exoFfwd.clearColorFilter()
        imageViewFullScreen.clearColorFilter()

        // Khôi phục màu nền gốc của các LinearLayout con trong controlButtonsTop
        findViewById<LinearLayout>(R.id.btn_mirroring)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
        findViewById<LinearLayout>(R.id.btn_pip)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
        controlButtonsTop.getChildAt(2)?.setBackgroundResource(R.drawable.bg_menu_playcontrol) // Favorite
        controlButtonsTop.getChildAt(3)?.setBackgroundResource(R.drawable.bg_menu_playcontrol) // Lock

        // Khôi phục constraint gốc của controlButtonsTop
        val controlButtonsParams = controlButtonsTop.layoutParams as ConstraintLayout.LayoutParams
        controlButtonsParams.topToBottom = R.id.playerView // Ràng buộc lại với PlayerView
        controlButtonsParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET // Bỏ neo đáy
        controlButtonsParams.bottomMargin = 0
        controlButtonsParams.topMargin = resources.getDimensionPixelSize(R.dimen.control_margin_default) // 24dp
        controlButtonsTop.layoutParams = controlButtonsParams

        // Bật lại touchOverlay để người dùng có thể chạm lần nữa
        touchOverlay.isClickable = true

        controlHideHandler.removeCallbacks(hideControlRunnable)
    }

    private fun setupFavorite() {
        Log.d(TAG, "setupFavorite: Setting up favorite button listener for channel: ${channel.name}")
        val favoriteLayout = controlButtonsTop.getChildAt(2) as? LinearLayout
        val favoriteIcon = favoriteLayout?.findViewById<ImageView>(0)
        favoriteIcon?.setOnClickListener {
            Log.d(TAG, "setupFavorite: Toggling favorite for channel: ${channel.name}")
            channelsProvider.toggleFavorite(channel)
            favoriteIcon.setImageResource(if (channel.isFavorite) R.drawable.fav_on_channel else R.drawable.ic_fav)
            channel = channel.copy(isFavorite = !channel.isFavorite)
            Log.d(TAG, "setupFavorite: Updated favorite status to ${channel.isFavorite}")
        }
    }

    private fun setupPip() {
        Log.d(TAG, "setupPip: Setting up PiP button listener")
        if (Build.VERSION.SDK_INT >= MIN_PIP_API) {
            btnPip.setOnClickListener {
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

                    val aspectRatio = Rational(width, height) // Giữ tỉ lệ của PlayerView hiện tại
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

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
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
            params.topToBottom = ConstraintLayout.LayoutParams.UNSET // Bỏ constraint top
            params.bottomToTop = ConstraintLayout.LayoutParams.UNSET // Bỏ constraint bottom
            params.topMargin = 0
            playerView.layoutParams = params
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
            params.height = resources.getDimensionPixelSize(R.dimen.player_height) // 200dp
            params.topToBottom = R.id.tvTitle
            params.bottomToTop = R.id.linearLayoutControlUp
            params.topMargin = resources.getDimensionPixelSize(R.dimen.player_margin_top) // 120dp
            playerView.layoutParams = params
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
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
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
        Log.d(TAG, "onResume: Resuming player")
        if (!::player.isInitialized) {
            setupPlayer()
        }
        if (Util.SDK_INT <= 23 || !isPlayerReady) {
            player.playWhenReady = true
        }
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
        controlHideHandler.removeCallbacks(hideControlRunnable) // Xóa handler khi activity hủy
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