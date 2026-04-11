package com.example.netgeocourier.helper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.netgeocourier.BuildConfig
import com.example.netgeocourier.data.NetTestResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileHelper {
    private const val TAG = "FileHelper"
    private const val CSV_FILE_NAME = "nettest.csv"

    fun getDocumentsDir(context: Context): File? {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (dir != null && !dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getCsvFile(context: Context): File? {
        val dir = getDocumentsDir(context) ?: return null
        return File(dir, CSV_FILE_NAME)
    }

    /**
     * 追加单条记录到CSV文件
     * @return 文件路径，如果失败返回null
     */
    fun appendCsvRecord(context: Context, result: NetTestResult): String? {
        val file = getCsvFile(context) ?: return null

        return try {
            // 如果文件不存在，先写入表头
            if (!file.exists()) {
                file.writeText("timestamp,longitude_gcj02,latitude_gcj02,upload_mbps,download_mbps,ping_ms\n")
            }

            // 追加单条记录
            FileOutputStream(file, true).bufferedWriter().use { out ->
                val gcjLat = result.latitude
                val gcjLon = result.longitude
                out.write(
                    "${result.timestamp},$gcjLon,${"%.6f".format(gcjLat)}," +
                        "${"%.2f".format(result.upload)},${"%.2f".format(result.download)},${result.ping}\n"
                )
            }

            Log.d(TAG, "Appended record to CSV: ${file.name}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append CSV record: ${e.message}", e)
            Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * 追加多条记录到CSV文件（用于手动保存批量新数据）
     * @return 文件路径，如果失败返回null
     */
    fun appendCsvRecords(context: Context, results: List<NetTestResult>): String? {
        if (results.isEmpty()) return null

        val file = getCsvFile(context) ?: return null

        return try {
            // 如果文件不存在，先写入表头
            if (!file.exists()) {
                file.writeText("timestamp,longitude_gcj02,latitude_gcj02,upload_mbps,download_mbps,ping_ms\n")
            }

            // 追加所有记录
            FileOutputStream(file, true).bufferedWriter().use { out ->
                results.forEach {
                    val gcjLat = it.latitude
                    val gcjLon = it.longitude
                    out.write(
                        "${it.timestamp},$gcjLon,${"%.6f".format(gcjLat)}," +
                            "${"%.2f".format(it.upload)},${"%.2f".format(it.download)},${it.ping}\n"
                    )
                }
            }

            Log.d(TAG, "Appended ${results.size} records to CSV: ${file.name}")
            Toast.makeText(context, "CSV saved: ${file.name} (+${results.size} records)", Toast.LENGTH_SHORT).show()
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append CSV records: ${e.message}", e)
            Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * 生成高德地图HTML文件并立即删除，不保存到本地
     * @return HTML内容，如果失败返回null
     */
    fun generateAmapHtmlContent(context: Context, list: List<NetTestResult>): String? {
        val key = BuildConfig.AMAP_WEB_KEY

        if (key.isBlank()) {
            Toast.makeText(context, "Please configure AMAP_WEB_KEY in local.properties", Toast.LENGTH_LONG).show()
            return null
        }

        if (list.isEmpty()) {
            Toast.makeText(context, "No data to generate map", Toast.LENGTH_SHORT).show()
            return null
        }

        val first = list.firstOrNull()!!
        val (centerLat, centerLon) = first.latitude to first.longitude

        val markers = list.joinToString("\n") { item ->
            val (lat, lon) = item.latitude to item.longitude
            """
            |new AMap.Marker({
            |  position: [$lon, $lat],
            |  map: map,
            |  title: "${item.timestamp}"
            |});
            """.trimMargin()
        }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <title>Network Test Map</title>
              <style>html,body,#container{width:100%;height:100%;margin:0;padding:0;}</style>
              <script src="https://webapi.amap.com/maps?v=2.0&key=$key"></script>
            </head>
            <body>
              <div id="container"></div>
              <script>
                var map = new AMap.Map('container', {
                  resizeEnable: true,
                  zoom: 13,
                  center: [$centerLon, $centerLat]
                });
                $markers
                // 启用定位蓝点
                map.setMyLocationEnabled(true);
                var myLocationStyle = new AMap.MyLocationStyle();
                myLocationStyle.myLocationType(AMap.MyLocationType.LOCATION_TYPE_LOCATION_ROTATE);
                myLocationStyle.interval(2000);
                map.setMyLocationStyle(myLocationStyle);
              </script>
            </body>
            </html>
        """.trimIndent()

        Log.d(TAG, "Generated map HTML content for ${list.size} points")
        return html
    }

    /**
     * 生成高德地图HTML文件（为了兼容旧调用，已废弃）
     * @return null（文件不保存）
     */
    @Deprecated("Use generateAmapHtmlContent instead. Map files are not persisted.")
    fun saveAmapHtml(context: Context, list: List<NetTestResult>): String? {
        // 不再保存文件到本地
        Toast.makeText(context, "Map generated (not saved locally)", Toast.LENGTH_SHORT).show()
        return null
    }

    /**
     * 发送邮件（支持动态生成的HTML内容，不依赖本地文件）
     */
    fun sendEmailWithGeneratedMap(context: Context, csvPath: String?, mapHtmlContent: String?) {
        if (csvPath == null && mapHtmlContent == null) {
            Toast.makeText(context, "No data to send", Toast.LENGTH_SHORT).show()
            return
        }

        val email = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_SUBJECT, "Network Test Data")
        }

        val uris = ArrayList<Uri>()

        // 添加CSV文件
        csvPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                uris.add(
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                )
            }
        }

        // 动态生成HTML临时文件
        var tempHtmlFile: File? = null
        if (mapHtmlContent != null) {
            try {
                val dir = context.cacheDir  // 使用缓存目录，自动清理
                tempHtmlFile = File.createTempFile("netmap_", ".html", dir)
                tempHtmlFile.writeText(mapHtmlContent)
                uris.add(
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        tempHtmlFile
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create temp HTML file: ${e.message}", e)
            }
        }

        email.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        email.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(email, "Send network test files"))

        // 发送后删除临时文件（延迟一点，确保邮件客户端已读取）
        tempHtmlFile?.deleteOnExit()
    }
}
