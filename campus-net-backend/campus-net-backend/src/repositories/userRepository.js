const crypto = require("node:crypto");

const { database } = require("./jsonDatabase");
const { createHttpError } = require("../utils/httpErrors");

function toPublicUser(user) {
  return {
    id: user.id,
    username: user.username,
    displayName: user.displayName,
    studentId: user.studentId,
    createdAt: user.createdAt,
    updatedAt: user.updatedAt,
  };
}

function normalizeUsername(username) {
  return String(username || "").trim().toLowerCase();
}

const userRepository = {
  toPublicUser,

  async findByUsername(username) {
    const normalized = normalizeUsername(username);
    const data = await database.read();
    return data.users.find((user) => user.usernameNormalized === normalized) || null;
  },

  async findById(id) {
    const data = await database.read();
    return data.users.find((user) => user.id === id) || null;
  },

  async createUser(input) {
    return database.transaction(async (data) => {
      const usernameNormalized = normalizeUsername(input.username);
      const existing = data.users.find((user) => user.usernameNormalized === usernameNormalized);
      if (existing) {
        throw createHttpError(409, "AUTH_USERNAME_EXISTS", "Username is already registered.");
      }

      const now = new Date().toISOString();
      const user = {
        id: crypto.randomUUID(),
        username: input.username,
        usernameNormalized,
        displayName: input.displayName || input.username,
        studentId: input.studentId || "",
        passwordHash: input.passwordHash,
        createdAt: now,
        updatedAt: now,
      };
      data.users.push(user);
      return user;
    });
  },
};

module.exports = { userRepository };
