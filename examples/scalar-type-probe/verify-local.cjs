#!/usr/bin/env node
'use strict';
const fs = require('node:fs'), path = require('node:path'), os = require('node:os'), cp = require('node:child_process'), crypto = require('node:crypto'), assert = require('node:assert/strict');
const {createOwnedRunner, ownedBytes} = require('../fix-e-alignment/owned-command.cjs');
const repo = path.resolve(__dirname, '../..'), sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
assert(process.argv.length === 2 || process.argv.length === 3 && ['--restoration-views','--outer-replay','--finite','--jdbc-exact','--sql-millis'].includes(process.argv[2]));
const restorationViews = process.argv[2] === '--restoration-views';
const outerReplay = process.argv[2] === '--outer-replay';
const finite = process.argv[2] === '--finite';
const jdbcExact = process.argv[2] === '--jdbc-exact';
const sqlMillis = process.argv[2] === '--sql-millis';
const requested = process.env.TEAQL_JAVA_SCALAR_EVIDENCE_DIR;
const output = requested ? path.resolve(requested) : fs.mkdtempSync(path.join(os.tmpdir(), 'teaql-java-name-evidence-'));
if (requested) { assert(!fs.existsSync(output), 'never overwrite evidence'); fs.mkdirSync(output, {recursive: true}); }
function sourceFiles() { return [...new Set(cp.execFileSync('git', ['-C', repo, 'ls-files', '-z', '--cached', '--others', '--exclude-standard']).toString().split('\0').filter(Boolean))].sort(); }
function sourceHash() {
  const hash = crypto.createHash('sha256');
  for (const file of sourceFiles()) { const source = path.join(repo, file); hash.update(file + '\0');
    if (!fs.existsSync(source)) { hash.update('deleted\0'); continue; }
    assert(fs.lstatSync(source).isFile()); hash.update(fs.readFileSync(source)); hash.update('\0'); }
  return hash.digest('hex');
}
function fixture() {
  const bytes = fs.readFileSync(path.join(__dirname, 'fixture-manifest.json')), manifest = JSON.parse(bytes);
  assert.equal(manifest.language, 'java'); assert.equal(manifest.evaluationErrors, 0);
  assert.equal(manifest.modelSha256, '8d3017392ffd00abbc06be2555ced27ac6360e2d05fabc4299e27a5ef8f1c762');
  assert.equal(sha(fs.readFileSync(path.join(__dirname, 'model.xml'))), manifest.modelSha256);
  const actual = [];
  function walk(directory, prefix = '') {
    for (const item of fs.readdirSync(directory, {withFileTypes: true})) {
      assert(!item.isSymbolicLink()); const name = path.join(prefix, item.name), file = path.join(directory, item.name);
      if (item.isDirectory()) walk(file, name); else { assert(item.isFile()); actual.push(name); }
    }
  }
  walk(path.join(__dirname, 'generated'));
  assert.deepEqual(actual.sort(), Object.keys(manifest.files).sort(), 'unexpected generated source');
  for (const [file, expected] of Object.entries(manifest.files)) {
    assert(file.startsWith('lib/') && !file.split('/').includes('..'));
    assert.equal(sha(fs.readFileSync(path.join(__dirname, 'generated', file))), expected, file);
  }
  return {manifest, manifestSha256: sha(bytes)};
}
const scratch = fs.mkdtempSync(path.join(os.tmpdir(), 'teaql-java-name-build-')), runner = createOwnedRunner();
const report = {schema: sqlMillis ? 'teaql.java-sql-millisecond-example/v1' : finite ? 'teaql.java-finite-scalar-example/v1' : jdbcExact ? 'teaql.java-jdbc-exact-read-example/v1' : outerReplay ? 'teaql.java-outer-replay-example/v1' : restorationViews ? 'teaql.java-restoration-views-example/v1' : 'teaql.java-canonical-name-example/v1', passed: false, commands: [], rounds: [], sameDatabase: true, publicPublication: false, internalArtifactAccepted: false};
async function main() {
  try {
    const disk = fs.statfsSync(scratch); assert(disk.bavail * disk.bsize > 8 * 1024 ** 3);
    const before = sourceHash(), input = fixture(); report.sourceSha256 = before;
    report.sourceCommit = cp.execFileSync('git', ['-C', repo, 'rev-parse', 'HEAD']).toString().trim();
    report.fixture = input;
    const source = path.join(scratch, 'source'), generated = path.join(scratch, 'generated'), consumer = path.join(scratch, 'consumer');
    for (const file of sourceFiles()) {
      const from = path.join(repo, file); if (!fs.existsSync(from)) continue;
      const to = path.join(source, file); fs.mkdirSync(path.dirname(to), {recursive: true}); fs.copyFileSync(from, to);
    }
    fs.cpSync(path.join(__dirname, 'generated/lib'), generated, {recursive: true});
    const tests = path.join(consumer, 'src/test/java/com/example/scalartypeconformanceservice'); fs.mkdirSync(tests, {recursive: true});
    fs.copyFileSync(path.join(__dirname, 'GeneratedCanonicalNameTest.java'), path.join(tests, 'GeneratedCanonicalNameTest.java'));
    report.applicationSha256 = sha(fs.readFileSync(path.join(__dirname, 'GeneratedCanonicalNameTest.java')));
    if (sqlMillis) {
      const name = 'GeneratedSqlMillisecondTest.java', body = fs.readFileSync(path.join(__dirname, name));
      fs.writeFileSync(path.join(tests, name), body); report.sqlMillisApplicationSha256 = sha(body);
    }
    if (finite) for (const name of ['JavaFiniteScalarProbe.java','GeneratedFiniteScalarTest.java']) {
      const body = fs.readFileSync(path.join(__dirname,name)); fs.writeFileSync(path.join(tests,name),body);
      report.finiteApplicationInputs ||= {}; report.finiteApplicationInputs[name] = sha(body);
    }
    if (jdbcExact) {
      fs.copyFileSync(path.join(__dirname, 'GeneratedJdbcExactReadTest.java'), path.join(tests, 'GeneratedJdbcExactReadTest.java'));
      report.jdbcExactApplicationSha256 = sha(fs.readFileSync(path.join(__dirname, 'GeneratedJdbcExactReadTest.java')));
    }
    if (restorationViews) {
      fs.copyFileSync(path.join(__dirname, 'GeneratedRestorationViewTest.java'), path.join(tests, 'GeneratedRestorationViewTest.java'));
      report.restorationViewApplicationSha256 = sha(fs.readFileSync(path.join(__dirname, 'GeneratedRestorationViewTest.java')));
    }
    if (outerReplay) {
      fs.copyFileSync(path.join(__dirname, 'GeneratedOuterReplayTest.java'), path.join(tests, 'GeneratedOuterReplayTest.java'));
      report.outerReplayApplicationSha256 = sha(fs.readFileSync(path.join(__dirname, 'GeneratedOuterReplayTest.java')));
    }
    const version = fs.readFileSync(path.join(repo, 'pom.xml'), 'utf8').match(/<version>([^<]+)<\/version>/)[1];
    const dependencies = ['teaql-runtime', 'teaql-sqlite', 'teaql-jackson', ...(restorationViews || outerReplay ? ['teaql-dynamic-fields-jdbc'] : [])].map(name =>
      `<dependency><groupId>io.teaql</groupId><artifactId>${name}</artifactId><version>${version}</version></dependency>`).join('')
      + '<dependency><groupId>com.fasterxml.jackson.datatype</groupId><artifactId>jackson-datatype-jsr310</artifactId></dependency>';
    const pom = `<project><modelVersion>4.0.0</modelVersion><parent><groupId>io.teaql</groupId><artifactId>teaql-java-parent</artifactId><version>${version}</version><relativePath>../source/pom.xml</relativePath></parent><groupId>io.teaql.examples</groupId><artifactId>teaql-canonical-name-example</artifactId><version>${version}</version><dependencies><dependency><groupId>com.example</groupId><artifactId>scalar-type-conformance-service-core</artifactId><version>0.0.1-SNAPSHOT</version></dependency>${dependencies}<dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4.13.2</version><scope>test</scope></dependency></dependencies><build><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><configuration><useModulePath>false</useModulePath></configuration></plugin></plugins></build></project>`;
    fs.writeFileSync(path.join(consumer, 'pom.xml'), pom); fs.writeFileSync(path.join(output, 'consumer-pom.xml'), pom);
    const reactor = path.join(scratch, 'pom.xml'); fs.writeFileSync(reactor, '<project><modelVersion>4.0.0</modelVersion><groupId>io.teaql.test</groupId><artifactId>canonical-name-reactor</artifactId><version>0.0.0</version><packaging>pom</packaging><modules><module>source</module><module>generated</module><module>consumer</module></modules></project>');
    const expectedCases = ['canonical-create', 'q-e-load-state', 'canonical-update', 'malformed-zero-provider', 'required-zero-provider', 'typed-json-default-profile', 'real-second-update-rollback', 'same-graph-retry'];
    for (const round of [1, 2]) {
      const command = ['mvn', '-o', '-B', '-ntp', '-f', reactor, '-pl', ':teaql-canonical-name-example', '-am', 'test', sqlMillis ? '-Dtest=GeneratedSqlMillisecondTest' : finite ? '-Dtest=GeneratedFiniteScalarTest' : jdbcExact ? '-Dtest=GeneratedJdbcExactReadTest' : outerReplay ? '-Dtest=GeneratedOuterReplayTest' : restorationViews ? '-Dtest=GeneratedRestorationViewTest' : '-Dtest=GeneratedCanonicalNameTest', '-Dsurefire.failIfNoSpecifiedTests=false', '-Dteaql.version=' + version, '-Dteaql.expected.runtime.root=' + source];
      const log = path.join(output, 'round-' + round + '.log'), fd = fs.openSync(log, 'wx'); let result;
      try { result = await runner.run(command, {cwd: scratch, timeout: 300000, env: {...process.env,
        TEAQL_NAME_DB: path.join(output, 'fixture.sqlite'), TEAQL_NAME_ROUND: String(round)}, stdio: ['ignore', fd, fd]}); }
      finally { fs.closeSync(fd); }
      const bytes = fs.readFileSync(log), text = bytes.toString();
      report.commands.push({command, exitCode: result.status, signal: result.signal, logSha256: sha(bytes)});
      if (sqlMillis) {
        const junit = path.join(consumer, 'target/surefire-reports/TEST-com.example.scalartypeconformanceservice.GeneratedSqlMillisecondTest.xml');
        if (fs.existsSync(junit)) fs.copyFileSync(junit, path.join(output, 'round-'+round+'-junit.xml'));
      }
      assert.equal(result.status, 0, text.slice(-12000)); assert.equal(result.signal, null);
      if (sqlMillis) {
        const values = [...text.matchAll(/^JAVA_SQL_MILLISECOND=(.+)$/gm)].map(match => JSON.parse(match[1]));
        assert.equal(values.length, 4); assert(values.every(v => v.passed && v.round === round && !v.legacySqlPrecisionAccepted && !v.businessClockChanged));
        assert.deepEqual(values.map(v => [v.case,v.cases]).sort(), [['matrix-save-query-e',18],['minimal-projection',1],['native-json',18],['update-equality',3]]);
        report.rounds.push({round,cases:values}); assert.deepEqual(fixture(),input); assert.equal(sourceHash(),before); continue;
      }
      if (finite) {
        const frames = marker => text.split(/\r?\n/).filter(v => v.startsWith(marker+'=')).map(v => JSON.parse(v.slice(marker.length+1)));
        const rows = frames('JAVA_FINITE_SCALAR_CASE'); assert.equal(rows.length,25); assert(rows.every(v => v.contractPass && v.round===round));
        const summary = frames('JAVA_FINITE_SCALAR_RESULT'); assert.equal(summary.length,1); assert.equal(summary[0].contractFailures,0);
        const fix = frames('JAVA_FINITE_CUSTOM_FIX'); assert.deepEqual(fix,[{round,rejected:24,repaired:4,oneFixPerSave:true,passed:true}]);
        const legacy = frames('JAVA_FINITE_LEGACY'); assert.deepEqual(legacy,[{round,cases:8,unchangedVersions:true,passed:true}]);
        const graph = frames('JAVA_FINITE_GRAPH_RETRY'); assert.deepEqual(graph,[{round,children:2,version:2,sameGraph:true,passed:true}]);
        const origins = frames('JAVA_FINITE_OFFICIAL_ORIGINS'); assert.equal(origins.length,1);
        assert.equal(Object.keys(origins[0]).length,4); assert(Object.values(origins[0]).every(v => v.startsWith(source+path.sep)));
        report.rounds.push({round,cases:rows,summary:summary[0],fix:fix[0],legacy:legacy[0],graph:graph[0],origins:origins[0]});
        assert.deepEqual(fixture(),input); assert.equal(sourceHash(),before); continue;
      }
      if (jdbcExact) {
        const values = [...text.matchAll(/^JAVA_JDBC_EXACT_READ=(.+)$/gm)].map(match => JSON.parse(match[1]));
        assert.equal(values.length, 1); const value = values[0]; assert(value.passed && value.round === round);
        const names = ['integer-fraction','integer-upper-overflow','integer-lower-overflow','long-fraction','long-rounded-overflow','float-overflow','double-infinity','decimal-private-text'];
        assert.deepEqual(value.cases, ['one','stream'].flatMap(mode => names.map(name => mode + '/' + name)));
        for (const key of ['generatedQ','loadedE','unselectedNotLoaded','physicalCorruptionConfirmed','queryProducesNoMutation','privateValuesExcluded','sameAdapterRecovers','exactNativeBounds']) assert.equal(value[key], true);
        assert.equal(value.boundaryVersion, 2); report.rounds.push(value); assert.deepEqual(fixture(), input); assert.equal(sourceHash(), before); continue;
      }
      if (outerReplay) {
        const values = [...text.matchAll(/^JAVA_OUTER_REPLAY=(.+)$/gm)].map(match => JSON.parse(match[1]));
        assert.equal(values.length, 7); assert(values.every(value => value.passed && value.round === round));
        assert.deepEqual(values.map(value => value.case).sort(), ['different-native-fields','dynamic-saved-unsaved','dynamic-then-native','new-then-update','raw-admission','same-native-field','unsaved-native-field']);
        report.rounds.push({round, cases: values}); assert.deepEqual(fixture(), input); assert.equal(sourceHash(), before);
        continue;
      }
      if (restorationViews) {
        const values = [...text.matchAll(/^JAVA_RESTORATION_VIEW=(.+)$/gm)].map(match => JSON.parse(match[1]));
        assert.equal(values.length, 3); assert(values.every(value => value.passed && value.round === round));
        assert.deepEqual(values.map(value => value.case).sort(), ['caller-transaction-rollback-retry-commit', 'persistent-dynamic-noop-and-real-edit', 'readonly-sparse-noop']);
        report.rounds.push({round, cases: values}); assert.deepEqual(fixture(), input); assert.equal(sourceHash(), before);
        continue;
      }
      const rows = text.split(/\r?\n/).filter(line => line.startsWith('JAVA_CANONICAL_NAME=')); assert.equal(rows.length, 1);
      const value = JSON.parse(rows[0].slice('JAVA_CANONICAL_NAME='.length));
      assert.equal(value.passed, true); assert.equal(value.round, round); assert.deepEqual(value.cases, expectedCases);
      assert.equal(value.malformedRejections, 12); assert.equal(value.requiredRejections, 2);
      assert.equal(value.rolledBackAfterWrites, 1); assert.equal(value.retryAuditEntities, 2);
      report.rounds.push(value); assert.deepEqual(fixture(), input); assert.equal(sourceHash(), before);
    }
    if (sqlMillis) report.databases = Object.fromEntries(['matrix','update','json','projection'].map(name => {
      const file = 'fixture.sqlite.sql-millis-'+name; return [file,sha(fs.readFileSync(path.join(output,file)))];
    }));
    else if (finite) report.databases = Object.fromEntries(['','.finite-fix','.finite-legacy','.finite-retry'].map(suffix => {
      const file = 'fixture.sqlite'+suffix; return [file,sha(fs.readFileSync(path.join(output,file)))];
    }));
    else if (outerReplay) report.databases = Object.fromEntries(['different-native-fields','dynamic-saved-unsaved','dynamic-then-native','new-then-update','raw-admission','same-native-field','unsaved-native-field'].map(name => {
      const file = 'fixture.sqlite.outer-replay-' + name + '.sqlite'; return [file, sha(fs.readFileSync(path.join(output, file)))];
    }));
    else if (restorationViews) report.databases = Object.fromEntries(['readonly', 'transaction', 'dynamic'].map(name => {
      const file = 'fixture.sqlite.views-' + name + '.sqlite'; return [file, sha(fs.readFileSync(path.join(output, file)))];
    }));
    else report.databaseSha256 = sha(fs.readFileSync(path.join(output, 'fixture.sqlite')));
    report.passed = true;
    console.log(sqlMillis ? 'PASS generated Java SQL milliseconds save, typed Q/E, native JSON and projections twice'
      : finite ? 'PASS generated Java finite scalar, customer Fix, legacy reads and graph retry twice'
      : jdbcExact ? 'PASS generated Java exact JDBC Q/E corruption boundaries twice'
      : outerReplay ? 'PASS generated Java seven outer transaction retry scenarios twice'
      : restorationViews ? 'PASS generated Java unchanged view geometry and caller transaction rollback/retry/commit twice'
      : 'PASS generated Java canonical naming, Q/E, Checker, default JSON and real graph rollback/retry twice');
  } catch (error) { report.error = error.stack; console.error(error.stack); process.exitCode ||= 1; }
  finally {
    report.disposedOwnedBytes = runner.safeToClean ? ownedBytes(scratch) : 0;
    if (runner.safeToClean) fs.rmSync(scratch, {recursive: true}); else report.unsafeBuildRetention = scratch;
    report.scratchRemoved = !fs.existsSync(scratch); runner.dispose();
    fs.writeFileSync(path.join(output, 'receipt.json'), JSON.stringify(report, null, 2) + '\n');
    console.log('Retained compact name-probe evidence: ' + output);
  }
}
main().catch(error => { console.error(error.stack); process.exitCode ||= 1; });
