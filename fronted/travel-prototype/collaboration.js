/* Shared trip UI state. Invitation secrets live in memory only. */
(function(root) {
  function create(api) {
    let tripId=null, members=[], issued=null, opened=false, invitations=[], more=false;
    const path=()=>'/api/v1/trips/'+tripId;
    const role=user=>members.find(m=>String(m.user_id)===String(user))?.role;
    async function load(id) {
      const result=await api.get('/api/v1/trips/'+id+'/members');
      if(String(id)!==String(tripId)){issued=null;opened=false;invitations=[];more=false;}
      tripId=id;members=result;
    }
    function reset(){tripId=null;members=[];issued=null;opened=false;invitations=[];more=false;}
    async function invite(target,access){
      if(!/^[1-9][0-9]*$/.test(String(target)) || !Number.isSafeInteger(Number(target)))throw Error('请输入有效的成员账号 ID');
      issued=await api.post(path()+'/invitations',{targetUserId:Number(target),role:access});
      return issued;
    }
    async function accept(token){
      token=token.trim();if(!/^[A-Za-z0-9_-]{43}$/.test(token))throw Error('邀请码应为 43 位，请检查是否复制完整');
      return api.post('/api/v1/trips/invitations/accept',{token});
    }
    async function history(append=false){
      const before=append&&invitations.length?invitations[invitations.length-1].id:0;
      const rows=await api.get(path()+'/invitations?before='+before);
      invitations=append?[...invitations,...rows]:rows;more=rows.length===50;
    }
    async function revoke(id=issued?.id){if(id){await api.delete(path()+'/invitations/'+id);if(String(issued?.id)===String(id))issued=null;}}

    async function change(target,access){await api.put(path()+'/members/'+target,{role:access});await load(tripId);}
    async function remove(target){await api.delete(path()+'/members/'+target);await load(tripId);}
    return {history,get invitations(){return invitations;},get more(){return more;},load,reset,invite,accept,revoke,change,remove,role,
      toggle(){opened=!opened;},get opened(){return opened;},get members(){return members;},get issued(){return issued;}};
  }
  root.tripCollaboration={create};
  if(typeof module!=='undefined')module.exports={create};
})(typeof window==='undefined'?globalThis:window);
