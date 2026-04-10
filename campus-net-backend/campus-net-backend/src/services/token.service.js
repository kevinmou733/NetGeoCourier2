const crypto = require("node:crypto");

const { config } = require("../config");
const { createHttpError } = require("../utils/httpErrors");

function parseDurationToSeconds(value) {
  const match = String(value || "").trim().match(/^(\d+)([smhd])?$/i);
  if (!match) {
    return 7 * 24 * 60 * 60;
  }

  const amount = Number.parseInt(match[1], 10);
  const unit = (match[2] || "s").toLowerCase();
  const multipliers = {
    s: 1,
    m: 60,
    h: 60 * 60,
    d: 24 * 60 * 60,
  };
  return amount * multipliers[unit];
}

function encodeJson(value) {
  return Buffer.from(JSON.stringify(value)).toString("base64url");
}

function signInput(input) {
  return crypto.createHmac("sha256", config.jwtSecret).update(input).digest("base64url");
}

function safeEqual(left, right) {
  const leftBuffer = Buffer.from(left);
  const rightBuffer = Buffer.from(right);
  return leftBuffer.length === rightBuffer.length && crypto.timingSafeEqual(leftBuffer, rightBuffer);
}

const tokenService = {
  sign(payload) {
    const now = Math.floor(Date.now() / 1000);
    const expiresIn = parseDurationToSeconds(config.jwtExpiresIn);
    const header = { alg: "HS256", typ: "JWT" };
    const body = {
      ...payload,
      iat: now,
      exp: now + expiresIn,
    };

    const encodedHeader = encodeJson(header);
    const encodedPayload = encodeJson(body);
    const unsignedToken = `${encodedHeader}.${encodedPayload}`;
    const signature = signInput(unsignedToken);

    return {
      accessToken: `${unsignedToken}.${signature}`,
      expiresIn,
    };
  },

  verify(token) {
    const parts = String(token || "").split(".");
    if (parts.length !== 3) {
      throw createHttpError(401, "AUTH_INVALID_TOKEN", "Invalid token.");
    }

    const [encodedHeader, encodedPayload, signature] = parts;
    const expectedSignature = signInput(`${encodedHeader}.${encodedPayload}`);
    if (!safeEqual(signature, expectedSignature)) {
      throw createHttpError(401, "AUTH_INVALID_TOKEN", "Invalid token signature.");
    }

    let payload;
    try {
      const header = JSON.parse(Buffer.from(encodedHeader, "base64url").toString("utf8"));
      payload = JSON.parse(Buffer.from(encodedPayload, "base64url").toString("utf8"));
      if (header.alg !== "HS256" || header.typ !== "JWT") {
        throw new Error("Unsupported JWT header.");
      }
    } catch {
      throw createHttpError(401, "AUTH_INVALID_TOKEN", "Invalid token payload.");
    }

    const now = Math.floor(Date.now() / 1000);
    if (!payload.exp || payload.exp < now) {
      throw createHttpError(401, "AUTH_TOKEN_EXPIRED", "Token has expired.");
    }

    return payload;
  },
};

module.exports = { tokenService, parseDurationToSeconds };
