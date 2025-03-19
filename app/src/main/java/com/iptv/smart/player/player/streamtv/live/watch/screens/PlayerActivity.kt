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
import android.graphics.PorterDuff
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.admob.max.dktlibrary.AppOpenManager
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.INTER_BACK_PLAY_TO_LIST
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.gone
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.visible
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
import com.iptv.smart.player.player.streamtv.live.watch.databinding.CustomControllerChannelBinding
import com.iptv.smart.player.player.streamtv.live.watch.model.Channel
import com.iptv.smart.player.player.streamtv.live.watch.provider.ChannelsProvider
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
    private var isReturnFromPiPSetting = false
    private var isLock = false
    private var isInPictureInPictureMode = false
    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "hnvytnynvynynyj"
    private lateinit var tvTitle: TextView
    private val binding by lazy { CustomControllerChannelBinding.inflate(layoutInflater) }
    private var isControlVisible = false
    private val controlHideHandler = Handler(Looper.getMainLooper())
    private val hideControlRunnable = Runnable {
        if (!isLock && isFullScreen) {
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

    private var defaultLockLayoutParams: LinearLayout.LayoutParams? = null

    companion object {
        private const val INCREMENT_MILLIS = 3000L
        private const val MIN_PIP_API = Build.VERSION_CODES.O

        fun start(context: Context, channel: Channel) {
            Log.d("sadasdasdsads", "start From: "+channel)
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
        setContentView(binding.root)
        channel = savedInstanceState?.getParcelable("channel") ?: intent.getParcelableExtra("channel") ?: run {
            finish()
            return
        }
        Log.d("TAG23232323232", "onCreate: "+channel.isFavorite)

        channelsProvider = ViewModelProvider(this).get(ChannelsProvider::class.java)
        channelsProvider.init(this)

        setFindViewById()
        isLocalFile = Uri.parse(channel.streamUrl).scheme in listOf("content", "file")
        setupNetworkMonitoring()
        setLockScreen()
        setFullScreen()
        setupFavorite()
        setupPip()

        binding.btnMirroring1.gone()
        binding.btnPip1.gone()

        channelsProvider.addToRecent(this,channel)
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

//      rf db
        channelsProvider.fetchChannelsFromRoom()
    }


    private fun setupNetworkMonitoring() {
        if (isLocalFile) return

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    dismissNoInternetDialog()
                    if (!::player.isInitialized || !player.isPlaying) {
                        setupPlayer()
                    }
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
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
            return
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
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
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
                setResultAndFinish()
            }

            else -> {
                Common.countInterBackPLay++
                if (Common.countInterBackPLay % RemoteConfig.INTER_BACK_PLAY_TO_LIST_050325.toInt() == 0) {
                    AdsManager.loadAndShowInter(this, INTER_BACK_PLAY_TO_LIST) {
                        setResultAndFinish()
                    }
                } else {
                    setResultAndFinish()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setFindViewById() {
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

        val lockLayoutNormal = findViewById<LinearLayout>(R.id.btn_lock)
        defaultLockLayoutParams = lockLayoutNormal.layoutParams as LinearLayout.LayoutParams

        btnMirroring.setOnClickListener {
            if (isLocalFile || isInternetAvailable()) wifiDisplay() else showNoInternetDialog()
            AppOpenManager.getInstance().disableAppResumeWithActivity(PlayerActivity::class.java)
        }

        binding.btnMirroring1.setOnClickListener {
            if (isLocalFile || isInternetAvailable()) wifiDisplay() else showNoInternetDialog()
            AppOpenManager.getInstance().disableAppResumeWithActivity(PlayerActivity::class.java)
        }

        btnBack.setOnClickListener {
            if (isLock) {
                return@setOnClickListener
            }
            if (isFullScreen && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                imageViewFullScreen.performClick()
            } else if (Build.VERSION.SDK_INT >= MIN_PIP_API && isInPictureInPictureMode) {
                moveTaskToBack(true)
            } else {
                startAds()
            }
        }
    }

    private fun setResultAndFinish() {
        val resultIntent = Intent().apply {
            putExtra("REFRESH_DATA", true)
            putExtra("CHANNEL_NAME", channel.name)
            putExtra("IS_FAVORITE", channel.isFavorite)
        }
        Log.d(
            TAG,
            "setResultAndFinish: Sending result - Channel: ${channel.name}, isFavorite: ${channel.isFavorite}"
        )
        setResult(RESULT_OK, resultIntent)
        finish()
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
                    Toast.makeText(applicationContext,
                        getString(R.string.device_not_supported), Toast.LENGTH_LONG)
                        .show()
                }
            }
        } catch (ex: Exception) {
            Toast.makeText(applicationContext, R.string.device_not_supported, Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 169) {
            if (::player.isInitialized && wasPlayingBeforePause) {
                player.playWhenReady = true
            }
        }
    }



    private fun setupFavorite() {
        val favoriteLayout = controlButtonsTop.getChildAt(2) as? LinearLayout
        val favoriteIcon = favoriteLayout?.findViewById<ImageView>(R.id.img_fav)
        val favoriteBtn = favoriteLayout?.findViewById<LinearLayout>(R.id.btn_fav)
        favoriteBtn?.setOnClickListener {
            channelsProvider.toggleFavorite(this, channel)
            updateFavoriteIcon(favoriteIcon)
        }
    }

    private fun updateFavoriteIcon(
        favoriteIcon: ImageView? = if (isFullScreen)
            binding.controlButtonsTop1.getChildAt(2)?.findViewById(R.id.img_fav1)
        else controlButtonsTop.getChildAt(2)?.findViewById(R.id.img_fav)
    ) {
        val listFav: ArrayList<Channel> = ArrayList()
        listFav.addAll(Common.getChannels(this))
        val newChannel = filterChannelsByStreamUrl(listFav,channel.streamUrl)
        if (listFav.contains(newChannel)) {
            favoriteIcon?.setImageResource(
                R.drawable.fav_on_channel
            )
        } else {
            favoriteIcon?.setImageResource(
                R.drawable.ic_fav
            )
        }
    }

    private fun filterChannelsByStreamUrl(channels: List<Channel>, selectedStreamUrl: String): Channel? {
        return channels.find { it.streamUrl == selectedStreamUrl }
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

        if (::player.isInitialized) {
            player.release()
        }
        player = ExoPlayer.Builder(this).setSeekBackIncrementMs(INCREMENT_MILLIS)
            .setSeekForwardIncrementMs(INCREMENT_MILLIS).build().also { exoPlayer ->
                playerView.player = exoPlayer

                val uri = Uri.parse(channel.streamUrl)
                if (uri == null || uri.toString().isEmpty()) {

                    Toast.makeText(this, "Invalid video URL", Toast.LENGTH_SHORT).show()
                    return
                }
                Log.e(TAG, "setupPlayer: Invalid URIiiiiiiiiiiiiii: ${uri}")
                val mediaItem = MediaItem.fromUri(uri)
                val mediaSource = when {
                    uri.scheme == "content" || uri.scheme == "file" -> {
                        Log.d(
                            TAG,
                            "setupPlayer: Detected local file URI (MP4), using ProgressiveMediaSource"
                        )
                        binding.btnFav.gone()
                        binding.btnFa.gone()
                        ProgressiveMediaSource.Factory(
                            DefaultDataSource.Factory(this)
                        ).createMediaSource(mediaItem)
                    }

                    uri.scheme == "http" || uri.scheme == "https" -> {
                        Log.d(TAG, "setupPlayer: Detected streaming URL, using HlsMediaSource")
                        binding.btnFav.visible()
                        binding.btnFa.visible()
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
                        loadingProgress.visibility =
                            if (isLoading && !exoPlayer.isPlaying) View.VISIBLE else View.GONE
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
        exoPlay.visibility = if (isPlaying) View.GONE else View.VISIBLE
        exoPause.visibility = if (isPlaying) View.VISIBLE else View.GONE
    }

    private val originalLayoutParams = mutableMapOf<View, LinearLayout.LayoutParams>()

    private fun lockScreen(lock: Boolean) {
        isLock = lock

        val controlButtons = if (isFullScreen) binding.controlButtonsTop1 else controlButtonsTop
        val lockLayout =
            controlButtons.findViewById<LinearLayout>(if (isFullScreen) R.id.btn_lock1 else R.id.btn_lock)
        val lockIcon =
            controlButtons.findViewById<ImageView>(if (isFullScreen) R.id.img_lock1 else R.id.img_lock)
        val lockText =
            controlButtons.findViewById<TextView>(if (isFullScreen) R.id.txt_lock1 else R.id.txt_lock)

        if (lock) {
            linearLayoutControlUp.visibility = View.INVISIBLE
            linearLayoutControlBottom.visibility = View.INVISIBLE
            controlButtons.visibility = View.VISIBLE

            for (i in 0 until controlButtons.childCount) {
                val child = controlButtons.getChildAt(i)
                if (child != lockLayout) {
                    originalLayoutParams[child] = child.layoutParams as LinearLayout.LayoutParams
                    child.visibility = View.GONE
                } else {
                    child.visibility = View.VISIBLE
                }
            }

            lockIcon.setImageDrawable(
                ContextCompat.getDrawable(applicationContext, R.drawable.ic_lock_screen)
            )
            lockText.text = getString(R.string.screen_lock_long_press_to_unlock)
            if (!isFullScreen) {
                playerView.useController = false
                playerView.hideController()
                lockLayout.setBackgroundResource(R.drawable.bg_lock_normal)
                lockText.setTextColor(Color.parseColor("#3F484A"))
            } else {
                btnBack.gone()
                tvTitle.gone()
                playerView.useController = false
                playerView.hideController()
                lockLayout.setBackgroundResource(R.drawable.bg_locked)
                val lockLayoutParams = lockLayout?.layoutParams ?: LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lockLayoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
                lockLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                lockLayout?.layoutParams = lockLayoutParams
                lockLayout?.requestLayout()
                lockText.visible()
            }

        } else {
            linearLayoutControlUp.visibility = View.VISIBLE
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
            lockText.text = getString(R.string.lock_text)
            if (!isFullScreen) {
                lockLayout.setBackgroundResource(R.drawable.bg_menu_playcontrol)
                lockText.setTextColor(Color.parseColor("#3F484A"))
            } else {
                lockLayout.setBackgroundResource(R.drawable.bg_menu_playcontrol1)
                lockText.setTextColor(Color.WHITE)
            }


            controlButtons.requestLayout()

        }

    }

    private fun setLockScreen() {
        val lockButton = findViewById<LinearLayout>(R.id.btn_lock)
        val lockIcon = findViewById<ImageView>(R.id.img_lock)

        lockButton.setOnClickListener {
            if (!isLock) {
                isLock = true
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

    private fun hideBottomTool() {
        if (isFullScreen && !isInPictureInPictureMode) {
            binding.controlButtonsTop1.gone()
            btnBack.gone()
            tvTitle.gone()
        }
        if (isInPictureInPictureMode) {
            btnBack.gone()
            tvTitle.gone()
            playerView.hideController()
        }
    }


    private fun showBottomTool() {
        if (isFullScreen && !isInPictureInPictureMode) {
            binding.controlButtonsTop1.visible()
            btnBack.visible()
            tvTitle.visible()
            tvTitle.setTextColor(Color.WHITE)
            btnBack.setColorFilter(Color.WHITE)
            handler.removeCallbacks(hideControlRunnable)
            handler.postDelayed(hideControlRunnable, 3000)
        }
        if (isInPictureInPictureMode) {
            btnBack.gone()
            tvTitle.gone()
            playerView.hideController()
        }


    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("SourceLockedOrientationActivity")
    private fun setFullScreen() {
        imageViewFullScreen.setOnClickListener {
            isFullScreen = !isFullScreen
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
                if (binding.controlButtonsTop1.visibility != View.VISIBLE) {
                    showBottomTool()
                } else hideBottomTool()
                Log.d("TAGadsafsdfsafd", "setFullScreen: playerClick")
            }
            mainCustomControl.setOnClickListener {
                linearLayoutControlUp.gone()
                linearLayoutControlBottom.gone()
                // showBottomTool()
                if (binding.controlButtonsTop1.visibility != View.VISIBLE) {
                    showBottomTool()
                    playerView.showController()
                } else  {
                    hideBottomTool()
                    playerView.hideController()
                }
                Log.d("TAGadsafsdfsafd", "setFullScreen: mainClick")

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
                updateFavoriteIcon()


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
                updateFavoriteIcon()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupControlsForFullscreen() {
        binding.btnMirroring1.setOnClickListener {
            if (isLocalFile || isInternetAvailable()) wifiDisplay() else showNoInternetDialog()
        }
        binding.btnMirroring1.setBackgroundResource(R.drawable.bg_menu_playcontrol1)
        binding.btnPip1.setBackgroundResource(R.drawable.bg_menu_playcontrol1)

        val favoriteLayout = binding.controlButtonsTop1.getChildAt(2) as? LinearLayout
        val favoriteIcon = favoriteLayout?.findViewById<ImageView>(R.id.img_fav1)
        val favoriteBtn = favoriteLayout?.findViewById<LinearLayout>(R.id.btn_fa)
        updateFavoriteIcon(favoriteIcon)
        favoriteBtn?.setBackgroundResource(R.drawable.bg_menu_playcontrol1)

        favoriteBtn?.setOnClickListener {
            channelsProvider.toggleFavorite(this, channel)
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

        findViewById<ImageView>(R.id.img_mirroring1)?.setColorFilter(
            Color.parseColor("#FFFFFF"),
            PorterDuff.Mode.SRC_IN
        )
        findViewById<ImageView>(R.id.img_pip1)?.setColorFilter(
            Color.parseColor("#FFFFFF"),
            PorterDuff.Mode.SRC_IN
        )
        findViewById<ImageView>(R.id.img_fav1)?.setColorFilter(
            Color.parseColor("#FFFFFF"),
            PorterDuff.Mode.SRC_IN
        )
        findViewById<ImageView>(R.id.img_lock1)?.setColorFilter(
            Color.parseColor("#FFFFFF"),
            PorterDuff.Mode.SRC_IN
        )
        btnBack.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)

        exoPlay.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        exoPause.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        exoRew.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        exoFfwd.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)
        imageViewFullScreen.setColorFilter(Color.parseColor("#FFFFFF"), PorterDuff.Mode.SRC_IN)

        findViewById<LinearLayout>(R.id.btn_mirroring1)?.setBackgroundResource(R.drawable.bg_menu_playcontrol1)
        findViewById<LinearLayout>(R.id.btn_pip1)?.setBackgroundResource(R.drawable.bg_menu_playcontrol1)
        binding.controlButtonsTop1.getChildAt(2)
            ?.setBackgroundResource(R.drawable.bg_menu_playcontrol1)
        binding.controlButtonsTop1.getChildAt(3)
            ?.setBackgroundResource(R.drawable.bg_menu_playcontrol1)


        val timeBar =
            linearLayoutControlBottom.findViewById<com.google.android.exoplayer2.ui.DefaultTimeBar>(
                R.id.exo_progress
            )
        timeBar?.visibility = View.VISIBLE
        showBottomTool()


        controlHideHandler.removeCallbacks(hideControlRunnable)
        controlHideHandler.postDelayed(hideControlRunnable, CONTROL_HIDE_DELAY)
    }

    private fun hideControlsInFullscreen() {
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
        binding.controlButtonsTop1.getChildAt(2)
            ?.setBackgroundResource(R.drawable.bg_menu_playcontrol)
        binding.controlButtonsTop1.getChildAt(3)
            ?.setBackgroundResource(R.drawable.bg_menu_playcontrol)


        controlHideHandler.removeCallbacks(hideControlRunnable)
    }

    private fun setupPip() {
        if (Build.VERSION.SDK_INT >= MIN_PIP_API) {
            btnPip.setOnClickListener {
                AppOpenManager.getInstance().disableAppResumeWithActivity(PlayerActivity::class.java)
                enterPictureInPictureModeIfAvailable()
            }
            binding.btnPip1.setOnClickListener {
                AppOpenManager.getInstance().disableAppResumeWithActivity(PlayerActivity::class.java)
                enterPictureInPictureModeIfAvailable()
            }
        } else {
            btnPip.visibility = View.GONE
        }
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

    private val pipPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            isReturnFromPiPSetting = true
            if (isPipPermissionGranted()) {
                if (::player.isInitialized && wasPlayingBeforePause) {
                    player.playWhenReady = true
                }
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
                getString(R.string.please_enable_picture_in_picture_for_this_app),
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
                    getString(
                        R.string.go_to_picture_in_picture_settings_and_enable_it_for,
                        packageName
                    ),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: ActivityNotFoundException) {
                val finalFallbackIntent = Intent(Settings.ACTION_SETTINGS)
                pipPermissionLauncher.launch(finalFallbackIntent)
                Toast.makeText(
                    this,
                    getString(R.string.go_to_apps_picture_in_picture_and_enable_it, packageName),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun enterPictureInPictureModeIfAvailable() {
        if (Build.VERSION.SDK_INT < MIN_PIP_API) return
//        binding.btnBack.gone()
//        binding.tvTitle.gone()
        binding.controlButtonsTop1.gone()
        binding.frHome.gone()
        playerView.hideController()
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            Toast.makeText(this,
                getString(R.string.pip_not_supported_on_this_device), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this,
                    getString(R.string.player_not_ready_for_pip), Toast.LENGTH_SHORT).show()
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
                player.playWhenReady = false
                Toast.makeText(this,
                    getString(R.string.failed_to_entxer_pip_mode), Toast.LENGTH_SHORT).show()
            }
        }
        AppOpenManager.getInstance().disableAppResumeWithActivity(PlayerActivity::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean, @NonNull newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
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

            if (isFullScreen) {
                binding.controlButtonsTop1.gone()
            }

            val params = playerView.layoutParams as ConstraintLayout.LayoutParams
            params.height = ConstraintLayout.LayoutParams.MATCH_PARENT
            params.topToBottom = ConstraintLayout.LayoutParams.UNSET
            params.bottomToTop = ConstraintLayout.LayoutParams.UNSET
            params.topMargin = 0
            params.bottomMargin = 0
            playerView.layoutParams = params
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        } else {

            if (::player.isInitialized) {
                    player.playWhenReady = false
            }

            if (isFullScreen) {
                binding.controlButtonsTop1.visible()
            }

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

//            if (::player.isInitialized && wasPlayingBeforePause && isPipPermissionGranted() && !isFinishing) {
//                player.playWhenReady = true
//            }

        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (::player.isInitialized && !isInPictureInPictureMode) {
            player.playWhenReady = false
            Log.d(TAG, "User left app or closed PiP, pausing player")
        }
    }

    private fun updateTime() {
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

            if (!isControlVisible) {
                showControlsInFullscreen()
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

            isControlVisible = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("channel", channel)
        player?.let { playbackPosition = it.currentPosition }
        outState.putLong("playbackPosition", playbackPosition)
    }

    override fun onStart() {
        super.onStart()
        when (RemoteConfig.ADS_PLAY_CONTROL_050325) {
            "1" -> {
                AdsManager.showAdsBanner(
                    this,
                    AdsManager.BANNER_PLAY_CONTROL,
                    binding.frHome,
                    binding.line
                )
            }
            "2" -> {
                AdsManager.showAdBannerCollapsible(
                    this,
                    AdsManager.BANNER_COLLAP_PLAY_CONTROL,
                    binding.frHome,
                    binding.line
                )
            }
            else -> {
                binding.frHome.gone()
                binding.line.gone()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        super.onResume()
        checkInternetConnection()
        if (!isReturnFromPiPSetting) {
            AppOpenManager.getInstance().enableAppResumeWithActivity(PlayerActivity::class.java)
        } else {
            AppOpenManager.getInstance().disableAppResumeWithActivity(PlayerActivity::class.java)
            isReturnFromPiPSetting = false
        }
        if (isLock) {
            Log.d(TAG, "onResume: ")
            binding.btnLock1.visible()
            linearLayoutControlUp.visibility = View.INVISIBLE
            linearLayoutControlBottom.visibility = View.INVISIBLE
            playerView.useController = false
            playerView.hideController()
        } else {
            playerView.useController = true
            playerView.showController()
        }

        if (::player.isInitialized && !isInPictureInPictureMode && wasPlayingBeforePause && isPipPermissionGranted()) {
            player.playWhenReady = true
        }

        updateFavoriteIcon()

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
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isLock) {
            return
        }
        if (isFullScreen && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            imageViewFullScreen.performClick()
        } else if (Build.VERSION.SDK_INT >= MIN_PIP_API && isInPictureInPictureMode) {
            moveTaskToBack(true)
        } else {
            super.onBackPressed()
        }
    }
}