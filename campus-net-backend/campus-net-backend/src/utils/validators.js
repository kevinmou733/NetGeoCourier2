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
    throw createHttpError(400, "VALIDATION_ERROR", `${fieldName} is too long.`);
  }
  return text;
}

function validateUsername(username) {
  const value = String(username || "").trim();
  if (!USERNAME_PATTERN.test(value)) {
    throw createHttpError(
      400,
      "VALIDATION_ERROR",
      "username must be 3-64 chars and only contain letters, numbers, underscore, dot, @ or hyphen."
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
      `password must be ${config.passwordMinLength}-128 chars.`
    );
  }
  return value;
}

function validateRegister(body) {
  ensureObject(body, "VALIDATION_ERROR", "Request body must be a JSON object.");
  return {
    username: validateUsername(body.username),
    password: validatePassword(body.password),
    displayName: readOptionalString(body.displayName, 50, "displayName"),
    studentId: readOptionalString(body.studentId, 32, "studentId"),
  };
}

function validateLogin(body) {
  ensureObject(body, "VALIDATION_ERROR", "Request body must be a JSON object.");
  return {
    username: validateUsername(body.username),
    password: validatePassword(body.password),
  };
}

function validateRecordBatch(body) {
  ensureObject(body, "VALIDATION_ERROR", "Request body must be a JSON object.");
  if (!Array.isArray(body.records) || body.records.length === 0) {
    throw createHttpError(400, "VALIDATION_ERROR", "records must be a non-empty array.");
  }
  if (body.records.length > 100) {
    throw createHttpError(400, "VALIDATION_ERROR", "records cannot exceed 100 items per batch.");
  }

  return body.records.map((record, index) => {
    ensureObject(record, "VALIDATION_ERROR", `records[${index}] must be an object.`);
    ensureObject(record.metrics, "VALIDATION_ERROR", `records[${index}].metrics must be an object.`);

    return {
      capturedAt: readOptionalString(record.capturedAt, 40, `records[${index}].capturedAt`),
      metrics: record.metrics,
      location: record.location || null,
      remark: readOptionalString(record.remark, 200, `records[${index}].remark`),
    };
  });
}

module.exports = { validateRegister, validateLogin, validateRecordBatch };
