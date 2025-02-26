package com.samyak2403.iptvmine.util

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Định nghĩa data class cho kênh với thêm logoUrl
data class Channel(
    val name: String,
    val groupTitle: String?,
    val url: String,
    val logoUrl: String? = null // Thêm logoUrl, mặc định là null nếu không có
)

// Hàm phân tích tệp M3U
suspend fun parseM3U(url: String): List<Channel> = withContext(Dispatchers.IO) {
    val channels = mutableListOf<Channel>()
    val connection = URL(url).openConnection()
    val reader = BufferedReader(InputStreamReader(connection.getInputStream()))

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
                currentGroup = groupMatch ?: "Unknown" // Gán "Unknown" nếu không có group-title
                currentLogoUrl = logoMatch // Lấy logoUrl, giữ null nếu không có
            } else if (line.isNotBlank() && !line.startsWith("#")) {
                currentUrl = line.trim()
                channels.add(Channel(currentName, currentGroup, currentUrl, currentLogoUrl))
                currentName = ""
                currentGroup = null
                currentUrl = ""
                currentLogoUrl = null
            }
        }
    }
    return@withContext channels
}