package com.iptv.smart.player.player.streamtv.live.watch.adapter

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.model.Channel

class VideoDetailAdapter(
    private val context: Context,
    private val videoList: MutableList<Channel>,
    private val onPlayClicked: (Channel) -> Unit,
    private val onFavoriteClicked: (Channel) -> Unit,
) : RecyclerView.Adapter<VideoDetailAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.logoImageView)
        val title: TextView = itemView.findViewById(R.id.nameTextView)
        val favButton: ImageButton = itemView.findViewById(R.id.fav_channel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val videoItem = videoList[position]

        holder.title.text = videoItem.name
        val thumbnail = getVideoThumbnail(videoItem.streamUrl.toUri())
        if (thumbnail != null) {
            holder.thumbnail.setImageBitmap(thumbnail)
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_tv)
        }

        updateFavoriteIcon(holder, videoItem.isFavorite)

        holder.itemView.setOnClickListener {
            onPlayClicked(videoItem)
        }

        holder.favButton.setOnClickListener {
            Log.d("TAGfffffffffffff", "onBindViewHolder: " + videoItem.isFavorite)
            onFavoriteClicked(videoItem)
            updateFavoriteIcon(holder, videoItem.isFavorite)
            notifyItemChanged(position)
        }

    }


    fun updateData(newVideoList: MutableList<Channel>) {
        videoList.clear()
        videoList.addAll(newVideoList)
        notifyDataSetChanged()
    }

    override fun getItemCount() = videoList.size

    private fun updateFavoriteIcon(holder: VideoViewHolder, isFavorite: Boolean) {
        if (isFavorite) {
            holder.favButton.setImageResource(R.drawable.fav_on_channel)
        } else {
            holder.favButton.setImageResource(R.drawable.fav_channel)
        }
    }


    private fun getVideoThumbnail(uri: Uri): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val bitmap = retriever.frameAtTime
            retriever.release()
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}