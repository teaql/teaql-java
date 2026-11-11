'use strict';
const fs = require('node:fs'), path = require('node:path'), os = require('node:os');
const cp = require('node:child_process'), crypto = require('node:crypto'), assert = require('node:assert/strict');
const {createOwnedRunner, ownedBytes} = require('../../fix-e-alignment/owned-command.cjs');
const repo = path.resolve(__dirname, '../../..'), fixture = path.join(__dirname, 'fixture');
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const requested = process.env.TEAQL_CONTEXT_FIX_EVIDENCE_DIR;
const output = requested ? path.resolve(requested) : fs.mkdtempSync(path.join(os.tmpdir(), 'teaql-context-evidence-'));
if (requested) { assert(!fs.existsSync(output), 'use a new evidence directory'); fs.mkdirSync(output, {recursive: true}); }
const scratch = fs.mkdtempSync(path.join(os.tmpdir(), 'teaql-context-build-'));
const runner = createOwnedRunner();
const report = {schema: 'teaql.java-context-derived-mutation/v1', passed: false, rounds: [], publicPublication: false};
function inputs() {
  const manifest = fs.readFileSync(path.join(fixture, 'generated-files.json'));
  const files = JSON.parse(manifest);
  for (const [name, hash] of Object.entries(files)) {
    assert((name.startsWith('lib/') || name === 'teaql/model-provenance.json') && !name.split('/').includes('..'));
    const file = path.join(fixture, name); assert.equal(fs.realpathSync(file), file);
    assert.equal(sha(fs.readFileSync(file)), hash, name);
  }
  return {files, manifestSha256: sha(manifest), modelSha256: sha(fs.readFileSync(path.join(fixture, 'model.xml')))};
}
async function main() {
  try {
    const disk = fs.statfsSync(scratch); assert(disk.bavail * disk.bsize > 2 * 1024 ** 3);
    const fixtureInputs = inputs(); report.fixture = fixtureInputs;
    const sourceFiles = cp.execFileSync('git', ['-C', repo, 'ls-files', '-z', '--cached', '--others', '--exclude-standard'])
      .toString().split('\0').filter(Boolean);
    const runtime = path.join(scratch, 'runtime'), bundle = path.join(scratch, 'model'), generated = path.join(bundle, 'lib');
    const consumer = path.join(scratch, 'consumer'), tests = path.join(consumer, 'src/test/java/io/teaql/examples/contextfix');
    const sourceHash = crypto.createHash('sha256');
    for (const name of [...new Set(sourceFiles)].sort()) {
      const file = path.join(repo, name); if (!fs.existsSync(file)) continue;
      assert(fs.lstatSync(file).isFile(), 'no source symlinks');
      const bytes = fs.readFileSync(file); sourceHash.update(name + '\0'); sourceHash.update(bytes);
      const target = path.join(runtime, name); fs.mkdirSync(path.dirname(target), {recursive: true}); fs.copyFileSync(file, target);
    }
    report.sourceCommit = cp.execFileSync('git', ['-C', repo, 'rev-parse', 'HEAD']).toString().trim();
    report.sourceSha256 = sourceHash.digest('hex');
    for (const name of Object.keys(fixtureInputs.files)) {
      const target = path.join(bundle, name); fs.mkdirSync(path.dirname(target), {recursive: true});
      fs.copyFileSync(path.join(fixture, name), target);
    }
    const version = fs.readFileSync(path.join(runtime, 'pom.xml'), 'utf8').match(/<version>([^<]+)<\/version>/)[1];
    report.metadataOverride = require('../../../scripts/pin-local-example-pom.cjs').pin(path.join(generated, 'pom.xml'), runtime, scratch);
    fs.mkdirSync(tests, {recursive: true});
    const app = fs.readFileSync(path.join(__dirname, 'GeneratedContextFixTest.java'));
    report.applicationSha256 = sha(app); fs.writeFileSync(path.join(tests, 'GeneratedContextFixTest.java'), app);
    const dependencies = ['teaql-runtime', 'teaql-provider-jdbc', 'teaql-sqlite'].map(name =>
      `<dependency><groupId>io.teaql</groupId><artifactId>${name}</artifactId><version>${version}</version></dependency>`).join('');
    fs.writeFileSync(path.join(consumer, 'pom.xml'), `<project><modelVersion>4.0.0</modelVersion><parent><groupId>io.teaql</groupId><artifactId>teaql-java-parent</artifactId><version>${version}</version><relativePath>../runtime/pom.xml</relativePath></parent><groupId>io.teaql.examples</groupId><artifactId>context-fix-consumer</artifactId><version>${version}</version><dependencies><dependency><groupId>com.example</groupId><artifactId>context-fix-service-core</artifactId><version>0.0.1-SNAPSHOT</version></dependency>${dependencies}<dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4.13.2</version><scope>test</scope></dependency></dependencies><build><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><configuration><useModulePath>false</useModulePath></configuration></plugin></plugins></build></project>`);
    const reactor = path.join(scratch, 'pom.xml');
    fs.writeFileSync(reactor, '<project><modelVersion>4.0.0</modelVersion><groupId>io.teaql.test</groupId><artifactId>context-reactor</artifactId><version>0</version><packaging>pom</packaging><modules><module>runtime</module><module>model/lib</module><module>consumer</module></modules></project>');
    for (const round of [1, 2]) {
      const command = [process.env.MAVEN || 'mvn', '-o', '-q', '-ntp', '-f', reactor, '-pl', ':context-fix-consumer', '-am',
        'test', '-Dtest=GeneratedContextFixTest', '-Dsurefire.failIfNoSpecifiedTests=false', '-Dteaql.expected.runtime.root=' + runtime];
      const log = path.join(output, 'round-' + round + '.log'), fd = fs.openSync(log, 'wx'); let result;
      try { result = await runner.run(command, {cwd: scratch, timeout: 300000, env: {...process.env,
        TEAQL_CONTEXT_FIX_DB: path.join(output, 'fixture.sqlite')}, stdio: ['ignore', fd, fd]}); }
      finally { fs.closeSync(fd); }
      const bytes = fs.readFileSync(log);
      report.rounds.push({round, command, exitCode: result.status, logSha256: sha(bytes)});
      assert.equal(result.status, 0, bytes.toString().slice(-6000));
      assert.match(bytes.toString(), /JAVA_CONTEXT_FIX_SQLITE=passed typedQ=true typedE=true noOpProviderCalls=0 failedCheckProviderCalls=0 updateAudits=1 objectLocation=true/);
      assert.deepEqual(inputs(), fixtureInputs, 'generated fixture changed');
    }
    report.sameDatabase = true; report.databaseSha256 = sha(fs.readFileSync(path.join(output, 'fixture.sqlite')));
    report.passed = true; console.log('PASS generated Java Context Fix SQLite example twice with local runtime');
  } catch (error) { report.error = error.stack; process.exitCode ||= 1; console.error(error.stack); }
  finally {
    report.disposedOwnedBytes = runner.safeToClean ? ownedBytes(scratch) : 0;
    if (runner.safeToClean) fs.rmSync(scratch, {recursive: true}); else report.unsafeBuildRetention = scratch;
    report.scratchRemoved = !fs.existsSync(scratch); runner.dispose();
    fs.writeFileSync(path.join(output, 'receipt.json'), JSON.stringify(report, null, 2) + '\n');
    console.log('Context Fix evidence: ' + output);
  }
}
main().catch(error => { console.error(error.stack); process.exitCode ||= 1; });
