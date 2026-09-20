const {test}=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
function setup(fetch, storage={getItem:()=>null,setItem(){},removeItem(){}}){
 const context={window:{},sessionStorage:storage,fetch,FormData,AbortSignal,URL,location:{origin:'http://localhost'}};
 vm.runInNewContext(fs.readFileSync(__dirname+'/api.js','utf8'),context);
 return context.window.ledgerApi;
}
const response=(data,code=0)=>({ok:true,status:200,headers:new Headers({'content-type':'application/json'}),json:async()=>({code,data})});
test('explicit request key is sent unchanged',async()=>{
 let observed;
 const api=setup(async(path,options)=>{observed=options;return response({billId:42});});
 await api.request('/api/v1/books/1/bills/personal-bill',{method:'POST',body:{amount:1},idempotencyKey:'stable-request-key'});
 assert.equal(observed.headers['Idempotency-Key'],'stable-request-key');
});
test('token refresh preserves request key and body',async()=>{
 const calls=[];
 const api=setup(async(path,options)=>{calls.push({path,options});return calls.length===1?response(null,4002):path.endsWith('refresh-token')?response({token:'new'}):response({billId:42});});
 api.setSession({token:'old',refreshToken:'refresh'});
 await api.request('/api/v1/books/1/bills/personal-bill',{method:'POST',body:{amount:1},idempotencyKey:'stable-request-key'});
 assert.equal(calls[0].options.headers['Idempotency-Key'],calls[2].options.headers['Idempotency-Key']);
 assert.equal(calls[0].options.body,calls[2].options.body);
 assert.equal(calls[2].options.headers.Authorization,'Bearer new');
});

const pending=()=>({path:'/api/v1/books/1/bills/personal-bill',body:{amount:123},key:'stable-request-123'});
test('parameter rejection releases pending submission for editing',async()=>{
 const api=setup(async()=>response(null,4001));const holder={};
 await assert.rejects(api.submitBill(holder,pending()),/修改后重新提交/);
 assert.equal(holder.pendingBill,null);
});
test('network failure retains original submission and retry clears only on success',async()=>{
 const calls=[];const api=setup(async(path,options)=>{
  calls.push(options);if(calls.length===1)throw new TypeError('network lost');return response({billId:42});
 });const holder={};const draft=pending();
 await assert.rejects(api.submitBill(holder,draft),/结果未确认/);
 assert.equal(holder.pendingBill,draft);
 await api.submitBill(holder);
 assert.equal(holder.pendingBill,null);
 assert.equal(calls[0].body,calls[1].body);
 assert.equal(calls[0].headers['Idempotency-Key'],calls[1].headers['Idempotency-Key']);
});
for(const code of [4002,4003,4007,5000])test(`error ${code} does not discard uncertain operation`,async()=>{
 const api=setup(async()=>response(null,code));const holder={};const draft=pending();
 await assert.rejects(api.submitBill(holder,draft));assert.equal(holder.pendingBill,draft);
});
test('parameter rejection during retry also unlocks form',async()=>{
 let count=0;const api=setup(async()=>{if(!count++)throw new TypeError('lost');return response(null,4001);});
 const holder={};await assert.rejects(api.submitBill(holder,pending()));
 await assert.rejects(api.submitBill(holder),/修改后重新提交/);assert.equal(holder.pendingBill,null);
});

function storage(){const map=new Map();return {getItem:k=>map.get(k)||null,setItem:(k,v)=>map.set(k,v),removeItem:k=>map.delete(k)};}
const owned=()=>({...pending(),ownerId:1,bookId:1});
test('reload recovers original request and clears durable record after success',async()=>{
 const disk=storage();const first=setup(async()=>{throw new TypeError('lost');},disk);
 await assert.rejects(first.submitBill({},owned()));
 let sent;const reloaded=setup(async(path,options)=>{sent=options;return response({billId:42});},disk);
 const recovered=reloaded.recoverBill(1,1);assert.equal(recovered.key,owned().key);
 await reloaded.submitBill({},recovered);assert.equal(sent.headers['Idempotency-Key'],owned().key);
 assert.equal(reloaded.recoverBill(1,1),null);
});
test('pending requests are isolated by user and book',async()=>{
 const api=setup(async()=>{throw new Error('offline');},storage());
 await assert.rejects(api.submitBill({},owned()));
 assert.equal(api.recoverBill(2,1),null);assert.equal(api.recoverBill(1,2),null);
 assert.ok(api.recoverBill(1,1));
});
test('storage quota failure prevents network submission',async()=>{
 let calls=0;const disk=storage();disk.setItem=()=>{throw new Error('quota');};
 const api=setup(async()=>{calls++;return response({});},disk);
 await assert.rejects(api.submitBill({},owned()),/quota/);assert.equal(calls,0);
});
test('new request cannot overwrite unresolved submission',async()=>{
 let calls=0;const api=setup(async()=>{calls++;throw new Error('offline');},storage());
 await assert.rejects(api.submitBill({},owned()));
 await assert.rejects(api.submitBill({},{...owned(),key:'another-request-123'}),/先处理/);
 assert.equal(calls,1);assert.equal(api.recoverBill(1,1).key,owned().key);
});
test('explicit validation rejection clears persisted state',async()=>{
 const api=setup(async()=>response(null,4001),storage());
 await assert.rejects(api.submitBill({},owned()));assert.equal(api.recoverBill(1,1),null);
});
test('corrupted stored destination is never accepted for replay',()=>{
 const disk=storage();disk.setItem('trip-ledger-pending-v1:1:1',JSON.stringify({...owned(),path:'/api/v1/books/2/bills/personal-bill'}));
 const api=setup(async()=>{throw new Error('unexpected');},disk);
 assert.throws(()=>api.recoverBill(1,1),/无效/);
});
