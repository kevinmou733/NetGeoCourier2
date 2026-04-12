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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.netgeocourier.R
import com.example.netgeocourier.data.EvaluationData
import com.example.netgeocourier.helper.AuthTokenStore
import com.example.netgeocourier.network.ApiClient
import com.example.netgeocourier.network.EvaluationRepository
import com.example.netgeocourier.viewmodel.NetTestViewModel
import kotlinx.coroutines.launch
import java.util.Locale

private data class EvaluationUiState(
    val isLoading: Boolean = false,
    val data: EvaluationData? = null,
    val errorMessage: String? = null,
    val tokenMissing: Boolean = false
)

@Composable
fun EvaluationScreen(
    viewModel: NetTestViewModel,
    onBack: () -> Unit,
    onOpenAuth: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val session = AuthTokenStore.getSession(context)
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
                    errorMessage = context.getString(R.string.evaluation_missing_auth)
                )
                return@launch
            }

            uiState = EvaluationUiState(isLoading = true)
            val syncResult = viewModel.syncAllLocalRecords()
            if (syncResult.isFailure) {
                val throwable = syncResult.exceptionOrNull()
                uiState = EvaluationUiState(
                    errorMessage = throwable?.message ?: context.getString(R.string.request_failed)
                )
                return@launch
            }

            repository.getEvaluation()
                .onSuccess { result ->
                    uiState = EvaluationUiState(data = result)
                }
                .onFailure { throwable ->
                    uiState = EvaluationUiState(
                        errorMessage = throwable.message ?: context.getString(R.string.request_failed)
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
                    text = stringResource(R.string.network_evaluation),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.evaluation_api_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (session?.user == null) {
                        stringResource(R.string.evaluation_session_title)
                    } else {
                        stringResource(R.string.evaluation_signed_in_title)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (session?.user != null) {
                    Text(
                        text = "${session.user.displayName} (${session.user.username})",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onLogout) {
                            Text(stringResource(R.string.session_sign_out))
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.evaluation_signed_out_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onOpenAuth) {
                        Text(stringResource(R.string.evaluation_open_auth))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.back))
            }
            Button(
                onClick = { loadEvaluation() },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.refresh))
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
                    title = if (uiState.tokenMissing) stringResource(R.string.token_required) else stringResource(R.string.request_failed),
                    message = uiState.errorMessage ?: stringResource(R.string.unknown_error),
                    showLoginAction = true,
                    onOpenAuth = onOpenAuth
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
                text = stringResource(R.string.overall_score),
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
                text = stringResource(R.string.record_count, data.recordCount),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun LevelBadge(level: String) {
    val label = when (level.lowercase(Locale.getDefault())) {
        "excellent" -> stringResource(R.string.level_excellent)
        "good" -> stringResource(R.string.level_good)
        "fair" -> stringResource(R.string.level_fair)
        "poor" -> stringResource(R.string.level_poor)
        "no-data" -> stringResource(R.string.level_no_data)
        else -> level.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Text(
            text = "${stringResource(R.string.level)}: $label",
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
                text = stringResource(R.string.metrics),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            MetricRow(label = stringResource(R.string.average_download), value = formatMetric(data.metrics.downloadAvg, stringResource(R.string.unit_mbps_short)))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            MetricRow(label = stringResource(R.string.average_ping), value = formatMetric(data.metrics.pingAvg, stringResource(R.string.unit_ms_short)))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            MetricRow(label = stringResource(R.string.average_rssi), value = formatMetric(data.metrics.rssiAvg, stringResource(R.string.unit_dbm)))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            MetricRow(label = stringResource(R.string.average_snr), value = formatMetric(data.metrics.snrAvg, stringResource(R.string.unit_db)))
        }
    }
}

@Composable
private fun SuggestionsCard(suggestions: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.suggestions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (suggestions.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_suggestions),
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
private fun StatusCard(title: String, message: String, showLoginAction: Boolean, onOpenAuth: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            if (showLoginAction) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onOpenAuth) {
                    Text(stringResource(R.string.evaluation_open_auth))
                }
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
@Composable
private fun formatMetric(value: Double?, unit: String): String {
    if (value == null) {
        return stringResource(R.string.na)
    }

    val formatted = if (value % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.2f", value)
    }
    return "$formatted $unit"
}
