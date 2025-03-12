package com.iptv.smart.player.player.streamtv.live.watch.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.db.PlaylistEntity
import com.iptv.smart.player.player.streamtv.live.watch.ChannelListActivity
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig
import com.iptv.smart.player.player.streamtv.live.watch.screens.VideoDetailActivity
import com.iptv.smart.player.player.streamtv.live.watch.utils.Common
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.iptv.smart.player.player.streamtv.live.watch.utils.ViewType
import com.google.android.gms.ads.LoadAdError


class PlaylistAdapter(
    private val activity: Activity,
    private var playlists: List<PlaylistEntity>,
    private val onDeletePlaylist: (PlaylistEntity) -> Unit = {},
    private val onRenamePlaylist: (PlaylistEntity, String) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Danh sách tổng hợp playlist và quảng cáo
    private val items = mutableListOf<Any>()

    init {
        setupItems()
    }

    // Logic chèn Native Ad vào danh sách
    private fun setupItems() {
        items.clear()
        var adCount = 0
        playlists.forEachIndexed { index, playlist ->
            if (RemoteConfig.NATIVE_PLAYLIST_CHANNEL_050325 != "0" &&
                (index == 2 || (index > 2 && (index - 2) % 4 == 0))) {
                items.add("Native Ad $adCount")
                adCount++
            }
            items.add(playlist)
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String && (items[position] as String).startsWith("Native Ad")) {
            ViewType.NATIVE_AD
        } else {
            ViewType.PLAYLIST_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.PLAYLIST_ITEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_playlist, parent, false)
                PlaylistViewHolder(view, activity, onRenamePlaylist, onDeletePlaylist)
            }
            ViewType.NATIVE_AD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.ad_template_small_bot, parent, false) // Layout cho Native Ad
                NativeAdViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PlaylistViewHolder -> holder.bind(items[position] as PlaylistEntity)
            is NativeAdViewHolder -> holder.bind()
        }
    }

    class PlaylistViewHolder(
        itemView: View,
        private val activity: Activity,
        private val onRenamePlaylist: (PlaylistEntity, String) -> Unit, // Thêm callback
        private val onDeletePlaylist: (PlaylistEntity) -> Unit // Thêm callback
    ) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.playlistName)
        val count: TextView = itemView.findViewById(R.id.channelCount)
        val optButton: ImageButton = itemView.findViewById(R.id.opt_channel)

        @SuppressLint("ResourceAsColor")
        fun bind(playlist: PlaylistEntity) {
            name.text = playlist.name

            val fullText = "${playlist.channelCount} channels"
            val spannableString = SpannableString(fullText)

            val start = 0
            val end = playlist.channelCount.toString().length

            spannableString.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(itemView.context, R.color.cool_blue)),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            count.text = spannableString


            count.setTextColor(Color.BLACK)
            count.highlightColor = Color.TRANSPARENT

            val iconRes = when (playlist.sourceType) {
                "URL" -> R.drawable.link_channel
                "FILE" -> R.drawable.link_docs
                "DEVICE" -> R.drawable.link_video
                else -> R.drawable.link_channel
            }
            itemView.findViewById<ImageView>(R.id.img_item_playlist).setImageResource(iconRes)

            itemView.setOnClickListener {
                startAds(playlist)
            }

            optButton.setOnClickListener {
                showBottomSheet(itemView.context, playlist)
            }
        }

        private fun startAds(playlist: PlaylistEntity) {
            when (RemoteConfig.INTER_ITEMS_PLAYLIST_050325) {
                "0" -> nextActivity(playlist)
                else -> {
                    Common.countInterAdd++
                    if (Common.countInterAdd % RemoteConfig.INTER_ITEMS_PLAYLIST_050325.toInt() == 0) {
                        AdsManager.loadAndShowInter(activity, AdsManager.INTER_ITEMS_PLAYLIST) {
                            nextActivity(playlist)
                        }
                    } else {
                        nextActivity(playlist)
                    }
                }
            }
        }

        private fun nextActivity(playlist: PlaylistEntity) {
            if (playlist.sourceType == "DEVICE" && !playlist.sourcePath.endsWith(".m3u")) {
                val intent = Intent(activity, VideoDetailActivity::class.java)
                intent.putExtra("GROUP_NAME", playlist.name)
                intent.putExtra("SOURCE_PATH", playlist.sourcePath)
                activity.startActivity(intent)
            } else {
                val intent = Intent(activity, ChannelListActivity::class.java)
                intent.putExtra("GROUP_NAME", playlist.name)
                intent.putExtra("SOURCE_PATH", playlist.sourcePath)
                activity.startActivity(intent)
            }
        }

        private fun showBottomSheet(context: Context, playlist: PlaylistEntity) {
            val bottomSheetDialog = BottomSheetDialog(context)
            val bottomSheetView = LayoutInflater.from(context).inflate(R.layout.bts_item, null)
            bottomSheetDialog.setContentView(bottomSheetView)

            val playlistName = bottomSheetView.findViewById<TextView>(R.id.playlist_name)
            playlistName.text = playlist.name

            bottomSheetView.findViewById<View>(R.id.btn_close).setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            bottomSheetView.findViewById<View>(R.id.rename).setOnClickListener {
                bottomSheetDialog.dismiss()
                showRenameDialog(context, playlist)
            }

            bottomSheetView.findViewById<View>(R.id.del).setOnClickListener {
                bottomSheetDialog.dismiss()
                showConfirmDeleteDialog(context, playlist)
            }

            bottomSheetDialog.show()
        }

        private fun showRenameDialog(context: Context, playlist: PlaylistEntity) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null)
            val dialog = AlertDialog.Builder(context).setView(dialogView).create()
            dialog.show()

            val width = (290 * context.resources.displayMetrics.density).toInt()
            val height = (215 * context.resources.displayMetrics.density).toInt()
            dialog.window?.apply {
                setLayout(width, height)
                setBackgroundDrawableResource(R.drawable.bg_confirm_del)
            }

            val nameEditText = dialogView.findViewById<EditText>(android.R.id.text1)
            val btnDelText = dialogView.findViewById<ImageView>(R.id.btn_del_text)
            nameEditText.setText(playlist.name)

            btnDelText.visibility = if (nameEditText.text.isNotEmpty()) View.VISIBLE else View.GONE

            btnDelText.setOnClickListener {
                nameEditText.text.clear()
                btnDelText.visibility = View.GONE
            }

            nameEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    btnDelText.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                }
            })

            dialogView.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
                dialog.dismiss()
            }

            dialogView.findViewById<TextView>(R.id.btn_delete).setOnClickListener {
                val newName = nameEditText.text.toString().trim()
                if (nameEditText.text != null) {
                    if (newName.isNotEmpty()) {
                        if (newName != playlist.name) {
                            val updatedPlaylist = playlist.copy(name = newName)
                            onRenamePlaylist(updatedPlaylist, newName)
                        }
                        dialog.dismiss()
                    } else {
                        Toast.makeText(dialogView.context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(dialogView.context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun showConfirmDeleteDialog(context: Context, playlist: PlaylistEntity) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.confirm_del, null)
            val dialog = AlertDialog.Builder(context).setView(dialogView).create()
            dialog.show()

            val width = (312 * context.resources.displayMetrics.density).toInt()
            dialog.window?.apply {
                setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawableResource(R.drawable.bg_confirm_del)
            }

            val message = dialogView.findViewById<TextView>(R.id.message)
            val fullText = context.getString(R.string.do_you_want_to_delete_playlist, playlist.name)
            val spannableString = android.text.SpannableString(fullText)
            val start = fullText.indexOf(playlist.name)
            val end = start + playlist.name.length
            spannableString.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                start,
                end,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            message.text = spannableString

            dialogView.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
                dialog.dismiss()
            }

            dialogView.findViewById<TextView>(R.id.btn_delete).setOnClickListener {
                onDeletePlaylist(playlist)
                dialog.dismiss()
            }
        }
    }

    class NativeAdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            val adLoader = AdLoader.Builder(itemView.context,
                AdsManager.NATIVE_PLAYLIST_CHANNEL.toString()
            )
                .forNativeAd { nativeAd ->
                    val adView = itemView as NativeAdView
                    // Gắn dữ liệu quảng cáo vào view (tuỳ chỉnh theo layout)
                    adView.headlineView = adView.findViewById(R.id.ad_headline)
                    adView.setNativeAd(nativeAd)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("NativeAd", "Ad failed to load: ${error.message}")
                    }
                })
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    // Cập nhật dữ liệu và danh sách item
    fun updateData(newPlaylists: List<PlaylistEntity>) {
        playlists = newPlaylists
        setupItems()
        notifyDataSetChanged()
    }
}