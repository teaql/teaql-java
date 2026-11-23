#!/usr/bin/env node
"use strict";
// Source-only ordinary example. No install/deploy, source editing or shared cleanup.
const fs = require("node:fs"), path = require("node:path"), os = require("node:os");
const cp = require("node:child_process"), crypto = require("node:crypto"), assert = require("node:assert/strict");
const runtimeRoot = path.resolve(__dirname, "../..");
const requested = process.env.TEAQL_JAVA_EXACT_EVIDENCE_DIR;
const evidence = requested ? path.resolve(requested) : fs.mkdtempSync(path.join(os.tmpdir(), "teaql-java-decimal-evidence-"));
if (requested) { assert(!fs.existsSync(evidence), "never overwrite retained evidence"); fs.mkdirSync(evidence); }
const sha = bytes => crypto.createHash("sha256").update(bytes).digest("hex");
const git = (root, ...args) => cp.execFileSync("git", ["-C", root, ...args], {maxBuffer: 32 * 1024 * 1024});
const commands = [];
function files(root) {
  assert.equal(fs.realpathSync(git(root, "rev-parse", "--show-toplevel").toString().trim()), fs.realpathSync(root));
  return [...new Set(git(root, "ls-files", "-z", "--cached", "--others", "--exclude-standard").toString().split("\0").filter(Boolean))].sort();
}
function snapshot(root) {
  const hash = crypto.createHash("sha256");
  for (const file of files(root)) {
    assert(!path.isAbsolute(file) && !file.split("/").includes(".."));
    const absolute = path.join(root, file);
    hash.update(file + "\0");
    if (!fs.existsSync(absolute)) { hash.update("deleted\0"); continue; }
    assert(fs.lstatSync(absolute).isFile(), "source snapshot requires regular files: " + file);
    hash.update(fs.readFileSync(absolute)); hash.update("\0");
  }
  return {commit: git(root, "rev-parse", "HEAD").toString().trim(), diffSha256: sha(git(root, "diff", "--binary", "HEAD")),
    sourceSha256: hash.digest("hex")};
}
function copy(root, destination) {
  for (const file of files(root)) {
    const original = path.join(root, file);
    if (!fs.existsSync(original)) continue;
    assert(fs.lstatSync(original).isFile());
    const target = path.join(destination, file);
    fs.mkdirSync(path.dirname(target), {recursive: true}); fs.copyFileSync(original, target);
  }
}
function run(name, command, cwd, env = {}) {
  const log = path.join(evidence, name + ".log"), fd = fs.openSync(log, "wx"), started = Date.now();
  let result;
  try { result = cp.spawnSync("timeout", ["--kill-after=10s", "300s", ...command], {
    cwd, env: {...process.env, ...env}, stdio: ["ignore", fd, fd]}); }
  finally { fs.closeSync(fd); }
  commands.push({name, command, cwd, exitCode: result.status, signal: result.signal,
    milliseconds: Date.now() - started, logSha256: sha(fs.readFileSync(log))});
  assert.equal(result.status, 0, `${name} failed; see ${log}`);
  return fs.readFileSync(log, "utf8");
}
function generatedHashes(root) {
  const result = {};
  function walk(directory) {
    for (const file of fs.readdirSync(directory).sort()) {
      if (file === "target") continue;
      const full = path.join(directory, file), stat = fs.lstatSync(full);
      assert(!stat.isSymbolicLink());
      if (stat.isDirectory()) walk(full);
      else { assert(stat.isFile()); result[path.relative(root, full)] = sha(fs.readFileSync(full)); }
    }
  }
  walk(root); return result;
}
const before = {runtime: snapshot(runtimeRoot)};
const scratch = fs.mkdtempSync(path.join(os.tmpdir(), "teaql-java-decimal-build-"));
let passed = false, failure, generated, localClasses = {}, artifactSha256;
try {
  const disk = fs.statfsSync(scratch);
  assert(disk.bavail * disk.bsize > 8 * 1024 ** 3, "8 GiB free build space required");
  const runtime = path.join(scratch, "runtime");
  copy(runtimeRoot, runtime);
  const example = path.join(runtime, 'examples/exact-decimal');
  const app = path.join(example, 'java-app-console');
  const library = path.join(example, 'java-lib-core');
  const modelSha256 = sha(fs.readFileSync(path.join(example, 'model.xml')));
  const spec = JSON.parse(fs.readFileSync(path.join(example, 'showcase.json')));
  assert.equal(modelSha256, spec.model.sha256);
  const version = fs.readFileSync(path.join(runtime, "pom.xml"), "utf8").match(/<version>([^<]+)<\/version>/)[1];
  const evaluatedVersion = fs.readFileSync(path.join(app, 'pom.xml'), 'utf8')
    .match(/<teaql.version>([^<]+)<\/teaql.version>/)[1];
  assert.equal(version, evaluatedVersion, 'Maven reactor must match the evaluated example runtime');
  const scopes = JSON.parse(fs.readFileSync(path.join(example, 'generated-scope-hashes.json')));
  function verifyScopes() {
    assert.deepEqual(generatedHashes(library), scopes['java-lib-core']);
    const observed = generatedHashes(app);
    assert.equal(spec.workspace.useSiblingMavenLibrary, true);
    assert(!fs.existsSync(path.join(app, 'lib')), 'Workspace must consume the sibling library');
    for (const [name, digest] of Object.entries(scopes['java-app-console'])) {
      if (name.startsWith('lib/')) {
        if (name !== 'lib/pom.xml') assert.equal(scopes['java-lib-core'][name], digest, name);
        continue;
      }
      if (name === 'Makefile') {
        const installed = fs.readFileSync(path.join(app, name), 'utf8');
        const sibling = 'mvn -f ../java-lib-core/lib/pom.xml';
        assert.equal(installed.split(sibling).length, 3);
        assert.equal(sha(installed.replaceAll(sibling, 'mvn -f lib/pom.xml')), digest, name);
        continue;
      }
      assert.equal(observed[name], digest, 'generated workspace file changed: ' + name);
    }
  }
  verifyScopes();
  generated = {modelSha256, producer: spec.producer, files: scopes, workspaceUsesSiblingMavenLibrary: true};
  fs.writeFileSync(path.join(evidence, "generated-manifest.json"), JSON.stringify(generated, null, 2) + "\n");
  const reactor = path.join(scratch, "pom.xml");
  fs.writeFileSync(reactor, '<project><modelVersion>4.0.0</modelVersion><groupId>io.teaql.test</groupId><artifactId>exact-decimal-source-reactor</artifactId><version>0.0.0</version><packaging>pom</packaging><modules><module>runtime</module><module>runtime/examples/exact-decimal/java-lib-core/lib</module><module>runtime/examples/exact-decimal/java-app-console</module></modules></project>\n');
  run("source-paired-build", ["mvn", "-B", "-ntp", "-f", reactor, "-pl", ":exact-decimal-service", "-am", "package",
    "-DskipTests", "-Dteaql.version=" + version], scratch);
  const artifact = path.join(app, "target/exact-decimal-service-0.0.1-SNAPSHOT.jar");
  artifactSha256 = sha(fs.readFileSync(artifact));
  for (const [module, entry] of [
    ["teaql-core", "io/teaql/core/UserContext.class"],
    ["teaql-runtime", "io/teaql/runtime/TeaQLRuntime.class"],
    ["teaql-runtime", "io/teaql/runtime/memory/MemoryDataService.class"],
    ["teaql-runtime", "io/teaql/runtime/memory/MemoryAggregations.class"],
    ["teaql-sqlite", "io/teaql/core/sqlite/SqliteDecimalFunctions.class"],
  ]) {
    const compiled = fs.readFileSync(path.join(runtime, module, "target/classes", entry));
    const packaged = cp.execFileSync("unzip", ["-p", artifact, entry]);
    assert.equal(sha(packaged), sha(compiled), "workspace shaded a different runtime: " + entry);
    localClasses[entry] = sha(compiled);
  }
  for (const round of [1, 2]) {
    const log = run("round-" + round, ["java", "-cp", artifact, "com.example.exactdecimalservice.GeneratedExactDecimalProbe"], scratch,
      {TEAQL_EXACT_DECIMAL_DB: path.join(evidence, "source.sqlite")});
    assert(log.includes("GENERATED_EXACT_DECIMAL_PASS q=1 e=1 mutation=1 sum=0.3 scale=3.50 precision=19 exponent=2 rows=5 fresh=" + (round === 1)));
    const startup = run("workspace-" + round, ["java", "-cp", artifact, "com.example.exactdecimalservice.GeneratedWorkspaceStartupProbe",
      "--teaql.datasource.url=jdbc:sqlite:" + path.join(evidence, "workspace.sqlite")], scratch);
    assert(startup.includes("GENERATED_EXACT_DECIMAL_WORKSPACE_PASS"));
    const memory = run("memory-" + round, ["java", "-cp", artifact, "com.example.exactdecimalservice.GeneratedMemoryAggregateProbe"], scratch);
    assert(memory.includes("GENERATED_MEMORY_AGGREGATE_PASS q=1 e=1 mutation=1 sum=0.3 empty=null fallback=0 rows=2 sql=0"));
    verifyScopes();
  }
  assert.deepEqual({runtime: snapshot(runtimeRoot)}, before, "source changed during verification");
  passed = true;
} catch (error) { failure = error.stack; process.exitCode = 1; console.error(error.message); }
finally {
  // All child commands are synchronous, have returned, and are bounded by timeout.
  const ownedBytes = Number(cp.execFileSync("du", ["-sb", scratch], {encoding: "utf8"}).split(/\s/)[0]);
  fs.rmSync(scratch, {recursive: true});
  const databases = {};
  for (const name of ["source.sqlite", "workspace.sqlite"]) {
    const file = path.join(evidence, name);
    if (fs.existsSync(file)) databases[name] = {bytes: fs.statSync(file).size, sha256: sha(fs.readFileSync(file))};
  }
  fs.writeFileSync(path.join(evidence, "receipt.json"), JSON.stringify({schema: "teaql.java-exact-source-example/v1", passed,
    before, commands, failure, modelSha256: generated?.modelSha256, artifactSha256, localClasses, databases,
    rounds: passed ? 2 : undefined, sameDatabase: true, generatedLibraryUnchanged: passed,
    disposedOwnedBytes: ownedBytes, scratchRemoved: !fs.existsSync(scratch), sharedCacheInstall: false,
    publicPublication: false, internalRegistryAcceptance: false}, null, 2) + "\n");
  if (passed) console.log("GENERATED_EXACT_DECIMAL_GATE_PASS rounds=2 workspace=2 library_unchanged=1 source_paired=1 evidence=" + evidence);
}
