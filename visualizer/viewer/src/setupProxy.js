const path = require('path');
const fs = require('fs');

const CONTAINERS_FILE = path.resolve(__dirname, '..', 'public', 'assets', 'containers.json');
const EMPTY_RESPONSE = '{"containers":[]}';

console.log('[viewer] setupProxy: watching containers.json at', CONTAINERS_FILE);

module.exports = function (app) {
  app.get('/assets/containers.json', function (req, res) {
    res.set('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
    res.set('Pragma', 'no-cache');
    res.set('Expires', '0');
    res.set('Content-Type', 'application/json; charset=utf-8');

    let data;
    try {
      data = fs.readFileSync(CONTAINERS_FILE, 'utf8');
      const parsed = JSON.parse(data);
      const count = parsed.containers ? parsed.containers.length : 0;
      console.log(`[viewer] Serving containers.json: ${count} container(s) from ${CONTAINERS_FILE}`);
    } catch (e) {
      data = EMPTY_RESPONSE;
      console.log('[viewer] containers.json not found or invalid — serving empty response');
    }
    res.end(data);
  });
};
