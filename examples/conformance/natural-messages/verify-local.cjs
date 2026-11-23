'use strict';
const fs = require('node:fs'), path = require('node:path'), os = require('node:os');
const cp = require('node:child_process'), crypto = require('node:crypto'), assert = require('node:assert/strict');
const {createOwnedRunner, ownedBytes} = require('../../fix-e-alignment/owned-command.cjs');
const repo = path.resolve(__dirname, '../../..'), fixture = path.join(__dirname, 'automatic-fixture');
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const requested = process.env.TEAQL_NATURAL_MESSAGES_EVIDENCE_DIR;
const output = requested ? path.resolve(requested) : fs.mkdtempSync(path.join(os.tmpdir(), 'teaql-natural-evidence-'));
if (requested) { assert(!fs.existsSync(output), 'use a new evidence directory'); fs.mkdirSync(output, {recursive: true}); }
const scratch = fs.mkdtempSync(path.join(os.tmpdir(), 'teaql-natural-build-'));
const runner = createOwnedRunner();
const report = {schema: 'teaql.java-natural-checker-messages/v1', passed: false, rounds: [], publicPublication: false};
function inputs() {
  const manifest = fs.readFileSync(path.join(fixture, 'generated-files.json'));
  const files = JSON.parse(manifest);
  for (const [name, hash] of Object.entries(files)) {
    assert((name.startsWith('lib/') || ['teaql/model-provenance.json','teaql/checker-model.json'].includes(name)) && !name.split('/').includes('..'));
    const file = path.join(fixture, name); assert.equal(fs.realpathSync(file), file);
    assert.equal(sha(fs.readFileSync(file)), hash, name);
  }
  return {files, manifestSha256: sha(manifest), modelSha256: sha(fs.readFileSync(path.join(__dirname, 'model.xml')))};
}
async function main() {
  try {
    const disk = fs.statfsSync(scratch); assert(disk.bavail * disk.bsize > 2 * 1024 ** 3);
    const fixtureInputs = inputs(); report.fixture = fixtureInputs;
    report.generation = JSON.parse(fs.readFileSync(path.join(fixture, 'generation.json')));
    assert.equal(report.generation.modelSha256, fixtureInputs.modelSha256);
    report.schema = 'teaql.java-natural-checker-messages/v1';
    const sourceFiles = cp.execFileSync('git', ['-C', repo, 'ls-files', '-z', '--cached', '--others', '--exclude-standard'])
      .toString().split('\0').filter(Boolean);
    const runtime = path.join(scratch, 'runtime'), bundle = path.join(scratch, 'model'), generated = path.join(bundle, 'lib');
    const consumer = path.join(scratch, 'consumer'), tests = path.join(consumer, 'src/test/java/io/teaql/examples/naturalmessages');
    const sourceHash = crypto.createHash('sha256'); report.sourceHashes = {};
    for (const name of [...new Set(sourceFiles)].sort()) {
      const file = path.join(repo, name);
      assert(fs.existsSync(file) && fs.lstatSync(file).isFile(), 'source missing or not a regular file: ' + name);
      assert.equal(fs.realpathSync(file), file, 'source path escapes through a link');
      const bytes = fs.readFileSync(file); sourceHash.update(name + '\0'); sourceHash.update(bytes);
      report.sourceHashes[name] = sha(bytes);
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
    const app = fs.readFileSync(path.join(__dirname, 'GeneratedNaturalMessagesTest.java'));
    report.applicationSha256 = sha(app); fs.writeFileSync(path.join(tests, 'GeneratedNaturalMessagesTest.java'), app);
    const dependencies = ['teaql-runtime', 'teaql-provider-jdbc', 'teaql-sqlite'].map(name =>
      `<dependency><groupId>io.teaql</groupId><artifactId>${name}</artifactId><version>${version}</version></dependency>`).join('');
    fs.writeFileSync(path.join(consumer, 'pom.xml'), `<project><modelVersion>4.0.0</modelVersion><parent><groupId>io.teaql</groupId><artifactId>teaql-java-parent</artifactId><version>${version}</version><relativePath>../runtime/pom.xml</relativePath></parent><groupId>io.teaql.examples</groupId><artifactId>natural-checker-consumer</artifactId><version>${version}</version><dependencies><dependency><groupId>com.example</groupId><artifactId>checker-natural-message-service-core</artifactId><version>0.0.1-SNAPSHOT</version></dependency>${dependencies}<dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4.13.2</version><scope>test</scope></dependency></dependencies><build><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><configuration><useModulePath>false</useModulePath></configuration></plugin></plugins></build></project>`);
    const reactor = path.join(scratch, 'pom.xml');
    fs.writeFileSync(reactor, '<project><modelVersion>4.0.0</modelVersion><groupId>io.teaql.test</groupId><artifactId>natural-checker-reactor</artifactId><version>0</version><packaging>pom</packaging><modules><module>runtime</module><module>model/lib</module><module>consumer</module></modules></project>');
    if (process.env.TEAQL_NATURAL_MESSAGES_UNIT_TESTS === '1') {
      const command = [process.env.MAVEN || 'mvn', '-o', '-q', '-ntp', '-f', path.join(runtime, 'pom.xml'),
        '-pl', 'teaql-runtime', '-am', 'test'];
      const log = path.join(output, 'unit.log'), fd = fs.openSync(log, 'wx'); let result;
      try { result = await runner.run(command, {cwd: scratch, timeout: 300000, env: {...process.env}, stdio: ['ignore', fd, fd]}); }
      finally { fs.closeSync(fd); }
      report.unit = {command, exitCode: result.status, logSha256: sha(fs.readFileSync(log))};
      assert.equal(result.status, 0, fs.readFileSync(log, 'utf8').slice(-6000));
      report.unit.suites = [];
      for (const module of fs.readdirSync(runtime)) {
        const directory = path.join(runtime, module, 'target', 'surefire-reports');
        if (fs.existsSync(directory)) for (const file of fs.readdirSync(directory).filter(name => name.endsWith('.txt'))) {
          const summary = fs.readFileSync(path.join(directory, file), 'utf8').match(/Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)/);
          if (summary) {
            const xml = file.replace(/\.txt$/, '.xml'), xmlFile = 'TEST-' + xml;
            const bytes = fs.readFileSync(path.join(directory, xmlFile));
            const retained = path.join(output, 'unit-reports', module, xmlFile);
            fs.mkdirSync(path.dirname(retained), {recursive: true}); fs.writeFileSync(retained, bytes);
            report.unit.suites.push({module, file, tests: +summary[1], failures: +summary[2], errors: +summary[3], skipped: +summary[4],
              report: path.relative(output, retained), sha256: sha(bytes)});
          }
        }
      }
      assert(report.unit.suites.some(suite => suite.file.includes('NaturalCheckerMessageTest') && suite.tests >= 15));
    }
    for (const round of [1, 2]) {
      const command = [process.env.MAVEN || 'mvn', '-o', '-q', '-ntp', '-f', reactor, '-pl', ':natural-checker-consumer', '-am',
        'test', '-Dtest=GeneratedNaturalMessagesTest', '-Dsurefire.failIfNoSpecifiedTests=false', '-Dteaql.expected.runtime.root=' + runtime];
      const log = path.join(output, 'round-' + round + '.log'), fd = fs.openSync(log, 'wx'); let result;
      try { result = await runner.run(command, {cwd: scratch, timeout: 300000, env: {...process.env,
        TEAQL_NATURAL_MESSAGES_DB: path.join(output, 'fixture.sqlite')}, stdio: ['ignore', fd, fd]}); }
      finally { fs.closeSync(fd); }
      const bytes = fs.readFileSync(log);
      report.rounds.push({round, command, exitCode: result.status, logSha256: sha(bytes)});
      assert.equal(result.status, 0, bytes.toString().slice(-6000));
      const frames = [...bytes.toString().matchAll(/^JAVA_NATURAL_CHECKER_SQLITE=(.+)$/gm)].map(match => JSON.parse(match[1]));
      assert.equal(frames.length, 1); const facts = frames[0];
      assert(facts.passed && facts.typedQ && facts.typedE && facts.automaticMetadata && facts.originalGeneratedChecker && facts.unchangedParent);
      assert.equal(facts.updatedAudits, 1); assert.equal(facts.customerRendererCalls, 1); assert.equal(facts.responses, 4);
      assert.equal(facts.providerCallsOnRejection, 0); assert.equal(facts.transactionCallsOnRejection, 0);
      const responses = [...bytes.toString().matchAll(/^JAVA_NATURAL_CHECKER_RESPONSE=(.+)$/gm)].map(match => JSON.parse(match[1]));
      assert.equal(responses.length, 4);
      for (const response of responses) {
        assert.equal(response.errors.length, 1);
        assert(response.errors.every(error => error.entityType === 'Entry' && error.instancePath === '/entryList/1/name'));
        assert(!JSON.stringify(response).includes('密钥🔑' + 'x'.repeat(98)), 'private name reached the response');
      }
      report.rounds[report.rounds.length - 1].responses = responses;
      report.rounds[report.rounds.length - 1].facts = facts;
      assert.deepEqual(inputs(), fixtureInputs, 'generated fixture changed');
    }
    const oracle = cp.spawnSync('python3', ['-c', 'import json,sqlite3,sys; c=sqlite3.connect("file:"+sys.argv[1]+"?mode=ro",uri=True); print(json.dumps({"integrity":c.execute("pragma quick_check").fetchone()[0],"rows":c.execute("select name,version from entry_data order by id").fetchall()})); c.close()', path.join(output, 'fixture.sqlite')], {encoding:'utf8'});
    assert.equal(oracle.status, 0, oracle.stderr); report.databaseOracle = JSON.parse(oracle.stdout);
    assert.deepEqual(report.databaseOracle, {integrity:'ok', rows:[['accepted',2],['second',1],['accepted',2],['second',1]]});
    const finalFiles = cp.execFileSync('git', ['-C', repo, 'ls-files', '-z', '--cached', '--others', '--exclude-standard'])
      .toString().split('\0').filter(Boolean);
    const finalHashes = {};
    for (const name of [...new Set(finalFiles)].sort()) {
      const file = path.join(repo, name); assert(fs.lstatSync(file).isFile(), 'final source not a regular file: ' + name);
      assert.equal(fs.realpathSync(file), file); finalHashes[name] = sha(fs.readFileSync(file));
    }
    assert.deepEqual(finalHashes, report.sourceHashes, 'runtime, generated library or application changed during verification');
    report.sourceUnchanged = true;
    report.sameDatabase = true; report.databaseSha256 = sha(fs.readFileSync(path.join(output, 'fixture.sqlite')));
    report.passed = true; console.log('PASS generated Java natural Checker messages through SQLite twice with local runtime');
  } catch (error) { report.error = error.stack; process.exitCode ||= 1; console.error(error.stack); }
  finally {
    report.disposedOwnedBytes = runner.safeToClean ? ownedBytes(scratch) : 0;
    if (runner.safeToClean) fs.rmSync(scratch, {recursive: true}); else report.unsafeBuildRetention = scratch;
    report.scratchRemoved = !fs.existsSync(scratch); runner.dispose();
    fs.writeFileSync(path.join(output, 'receipt.json'), JSON.stringify(report, null, 2) + '\n');
    console.log('Natural Checker evidence: ' + output);
  }
}
main().catch(error => { console.error(error.stack); process.exitCode ||= 1; });
