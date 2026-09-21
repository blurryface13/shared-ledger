/* Polling change feed: cursor advances only after a successful response. */
(function(root){
 function create(api){
  let tripId=null,version=0,cursor=0,events=[],pending=false,epoch=0,inflight=false,message='';
  function reset(id=null,v=0){tripId=id;version=v;cursor=0;events=[];pending=false;message='';epoch++;inflight=false;}
  function applied(v){version=v;pending=false;epoch++;inflight=false;}
  async function poll(){
   if(tripId===null||inflight)return;
   const generation=epoch,id=tripId;inflight=true;
   try{
    const rows=await api.get('/api/v1/trips/'+id+'/changes?after='+cursor);
    if(generation!==epoch)return;
    const fresh=rows.filter(x=>Number(x.id)>cursor);
    if(fresh.some(x=>Number(x.version)>version)|| (cursor>0&&fresh.some(x=>x.action!=='UPDATED'&&x.action!=='CREATED')))pending=true;
    if(fresh.length){cursor=Math.max(cursor,...fresh.map(x=>Number(x.id)));events=[...events,...fresh].slice(-200);}
    message='';
   }catch(ex){if(generation===epoch)message=[4003,4004].includes(ex.code)?'行程访问权限已变化，请返回行程列表。':'暂时无法检查更新，恢复连接后会继续。';}
   finally{if(generation===epoch)inflight=false;}
  }
  return {reset,applied,poll,get pending(){return pending;},get message(){return message;},get events(){return events;},get cursor(){return cursor;},get id(){return tripId;}};
 }
 root.tripSync={create};if(typeof module!=='undefined')module.exports={create};
})(typeof window==='undefined'?globalThis:window);
