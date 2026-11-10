'use strict';
const {test} = require('node:test'), assert = require('node:assert/strict');
const fs = require('node:fs'), path = require('node:path'), os = require('node:os');
const {pin} = require('./pin-local-example-pom.cjs');
function fixture(run) {
  const scratch = fs.mkdtempSync(path.join(os.tmpdir(), 'teaql-local-pom-test-'));
  try {
    const runtime = path.join(scratch, 'runtime'), owned = path.join(scratch, 'consumer');
    fs.mkdirSync(runtime); fs.mkdirSync(owned);
    fs.writeFileSync(path.join(runtime, 'pom.xml'), '<project><version>1.556-RELEASE</version><modules><module>teaql-core</module><module>teaql-sqlite</module></modules></project>');
    const file = path.join(owned, 'pom.xml');
    const original = '<project><properties><teaql.version>1.555-RELEASE</teaql.version></properties><dependencies>' +
      '<dependency><groupId>io.teaql</groupId><artifactId>teaql-core</artifactId><version>${teaql.version}</version></dependency>' +
      '<dependency><groupId>io.teaql</groupId><artifactId>teaql-sqlite</artifactId><version>1.555-RELEASE</version></dependency>' +
      '<dependency><groupId>other</groupId><artifactId>teaql-sqlite</artifactId><version>1.555-RELEASE</version></dependency></dependencies></project>';
    fs.writeFileSync(file, original); run({runtime, owned, file, original});
  } finally {fs.rmSync(scratch, {recursive: true});}
}
test('binds explicit provider versions and property without changing other dependencies', () => fixture(({runtime, owned, file}) => {
  const r = pin(file, runtime, owned), text = fs.readFileSync(file, 'utf8');
  assert.equal(r.changes.length, 2); assert.equal(r.version, '1.556-RELEASE');
  assert.equal(r.generatedImplementationEdited, false); assert.equal(r.sourceFixtureEdited, false);
  assert(text.includes('<teaql.version>1.556-RELEASE</teaql.version>'));
  assert(text.includes('<artifactId>teaql-core</artifactId><version>${teaql.version}</version>'));
  assert(text.includes('<groupId>other</groupId><artifactId>teaql-sqlite</artifactId><version>1.555-RELEASE</version>'));
}));
test('rejects a source POM even if a caller labels its parent as owned', () => fixture(({runtime, file, original}) => {
  const source = path.join(runtime, 'pom-under-test'); fs.mkdirSync(source);
  const target = path.join(source, 'pom.xml'); fs.copyFileSync(file, target);
  assert.throws(() => pin(target, runtime, source), /disposable/);
  assert.equal(fs.readFileSync(target, 'utf8'), original);
}));
test('rejects a symlink and undeclared ownership before writing', () => fixture(({runtime, owned, file, original}) => {
  assert.throws(() => pin(file, runtime, runtime), /disposable/);
  const link = path.join(owned, 'linked.xml'); fs.symlinkSync(file, link);
  assert.throws(() => pin(link, runtime, owned));
  assert.equal(fs.readFileSync(file, 'utf8'), original);
}));
