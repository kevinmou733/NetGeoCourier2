const { tokenService } = require("../services/token.service");
const { userRepository } = require("../repositories/userRepository");
const { createHttpError } = require("../utils/httpErrors");

function readBearerToken(headerValue) {
  const [scheme, token] = String(headerValue || "").split(" ");
  if (scheme !== "Bearer" || !token) {
    return null;
  }
  return token;
}

function authenticate(req, res, next) {
  Promise.resolve()
    .then(async () => {
      const token = readBearerToken(req.headers.authorization);
      if (!token) {
        throw createHttpError(401, "AUTH_MISSING_TOKEN", "Missing Bearer token.");
      }

      const payload = tokenService.verify(token);
      const user = await userRepository.findById(payload.sub);
      if (!user) {
        throw createHttpError(401, "AUTH_USER_NOT_FOUND", "Token user no longer exists.");
      }

      req.auth = payload;
      req.user = userRepository.toPublicUser(user);
      next();
    })
    .catch(next);
}

module.exports = { authenticate };
