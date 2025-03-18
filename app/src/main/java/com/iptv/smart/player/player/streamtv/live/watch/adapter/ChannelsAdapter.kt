package com.iptv.smart.player.player.streamtv.live.watch.adapter

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.model.Channel
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig
import com.iptv.smart.player.player.streamtv.live.watch.utils.Common

class ChannelsAdapter(
    private val context: Context,
    var channels: MutableList<Channel>,
    private val onChannelClicked: (Channel) -> Unit,
    private val onFavoriteClicked: (Channel) -> Unit,
    private val onRenameChannel: (Channel, String) -> Unit,
    private val onDeleteChannel: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelsAdapter.ChannelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(channels[position])
    }

    override fun getItemCount(): Int = channels.size
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
            val listFav :ArrayList<Channel> = ArrayList()
            listFav.addAll(Common.getChannels(context))
            val newChannel = channel
            newChannel.isFavorite = false
            newChannel.groupTitle=""
            if (listFav.contains(newChannel)){
                favButton.setImageResource(R.drawable.fav_on_channel)
            }else{
                favButton.setImageResource(R.drawable.fav_channel)
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
