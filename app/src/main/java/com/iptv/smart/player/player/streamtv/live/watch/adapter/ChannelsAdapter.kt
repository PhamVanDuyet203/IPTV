package com.iptv.smart.player.player.streamtv.live.watch.adapter

import android.app.AlertDialog
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

class ChannelsAdapter(
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
        val diffResult = DiffUtil.calculateDiff(ChannelDiffCallback(channels, newChannels))
        newChannels.forEach {
            Log.d("TAGTAGlistUrl", "updateChannels: " + it.streamUrl)
            listUrl.add(it)
        }
        channels.clear()
        channels.addAll(newChannels)
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateChannel(channel: Channel) {
        val position = channels.indexOfFirst { it.streamUrl == channel.streamUrl }
        if (position != -1) {
            channels[position] = channel
            notifyItemChanged(position)
        }
    }

    inner class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val logoImageView: ImageView = itemView.findViewById(R.id.logoImageView)
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val favButton: ImageButton = itemView.findViewById(R.id.fav_channel)

        fun bind(channel: Channel) {
            nameTextView.text = channel.name
            Glide.with(itemView.context).load(channel.logoUrl).placeholder(R.drawable.ic_tv)
                .into(logoImageView)

            updateFavoriteIcon(channel.isFavorite)

            favButton.setOnClickListener {
                onFavoriteClicked(channel)
                channel.isFavorite = !channel.isFavorite
                updateFavoriteIcon(channel.isFavorite)
                notifyItemChanged(adapterPosition)
            }


            itemView.setOnClickListener {
                onChannelClicked(channel)
            }
        }

        private fun updateFavoriteIcon(isFavorite: Boolean) {
            if (isFavorite) {
                favButton.setImageResource(R.drawable.fav_on_channel)
            } else {
                favButton.setImageResource(R.drawable.fav_channel)
            }
        }

        private fun showBottomSheet(channel: Channel) {
            val context = itemView.context
            val bottomSheetDialog = BottomSheetDialog(context)
            val bottomSheetView = LayoutInflater.from(context).inflate(R.layout.bts_item, null)
            bottomSheetDialog.setContentView(bottomSheetView)

            val playlistName = bottomSheetView.findViewById<TextView>(R.id.playlist_name)
            playlistName.text = channel.name

            bottomSheetView.findViewById<View>(R.id.btn_close).setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            bottomSheetView.findViewById<View>(R.id.rename).setOnClickListener {
                bottomSheetDialog.dismiss()
                showRenameDialog(channel)
            }

            bottomSheetView.findViewById<View>(R.id.del).setOnClickListener {
                bottomSheetDialog.dismiss()
                showConfirmDeleteDialog(channel)
            }

            bottomSheetDialog.show()
        }

        private fun showRenameDialog(channel: Channel) {
            val context = itemView.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null)
            val dialog = AlertDialog.Builder(context).setView(dialogView).create()

            val width = (290 * context.resources.displayMetrics.density).toInt()
            val height = (215 * context.resources.displayMetrics.density).toInt()
            dialog.window?.setLayout(width, height)

            val nameEditText = dialogView.findViewById<EditText>(android.R.id.text1)
            nameEditText.setText(channel.name)

            dialogView.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
                dialog.dismiss()
            }

            dialogView.findViewById<TextView>(R.id.btn_delete).setOnClickListener {
                val newName = nameEditText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val updatedChannel = channel.copy(name = newName)
                    onRenameChannel(updatedChannel, newName)
                }
                dialog.dismiss()
            }

            dialog.show()
        }

        private fun showConfirmDeleteDialog(channel: Channel) {
            val context = itemView.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.confirm_del, null)
            val dialog = AlertDialog.Builder(context).setView(dialogView).create()

            val width = (290 * context.resources.displayMetrics.density).toInt()
            val height = (215 * context.resources.displayMetrics.density).toInt()
            dialog.window?.setLayout(width, height)

            val message = dialogView.findViewById<TextView>(R.id.message)
            message.text = context.getString(R.string.do_you_want_to_delete_channel, channel.name)

            dialogView.findViewById<TextView>(R.id.btn_cancel).setOnClickListener {
                dialog.dismiss()
            }

            dialogView.findViewById<TextView>(R.id.btn_delete).setOnClickListener {
                onDeleteChannel(channel)
                dialog.dismiss()
            }

            dialog.show()
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
