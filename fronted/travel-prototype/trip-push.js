/* Push is a wake-up hint. HTTP polling remains the durable catch-up path. */
(function(root){
 function create({Socket,url,notify,schedule=setTimeout,cancel=clearTimeout}){
  let socket=null,target=null,timer=null,generation=0,attempt=0,watchdog=null;
  function stop(){generation++;target=null;if(timer)cancel(timer);if(watchdog)cancel(watchdog);timer=null;watchdog=null;const old=socket;socket=null;if(old)old.close();}
  function connect(){
   if(!target)return;const current=generation,desired=target;
   let ws;
   try{ws=new Socket(url);}catch{timer=schedule(connect,30000);return;}
   socket=ws;
   function alive(){if(watchdog)cancel(watchdog);watchdog=schedule(()=>{if(current===generation)ws.close();},45000);}
   ws.onopen=()=>{if(current===generation){ws.send(JSON.stringify({token:desired.token,tripId:desired.id}));alive();}};
   ws.onmessage=event=>{if(current!==generation)return;alive();try{const data=JSON.parse(event.data);if(data.type==='ready'){attempt=0;notify();}else if(data.type==='changed')notify();}catch{ws.close();}};
   ws.onclose=()=>{if(current!==generation)return;socket=null;if(watchdog)cancel(watchdog);watchdog=null;timer=schedule(connect,Math.min(30000,1000*2**Math.min(attempt++,5))+Math.floor(Math.random()*500));};
   ws.onerror=()=>ws.close();
  }
  function start(id,token){if(target&&target.id===id&&target.token===token)return;stop();if(!id||!token)return;target={id,token};attempt=0;connect();}
  return {start,stop};
 }
 root.tripPush={create};if(typeof module!=='undefined')module.exports={create};
})(typeof window==='undefined'?globalThis:window);
