package com.samyak2403.iptvmine.adapter

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.model.VideoItem

class VideoAdapter(
    private val context: Context,
    private val videoList: MutableList<VideoItem>,
    private val onDelete: (VideoItem) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.imageView)
        val title: TextView = view.findViewById(R.id.text_title)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val videoItem = videoList[position]

        // Hiển thị tên file video
        holder.title.text = videoItem.fileName

        // Hiển thị thumbnail video
        holder.thumbnail.setImageBitmap(getVideoThumbnail(videoItem.uri))

        // Xử lý sự kiện xóa
        holder.btnDelete.setOnClickListener {
            onDelete(videoItem)
        }
    }

    override fun getItemCount() = videoList.size

    // Lấy thumbnail video từ Uri
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
