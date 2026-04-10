package com.example.netgeocourier.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.netgeocourier.data.EvaluationData
import com.example.netgeocourier.helper.AuthTokenStore
import com.example.netgeocourier.network.ApiClient
import com.example.netgeocourier.network.EvaluationRepository
import kotlinx.coroutines.launch
import java.util.Locale

private data class EvaluationUiState(
    val isLoading: Boolean = false,
    val data: EvaluationData? = null,
    val errorMessage: String? = null,
    val tokenMissing: Boolean = false
)

@Composable
fun EvaluationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) {
        EvaluationRepository(ApiClient.evaluationService(context))
    }
    val coroutineScope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf(EvaluationUiState(isLoading = true)) }
    val scrollState = rememberScrollState()

    fun loadEvaluation() {
        coroutineScope.launch {
            val token = AuthTokenStore.getAccessToken(context)
            if (token.isNullOrBlank()) {
                uiState = EvaluationUiState(
                    isLoading = false,
                    tokenMissing = true,
                    errorMessage = "No saved access token was found. Save the login accessToken before opening this page."
                )
                return@launch
            }

            uiState = EvaluationUiState(isLoading = true)
            repository.getEvaluation()
                .onSuccess { result ->
                    uiState = EvaluationUiState(data = result)
                }
                .onFailure { throwable ->
                    uiState = EvaluationUiState(
                        errorMessage = throwable.message ?: "Failed to load evaluation data."
                    )
                }
        }
    }

    LaunchedEffect(Unit) {
        loadEvaluation()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Network Evaluation",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pulls GET /api/v1/evaluation with the saved Bearer token.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            Button(
                onClick = { loadEvaluation() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Refresh")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.data != null -> {
                EvaluationSummaryCard(uiState.data!!)
                Spacer(modifier = Modifier.height(16.dp))
                MetricsCard(uiState.data!!)
                Spacer(modifier = Modifier.height(16.dp))
                SuggestionsCard(uiState.data!!.suggestions)
            }

            else -> {
                StatusCard(
                    title = if (uiState.tokenMissing) "Token Required" else "Request Failed",
                    message = uiState.errorMessage ?: "Unknown error.",
                    showHint = uiState.tokenMissing
                )
            }
        }
    }
}

@Composable
private fun EvaluationSummaryCard(data: EvaluationData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Overall Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = data.score.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            LevelBadge(level = data.level)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Record count: ${data.recordCount}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun LevelBadge(level: String) {
    val label = level.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Text(
            text = "Level: $label",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MetricsCard(data: EvaluationData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            MetricRow(label = "Average download", value = formatMetric(data.metrics.downloadAvg, "Mbps"))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            MetricRow(label = "Average ping", value = formatMetric(data.metrics.pingAvg, "ms"))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            MetricRow(label = "Average RSSI", value = formatMetric(data.metrics.rssiAvg, "dBm"))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            MetricRow(label = "Average SNR", value = formatMetric(data.metrics.snrAvg, "dB"))
        }
    }
}

@Composable
private fun SuggestionsCard(suggestions: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Suggestions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (suggestions.isEmpty()) {
                Text(
                    text = "No suggestions returned.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                suggestions.forEachIndexed { index, suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(999.dp)
                                )
                        ) {
                            Text(
                                text = "${index + 1}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, message: String, showHint: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            if (showHint) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Save the login response field data.accessToken with AuthTokenStore.saveAccessToken(context, accessToken).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatMetric(value: Double?, unit: String): String {
    if (value == null) {
        return "--"
    }

    val formatted = if (value % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.2f", value)
    }
    return "$formatted $unit"
}
