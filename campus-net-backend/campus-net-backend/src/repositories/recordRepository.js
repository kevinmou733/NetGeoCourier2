const crypto = require("node:crypto");

const { database } = require("./jsonDatabase");

function normalizeLimit(limit) {
  if (!Number.isInteger(limit) || limit <= 0) {
    return 50;
  }
  return Math.min(limit, 200);
}

function normalizeNumber(value, digits = 4) {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return null;
  }
  return Number(value.toFixed(digits));
}

function buildRecordSignature(userId, record) {
  const metrics = record && typeof record.metrics === "object" ? record.metrics : {};
  const location =
    record && record.location && typeof record.location === "object" ? record.location : null;

  return JSON.stringify({
    userId,
    capturedAt: String(record.capturedAt || ""),
    metrics: {
      downloadMbps: normalizeNumber(metrics.downloadMbps),
      uploadMbps: normalizeNumber(metrics.uploadMbps),
      pingMs: Number.isInteger(metrics.pingMs) ? metrics.pingMs : null,
      latencyMs: Number.isInteger(metrics.latencyMs) ? metrics.latencyMs : null,
      download: normalizeNumber(metrics.download),
      upload: normalizeNumber(metrics.upload),
      ping: Number.isInteger(metrics.ping) ? metrics.ping : null,
      rssi: normalizeNumber(metrics.rssi, 2),
      snr: normalizeNumber(metrics.snr, 2),
    },
    location: location
      ? {
          latitude: normalizeNumber(location.latitude, 6),
          longitude: normalizeNumber(location.longitude, 6),
          source: String(location.source || ""),
        }
      : null,
    remark: String(record.remark || "").trim(),
  });
}

const recordRepository = {
  async createMany(userId, records) {
    return database.transaction(async (data) => {
      const now = new Date().toISOString();
      const userRecords = data.records.filter((record) => record.userId === userId);
      const existingBySignature = new Map(
        userRecords.map((record) => [buildRecordSignature(userId, record), record])
      );

      const savedRecords = records.map((record) => {
        const normalizedRecord = {
          capturedAt: record.capturedAt || now,
          metrics: record.metrics,
          location: record.location || null,
          remark: record.remark || "",
        };
        const signature = buildRecordSignature(userId, normalizedRecord);
        const existingRecord = existingBySignature.get(signature);
        if (existingRecord) {
          return existingRecord;
        }

        const savedRecord = {
          id: crypto.randomUUID(),
          userId,
          ...normalizedRecord,
          createdAt: now,
        };

        data.records.push(savedRecord);
        existingBySignature.set(signature, savedRecord);
        return savedRecord;
      });

      return savedRecords;
    });
  },

  async listByUserId(userId, options = {}) {
    const limit = normalizeLimit(options.limit);
    const data = await database.read();
    return data.records
      .filter((record) => record.userId === userId)
      .sort((left, right) => String(right.createdAt).localeCompare(String(left.createdAt)))
      .slice(0, limit);
  },
};

module.exports = { recordRepository };
