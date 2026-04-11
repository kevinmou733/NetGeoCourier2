const crypto = require("node:crypto");

const { database } = require("./jsonDatabase");

function normalizeLimit(limit) {
  if (!Number.isInteger(limit) || limit <= 0) {
    return 50;
  }
  return Math.min(limit, 200);
}

const recordRepository = {
  async createMany(userId, records) {
    return database.transaction(async (data) => {
      const now = new Date().toISOString();
      const savedRecords = records.map((record) => ({
        id: crypto.randomUUID(),
        userId,
        capturedAt: record.capturedAt || now,
        metrics: record.metrics,
        location: record.location || null,
        remark: record.remark || "",
        createdAt: now,
      }));

      data.records.push(...savedRecords);
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
