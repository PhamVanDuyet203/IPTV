package com.iptv.smart.player.player.streamtv.live.watch.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.db.PlaylistEntity
import com.iptv.smart.player.player.streamtv.live.watch.ChannelDetailActivity

class GroupAdapter(private var groups: List<PlaylistEntity>) :
    RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.playlistName)
        val count: TextView = itemView.findViewById(R.id.channelCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist_detail, parent, false) // Đảm bảo layout đúng
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.name.text = group.name
        holder.count.text = "${group.channelCount}"

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ChannelDetailActivity::class.java)
            intent.putExtra("GROUP_NAME", group.name)
            intent.putExtra("SOURCE_PATH", group.sourcePath)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = groups.size

    fun updateData(newGroups: List<PlaylistEntity>) {
        groups = newGroups
        notifyDataSetChanged()
    }

    // Thêm hàm getter để truy cập groups từ bên ngoài
    fun getGroups(): List<PlaylistEntity> {
        return groups
    }
}