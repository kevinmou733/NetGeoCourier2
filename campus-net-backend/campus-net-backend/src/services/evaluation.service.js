const { database } = require("../repositories/jsonDatabase");

function isFiniteNumber(value) {
  return typeof value === "number" && Number.isFinite(value);
}

function readMetric(metrics, keys) {
  for (const key of keys) {
    const value = metrics ? metrics[key] : undefined;
    if (isFiniteNumber(value)) {
      return value;
    }
  }
  return null;
}

function average(values) {
  if (values.length === 0) {
    return null;
  }

  const total = values.reduce((sum, value) => sum + value, 0);
  return Number((total / values.length).toFixed(2));
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

function scoreDownload(value) {
  return clamp(value, 0, 100);
}

function scorePing(value) {
  return clamp(((200 - value) / 180) * 100, 0, 100);
}

function scoreRssi(value) {
  return clamp(((value + 90) / 40) * 100, 0, 100);
}

function scoreSnr(value) {
  return clamp(((value - 10) / 30) * 100, 0, 100);
}

function buildLevel(score) {
  if (score >= 85) {
    return "excellent";
  }
  if (score >= 70) {
    return "good";
  }
  if (score >= 55) {
    return "fair";
  }
  return "poor";
}

function buildSuggestions(metrics) {
  const suggestions = [];

  if (metrics.downloadAvg !== null && metrics.downloadAvg < 20) {
    suggestions.push("Download speed is low. Check whether the access point bandwidth is saturated.");
  }

  if (metrics.pingAvg !== null && metrics.pingAvg > 80) {
    suggestions.push("Ping is high. Try testing closer to the access point or reducing interference.");
  }

  if (metrics.rssiAvg !== null && metrics.rssiAvg < -70) {
    suggestions.push("RSSI is weak. Consider moving closer to the router or adjusting antenna placement.");
  }

  if (metrics.snrAvg !== null && metrics.snrAvg < 20) {
    suggestions.push("SNR is low. There may be strong radio interference in the current area.");
  }

  if (suggestions.length === 0) {
    suggestions.push("Overall network status is stable. Keep monitoring during peak campus hours.");
  }

  return suggestions;
}

function buildScore(metrics) {
  const parts = [];

  if (metrics.downloadAvg !== null) {
    parts.push(scoreDownload(metrics.downloadAvg));
  }
  if (metrics.pingAvg !== null) {
    parts.push(scorePing(metrics.pingAvg));
  }
  if (metrics.rssiAvg !== null) {
    parts.push(scoreRssi(metrics.rssiAvg));
  }
  if (metrics.snrAvg !== null) {
    parts.push(scoreSnr(metrics.snrAvg));
  }

  if (parts.length === 0) {
    return 0;
  }

  const total = parts.reduce((sum, value) => sum + value, 0);
  return Math.round(total / parts.length);
}

const evaluationService = {
  async evaluateByUserId(userId) {
    const data = await database.read();
    const userRecords = data.records.filter((record) => record.userId === userId);

    const downloads = userRecords
      .map((record) => readMetric(record.metrics, ["downloadMbps", "download"]))
      .filter((value) => value !== null);
    const pings = userRecords
      .map((record) => readMetric(record.metrics, ["pingMs", "latencyMs", "ping"]))
      .filter((value) => value !== null);
    const rssiValues = userRecords
      .map((record) => readMetric(record.metrics, ["rssi"]))
      .filter((value) => value !== null);
    const snrValues = userRecords
      .map((record) => readMetric(record.metrics, ["snr"]))
      .filter((value) => value !== null);

    const metrics = {
      downloadAvg: average(downloads),
      pingAvg: average(pings),
      rssiAvg: average(rssiValues),
      snrAvg: average(snrValues),
    };

    if (userRecords.length === 0) {
      return {
        score: 0,
        level: "no-data",
        suggestions: ["No network records yet. Upload records before requesting an evaluation."],
        metrics,
        recordCount: 0,
      };
    }

    const score = buildScore(metrics);

    return {
      score,
      level: buildLevel(score),
      suggestions: buildSuggestions(metrics),
      metrics,
      recordCount: userRecords.length,
    };
  },
};

module.exports = { evaluationService };
