'use strict';
// Rebind only build metadata in a disposable consumer, after fixture validation.
// The evaluated model and frozen generated implementation are never edited.
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const crypto = require('node:crypto'), sha = value => crypto.createHash('sha256').update(value).digest('hex');
function pin(file, runtime, ownedRoot) {
  for (const target of [file, runtime, ownedRoot]) assert.equal(fs.realpathSync(target), target);
  assert(file.startsWith(ownedRoot + path.sep) && !file.startsWith(runtime + path.sep), 'Only an owned disposable POM may be adapted');
  assert.equal(path.basename(file), 'pom.xml');
  const root = fs.readFileSync(path.join(runtime, 'pom.xml'), 'utf8');
  const version = root.match(/<version>([^<]+)<\/version>/)[1];
  const modules = new Set([...root.matchAll(/<module>(teaql-[^<]+)<\/module>/g)].map(match => match[1]));
  assert(modules.size > 0);
  const before = fs.readFileSync(file, 'utf8'), changes = [];
  let after = before.replace(/<teaql\.version>([^<]+)<\/teaql\.version>/g, (_, from) => {
    changes.push({property: 'teaql.version', from, to: version}); return '<teaql.version>' + version + '</teaql.version>';
  });
  after = after.replace(/<dependency>[\s\S]*?<\/dependency>/g, block => {
    if (!/<groupId>io\.teaql<\/groupId>/.test(block)) return block;
    const name = block.match(/<artifactId>([^<]+)<\/artifactId>/)?.[1];
    if (!modules.has(name)) return block;
    return block.replace(/<version>(\d[^<]*)<\/version>/, (_, from) => {
      changes.push({artifact: name, from, to: version}); return '<version>' + version + '</version>';
    });
  });
  assert(changes.length > 0, 'No declared runtime coordinates to bind');
  fs.writeFileSync(file, after);
  return {version, changes, originalPomSha256: sha(before), consumerPomSha256: sha(after), sourceFixtureEdited: false, generatedImplementationEdited: false};
}
module.exports = {pin};
