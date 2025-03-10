package com.iptv.smart.player.player.streamtv.live.watch.util

import com.iptv.smart.player.player.streamtv.live.watch.model.Channel
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun parseM3U(url: String): List<Channel> = withContext(Dispatchers.IO) {
    val channels = mutableListOf<Channel>()
    val connection = URL(url).openConnection()
    val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
    parseM3UContent(reader, channels)
    channels
}

suspend fun parseM3UFromFile(inputStream: InputStream): List<Channel> = withContext(Dispatchers.IO) {
    val channels = mutableListOf<Channel>()
    val reader = BufferedReader(InputStreamReader(inputStream))
    parseM3UContent(reader, channels)
    channels
}

private fun parseM3UContent(reader: BufferedReader, channels: MutableList<Channel>) {
    var currentName = ""
    var currentGroup: String? = null
    var currentUrl = ""
    var currentLogoUrl: String? = null

    reader.useLines { lines ->
        lines.forEach { line ->
            if (line.startsWith("#EXTINF")) {
                val nameMatch = Regex("tvg-name=\"([^\"]+)\"").find(line)?.groupValues?.get(1)
                val groupMatch = Regex("group-title=\"([^\"]+)\"").find(line)?.groupValues?.get(1)
                val logoMatch = Regex("tvg-logo=\"([^\"]+)\"").find(line)?.groupValues?.get(1)

                currentName = nameMatch ?: line.substringAfter(",").trim()
                currentGroup = groupMatch ?: "Unknown"
                currentLogoUrl = logoMatch ?: "assets/images/ic_tv.png"
            } else if (line.isNotBlank() && !line.startsWith("#")) {
                currentUrl = line.trim()
                channels.add(
                    Channel(
                        name = currentName,
                        logoUrl = currentLogoUrl ?: "assets/images/ic_tv.png",
                        streamUrl = currentUrl,
                        groupTitle = currentGroup,
                        isFavorite = false
                    )
                )
                currentName = ""
                currentGroup = null
                currentUrl = ""
                currentLogoUrl = null
            }
        }
    }
}