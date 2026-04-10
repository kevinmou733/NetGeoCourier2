package com.example.netgeocourier.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.netgeocourier.data.NetTestResult
import com.example.netgeocourier.helper.CoordTransform
import com.example.netgeocourier.helper.FileHelper
import com.example.netgeocourier.helper.LocationHelper
import com.example.netgeocourier.helper.SpeedTestHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NetTestScreen(
    locationHelper: LocationHelper,
    onOpenEvaluation: () -> Unit
) {
    val context = LocalContext.current
    var isTesting by remember { mutableStateOf(false) }
    var isAutoTesting by remember { mutableStateOf(false) }
    var autoJob by remember { mutableStateOf<Job?>(null) }
    var testResults by remember { mutableStateOf(listOf<NetTestResult>()) }
    var currentResult by remember { mutableStateOf<NetTestResult?>(null) }
    var csvPath by remember { mutableStateOf<String?>(null) }
    var htmlPath by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun doTest(onFinish: (() -> Unit)? = null) {
        isTesting = true
        currentResult = null
        coroutineScope.launch {
            val location = locationHelper.getCurrentLocation(context)
            if (location == null) {
                Toast.makeText(context, "Failed to get location.", Toast.LENGTH_SHORT).show()
                isTesting = false
                onFinish?.invoke()
                return@launch
            }

            val download = SpeedTestHelper.measureDownloadSpeed()
            val upload = SpeedTestHelper.measureUploadSpeed()
            val ping = SpeedTestHelper.measurePing()

            val timeText = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val result = NetTestResult(
                timestamp = timeText,
                latitude = location.latitude,
                longitude = location.longitude,
                upload = upload,
                download = download,
                ping = ping
            )

            currentResult = result
            testResults = testResults + result
            isTesting = false
            onFinish?.invoke()
        }
    }

    fun startAutoTest() {
        if (isAutoTesting) {
            return
        }
        isAutoTesting = true
        autoJob = coroutineScope.launch {
            while (isAutoTesting) {
                val waitForSingleTest = CompletableDeferred<Unit>()
                doTest { waitForSingleTest.complete(Unit) }
                waitForSingleTest.await()
                delay(5000)
            }
        }
    }

    fun stopAutoTest() {
        isAutoTesting = false
        autoJob?.cancel()
        autoJob = null
        isTesting = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(text = "NetGeoCourier", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Measure upload/download speed and open the evaluation page when you need a summary.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { doTest() },
                enabled = !isTesting && !isAutoTesting,
                modifier = Modifier.weight(1f)
            ) {
                Text("Start Test")
            }

            Button(
                onClick = { stopAutoTest() },
                enabled = isTesting || isAutoTesting,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { csvPath = FileHelper.saveCsv(context, testResults) },
                enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save CSV")
            }

            OutlinedButton(
                onClick = {
                    if (isAutoTesting) stopAutoTest() else startAutoTest()
                },
                enabled = !isTesting,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isAutoTesting) "Stop Auto" else "Auto Test")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { htmlPath = FileHelper.saveAmapHtml(context, testResults) },
                enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
                modifier = Modifier.weight(1f)
            ) {
                Text("Generate Map")
            }

            OutlinedButton(
                onClick = { FileHelper.sendEmail(context, csvPath, htmlPath) },
                enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
                modifier = Modifier.weight(1f)
            ) {
                Text("Send Email")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onOpenEvaluation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Network Evaluation")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Latest Result", style = MaterialTheme.typography.titleMedium)

        if (currentResult != null) {
            ResultDetail(currentResult!!)
        } else {
            Text(text = "No test result yet.", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "History", style = MaterialTheme.typography.titleSmall)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            if (testResults.isEmpty()) {
                Text(text = "No history data.")
            }
            testResults.reversed().forEach {
                ResultDetail(it)
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ResultDetail(result: NetTestResult) {
    val (gcjLat, gcjLon) = remember(result) {
        CoordTransform.wgs84ToGcj02(result.latitude, result.longitude)
    }

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text("Time: ${result.timestamp}", style = MaterialTheme.typography.bodySmall)
        Text(
            "Location (GCJ-02): ${"%.6f".format(gcjLat)}, ${"%.6f".format(gcjLon)}",
            style = MaterialTheme.typography.bodySmall
        )
        Text("Upload: ${"%.2f".format(result.upload)} Mbps", style = MaterialTheme.typography.bodySmall)
        Text("Download: ${"%.2f".format(result.download)} Mbps", style = MaterialTheme.typography.bodySmall)
        Text("Ping: ${result.ping} ms", style = MaterialTheme.typography.bodySmall)
    }
}
