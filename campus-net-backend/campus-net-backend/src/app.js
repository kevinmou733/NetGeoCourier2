const express = require("express");
const cors = require("cors");

const { config } = require("./config");
const { requestId } = require("./middleware/requestId");
const { errorHandler, notFoundHandler } = require("./middleware/errorHandler");
const authRoutes = require("./routes/auth.routes");
const evaluationRoutes = require("./routes/evaluation.routes");
const healthRoutes = require("./routes/health.routes");
const recordRoutes = require("./routes/records.routes");

function createCorsOptions() {
  if (config.allowedOrigins.length === 0) {
    return { origin: true };
  }
  return {
    origin(origin, callback) {
      if (!origin || config.allowedOrigins.includes(origin)) {
        callback(null, true);
        return;
      }
      callback(new Error("Origin is not allowed by CORS"));
    },
  };
}

function createApp() {
  const app = express();

  app.disable("x-powered-by");
  app.use(requestId);
  app.use(cors(createCorsOptions()));
  app.use(express.json({ limit: "1mb" }));

  app.use("/api/v1/health", healthRoutes);
  app.use("/api/v1/auth", authRoutes);
  app.use("/api/v1/records", recordRoutes);
  app.use("/api/v1/evaluation", evaluationRoutes);

  app.use(notFoundHandler);
  app.use(errorHandler);

  return app;
}

module.exports = { createApp };
