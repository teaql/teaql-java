'use strict';
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
function currentLibraryInputs(){const root=path.join(__dirname,"java-lib-core"),meta=path.join(root,'current-generator-fixture.json');
const m=JSON.parse(fs.readFileSync(meta));assert.equal(m.evaluationErrors,0);assert.equal(sha(fs.readFileSync(path.join(__dirname,'model.xml'))),m.modelSha256);
const actual=[];function walk(dir){for(const e of fs.readdirSync(dir,{withFileTypes:true})){assert(!e.isSymbolicLink());const p=path.join(dir,e.name);if(e.isDirectory())walk(p);else{assert(e.isFile());actual.push(path.relative(root,p));}}}walk(root);
assert.deepEqual(actual.sort(),[...Object.keys(m.files),'current-generator-fixture.json'].sort());
for(const[n,h]of Object.entries(m.files)){assert(!path.isAbsolute(n)&&!n.split('/').includes('..'));assert.equal(sha(fs.readFileSync(path.join(root,n))),h,n);}
return{files:m.files,modelSha256:m.modelSha256,metadataSha256:sha(fs.readFileSync(meta))};}
module.exports={currentLibraryInputs};
