const { evaluationService } = require("../services/evaluation.service");
const { ok } = require("../utils/responses");

async function getEvaluation(req, res) {
  const result = await evaluationService.evaluateByUserId(req.user.id);
  ok(res, result);
}

module.exports = { getEvaluation };
