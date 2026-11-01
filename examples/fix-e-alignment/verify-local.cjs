#!/usr/bin/env node
"use strict";
// Source-paired reactor: no shared Maven install/deploy and no generator needed.
const fs = require("node:fs"), path = require("node:path"), os = require("node:os"), cp = require("node:child_process"), crypto = require("node:crypto"), assert = require("node:assert/strict");
const example = __dirname, repo = path.resolve(example, "../.."), sha = bytes => crypto.createHash("sha256").update(bytes).digest("hex");
function fingerprint() {
  const m = JSON.parse(fs.readFileSync(path.join(example, "fixture-manifest.json")));
  assert.equal(m.language, "java"); assert.equal(m.evaluationErrors, 0);
  assert.equal(sha(fs.readFileSync(path.join(example, "model.xml"))), m.modelSha256);
  const actual = [];
  function walk(dir, prefix = "") { for (const e of fs.readdirSync(dir, {withFileTypes: true})) {
    const name = path.join(prefix, e.name), file = path.join(dir, e.name); assert(!e.isSymbolicLink());
    if (e.isDirectory()) walk(file, name); else { assert(e.isFile()); actual.push(name); }
  } }
  walk(path.join(example, "generated")); assert.deepEqual(actual.sort(), Object.keys(m.files).sort());
  for (const [name, expected] of Object.entries(m.files)) assert.equal(sha(fs.readFileSync(path.join(example, "generated", name))), expected, name);
  return m;
}
const requested = process.env.TEAQL_FIX_E_EVIDENCE_DIR;
const evidence = requested ? path.resolve(requested) : fs.mkdtempSync(path.join(os.tmpdir(), "teaql-java-fix-e-evidence-"));
if (requested) { assert(path.isAbsolute(requested) && !fs.existsSync(evidence)); fs.mkdirSync(evidence, {recursive: true}); }
const scratch = fs.mkdtempSync(path.join(os.tmpdir(), "teaql-java-fix-e-build-"));
const report = {schema: "teaql.java-fix-e-example/v1", passed: false, rounds: [], sameDatabase: true,
  localRuntime: repo, databaseRetained: true, sharedMavenInstall: false};
