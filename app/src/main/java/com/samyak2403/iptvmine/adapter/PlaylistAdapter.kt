package com.samyak2403.iptvmine.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.db.PlaylistEntity
import com.samyak2403.iptvmine.ChannelListActivity
import com.samyak2403.iptvmine.screens.VideoDetailActivity

class PlaylistAdapter(
    private var playlists: List<PlaylistEntity>,
    private val onDeletePlaylist: (PlaylistEntity) -> Unit = {},
    private val onRenamePlaylist: (PlaylistEntity, String) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.playlistName)
        val count: TextView = itemView.findViewById(R.id.channelCount)
        val optButton: ImageButton = itemView.findViewById(R.id.opt_channel)
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

        Log.d("PlaylistAdapter", "SourceType: ${playlist.sourceType}") //

        // Thay đổi icon dựa trên sourceType
        val iconRes = when (playlist.sourceType) {
            "URL" -> R.drawable.link_channel
            "FILE" -> R.drawable.link_docs
            "DEVICE" -> R.drawable.link_video
            else -> R.drawable.link_channel // Mặc định
        }
        holder.itemView.findViewById<ImageView>(R.id.img_item_playlist).setImageResource(iconRes)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            if (playlist.sourceType == "DEVICE" && !playlist.sourcePath.endsWith(".m3u")) {
                val intent = Intent(context, VideoDetailActivity::class.java)
                intent.putExtra("GROUP_NAME", playlist.name)
                intent.putExtra("SOURCE_PATH", playlist.sourcePath)
                context.startActivity(intent)
            } else {
                val intent = Intent(context, ChannelListActivity::class.java)
                intent.putExtra("GROUP_NAME", playlist.name)
                intent.putExtra("SOURCE_PATH", playlist.sourcePath)
                context.startActivity(intent)
            }
        }

        holder.optButton.setOnClickListener {
            showBottomSheet(holder.itemView.context, playlist, position)
        }
    }

    override fun getItemCount() = playlists.size

    fun updateData(newPlaylists: List<PlaylistEntity>) {
        playlists = newPlaylists
        notifyDataSetChanged()
    }

    private fun showBottomSheet(context: Context, playlist: PlaylistEntity, position: Int) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val bottomSheetView = LayoutInflater.from(context).inflate(R.layout.bts_item, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        val playlistName = bottomSheetView.findViewById<TextView>(R.id.playlist_name)
        playlistName.text = playlist.name

        bottomSheetView.findViewById<View>(R.id.btn_close).setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetView.findViewById<View>(R.id.rename).setOnClickListener {
            bottomSheetDialog.dismiss()
            showRenameDialog(context, playlist, position)
        }

        bottomSheetView.findViewById<View>(R.id.del).setOnClickListener {
            bottomSheetDialog.dismiss()
            showConfirmDeleteDialog(context, playlist, position)
        }

        bottomSheetDialog.show()
    }

    private fun showRenameDialog(context: Context, playlist: PlaylistEntity, position: Int) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        dialog.show()

        val width = (290 * context.resources.displayMetrics.density).toInt()
        val height = (215 * context.resources.displayMetrics.density).toInt()
        dialog.window?.apply {
            setLayout(width, height)
            setBackgroundDrawableResource(R.drawable.bg_confirm_del) // Áp dụng background với border radius
        }

        val nameEditText = dialogView.findViewById<EditText>(android.R.id.text1)
        val btnDelText = dialogView.findViewById<ImageView>(R.id.btn_del_text)
        nameEditText.setText(playlist.name)

        // Ban đầu, hiển thị btn_del_text nếu EditText có giá trị
        btnDelText.visibility = if (nameEditText.text.isNotEmpty()) View.VISIBLE else View.GONE

        // Xử lý sự kiện khi nhấn btn_del_text
        btnDelText.setOnClickListener {
            nameEditText.text.clear() // Xóa nội dung EditText
            btnDelText.visibility = View.GONE // Ẩn nút xóa
        }

        // Theo dõi thay đổi trong EditText để hiển thị/ẩn btn_del_text
        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                btnDelText.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
        })

        dialogView.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.btn_delete).setOnClickListener {
            val newName = nameEditText.text.toString().trim()
            if (newName.isNotEmpty() && newName != playlist.name) {
                val updatedPlaylist = playlist.copy(name = newName)
                onRenamePlaylist(updatedPlaylist, newName) // Gọi callback để lưu vào DB
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showConfirmDeleteDialog(context: Context, playlist: PlaylistEntity, position: Int) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.confirm_del, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        dialog.show()

        val width = (312 * context.resources.displayMetrics.density).toInt()
        dialog.window?.apply {
            setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(R.drawable.bg_confirm_del) // Áp dụng background với border radius
        }

        val message = dialogView.findViewById<TextView>(R.id.message)
        val fullText = context.getString(R.string.do_you_want_to_delete_playlist, playlist.name)
        val spannableString = android.text.SpannableString(fullText)
        val start = fullText.indexOf(playlist.name)
        val end = start + playlist.name.length
        spannableString.setSpan(
            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            start,
            end,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        message.text = spannableString

        dialogView.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<TextView>(R.id.btn_delete).setOnClickListener {
            onDeletePlaylist(playlist) // Gọi callback để xóa khỏi DB
            dialog.dismiss()
        }

        dialog.show()
    }
}