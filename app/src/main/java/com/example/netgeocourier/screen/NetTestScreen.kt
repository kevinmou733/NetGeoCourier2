package com.example.netgeocourier.screen


import android.graphics.Paint
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.netgeocourier.R
import com.example.netgeocourier.data.NetTestResult
import com.example.netgeocourier.helper.FileHelper
import com.example.netgeocourier.helper.SpeedTestHelper
import com.example.netgeocourier.viewmodel.NetTestViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetTestScreen(
    viewModel: NetTestViewModel,
    onOpenEvaluation: () -> Unit
) {
    val context = LocalContext.current
    val isTesting by viewModel.isTesting.collectAsState()
    val isAutoTesting by viewModel.isAutoTesting.collectAsState()
    val testResults by viewModel.testResults.collectAsState()
    val currentResult by viewModel.curResult.collectAsState()
    val csvPath by viewModel.csvPath.collectAsState()
    val htmlPath by viewModel.htmlPath.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.network_test_controls),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.doTest(
                                onError = { errorMessage ->
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                }
                            )},
                            enabled = !isTesting && !isAutoTesting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.start_test), fontSize = 14.sp)
                        }

                        Button(
                            onClick = { viewModel.stopAutoTest() },
                            enabled = isTesting || isAutoTesting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.stop), fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveCsv(context) },
                            enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.save_csv), fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                if (isAutoTesting) viewModel.stopAutoTest() else viewModel.startAutoTest()
                            },
                            enabled = !isTesting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isAutoTesting) stringResource(R.string.stop_auto) else stringResource(R.string.auto_test),
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveAmapHtml(context) },
                            enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.generate_map), fontSize = 14.sp)
                        }

                        Button(
                            onClick = { viewModel.sendEmail(context) },
                            enabled = testResults.isNotEmpty() && !isTesting && !isAutoTesting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.send_email), fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onOpenEvaluation,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.open_network_evaluation), fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.latest_result),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentResult != null) {
                        ResultDetailCard(currentResult!!)
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.no_test_result_yet),
                                modifier = Modifier.padding(32.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                                text = stringResource(R.string.speed_trend),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${testResults.size} ${stringResource(R.string.records)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        ) {
                            SimpleSpeedChart(testResults = testResults)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.tap_chart_point_details),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.history),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (testResults.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.no_history_data),
                                modifier = Modifier.padding(32.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        testResults.reversed().forEachIndexed { index, result ->
                            ResultDetailItem(result = result, isFirst = index == 0)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultDetailCard(result: NetTestResult) {
    // 高德定位返回的坐标已是GCJ02，直接使用，无需转换
    val (gcjLat, gcjLon) = remember(result) {
        result.latitude to result.longitude
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
                text = result.timestamp,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SpeedItem(label = stringResource(R.string.download), value = result.download.toFloat(), color = Color(0xFF4285F4))
                    SpeedItem(label = stringResource(R.string.upload), value = result.upload.toFloat(), color = Color(0xFFEA4335))
                    SpeedItem(label = stringResource(R.string.ping), value = result.ping.toFloat(), color = Color(0xFF34A853), unit = stringResource(R.string.unit_ms_short))
                }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${stringResource(R.string.location)}: ${"%.4f".format(gcjLat)}, ${"%.4f".format(gcjLon)}",
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
            text = label,
            fontSize = 11.sp,
            color = color
        )
        Text(
            text = String.format(Locale.US, "%.2f", value),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = unit,
            fontSize = 9.sp,
            color = color.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ResultDetailItem(result: NetTestResult, isFirst: Boolean) {
    // 高德定位返回的坐标已是GCJ02，直接使用，无需转换
    val (gcjLat, gcjLon) = remember(result) {
        result.latitude to result.longitude
    }

    Column {
        if (!isFirst) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.timestamp,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${stringResource(R.string.download)} ${String.format(Locale.US, "%.2f", result.download)} ${stringResource(R.string.unit_mbps_short)} | ${stringResource(R.string.upload)} ${String.format(Locale.US, "%.2f", result.upload)} ${stringResource(R.string.unit_mbps_short)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${stringResource(R.string.location)} ${"%.4f".format(gcjLat)}, ${"%.4f".format(gcjLon)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${result.ping} ms",
                fontSize = 11.sp,
                color = Color(0xFF34A853)
            )
        }
    }
}

@Composable
fun SimpleSpeedChart(testResults: List<NetTestResult>) {
    if (testResults.isEmpty()) {
        return
    }

    var maxSpeed = 10f
    for (result in testResults) {
        val download = result.download.toFloat()
        val upload = result.upload.toFloat()
        if (download > maxSpeed) maxSpeed = download
        if (upload > maxSpeed) maxSpeed = upload
    }
    maxSpeed = if (maxSpeed <= 10f) 10f else ((maxSpeed + 9f) / 10f).toInt() * 10f

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
                    text = stringResource(R.string.test_dialog_title, selectedIndex + 1),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(text = "${stringResource(R.string.time_label)}: ${selectedResult!!.timestamp}", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SpeedItem(label = stringResource(R.string.download), value = selectedResult!!.download.toFloat(), color = Color(0xFF4285F4))
                        SpeedItem(label = stringResource(R.string.upload), value = selectedResult!!.upload.toFloat(), color = Color(0xFFEA4335))
                        SpeedItem(label = stringResource(R.string.ping), value = selectedResult!!.ping.toFloat(), color = Color(0xFF34A853), unit = stringResource(R.string.unit_ms_short))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.close))
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
    if (testResults.size < 2) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
                Text(
                    text = stringResource(R.string.need_two_records),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
        }
        return
    }

    val downloadPointPositions = remember { mutableStateListOf<Pair<Int, Offset>>() }
    val uploadPointPositions = remember { mutableStateListOf<Pair<Int, Offset>>() }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(testResults) {
                detectTapGestures { offset ->
                    var tappedIndex = -1

                    for ((index, pos) in downloadPointPositions) {
                        val dx = offset.x - pos.x
                        val dy = offset.y - pos.y
                        if (sqrt(dx * dx + dy * dy) < 30f) {
                            tappedIndex = index
                            break
                        }
                    }

                    if (tappedIndex == -1) {
                        for ((index, pos) in uploadPointPositions) {
                            val dx = offset.x - pos.x
                            val dy = offset.y - pos.y
                            if (sqrt(dx * dx + dy * dy) < 30f) {
                                tappedIndex = index
                                break
                            }
                        }
                    }

                    if (tappedIndex in testResults.indices) {
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

        val downloadPoints = Array(testResults.size) { index ->
            val x = paddingLeft + index * xStep
            val y = paddingTop + chartHeight * (1 - testResults[index].download.toFloat() / maxSpeed)
            Offset(x, y)
        }

        val uploadPoints = Array(testResults.size) { index ->
            val x = paddingLeft + index * xStep
            val y = paddingTop + chartHeight * (1 - testResults[index].upload.toFloat() / maxSpeed)
            Offset(x, y)
        }

        downloadPoints.forEachIndexed { index, point ->
            downloadPointPositions.add(index to point)
        }
        uploadPoints.forEachIndexed { index, point ->
            uploadPointPositions.add(index to point)
        }

        val downloadPath = Path().apply {
            downloadPoints.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }
        drawPath(
            path = downloadPath,
            color = Color(0xFF4285F4),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        downloadPoints.forEach { point ->
            drawCircle(color = Color(0xFF4285F4), radius = 7f, center = point)
            drawCircle(color = Color.White, radius = 3f, center = point)
        }

        val uploadPath = Path().apply {
            uploadPoints.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }
        drawPath(
            path = uploadPath,
            color = Color(0xFFEA4335),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        uploadPoints.forEach { point ->
            drawCircle(color = Color(0xFFEA4335), radius = 7f, center = point)
            drawCircle(color = Color.White, radius = 3f, center = point)
        }

        drawLine(
            color = Color.Black,
            start = Offset(paddingLeft, paddingTop),
            end = Offset(paddingLeft, height - paddingBottom),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color.Black,
            start = Offset(paddingLeft, height - paddingBottom),
            end = Offset(width - paddingRight, height - paddingBottom),
            strokeWidth = 1.5f
        )
    }
}
