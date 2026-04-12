const path = require("node:path");

const envFile = process.env.ENV_FILE || path.resolve(__dirname, "../../.env");
require("dotenv").config({ path: envFile });

const projectRoot = path.resolve(__dirname, "../..");

function parsePort(value, fallback) {
  const parsed = Number.parseInt(value, 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

function resolveProjectPath(value, fallback) {
  const target = value || fallback;
  return path.isAbsolute(target) ? target : path.resolve(projectRoot, target);
}

function parseList(value) {
  return String(value || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

const config = {
  projectRoot,
  envFile,
  nodeEnv: process.env.NODE_ENV || "development",
  host: process.env.HOST || "0.0.0.0",
  port: 3000,
  jwtSecret: process.env.JWT_SECRET || "change-me-local-dev-secret",
  jwtExpiresIn: process.env.JWT_EXPIRES_IN || "7d",
  dataFile: resolveProjectPath(process.env.DATA_FILE, "./data/local-db.json"),
  passwordMinLength: parsePort(process.env.PASSWORD_MIN_LENGTH, 6),
  allowedOrigins: parseList(process.env.ALLOWED_ORIGINS),
};

function warnIfUsingDefaultSecret() {
  if (config.jwtSecret === "change-me-local-dev-secret") {
    console.warn("[config] JWT_SECRET is using the local development default.");
  }
}

module.exports = { config, warnIfUsingDefaultSecret };
