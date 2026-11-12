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
        fs.writeFileSync(path.join(output, 'source-changes.patch'),
            cp.execFileSync('git', ['diff', '--binary', 'HEAD'], {cwd: repo}));
        const command = [process.env.MAVEN || 'mvn', '-o', '-B', '-pl', 'teaql-sql-portable', '-am',
            '-Dtest=PortableSQLDatabaseTest#rollup*', '-Dsurefire.failIfNoSpecifiedTests=false', 'test'];
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
        receipt.junit = Object.fromEntries(['tests', 'failures', 'errors', 'skipped'].map(name => {
            const match = xml.match(new RegExp('\\b' + name + '="(\\d+)"'));
            assert(match, 'Missing JUnit count: ' + name); return [name, Number(match[1])];
        }));
        receipt.passed = receipt.result.status === 0 && receipt.junit.tests >= 2
            && ['failures', 'errors', 'skipped'].every(name => receipt.junit[name] === 0);
        assert(receipt.passed, 'Roll-up acceptance failed: ' + JSON.stringify(receipt.junit));
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
        process.stdout.write(JSON.stringify(receipt, null, 2) + '\n');
    }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
