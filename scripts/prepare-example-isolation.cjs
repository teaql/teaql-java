#!/usr/bin/env node
"use strict";
const fs = require('node:fs'), path = require('node:path'), os = require('node:os');
const cp = require('node:child_process'), crypto = require('node:crypto'), assert = require('node:assert/strict');
const [buildRoot, generatorSource, evidence] = process.argv.slice(2);
assert(buildRoot && generatorSource && evidence && process.argv.length === 5);
const build = fs.realpathSync(buildRoot), source = fs.realpathSync(generatorSource);
const git = (...args) => cp.execFileSync('git', ['-C', source, ...args], {maxBuffer: 32 * 1024 * 1024});
assert.equal(fs.realpathSync(git('rev-parse', '--show-toplevel').toString().trim()), source);
const maven = path.join(build, 'maven'), generator = path.join(build, 'generator');
assert(!fs.existsSync(maven) && !fs.existsSync(generator));
const cache = path.resolve(process.env.TEAQL_MAVEN_DEPENDENCY_CACHE || path.join(os.homedir(), '.m2/repository'));
assert(cache !== maven && !maven.startsWith(cache + path.sep));
let dependencyFiles = 0;
if (fs.existsSync(cache)) fs.cpSync(cache, maven, {recursive: true, filter: file => {
  const relative = path.relative(cache, file), parts = relative.split(path.sep);
  const stat = fs.lstatSync(file);
  // Reuse third-party downloads only. No TeaQL/group/model artifact may leak
  // from another local build, and symlinks or in-flight cache files are ignored.
  if (stat.isSymbolicLink() || parts.some(part => /^teaql(?:$|-)/.test(part))
      || /(?:\.lastUpdated|\.part|\.lock)$/.test(file)) return false;
  if (stat.isFile()) dependencyFiles++;
  return true;
}});
fs.mkdirSync(maven, {recursive: true});
const hash = crypto.createHash('sha256');
const files = [...new Set(git('ls-files', '-z', '--cached', '--others', '--exclude-standard').toString().split('\0').filter(Boolean))].sort();
for (const relative of files) {
  assert(!path.isAbsolute(relative) && !relative.split(/[\\/]/).includes('..'));
  const original = path.join(source, relative);
  if (!fs.existsSync(original)) continue;
  assert(fs.lstatSync(original).isFile(), 'generator source requires regular files');
  const destination = path.join(generator, relative), bytes = fs.readFileSync(original);
  fs.mkdirSync(path.dirname(destination), {recursive: true}); fs.writeFileSync(destination, bytes);
  hash.update(relative + '\0').update(bytes).update('\0');
}
fs.mkdirSync(evidence, {recursive: true});
fs.writeFileSync(path.join(evidence, 'isolation.json'), JSON.stringify({
  schema: 'teaql.java-example-isolation/v1', sourceGenerator: source,
  generatorCommit: git('rev-parse', 'HEAD').toString().trim(), generatorSourceSha256: hash.digest('hex'),
  copiedGeneratorSourceFiles: files.length, reusedThirdPartyDependencyFiles: dependencyFiles,
  cachedTeaqlArtifactsCopied: false, isolatedMavenRepository: true, sharedRepositoryWrites: false,
}, null, 2) + '\n', {flag: 'wx'});
