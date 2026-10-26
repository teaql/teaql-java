#!/usr/bin/env node
'use strict';
const fs = require('node:fs'), path = require('node:path'), os = require('node:os'), cp = require('node:child_process'), crypto = require('node:crypto'), assert = require('node:assert/strict');
const {createOwnedRunner, ownedBytes} = require('../fix-e-alignment/owned-command.cjs');
assert(process.argv.length === 2 || (process.argv.length === 3 && process.argv[2] === '--lifecycle'));
const lifecycle = process.argv[2] === '--lifecycle';
const testClass = lifecycle ? 'GeneratedJsonLifecycleTest' : 'GeneratedJsonProfileTest';
const marker = lifecycle ? 'JAVA_JSON_LIFECYCLE' : 'JAVA_JSON_MODEL_PROFILE';
const repo = path.resolve(__dirname, '../..'), inputs = path.join(__dirname, 'json-profiles');
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const requested = process.env.TEAQL_JAVA_JSON_PROFILE_EVIDENCE_DIR;
const output = requested ? path.resolve(requested) : fs.mkdtempSync(path.join(os.tmpdir(), 'teaql-java-json-evidence-'));
if (requested) { assert(!fs.existsSync(output)); fs.mkdirSync(output, {recursive: true}); }
function files() { return [...new Set(cp.execFileSync('git', ['-C', repo, 'ls-files', '-z', '--cached', '--others', '--exclude-standard']).toString().split('\0').filter(Boolean))].sort(); }
function snapshot() {
  const h = crypto.createHash('sha256');
  for (const file of files()) { h.update(file + '\0'); const source = path.join(repo, file);
    if (!fs.existsSync(source)) { h.update('deleted\0'); continue; } const stat = fs.lstatSync(source); assert(stat.isFile());
    h.update(String(stat.mode) + '\0'); h.update(fs.readFileSync(source)); h.update('\0'); }
  return h.digest('hex');
}
function fixture() {
  const bytes = fs.readFileSync(path.join(inputs, 'provenance.json')), provenance = JSON.parse(bytes);
  for (const [file, expected] of Object.entries(provenance.files)) {
    assert(!path.isAbsolute(file) && !file.split('/').includes('..')); const source = path.join(inputs, file);
    assert.equal(fs.realpathSync(source), source); assert.equal(sha(fs.readFileSync(source)), expected);
  }
  const manifest = JSON.parse(fs.readFileSync(path.join(inputs, 'manifest.json')));
  assert.equal(manifest.schema, 'teaql.java-json-profile-generated-source/v1'); assert.equal(manifest.localSourceOnly, true);
  assert.deepEqual(Object.keys(manifest.profiles).sort(), ['PascalCase', 'camelCase', 'snake_case']);
  assert.equal(manifest.baseModelSha256, '8d3017392ffd00abbc06be2555ced27ac6360e2d05fabc4299e27a5ef8f1c762');
  for (const item of Object.values(manifest.profiles)) assert.equal(item.evaluation.errors.length, 0);
  return {provenance, provenanceSha256: sha(bytes), manifest};
}
const scratch = fs.mkdtempSync(path.join(os.tmpdir(), 'teaql-java-json-build-')), runner = createOwnedRunner();
const report = {schema: lifecycle ? 'teaql.java-model-json-lifecycle-example/v1' : 'teaql.java-model-json-profiles-example/v1', passed: false, profiles: {}, commands: [], publicPublication: false, internalArtifactAccepted: false};
async function main() {
  try {
    const disk = fs.statfsSync(scratch); assert(disk.bavail * disk.bsize > 8 * 1024 ** 3);
    const before = snapshot(), input = fixture(); report.sourceSha256 = before; report.sourceHashAlgorithm = 'teaql.source-contents/v1'; report.fixture = input;
    report.sourceCommit = cp.execFileSync('git', ['-C', repo, 'rev-parse', 'HEAD']).toString().trim();
    const runtime = path.join(scratch, 'source');
    for (const file of files()) { const source = path.join(repo, file); if (!fs.existsSync(source)) continue;
      const target = path.join(runtime, file); fs.mkdirSync(path.dirname(target), {recursive: true}); fs.copyFileSync(source, target); }
    const version = fs.readFileSync(path.join(repo, 'pom.xml'), 'utf8').match(/<version>([^<]+)<\/version>/)[1];
    const dependencies = ['teaql-runtime', 'teaql-sqlite', 'teaql-jackson'].map(name => `<dependency><groupId>io.teaql</groupId><artifactId>${name}</artifactId><version>${version}</version></dependency>`).join('');
    report.applications = Object.fromEntries(['GeneratedCanonicalNameTest.java', testClass + '.java'].map(name => [name, sha(fs.readFileSync(path.join(__dirname, name)))]));
    for (const profile of ['camelCase', 'snake_case', 'PascalCase']) {
      const work = path.join(scratch, profile), consumer = path.join(work, 'consumer'), evidence = path.join(output, profile);
      fs.mkdirSync(evidence); const tests = path.join(consumer, 'src/test/java/com/example/scalartypeconformanceservice'); fs.mkdirSync(tests, {recursive: true});
      for (const name of Object.keys(report.applications)) fs.copyFileSync(path.join(__dirname, name), path.join(tests, name));
      fs.cpSync(path.join(inputs, profile, 'lib'), path.join(work, 'generated'), {recursive: true});
      const pom = `<project><modelVersion>4.0.0</modelVersion><parent><groupId>io.teaql</groupId><artifactId>teaql-java-parent</artifactId><version>${version}</version><relativePath>../../source/pom.xml</relativePath></parent><groupId>io.teaql.examples</groupId><artifactId>teaql-model-json-profile-example</artifactId><version>${version}</version><dependencies><dependency><groupId>com.example</groupId><artifactId>scalar-type-conformance-service-core</artifactId><version>0.0.1-SNAPSHOT</version></dependency>${dependencies}<dependency><groupId>com.fasterxml.jackson.datatype</groupId><artifactId>jackson-datatype-jsr310</artifactId></dependency><dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4.13.2</version><scope>test</scope></dependency></dependencies><build><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><configuration><useModulePath>false</useModulePath></configuration></plugin></plugins></build></project>`;
      fs.writeFileSync(path.join(consumer, 'pom.xml'), pom); fs.writeFileSync(path.join(evidence, 'consumer-pom.xml'), pom);
      const reactor = path.join(work, 'pom.xml'); fs.writeFileSync(reactor, '<project><modelVersion>4.0.0</modelVersion><groupId>io.teaql.test</groupId><artifactId>model-json-profile-reactor</artifactId><version>0.0.0</version><packaging>pom</packaging><modules><module>../source</module><module>generated</module><module>consumer</module></modules></project>');
      const rounds = [];
      for (const round of [1, 2]) {
        const command = ['mvn', '-o', '-B', '-ntp', '-f', reactor, '-pl', ':teaql-model-json-profile-example', '-am', 'test', '-Dtest=' + testClass, '-Dsurefire.failIfNoSpecifiedTests=false', '-Dteaql.version=' + version, '-Dteaql.expected.runtime.root=' + runtime];
        const log = path.join(evidence, 'round-' + round + '.log'), fd = fs.openSync(log, 'wx'); let result;
        try { result = await runner.run(command, {cwd: work, timeout: 300000, env: {...process.env, TEAQL_JSON_PROFILE: profile,
          TEAQL_NAME_DB: path.join(evidence, 'fixture.sqlite'), TEAQL_NAME_ROUND: String(round)}, stdio: ['ignore', fd, fd]}); }
        finally { fs.closeSync(fd); }
        const bytes = fs.readFileSync(log); report.commands.push({profile, round, command, exitCode: result.status, signal: result.signal, logSha256: sha(bytes)});
        assert.equal(result.status, 0, bytes.toString().slice(-8000)); assert.equal(result.signal, null);
        const markers = [...bytes.toString().matchAll(new RegExp('^' + marker + '=(.+)$', 'gm'))]; assert.equal(markers.length, 1);
        const value = JSON.parse(markers[0][1]); assert.equal(value.passed, true); assert.equal(value.profile, profile); assert.equal(value.round, round);
        const checks = lifecycle ? ['cleanDecode', 'updateNotInsert', 'independentLedgers', 'staleConflict', 'failedIntentRetained',
          'partialRejectedBeforeProvider', 'untouchedPartialReference', 'deleteRecover', 'zeroProviderCodec']
          : ['nestedIdentity', 'loadedNull', 'notLoaded', 'zeroProviderCodec', 'duplicateKeysRejected'];
        for (const key of checks) assert.equal(value[key], true);
        rounds.push(value); assert.deepEqual(fixture(), input); assert.equal(snapshot(), before);
      }
      report.profiles[profile] = {rounds, sameDatabase: true, databaseSha256: sha(fs.readFileSync(path.join(evidence, 'fixture.sqlite')))};
    }
    report.passed = true; console.log('PASS three generated Java model JSON ' + (lifecycle ? 'lifecycles' : 'profiles') + ' twice with local runtime');
  } catch (error) { report.error = error.stack; console.error(error.stack); process.exitCode ||= 1; }
  finally {
    report.disposedOwnedBytes = runner.safeToClean ? ownedBytes(scratch) : 0;
    if (runner.safeToClean) fs.rmSync(scratch, {recursive: true}); else report.unsafeBuildRetention = scratch;
    report.scratchRemoved = !fs.existsSync(scratch); runner.dispose();
    fs.writeFileSync(path.join(output, 'receipt.json'), JSON.stringify(report, null, 2) + '\n');
    console.log('Retained compact JSON-profile evidence: ' + output);
  }
}
main().catch(error => { console.error(error.stack); process.exitCode ||= 1; });
