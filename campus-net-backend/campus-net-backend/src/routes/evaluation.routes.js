const express = require("express");

const evaluationController = require("../controllers/evaluation.controller");
const { authenticate } = require("../middleware/authenticate");
const { asyncHandler } = require("../utils/asyncHandler");

const router = express.Router();

router.use(authenticate);
router.get("/", asyncHandler(evaluationController.getEvaluation));

module.exports = router;
