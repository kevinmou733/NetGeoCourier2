const express = require("express");

const recordsController = require("../controllers/records.controller");
const { authenticate } = require("../middleware/authenticate");
const { asyncHandler } = require("../utils/asyncHandler");

const router = express.Router();

router.use(authenticate);
router.get("/", asyncHandler(recordsController.listMine));
router.post("/", asyncHandler(recordsController.uploadOne));
router.post("/batch", asyncHandler(recordsController.uploadBatch));

module.exports = router;
