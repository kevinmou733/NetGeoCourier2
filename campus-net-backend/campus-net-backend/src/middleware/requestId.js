const crypto = require("node:crypto");

function requestId(req, res, next) {
  req.id = req.headers["x-request-id"] || crypto.randomUUID();
  res.setHeader("x-request-id", req.id);
  next();
}

module.exports = { requestId };
