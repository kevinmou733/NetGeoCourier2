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

object FileHelper {
    private const val TAG = "FileHelper"
    private const val CSV_FILE_NAME = "nettest.csv"

    fun getDocumentsDir(context: Context): File? {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (dir != null && !dir.exists()) dir.mkdirs()
        return dir
    }

    fun getCsvFile(context: Context): File? {
        val dir = getDocumentsDir(context) ?: return null
        return File(dir, CSV_FILE_NAME)
    }

    fun appendCsvRecord(context: Context, result: NetTestResult): String? {
        val file = getCsvFile(context) ?: return null
        return try {
            if (!file.exists()) {
                file.writeText("timestamp,longitude_gcj02,latitude_gcj02,upload_mbps,download_mbps,ping_ms\n")
            }
            FileOutputStream(file, true).bufferedWriter().use { out ->
                out.write("${result.timestamp},${result.longitude},${"%.6f".format(result.latitude)}," +
                        "${"%.2f".format(result.upload)},${"%.2f".format(result.download)},${result.ping}\n")
            }
            Log.d(TAG, "Appended CSV record")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "CSV append failed: ${e.message}", e)
            null
        }
    }

    fun appendCsvRecords(context: Context, results: List<NetTestResult>): String? {
        if (results.isEmpty()) return null
        val file = getCsvFile(context) ?: return null
        return try {
            if (!file.exists()) {
                file.writeText("timestamp,longitude_gcj02,latitude_gcj02,upload_mbps,download_mbps,ping_ms\n")
            }
            FileOutputStream(file, true).bufferedWriter().use { out ->
                results.forEach {
                    out.write("${it.timestamp},${it.longitude},${"%.6f".format(it.latitude)}," +
                            "${"%.2f".format(it.upload)},${"%.2f".format(it.download)},${it.ping}\n")
                }
            }
            Log.d(TAG, "Appended ${results.size} CSV records")
            Toast.makeText(context, "Saved +${results.size} records", Toast.LENGTH_SHORT).show()
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "CSV batch save failed: ${e.message}", e)
            null
        }
    }

    fun getCsvRecordCount(context: Context): Int {
        val file = getCsvFile(context) ?: return 0
        if (!file.exists()) return 0
        return try { file.readLines().size - 1 } catch (e: Exception) { 0 }
    }

    fun generateAmapHtmlContent(context: Context, list: List<NetTestResult>): String? {
        val key = BuildConfig.AMAP_WEB_KEY
        if (key.isBlank()) {
            Toast.makeText(context, "Configure AMAP_WEB_KEY", Toast.LENGTH_LONG).show()
            return null
        }
        if (list.isEmpty()) {
            Toast.makeText(context, "No data", Toast.LENGTH_SHORT).show()
            return null
        }
         val first = list.first()
         val markers = list.joinToString("\n") { item ->
             "new AMap.Marker({position: [${item.longitude}, ${item.latitude}], map: map, title: '${item.timestamp}\\n下载: ${item.download} Mbps\\n上传: ${item.upload} Mbps', label: {content: '↓${item.download} ↑${item.upload}', style: {fontSize: '10px', color: '#333', background: 'rgba(255,255,255,0.7)', padding: '2px 4px', borderRadius: '3px'}}});"
         }
        return """
            <!DOCTYPE html><html><head>
            <meta charset="utf-8"><title>Map</title>
            <style>html,body,#container{width:100%;height:100%;margin:0;padding:0;}</style>
            <script src="https://webapi.amap.com/maps?v=2.0&key=$key"></script>
            </head><body><div id="container"></div><script>
            var map = new AMap.Map('container', {resizeEnable:true, zoom:13, center: [${first.longitude}, ${first.latitude}]});
            $markers
            map.setMyLocationEnabled(true);
            </script></body></html>
        """.trimIndent()
    }

    fun generateAndOpenMap(context: Context, list: List<NetTestResult>): Boolean {
        if (list.isEmpty()) {
            Toast.makeText(context, "No data", Toast.LENGTH_SHORT).show()
            return false
        }
        val html = generateAmapHtmlContent(context, list) ?: return false
        return try {
            val tempFile = File.createTempFile("netmap_", ".html", context.cacheDir)
            tempFile.writeText(html)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/html")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open map"))
            Thread {
                try { Thread.sleep(3000) } catch (e: InterruptedException) {}
                tempFile.delete()
                Log.d(TAG, "Deleted temp map file")
            }.start()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Open map failed: ${e.message}", e)
            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun sendEmailWithGeneratedMap(context: Context, csvPath: String?, mapHtmlContent: String?) {
        if (csvPath == null && mapHtmlContent == null) {
            Toast.makeText(context, "No files to send", Toast.LENGTH_SHORT).show()
            return
        }
        val email = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_SUBJECT, "Network Test Data")
        }
        val uris = ArrayList<Uri>()
        csvPath?.let { path ->
            File(path).takeIf { it.exists() }?.let {
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it))
            }
        }
        var tempFile: File? = null
        if (mapHtmlContent != null) {
            try {
                tempFile = File.createTempFile("netmap_", ".html", context.cacheDir)
                tempFile.writeText(mapHtmlContent)
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile))
            } catch (e: Exception) {
                Log.e(TAG, "Temp file error: ${e.message}", e)
            }
        }
        email.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        email.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(email, "Send email"))
        tempFile?.deleteOnExit()
    }

    /**
     * 仅发送CSV文件（不包含地图）
     */
    fun sendEmailCsvOnly(context: Context, csvPath: String?) {
        if (csvPath == null) {
            Toast.makeText(context, "No CSV file to send", Toast.LENGTH_SHORT).show()
            return
        }
        val file = File(csvPath)
        if (!file.exists()) {
            Toast.makeText(context, "CSV file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val email = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Network Test CSV Data")
            putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(email, "Send CSV"))
    }
}
