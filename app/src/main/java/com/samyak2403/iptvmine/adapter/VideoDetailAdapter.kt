package com.samyak2403.iptvmine.adapter

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.model.Channel
import com.samyak2403.iptvmine.model.VideoItem
import com.samyak2403.iptvmine.screens.PlayerActivity

class VideoDetailAdapter(
    private val context: Context,
    private val videoList: MutableList<VideoItem>,
    private val onPlayClicked: (VideoItem) -> Unit,
    private val onFavoriteClicked: (VideoItem) -> Unit,
    private val onRenameVideo: (VideoItem, String) -> Unit,
    private val onDeleteVideo: (VideoItem) -> Unit
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

        // Hiển thị tên file video
        holder.title.text = videoItem.fileName

        // Hiển thị thumbnail video
        val thumbnail = getVideoThumbnail(videoItem.uri)
        if (thumbnail != null) {
            holder.thumbnail.setImageBitmap(thumbnail)
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_tv)
        }

        // Cập nhật trạng thái yêu thích
        updateFavoriteIcon(holder, videoItem.isFavorite)

        // Sự kiện click để phát video
        holder.itemView.setOnClickListener {
            onPlayClicked(videoItem)
        }

        // Sự kiện click nút yêu thích
        holder.favButton.setOnClickListener {
            onFavoriteClicked(videoItem)
            videoItem.isFavorite = !videoItem.isFavorite
            updateFavoriteIcon(holder, videoItem.isFavorite)
            notifyItemChanged(position) // Sử dụng position thay vì adapterPosition
        }

    }

    override fun getItemCount() = videoList.size

    private fun updateFavoriteIcon(holder: VideoViewHolder, isFavorite: Boolean) {
        if (isFavorite) {
            holder.favButton.setImageResource(R.drawable.fav_on_channel)
        } else {
            holder.favButton.setImageResource(R.drawable.fav_channel)
        }
    }

    private fun showBottomSheet(videoItem: VideoItem, position: Int) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val bottomSheetView = LayoutInflater.from(context).inflate(R.layout.bts_item, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        val playlistName = bottomSheetView.findViewById<TextView>(R.id.playlist_name)
        playlistName.text = videoItem.fileName

        bottomSheetView.findViewById<View>(R.id.btn_close).setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetView.findViewById<View>(R.id.rename).setOnClickListener {
            bottomSheetDialog.dismiss()
            showRenameDialog(videoItem, position)
        }

        bottomSheetView.findViewById<View>(R.id.del).setOnClickListener {
            bottomSheetDialog.dismiss()
            showConfirmDeleteDialog(videoItem, position)
        }

        bottomSheetDialog.show()
    }

    private fun showRenameDialog(videoItem: VideoItem, position: Int) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        val width = (290 * context.resources.displayMetrics.density).toInt()
        val height = (215 * context.resources.displayMetrics.density).toInt()
        dialog.window?.setLayout(width, height)

        val nameEditText = dialogView.findViewById<EditText>(android.R.id.text1)
        nameEditText.setText(videoItem.fileName)

        dialogView.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.btn_delete).setOnClickListener {
            val newName = nameEditText.text.toString().trim()
            if (newName.isNotEmpty()) {
                val updatedVideo = videoItem.copy(fileName = newName)
                onRenameVideo(updatedVideo, newName)
                videoList[position] = updatedVideo // Cập nhật trực tiếp trong danh sách
                notifyItemChanged(position)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showConfirmDeleteDialog(videoItem: VideoItem, position: Int) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.confirm_del, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        val width = (290 * context.resources.displayMetrics.density).toInt()
        val height = (215 * context.resources.displayMetrics.density).toInt()
        dialog.window?.setLayout(width, height)

        val message = dialogView.findViewById<TextView>(R.id.message)
        message.text = context.getString(R.string.do_you_want_to_delete_channel, videoItem.fileName)

        dialogView.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.btn_delete).setOnClickListener {
            onDeleteVideo(videoItem)
            videoList.removeAt(position)
            notifyItemRemoved(position)
            dialog.dismiss()
        }

        dialog.show()
    }

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