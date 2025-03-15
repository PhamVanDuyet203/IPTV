package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
import androidx.activity.result.contract.ActivityResultContracts
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
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.model.Channel
import com.iptv.smart.player.player.streamtv.live.watch.provider.ChannelsProvider
import android.graphics.PorterDuff
import android.provider.Settings
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import com.admob.max.dktlibrary.AppOpenManager
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.INTER_BACK_PLAY_TO_LIST
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.gone
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.visible
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
import com.iptv.smart.player.player.streamtv.live.watch.databinding.CustomControllerChannelBinding
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig
import com.iptv.smart.player.player.streamtv.live.watch.utils.Common

class PlayerActivity : BaseActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var btnBack: ImageView
    private lateinit var exoPlay: ImageView
    private lateinit var exoPause: ImageView
    private lateinit var exoRew: ImageView
    private lateinit var exoFfwd: ImageView
    private lateinit var txtMirroring: TextView
    private lateinit var txtPip: TextView
    private lateinit var txtFav: TextView
    private lateinit var txtLock: TextView
    private lateinit var linearLayoutControlUp: LinearLayout
    private lateinit var linearLayoutControlBottom: LinearLayout
    private lateinit var controlButtonsTop: LinearLayout
    private lateinit var exoPosition: TextView
    private lateinit var exoDuration: TextView
    private lateinit var imageViewFullScreen: ImageView
    private lateinit var btnPip: LinearLayout
    private lateinit var channel: Channel
    private lateinit var channelsProvider: ChannelsProvider
    private lateinit var btnMirroring: LinearLayout
    private lateinit var toolPlayer: View
    private var playbackPosition = 0L
    private var isPlayerReady = false
    private var isFullScreen = false
    private var isLock = false
    private var isInPictureInPictureMode = false
    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "hnvytnynvynynyj"
    private lateinit var tvTitle: TextView
    private val binding by lazy { CustomControllerChannelBinding.inflate(layoutInflater) }
    private var isControlVisible = false
    private val controlHideHandler = Handler(Looper.getMainLooper())
    private val hideControlRunnable = Runnable {
        if (!isLock && isFullScreen ){
            binding.controlButtonsTop1.visibility = View.INVISIBLE
            btnBack.visibility = View.INVISIBLE
            tvTitle.visibility = View.INVISIBLE
        }
        playerView.hideController()
    }
    private val CONTROL_HIDE_DELAY = 3000L
    private lateinit var frHome: FrameLayout
    private lateinit var vLine: View
    private var wasPlayingBeforePause = false
    private var noInternetDialog: AlertDialog? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isLocalFile = false


    companion object {
        private const val INCREMENT_MILLIS = 3000L
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
        Log.d(
            TAG,
            "onCreate: Starting PlayerActivity for channel: ${intent.getParcelableExtra<Channel>("channel")?.name}"
        )
        setContentView(binding.root)

        channel =
            savedInstanceState?.getParcelable("channel") ?: intent.getParcelableExtra("channel")
                    ?: run {
                Log.e(TAG, "onCreate: Channel not found in Intent, finishing activity")
                finish()
                return
            }

        channelsProvider = ViewModelProvider(this).get(ChannelsProvider::class.java)
        Log.d(TAG, "onCreate: Initializing ChannelsProvider")
        channelsProvider.init(this)

        setFindViewById()
        isLocalFile = Uri.parse(channel.streamUrl).scheme in listOf("content", "file")
        setupNetworkMonitoring()
        setLockScreen()
        setFullScreen()
        setupFavorite()

        if (Build.VERSION.SDK_INT >= MIN_PIP_API) {
            setupPip()
        }

        binding.btnMirroring1.gone()
        binding.btnPip1.gone()



        Log.d(TAG, "onCreate: Adding channel to recent: ${channel.name}")
        channelsProvider.addToRecent(channel)
        controlButtonsTop.visibility = View.VISIBLE
        binding.controlButtonsTop1.visibility = View.GONE

        channelsProvider.channels.observe(this) { channels ->
            channels.find { it.streamUrl == channel.streamUrl }?.let { updatedChannel ->
                channel = updatedChannel
                updateFavoriteIcon()
                tvTitle.text = channel.name
                tvTitle.isSelected = true
                Log.d(
                    TAG,
                    "Channels observed: Synced Favorite: ${channel.isFavorite}, Name: ${channel.name}"
                )
            }
        }

        channelsProvider.fetchChannelsFromRoom()
    }


    private fun setupNetworkMonitoring() {
        if (isLocalFile) return

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    Log.d(TAG, "Network available")
                    dismissNoInternetDialog()
                    if (!::player.isInitialized || !player.isPlaying) {
                        setupPlayer()
                    }
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    Log.d(TAG, "Network lost")
                    if (::player.isInitialized) {
                        wasPlayingBeforePause = player.isPlaying
                        player.playWhenReady = false
                    }
                    showNoInternetDialog()
                }
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        networkCallback?.let {
            connectivityManager?.registerNetworkCallback(networkRequest, it)
        }
    }



    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkInternetConnection() {
        if (isLocalFile) {
            setupPlayer()
            return // Không kiểm tra mạng cho file cục bộ
        }

        if (!isInternetAvailable()) {
            showNoInternetDialog()
        } else {
            dismissNoInternetDialog()
            setupPlayer()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun showNoInternetDialog() {
        if (noInternetDialog == null || !noInternetDialog!!.isShowing) {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_no_internet, null)
            noInternetDialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            val width = (320 * resources.displayMetrics.density).toInt()
            val height = (312 * resources.displayMetrics.density).toInt()
            noInternetDialog?.window?.apply {
                setLayout(width, height)
                setBackgroundDrawableResource(R.drawable.bg_no_connect)
            }

            dialogView.findViewById<TextView>(R.id.btn_Connect).setOnClickListener {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }

            noInternetDialog?.show()
        }
    }

    private fun dismissNoInternetDialog() {
        noInternetDialog?.takeIf { it.isShowing }?.dismiss()
    }

    private fun startAds() {
        when (RemoteConfig.INTER_BACK_PLAY_TO_LIST_050325) {
            "0" -> {
                finish()
            }
            else -> {
                Common.countInterBackPLay++
                if (Common.countInterBackPLay % RemoteConfig.INTER_BACK_PLAY_TO_LIST_050325.toInt() == 0) {
                    AdsManager.loadAndShowInter(this, INTER_BACK_PLAY_TO_LIST) {
                        finish()
                    }
                } else {
                    finish()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setFindViewById() {
        Log.d(TAG, "setFindViewById: Initializing UI components")
        playerView = findViewById(R.id.playerView)
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

        txtMirroring = findViewById(R.id.txt_mirroring)
        txtPip = findViewById(R.id.txt_pip)
        txtFav = findViewById(R.id.txt_fav)
        txtLock = findViewById(R.id.txt_lock)
        txtMirroring.isSelected = true
        txtPip.isSelected = true
        txtFav.isSelected = true
        txtLock.isSelected = true

        btnMirroring = findViewById(R.id.btn_mirroring)
        frHome = findViewById(R.id.fr_home)
        vLine = findViewById(R.id.line)
        Log.d(TAG, "setFindViewById: Set tvTitle to ${channel.name}")

        btnMirroring.setOnClickListener {
            if (isLocalFile || isInternetAvailable()) wifiDisplay() else showNoInternetDialog()
            AppOpenManager.getInstance().enableAppResumeWithActivity(PlayerActivity::class.java)
        }

        binding.btnMirroring1.setOnClickListener {
            if (isLocalFile || isInternetAvailable()) wifiDisplay() else showNoInternetDialog()
            AppOpenManager.getInstance().enableAppResumeWithActivity(PlayerActivity::class.java)
        }

        btnBack.setOnClickListener {
            Log.d(TAG, "btnBack clicked")
            if (isLock) {
                return@setOnClickListener
            }
            if (isFullScreen && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Log.d(TAG, "btnBack: Exiting full screen mode")
                imageViewFullScreen.performClick()
            } else if (Build.VERSION.SDK_INT >= MIN_PIP_API && isInPictureInPictureMode) {
                Log.d(TAG, "btnBack: In PiP mode, moving to background")
                moveTaskToBack(true)
            } else {
                startAds()
            }
        }
    }

    fun wifiDisplay() {
        if (::player.isInitialized) {
            wasPlayingBeforePause = player.isPlaying
            player.playWhenReady = false
        }
        try {
            val intent = Intent("android.settings.WIFI_DISPLAY_SETTINGS")
            startActivityForResult(intent, 169)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            try {
                startActivity(packageManager.getLaunchIntentForPackage("com.samsung.wfd.LAUNCH_WFD_PICKER_DLG"))
            } catch (e2: Exception) {
                try {
                    startActivityForResult(Intent("android.settings.CAST_SETTINGS"), 169)
                } catch (e3: Exception) {
                    Toast.makeText(applicationContext, "Device not supported", Toast.LENGTH_LONG).show()
                }
            }
        } catch (ex: Exception) {
            Toast.makeText(applicationContext, "Device not supported", Toast.LENGTH_LONG).show()
        }
        AppOpenManager.getInstance().enableAppResumeWithActivity(PlayerActivity::class.java)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 169) {
            Log.d(TAG, "onActivityResult: Returned from Wi-Fi Display settings")
            if (::player.isInitialized && wasPlayingBeforePause) {
                player.playWhenReady = true
            }
        }
    }

    private fun setupFavorite() {
        Log.d(
            TAG, "setupFavorite: Setting up favorite button listener for channel: ${channel.name}"
        )
        val favoriteLayout = controlButtonsTop.getChildAt(2) as? LinearLayout
        val favoriteIcon = favoriteLayout?.findViewById<ImageView>(R.id.img_fav)
        val favoriteBtn = favoriteLayout?.findViewById<LinearLayout>(R.id.btn_fav)
        updateFavoriteIcon(favoriteIcon)

        favoriteBtn?.setOnClickListener {
            Log.d(TAG, "setupFavorite: Toggling favorite for channel: ${channel.name}")
            channelsProvider.toggleFavorite(channel, true)
            updateFavoriteIcon(favoriteIcon)
        }
    }

    private fun updateFavoriteIcon(
        favoriteIcon: ImageView? = if (isFullScreen)
            binding.controlButtonsTop1.getChildAt(2)?.findViewById(R.id.img_fav1)
        else controlButtonsTop.getChildAt(2)?.findViewById(R.id.img_fav)
    ) {
        val updatedChannel =
            channelsProvider.channels.value?.find { it.streamUrl == channel.streamUrl }
        channel = updatedChannel ?: channel
        favoriteIcon?.setImageResource(
            if (channel.isFavorite) R.drawable.fav_on_channel else R.drawable.ic_fav
        )
        Log.d(TAG, "updateFavoriteIcon: Updated icon to Favorite: ${channel.isFavorite}")
    }

    private fun showErrorDialog() {
        if (!this.isFinishing && !isDestroyed) {
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_unavailable, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()

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

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupPlayer() {
        if (!isLocalFile && !isInternetAvailable()) {
            showNoInternetDialog()
            return
        }

        Log.d(TAG, "setupPlayer: Setting up ExoPlayer for stream URL: ${channel.streamUrl}")
        if (::player.isInitialized) {
            player.release()
        }
        player = ExoPlayer.Builder(this).setSeekBackIncrementMs(INCREMENT_MILLIS)
            .setSeekForwardIncrementMs(INCREMENT_MILLIS).build().also { exoPlayer ->
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
                        Log.d(
                            TAG,
                            "setupPlayer: Detected local file URI (MP4), using ProgressiveMediaSource"
                        )
                        ProgressiveMediaSource.Factory(
                            DefaultDataSource.Factory(this)
                        ).createMediaSource(mediaItem)
                    }

                    uri.scheme == "http" || uri.scheme == "https" -> {
                        Log.d(TAG, "setupPlayer: Detected streaming URL, using HlsMediaSource")
                        HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
                            .createMediaSource(mediaItem)
                    }

                    else -> {
                        Log.w(
                            TAG,
                            "setupPlayer: Unsupported URI scheme: ${uri.scheme}, defaulting to HlsMediaSource"
                        )
                        HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
                            .createMediaSource(mediaItem)
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
                        Log.e(
                            TAG,
                            "onPlayerError: Error playing video - ${error.errorCodeName}, ${error.message}"
                        )
                        loadingProgress.visibility = View.GONE
                        showErrorDialog()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d(TAG, "onPlaybackStateChanged: State = $playbackState")
                        when (playbackState) {
                            Player.STATE_READY -> {
                                loadingProgress.visibility = View.GONE
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

        val controlButtons = if (isFullScreen) binding.controlButtonsTop1 else controlButtonsTop
        val lockLayout = controlButtons.findViewById<LinearLayout>(if (isFullScreen) R.id.btn_lock1 else R.id.btn_lock)
        val lockIcon = controlButtons.findViewById<ImageView>(if (isFullScreen) R.id.img_lock1 else R.id.img_lock)
        val lockText = controlButtons.findViewById<TextView>(if (isFullScreen) R.id.txt_lock1 else R.id.txt_lock)

        if (lock) {
            linearLayoutControlUp.visibility = View.INVISIBLE
            linearLayoutControlBottom.visibility = View.INVISIBLE
            controlButtons.visibility = View.VISIBLE

            for (i in 0 until controlButtons.childCount) {
                val child = controlButtons.getChildAt(i)
                if (child != lockLayout) {
                    originalLayoutParams[child] = child.layoutParams as LinearLayout.LayoutParams
                    child.visibility = View.GONE
                }
                else {
                    child.visibility = View.VISIBLE
                }
            }

            lockIcon.setImageDrawable(
                ContextCompat.getDrawable(applicationContext, R.drawable.ic_lock_screen)
            )
            lockText.text = getString(R.string.screen_lock_long_press_to_unlock)
            if (!isFullScreen)
            {
                lockLayout.setBackgroundResource(R.drawable.bg_lock_normal)
                lockText.setTextColor(Color.parseColor("#3F484A"))
            } else
            {
                btnBack.gone()
                tvTitle.gone()
                playerView.useController = false
                playerView.hideController()
                lockLayout.setBackgroundResource(R.drawable.bg_menu_playcontrol1)
                lockText.gone()
            }

        } else {
            linearLayoutControlUp.visibility =  View.VISIBLE
            linearLayoutControlBottom.visibility = View.VISIBLE
            btnBack.visible()
            tvTitle.visible()
            lockText.visible()
            playerView.useController = true
            playerView.showController()
            controlButtons.visibility = View.VISIBLE

            val margin4dp = resources.getDimensionPixelSize(R.dimen.button_margin)

            for (i in 0 until controlButtons.childCount) {
                val child = controlButtons.getChildAt(i)
                child.visibility = View.VISIBLE
                originalLayoutParams[child]?.let {
                    child.layoutParams = it
                } ?: run {
                    val params = child.layoutParams as LinearLayout.LayoutParams
                    params.setMargins(margin4dp, params.topMargin, margin4dp, params.bottomMargin)
                    child.layoutParams = params
                }
            }

            lockIcon.setImageDrawable(
                ContextCompat.getDrawable(applicationContext, R.drawable.ic_lock)
            )
            lockText.text = "Lock"
            if (!isFullScreen) {
                lockLayout.setBackgroundResource(R.drawable.bg_menu_playcontrol)
                lockText.setTextColor(Color.parseColor("#3F484A"))
            } else {
                lockLayout.setBackgroundResource(R.drawable.bg_menu_playcontrol1)
                lockText.setTextColor(Color.WHITE)
            }


            controlButtons.requestLayout()

        }

        Log.d(TAG, "lockScreen: UI state - controlButtons childCount=${controlButtons.childCount}")
    }

    private fun setLockScreen() {
        Log.d(TAG, "setLockScreen: Setting up lock button listener")
        val lockButton = findViewById<LinearLayout>(R.id.btn_lock)
        val lockIcon = findViewById<ImageView>(R.id.img_lock)

        lockButton.setOnClickListener {
            if (!isLock) {
                isLock = true
                Log.d(TAG, "setLockScreen: Lock enabled")
                lockIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext, R.drawable.ic_lock_screen
                    )
                )
                lockScreen(true)
            }
        }

        lockButton.setOnLongClickListener {
            if (isLock) {
                Log.d(TAG, "setLockScreen: Long press detected, unlocking")
                isLock = false

                lockIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext, R.drawable.ic_lock
                    )
                )
                lockScreen(false)
                true
            } else {
                false
            }
        }
    }

    private fun showBottomTool(){
        if (isFullScreen && !isInPictureInPictureMode){
            Log.d(TAG, "onResume: ")
            binding.controlButtonsTop1.visible()
            btnBack.visible()
            tvTitle.visible()
            tvTitle.setTextColor(Color.WHITE)
            btnBack.setColorFilter(Color.WHITE)
            handler.removeCallbacks(hideControlRunnable)
            handler.postDelayed(hideControlRunnable,3000)
        }
        if (isInPictureInPictureMode){
            btnBack.gone()
            tvTitle.gone()
            playerView.hideController()
        }


    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SourceLockedOrientationActivity")
    private fun setFullScreen() {
        Log.d(TAG, "setFullScreen: Setting up full screen button listener")
        imageViewFullScreen.setOnClickListener {
            Log.d(TAG, "playerView.useController = ${playerView.useController}, isControllerVisible = ${playerView.isControllerVisible}")
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

            val toolbar = playerView.findViewById<View>(R.id.toolbar_player)
            val mainCustomControl = findViewById<RelativeLayout>(R.id.mainCustomControl)
            val playserView = findViewById<PlayerView>(R.id.playerView)
            playserView.setOnClickListener {
                linearLayoutControlUp.visible()
                linearLayoutControlBottom.visible()
                showBottomTool()
            }
            mainCustomControl.setOnClickListener {
                linearLayoutControlUp.gone()
                linearLayoutControlBottom.gone()
                showBottomTool()
            }
            if (isFullScreen) {
                showBottomTool()
                binding.btnMirroring1.visible()
                binding.btnPip1.visible()
                binding.root.setBackgroundColor(getColor(R.color.black))
                btnBack.visibility = View.VISIBLE
                tvTitle.visibility = View.VISIBLE

                binding.line.gone()
                controlButtonsTop.visibility = View.GONE
                loadingProgress.visibility = View.GONE
                playerView.useController = true
                playerView.showController()
                isControlVisible = true

                val params = playerView.layoutParams as ConstraintLayout.LayoutParams
                params.height = ConstraintLayout.LayoutParams.MATCH_PARENT
                params.topToBottom = ConstraintLayout.LayoutParams.UNSET
                params.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                params.topMargin = 0
                params.bottomMargin = 0
                playerView.layoutParams = params

                toolbar.visible()


                setupControlsForFullscreen()
            } else {
                showBottomTool()
                loadingProgress.visibility = View.GONE
                binding.controlButtonsTop1.visibility = View.GONE
                binding.root.setBackgroundColor(getColor(R.color.white))
                btnBack.visibility = View.VISIBLE
                btnBack.clearColorFilter()
                tvTitle.visibility = View.VISIBLE
                tvTitle.setTextColor(Color.BLACK)
                linearLayoutControlUp.visibility = View.VISIBLE
                linearLayoutControlBottom.visibility = View.VISIBLE
                controlButtonsTop.visibility = View.VISIBLE
                binding.line.visible()
                loadingProgress.visibility =
                    if (player.isLoading && !player.isPlaying) View.VISIBLE else View.GONE
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

                toolbar.gone()


                tvTitle.setTextColor(Color.parseColor("#000000"))
                btnBack.clearColorFilter()

                val controlButtonsParams =
                    controlButtonsTop.layoutParams as ConstraintLayout.LayoutParams
                controlButtonsParams.topToBottom = R.id.playerView
                controlButtonsParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                controlButtonsParams.bottomMargin = 0
                controlButtonsParams.topMargin =
                    resources.getDimensionPixelSize(R.dimen.control_margin_default)
                controlButtonsTop.layoutParams = controlButtonsParams

                findViewById<LinearLayout>(R.id.btn_mirroring)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
                findViewById<LinearLayout>(R.id.btn_pip)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
                controlButtonsTop.getChildAt(2)
                    ?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
                controlButtonsTop.getChildAt(3)
                    ?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
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

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupControlsForFullscreen() {
        binding.btnMirroring1.setOnClickListener {
            if (isLocalFile || isInternetAvailable()) wifiDisplay() else showNoInternetDialog()
        }
        binding.btnMirroring1.setBackgroundResource(R.drawable.bg_menu_playcontrol1)
//
//        if (Build.VERSION.SDK_INT >= MIN_PIP_API) {
//            binding.btnPip1.setOnClickListener {
//                enterPictureInPictureModeIfAvailable()
//            }
//        }
        binding.btnPip1.setBackgroundResource(R.drawable.bg_menu_playcontrol1)

        val favoriteLayout = binding.controlButtonsTop1.getChildAt(2) as? LinearLayout
        val favoriteIcon = favoriteLayout?.findViewById<ImageView>(R.id.img_fav1)
        val favoriteBtn = favoriteLayout?.findViewById<LinearLayout>(R.id.btn_fa)
        updateFavoriteIcon(favoriteIcon)
        favoriteBtn?.setBackgroundResource(R.drawable.bg_menu_playcontrol1)

        favoriteBtn?.setOnClickListener {
            channelsProvider.toggleFavorite(channel, true)
            updateFavoriteIcon(favoriteIcon)
        }

        val lockLayout = binding.controlButtonsTop1.getChildAt(3) as? LinearLayout
        val lockIcon = lockLayout?.findViewById<ImageView>(R.id.img_lock1)
        val lockButton = lockLayout?.findViewById<LinearLayout>(R.id.btn_lock1)
        lockLayout?.setBackgroundResource(R.drawable.bg_menu_playcontrol1)

        lockButton?.setOnClickListener {
            if (!isLock) {
                isLock = true
                lockIcon?.setImageDrawable(
                    ContextCompat.getDrawable(applicationContext, R.drawable.ic_lock_screen)
                )
                lockScreen(true)
            }
        }

        lockButton?.setOnLongClickListener {
            if (isLock) {
                isLock = false
                lockIcon?.setImageDrawable(
                    ContextCompat.getDrawable(applicationContext, R.drawable.ic_lock)
                )
                lockScreen(false)
                true
            } else {
                false
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
        Log.d(TAG, "playerView.useController = ${playerView.useController}, isControllerVisible = ${playerView.isControllerVisible}")
        isControlVisible = true

        binding.controlButtonsTop1.visibility = View.VISIBLE

        btnBack.visibility = if (isLock) View.GONE else View.VISIBLE
        tvTitle.visibility = if (isLock) View.GONE else View.VISIBLE
        playerView.useController = true
        playerView.showController()
        controlButtonsTop.visibility = View.GONE


        tvTitle.setTextColor(Color.parseColor("#FFFFFF"))
        findViewById<TextView>(R.id.txt_mirroring1)?.setTextColor(Color.parseColor("#FFFFFF"))
        findViewById<TextView>(R.id.txt_pip1)?.setTextColor(Color.parseColor("#FFFFFF"))
        findViewById<TextView>(R.id.txt_fav1)?.setTextColor(Color.parseColor("#FFFFFF"))
        findViewById<TextView>(R.id.txt_lock1)?.setTextColor(Color.parseColor("#FFFFFF"))

        exoPosition.setTextColor(Color.parseColor("#FFFFFF"))
        exoDuration.setTextColor(Color.parseColor("#FFFFFF"))

        findViewById<ImageView>(R.id.img_mirroring1)?.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.img_pip1)?.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.img_fav1)?.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        findViewById<ImageView>(R.id.img_lock1)?.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        btnBack.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)

        exoPlay.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        exoPause.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        exoRew.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        exoFfwd.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        imageViewFullScreen.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)

        findViewById<LinearLayout>(R.id.btn_mirroring1)?.setBackgroundResource(R.drawable.bg_menu_playcontrol1)
        findViewById<LinearLayout>(R.id.btn_pip1)?.setBackgroundResource(R.drawable.bg_menu_playcontrol1)
        binding.controlButtonsTop1.getChildAt(2)?.setBackgroundResource(R.drawable.bg_menu_playcontrol1)
        binding.controlButtonsTop1.getChildAt(3)?.setBackgroundResource(R.drawable.bg_menu_playcontrol1)


        val timeBar = linearLayoutControlBottom.findViewById<com.google.android.exoplayer2.ui.DefaultTimeBar>(R.id.exo_progress)
        timeBar?.visibility = View.VISIBLE
        showBottomTool()


        controlHideHandler.removeCallbacks(hideControlRunnable)
        controlHideHandler.postDelayed(hideControlRunnable, CONTROL_HIDE_DELAY)
    }

    private fun hideControlsInFullscreen() {
        Log.d(TAG, "hideControlsInFullscreen: Hiding controls")
        Log.d("TAGffffffffffffffffffff", "playerView.useController = ${playerView.useController}, isControllerVisible = ${playerView.isControllerVisible}")
        isControlVisible = false

        btnBack.visibility = View.GONE
        tvTitle.visibility = View.GONE
        binding.controlButtonsTop1.visibility = View.GONE
        loadingProgress.visibility = View.GONE

        playerView.useController = true
        playerView.hideController()

        tvTitle.setTextColor(Color.parseColor("#000000"))
        findViewById<TextView>(R.id.txt_mirroring1)?.setTextColor(Color.parseColor("#3F484A"))
        findViewById<TextView>(R.id.txt_pip1)?.setTextColor(Color.parseColor("#3F484A"))
        findViewById<TextView>(R.id.txt_fav1)?.setTextColor(Color.parseColor("#3F484A"))
        findViewById<TextView>(R.id.txt_lock1)?.setTextColor(Color.parseColor("#3F484A"))

        exoPosition.setTextColor(Color.parseColor("#FFFFFF"))
        exoDuration.setTextColor(Color.parseColor("#CBCDC8"))

        findViewById<ImageView>(R.id.img_mirroring1)?.clearColorFilter()
        findViewById<ImageView>(R.id.img_pip1)?.clearColorFilter()
        findViewById<ImageView>(R.id.img_fav1)?.clearColorFilter()
        findViewById<ImageView>(R.id.img_lock1)?.clearColorFilter()
        btnBack.clearColorFilter()

        exoPlay.clearColorFilter()
        exoPause.clearColorFilter()
        exoRew.clearColorFilter()
        exoFfwd.clearColorFilter()
        imageViewFullScreen.clearColorFilter()

        findViewById<LinearLayout>(R.id.btn_mirroring1)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
        findViewById<LinearLayout>(R.id.btn_pip1)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
        binding.controlButtonsTop1.getChildAt(2)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
        binding.controlButtonsTop1.getChildAt(3)?.setBackgroundResource(R.drawable.bg_menu_playcontrol)


        controlHideHandler.removeCallbacks(hideControlRunnable)
    }

    private fun setupPip() {
        if (Build.VERSION.SDK_INT >= MIN_PIP_API) {
            btnPip.setOnClickListener {
                enterPictureInPictureModeIfAvailable()
            }
            binding.btnPip1.setOnClickListener {

                enterPictureInPictureModeIfAvailable()

            }
        } else {
            btnPip.visibility = View.GONE }
    }

    private fun isPipPermissionGranted(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                android.os.Process.myUid(),
                packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } else {
            true
        }
    }

    private val pipPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (isPipPermissionGranted()) {
            if (::player.isInitialized && wasPlayingBeforePause) {
                player.playWhenReady = true
            }
            enterPictureInPictureModeIfAvailable()
        } else {
            Toast.makeText(this, "PiP permission denied", Toast.LENGTH_SHORT).show()
            if (::player.isInitialized && wasPlayingBeforePause) {
                player.playWhenReady = true
            }
        }
    }

    private fun requestPipPermission() {
        if (::player.isInitialized) {
            wasPlayingBeforePause = player.isPlaying
            player.playWhenReady = false
        }

        val intent = Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS").apply {
            data = Uri.parse("package:$packageName")
        }

        try {
            pipPermissionLauncher.launch(intent)
            Toast.makeText(
                this,
                "Please enable Picture-in-Picture for this app",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: ActivityNotFoundException) {
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            try {
                pipPermissionLauncher.launch(fallbackIntent)
                Toast.makeText(
                    this,
                    "Go to Picture-in-Picture settings and enable it for $packageName",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: ActivityNotFoundException) {
                val finalFallbackIntent = Intent(Settings.ACTION_SETTINGS)
                pipPermissionLauncher.launch(finalFallbackIntent)
                Toast.makeText(
                    this,
                    "Go to Apps > $packageName > Picture-in-Picture and enable it",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun enterPictureInPictureModeIfAvailable() {
        if (Build.VERSION.SDK_INT < MIN_PIP_API) return
        Log.d("dgdfgdfgfgfg", "enterPictureInPictureModeIfAvailable: ")
        binding.btnBack.gone()
        binding.btnBack.gone()
        playerView.hideController()
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            Toast.makeText(this, "PiP not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }

        if (isInPictureInPictureMode) return

        if (!isPipPermissionGranted()) {
            requestPipPermission()
            return
        }

        playerView.post {
            val width = playerView.measuredWidth
            val height = playerView.measuredHeight
            if (width <= 0 || height <= 0) {
                Toast.makeText(this, "Player not ready for PiP", Toast.LENGTH_SHORT).show()
                return@post
            }

            val aspectRatio = Rational(16, 9)
            val params = PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build()
            if (enterPictureInPictureMode(params)) {
                isInPictureInPictureMode = true
                if (::player.isInitialized && wasPlayingBeforePause) {
                    player.playWhenReady = true
                }
            } else {
                Toast.makeText(this, "Failed to enter PiP mode", Toast.LENGTH_SHORT).show()
            }
        }
        AppOpenManager.getInstance().enableAppResumeWithActivity(PlayerActivity::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean, @NonNull newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        Log.d(TAG, "onPictureInPictureModeChanged: PiP mode changed to $isInPictureInPictureMode")
        this.isInPictureInPictureMode = isInPictureInPictureMode

        if (isInPictureInPictureMode) {
            btnBack.visibility = View.GONE
            tvTitle.visibility = View.GONE
            toolPlayer = findViewById(R.id.toolbar_player)
            toolPlayer.visibility = View.GONE
            linearLayoutControlUp.visibility = View.GONE
            controlButtonsTop.visibility = View.GONE
            loadingProgress.visibility = View.GONE
            playerView.useController = false
            playerView.visibility = View.VISIBLE
            binding.frHome.visibility = View.GONE
            binding.line.visibility = View.GONE

            val params = playerView.layoutParams as ConstraintLayout.LayoutParams
            params.height = ConstraintLayout.LayoutParams.MATCH_PARENT
            params.topToBottom = ConstraintLayout.LayoutParams.UNSET
            params.bottomToTop = ConstraintLayout.LayoutParams.UNSET
            params.topMargin = 0
            params.bottomMargin = 0
            playerView.layoutParams = params
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            btnBack.visibility = View.VISIBLE
            tvTitle.visibility = View.VISIBLE
            btnBack.clearColorFilter()
            tvTitle.setTextColor(Color.BLACK)
            linearLayoutControlUp.visibility = View.VISIBLE
            controlButtonsTop.visibility = View.VISIBLE
            binding.controlButtonsTop1.visibility = View.GONE
            loadingProgress.visibility = if (player.isLoading) View.VISIBLE else View.GONE

            playerView.useController = true
            playerView.visibility = View.VISIBLE

            binding.frHome.visibility = View.VISIBLE
            binding.line.visibility = View.VISIBLE

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

            binding.root.setBackgroundColor(getColor(R.color.white))

            imageViewFullScreen.setImageDrawable(
                ContextCompat.getDrawable(
                    applicationContext, R.drawable.ic_baseline_fullscreen
                )
            )
            isFullScreen = false

            if (::player.isInitialized && wasPlayingBeforePause) {
                player.playWhenReady = true
            }

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

            Log.d(TAG, "onConfigurationChanged: Entered full screen mode")
            if (!isControlVisible) {
                showControlsInFullscreen()
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

            Log.d(TAG, "onConfigurationChanged: Returned to portrait mode")
            isControlVisible = false
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
        if (RemoteConfig.ADS_PLAY_CONTROL_050325 == "1") {
            AdsManager.showAdsBanner(this, AdsManager.BANNER_PLAY_CONTROL, binding.frHome, binding.line)
        } else if (RemoteConfig.ADS_PLAY_CONTROL_050325 == "2") {
            AdsManager.showAdBannerCollapsible(this, AdsManager.BANNER_COLLAP_PLAY_CONTROL, binding.frHome, binding.line)
        } else {
            binding.frHome.gone()
            binding.line.gone()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        super.onResume()
        checkInternetConnection()
        AppOpenManager.getInstance().disableAppResumeWithActivity(PlayerActivity::class.java)
        if (isLock) {
            Log.d(TAG, "onResume: ")
            binding.btnLock1.visible()
            linearLayoutControlUp.visibility = View.INVISIBLE
            linearLayoutControlBottom.visibility = View.INVISIBLE
            playerView.useController = false
            playerView.hideController()
        }
        else {
            playerView.useController = true
            playerView.showController()
        }

    }

    override fun onPause() {
        super.onPause()
        if (::player.isInitialized) {
            wasPlayingBeforePause = player.isPlaying
            if (Util.SDK_INT <= 23 || !isInPictureInPictureMode) {
                player.playWhenReady = false
                playbackPosition = player.currentPosition
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: Handling player stop")
        if (::player.isInitialized) {
            wasPlayingBeforePause = player.isPlaying
            if (Util.SDK_INT > 23 && !isInPictureInPictureMode) {
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
        networkCallback?.let {
            connectivityManager?.unregisterNetworkCallback(it)
        }
        networkCallback = null
        connectivityManager = null
        dismissNoInternetDialog()
        noInternetDialog = null
        if (::player.isInitialized && !isInPictureInPictureMode) {
            player.release()
        }
        controlHideHandler.removeCallbacks(hideControlRunnable)

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