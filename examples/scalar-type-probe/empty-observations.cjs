'use strict';
const assert=require('node:assert/strict'),vectors=require('./empty-vectors.json');
const purpose='verify generated empty aggregate contract';
function observation(r,round){
 assert.equal(r.schema,'teaql.java-generated-empty-aggregate/v1');assert.equal(r.passed,true);assert.equal(r.round,round);
 assert.equal(r.rows.length,22);assert.equal(r.scalarIds.length,4);assert.equal(r.bucketIds.length,4);
 const ids=[...r.scalarIds,...r.bucketIds];assert.equal(new Set(ids).size,8);for(const id of ids)assert(Number.isSafeInteger(id)&&id>0);
 let position=0,total=0;
 function next(id,logging,count,outcome='success'){
  const row=r.rows[position++];assert.equal(row.case,id);assert.equal(row.logging,logging);assert.equal(row.sql.length,count);total+=count;
  for(const sql of row.sql){assert.equal(sql.sql,sql.parameterizedSQL);assert.equal(sql.outcome,outcome);assert.equal(sql.purpose,purpose);
   assert(sql.comment?.trim());assert(sql.sql.startsWith('SELECT '));
   assert.equal(sql.traceChain[0].kind,'PURPOSE');assert.equal(sql.traceChain[0].comment,purpose);
   assert.equal(sql.traceChain[1].kind,'COMMENT');assert(sql.traceChain[1].comment);
   assert.equal(sql.tracePath[0].kind,'OPERATION');assert.equal(sql.tracePath[0].comment,'query');
   assert.equal(sql.tracePath[1].kind,'REQUEST');
   assert.deepEqual(sql.tracePath.slice(-2).map(n=>[n.kind,n.name]),[['PROVIDER','sqlite'],['SQL','select']]);
  }
  return row;
 }
 for(const logging of [true,false]){
  for(const vector of vectors.scalarCases){const row=next(vector.id,logging,2);
   assert.deepEqual(row.values,{total:vector.sum,rows:vector.countStar,countValue:vector.countValue});
   assert.match(row.sql[0].sql,/sum\(optional_integer\).*count\(id\)/i);assert.match(row.sql[1].sql,/optional_integer IS NOT NULL/i);
  }
  const empty=next('EA-05',logging,3);assert.deepEqual(empty.values,{count:0,sum:null});
  for(const [index,operation,comment]of [[1,'countScalarTypeProbesWith("rows")','count aggregate contributions'],
    [2,'sumOptionalIntegerOfScalarTypeProbesAs("total")','sum aggregate contributions']]){
   assert.deepEqual(empty.sql[index].traceChain.slice(2).map(n=>n.comment),[operation,comment]);
  }
  for(const includeAll of [true,false]){
   const facet=next('EA-06',logging,9);assert.equal(facet.includeAll,includeAll);assert.equal(facet.visibleRows,1);
   const expected=vectors.bucketCases[1].expected;assert.deepEqual(facet.values,includeAll?expected:expected.filter(v=>v.count>0));
   for(const sql of facet.sql.slice(2))assert.equal(sql.traceChain[2].comment,'facetByTypePlatformAs("buckets")');
   assert(facet.sql.some(s=>s.traceChain.some(n=>n.comment==='sum facet contributions')));
   assert(facet.sql.some(s=>s.traceChain.some(n=>n.comment==='count facet contributions')));
  }
  const fallback=next('EA-07',logging,1);assert.deepEqual(fallback.values,{raw:null,fallback:0,rawRemains:null});
  const grouped=next('EA-08',logging,1);assert.deepEqual(grouped.groups,[]);assert.match(grouped.sql[0].sql,/GROUP BY/i);
  const failure=next('REAL-SQLITE-FAILURE',logging,1,'failure');assert.equal(failure.sql[0].comment,'inspect actual SQLite aggregate failure');
  const recovery=next('QUERY-RECOVERY',logging,1);assert.equal(recovery.sql[0].comment,'load independent detail after SQLite failure');
  assert.equal(recovery.sql[0].traceChain[1].comment,'load independent detail after SQLite failure');
 }
 assert.equal(total,66);return {round,cases:22,sqlDriverAttempts:total,loggingModes:2,emptyVectors:8,realSQLiteErrors:2,independentRecoveries:2};
}
module.exports={observation};
