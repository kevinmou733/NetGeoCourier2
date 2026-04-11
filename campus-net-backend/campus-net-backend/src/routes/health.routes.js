const express = require("express");

const { ok } = require("../utils/responses");

const router = express.Router();

router.get("/", (req, res) => {
  ok(res, {
    service: "campus-net-backend",
    status: "up",
    time: new Date().toISOString(),
  });
});

module.exports = router;
