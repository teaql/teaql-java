'use strict';
const fs=require('node:fs'),path=require('node:path'),os=require('node:os'),cp=require('node:child_process'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const {createOwnedRunner,ownedBytes}=require('./owned-command.cjs');
const hash=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
async function main(){
  const [output,container,port]=process.argv.slice(2),repo=path.resolve(__dirname,'../..');
  assert(output&&path.isAbsolute(output)&&!fs.existsSync(output),'Use a fresh absolute evidence path');
  assert(container&&/^[a-zA-Z0-9_-]+$/.test(container)); assert(port&&/^\d{1,5}$/.test(port)&&Number(port)>0&&Number(port)<65536);
  assert(fs.statfsSync(repo).bavail*fs.statfsSync(repo).bsize>4*1024**3);
  const targets=fs.readdirSync(repo).filter(n=>fs.existsSync(path.join(repo,n,'pom.xml'))).map(n=>path.join(repo,n,'target'));
  assert(targets.every(p=>!fs.existsSync(p)),'Use an isolated target-free Java worktree');
  fs.mkdirSync(output,{recursive:true});
  const runner=createOwnedRunner(),lock=path.join(os.tmpdir(),'teaql-rollup-postgres-279.lock');let ownsLock=false,secret='';
  const receipt={schema:'teaql.java-rollup-postgres/v1',passed:false,publication:false,commands:[],
    commit:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim()};
  const sql=(query,database='postgres')=>cp.execFileSync('docker',['exec','-i','-u','postgres',container,'psql','-X','-v','ON_ERROR_STOP=1','-d',database,'-At'],
    {input:query,encoding:'utf8',stdio:['pipe','pipe','pipe']}).trim();
  const counts=xml=>Object.fromEntries(['tests','failures','errors','skipped'].map(k=>{const m=xml.match(new RegExp('\\b'+k+'="(\\d+)"'));assert(m);return[k,Number(m[1])];}));
  try{
    const fd=fs.openSync(lock,'wx');ownsLock=true;try{fs.writeFileSync(fd,JSON.stringify({pid:process.pid,repo,output}));}finally{fs.closeSync(fd);}
    const database='teaql_rollup_279',marker='TeaQL #279 owned layered statistics fixture';
    const exists=sql("SELECT COUNT(*) FROM pg_database WHERE datname='teaql_rollup_279';")==='1';
    if(exists)assert.equal(sql("SELECT COALESCE(shobj_description(oid,'pg_database'),'') FROM pg_database WHERE datname='teaql_rollup_279';"),marker);
    else{sql('CREATE DATABASE teaql_rollup_279;');sql("COMMENT ON DATABASE teaql_rollup_279 IS 'TeaQL #279 owned layered statistics fixture';");}
    receipt.database={database,container,port:Number(port),created:!exists,ownership:marker,retained:true,server:sql('SELECT version();')};
    // Fixture provisioning only, after the exact database ownership check.
    sql('CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;',database);
    receipt.database.extensions={fuzzystrmatch:sql("SELECT extversion FROM pg_extension WHERE extname='fuzzystrmatch';",database)};
    assert(receipt.database.extensions.fuzzystrmatch,'SOUNDEX grouping requires the real PostgreSQL extension');
    secret=cp.execFileSync('docker',['exec',container,'sh','-c','printf %s "$POSTGRES_PASSWORD"'],{encoding:'utf8',stdio:['ignore','pipe','pipe']});assert(secret);
    const env={...process.env,TEAQL_TEST_POSTGRES_JDBC_URL:'jdbc:postgresql://127.0.0.1:'+port+'/'+database,
      TEAQL_TEST_POSTGRES_USER:'postgres',TEAQL_TEST_POSTGRES_PASSWORD:secret};
    receipt.sourceHashes={};
    for(const file of ['pom.xml','teaql-sql-portable/pom.xml',
      'teaql-sql-portable/src/test/java/io/teaql/core/sql/portable/LayeredAggregationTest.java',
      'teaql-sql-portable/src/test/java/io/teaql/core/sql/portable/PostgresLayeredAggregationIT.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/LayeredAggregation.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/internal/StatisticalGroupingExpression.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/internal/StatisticalIntegerOperand.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/internal/StatisticalMeasureExpression.java',
      'teaql-sql-portable/src/test/java/io/teaql/core/sql/internal/StatisticalMeasureExpressionTest.java',
      'teaql-sql-portable/src/test/java/io/teaql/core/sql/internal/StatisticalIntegerOperandTest.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/internal/StatisticalOrderExpression.java',
      'teaql-sql-portable/src/test/java/io/teaql/core/sql/internal/StatisticalOrderExpressionTest.java',
      'teaql-sql-portable/src/test/java/io/teaql/core/sql/internal/StatisticalGroupingExpressionTest.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/SqlAstCompiler.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/SqlCompilerDelegate.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/SQLColumnResolver.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/expression/PropertyParser.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/dialect/SqlDialect.java',
      'teaql-sql-portable/src/test/java/io/teaql/core/sql/portable/PortableSQLDatabaseTest.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/PortableSQLRepository.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/StatisticalResultFilter.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/expression/AggrExpressionParser.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/expression/SubQueryParser.java',
      'teaql-sql-portable/src/main/java/io/teaql/core/sql/dialect/PostgreSqlDialect.java',
      'teaql-runtime/src/main/java/io/teaql/runtime/StatisticsRequestPreparation.java',
      'teaql-core/src/main/java/io/teaql/core/BaseRequest.java','teaql-core/src/main/java/io/teaql/core/AggrFunction.java',
      'teaql-core/src/main/java/io/teaql/core/SubQuerySearchCriteria.java','teaql-core/src/main/java/io/teaql/core/QueryNavigation.java',
      'teaql-core/src/test/java/io/teaql/core/QueryNavigationTest.java',
      'teaql-core/src/main/java/io/teaql/core/QueryRequestSnapshot.java',
      'teaql-core/src/main/java/io/teaql/core/QueryRequestGraphCopy.java',
      'teaql-core/src/main/java/io/teaql/core/SearchRequest.java',
      'teaql-core/src/test/java/io/teaql/core/QueryRequestSnapshotIsolationTest.java',
      'teaql-runtime/src/test/java/io/teaql/runtime/RequestedListCountTest.java',
      'teaql-runtime/src/test/java/io/teaql/runtime/TeaQLRuntimeTest.java',
      'examples/fix-e-alignment/verify-rollup-postgres.cjs']){
      const bytes=fs.readFileSync(path.join(repo,file));receipt.sourceHashes[file]=hash(bytes);
      const target=path.join(output,'sources',file);fs.mkdirSync(path.dirname(target),{recursive:true});fs.writeFileSync(target,bytes);
    }
    async function run(command,name,runEnv=env){
      const fd=fs.openSync(path.join(output,name),'wx');let result;
      try{result=await runner.run(command,{cwd:repo,env:runEnv,timeout:900000,stdio:['ignore',fd,fd]});}finally{fs.closeSync(fd);}
      const bytes=fs.readFileSync(path.join(output,name));receipt.commands.push({command,result,log:name,logSha256:hash(bytes)});
      assert.equal(result.status,0,bytes.toString().slice(-5000));return bytes.toString();
    }
    receipt.rounds=[];
    for(const round of [1,2]){
      const log=await run([process.env.MAVEN||'mvn','-o','-B','-pl','teaql-sql-portable','-am','-Dtest=PostgresLayeredAggregationIT',
        '-Dsurefire.failIfNoSpecifiedTests=false','test'],'postgres-round-'+round+'.log');
      const report=path.join(repo,'teaql-sql-portable/target/surefire-reports/TEST-io.teaql.core.sql.portable.PostgresLayeredAggregationIT.xml');
      const xml=fs.readFileSync(report,'utf8'),junit=counts(xml);fs.copyFileSync(report,path.join(output,'junit-round-'+round+'.xml'));
      assert.deepEqual(junit,{tests:64,failures:0,errors:0,skipped:0});
      const observations=log.split(/\r?\n/).filter(l=>l.startsWith('JAVA_POSTGRES_ROLLUP=')).map(l=>JSON.parse(l.slice('JAVA_POSTGRES_ROLLUP='.length)));
      assert.deepEqual(observations.map(o=>o.case).sort(),['extended-owner-grain','empty-extended','all-null-source-singleton-parent','variance-having',
        ...['positive','negative','neighboring'].flatMap(kind=>['ordinary','parent','nested','having-ordinary','having-parent','having-nested'].map(layer=>'numeric-'+layer+'-'+kind)),
        'gbk-ordinary','gbk-parent','gbk-nested','bitwise-expression-fields','bitwise-expression-parameters',
        'expression-measure-owner-grain','expression-measure-transforms'].sort());
      receipt.rounds.push({round,junit,observations});
    }
    receipt.remainingFixtureTables=Number(sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_name IN('rollup_school','rollup_kind','rollup_platform');",database));
    assert.equal(receipt.remainingFixtureTables,0,'Each test must roll back its own fixture, not leave tables for the next test');
    const nativeEnv={...process.env};delete nativeEnv.TEAQL_TEST_POSTGRES_JDBC_URL;delete nativeEnv.TEAQL_TEST_POSTGRES_PASSWORD;delete nativeEnv.TEAQL_TEST_POSTGRES_USER;
    await run([process.env.MAVEN||'mvn','-o','-B','-pl','teaql-sql-portable','-am','test'],'native-regression.log',nativeEnv);
    receipt.nativeSuites={};
    for(const target of targets){const dir=path.join(target,'surefire-reports');if(!fs.existsSync(dir))continue;
      receipt.nativeSuites[path.basename(path.dirname(target))]=fs.readdirSync(dir).filter(n=>/^TEST-.*\.xml$/.test(n)&&!n.includes('PostgresLayeredAggregationIT'))
        .map(name=>({name,...counts(fs.readFileSync(path.join(dir,name),'utf8'))}));}
    const suites=Object.values(receipt.nativeSuites).flat();assert(suites.every(x=>x.failures===0&&x.errors===0));
    receipt.nativeTestsPassed=suites.reduce((n,s)=>n+s.tests-s.skipped,0);assert(receipt.nativeTestsPassed>=1118);
    for(const[file,expected]of Object.entries(receipt.sourceHashes))assert.equal(hash(fs.readFileSync(path.join(repo,file))),expected,'Source changed: '+file);
    receipt.sameDatabase=true;receipt.passed=true;
  }catch(error){receipt.error=secret?String(error.message).split(secret).join('[REDACTED]'):error.message;process.exitCode=1;}
  finally{
    if(runner.safeToClean){receipt.cleanup=[];for(const target of targets)if(fs.existsSync(target)){receipt.cleanup.push({path:target,bytes:ownedBytes(target)});fs.rmSync(target,{recursive:true});}
      if(ownsLock)fs.unlinkSync(lock);
    }else receipt.unsafeRetention={targets,processGroup:runner.unsafeGroup,lock:ownsLock?lock:undefined};
    runner.dispose();fs.writeFileSync(path.join(output,'receipt.json'),JSON.stringify(receipt,null,2)+'\n');
    process.stdout.write(JSON.stringify({passed:receipt.passed,evidence:output,error:receipt.error?.slice(-1000),rounds:receipt.rounds?.map(x=>x.junit),
      nativeTestsPassed:receipt.nativeTestsPassed,removedBytes:receipt.cleanup?.reduce((n,x)=>n+x.bytes,0)})+'\n');
  }
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
