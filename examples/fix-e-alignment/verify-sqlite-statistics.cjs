'use strict';
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const cp = require('node:child_process'), crypto = require('node:crypto');
const {createOwnedRunner, ownedBytes} = require('./owned-command.cjs');
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const counts = xml => Object.fromEntries(['tests', 'failures', 'errors', 'skipped'].map(key => {
    const match = xml.match(new RegExp('\\b' + key + '="(\\d+)"'));
    assert(match, 'Missing JUnit count: ' + key); return [key, Number(match[1])];
}));
async function main() {
    const repo = path.resolve(__dirname, '../..'), output = process.argv[2];
    assert(output && path.isAbsolute(output) && !fs.existsSync(output), 'Supply a fresh absolute evidence path');
    const targets = fs.readdirSync(repo).filter(name => fs.existsSync(path.join(repo, name, 'pom.xml')))
        .map(name => path.join(repo, name, 'target'));
    assert(targets.every(target => !fs.existsSync(target)), 'Isolated target-free worktree required');
    assert(fs.statfsSync(repo).bavail * fs.statfsSync(repo).bsize > 4 * 1024 ** 3, 'Need 4 GiB free');
    fs.mkdirSync(output, {recursive: true});
    const runner = createOwnedRunner();
    const receipt = {schema: 'teaql.java-sqlite-statistics/v1', passed: false, publication: false,
        commit: cp.execFileSync('git', ['rev-parse', 'HEAD'], {cwd: repo, encoding: 'utf8'}).trim(),
        sourceHashes: {}, commands: []};
    try {
        for (const name of ['pom.xml', 'teaql-sqlite/pom.xml',
            'teaql-core/src/main/java/io/teaql/core/AggrFunction.java',
            'teaql-core/src/main/java/io/teaql/core/BaseRequest.java',
            'teaql-core/src/main/java/io/teaql/core/QueryRequestSnapshot.java',
            'teaql-core/src/main/java/io/teaql/core/QueryRequestGraphCopy.java',
            'teaql-core/src/test/java/io/teaql/core/QueryRequestSnapshotIsolationTest.java',
            'teaql-runtime/src/test/java/io/teaql/runtime/RequestedListCountTest.java',
            'teaql-runtime/src/test/java/io/teaql/runtime/TeaQLRuntimeTest.java',
            'teaql-sqlite/src/main/java/io/teaql/core/sqlite/SqliteDataServiceExecutor.java',
            'teaql-sqlite/src/main/java/io/teaql/core/sqlite/SqliteDialect.java',
            'teaql-sqlite/src/main/java/io/teaql/core/sqlite/SqliteStatisticalFunctions.java',
            'teaql-sqlite/src/test/java/io/teaql/core/sqlite/SqliteStatisticalFunctionsTest.java',
            'teaql-provider-jdbc/src/main/java/io/teaql/provider/jdbc/JdbcSqlExecutor.java',
            'teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/LayeredAggregation.java',
            'teaql-sql-portable/src/main/java/io/teaql/core/sql/internal/StatisticalGroupingExpression.java',
            'teaql-sql-portable/src/test/java/io/teaql/core/sql/internal/StatisticalGroupingExpressionTest.java',
            'teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/PortableSQLRepository.java',
            'teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/PortableSQLDataService.java',
            'teaql-sql-portable/src/main/java/io/teaql/core/sql/expression/AggrExpressionParser.java',
            'teaql-sql-portable/src/test/java/io/teaql/core/sql/expression/AggrExpressionParserTest.java',
            'teaql-sql-portable/src/test/java/io/teaql/core/sql/portable/LayeredAggregationTest.java',
            'teaql-sql-portable/src/test/java/io/teaql/core/sql/portable/PortableSQLDatabaseTest.java',
            'teaql-sql-portable/src/main/java/io/teaql/core/sql/SqlAstCompiler.java',
            'teaql-sqlite/src/test/java/io/teaql/sqlite/DerivedQueryTraceSqliteTest.java',
            'examples/fix-e-alignment/verify-sqlite-statistics.cjs',
            'examples/fix-e-alignment/owned-command.cjs']) {
            if (!fs.existsSync(path.join(repo, name))) {
                assert(name.endsWith('/SqliteStatisticalFunctions.java'), 'Missing source: ' + name);
                continue;
            }
            const bytes = fs.readFileSync(path.join(repo, name)); receipt.sourceHashes[name] = hash(bytes);
            const target = path.join(output, 'sources', name);
            fs.mkdirSync(path.dirname(target), {recursive: true}); fs.writeFileSync(target, bytes);
        }
        fs.writeFileSync(path.join(output, 'source-changes.patch'),
            cp.execFileSync('git', ['diff', '--binary', 'HEAD'], {cwd: repo}));
        async function execute(label, options) {
            const command = [process.env.MAVEN || 'mvn', '-o', '-B', '-pl', 'teaql-sqlite', '-am', ...options, 'test'];
            const log = path.join(output, label + '.log'), fd = fs.openSync(log, 'wx');
            let result;
            try { result = await runner.run(command, {cwd: repo, env: process.env,
                timeout: 900000, stdio: ['ignore', fd, fd]}); }
            finally { fs.closeSync(fd); }
            const run = {command, result, logSha256: hash(fs.readFileSync(log)), suites: {}};
            receipt.commands.push(run);
            for (const target of targets) {
                const directory = path.join(target, 'surefire-reports');
                if (!fs.existsSync(directory)) continue;
                const module = path.basename(path.dirname(target));
                const files = fs.readdirSync(directory).filter(name => /^TEST-.*\.xml$/.test(name));
                run.suites[module] = files.map(name => {
                    const bytes = fs.readFileSync(path.join(directory, name));
                    const saved = path.join(output, label, module, name);
                    fs.mkdirSync(path.dirname(saved), {recursive: true}); fs.writeFileSync(saved, bytes);
                    return {name, ...counts(bytes.toString('utf8'))};
                });
            }
            assert.equal(result.status, 0, label + ' command failed');
            assert(Object.values(run.suites).flat().every(s => !s.failures && !s.errors && !s.skipped), 'JUnit failure/skip');
            return run;
        }
        for (let round = 1; round <= 2; round++) {
            const run = await execute('focused-' + round,
                ['-Dtest=QueryRequestSnapshotIsolationTest,SqliteStatisticalFunctionsTest,DerivedQueryTraceSqliteTest#loadedCollectionFacetsRetainPerParentMembershipCountsAndRequestedEmpty,LayeredAggregationTest,AggrExpressionParserTest,StatisticalGroupingExpressionTest', '-Dsurefire.failIfNoSpecifiedTests=false']);
            assert.equal(run.suites['teaql-core']?.find(s => s.name.endsWith('.QueryRequestSnapshotIsolationTest.xml'))?.tests,
                5, 'Missing independent graph snapshot/copy acceptance');
            assert.equal(run.suites['teaql-sqlite']?.find(s => s.name.endsWith('.SqliteStatisticalFunctionsTest.xml'))?.tests,
                10, 'Missing ten statistical kernel/integration tests');
            assert.equal(run.suites['teaql-sqlite']?.find(s => s.name.endsWith('.DerivedQueryTraceSqliteTest.xml'))?.tests,
                1, 'Missing real collection Facet regression');
            assert.equal(run.suites['teaql-sql-portable']?.find(s => s.name.endsWith('.LayeredAggregationTest.xml'))?.tests,
                34, 'Missing complete shared roll-up statistics suite');
            assert.equal(run.suites['teaql-sql-portable']?.find(s => s.name.endsWith('.StatisticalGroupingExpressionTest.xml'))?.tests,
                3, 'Missing grouping expression safety and ordinary GBK compatibility tests');
            assert.equal(run.suites['teaql-sql-portable']?.find(s => s.name.endsWith('.AggrExpressionParserTest.xml'))?.tests,
                3, 'Missing aggregate operator compiler regression');
        }
        const regression = await execute('native-regression', []);
        for (const module of ['teaql-core', 'teaql-runtime', 'teaql-sql-portable', 'teaql-provider-jdbc', 'teaql-sqlite'])
            assert(regression.suites[module]?.some(s => s.tests > 0), 'Missing reactor suite: ' + module);
        for (const [name, expected] of Object.entries(receipt.sourceHashes))
            assert.equal(hash(fs.readFileSync(path.join(repo, name))), expected, 'Source changed: ' + name);
        receipt.passed = true;
    } catch (error) {
        receipt.error = error.message; process.exitCode = runner.interrupted === 'SIGINT' ? 130 : 1;
    } finally {
        receipt.cleanup = [];
        if (runner.safeToClean) {
            for (const target of targets) if (fs.existsSync(target)) {
                const bytes = ownedBytes(target); fs.rmSync(target, {recursive: true}); receipt.cleanup.push({path: target, bytes});
            }
        } else receipt.unsafeRetention = {targets, processGroup: runner.unsafeGroup};
        runner.dispose();
        fs.writeFileSync(path.join(output, 'receipt.json'), JSON.stringify(receipt, null, 2) + '\n');
        process.stdout.write(JSON.stringify({passed: receipt.passed, evidence: output,
            tests: receipt.commands.map(run => Object.values(run.suites).flat().reduce((sum, s) => sum + s.tests, 0)),
            error: receipt.error, removedBytes: receipt.cleanup.reduce((sum, item) => sum + item.bytes, 0)}) + '\n');
    }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
