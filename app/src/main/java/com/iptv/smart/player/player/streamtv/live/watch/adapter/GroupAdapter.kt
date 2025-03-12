package com.iptv.smart.player.player.streamtv.live.watch.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.ChannelDetailActivity
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.db.PlaylistEntity
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig
import com.iptv.smart.player.player.streamtv.live.watch.utils.ViewTypeGroup

class GroupAdapter(private var groups: List<PlaylistEntity>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>()

    init {
        setupItems()
    }

    private fun setupItems() {
        items.clear()
        var adCount = 0
        groups.forEachIndexed { index, group ->
            if (RemoteConfig.NATIVE_PLAYLIST_CHANNEL_050325 != "0" &&
                (index == 2 || (index > 2 && (index - 2) % 4 == 0))) {
                items.add("Native Ad $adCount")
                adCount++
            }
            items.add(group)
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        val type = if (items[position] is String && (items[position] as String).startsWith("Native Ad")) {
            ViewTypeGroup.NATIVE_AD_G
        } else {
            ViewTypeGroup.PLAYLIST_GROUP
        }
        return type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewTypeGroup.PLAYLIST_GROUP -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_playlist_detail, parent, false)
                GroupViewHolder(view)
            }
            ViewTypeGroup.NATIVE_AD_G -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.ad_template_small_bot, parent, false)
                NativeAdViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GroupViewHolder -> {
                if (items[position] is PlaylistEntity) {
                    val group = items[position] as PlaylistEntity
                    holder.bind(group)
                } else {
                }
            }
            is NativeAdViewHolder -> {
                holder.bind()
            }
        }
    }

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.playlistName)
        val count: TextView = itemView.findViewById(R.id.channelCount)

        fun bind(group: PlaylistEntity) {
            name.text = group.name
            count.text = "${group.channelCount}"

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, ChannelDetailActivity::class.java)
                intent.putExtra("GROUP_NAME", group.name)
                intent.putExtra("SOURCE_PATH", group.sourcePath)
                itemView.context.startActivity(intent)
            }
        }
    }

    class NativeAdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            val adLoader = AdLoader.Builder(itemView.context, AdsManager.NATIVE_PLAYLIST_CHANNEL.toString())
                .forNativeAd { nativeAd ->
                    val adView = itemView as NativeAdView
                    adView.headlineView = adView.findViewById(R.id.ad_headline)
                    adView.setNativeAd(nativeAd)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                    }
                    override fun onAdLoaded() {
                        Log.d("GroupAdapter", "Ad loaded successfully")
                    }
                })
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    fun updateData(newGroups: List<PlaylistEntity>) {
        groups = newGroups
        setupItems()
        notifyDataSetChanged()
    }

    fun getGroups(): List<PlaylistEntity> {
        return groups
    }
}