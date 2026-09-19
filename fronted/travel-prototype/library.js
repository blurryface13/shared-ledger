/* Independent trip and ledger collections; all storage remains local to this prototype. */
(() => {
  const key='shared-ledger-library-v1', previousRender=render, previousSave=save, previousBill=openBill;
  let library, screen='library', historyMode=false, creating=false, settingsOpen=false;
  const baseMeta={id:'trip-demo',name:'杭州两日',destination:'杭州',start:'2026-09-26',end:'2026-09-27',archived:false};
  // Migrate only exact original demo text, preserving user-written titles and notes.
  const copyMap={'西湖，沿着湖边走走':'西湖','湖边的午餐':'午餐','入住 · 湖畔小住':'酒店入住','山间午餐':'午餐','湖畔小住 · 两间房':'酒店 · 两间房','A morning by West Lake':'West Lake','Lunch, with a view':'Lunch','A place to slow down':'Check-in','A walk through the green':'Jiuxi','A table in the hills':'Lunch','One last little wander':'Hefang Street','从断桥出发，今天不用赶路。':'起点：断桥。','找一家顺眼的小店，慢慢吃。':'餐厅待定。','把下午留给茶香。开放与预约信息待确认。':'开放时间与预约待确认。','带一瓶水，穿一双好走的鞋。':'步行游览。','今天的午餐，留给沿途的发现。':'餐厅待定。','给朋友带一点小礼物，再慢慢回家。':'步行街。'};
  function simpleCopy(data){data.days.flat().forEach(item=>['title','english','note'].forEach(k=>{if(copyMap[item[k]])item[k]=copyMap[item[k]];}));data.bills.forEach(b=>{if(copyMap[b.title])b.title=copyMap[b.title];});}
  simpleCopy(state);
  const freshLedger=(name,start)=>({id:crypto.randomUUID(),name,start,budget:4000,tripId:null,categories:['餐饮','交通','住宿','门票','其他'],roles:{我:'所有者',小林:'管理员',小陈:'成员',小余:'成员'},invitations:[],temps:[],requests:[],payments:[],exports:[],logs:[],archived:false});
  try {library=JSON.parse(localStorage.getItem(key));if(!Array.isArray(library?.trips)||!library.trips.length||!Array.isArray(library.books)||!library.books.length)library=null;}catch{}
  if(!library){
    state.ledger={...freshLedger('杭州两日','2026-09-26'),...state.ledger,id:'book-demo',tripId:'trip-demo'};
    state.ledger.budget ||= state.prefs.budget;
    library={trips:[{meta:baseMeta,prefs:structuredClone(state.prefs),days:structuredClone(state.days)}],books:[{meta:structuredClone(state.ledger),bills:structuredClone(state.bills)}],tripId:'trip-demo',bookId:'book-demo'};
  }
  const currentTrip=()=>library.trips.find(x=>x.meta.id===library.tripId)||library.trips[0];
  const currentBook=()=>library.books.find(x=>x.meta.id===library.bookId)||library.books[0];
  function hydrate(){window.resetLedgerPanel?.();const t=currentTrip(),b=currentBook();state.trip=structuredClone(t.meta);state.days=structuredClone(t.days);state.prefs=structuredClone(t.prefs);state.ledger=structuredClone(b.meta);state.bills=structuredClone(b.bills);day=0;expanded=null;showRoute=false;}
  function persist(){try{localStorage.setItem(key,JSON.stringify(library));}catch{toast('本次修改仅在当前页面保留');}}
  function flush(){const t=currentTrip(),b=currentBook();t.meta=structuredClone(state.trip);t.days=structuredClone(state.days);t.prefs=structuredClone(state.prefs);b.meta=structuredClone(state.ledger);b.bills=structuredClone(state.bills);persist();}
  hydrate();persist();$('[data-action="reset"]').hidden=true;
  save=function(){flush();previousSave();};
  function route(){history.replaceState(null,'',`#${view}${screen==='detail'&&view!=='media'?'/'+(view==='itinerary'?library.tripId:library.bookId):''}`);}
  function open(kind,id){flush();view=kind;library[kind==='itinerary'?'tripId':'bookId']=id;hydrate();screen='detail';creating=false;settingsOpen=false;route();render();window.scrollTo(0,0);}
  const isTrip=()=>view==='itinerary';
  function linkedNames(book){return library.trips.find(t=>t.meta.id===book.meta.tripId)?.meta.name;}
  function choices(kind,selected){return '<option value="">暂不绑定</option>'+(kind==='itinerary'?library.books:library.trips).filter(x=>!x.meta.archived).map(x=>`<option value="${esc(x.meta.id)}" ${selected===x.meta.id?'selected':''}>${esc(x.meta.name)}${(kind==='itinerary'?x.meta.tripId&&x.meta.id!==selected:library.books.some(b=>b.meta.tripId===x.meta.id)&&x.meta.id!==selected)?'（将替换关联）':''}</option>`).join('');}
  function creationHtml(){return `<form id="collection-create" class="collection-form"><div class="section-title"><h2>${isTrip()?'新建 Trip':'新建账本'}</h2><button type="button" data-library-action="cancel-create">取消</button></div><label>名称<input name="name" required maxlength="30" placeholder="${isTrip()?'例如：苏州三日':'例如：日常开销'}"></label>${isTrip()?'<label>目的地<input name="destination" required maxlength="30" placeholder="城市或地区"></label><div class="form-grid"><label>出发日期<input name="start" type="date" required></label><label>结束日期<input name="end" type="date" required></label></div>':'<label>预算 · 元<input name="budget" type="number" min="1" max="1000000" value="4000" required></label>'}<label>${isTrip()?'关联账本':'关联 Trip'}<select name="binding">${choices(view)}</select></label><p class="form-note">可以先独立创建，以后再绑定。${isTrip()?'行程最长 30 天。':''}</p><p class="collection-error" role="alert"></p><button class="primary-button">${isTrip()?'创建行程':'创建账本'}</button></form>`;}
  function libraryHtml(){const entries=(isTrip()?library.trips:library.books).filter(x=>!!x.meta.archived===historyMode);return `<div class="collection-toolbar"><div class="collection-tabs"><button data-library-filter="current" class="${historyMode?'':'active'}">已有${isTrip()?'行程':'账本'}</button><button data-library-filter="history" class="${historyMode?'active':''}">历史</button></div><button class="primary-button" data-library-action="create">＋ 新建</button></div>${creating?creationHtml():''}<div class="collection-list">${entries.map((x,i)=>{const m=x.meta,linked=isTrip()?library.books.filter(b=>b.meta.tripId===m.id).map(b=>b.meta.name).join('、'):linkedNames(x);return `<article class="collection-row"><div class="collection-index">${String(i+1).padStart(2,'0')}</div><div class="collection-info"><h2>${esc(m.name)}</h2><p>${isTrip()?esc(m.destination)+' · '+dateLabel(m.start)+' 至 '+dateLabel(m.end):x.bills.length+' 笔 · ¥'+money(x.bills.reduce((s,b)=>s+b.cents,0))}</p><span>${linked?'已绑定 · '+esc(linked):isTrip()?'未绑定账本':'独立账本'}</span>${historyMode?`<details><summary>查看记录</summary><p>${isTrip()?x.days.map((items,i)=>`第 ${i+1} 天：${items.map(a=>esc(a.title)).join('、')||'无安排'}`).join('<br>'):x.bills.map(b=>`${esc(b.title)} ¥${money(b.cents)}`).join('<br>')||'无账单'}</p></details>`:''}</div><button ${historyMode?'data-library-restore':'data-library-open'}="${esc(m.id)}" aria-label="${historyMode?'恢复':'打开'} ${esc(m.name)}">${historyMode?'恢复':'进入 ↗'}</button></article>`;}).join('')||`<div class="collection-empty"><img src="assets/${isTrip()?'itinerary':'ledger'}.png" alt=""><h2>${historyMode?'暂无历史记录':'还没有'+(isTrip()?'行程':'账本')}</h2><p>${historyMode?'归档后的记录会放在这里。':'点击新建，添加第一份记录。'}</p></div>`}</div>`;}
  function detailSettings(){return `<section class="detail-settings" ${settingsOpen?'':'hidden'}><form id="detail-settings-form"><h2>${isTrip()?'行程设置':'账本关联'}</h2>${isTrip()?`<label>行程名称<input name="name" maxlength="30" required value="${esc(state.trip.name)}"></label><div class="form-grid"><label>出发日期<input name="start" type="date" required value="${state.trip.start}"></label><label>结束日期<input name="end" type="date" required value="${state.trip.end}"></label></div>`:''}<label>${isTrip()?'关联账本':'关联 Trip'}<select name="binding">${choices(view,isTrip()?library.books.find(b=>b.meta.tripId===state.trip.id)?.meta.id:state.ledger.tripId)}</select></label><p class="form-note">行程与账本一对一关联，可随时解绑；替换关联不会合并或删除数据。${isTrip()?'已有安排的日期不能直接移除。':''}</p><p class="collection-error" role="alert"></p><div class="row-actions"><button class="primary-button">保存</button><button type="button" data-library-action="settings">收起</button><button type="button" data-library-action="archive">归档${isTrip()?'行程':'账本'}</button></div></form></section>`;}
  const stamp=document.createElement('button');stamp.className='trip-stamp';stamp.type='button';stamp.dataset.libraryAction='settings';$('.trip-stamp').replaceWith(stamp);
  const detailTools=document.createElement('div');detailTools.id='detail-tools';$('.page-heading').after(detailTools);
  render=function(){if(screen==='detail'&&view!=='media')route();previousRender();const hub=view!=='media'&&screen==='library',trip=isTrip(),m=trip?state.trip:state.ledger;
    document.body.classList.toggle('collection-screen',hub);$('.context-panel').hidden=hub||view==='media';$('.trip-stamp').hidden=hub||view!=='itinerary';$('.side-trip').hidden=hub||view==='media';$('.agent-entry').hidden=hub||view==='media';$('.top-actions').hidden=hub||view==='media';
    $('.breadcrumb').innerHTML=hub?`<strong>${trip?'Trips':'Ledgers'}</strong>`:`<button data-library-action="back">‹ ${trip?'全部行程':view==='ledger'?'全部账本':'Gallery'}</button>`;
    $('#detail-tools').innerHTML=hub||view==='media'?'':`<div class="detail-toolbar"><span>${trip?esc(state.trip.destination):state.ledger.tripId?'已绑定 · '+esc(linkedNames(currentBook())||''):'独立账本'}</span><button data-library-action="settings">${trip?'日期与绑定':'绑定行程'} ↗</button></div>${detailSettings()}`;
    if(hub){$('#page-kicker').textContent=trip?'ITINERARY':'LEDGER';$('#page-title').textContent=trip?'我的行程':'我的账本';$('#page-subtitle').textContent=trip?'新建路线，或继续已有行程':'独立记账，也可以关联旅行';$('#primary').innerHTML=libraryHtml();$('#primary').setAttribute('aria-label',trip?'行程列表':'账本列表');}
    else if(view!=='media'){$('#page-title').textContent=m.name;$('#page-kicker').textContent=trip?dateLabel(m.start)+' 至 '+dateLabel(m.end):'LEDGER';$('#page-subtitle').textContent=trip?state.prefs.people+' 人 · '+state.days.length+' 天':'4 位账本成员';$('.side-trip p').textContent=m.name;$('.side-trip > span').textContent=trip?dateLabel(m.start)+' / '+state.days.length+' DAYS':'LEDGER';$('.mini-landscape b').textContent=trip?m.destination:'账本';}
    const first=state.trip.start.split('-'),last=state.trip.end.split('-');stamp.innerHTML=`<span>${esc(state.trip.destination)}</span><b>${first[1]===last[1]?first[2]+' – '+last[2]:first[1]+'.'+first[2]+' – '+last[1]+'.'+last[2]}</b><small>${first[0]} / ${first[1]}</small><em>编辑日期 ↗</em>`;stamp.setAttribute('aria-label','编辑行程日期');stamp.setAttribute('aria-expanded',String(settingsOpen));
    if(!hub&&trip&&state.trip.id!=='trip-demo'){$('.companion').hidden=true;$('.agent-entry').hidden=true;}
  };
  openBill=function(activity=null){if(activity){const book=library.books.find(b=>b.meta.tripId===state.trip.id&&!b.meta.archived);if(!book){settingsOpen=true;render();toast('请先关联一个未归档的账本');return;}flush();library.bookId=book.meta.id;state.ledger=structuredClone(book.meta);state.bills=structuredClone(book.bills);}previousBill(activity);};
  function dateCount(start,end){return Math.round((new Date(end+'T12:00:00Z')-new Date(start+'T12:00:00Z'))/86400000)+1;}
  function bind(kind,id,target){if(kind==='itinerary'){library.books.forEach(b=>{if(b.meta.tripId===id)b.meta.tripId=null;});if(target)library.books.find(b=>b.meta.id===target).meta.tripId=id;}else {if(target)library.books.forEach(b=>{if(b.meta.tripId===target)b.meta.tripId=null;});library.books.find(b=>b.meta.id===id).meta.tripId=target||null;}}
  // Capture global navigation before the original detail-page handlers.
  document.addEventListener('click',event=>{const b=event.target.closest('button,a.brand');if(!b)return;
    if(b.dataset.view||b.matches('a.brand')){event.preventDefault();event.stopImmediatePropagation();flush();view=b.dataset.view||'itinerary';screen='library';historyMode=false;creating=false;settingsOpen=false;route();render();window.scrollTo(0,0);return;}
    if(b.dataset.libraryOpen){event.stopImmediatePropagation();open(view,b.dataset.libraryOpen);return;}
    if(b.dataset.libraryRestore){event.stopImmediatePropagation();const item=(isTrip()?library.trips:library.books).find(x=>x.meta.id===b.dataset.libraryRestore);item.meta.archived=false;persist();hydrate();render();toast('已恢复到已有记录');return;}
    if(b.dataset.libraryFilter){historyMode=b.dataset.libraryFilter==='history';creating=false;render();return;}
    const action=b.dataset.libraryAction||(b.dataset.ledgerAction==='archive'?'archive':null);if(!action)return;event.preventDefault();event.stopImmediatePropagation();
    if(action==='back'){flush();screen='library';historyMode=false;settingsOpen=false;route();}
    if(action==='create'){creating=true;historyMode=false;}
    if(action==='cancel-create')creating=false;
    if(action==='settings')settingsOpen=!settingsOpen;
    if(action==='archive'){if(isTrip())state.trip.archived=true;else state.ledger.archived=true;flush();screen='library';historyMode=true;settingsOpen=false;route();}
    render();if(action==='settings'&&settingsOpen)$('#detail-settings-form').scrollIntoView({block:'nearest'});
  },true);
  document.addEventListener('submit',event=>{
    const form=event.target;if(!['collection-create','detail-settings-form'].includes(form.id))return;event.preventDefault();event.stopImmediatePropagation();const data=new FormData(form),trip=isTrip(),start=data.get('start'),end=data.get('end'),count=trip?dateCount(start,end):0;
    const error=msg=>form.querySelector('.collection-error').textContent=msg;
    if(trip&&(!Number.isInteger(count)||count<1||count>30))return error('结束日期不能早于出发日期，行程最长 30 天。');
    if(data.has('name')&&!String(data.get('name')).trim())return error('请填写名称。');
    flush();
    if(form.id==='collection-create'){
      const id=crypto.randomUUID(),name=String(data.get('name')).trim();
      if(trip){if(!String(data.get('destination')).trim())return error('请填写目的地。');library.trips.push({meta:{id,name,destination:String(data.get('destination')).trim(),start,end,archived:false},prefs:structuredClone(initial.prefs),days:Array.from({length:count},()=>[])});}
      else library.books.push({meta:{...freshLedger(name,new Date().toLocaleDateString('en-CA')),id,budget:Number(data.get('budget'))},bills:[]});
      bind(view,id,data.get('binding'));persist();library[trip?'tripId':'bookId']=id;hydrate();screen='detail';creating=false;settingsOpen=false;route();
    }else{
      if(trip){const t=currentTrip();if(t.days.slice(count).some(items=>items.length))return error('将被移除的日期已有安排，请先移走这些行程卡。');t.meta={...t.meta,name:String(data.get('name')).trim(),start,end};t.days=Array.from({length:count},(_,i)=>t.days[i]||[]);}
      bind(view,trip?library.tripId:library.bookId,data.get('binding'));persist();hydrate();settingsOpen=false;
    }
    render();toast('已保存到本机');
  },true);
  function readRoute(){const [kind,id]=location.hash.slice(1).split('/');if(['itinerary','ledger','media'].includes(kind))view=kind;const list=view==='itinerary'?library.trips:library.books;const found=list.find(x=>x.meta.id===id&&!x.meta.archived);if(found){library[view==='itinerary'?'tripId':'bookId']=id;hydrate();screen='detail';}else screen='library';settingsOpen=false;render();}
  window.addEventListener('hashchange',readRoute);
  // Preserve collection integrity when the existing single-record reset is requested.
  document.addEventListener('click',event=>{if(event.target.closest('[data-action="reset"]')){event.stopImmediatePropagation();toast('多记录模式保留全部数据；可将不再使用的行程或账本归档。');}},true);
  readRoute();
})();
