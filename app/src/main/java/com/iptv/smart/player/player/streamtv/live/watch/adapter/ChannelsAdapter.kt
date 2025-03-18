package com.iptv.smart.player.player.streamtv.live.watch.adapter

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.admob.max.dktlibrary.AdmobUtils
import com.bumptech.glide.Glide
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.gone
import com.iptv.smart.player.player.streamtv.live.watch.databinding.ItemadBinding
import com.iptv.smart.player.player.streamtv.live.watch.model.Channel
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig
import com.iptv.smart.player.player.streamtv.live.watch.utils.Common

class ChannelsAdapter(
    private val context: Activity,
    var channels: MutableList<Channel>,
    private val onChannelClicked: (Channel) -> Unit,
    private val onFavoriteClicked: (Channel) -> Unit,
    private val onRenameChannel: (Channel, String) -> Unit,
    private val onDeleteChannel: (Channel) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_CHANNEL = 1
        const val TYPE_AD = 2
    }

    var listUrl = mutableListOf<Channel>()

    fun updateChannels(newChannels: List<Channel>) {
        val uniqueNewChannels = newChannels.distinctBy { it.streamUrl }
        val diffResult = DiffUtil.calculateDiff(ChannelDiffCallback(channels, uniqueNewChannels))

        uniqueNewChannels.forEach {
            if (listUrl.none { existing -> existing.streamUrl == it.streamUrl }) {
                listUrl.add(it)
            }
        }
        channels.clear()
        channels.addAll(uniqueNewChannels)

        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        val isNetworkConnected = AdmobUtils.isNetworkConnected(context)
        return if ((RemoteConfig.NATIVE_PLAYLIST_CHANNEL_050325 == "1") && isNetworkConnected) {
            val adPositions = getAdPositions()
            if (adPositions.contains(position)) {
                TYPE_AD
            } else {
                TYPE_CHANNEL
            }
        } else {
            TYPE_CHANNEL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_AD) {
            val binding = ItemadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            AdViewHolder(binding)
        } else {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
            ChannelViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_AD) {
            (holder as AdViewHolder).bind()
        } else {
            val actualPosition =
                if ((RemoteConfig.NATIVE_PLAYLIST_CHANNEL_050325 == "1") && AdmobUtils.isNetworkConnected(
                        context
                    )
                ) {
                    position - getAdsCountBeforePosition(position)
                } else {
                    position
                }

            if (actualPosition in channels.indices) {
                val channel = channels[actualPosition]
                (holder as ChannelViewHolder).bind(channel)
            }
        }
    }


    override fun getItemCount(): Int {
        return channels.size + getAdPositions().count { it < channels.size }
    }

    private fun getAdPositions(): List<Int> {
        val positions = mutableListOf<Int>()

        if ((RemoteConfig.NATIVE_PLAYLIST_CHANNEL_050325 == "1") && AdmobUtils.isNetworkConnected(
                context
            )
        ) {
            var position = 2
            while (position < channels.size) {
                positions.add(position)
                position += 4
            }
        }
        return positions
    }

    private fun getAdsCountBeforePosition(position: Int): Int {
        return getAdPositions().count { it < position }
    }

    inner class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val logoImageView: ImageView = itemView.findViewById(R.id.logoImageView)
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val favButton: ImageButton = itemView.findViewById(R.id.fav_channel)

        fun bind(channel: Channel) {
            nameTextView.text = channel.name
            Glide.with(itemView.context).load(channel.logoUrl).placeholder(R.drawable.ic_tv)
                .into(logoImageView)

            updateFavoriteIcon(channel)

            favButton.setOnClickListener {
                onFavoriteClicked(channel)
                updateFavoriteIcon(channel)
                notifyItemChanged(adapterPosition)
            }

            itemView.setOnClickListener {
                onChannelClicked(channel)
            }
        }

        private fun updateFavoriteIcon(channel: Channel) {
            val listFav: ArrayList<Channel> = ArrayList(Common.getChannels(context))
            val newChannel = channel.copy(isFavorite = false, groupTitle = "")

            if (listFav.contains(newChannel)) {
                favButton.setImageResource(R.drawable.fav_on_channel)
            } else {
                favButton.setImageResource(R.drawable.fav_channel)
            }
        }
    }

    inner class AdViewHolder(private val binding: ItemadBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {

            if ((RemoteConfig.NATIVE_PLAYLIST_CHANNEL_050325 == "1") && AdmobUtils.isNetworkConnected(
                    context
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

    private class ChannelDiffCallback(
        private val oldList: List<Channel>, private val newList: List<Channel>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].streamUrl == newList[newItemPosition].streamUrl
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

