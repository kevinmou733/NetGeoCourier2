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
    suggestions.push("下载速度偏低，建议检查当前接入点带宽是否已经接近饱和。");
  }

  if (metrics.pingAvg !== null && metrics.pingAvg > 80) {
    suggestions.push("延迟偏高，建议靠近接入点重新测试，或排查周边干扰源。");
  }

  if (metrics.rssiAvg !== null && metrics.rssiAvg < -70) {
    suggestions.push("RSSI 信号偏弱，建议靠近路由器或调整天线部署位置。");
  }

  if (metrics.snrAvg !== null && metrics.snrAvg < 20) {
    suggestions.push("SNR 偏低，当前区域可能存在较强的无线干扰。");
  }

  if (suggestions.length === 0) {
    suggestions.push("当前网络整体较稳定，建议继续关注校园高峰时段的表现。");
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
        suggestions: ["暂无网络记录，请先完成测速并同步后再查看评估。"],
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
