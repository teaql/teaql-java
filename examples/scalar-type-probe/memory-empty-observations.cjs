'use strict';
const assert=require('node:assert/strict'),vectors=require('./empty-vectors.json');
function observation(r,round){assert.equal(r.schema,'teaql.java-generated-memory-empty-aggregate/v1');assert.equal(r.passed,true);assert.equal(r.round,round);assert.equal(r.rows.length,18);assert.equal(r.scalarIds.length,4);assert.equal(r.bucketIds.length,4);assert.equal(new Set([...r.scalarIds,...r.bucketIds]).size,8);let position=0;
 const next=(id,logging)=>{const row=r.rows[position++];assert.equal(row.case,id);assert.equal(row.logging,logging);assert(!Object.hasOwn(row,'sql'));assert.equal(row.provider.length,1);assert(row.provider[0].queryCalls>0);assert.equal(row.provider[0].mutationCalls,0);assert.equal(row.provider[0].sqlCalls,0);return row;};
 for(const logging of [true,false]){for(const vector of vectors.scalarCases){const row=next(vector.id,logging);assert.deepEqual(row.values,{total:vector.sum,rows:vector.countStar,countValue:vector.countValue});}
  const empty=next('EA-05',logging);assert.deepEqual(empty.values,{count:0,sum:null});assert(Object.hasOwn(empty.entityJSON,'_total'));assert.equal(empty.entityJSON._total,null);assert.equal(empty.entityJSON._rows,0);
  for(const includeAll of [true,false]){const row=next('EA-06',logging);assert.equal(row.includeAll,includeAll);assert.equal(row.visibleRows,1);assert.deepEqual(row.values,includeAll?vectors.bucketCases[1].expected:vectors.bucketCases[1].expected.filter(v=>v.count>0));}
  assert.deepEqual(next('EA-07',logging).values,{raw:null,fallback:0,rawRemains:null});assert.deepEqual(next('EA-08',logging).groups,[]);
 }return{round,cases:18,emptyVectors:8,loggingModes:2,selectedNullEntityJSON:true,sqlCalls:0};}
module.exports={observation};
