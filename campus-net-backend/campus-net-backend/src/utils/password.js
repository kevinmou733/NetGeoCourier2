const crypto = require("node:crypto");

const ITERATIONS = 120000;
const KEY_LENGTH = 32;
const DIGEST = "sha256";

function pbkdf2(password, salt, iterations = ITERATIONS) {
  return new Promise((resolve, reject) => {
    crypto.pbkdf2(password, salt, iterations, KEY_LENGTH, DIGEST, (error, derivedKey) => {
      if (error) {
        reject(error);
        return;
      }
      resolve(derivedKey.toString("base64url"));
    });
  });
}

async function hashPassword(password) {
  const salt = crypto.randomBytes(16).toString("base64url");
  const hash = await pbkdf2(password, salt);
  return `pbkdf2_${DIGEST}$${ITERATIONS}$${salt}$${hash}`;
}

async function verifyPassword(password, storedHash) {
  const [algorithm, iterationsValue, salt, hash] = String(storedHash || "").split("$");
  if (algorithm !== `pbkdf2_${DIGEST}` || !iterationsValue || !salt || !hash) {
    return false;
  }

  const iterations = Number.parseInt(iterationsValue, 10);
  if (!Number.isInteger(iterations) || iterations <= 0) {
    return false;
  }

  const candidateHash = await pbkdf2(password, salt, iterations);
  const candidateBuffer = Buffer.from(candidateHash);
  const storedBuffer = Buffer.from(hash);
  return candidateBuffer.length === storedBuffer.length && crypto.timingSafeEqual(candidateBuffer, storedBuffer);
}

module.exports = { hashPassword, verifyPassword };
