/* Opt-in local acceptance: creates a dedicated trip; archives it afterwards. */
const assert=require('node:assert/strict');

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
  const wsUrl=new URL('/ws/trips',base);wsUrl.protocol='ws:';wsUrl.port='8081';
  function connect(token){
    const ws=new WebSocket(wsUrl),messages=[],waiters=[];let closed=null;
    ws.addEventListener('open',()=>ws.send(JSON.stringify({token,tripId:trip.id})));
    ws.addEventListener('message',event=>{const type=JSON.parse(event.data).type;messages.push(type);for(const wake of waiters)wake();});
    ws.addEventListener('close',event=>{closed=event.code;for(const wake of waiters)wake();});
    function wait(predicate){return new Promise((resolve,reject)=>{const timer=setTimeout(()=>{cleanup();reject(Error('Socket acceptance timeout'));},10000);function cleanup(){clearTimeout(timer);const i=waiters.indexOf(check);if(i>=0)waiters.splice(i,1);}function check(){if(predicate()){cleanup();resolve();}}waiters.push(check);check();});}
    return {ws,messages,ready:()=>wait(()=>messages.includes('ready')),changed:()=>wait(()=>messages.includes('changed')),closed:()=>wait(()=>closed!==null),code:()=>closed};
  }
  const invalid=connect('invalid');await invalid.closed();assert.equal(invalid.code(),1008);
  const client=connect(member);
  try{
    await client.ready();
    // Drain no application writes before this point; this update must produce a committed hint.
    const updated=await request(path,owner,'PUT',{...trip,name:'WebSocket 专项验收'});
    await client.changed();
    const latest=await request(path,member);assert.equal(latest.version,updated.version);
    await request(path+'/members/'+identity.userId,owner,'DELETE');
    await client.closed();assert.equal(client.code(),1008);
    console.log(JSON.stringify({tripId:trip.id,checks:['invalid token rejected','authenticated subscription ready','committed update pushes hint','authorized HTTP returns latest snapshot','revocation closes live socket'],transport:'real WebSocket and HTTP; single backend instance',passed:5},null,2));
  }finally{client.ws.close();}

 }finally{
  if(trip){const path='/api/v1/trips/'+trip.id;await request(path+'/members/'+identity.userId,owner,'DELETE');const latest=await request(path,owner);await request(path,owner,'PUT',{...latest,archived:true});}
 }
})().catch(e=>{console.error(e.message);process.exitCode=1;});
