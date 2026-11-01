#!/usr/bin/env node
'use strict';
const fs=require('node:fs'),path=require('node:path'),os=require('node:os'),cp=require('node:child_process');
const assert=require('node:assert/strict'),crypto=require('node:crypto');
const {createOwnedRunner,ownedBytes}=require('../../fix-e-alignment/owned-command.cjs');
const ROOT=path.resolve(__dirname,'../../..'),REFERENCE='6d162bb0cff43335e570302a95a5abc721915b77';
const CASES=['admission','audit-delivery-failure','graph-write-failure','nested-rollback','outer-commit-noop','outer-rollback-retry','requires-new','savepoint-boundaries'];
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const digest=f=>sha(fs.readFileSync(f));
const git=(...args)=>cp.execFileSync('git',['-C',ROOT,...args],{maxBuffer:64*1024**2});
function source(){
 const files=[...new Set(git('ls-files','-z','--cached','--others','--exclude-standard').toString().split('\0').filter(Boolean))].sort();
 const hash=crypto.createHash('sha256');
 for(const n of files){const f=path.join(ROOT,n);assert(fs.lstatSync(f).isFile(),n);hash.update(n+'\0').update(String(fs.statSync(f).mode)+'\0').update(fs.readFileSync(f)).update('\0');}
 return{commit:git('rev-parse','HEAD').toString().trim(),contentsSha256:hash.digest('hex'),files,dirty:git('status','--porcelain').toString()};
}
function fixture(){
 const prefix='examples/conformance/lib/';
 const files=git('ls-files','-z','--',prefix).toString().split('\0').filter(Boolean).sort();assert(files.length>10);
 const hashes={};for(const n of files){hashes[n.slice(prefix.length)]=digest(path.join(ROOT,n));assert.equal(sha(git('show','HEAD:'+n)),hashes[n.slice(prefix.length)],'never edit generated library: '+n);}
 const model='examples/conformance/model.xml';assert.equal(digest(path.join(ROOT,model)),sha(git('show','HEAD:'+model)));
 return{origin:'existing tracked two-object example; not regenerated in this gate',model,modelSha256:digest(path.join(ROOT,model)),files:hashes};
}
function seedPublicMaven(destination){
 const cache=path.resolve(process.env.TEAQL_MAVEN_DEPENDENCY_CACHE||path.join(os.homedir(),'.m2/repository'));
 assert(!fs.existsSync(destination));fs.mkdirSync(destination);let files=0,bytes=0;
 function visit(relative=''){
  for(const n of fs.readdirSync(path.join(cache,relative))){const rel=path.join(relative,n),from=path.join(cache,rel),stat=fs.lstatSync(from);
   if(stat.isSymbolicLink()||rel==='io/teaql'||rel==='com/teaql'||rel.startsWith('org/owasp/dependency-check-data'))continue;
   if(stat.isDirectory())visit(rel);else if(stat.isFile()&&/\.(jar|pom|sha1|sha256|sha512|md5|xml)$/.test(n)){
    const to=path.join(destination,rel);fs.mkdirSync(path.dirname(to),{recursive:true});fs.copyFileSync(from,to,fs.constants.COPYFILE_EXCL);files++;bytes+=stat.size;
   }
  }
 }
 visit();return{files,bytes,teaqlArtifactsBorrowed:0,cacheDatabasesCopied:false};
}