report.contextRootApplicationSha256 = sha(fs.readFileSync(path.join(example, 'ContextRootProbe.java')));
report.contextRootSubtypeApplicationSha256 = sha(fs.readFileSync(path.join(example, 'ContextRootSubtypeProbe.java')));
report.partialAncestorApplicationSha256 = sha(fs.readFileSync(path.join(example, 'partial-ancestors/PartialAncestorExampleTest.java')));
report.postFixApplicationSha256 = sha(fs.readFileSync(path.join(example, 'PostFixExampleTest.java')));
const {createOwnedRunner, ownedBytes} = require('./owned-command.cjs'), runner = createOwnedRunner();
async function main() {
try {
  const disk = fs.statfsSync(scratch); assert(disk.bavail * disk.bsize > 8 * 1024 ** 3);
  const m = fingerprint(); report.modelSha256 = m.modelSha256;
  const runtime = path.join(scratch, "source"), generated = path.join(scratch, "generated"), consumer = path.join(scratch, "consumer"), reactor = path.join(scratch, "reactor");
  const sourceFiles = cp.execFileSync("git", ["-C", repo, "ls-files", "-z", "--cached", "--others", "--exclude-standard"]).toString().split("\0").filter(Boolean);
  for (const file of new Set(sourceFiles)) {
    const from = path.join(repo, file); if (!fs.existsSync(from)) continue;
    assert(fs.lstatSync(from).isFile(), "source copy requires regular files");
    fs.mkdirSync(path.dirname(path.join(runtime, file)), {recursive: true}); fs.copyFileSync(from, path.join(runtime, file));
  }
  fs.cpSync(path.join(example, "generated/lib"), generated, {recursive: true});
  fs.mkdirSync(path.join(consumer, "src/test/java/io/teaql/examples/fixe"), {recursive: true});
  for (const file of fs.readdirSync(example).filter(file => file.endsWith(".java")))
    fs.copyFileSync(path.join(example, file), path.join(consumer, "src/test/java/io/teaql/examples/fixe", file));
  fs.copyFileSync(path.join(example, 'partial-ancestors/PartialAncestorExampleTest.java'),
    path.join(consumer, 'src/test/java/io/teaql/examples/fixe/PartialAncestorExampleTest.java'));
  const version = fs.readFileSync(path.join(repo, "pom.xml"), "utf8").match(/<version>([^<]+)<\/version>/)[1];
  const pom = `<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
    <parent><groupId>io.teaql</groupId><artifactId>teaql-java-parent</artifactId><version>${version}</version><relativePath>../source/pom.xml</relativePath></parent>
    <groupId>io.teaql.examples</groupId><artifactId>teaql-fix-e-example</artifactId><version>${version}</version>
    <dependencies>
      <dependency><groupId>com.teaql</groupId><artifactId>checker-fix-advanced-core</artifactId><version>0.0.1-SNAPSHOT</version></dependency>
      <dependency><groupId>io.teaql</groupId><artifactId>teaql-runtime</artifactId><version>${version}</version></dependency>
      <dependency><groupId>io.teaql</groupId><artifactId>teaql-sqlite</artifactId><version>${version}</version></dependency>
      <dependency><groupId>io.teaql</groupId><artifactId>teaql-jackson</artifactId><version>${version}</version></dependency>
      <dependency><groupId>com.fasterxml.jackson.datatype</groupId><artifactId>jackson-datatype-jsr310</artifactId><scope>test</scope></dependency>
      <dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4.13.2</version><scope>test</scope></dependency>
    </dependencies><build><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><configuration><useModulePath>false</useModulePath></configuration></plugin></plugins></build></project>`;
  fs.writeFileSync(path.join(consumer, "pom.xml"), pom); fs.writeFileSync(path.join(evidence, "consumer-pom.xml"), pom);
  fs.mkdirSync(reactor);
  fs.writeFileSync(path.join(reactor, "pom.xml"), '<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>io.teaql.test</groupId><artifactId>fix-e-reactor</artifactId><version>0.0.0</version><packaging>pom</packaging><modules><module>../source</module><module>../generated</module><module>../consumer</module></modules></project>');
  for (const round of [1, 2]) {
    const command = ['mvn', '-o', '-B', '-ntp', '-f', path.join(reactor, 'pom.xml'), '-pl', ':teaql-fix-e-example', '-am', 'test',
      '-Dtest=GeneratedFixEExampleTest,PartialAncestorExampleTest,PostFixExampleTest', '-Dsurefire.failIfNoSpecifiedTests=false', '-Dteaql.version=' + version,
      '-Dteaql.expected.runtime.root=' + runtime];
    const log = path.join(evidence, 'round-' + round + '.log'), fd = fs.openSync(log, 'w'); let result;
    try { result = await runner.run(command, {cwd: scratch, timeout: 300000,
      env: {...process.env, TEAQL_FIX_E_DB: path.join(evidence, 'fixture.sqlite')}, stdio: ['ignore', fd, fd]}); }
    finally { fs.closeSync(fd); }
    const bytes = fs.readFileSync(log);
    report.rounds.push({command, exitCode: result.status, signal: result.signal, logSha256: sha(bytes)});
    if (result.status !== 0) process.exitCode = result.status || 1;
    assert.equal(result.status, 0, bytes.toString().slice(-12000));
    assert(bytes.toString().includes('Tests run: 5, Failures: 0, Errors: 0, Skipped: 0'));
    const postFix = bytes.toString().split(/\r?\n/).filter(line => line.startsWith('JAVA_POST_FIX_CASE='))
      .map(line => JSON.parse(line.slice('JAVA_POST_FIX_CASE='.length)));
    assert.equal(postFix.length, 3);
    assert.deepEqual(postFix.map(row => row.case).sort(), ['typed-defaults-preserve-business-input',
      'value-free-provenance-and-graph-once', 'post-fix-reject-zero-provider-and-retry'].sort());
    for (const row of postFix) assert.equal(row.passed, true);
    for (const marker of ['FIX_E_EXAMPLE=', 'CORE_LOAD_EXAMPLE=', 'CORE_MUTATION_OWNERSHIP=', 'CORE_MUTATION_PAGING=', 'JAVA_E_BOUNDARY_RESULT=', 'JAVA_CONTEXT_ROOT_RESULT=', 'JAVA_CONTEXT_ROOT_SUBTYPE_RESULT=', 'JAVA_PARTIAL_ANCESTORS_RESULT=']) {
      const values = bytes.toString().split(/\r?\n/).filter(line => line.startsWith(marker)).map(line => JSON.parse(line.slice(marker.length)));
      assert.equal(values.length, 1, marker); assert.equal(values[0].passed, true, marker);
    }
    assert.deepEqual(fingerprint(), m);
  }
  report.databaseSha256 = sha(fs.readFileSync(path.join(evidence, 'fixture.sqlite')));
  report.expressionBoundaryApplicationSha256 = sha(fs.readFileSync(path.join(example, 'ExpressionBoundaryProbe.java')));
  report.expressionBoundaryDatabase = {file: 'fixture.sqlite.expression-boundaries.sqlite',
    sha256: sha(fs.readFileSync(path.join(evidence, 'fixture.sqlite.expression-boundaries.sqlite')))};
  report.contextRootApplicationSha256 = sha(fs.readFileSync(path.join(example, 'ContextRootProbe.java')));
  report.contextRootDatabase = {file: 'fixture.sqlite.context-root.sqlite',
    sha256: sha(fs.readFileSync(path.join(evidence, 'fixture.sqlite.context-root.sqlite')))};
  report.contextRootSubtypeDatabase = {file: 'fixture.sqlite.root-subtype.sqlite',
    sha256: sha(fs.readFileSync(path.join(evidence, 'fixture.sqlite.root-subtype.sqlite')))};
  report.partialAncestorDatabase = {file: 'fixture.sqlite.partial-ancestors.sqlite',
    sha256: sha(fs.readFileSync(path.join(evidence, 'fixture.sqlite.partial-ancestors.sqlite')))};
  report.postFixDatabases = ['typed', 'evidence', 'rejection'].map(name => {
    const file = 'fixture.sqlite.post-fix-' + name + '.sqlite';
    return {file, sha256: sha(fs.readFileSync(path.join(evidence, file)))};
  });
  report.passed = true;
  console.log('PASS Java retained Fix/E/loading/ownership example twice on one database');
} catch (error) { console.error(error.stack); process.exitCode ||= 1; }
finally {
  report.disposedOwnedBytes = runner.safeToClean ? ownedBytes(scratch) : 0;
  if (runner.safeToClean) fs.rmSync(scratch, {recursive: true});
  else report.unsafeBuildRetention = {path: scratch, processGroup: runner.unsafeGroup};
  report.scratchRemoved = !fs.existsSync(scratch); runner.dispose();
  fs.writeFileSync(path.join(evidence, 'receipt.json'), JSON.stringify(report, null, 2) + '\n');
  console.log('Retained compact example evidence: ' + evidence);
}
}
main().catch(error => { console.error(error.stack); process.exitCode ||= 1; });
