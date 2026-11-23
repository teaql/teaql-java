'use strict';
const fs=require('node:fs'),path=require('node:path'),os=require('node:os'),cp=require('node:child_process');
const crypto=require('node:crypto'),assert=require('node:assert/strict');
const {createOwnedRunner,ownedBytes}=require('../fix-e-alignment/owned-command.cjs');
const sha=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
const repo=path.resolve(__dirname,'../..');
const app=path.join(__dirname,'java-app-console'), library=path.join(__dirname,'java-lib-core/lib');
async function main(){
  const args={};for(let i=2;i<process.argv.length;i+=2){const key=process.argv[i];assert(['--output','--provider','--postgres-container','--postgres-port'].includes(key)&&process.argv[i+1]);assert(!args[key]);args[key]=process.argv[i+1];}
  const output=args['--output'],provider=args['--provider']||'sqlite';
  assert(output&&path.isAbsolute(output)&&!fs.existsSync(output));assert(['sqlite','postgres'].includes(provider));
  const targets=fs.readdirSync(repo).filter(name=>fs.existsSync(path.join(repo,name,'pom.xml'))).map(name=>path.join(repo,name,'target'));
  targets.push(path.join(app,'target'),path.join(library,'target'));
  assert(targets.every(file=>!fs.existsSync(file)),'Use an isolated target-free runtime checkout');
  assert(fs.statfsSync(repo).bavail*fs.statfsSync(repo).bsize>4*1024**3);
  fs.mkdirSync(output,{recursive:true});const runner=createOwnedRunner();let scratch,owned=false;
  const receipt={schema:'teaql.java-inherited-save-parity/v1',passed:false,publication:false,provider,commands:[],rounds:[]};
  try{
    scratch=fs.mkdtempSync(path.join(os.tmpdir(),'teaql-java-inheritance-'));owned=true;
    const generated=JSON.parse(fs.readFileSync(path.join(__dirname,'generated-files.json')));
    const inventory=()=>Object.fromEntries(Object.keys(generated).map(name=>[name,sha(fs.readFileSync(path.join(__dirname,'java-lib-core',name)))]));
    assert.deepEqual(inventory(),generated);receipt.generatedFileHashes=generated;
    receipt.sourceCommit=cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim();
    const sources=['pom.xml','teaql-core/src/main/java/io/teaql/core/Entity.java','teaql-core/src/main/java/io/teaql/core/Audited.java',
      'teaql-core/src/main/java/io/teaql/core/BaseEntity.java','teaql-core/src/main/java/io/teaql/core/checker/Checker.java',
      'teaql-core/src/main/java/io/teaql/core/EntityStatus.java','teaql-core/src/test/java/io/teaql/core/EntityMutationLedgerTest.java',
      'teaql-runtime/src/main/java/io/teaql/runtime/TeaQLRuntime.java','teaql-runtime/src/test/java/io/teaql/runtime/TeaQLRuntimeTest.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/PortableSQLRepository.java','teaql-sql-portable/src/main/java/io/teaql/core/sql/SqlAstCompiler.java',
      'examples/fix-e-alignment/owned-command.cjs','examples/inheritance-save/java-app-console/pom.xml','examples/inheritance-save/verify.cjs',
      'examples/inheritance-save/java-app-console/src/test/java/io/teaql/examples/inheritance/InheritanceLifecycleExampleTest.java'];
    sources.push('examples/inheritance-save/java-app-console/src/test/java/io/teaql/examples/inheritance/GraphRollbackProbe.java');
    sources.push('teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/PortableSQLDataService.java',
      'teaql-sql-portable/src/test/java/io/teaql/core/sql/portable/VersionLifecyclePortableSQLTest.java',
      'teaql-sql-portable/src/test/java/io/teaql/core/sql/portable/SqlParameterPropagationTest.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/IdSpaceIdGenerator.java',
      'teaql-sql-portable/src/test/java/io/teaql/core/sql/portable/IdSpaceIdGeneratorTest.java');
    const sourceHashes=()=>Object.fromEntries(sources.map(name=>[name,sha(fs.readFileSync(path.join(repo,name)))]));
    receipt.sourceHashes=sourceHashes();
    const version=fs.readFileSync(path.join(repo,'pom.xml'),'utf8').match(/<version>([^<]+)<\/version>/)[1];
    receipt.localSourceVersionOverride=version;
    const escape=value=>value.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('"','&quot;');
    const reactor='<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>io.teaql.test</groupId><artifactId>inherited-save-reactor</artifactId><version>0.0.0</version><packaging>pom</packaging><modules>'
      +[repo,library,app].map(dir=>'<module>'+escape(path.relative(scratch,dir))+'</module>').join('')+'</modules></project>';
    fs.writeFileSync(path.join(scratch,'pom.xml'),reactor);fs.writeFileSync(path.join(output,'reactor-pom.xml'),reactor);
    const env={...process.env,TEAQL_TELEMETRY_ENABLED:'false'};
    async function run(command,log){const fd=fs.openSync(path.join(output,log),'wx');let result;
      try{result=await runner.run(command,{cwd:repo,env,timeout:900000,stdio:['ignore',fd,fd]});}finally{fs.closeSync(fd);}
      const bytes=fs.readFileSync(path.join(output,log));receipt.commands.push({command,log,exitCode:result.status,signal:result.signal,logSha256:sha(bytes)});
      assert.equal(result.status,0,bytes.toString().slice(-7000));return bytes.toString();
    }
    let pgSql;
    if(provider==='postgres'){
      const container=args['--postgres-container'],port=Number(args['--postgres-port']);
      assert(container&&/^[a-zA-Z0-9_-]+$/.test(container));assert(Number.isInteger(port)&&port>0&&port<65536);
      const database='teaql_java_inherited_280_'+sha(Buffer.from(output)).slice(0,12);
      const sql=(db,statement)=>cp.execFileSync('docker',['exec','-i','-u','postgres',container,'psql','-X','-v','ON_ERROR_STOP=1','-At','-d',db],{input:statement,encoding:'utf8',stdio:['pipe','pipe','pipe']}).trim();
      assert.equal(sql('postgres',"SELECT count(*) FROM pg_database WHERE datname='"+database+"';"),'0','Never reuse an unexpected database');
      sql('postgres','CREATE DATABASE '+database+';');sql('postgres',"COMMENT ON DATABASE "+database+" IS 'TeaQL #280 owned inheritance parity fixture';");
      const password=cp.execFileSync('docker',['exec',container,'sh','-c','printf %s "$POSTGRES_PASSWORD"'],{encoding:'utf8',stdio:['ignore','pipe','pipe']});assert(password);
      env.TEAQL_TEST_INHERITANCE_JDBC_URL='jdbc:postgresql://127.0.0.1:'+port+'/'+database;env.TEAQL_TEST_POSTGRES_USER='postgres';env.TEAQL_TEST_POSTGRES_PASSWORD=password;
      receipt.database={name:database,container,port,created:true,retained:true,ownership:'TeaQL #280 owned inheritance parity fixture'};
      pgSql=statement=>sql(database,statement);
    }
    for(const round of [1,2]){
      const log=await run([process.env.MAVEN||'mvn','-o','-B','-ntp','-f',path.join(scratch,'pom.xml'),'-pl',':inherited-save-example','-am',
        '-Dteaql.version='+version,'-Dtest=InheritanceLifecycleExampleTest','-Dsurefire.failIfNoSpecifiedTests=false',
        '-Dteaql.expected.runtime.root='+repo,'-Dteaql.expected.inheritance.root='+library,
        '-Dteaql.inheritance.provider='+provider,'-Dteaql.inheritance.round='+round,
        '-Dteaql.inheritance.database='+path.join(output,'inheritance.sqlite'),'test'],'round-'+round+'.log');
      const lines=log.split('\n').filter(line=>line.startsWith('INHERITED_SAVE_PARITY='));assert.equal(lines.length,1);
      const proof=JSON.parse(lines[0].slice('INHERITED_SAVE_PARITY='.length));assert.equal(proof.passed,true);assert.equal(proof.round,round);assert.equal(proof.manualParentSeed,false);
      assert.deepEqual(proof.versions,[1,2,3,-4,5,-6,7,8]);assert.equal(proof.genericSavePreservesType,true);assert.equal(proof.sameIdOtherTypeUnchanged,true);
      for(const key of ['recoveryWithoutScalarEdit','recoveryWithEdits','staleRecoveryRejected'])assert.equal(proof[key],true,key);
      for(const key of ['partialRecoveryRejected','liveRecoveryRejected','recoveryAuditLineage','mixedGraphRollback','retrySameGraph'])assert.equal(proof[key],true,key);
      assert.equal(proof.rollback.passed,true);assert.equal(proof.rollback.retrySameGraph,true);assert.equal(proof.rollback.failedAudits,0);assert.equal(proof.rollback.retryAudits,2);
      assert.equal(proof.typedViewSubtypeMetadata,true);
      assert.equal(proof.parentNoopSubtypeRetained,true);
      assert.deepEqual(proof.subtypeViews,[['School','Academy','Academy'],['School','TrainingAcademy','Academy'],
        ['Academy','TrainingAcademy','TrainingAcademy'],['TrainingAcademy','TrainingAcademy','TrainingAcademy'],
        ['School','College','College'],['School','School','School']]);
      receipt.rounds.push(proof);
      const report=path.join(app,'target/surefire-reports/TEST-io.teaql.examples.inheritance.InheritanceLifecycleExampleTest.xml');
      const xml=fs.readFileSync(report,'utf8');for(const key of ['failures','errors','skipped'])assert.equal(Number(xml.match(new RegExp('\\b'+key+'="(\\d+)"'))[1]),0);
      fs.copyFileSync(report,path.join(output,'junit-'+round+'.xml'));
    }
    await run([process.env.MAVEN||'mvn','-o','-B','-ntp','-pl','teaql-runtime','-am',
      '-Dtest=AuditedTest,EntityMutationLedgerTest,TeaQLRuntimeTest','-Dsurefire.failIfNoSpecifiedTests=false','test'],'native-view-dispatch.log');
    for(const [module,name] of [['teaql-core','io.teaql.core.AuditedTest'],['teaql-core','io.teaql.core.EntityMutationLedgerTest'],['teaql-runtime','io.teaql.runtime.TeaQLRuntimeTest']]){
      const report=path.join(repo,module,'target/surefire-reports/TEST-'+name+'.xml');const xml=fs.readFileSync(report,'utf8');
      assert(Number(xml.match(/\btests="(\d+)"/)[1])>0);
      for(const key of ['failures','errors','skipped'])assert.equal(Number(xml.match(new RegExp('\\b'+key+'="(\\d+)"'))[1]),0);
      fs.copyFileSync(report,path.join(output,'junit-'+name+'.xml'));
    }
    await run([process.env.MAVEN||'mvn','-o','-B','-ntp','-pl','teaql-sql-portable','-am',
      '-Dtest=VersionLifecyclePortableSQLTest,SqlParameterPropagationTest,IdSpaceIdGeneratorTest','-Dsurefire.failIfNoSpecifiedTests=false','test'],'native-recovery-provider.log');
    for(const name of ['VersionLifecyclePortableSQLTest','SqlParameterPropagationTest','IdSpaceIdGeneratorTest']){
      const report=path.join(repo,'teaql-sql-portable/target/surefire-reports/TEST-io.teaql.core.sql.portable.'+name+'.xml');const xml=fs.readFileSync(report,'utf8');
      assert(Number(xml.match(/\btests="(\d+)"/)[1])>0);
      for(const key of ['failures','errors','skipped'])assert.equal(Number(xml.match(new RegExp('\\b'+key+'="(\\d+)"'))[1]),0);
      fs.copyFileSync(report,path.join(output,'junit-'+name+'.xml'));
    }
    // Independent, read-only storage oracle; the application never uses SQL to
    // create a parent, repair a discriminator, or conceal failed persistence.
    const parentQuery='SELECT id,version FROM school_data ORDER BY id';
    const childQuery='SELECT a.id,s.version,a."Legacy Campus Code" FROM academy_data a JOIN school_data s ON s.id=a.id ORDER BY a.id';
    const constantQuery='SELECT id,version FROM school_type_data ORDER BY id';
    if(provider==='sqlite'){
      const python='import sqlite3,sys,pathlib,json; c=sqlite3.connect(pathlib.Path(sys.argv[1]).resolve().as_uri()+"?mode=ro",uri=True); r={"parents":c.execute(sys.argv[2]).fetchall(),"children":c.execute(sys.argv[3]).fetchall(),"constants":c.execute(sys.argv[4]).fetchall(),"childColumns":[x[1] for x in c.execute("PRAGMA table_info(academy_data)")]}; c.close(); print(json.dumps(r))';
      receipt.oracle=JSON.parse(await run(['python3','-B','-c',python,path.join(output,'inheritance.sqlite'),parentQuery,childQuery,constantQuery],'oracle.log'));
    }else{
      const rows=query=>JSON.parse(pgSql("BEGIN READ ONLY; SELECT coalesce(jsonb_agg(to_jsonb(t)),'[]'::jsonb)::text FROM ("+query+") t; COMMIT;").split('\n').find(line=>line.startsWith('[')));
      receipt.oracle={parents:rows(parentQuery).map(x=>[x.id,x.version]),children:rows(childQuery).map(x=>[x.id,x.version,x['Legacy Campus Code']]),
        constants:rows(constantQuery).map(x=>[x.id,x.version]),childColumns:rows("SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND table_name='academy_data' ORDER BY ordinal_position").map(x=>x.column_name)};
      fs.writeFileSync(path.join(output,'oracle.log'),JSON.stringify(receipt.oracle,null,2)+'\n');
    }
    assert.deepEqual(receipt.oracle.parents,[[1001,8],[1002,8],[2001,1],[2002,1],[3001,1],[3002,1],[4001,1],[4002,1]]);
    assert.deepEqual(receipt.oracle.children,[[1001,8,'WEST'],[1002,8,'WEST'],[2001,1,'NORTH'],[2002,1,'NORTH']]);
    assert.deepEqual(receipt.oracle.constants,[[1001,1],[1002,1]]);assert(!receipt.oracle.childColumns.includes('version'));
    receipt.independentReadOnlyOracle=true;
    assert.deepEqual(inventory(),generated);receipt.generatedLibraryUnchanged=true;assert.deepEqual(sourceHashes(),receipt.sourceHashes);receipt.sameDatabase=true;receipt.passed=true;
  }catch(error){receipt.error=String(error);process.exitCode=1;}
  finally{
    if(owned&&runner.safeToClean){const reports=path.join(app,'target/surefire-reports');if(fs.existsSync(reports)){
      fs.mkdirSync(path.join(output,'last-junit'),{recursive:true});
      for(const file of fs.readdirSync(reports))if(/\.(xml|txt)$/.test(file))fs.copyFileSync(path.join(reports,file),path.join(output,'last-junit',file));
    }}
    receipt.disposableBytesRemoved=0;
    if(runner.safeToClean&&owned){for(const dir of [...targets,scratch])if(fs.existsSync(dir)){receipt.disposableBytesRemoved+=ownedBytes(dir);fs.rmSync(dir,{recursive:true});}}
    else if(scratch)receipt.unsafeRetention={scratch,targets,processGroup:runner.unsafeGroup};
    runner.dispose();receipt.scratchRemoved=!scratch||!fs.existsSync(scratch);
    fs.writeFileSync(path.join(output,'receipt.json'),JSON.stringify(receipt,null,2)+'\n');
    console.log(JSON.stringify({passed:receipt.passed,evidence:output,error:receipt.error?.slice(-1500),scratchRemoved:receipt.scratchRemoved,disposableBytesRemoved:receipt.disposableBytesRemoved}));
  }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
