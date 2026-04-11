const { config } = require("../config");
const { createHttpError } = require("./httpErrors");

const USERNAME_PATTERN = /^[a-zA-Z0-9_.@-]{3,64}$/;

function ensureObject(value, code, message) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw createHttpError(400, code, message);
  }
}

function readOptionalString(value, maxLength, fieldName) {
  if (value === undefined || value === null) {
    return "";
  }

  const text = String(value).trim();
  if (text.length > maxLength) {
    throw createHttpError(400, "VALIDATION_ERROR", `${fieldName} 长度超出限制。`);
  }
  return text;
}

function validateUsername(username) {
  const value = String(username || "").trim();
  if (!USERNAME_PATTERN.test(value)) {
    throw createHttpError(
      400,
      "VALIDATION_ERROR",
      "用户名长度需为 3-64 个字符，且只能包含字母、数字、下划线、点、@ 或连字符。"
    );
  }
  return value;
}

function validatePassword(password) {
  const value = String(password || "");
  if (value.length < config.passwordMinLength || value.length > 128) {
    throw createHttpError(
      400,
      "VALIDATION_ERROR",
      `密码长度需为 ${config.passwordMinLength}-128 个字符。`
    );
  }
  return value;
}

function validateRegister(body) {
  ensureObject(body, "VALIDATION_ERROR", "请求体必须是 JSON 对象。");
  return {
    username: validateUsername(body.username),
    password: validatePassword(body.password),
    displayName: readOptionalString(body.displayName, 50, "displayName"),
    studentId: readOptionalString(body.studentId, 32, "studentId"),
  };
}

function validateLogin(body) {
  ensureObject(body, "VALIDATION_ERROR", "请求体必须是 JSON 对象。");
  return {
    username: validateUsername(body.username),
    password: validatePassword(body.password),
  };
}

function validateRecordBatch(body) {
  ensureObject(body, "VALIDATION_ERROR", "请求体必须是 JSON 对象。");
  if (!Array.isArray(body.records) || body.records.length === 0) {
    throw createHttpError(400, "VALIDATION_ERROR", "records 必须是非空数组。");
  }
  if (body.records.length > 100) {
    throw createHttpError(400, "VALIDATION_ERROR", "单次上传的 records 不能超过 100 条。");
  }

  return body.records.map((record, index) => {
    ensureObject(record, "VALIDATION_ERROR", `records[${index}] 必须是对象。`);
    ensureObject(record.metrics, "VALIDATION_ERROR", `records[${index}].metrics 必须是对象。`);

    return {
      capturedAt: readOptionalString(record.capturedAt, 40, `records[${index}].capturedAt`),
      metrics: record.metrics,
      location: record.location || null,
      remark: readOptionalString(record.remark, 200, `records[${index}].remark`),
    };
  });
}

module.exports = { validateRegister, validateLogin, validateRecordBatch };
