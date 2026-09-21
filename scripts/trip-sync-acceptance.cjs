/* Opt-in local acceptance: creates a dedicated trip; archives it afterwards. */
const assert=require('node:assert/strict');
const {create}=require('../fronted/travel-prototype/trip-sync.js');
const base=process.env.TRIP_SYNC_BASE||'http://127.0.0.1:4178';
if(!['127.0.0.1','localhost'].includes(new URL(base).hostname))throw Error('Local test service required');
const ownerCode=process.env.TRIP_SYNC_OWNER_CODE,memberCode=process.env.TRIP_SYNC_MEMBER_CODE;
if(!ownerCode||!memberCode)throw Error('Set TRIP_SYNC_OWNER_CODE and TRIP_SYNC_MEMBER_CODE to dedicated local test accounts');
async function request(path,token,method='GET',body){
 const response=await fetch(base+path,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(15000)});
 const result=await response.json();if(result.code!==0){const e=Error(result.message);e.code=result.code;throw e;}return result.data;
}
(async()=>{
 const owner=(await request('/api/v1/auth/wechat-login',null,'POST',{code:ownerCode})).token;
 const member=(await request('/api/v1/auth/wechat-login',null,'POST',{code:memberCode})).token;
 assert.ok(owner&&member,'Test accounts must be bound');
 const identity=await request('/api/v1/users/me',member);
 let trip;
 try{
  trip=await request('/api/v1/trips',owner,'POST',{name:'同步恢复专项验收',destination:'杭州',startDate:'2026-09-26',endDate:'2026-09-27',people:2,budgetCent:10000,pace:'balanced',style:'culture',stay:'metro',activities:[]});
  const path='/api/v1/trips/'+trip.id;
  await request(path+'/members/'+identity.userId,owner,'PUT',{role:'EDITOR'});
  let offline=false;
  const sync=create({get:p=>offline?Promise.reject(Error('Injected disconnected transport')):request(p,member)});
  const stale=await request(path,member);sync.reset(trip.id,stale.version);await sync.poll();
  const baseline=sync.cursor;offline=true;
  const updated=await request(path,owner,'PUT',{...trip,name:'同步恢复专项验收 · 更新'});
  await sync.poll();assert.equal(sync.cursor,baseline);assert.ok(sync.message);
  await assert.rejects(request(path,member,'PUT',{...stale,name:'过期草稿'}),e=>e.code===4007);
  offline=false;await sync.poll();assert.equal(sync.pending,true);assert.ok(sync.cursor>baseline);
  const latest=await request(path,member);assert.equal(latest.version,updated.version);assert.equal(latest.name,updated.name);
  sync.applied(latest.version);await sync.poll();assert.equal(sync.pending,false);
  await request(path+'/members/'+identity.userId,owner,'DELETE');await sync.poll();assert.match(sync.message,/权限/);
  console.log(JSON.stringify({tripId:trip.id,checks:['disconnected cursor retained','stale write rejected','reconnect catches update','latest snapshot applied','revoked member denied'],transport:'real HTTP via local proxy; disconnect injected in client adapter',passed:5},null,2));
 }finally{
  if(trip){const path='/api/v1/trips/'+trip.id;await request(path+'/members/'+identity.userId,owner,'DELETE');const latest=await request(path,owner);await request(path,owner,'PUT',{...latest,archived:true});}
 }
})().catch(e=>{console.error(e.message);process.exitCode=1;});
