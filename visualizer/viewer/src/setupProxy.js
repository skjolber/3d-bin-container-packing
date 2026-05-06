const path = require('path');
const fs = require('fs');

const CONTAINERS_FILE = path.join(__dirname, '..', 'public', 'assets', 'containers.json');
const EMPTY_RESPONSE = JSON.stringify({ containers: [] });

module.exports = function (app) {
  app.get('/assets/containers.json', function (req, res) {
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
    res.setHeader('Pragma', 'no-cache');
    res.setHeader('Expires', '0');
    res.setHeader('Content-Type', 'application/json');

    if (fs.existsSync(CONTAINERS_FILE)) {
      res.sendFile(CONTAINERS_FILE);
    } else {
      res.send(EMPTY_RESPONSE);
    }
  });
};
