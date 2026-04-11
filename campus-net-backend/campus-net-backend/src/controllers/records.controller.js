const { recordRepository } = require("../repositories/recordRepository");
const { ok } = require("../utils/responses");
const { validateRecordBatch } = require("../utils/validators");

async function uploadBatch(req, res) {
  const records = validateRecordBatch(req.body);
  const savedRecords = await recordRepository.createMany(req.user.id, records);
  ok(res, { count: savedRecords.length, records: savedRecords }, "ok", 201);
}

async function uploadOne(req, res) {
  const records = validateRecordBatch({ records: [req.body] });
  const savedRecords = await recordRepository.createMany(req.user.id, records);
  ok(res, { record: savedRecords[0] }, "ok", 201);
}

async function listMine(req, res) {
  const limit = Number.parseInt(req.query.limit, 10) || 50;
  const records = await recordRepository.listByUserId(req.user.id, { limit });
  ok(res, { records });
}

module.exports = { uploadBatch, uploadOne, listMine };
