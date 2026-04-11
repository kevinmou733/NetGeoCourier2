package com.example.netgeocourier.helper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.netgeocourier.BuildConfig
import com.example.netgeocourier.data.NetTestResult
import java.io.File
import java.io.FileOutputStream

object FileHelper {

    fun getDocumentsDir(context: Context): File? {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (dir != null && !dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun saveCsv(context: Context, list: List<NetTestResult>): String? {
        val dir = getDocumentsDir(context) ?: return null
        val file = File(dir, "nettest.csv")
        val isNewFile = !file.exists()

        FileOutputStream(file, true).bufferedWriter().use { out ->
            if (isNewFile) {
                out.write("timestamp,longitude_gcj02,latitude_gcj02,upload_mbps,download_mbps,ping_ms\n")
            }
            list.forEach {
                val (gcjLat, gcjLon) = CoordTransform.wgs84ToGcj02(it.latitude, it.longitude)
                out.write(
                    "${it.timestamp},$gcjLon,${"%.6f".format(gcjLat)}," +
                        "${"%.2f".format(it.upload)},${"%.2f".format(it.download)},${it.ping}\n"
                )
            }
        }

        Toast.makeText(context, "CSV saved: ${file.name}", Toast.LENGTH_SHORT).show()
        return file.absolutePath
    }

    fun saveAmapHtml(context: Context, list: List<NetTestResult>): String? {
        val dir = getDocumentsDir(context) ?: return null
        val file = File(dir, "netmap_${System.currentTimeMillis()}.html")
        val key = BuildConfig.AMAP_WEB_KEY

        if (key.isBlank()) {
            Toast.makeText(context, "Please configure AMAP_WEB_KEY in local.properties", Toast.LENGTH_LONG).show()
            return null
        }

        val first = list.firstOrNull()
        val (centerLat, centerLon) = first?.let {
            CoordTransform.wgs84ToGcj02(it.latitude, it.longitude)
        } ?: (39.90923 to 116.397428)

        val markers = list.joinToString("\n") { item ->
            val (lat, lon) = CoordTransform.wgs84ToGcj02(item.latitude, item.longitude)
            """
            |new AMap.Marker({
            |  position: [$lon, $lat],
            |  map: map,
            |  title: "${item.timestamp} upload ${"%.2f".format(item.upload)} download ${"%.2f".format(item.download)}"
            |});
            """.trimMargin()
        }

        val html = """
            |<!DOCTYPE html>
            |<html>
            |<head>
            |  <meta charset="utf-8">
            |  <title>Network Test Map</title>
            |  <style>html,body,#container{width:100%;height:100%;margin:0;padding:0;}</style>
            |  <script src="https://webapi.amap.com/maps?v=2.0&key=$key"></script>
            |</head>
            |<body>
            |  <div id="container"></div>
            |  <script>
            |    var map = new AMap.Map('container', {
            |      resizeEnable: true,
            |      zoom: 13,
            |      center: [$centerLon, $centerLat]
            |    });
            |    $markers
            |  </script>
            |</body>
            |</html>
        """.trimMargin()

        file.writeText(html)
        Toast.makeText(context, "Map HTML generated: ${file.name}", Toast.LENGTH_SHORT).show()
        return file.absolutePath
    }

    fun sendEmail(context: Context, csvPath: String?, htmlPath: String?) {
        if (csvPath == null && htmlPath == null) {
            Toast.makeText(context, "No files to send.", Toast.LENGTH_SHORT).show()
            return
        }

        val email = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_SUBJECT, "Network Test Data")
        }

        val uris = ArrayList<Uri>()
        csvPath?.let {
            uris.add(
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(it)
                )
            )
        }
        htmlPath?.let {
            uris.add(
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(it)
                )
            )
        }

        email.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        email.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(email, "Send network test files"))
    }
}
