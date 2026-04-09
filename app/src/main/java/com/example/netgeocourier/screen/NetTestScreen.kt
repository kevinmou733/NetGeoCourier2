package com.example.netgeocourier.screen

import android.graphics.Paint
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.netgeocourier.R
import com.example.netgeocourier.data.NetTestResult
import com.example.netgeocourier.helper.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // 操作按钮卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { doTest() },
                            enabled = !isTesting && !isAutoTesting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("开始测试", fontSize = 14.sp)
                        }

                        Button(
                            onClick = { isTesting = false; stopAutoTest() },
                            enabled = isTesting || isAutoTesting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("停止测试", fontSize = 14.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { csvPath = FileHelper.saveCsv(context, testResults) },
                            enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("保存CSV", fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                if (!isAutoTesting) startAutoTest() else stopAutoTest()
                            },
                            enabled = !isTesting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                if (isAutoTesting) "自动测试中..." else "自动测试",
                                fontSize = 14.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { htmlPath = FileHelper.saveAmapHtml(context, testResults) },
                            enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("生成地图", fontSize = 14.sp)
                        }

                        Button(
                            onClick = { FileHelper.sendEmail(context, csvPath, htmlPath) },
                            enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("发送邮件", fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 最新结果卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "最新结果",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(8.dp))

                    if (curResult != null) {
                        ResultDetailCard(curResult!!)
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                "暂无测试数据",
                                modifier = Modifier.padding(32.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 折线图卡片
            if (testResults.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "速率趋势图",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "共 ${testResults.size} 次测试",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        ) {
                            SimpleSpeedChart(testResults = testResults)
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "提示：点击图表上的圆点查看详细信息",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            // 历史记录卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "历史记录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(8.dp))

                    if (testResults.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                "暂无历史数据，点击\"开始测试\"",
                                modifier = Modifier.padding(32.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        testResults.reversed().forEachIndexed { index, result ->
                            ResultDetailItem(result, index == 0)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultDetailCard(result: NetTestResult) {
    val (gcjLat, gcjLon) = remember(result) {
        CoordTransform.wgs84ToGcj02(result.latitude, result.longitude)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                result.timestamp,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpeedItem("下行", result.download.toFloat(), Color(0xFF4285F4))
                SpeedItem("上行", result.upload.toFloat(), Color(0xFFEA4335))
                SpeedItem("Ping", result.ping.toFloat(), Color(0xFF34A853), "ms")
            }

            Text(
                "坐标: ${"%.4f".format(gcjLat)}, ${"%.4f".format(gcjLon)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SpeedItem(label: String, value: Float, color: Color, unit: String = "Mbps") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            fontSize = 11.sp,
            color = color
        )
        Text(
            "${String.format("%.2f", value)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            unit,
            fontSize = 9.sp,
            color = color.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ResultDetailItem(result: NetTestResult, isFirst: Boolean) {
    val (gcjLat, gcjLon) = remember(result) {
        CoordTransform.wgs84ToGcj02(result.latitude, result.longitude)
    }

    Column {
        if (!isFirst) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.timestamp,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "📡 ${String.format("%.2f", result.download)} Mbps ↓  ${String.format("%.2f", result.upload)} Mbps ↑",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "📍 ${"%.4f".format(gcjLat)}, ${"%.4f".format(gcjLon)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Text(
                "🏓 ${result.ping}ms",
                fontSize = 11.sp,
                color = Color(0xFF34A853)
            )
        }
    }
}

@Composable
fun SimpleSpeedChart(testResults: List<NetTestResult>) {
    if (testResults.isEmpty()) return

    var maxSpeed = 10f
    for (result in testResults) {
        val download = result.download.toFloat()
        val upload = result.upload.toFloat()
        if (download > maxSpeed) maxSpeed = download
        if (upload > maxSpeed) maxSpeed = upload
    }
    maxSpeed = if (maxSpeed <= 10) 10f else ((maxSpeed + 9) / 10).toInt() * 10f

    var showDialog by remember { mutableStateOf(false) }
    var selectedResult by remember { mutableStateOf<NetTestResult?>(null) }
    var selectedIndex by remember { mutableStateOf(-1) }

    Box(modifier = Modifier.fillMaxSize()) {
        SpeedChartCanvas(
            testResults = testResults,
            maxSpeed = maxSpeed,
            onPointClick = { index, result ->
                selectedIndex = index
                selectedResult = result
                showDialog = true
            }
        )
    }

    if (showDialog && selectedResult != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    "第 ${selectedIndex + 1} 次测试",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("时间：${selectedResult!!.timestamp}", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SpeedItem("下行", selectedResult!!.download.toFloat(), Color(0xFF4285F4))
                        SpeedItem("上行", selectedResult!!.upload.toFloat(), Color(0xFFEA4335))
                        SpeedItem("Ping", selectedResult!!.ping.toFloat(), Color(0xFF34A853), "ms")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
fun SpeedChartCanvas(
    testResults: List<NetTestResult>,
    maxSpeed: Float,
    onPointClick: ((Int, NetTestResult) -> Unit)? = null
) {
    val downloadPointPositions = remember { mutableStateListOf<Pair<Int, Offset>>() }
    val uploadPointPositions = remember { mutableStateListOf<Pair<Int, Offset>>() }

    Canvas(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                var tappedIndex = -1

                for ((index, pos) in downloadPointPositions) {
                    val dx = offset.x - pos.x
                    val dy = offset.y - pos.y
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (distance < 30f) {
                        tappedIndex = index
                        break
                    }
                }

                if (tappedIndex == -1) {
                    for ((index, pos) in uploadPointPositions) {
                        val dx = offset.x - pos.x
                        val dy = offset.y - pos.y
                        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                        if (distance < 30f) {
                            tappedIndex = index
                            break
                        }
                    }
                }

                if (tappedIndex != -1 && tappedIndex < testResults.size) {
                    onPointClick?.invoke(tappedIndex, testResults[tappedIndex])
                }
            }
        }
    ) {
        downloadPointPositions.clear()
        uploadPointPositions.clear()

        val width = size.width
        val height = size.height
        val paddingLeft = 45f
        val paddingRight = 15f
        val paddingTop = 20f
        val paddingBottom = 35f
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        if (testResults.size < 2) return@Canvas

        val xStep = chartWidth / (testResults.size - 1)
        val ySteps = 5

        for (i in 0..ySteps) {
            val y = paddingTop + chartHeight * (1 - i.toFloat() / ySteps)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1f
            )
        }

        val textPaint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 26f
            textAlign = Paint.Align.RIGHT
        }

        for (i in 0..ySteps) {
            val value = (maxSpeed * i / ySteps).toInt()
            val y = paddingTop + chartHeight * (1 - i.toFloat() / ySteps)
            drawContext.canvas.nativeCanvas.drawText(
                value.toString(),
                paddingLeft - 8f,
                y + 8f,
                textPaint
            )
        }

        val labelPaint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }

        for (i in testResults.indices) {
            val x = paddingLeft + i * xStep
            val timeStr = testResults[i].timestamp
            val label = if (timeStr.length >= 16) timeStr.substring(11, 16) else timeStr
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                height - paddingBottom + 18f,
                labelPaint
            )
        }

        val downloadPoints = Array(testResults.size) { i ->
            val x = paddingLeft + i * xStep
            val y = paddingTop + chartHeight * (1 - testResults[i].download.toFloat() / maxSpeed)
            Offset(x, y)
        }

        val uploadPoints = Array(testResults.size) { i ->
            val x = paddingLeft + i * xStep
            val y = paddingTop + chartHeight * (1 - testResults[i].upload.toFloat() / maxSpeed)
            Offset(x, y)
        }

        for (i in downloadPoints.indices) {
            downloadPointPositions.add(Pair(i, downloadPoints[i]))
        }
        for (i in uploadPoints.indices) {
            uploadPointPositions.add(Pair(i, uploadPoints[i]))
        }

        val downloadPath = Path()
        for (i in downloadPoints.indices) {
            if (i == 0) downloadPath.moveTo(downloadPoints[i].x, downloadPoints[i].y)
            else downloadPath.lineTo(downloadPoints[i].x, downloadPoints[i].y)
        }
        drawPath(path = downloadPath, color = Color(0xFF4285F4), style = Stroke(width = 2.5f, cap = StrokeCap.Round))

        for (point in downloadPoints) {
            drawCircle(color = Color(0xFF4285F4), radius = 7f, center = point)
            drawCircle(color = Color.White, radius = 3f, center = point)
        }

        val uploadPath = Path()
        for (i in uploadPoints.indices) {
            if (i == 0) uploadPath.moveTo(uploadPoints[i].x, uploadPoints[i].y)
            else uploadPath.lineTo(uploadPoints[i].x, uploadPoints[i].y)
        }
        drawPath(path = uploadPath, color = Color(0xFFEA4335), style = Stroke(width = 2.5f, cap = StrokeCap.Round))

        for (point in uploadPoints) {
            drawCircle(color = Color(0xFFEA4335), radius = 7f, center = point)
            drawCircle(color = Color.White, radius = 3f, center = point)
        }

        drawLine(color = Color.Black, start = Offset(paddingLeft, paddingTop), end = Offset(paddingLeft, height - paddingBottom), strokeWidth = 1.5f)
        drawLine(color = Color.Black, start = Offset(paddingLeft, height - paddingBottom), end = Offset(width - paddingRight, height - paddingBottom), strokeWidth = 1.5f)
    }
}