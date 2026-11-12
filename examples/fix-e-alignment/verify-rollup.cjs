'use strict';
// A local-source gate: never installs/publishes Maven artifacts. Only outputs
// created in this initially target-free worktree are eligible for cleanup.
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const cp = require('node:child_process'), crypto = require('node:crypto');
const {createOwnedRunner, ownedBytes} = require('./owned-command.cjs');
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
async function main() {
    const repo = path.resolve(__dirname, '../..');
    const output = process.argv[2];
    assert(output && path.isAbsolute(output) && !fs.existsSync(output), 'Supply a fresh absolute evidence path');
    const targets = fs.readdirSync(repo).filter(name => fs.existsSync(path.join(repo, name, 'pom.xml')))
        .map(name => path.join(repo, name, 'target'));
    assert(targets.every(target => !fs.existsSync(target)), 'Use an isolated worktree without Maven targets');
    assert(fs.statfsSync(repo).bavail * fs.statfsSync(repo).bsize > 4 * 1024 ** 3, 'Need 4 GiB free');
    fs.mkdirSync(output, {recursive: true});
    const receipt = {schema: 'teaql.java-rollup-acceptance/v1', passed: false, publication: false,
        commit: cp.execFileSync('git', ['rev-parse', 'HEAD'], {cwd: repo, encoding: 'utf8'}).trim()};
    const runner = createOwnedRunner();
    try {
        const source = 'teaql-sql-portable/src/test/java/io/teaql/core/sql/portable/PortableSQLDatabaseTest.java';
        receipt.testSourceSha256 = hash(fs.readFileSync(path.join(repo, source)));
        receipt.sourceHashes = {};
        for (const name of [source, 'teaql-runtime/src/main/java/io/teaql/runtime/TeaQLRuntime.java',
            'teaql-sql-portable/src/test/java/io/teaql/core/sql/portable/LayeredAggregationTest.java',
            'teaql-core/src/main/java/io/teaql/core/SearchRequest.java',
            'teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/PortableSQLRepository.java',
            'teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/LayeredAggregation.java',
            'teaql-runtime/src/main/java/io/teaql/runtime/StatisticsRequestPreparation.java',
            'examples/fix-e-alignment/verify-rollup.cjs']) {
            const bytes = fs.readFileSync(path.join(repo, name)); receipt.sourceHashes[name] = hash(bytes);
            const target = path.join(output, 'sources', name); fs.mkdirSync(path.dirname(target), {recursive: true});
            fs.writeFileSync(target, bytes);
        }
        fs.writeFileSync(path.join(output, 'source-changes.patch'),
            cp.execFileSync('git', ['diff', '--binary', 'HEAD'], {cwd: repo}));
        const command = [process.env.MAVEN || 'mvn', '-o', '-B', '-pl', 'teaql-sql-portable', '-am',
            '-Dtest=PortableSQLDatabaseTest#rollup*,LayeredAggregationTest', '-Dsurefire.failIfNoSpecifiedTests=false', 'test'];
        const fd = fs.openSync(path.join(output, 'test.log'), 'wx');
        try { receipt.result = await runner.run(command, {cwd: repo, env: process.env,
            timeout: 900000, stdio: ['ignore', fd, fd]}); }
        finally { fs.closeSync(fd); }
        receipt.command = command;
        receipt.logSha256 = hash(fs.readFileSync(path.join(output, 'test.log')));
        const report = path.join(repo, 'teaql-sql-portable/target/surefire-reports/TEST-io.teaql.core.sql.portable.PortableSQLDatabaseTest.xml');
        assert(fs.existsSync(report), 'No JUnit report: this is not a verified runtime result');
        fs.copyFileSync(report, path.join(output, 'junit.xml'));
        const xml = fs.readFileSync(report, 'utf8');
        const counts = xml => Object.fromEntries(['tests', 'failures', 'errors', 'skipped'].map(name => {
            const match = xml.match(new RegExp('\\b' + name + '="(\\d+)"'));
            assert(match, 'Missing JUnit count: ' + name); return [name, Number(match[1])];
        }));
        const layeredReport = path.join(path.dirname(report), 'TEST-io.teaql.core.sql.portable.LayeredAggregationTest.xml');
        assert(fs.existsSync(layeredReport), 'No layered statistics report');
        fs.copyFileSync(layeredReport, path.join(output, 'junit-layered.xml'));
        receipt.focusedSuites = [counts(xml), counts(fs.readFileSync(layeredReport, 'utf8'))];
        receipt.junit = Object.fromEntries(['tests', 'failures', 'errors', 'skipped'].map(key =>
            [key, receipt.focusedSuites.reduce((sum, suite) => sum + suite[key], 0)]));
        receipt.passed = receipt.result.status === 0 && receipt.junit.tests >= 17
            && ['failures', 'errors', 'skipped'].every(name => receipt.junit[name] === 0);
        assert(receipt.passed, 'Roll-up acceptance failed: ' + JSON.stringify(receipt.junit));
        receipt.passed = false;
        const regressionCommand = [process.env.MAVEN || 'mvn', '-o', '-B', '-pl', 'teaql-sql-portable', '-am', 'test'];
        const regressionFd = fs.openSync(path.join(output, 'native-regression.log'), 'wx'); let regression;
        try { regression = await runner.run(regressionCommand, {cwd: repo, env: process.env,
            timeout: 900000, stdio: ['ignore', regressionFd, regressionFd]}); }
        finally { fs.closeSync(regressionFd); }
        const suites = {};
        for (const target of targets) {
            const directory = path.join(target, 'surefire-reports');
            if (!fs.existsSync(directory)) continue;
            const files = fs.readdirSync(directory).filter(name => /^TEST-.*\.xml$/.test(name));
            suites[path.basename(path.dirname(target))] = files.map(name => ({name, ...counts(fs.readFileSync(path.join(directory, name), 'utf8'))}));
        }
        receipt.nativeRegression = {command: regressionCommand, result: regression, suites,
            logSha256: hash(fs.readFileSync(path.join(output, 'native-regression.log')))};
        assert.equal(regression.status, 0, 'Native regression failed');
        for (const module of ['teaql-core', 'teaql-runtime', 'teaql-sql-portable']) {
            assert(suites[module]?.reduce((sum, suite) => sum + suite.tests, 0) >= 20, 'Missing complete native suite: ' + module);
        }
        assert(Object.values(suites).flat().every(suite => suite.failures === 0 && suite.errors === 0), 'Native JUnit failure');
        for (const [name, expected] of Object.entries(receipt.sourceHashes)) {
            assert.equal(hash(fs.readFileSync(path.join(repo, name))), expected, 'Source changed during verification: ' + name);
        }
        receipt.passed = true;
    } catch (error) {
        receipt.error = error.message; process.exitCode = runner.interrupted === 'SIGINT' ? 130 : 1;
    } finally {
        receipt.cleanup = [];
        if (runner.safeToClean) {
            for (const target of targets) if (fs.existsSync(target)) {
                const bytes = ownedBytes(target); fs.rmSync(target, {recursive: true});
                receipt.cleanup.push({path: target, bytes});
            }
        } else receipt.unsafeRetention = {targets, processGroup: runner.unsafeGroup};
        runner.dispose();
        fs.writeFileSync(path.join(output, 'receipt.json'), JSON.stringify(receipt, null, 2) + '\n');
        process.stdout.write(JSON.stringify({passed: receipt.passed, evidence: output, focused: receipt.junit,
            nativeTests: Object.values(receipt.nativeRegression?.suites || {}).flat().reduce((sum, suite) => sum + suite.tests, 0),
            error: receipt.error, removedBytes: receipt.cleanup.reduce((sum, item) => sum + item.bytes, 0)}) + '\n');
    }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
