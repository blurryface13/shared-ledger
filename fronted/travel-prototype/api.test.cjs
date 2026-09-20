const {test}=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
function setup(fetch){
 const context={window:{},sessionStorage:{getItem:()=>null,setItem(){},removeItem(){}},fetch,FormData,AbortSignal,URL,location:{origin:'http://localhost'}};
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
