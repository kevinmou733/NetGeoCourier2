const { errorResponse } = require("../utils/responses");

function notFoundHandler(req, res, next) {
  next({
    statusCode: 404,
    code: "NOT_FOUND",
    message: `Route ${req.method} ${req.originalUrl} was not found.`,
  });
}

function errorHandler(err, req, res, next) {
  if (res.headersSent) {
    next(err);
    return;
  }

  const statusCode = err.statusCode || err.status || 500;
  const code = err.code || "INTERNAL_ERROR";
  const message = statusCode >= 500 ? "Internal server error." : err.message;

  if (statusCode >= 500) {
    console.error(`[${req.id}]`, err);
  }

  errorResponse(res, statusCode, code, message, req.id);
}

module.exports = { notFoundHandler, errorHandler };
