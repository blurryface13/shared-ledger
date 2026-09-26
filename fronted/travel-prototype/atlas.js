/* Independent map workspace. Public provider reads never persist schedule changes. */
window.tripAtlas=(()=>{
  const T=window.atlasTools, esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const paths={back:'m14 6-6 6 6 6',pin:'M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z M14 10a2 2 0 1 1-4 0 2 2 0 0 1 4 0',fit:'M8 3H3v5m13-5h5v5M3 16v5h5m13-5v5h-5M8 12h8m-4-4v8',search:'m16 16 5 5 M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',route:'M5 5h9a4 4 0 0 1 0 8H9a4 4 0 0 0 0 8h10 M3 3v4m-2-2h4',sights:'m3 20 6-13 4 8 3-5 5 10H3',stays:'M3 20V7m18 13V7M3 16h18M5 7h14v9M7 7V4h10v3M6 11h4m4 0h4',food:'M5 3v7m3-7v7M3 3v5a4 4 0 0 0 8 0V3M7 12v9M18 3c-4 4-4 9 1 9V3v18',plus:'M12 5v14M5 12h14',sun:'M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1 1m12 12 1 1M5 19l1-1M18 6l1-1M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0'};
  const icon=k=>`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${paths[k]||paths.pin}"/></svg>`;
  const btn=(action,label,cls='',attrs='')=>`<button type="button" data-atlas="${action}" class="${cls}" ${attrs}>${label}</button>`;
  const categories={sights:'景点',stays:'住宿',food:'餐饮'};
  const weatherCache=new Map();
  function create({api,onEdit,onAdd,onLink,onClose}){
    const host=document.createElement('dialog');host.className='atlas-dialog';host.setAttribute('aria-labelledby','atlas-title');document.body.append(host);
    let context,trip,day=0,map,tiles,markers,road,radius,selected=null,origin=null,nearby=[],results=[],category='sights',mode='planned',message='',request=0,routeRequest=0,weatherRequest=0,weatherAbort,searching=false,routing=false,routeSummary='',weather='',tileErrors=0,cityCenter=null,centerRequest=0;
    const reduced=()=>matchMedia('(prefers-reduced-motion: reduce)').matches;
    const node=s=>host.querySelector(s),planned=()=>T.ordered(trip.activities,day),all=()=>{const scheduled=planned().map((a,i)=>({...a,key:'a:'+a.id,name:a.title,number:i+1,planned:true}));const extra=mode==='nearby'?[...(origin&&!scheduled.some(p=>p.key===origin.key)?[origin]:[]),...nearby]:mode==='search'?results:[];return [...scheduled,...extra];};
    const current=()=>all().find(a=>a.key===selected),writable=()=>context.writable&&!trip.archived;
    const status=()=>{node('.atlas-message').textContent=message;node('.atlas-message').hidden=!message;};
    function invalidate(){request++;routeRequest++;weatherRequest++;centerRequest++;weatherAbort?.abort();searching=false;routing=false;}
    function close(){invalidate();map?.remove();map=null;host.close();onClose?.(day);}
    host.addEventListener('cancel',e=>{e.preventDefault();close();});
    function clearRoad(){routeRequest++;routing=false;routeSummary='';road?.remove();road=null;node('.atlas-route-label').textContent='查询步行路线';}
    function open(value){
      if(host.open)close();context=value;trip=value.trip;day=value.day??0;selected=null;origin=null;nearby=[];results=[];mode='planned';message='';weather='';category='sights';routeSummary='';tileErrors=0;cityCenter=null;
      host.innerHTML=`<header class="atlas-head">${btn('close',icon('back'),'atlas-icon','aria-label="返回行程"')}<div><span class="atlas-eyebrow">EXPLORE YOUR TRIP</span><h2 id="atlas-title">${esc(trip.destination)}<span>地图全知视图</span></h2></div><span class="atlas-total">${trip.activities.length} 个安排</span></header>
        <nav class="atlas-days" aria-label="地图日期">${btn('day:-1','全程',day===-1?'is-active':'',`aria-pressed="${day===-1}"`)}${Array.from({length:Math.round((Date.parse(trip.endDate)-Date.parse(trip.startDate))/86400000)+1},(_,i)=>btn('day:'+i,`<small>DAY ${String(i+1).padStart(2,'0')}</small>${T.dateAt(trip.startDate,i).slice(5).replace('-',' / ')}`,day===i?'is-active':'',`aria-pressed="${day===i}"`)).join('')}</nav>
        <div class="atlas-body"><section class="atlas-map-wrap" aria-label="地点地图"><div class="atlas-canvas" role="region" aria-label="可拖动地图，使用加减按钮缩放或从地点列表选择" tabindex="0"></div><div class="atlas-map-top"><span class="atlas-map-caption">${icon('pin')}<span>行程 · 周边</span></span><span class="atlas-weather" role="status"></span></div><div class="atlas-map-empty" hidden><strong>地点还没有位置</strong><span>搜索并确认地点后，就能看清景点、住宿和周边的关系。</span>${btn('focus-search','搜索地点 ↗')}</div><div class="atlas-map-tools">${btn('fit',icon('fit'),'atlas-icon','aria-label="显示全部地点"')}${btn('zoom-in','＋','atlas-icon','aria-label="放大地图"')}${btn('zoom-out','−','atlas-icon','aria-label="缩小地图"')}</div><div class="atlas-tile-error" role="status" hidden>底图暂未载入 ${btn('tiles','重试')}</div><div class="atlas-legend"><span><i></i>已安排</span><span><i class="stays"></i>住宿</span><span><i class="sights"></i>景点</span><span><i class="food"></i>餐饮</span></div></section>
        <aside class="atlas-panel" aria-label="地图地点"><div class="atlas-panel-top"><div class="atlas-panel-title"><h3>地点与周边</h3>${btn('expand','展开列表','atlas-expand','aria-expanded="false"')}</div><form class="atlas-search" role="search">${icon('search')}<input name="keyword" placeholder="搜索景点、酒店或餐厅" aria-label="搜索地点" maxlength="100" required autocomplete="off">${btn('search','搜索')}</form><nav class="atlas-tabs" aria-label="地点来源">${btn('planned','已安排','is-active')}${btn('nearby','找周边')}</nav><div class="atlas-categories" hidden>${Object.entries(categories).map(([k,v])=>btn('category:'+k,icon(k)+v,k===category?'is-active':'',`aria-pressed="${k===category}"`)).join('')}</div><p class="atlas-origin" hidden></p></div><div class="atlas-scroll"><p class="atlas-message" role="status" hidden></p><section class="atlas-selection" aria-label="选中地点" hidden></section><div class="atlas-list" aria-label="地点列表"></div></div><footer class="atlas-panel-foot">${btn('route',icon('route')+'<span class="atlas-route-label">查询步行路线</span>','atlas-route')}<small class="atlas-route-note">按当天时间顺序，沿真实道路计算</small></footer></aside></div>`;
      host.showModal();
      if(!window.L){message='地图库未加载，请刷新页面后重试。';status();paint();return;}
      map=L.map(node('.atlas-canvas'),{zoomControl:false,attributionControl:true,scrollWheelZoom:true,zoomAnimation:!reduced()}).setView([30.25,120.15],11);
      map.attributionControl.setPrefix('<a href="https://leafletjs.com">Leaflet</a>');
      const config=window.tripMapConfig||{};
      tiles=L.tileLayer(config.tileUrl||'https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:config.attribution||'© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'}).addTo(map);
      tiles.on('tileerror',()=>{if(++tileErrors>=2)node('.atlas-tile-error').hidden=false;});
      tiles.on('tileload',()=>{tileErrors=0;node('.atlas-tile-error').hidden=true;});
      markers=L.layerGroup().addTo(map);map.on('zoomend',draw);selected=all().find(T.valid)?.key||all()[0]?.key||null;paint();fit();loadWeather();if(!all().some(T.valid))loadCityCenter();
    }
    const panelHeight=()=>matchMedia('(max-width:720px)').matches?node('.atlas-panel').offsetHeight:0;
    const viewportPoint=latlng=>panelHeight()?map.unproject(map.project(latlng,map.getZoom()).add([0,panelHeight()/2]),map.getZoom()):latlng;
    function fit(){if(!map)return;const points=all().filter(T.valid).map(p=>T.toWgs(p.longitude,p.latitude,p.coordinateSystem));if(points.length)map.fitBounds(points,{paddingTopLeft:[42,42],paddingBottomRight:[42,42+panelHeight()],maxZoom:15,animate:!reduced()});else if(cityCenter){map.setView(T.toWgs(cityCenter.longitude,cityCenter.latitude,cityCenter.coordinateSystem),12,{animate:!reduced()});if(panelHeight())map.panTo(viewportPoint(map.getCenter()),{animate:!reduced()});}else map.setView([35,105],4,{animate:false});}
    async function loadCityCenter(){const ticket=++centerRequest;try{const c=await api.get(`/api/v1/trips/${trip.id}/center`);if(ticket===centerRequest&&host.open&&T.valid(c)){cityCenter=c;if(!all().some(T.valid))fit();}}catch{if(ticket===centerRequest&&host.open){message='搜索地点并确认位置后，已安排的地点会显示在地图上。';status();}}}
    function draw(){
      if(!markers)return;markers.clearLayers();const groups=new Map(),zoom=map.getZoom();
      all().filter(T.valid).forEach(p=>{const latlng=T.toWgs(p.longitude,p.latitude,p.coordinateSystem),xy=map.project(latlng,zoom),key=`${Math.floor(xy.x/64)},${Math.floor(xy.y/64)}`;if(!groups.has(key))groups.set(key,[]);groups.get(key).push(p);});
      groups.forEach(group=>{const p=group.find(p=>p.key===selected)||group[0],active=group.some(p=>p.key===selected),kind=group.length>1?'cluster':p.planned?'planned':p.category||'sights';
        const positions=group.map(x=>T.toWgs(x.longitude,x.latitude,x.coordinateSystem));const center=[positions.reduce((v,x)=>v+x[0],0)/positions.length,positions.reduce((v,x)=>v+x[1],0)/positions.length];
        const marker=L.marker(center,{keyboard:true,title:group.map(x=>x.name).slice(0,4).join('、'),alt:group.length>1?`${group.length} 个相近地点，点击放大`:p.name,zIndexOffset:active?1000:p.planned?200:0,icon:L.divIcon({className:'atlas-marker-wrap',html:`<span class="atlas-marker ${kind} ${active?'is-selected':''}">${group.length>1?group.length:p.planned?p.number:icon(kind)}</span>`,iconSize:[44,44],iconAnchor:[22,22]})}).addTo(markers);
        marker.on('click',()=>{if(group.length>1&&zoom<18){map.setView(center,Math.min(18,zoom+2),{animate:!reduced()});return;}const index=group.findIndex(p=>p.key===selected);select(group[(index+1)%group.length].key);});
      });
      radius?.remove();radius=null;if(mode==='nearby'&&T.valid(origin))radius=L.circle(T.toWgs(origin.longitude,origin.latitude,origin.coordinateSystem),{radius:3000,color:'#748b83',weight:1,dashArray:'5 7',fillOpacity:.025,interactive:false}).addTo(map);
    }
    function select(key){selected=key;const p=current();paint();if(map&&T.valid(p))map.panTo(viewportPoint(T.toWgs(p.longitude,p.latitude,p.coordinateSystem)),{animate:!reduced()});node('.atlas-scroll').scrollTop=0;loadWeather();}
    function paint(){
      const items=all(),p=current();
      node('.atlas-tabs').innerHTML=btn('planned',`已安排 <small>${planned().length}</small>`,mode==='planned'?'is-active':'',`aria-pressed="${mode==='planned'}"`)+btn('nearby','找周边',mode==='nearby'?'is-active':'',`aria-pressed="${mode==='nearby'}"`)+(mode==='search'?'<span class="atlas-search-label">搜索结果</span>':'');
      node('.atlas-categories').hidden=mode!=='nearby';node('.atlas-origin').hidden=mode!=='nearby';node('.atlas-origin').textContent=origin?`以 ${origin.name} 为中心 · 3 km 内`:'先选一个有坐标的地点，再找周边';
      host.querySelectorAll('[data-atlas^="category:"]').forEach(b=>{const active=b.dataset.atlas==='category:'+category;b.classList.toggle('is-active',active);b.setAttribute('aria-pressed',active);});
      const list=mode==='planned'?items.filter(p=>p.planned):mode==='nearby'?nearby:results;
      node('.atlas-list').innerHTML=searching?'<div class="atlas-empty"><span class="atlas-loading"></span><strong>正在查询地点</strong><p>保留已安排的地点，稍等片刻。</p></div>':list.length?list.map(p=>btn('select:'+p.key,`<span class="atlas-list-mark ${p.planned?'planned':p.category||'sights'}">${p.planned?String(p.number).padStart(2,'0'):icon(p.category||'pin')}</span><span><strong>${esc(p.name)}</strong><small>${p.planned?`第 ${p.day+1} 天 · ${esc(p.time)}${!T.valid(p)?' · 待补位置':''}`:esc(p.address||'暂无详细地址')}</small></span><span class="atlas-list-end">${p.planned?'↗':origin?T.meters(T.distance(origin,p)):'↗'}</span>`, `atlas-place ${selected===p.key?'is-selected':''}`,`aria-pressed="${selected===p.key}"`)).join(''):`<div class="atlas-empty">${icon('pin')}<strong>${mode==='planned'?'这一天还没有安排':mode==='nearby'?'这里暂未找到地点':'没有找到匹配地点'}</strong><p>${mode==='planned'?'搜索一个地点，放进你的行程。':'试试其他类型，或搜索具体名称。'}</p></div>`;
      const selection=node('.atlas-selection');selection.hidden=!p;
      if(p){const d=origin&&p.key!==origin.key?T.distance(origin,p):null,match=!p.planned&&trip.activities.find(a=>a.title.trim()===p.name.trim()&&!T.valid(a)&&!a.locked&&!a.done);selection.innerHTML=`<div class="atlas-selection-meta"><span>${p.planned?'行程安排':categories[p.category]||'搜索地点'}</span><span>${p.planned?esc(p.time):d!=null?'直线 '+T.meters(d):''}</span></div><h3>${esc(p.name)}</h3><p>${esc(p.planned?p.note||'暂无备注':p.address||'暂无详细地址')}</p><div class="atlas-selection-actions">${btn('around',icon('search')+'附近有什么','',T.valid(p)?'':'disabled')}${writable()?btn(p.planned?'edit':match?'link':'add',p.planned?(T.valid(p)?'编辑安排':'补充位置'):match?'关联已有安排':icon('plus')+'加入行程','atlas-primary'):''}</div>`;}
      node('.atlas-map-empty').hidden=all().some(T.valid);node('.atlas-route').disabled=day<0||routing;node('.atlas-route-label').textContent=routing?'正在查询路线':routeSummary||'查询步行路线';node('.atlas-route-note').textContent=day<0?'选择某一天后查看步行路线':mode==='nearby'?'周边距离为直线距离；路线仅含当天安排':'按当天时间顺序，沿真实道路计算';status();draw();
    }
    async function query(around){
      const ticket=++request;searching=true;message='';mode=around?'nearby':'search';if(around)nearby=[];else results=[];paint();
      try{const [aroundLat,aroundLon]=around?T.toWgs(origin.longitude,origin.latitude,origin.coordinateSystem):[null,null];const query=around?`nearby?longitude=${aroundLon}&latitude=${aroundLat}&category=${category}`:`places?keyword=${encodeURIComponent(node('[name=keyword]').value.trim())}`;
        const data=await api.get(`/api/v1/trips/${trip.id}/${query}`);if(ticket!==request||!host.open)return;
        const savedIds=new Set(trip.activities.map(p=>p.poiId).filter(Boolean));const list=data.filter(T.valid).filter(p=>!around||!savedIds.has(p.id)).map(p=>({...p,key:'p:'+p.id,category:around?category:null}));
        if(around)nearby=list;else results=list;selected=list[0]?.key||all().find(p=>p.planned)?.key||null;
      }catch(e){if(ticket!==request||!host.open)return;message=e.message||'地点查询失败，请重试。';}finally{if(ticket===request&&host.open){searching=false;paint();fit();loadWeather();}}
    }
    function around(){const p=current();if(!T.valid(p)){message='先从列表选择一个有坐标的地点。';status();return;}origin={...p};query(true);}
    async function loadRoute(){
      if(day<0||routing)return;clearRoad();const ticket=++routeRequest;routing=true;message='';paint();
      try{const r=await api.get(`/api/v1/trips/${trip.id}/route?day=${day}&mode=walking`);if(ticket!==routeRequest||!host.open)return;if(r.version!==trip.version)throw Error('行程已更新，请关闭地图并读取最新行程后重试。');if(r.coordinateSystem!=='WGS84')throw Error('路线坐标类型暂不支持。');const coords=r.coordinates.map(p=>T.toWgs(p[0],p[1],'WGS84'));if(coords.length<2||coords.some(p=>!p))throw Error('路线坐标不完整，请重试。');road=L.polyline(coords,{color:'#53736c',weight:4,opacity:.9,lineCap:'round'}).addTo(map);road.bringToBack();map.fitBounds(road.getBounds(),{paddingTopLeft:[45,45],paddingBottomRight:[45,45+panelHeight()],animate:!reduced()});routeSummary=`${T.meters(r.distanceMeters)} · 步行约 ${Math.round(r.durationSeconds/60)} 分钟`;
      }catch(e){if(ticket===routeRequest)message=e.message;}finally{if(ticket===routeRequest&&host.open){routing=false;paint();}}
    }
    async function loadWeather(){
      const ticket=++weatherRequest;weatherAbort?.abort();const p=current(),el=node('.atlas-weather');el.textContent='';if(!T.valid(p)||day<0)return;
      const date=T.dateAt(trip.startDate,day),today=new Date().toLocaleDateString('en-CA');if(date<today||Date.parse(date)-Date.parse(today)>15*86400000){el.textContent='预报范围外';return;}
      const [lat,lon]=T.toWgs(p.longitude,p.latitude,p.coordinateSystem),key=`${lat.toFixed(2)},${lon.toFixed(2)}`;el.textContent='天气查询中';
      try{let data=weatherCache.get(key);if(!data||Date.now()-data.at>1800000){weatherAbort=new AbortController();const timeout=setTimeout(()=>weatherAbort?.abort(),8000);let response;try{response=await fetch(`https://api.open-meteo.com/v1/forecast?latitude=${lat.toFixed(2)}&longitude=${lon.toFixed(2)}&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto&forecast_days=16`,{signal:weatherAbort.signal});if(!response.ok)throw Error();data={body:await response.json(),at:Date.now()};}finally{clearTimeout(timeout);}weatherCache.set(key,data);}
        if(ticket!==weatherRequest||!host.open)return;const f=T.forecast(data.body,date);el.textContent=f?`${f.low}–${f.high}°${Number.isFinite(f.rain)?' · 降水 '+f.rain+'%':''}`:'暂无当天预报';el.title='Open-Meteo · '+date;el.setAttribute('aria-label','Open-Meteo '+date+' '+el.textContent);
      }catch{if(ticket===weatherRequest&&host.open)el.textContent='天气暂不可用';}
    }
    host.addEventListener('submit',event=>{event.preventDefault();if(node('.atlas-search').reportValidity())query(false);});
    host.addEventListener('click',event=>{
      const b=event.target.closest('[data-atlas]');if(!b||b.disabled)return;const [action,...parts]=b.dataset.atlas.split(':'),value=parts.join(':');
      if(action==='close')return close();if(action==='focus-search'){node('[name=keyword]').focus();return;}if(action==='fit')return fit();if(action==='zoom-in')return map?.zoomIn();if(action==='zoom-out')return map?.zoomOut();if(action==='tiles'){tileErrors=0;node('.atlas-tile-error').hidden=true;return tiles?.redraw();}
      if(action==='select')return select(value);if(action==='expand'){const expanded=host.classList.toggle('atlas-expanded');b.textContent=expanded?'收起列表':'展开列表';b.setAttribute('aria-expanded',expanded);setTimeout(()=>{map?.invalidateSize();fit();},220);return;}
      if(action==='day'){invalidate();clearRoad();day=Number(value);mode='planned';origin=null;nearby=[];results=[];selected=all()[0]?.key||null;message='';host.querySelectorAll('[data-atlas^="day:"]').forEach(b=>{const active=b.dataset.atlas==='day:'+day;b.classList.toggle('is-active',active);b.setAttribute('aria-pressed',active);});paint();fit();loadWeather();return;}
      if(action==='planned'){request++;searching=false;mode='planned';message='';selected=all()[0]?.key||null;paint();fit();loadWeather();return;}
      if(action==='nearby'||action==='around')return around();if(action==='category'){category=value;if(origin)query(true);return;}
      if(action==='search'){if(node('.atlas-search').reportValidity())query(false);return;}if(action==='route')return loadRoute();
      if(action==='edit'&&writable())return onEdit(trip.activities.find(a=>'a:'+a.id===selected));
      if(action==='link'&&writable()){const p=current(),a=trip.activities.find(a=>a.title.trim()===p?.name.trim()&&!T.valid(a)&&!a.locked&&!a.done);if(p&&a)onLink(p,a);return;}
      if(action==='add'&&writable()){const p=current();if(p)onAdd(p,day<0?0:day);}
    });
    function refresh(next,canWrite){if(!host.open)return;if(!next||next.id!==trip.id){close();return;}context.writable=canWrite;if(next.version!==trip.version){invalidate();trip=next;clearRoad();mode='planned';nearby=[];results=[];origin=null;if(!current())selected=all()[0]?.key||null;paint();loadWeather();}}
    return {open,close,refresh};
  }
  return {create};
})();
