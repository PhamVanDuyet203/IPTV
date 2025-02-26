package com.samyak2403.iptvmine.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.util.Channel

class ChannelAdapter(private var channels: List<Channel>) :
    RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.playlistName)
        val count: TextView = itemView.findViewById(R.id.channelCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]
        holder.name.text = channel.name
        holder.count.text = "Live" // Có thể thay bằng thông tin khác nếu cần

        holder.itemView.setOnClickListener {
            // Mở URL của kênh trong trình phát mặc định
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(channel.url))
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = channels.size

    fun updateData(newChannels: List<Channel>) {
        channels = newChannels
        notifyDataSetChanged()
    }
}