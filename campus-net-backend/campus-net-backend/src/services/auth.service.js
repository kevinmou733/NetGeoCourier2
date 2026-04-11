const { userRepository } = require("../repositories/userRepository");
const { tokenService } = require("./token.service");
const { hashPassword, verifyPassword } = require("../utils/password");
const { createHttpError } = require("../utils/httpErrors");
const { validateLogin, validateRegister } = require("../utils/validators");

function buildAuthResponse(user) {
  const publicUser = userRepository.toPublicUser(user);
  const token = tokenService.sign({
    sub: publicUser.id,
    username: publicUser.username,
  });

  return {
    tokenType: "Bearer",
    accessToken: token.accessToken,
    expiresIn: token.expiresIn,
    user: publicUser,
  };
}

const authService = {
  async register(body) {
    const input = validateRegister(body);
    const passwordHash = await hashPassword(input.password);
    const user = await userRepository.createUser({
      ...input,
      passwordHash,
    });
    return buildAuthResponse(user);
  },

  async login(body) {
    const input = validateLogin(body);
    const user = await userRepository.findByUsername(input.username);
    if (!user) {
      throw createHttpError(401, "AUTH_INVALID_CREDENTIALS", "Invalid username or password.");
    }

    const isValidPassword = await verifyPassword(input.password, user.passwordHash);
    if (!isValidPassword) {
      throw createHttpError(401, "AUTH_INVALID_CREDENTIALS", "Invalid username or password.");
    }

    return buildAuthResponse(user);
  },
};

module.exports = { authService };
