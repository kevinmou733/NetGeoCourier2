const { authService } = require("../services/auth.service");
const { ok } = require("../utils/responses");

async function register(req, res) {
  const result = await authService.register(req.body);
  ok(res, result, "ok", 201);
}

async function login(req, res) {
  const result = await authService.login(req.body);
  ok(res, result);
}

async function me(req, res) {
  ok(res, { user: req.user });
}

async function logout(req, res) {
  ok(res, { loggedOut: true }, "Client should remove the local access token.");
}

module.exports = { register, login, me, logout };
