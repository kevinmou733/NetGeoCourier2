function ok(res, data, message = "ok", statusCode = 200) {
  res.status(statusCode).json({
    success: true,
    message,
    data,
  });
}

function errorResponse(res, statusCode, code, message, requestId) {
  res.status(statusCode).json({
    success: false,
    message,
    error: {
      code,
      message,
      requestId,
    },
  });
}

module.exports = { ok, errorResponse };
