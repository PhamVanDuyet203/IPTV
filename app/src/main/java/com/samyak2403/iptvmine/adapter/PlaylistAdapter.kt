package com.samyak2403.iptvmine.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.db.PlaylistEntity
import com.samyak2403.iptvmine.ChannelListActivity

class PlaylistAdapter(private var playlists: List<PlaylistEntity>) :
    RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.playlistName)
        val count: TextView = itemView.findViewById(R.id.channelCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.name.text = playlist.name
        holder.count.text = "${playlist.channelCount} channels"

        holder.itemView.setOnClickListener {
            // Tạo Intent để mở ChannelListActivity
            val intent = Intent(holder.itemView.context, ChannelListActivity::class.java)
            // Truyền dữ liệu nhóm được chọn
            intent.putExtra("GROUP_NAME", playlist.name)
            intent.putExtra("SOURCE_PATH", playlist.sourcePath)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = playlists.size

    fun updateData(newPlaylists: List<PlaylistEntity>) {
        playlists = newPlaylists
        notifyDataSetChanged()
    }
}