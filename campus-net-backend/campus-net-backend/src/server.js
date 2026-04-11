const { createApp } = require("./app");
const { config, warnIfUsingDefaultSecret } = require("./config");

warnIfUsingDefaultSecret();

const app = createApp();

app.listen(config.port, config.host, () => {
  console.log(`Campus network backend listening on http://${config.host}:${config.port}`);
  console.log(`Data file: ${config.dataFile}`);
});
