const fs = require("node:fs/promises");
const path = require("node:path");

const { config } = require("../config");

const EMPTY_DATABASE = {
  users: [],
  records: [],
};

class JsonDatabase {
  constructor(filePath) {
    this.filePath = filePath;
    this.queue = Promise.resolve();
  }

  async ensureFile() {
    await fs.mkdir(path.dirname(this.filePath), { recursive: true });
    try {
      await fs.access(this.filePath);
    } catch {
      await this.write({ ...EMPTY_DATABASE });
    }
  }

  async read() {
    await this.ensureFile();
    const raw = await fs.readFile(this.filePath, "utf8");
    const parsed = raw.trim() ? JSON.parse(raw) : {};
    return {
      users: Array.isArray(parsed.users) ? parsed.users : [],
      records: Array.isArray(parsed.records) ? parsed.records : [],
    };
  }

  async write(data) {
    await fs.mkdir(path.dirname(this.filePath), { recursive: true });
    const tmpFile = `${this.filePath}.tmp`;
    await fs.writeFile(tmpFile, `${JSON.stringify(data, null, 2)}\n`, "utf8");
    await fs.rename(tmpFile, this.filePath);
  }

  async transaction(mutator) {
    const run = this.queue.then(async () => {
      const data = await this.read();
      const result = await mutator(data);
      await this.write(data);
      return result;
    });

    this.queue = run.catch(() => undefined);
    return run;
  }
}

const database = new JsonDatabase(config.dataFile);

module.exports = { database, JsonDatabase };
