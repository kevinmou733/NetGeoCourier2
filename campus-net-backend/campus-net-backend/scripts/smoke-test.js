const assert = require("node:assert/strict");

const baseUrl = process.env.API_BASE_URL || "http://127.0.0.1:3000";
const unique = Date.now().toString(36);

async function request(path, options = {}) {
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers: {
      "content-type": "application/json",
      ...(options.headers || {}),
    },
  });
  const body = await response.json();
  return { response, body };
}

async function main() {
  const username = `smoke_${unique}`;
  const password = "123456";

  const register = await request("/api/v1/auth/register", {
    method: "POST",
    body: JSON.stringify({ username, password, displayName: "Smoke Test" }),
  });
  assert.equal(register.response.status, 201);
  assert.equal(register.body.success, true);
  assert.ok(register.body.data.accessToken);

  const login = await request("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
  assert.equal(login.response.status, 200);
  assert.equal(login.body.success, true);
  assert.ok(login.body.data.accessToken);

  const me = await request("/api/v1/auth/me", {
    headers: {
      authorization: `Bearer ${login.body.data.accessToken}`,
    },
  });
  assert.equal(me.response.status, 200);
  assert.equal(me.body.data.user.username, username);

  console.log("Smoke test passed.");
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
