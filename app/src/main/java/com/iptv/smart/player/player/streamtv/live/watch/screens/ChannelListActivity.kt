package com.iptv.smart.player.player.streamtv.live.watch

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iptv.smart.player.player.streamtv.live.watch.adapter.GroupAdapter
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.INTER_SELECT_CATEG_OR_CHANNEL
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.gone
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
import com.iptv.smart.player.player.streamtv.live.watch.databinding.ActivityPlaylistDetailBinding
import com.iptv.smart.player.player.streamtv.live.watch.db.PlaylistEntity
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig
import com.iptv.smart.player.player.streamtv.live.watch.util.parseM3U
import com.iptv.smart.player.player.streamtv.live.watch.util.parseM3UFromFile
import com.iptv.smart.player.player.streamtv.live.watch.utils.Common
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChannelListActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GroupAdapter
    private lateinit var tvTitle: TextView
    private lateinit var btnBack: ImageView
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var sortIcon: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var frNative: FrameLayout
    private lateinit var vLine: View
    private var debounceHandler: Handler? = null
    private var isSearchVisible: Boolean = false
    private var fullGroupList: List<PlaylistEntity> = emptyList()
    private var currentSortMode = "AZ"
    private lateinit var imgNotFound: ImageView
    private lateinit var txtNotFound: TextView
    private var noInternetDialog: AlertDialog? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var sourcePath: String = ""

    private lateinit var binding: ActivityPlaylistDetailBinding

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_detail)

        recyclerView = findViewById(R.id.recyclerView)
        tvTitle = findViewById(R.id.tvTitle)
        btnBack = findViewById(R.id.btnBack)
        searchEditText = findViewById(R.id.searchEditText)
        searchIcon = findViewById(R.id.search_icon)
        sortIcon = findViewById(R.id.pop_sort)
        progressBar = findViewById(R.id.progressBar)
        frNative = findViewById(R.id.fr_home)
        vLine = findViewById(R.id.line)
        imgNotFound = findViewById(R.id.imgNotFound)
        txtNotFound = findViewById(R.id.txtNotFound)

        imgNotFound.visibility = View.GONE
        txtNotFound.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = GroupAdapter(emptyList())
        recyclerView.adapter = adapter


        val playlistName = intent.getStringExtra("GROUP_NAME") ?: "Unknown Playlist"
        sourcePath = intent.getStringExtra("SOURCE_PATH") ?: ""

        Log.d("ChannelListActivity", "Received source file path: $sourcePath")
        Log.d("ChannelListActivity", "Playlist name: $playlistName")

        tvTitle.text = playlistName
        tvTitle.isSelected = true

        btnBack.setOnClickListener { finish() }

        searchIcon.setOnClickListener { toggleSearchBar() }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                debounceHandler?.removeCallbacksAndMessages(null)
                debounceHandler = Handler(Looper.getMainLooper())
                debounceHandler?.postDelayed({
                    filterGroups(s.toString())
                }, 500)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        sortIcon.setOnClickListener {

                showSortPopup(it)

        }

        setupNetworkMonitoring()
        checkInternetConnection()
    }

    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    Log.d("ChannelListActivity", "Network available")
                    dismissNoInternetDialog()
                    loadGroupedChannels(sourcePath)
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    Log.d("ChannelListActivity", "Network lost")
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
        if (!isInternetAvailable()) {
            showNoInternetDialog()
        } else {
            dismissNoInternetDialog()
            loadGroupedChannels(sourcePath)
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

    @RequiresApi(Build.VERSION_CODES.M)
    private fun loadGroupedChannels(sourcePath: String) {
        if (!isInternetAvailable() && sourcePath.startsWith("http")) {
            showNoInternetDialog()
            return
        }

        Log.d("ChannelListActivity", "Loading channels from sourcePath: $sourcePath")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                progressBar.visibility = View.VISIBLE
                val channels = if (sourcePath.startsWith("http://") || sourcePath.startsWith("https://")) {
                    Log.d("ChannelListActivity", "Parsing M3U from URL: $sourcePath")
                    parseM3U(sourcePath)
                } else {
                    val uri = android.net.Uri.parse(sourcePath)
                    Log.d("ChannelListActivity", "Parsed URI from sourcePath: $uri")

                    val hasPermission = contentResolver.persistedUriPermissions.any {
                        it.uri == uri && it.isReadPermission
                    }
                    Log.d("ChannelListActivity", "Checking permissions for URI: $uri")
                    Log.d("ChannelListActivity", "Persisted permissions: ${contentResolver.persistedUriPermissions}")
                    Log.d("ChannelListActivity", "Has read permission: $hasPermission")

                    if (!hasPermission) {
                        Log.w("ChannelListActivity", "No read permission for URI: $uri")
                        Toast.makeText(this@ChannelListActivity, "Quyền truy cập tệp đã bị thu hồi. Vui lòng chọn lại tệp.", Toast.LENGTH_LONG).show()
                        finish()
                        return@launch
                    }

                    val inputStream = try {
                        contentResolver.openInputStream(uri)
                            ?: throw Exception("Failed to open input stream for URI: $uri")
                    } catch (e: SecurityException) {
                        throw Exception("Permission denied for URI: $uri. Please select the file again.")
                    }
                    Log.d("ChannelListActivity", "Successfully opened input stream for URI: $uri")
                    inputStream.use { parseM3UFromFile(it) }
                }
                val groupedChannels = channels.groupBy { it.groupTitle ?: "Unknown" }
                val playlistEntities = groupedChannels.map { (groupTitle, channelList) ->
                    PlaylistEntity(
                        id = 0,
                        name = groupTitle,
                        channelCount = channelList.size,
                        sourceType = if (sourcePath.startsWith("http")) "URL" else "FILE",
                        sourcePath = sourcePath
                    )
                }.sortedBy { it.name }

                fullGroupList = playlistEntities
                adapter.updateData(playlistEntities)
                Log.d("ChannelListActivity", "Loaded ${playlistEntities.size} playlist entities from source file")
                progressBar.visibility = View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@ChannelListActivity, "Lỗi khi tải dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("ChannelListActivity", "Error loading channels: ${e.message}", e)
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun filterGroups(query: String) {
        val filteredList = if (query.isEmpty()) {
            fullGroupList
        } else {
            fullGroupList.filter { it.name.contains(query, ignoreCase = true) }
        }
        val sortedList = when (currentSortMode) {
            "AZ" -> filteredList.sortedBy { it.name }
            "ZA" -> filteredList.sortedByDescending { it.name }
            "09" -> filteredList.sortedBy { it.channelCount }
            "90" -> filteredList.sortedByDescending { it.channelCount }
            else -> filteredList
        }
        adapter.updateData(sortedList)

        if (sortedList.isEmpty()) {
            imgNotFound.visibility = View.VISIBLE
            txtNotFound.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            txtNotFound.text = getString(R.string.not_found, query)
        } else {
            imgNotFound.visibility = View.GONE
            txtNotFound.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.scrollToPosition(0)
        }
    }

    override fun onStart() {
        super.onStart()
        if (RemoteConfig.BANNER_DETAIL_PLAYLIST_CHANNEL_050325 == "1") {
            AdsManager.showAdBannerCollapsible(this, AdsManager.BANNER_DETAIL_PLAYLIST_CHANNEL, frNative, vLine)
        } else {
            frNative.gone()
            vLine.gone()
        }
    }

    private fun toggleSearchBar() {
        // Implement toggle logic if needed
        isSearchVisible = !isSearchVisible
        searchEditText.visibility = if (isSearchVisible) View.VISIBLE else View.GONE
    }

    private fun startAds() {
        when (RemoteConfig.INTER_SELECT_CATEG_OR_CHANNEL_050325) {
            "0" -> {}
            else -> {
                Common.countInterSelect++
                if (Common.countInterSelect % RemoteConfig.INTER_SELECT_CATEG_OR_CHANNEL_050325.toInt() == 0) {
                    AdsManager.loadAndShowInter(this, INTER_SELECT_CATEG_OR_CHANNEL) {}
                }
            }
        }
    }

    private fun showSortPopup(anchorView: View) {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_sorting, null)

        val popupWindow = PopupWindow(
            popupView,
            (156 * resources.displayMetrics.density).toInt(),
            (235 * resources.displayMetrics.density).toInt(),
            true
        )

        val marginTopPx = (8 * resources.displayMetrics.density).toInt()
        popupWindow.showAsDropDown(anchorView, 0, marginTopPx, Gravity.END)

        val sortAz = popupView.findViewById<TextView>(R.id.sort_az)
        val sortZa = popupView.findViewById<TextView>(R.id.sort_za)
        val sort09 = popupView.findViewById<TextView>(R.id.sort_09)
        val sort90 = popupView.findViewById<TextView>(R.id.sort_90)

        val defaultColor = android.graphics.Color.TRANSPARENT
        val highlightColor = android.graphics.Color.parseColor("#D0E4FF")

        sortAz.setBackgroundColor(if (currentSortMode == "AZ") highlightColor else defaultColor)
        sortZa.setBackgroundColor(if (currentSortMode == "ZA") highlightColor else defaultColor)
        sort09.setBackgroundColor(if (currentSortMode == "09") highlightColor else defaultColor)
        sort90.setBackgroundColor(if (currentSortMode == "90") highlightColor else defaultColor)

        sortAz.setOnClickListener {
            currentSortMode = "AZ"
            filterGroups(searchEditText.text.toString())
            popupWindow.dismiss()
        }

        sortZa.setOnClickListener {
            currentSortMode = "ZA"
            filterGroups(searchEditText.text.toString())
            popupWindow.dismiss()
        }

        sort09.setOnClickListener {
            currentSortMode = "09"
            filterGroups(searchEditText.text.toString())
            popupWindow.dismiss()
        }

        sort90.setOnClickListener {
            currentSortMode = "90"
            filterGroups(searchEditText.text.toString())
            popupWindow.dismiss()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        super.onResume()
        checkInternetConnection()
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
        debounceHandler?.removeCallbacksAndMessages(null)
    }
}