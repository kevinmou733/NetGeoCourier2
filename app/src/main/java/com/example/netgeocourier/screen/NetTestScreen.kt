package com.example.netgeocourier.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.netgeocourier.R
import com.example.netgeocourier.data.NetTestResult
import com.example.netgeocourier.helper.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NetTestScreen(locationHelper: LocationHelper) {
    val context = LocalContext.current
    var isTesting by remember { mutableStateOf(false) }
    var isAutoTesting by remember { mutableStateOf(false) }
    var autoJob by remember { mutableStateOf<Job?>(null) }
    var testResults by remember { mutableStateOf(listOf<NetTestResult>()) }
    var curResult by remember { mutableStateOf<NetTestResult?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var csvPath by remember { mutableStateOf<String?>(null) }
    var htmlPath by remember { mutableStateOf<String?>(null) }

    fun doTest(onFinish: (() -> Unit)? = null) {
        isTesting = true
        curResult = null
        coroutineScope.launch {
            val location = locationHelper.getCurrentLocation(context)
            if (location == null) {
                Toast.makeText(context, "定位失败", Toast.LENGTH_SHORT).show()
                isTesting = false
                onFinish?.invoke()
                return@launch
            }

            val download = SpeedTestHelper.measureDownloadSpeed()
            val upload = SpeedTestHelper.measureUploadSpeed()
            val ping = SpeedTestHelper.measurePing()

            val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val result = NetTestResult(
                timestamp = timeStr,
                latitude = location.latitude,
                longitude = location.longitude,
                upload = upload,
                download = download,
                ping = ping
            )

            curResult = result
            testResults = testResults + result
            isTesting = false
            onFinish?.invoke()
        }
    }

    fun startAutoTest() {
        if (isAutoTesting) return
        isAutoTesting = true
        autoJob = coroutineScope.launch {
            while (isAutoTesting) {
                val job = CompletableDeferred<Unit>()
                doTest { job.complete(Unit) }
                job.await()
                delay(5000)
            }
        }
    }

    fun stopAutoTest() {
        isAutoTesting = false
        autoJob?.cancel()
        autoJob = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { doTest() },
                enabled = !isTesting && !isAutoTesting,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.start_test)) }

            Button(
                onClick = { isTesting = false; stopAutoTest() },
                enabled = isTesting || isAutoTesting,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.stop_test)) }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { csvPath = FileHelper.saveCsv(context, testResults) },
                enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
                modifier = Modifier.weight(1f)
            ) { Text("保存CSV") }

            Button(
                onClick = {
                    if (!isAutoTesting) startAutoTest() else stopAutoTest()
                },
                enabled = !isTesting,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isAutoTesting) "测试中.." else "自动测试")
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { htmlPath = FileHelper.saveAmapHtml(context, testResults) },
                enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
                modifier = Modifier.weight(1f)
            ) { Text("生成地图") }

            Button(
                onClick = { FileHelper.sendEmail(context, csvPath, htmlPath) },
                enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
                modifier = Modifier.weight(1f)
            ) { Text("发送邮件") }
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.last_result), style = MaterialTheme.typography.titleMedium)

        if (curResult != null) {
            ResultDetail(curResult!!)
        } else {
            Text(stringResource(R.string.empty), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.history), style = MaterialTheme.typography.titleSmall)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            if (testResults.isEmpty()) {
                Text(stringResource(R.string.empty))
            }
            testResults.reversed().forEach {
                ResultDetail(it)
                Divider()
            }
        }
    }
}

@Composable
fun ResultDetail(result: NetTestResult) {
    val (gcjLat, gcjLon) = remember(result) {
        CoordTransform.wgs84ToGcj02(result.latitude, result.longitude)
    }

    Column(modifier = Modifier.padding(6.dp)) {
        Text("时间：${result.timestamp}", style = MaterialTheme.typography.bodySmall)
        Text("经纬度(GCJ-02)：${"%.6f".format(gcjLat)}, ${"%.6f".format(gcjLon)}", style = MaterialTheme.typography.bodySmall)
        Text("上行速率：${"%.2f".format(result.upload)} Mbps", style = MaterialTheme.typography.bodySmall)
        Text("下行速率：${"%.2f".format(result.download)} Mbps", style = MaterialTheme.typography.bodySmall)
        Text("Ping：${result.ping} ms", style = MaterialTheme.typography.bodySmall)
    }
}
