package com.iptv.smart.player.player.streamtv.live.watch.adapter

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.admob.max.dktlibrary.AdmobUtils
import com.iptv.smart.player.player.streamtv.live.watch.ChannelDetailActivity
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.gone
import com.iptv.smart.player.player.streamtv.live.watch.databinding.ItemadBinding
import com.iptv.smart.player.player.streamtv.live.watch.db.PlaylistEntity
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig

class GroupAdapter(
    private val context: Activity, private var groups: List<PlaylistEntity> = emptyList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_GROUP = 1
        const val TYPE_AD = 2
    }

    override fun getItemCount(): Int {
        val itemCount = groups.size
        val adCount = getAdPositions().count { it < itemCount }
        return itemCount + adCount
    }

    override fun getItemViewType(position: Int): Int {
        val isNetworkConnected = AdmobUtils.isNetworkConnected(context)
        return if (RemoteConfig.NATIVE_PLAYLIST_CHANNEL_050325 == "1" && isNetworkConnected) {
            val adPositions = getAdPositions()
            if (adPositions.contains(position)) TYPE_AD else TYPE_GROUP
        } else {
            TYPE_GROUP
        }
    }

    private fun getAdPositions(): List<Int> {
        val positions = mutableListOf<Int>()
        if (RemoteConfig.NATIVE_PLAYLIST_CHANNEL_050325 == "1" && AdmobUtils.isNetworkConnected(
                context
            ) && groups.size >= 3
        ) {
            var position = 2
            while (position < groups.size + positions.size) {
                positions.add(position)
                position += 5
            }
        }
        return positions
    }

    private fun getAdsCountBeforePosition(position: Int): Int {
        return getAdPositions().count { it < position }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_GROUP -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_playlist_detail, parent, false)
                GroupViewHolder(view)
            }

            TYPE_AD -> {
                val binding =
                    ItemadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                NativeAdViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_AD) {
            (holder as NativeAdViewHolder).bind()
        } else {
            val actualPosition =
                if (RemoteConfig.NATIVE_PLAYLIST_CHANNEL_050325 == "1" && AdmobUtils.isNetworkConnected(
                        holder.itemView.context
                    )
                ) {
                    position - getAdsCountBeforePosition(position)
                } else {
                    position
                }
            if (actualPosition in groups.indices) {
                (holder as GroupViewHolder).bind(groups[actualPosition])
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

    inner class NativeAdViewHolder(private val binding: ItemadBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {

            if ((RemoteConfig.NATIVE_PLAYLIST_CHANNEL_050325 == "1") && AdmobUtils.isNetworkConnected(
                    itemView.context
                )
            ) {
                when (RemoteConfig.NATIVE_PLAYLIST_CHANNEL_050325) {
                    "1" -> AdsManager.loadAndShowAdsNativeCustom(
                        context,
                        binding.frbannerHome,
                        AdsManager.NATIVE_PLAYLIST_CHANNEL,
                    )

                    else -> {
                        binding.frbannerHome.gone()
                    }
                }
            } else {
                binding.frbannerHome.gone()
            }
        }
    }

    fun updateData(newGroups: List<PlaylistEntity>) {
        groups = newGroups
        notifyDataSetChanged()
    }

    fun getGroups(): List<PlaylistEntity> {
        return groups
    }
}