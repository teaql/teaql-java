'use strict';
const {currentLibraryInputs}=require('./current-library.cjs');
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
    const fixtureInputs = inputs(); report.currentLibrary=currentLibraryInputs(); report.fixture = fixtureInputs;
    const sourceFiles = cp.execFileSync('git', ['-C', repo, 'ls-files', '-z', '--cached', '--others', '--exclude-standard'])
      .toString().split('\0').filter(Boolean);
    const runtime = path.join(scratch, 'runtime'), bundle = path.join(scratch, 'java-lib-core'), generated = path.join(bundle, 'lib');
    const consumer = path.join(scratch, 'java-app-console'), tests = path.join(consumer, 'showcase-tests/io/teaql/examples/contextfix');
    const sourceHash = crypto.createHash('sha256');
    for (const name of [...new Set(sourceFiles)].sort()) {
      const file = path.join(repo, name); if (!fs.existsSync(file)) continue;
      assert(fs.lstatSync(file).isFile(), 'no source symlinks');
      const bytes = fs.readFileSync(file); sourceHash.update(name + '\0'); sourceHash.update(bytes);
      const target = path.join(runtime, name); fs.mkdirSync(path.dirname(target), {recursive: true}); fs.copyFileSync(file, target);
    }
    report.sourceCommit = cp.execFileSync('git', ['-C', repo, 'rev-parse', 'HEAD']).toString().trim();
    report.sourceSha256 = sourceHash.digest('hex');
    fs.cpSync(path.join(__dirname,'java-lib-core'),bundle,{recursive:true});
    const version = fs.readFileSync(path.join(runtime, 'pom.xml'), 'utf8').match(/<version>([^<]+)<\/version>/)[1];
    report.metadataOverride = require('../../../scripts/pin-local-example-pom.cjs').pin(path.join(generated, 'pom.xml'), runtime, scratch);
    fs.cpSync(path.join(__dirname,'java-app-console'),consumer,{recursive:true});
    report.applicationSha256=sha(fs.readFileSync(path.join(tests,'GeneratedContextFixTest.java')));
    const pom=fs.readFileSync(path.join(consumer,'pom.xml'),'utf8').replace('../../../../pom.xml','../runtime/pom.xml');
    fs.writeFileSync(path.join(consumer,'pom.xml'),pom);fs.writeFileSync(path.join(output,'consumer-pom.xml'),pom);
    const reactor = path.join(scratch, 'pom.xml');
    fs.writeFileSync(reactor, '<project><modelVersion>4.0.0</modelVersion><groupId>io.teaql.test</groupId><artifactId>context-reactor</artifactId><version>0</version><packaging>pom</packaging><modules><module>runtime</module><module>java-lib-core/lib</module><module>java-app-console</module></modules></project>');
    if (process.env.TEAQL_CONTEXT_FIX_UNIT_TESTS === '1') {
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
          if (summary) report.unit.suites.push({module, file, tests: +summary[1], failures: +summary[2], errors: +summary[3], skipped: +summary[4]});
        }
      }
      assert(report.unit.suites.some(suite => suite.file.includes('ContextDerivedMutationTest') && suite.tests >= 7));
    }
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
      assert.match(bytes.toString(), /JAVA_CONTEXT_FIX_EXTENSIONS=passed dynamicOnly=true readonlyNoop=true equalDynamicNoop=true/);
      assert.match(bytes.toString(), /JAVA_CONTEXT_FIX_GRAPH=passed indexedLocation=true automaticUndo=true childOnly=true/);
      assert.deepEqual(currentLibraryInputs(),report.currentLibrary); assert.deepEqual(inputs(), fixtureInputs, 'generated fixture changed');
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