async function main(){
 assert(process.argv.length===2||process.argv.length===3&&process.argv[2]==='--baseline');
 const baseline=process.argv[2]==='--baseline',requested=process.env.TEAQL_JAVA_SPRING_EVIDENCE_DIR;
 const output=requested?path.resolve(requested):fs.mkdtempSync(path.join(os.tmpdir(),'teaql-java-spring-business-evidence-'));
 if(requested){assert(path.isAbsolute(requested)&&!fs.existsSync(output),'new absolute evidence directory required');fs.mkdirSync(output,{recursive:true});}
 const scratch=fs.mkdtempSync(path.join(os.tmpdir(),'teaql-java-spring-business-build-')),runner=createOwnedRunner();
 const report={schema:'teaql.java-spring-business-example/v1',passed:false,baseline,commands:[],rounds:[],sameDatabases:true,
  currentInstalledArtifactAccepted:false,allTransactionProvidersAccepted:false,allP0P1Complete:false,publicPublication:false};
 async function run(name,command,cwd,extra={}){
  const fd=fs.openSync(path.join(output,name+'.log'),'wx');let result;
  const env={...process.env,...extra,TMPDIR:scratch};for(const n of ['MAVEN_ARGS','MAVEN_OPTS','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS'])delete env[n];
  try{result=await runner.run(command,{cwd,env,timeout:300000,stdio:['ignore',fd,fd]});}finally{fs.closeSync(fd);}
  report.commands.push({name,command,cwd,exitCode:result.status,signal:result.signal,logSha256:digest(path.join(output,name+'.log'))});
  assert(!result.error&&result.signal===null&&runner.safeToClean);return result;
 }
 try{
  const disk=fs.statfsSync(scratch);assert(disk.bavail*disk.bsize>8*1024**3);
  const original=source();report.source=original;report.fixture=fixture();report.applicationSha256=digest(path.join(__dirname,'GeneratedSpringTransactionTest.java'));report.verifierSha256=digest(__filename);
  const runtime=path.join(scratch,'source'),consumer=path.join(scratch,'consumer'),maven=path.join(scratch,'maven');
  fs.mkdirSync(runtime);report.testedProductionCommit=baseline?REFERENCE:original.commit;
  if(baseline){
   const archive=path.join(scratch,'source.tar');assert.equal((await run('archive',['git','-C',ROOT,'archive','--format=tar','--output='+archive,REFERENCE],scratch)).status,0);
   assert.equal((await run('extract',['tar','-xf',archive,'-C',runtime],scratch)).status,0);fs.unlinkSync(archive);
  }else for(const n of original.files){const to=path.join(runtime,n);fs.mkdirSync(path.dirname(to),{recursive:true});fs.copyFileSync(path.join(ROOT,n),to);}
  // This library is already a module of the runtime's reactor. Verify it in
  // place rather than introduce a second module with the same Maven identity.
  for(const[n,h]of Object.entries(report.fixture.files))assert.equal(digest(path.join(runtime,'examples/conformance/lib',n)),h);
  const tests=path.join(consumer,'src/test/java/com/teaql/runtimeexampleconformanceservice');fs.mkdirSync(tests,{recursive:true});
  fs.copyFileSync(path.join(__dirname,'GeneratedSpringTransactionTest.java'),path.join(tests,'GeneratedSpringTransactionTest.java'));
  const version=fs.readFileSync(path.join(runtime,'pom.xml'),'utf8').match(/<version>([^<]+)<\/version>/)[1];
  const dependencies=['teaql-runtime','teaql-sqlite','teaql-provider-spring-jdbc','teaql-jackson'].map(n=>`<dependency><groupId>io.teaql</groupId><artifactId>${n}</artifactId><version>${version}</version></dependency>`).join('');
  const pom=`<project><modelVersion>4.0.0</modelVersion><parent><groupId>io.teaql</groupId><artifactId>teaql-java-parent</artifactId><version>${version}</version><relativePath>../source/pom.xml</relativePath></parent><groupId>io.teaql.examples</groupId><artifactId>teaql-spring-business-example</artifactId><version>${version}</version><dependencies><dependency><groupId>com.teaql</groupId><artifactId>runtime-example-conformance-service-core</artifactId><version>1.0.0</version></dependency>${dependencies}<dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4.13.2</version><scope>test</scope></dependency></dependencies><build><plugins><plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><configuration><useModulePath>false</useModulePath></configuration></plugin></plugins></build></project>`;
  fs.writeFileSync(path.join(consumer,'pom.xml'),pom);fs.writeFileSync(path.join(output,'consumer-pom.xml'),pom,{flag:'wx'});
  fs.writeFileSync(path.join(scratch,'pom.xml'),'<project><modelVersion>4.0.0</modelVersion><groupId>io.teaql.test</groupId><artifactId>spring-business-reactor</artifactId><version>0.0.0</version><packaging>pom</packaging><modules><module>source</module><module>consumer</module></modules></project>');
  report.cache=seedPublicMaven(maven);const databases=path.join(output,'databases');fs.mkdirSync(databases);
  for(const round of [1,2]){
   const command=['mvn','-o','-B','-ntp','-Dmaven.repo.local='+maven,'-f',path.join(scratch,'pom.xml'),'-pl',':teaql-spring-business-example','-am','test','-Dtest=GeneratedSpringTransactionTest','-Dsurefire.failIfNoSpecifiedTests=false','-Dteaql.version='+version,'-Dteaql.expected.runtime.root='+runtime];
   const result=await run('round-'+round,command,scratch,{TEAQL_SPRING_DB_ROOT:databases,TEAQL_SPRING_ROUND:String(round)});
   const log=fs.readFileSync(path.join(output,'round-'+round+'.log'),'utf8');
   if(/COMPILATION ERROR/.test(log))throw Error('Application did not compile:\n'+log.split('\n').filter(n=>n.startsWith('[ERROR]')).slice(0,30).join('\n'));
   const file=path.join(consumer,'target/surefire-reports/TEST-com.teaql.runtimeexampleconformanceservice.GeneratedSpringTransactionTest.xml');
   if(fs.existsSync(file)){fs.copyFileSync(file,path.join(output,'round-'+round+'-tests.xml'));report.rounds.push({round,junitSha256:digest(file),counts:log.match(/Tests run: 8, Failures: \d+, Errors: \d+, Skipped: \d+[^\n]*/)?.[0]});}
   const values=[...log.matchAll(/^JAVA_SPRING_BUSINESS=(.+)$/gm)].map(m=>JSON.parse(m[1]));
   if(report.rounds.length)report.rounds.at(-1).observations=values;
   assert.equal(result.status,0,log.slice(-14000));assert.equal(values.length,8);assert.deepEqual(values.map(v=>v.case).sort(),CASES);
   assert(values.every(v=>v.passed&&v.round===round));assert.match(log,/Tests run: 8, Failures: 0, Errors: 0, Skipped: 0/);
   assert.deepEqual(fixture(),report.fixture);assert.deepEqual(source(),original);
  }
  assert(!baseline,'a passing baseline does not reproduce the defect');report.passed=true;
 }catch(error){report.error=error.stack;console.error(error.stack);process.exitCode=1;}
 finally{
  report.databases={};const dir=path.join(output,'databases');if(fs.existsSync(dir))for(const n of fs.readdirSync(dir).sort()){assert(n.endsWith('.sqlite'));report.databases[n]={bytes:fs.statSync(path.join(dir,n)).size,sha256:digest(path.join(dir,n))};}
  const safe=runner.safeToClean;report.cleanup={scratchRemoved:false,disposedOwnedBytes:safe?ownedBytes(scratch):0,sharedCachesDeleted:false,originalSourcesDeleted:false,databasesDeleted:false};
  if(safe)fs.rmSync(scratch,{recursive:true});else report.unsafeBuildRetention=scratch;
  report.cleanup.scratchRemoved=!fs.existsSync(scratch);runner.dispose();fs.writeFileSync(path.join(output,'receipt.json'),JSON.stringify(report,null,2)+'\n',{flag:'wx'});
  console.log(JSON.stringify({output,passed:report.passed,baseline,rounds:report.rounds.length,scratchRemoved:report.cleanup.scratchRemoved,currentInstalledArtifactAccepted:false}));
 }
}
if(require.main===module)main().catch(error=>{console.error(error.stack);process.exitCode=1;});
module.exports={CASES,source,fixture,seedPublicMaven};
