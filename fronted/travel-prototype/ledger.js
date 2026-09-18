/* Local interaction model. Server-side membership and permissions remain authoritative. */
(() => {
  let section = 'bills', filter = '', editing = null, exportScope = 'book';
  const oldRender = render, oldDocument = documentHtml, oldOpenBill = openBill, oldAgent = openAgent;
  const labels = { bills: '明细', statistics: '统计', members: '成员', requests: '审批', settlement: '结算', settings: '设置' };
  function model() {
    state.ledger ||= { name: '杭州两日', categories: ['餐饮','交通','住宿','门票','其他'], roles: { 我:'所有者', 小林:'管理员', 小陈:'成员', 小余:'成员' }, invitations: [], temps: [], requests: [], payments: [], exports: [], logs: [], archived: false };
    return state.ledger;
  }
  function log(text) { model().logs.unshift({ text, at: new Date().toLocaleString('zh-CN') }); save(); }
  function split(bill) { return bill.participants?.length ? bill.participants : members; }
  function shares(bill) {
    const people = split(bill);
    return people.map((name,i) => ({name,cents:Math.floor(bill.cents/people.length)+(i<bill.cents%people.length?1:0)}));
  }
  function balances() {
    const result = Object.fromEntries(members.map(name=>[name,0]));
    state.bills.forEach(bill => {
      result[bill.payer] += bill.cents;
      shares(bill).forEach(part=>result[part.name]-=part.cents);
    });
    model().payments.filter(x=>x.status==='confirmed').forEach(x=>{result[x.from]+=x.cents;result[x.to]-=x.cents;});
    return result;
  }
  function transfers() {
    const entries=Object.entries(balances());
    const creditors=entries.filter(([,n])=>n>0).map(([name,cents])=>({name,cents}));
    const debtors=entries.filter(([,n])=>n<0).map(([name,n])=>({name,cents:-n}));
    const lines=[];
    for(const debtor of debtors) for(const creditor of creditors) {
      const cents=Math.min(debtor.cents,creditor.cents);
      if(cents>0) lines.push({from:debtor.name,to:creditor.name,cents});
      debtor.cents-=cents;creditor.cents-=cents;
    }
    return lines;
  }
  function categoriesHtml() {
    return model().categories.map(name=>{
      const cents=state.bills.filter(b=>b.category===name).reduce((sum,b)=>sum+b.cents,0);
      return `<div class="stat-line"><div><span>${esc(name)}</span><strong>¥${money(cents)}</strong></div><div class="progress-track"><i style="width:${total()?cents/total()*100:0}%"></i></div></div>`;
    }).join('');
  }
  function actionsLocked() { return model().archived || model().payments.some(x=>x.status==='confirmed'); }
  const statusText = {pending:'待确认',confirmed:'已确认',rejected:'已拒绝',cancelled:'已撤回',approved:'已通过'};
  const empty = text => `<p class="ledger-empty">${text}</p>`;
  function billsHtml() {
    const bills=state.bills.filter(b=>!filter||b.category===filter);
    return `<div class="ledger-controls"><label>筛选分类<select id="ledger-filter"><option value="">全部分类</option>${model().categories.map(x=>`<option ${filter===x?'selected':''}>${esc(x)}</option>`).join('')}</select></label><button class="primary-button" data-action="new-bill" ${actionsLocked()?'disabled':''}>＋ 记一笔</button></div>${bills.map(b=>`<article class="ledger-bill"><div class="bill-row"><span class="bill-icon">${b.category==='住宿'?'⌂':b.category==='交通'?'⇢':'▤'}</span><div class="bill-info"><strong>${esc(b.title)}</strong><p>9 月 ${26+b.day} 日 · ${esc(b.category)} · ${esc(b.payer)}先付</p></div><div class="bill-amount">¥${money(b.cents)}<small>${split(b).length===1?'个人支出':`${split(b).length} 人分摊`}</small></div></div><details><summary>分摊与操作</summary><div class="split-list">${shares(b).map(x=>`<span>${esc(x.name)} <b>¥${money(x.cents)}</b></span>`).join('')}</div><div class="row-actions"><button data-ledger-edit="${b.id}" ${actionsLocked()?'disabled':''}>修改账单</button><button data-ledger-delete="${b.id}" ${actionsLocked()?'disabled':''}>申请删除</button><button data-ledger-receipt="${b.id}">凭据</button></div></details></article>`).join('')||empty('暂无账单，记下第一笔支出。')}`;
  }
  function statisticsHtml() {
    return `<h2 class="ledger-section-title">分类支出</h2>${categoriesHtml()}<h2 class="ledger-section-title">成员垫付 / 应承担</h2>${members.map(name=>{
      const paid=state.bills.filter(b=>b.payer===name).reduce((sum,b)=>sum+b.cents,0);
      const owed=state.bills.reduce((sum,b)=>sum+(shares(b).find(x=>x.name===name)?.cents||0),0);
      return `<div class="management-row"><strong>${esc(name)}</strong><span>垫付 ¥${money(paid)}<small>应承担 ¥${money(owed)}</small></span></div>`;
    }).join('')}`;
  }
  function membersHtml() {
    return `<p class="form-note">本机演示管理者视角，邀请不会真实发送。正式成员沿用原账本角色，临时成员挂靠管理。</p>${members.map(name=>`<div class="management-row"><strong>${esc(name)}</strong>${name==='我'?'<span>所有者</span>':`<label class="sr-only" for="role-${name}">角色 ${name}</label><select id="role-${name}" data-member-role="${name}"><option ${model().roles[name]==='成员'?'selected':''}>成员</option><option ${model().roles[name]==='管理员'?'selected':''}>管理员</option></select>`}</div>`).join('')}<details class="ledger-details"><summary>邀请成员</summary><form id="invite-form"><label>成员昵称<input name="name" required maxlength="20" placeholder="输入昵称，创建本地邀请草稿"></label><button class="primary-button">创建邀请草稿</button></form>${model().invitations.map(x=>`<div class="management-row"><span>${esc(x.name)}<small>${x.status==='pending'?'待发送 · 草稿':'已撤回'}</small></span>${x.status==='pending'?`<button data-invite-cancel="${x.id}">撤回</button>`:''}</div>`).join('')}</details><details class="ledger-details"><summary>临时成员 ${model().temps.length ? `· ${model().temps.length}`:''}</summary><form id="temp-form"><label>昵称<input name="name" required maxlength="20"></label><label>挂靠成员<select name="attached">${members.map(x=>`<option>${esc(x)}</option>`).join('')}</select></label><button class="primary-button">添加临时成员</button></form>${model().temps.map(x=>`<div class="management-row"><span>${esc(x.name)}<small>挂靠 ${esc(x.attached)} · ${x.active?'启用':'停用'}</small></span><button data-temp-toggle="${x.id}">${x.active?'停用':'启用'}</button></div>`).join('')}<p class="form-note">临时成员分摊、挂靠转移与追偿将在原服务接入后开放。</p></details>`;
  }
  function requestsHtml() {
    return `<p class="form-note">修改或删除先生成申请，通过后才影响金额。这里演示管理员审批，真实权限以服务端为准。</p>${model().requests.map(r=>`<article class="request-row"><strong>${r.kind==='delete'?'删除':'修改'} · ${esc(r.title)}</strong><p>${r.kind==='edit'?`¥${money(r.beforeCents)} → ¥${money(r.next.cents)}`:'删除后重新计算支出和分摊'}</p><span class="pill">${statusText[r.status]}</span>${r.status==='pending'?`<div class="row-actions"><button data-request-approve="${r.id}">通过</button><button data-request-reject="${r.id}">拒绝</button><button data-request-cancel="${r.id}">撤回</button></div>`:''}</article>`).join('')||empty('暂无审批。可从账单明细发起修改或删除申请。')}`;
  }
  function settlementView() {
    const list=transfers();
    return `<p class="form-note">先记录付款，再由收款方确认。以下仅修改本机演示状态，不会转账。</p><h2 class="ledger-section-title">待结算建议</h2>${list.map((t,i)=>`<div class="management-row"><span>${esc(t.from)} → ${esc(t.to)}<small>¥${money(t.cents)}</small></span><button data-payment-create="${i}" ${model().payments.some(p=>p.status==='pending')?'disabled':''}>记录已付款</button></div>`).join('')||empty('账目已平。')}<h2 class="ledger-section-title">收付款记录</h2>${model().payments.map(p=>`<article class="request-row"><strong>${esc(p.from)} → ${esc(p.to)} · ¥${money(p.cents)}</strong><p>${statusText[p.status]}${p.status==='pending'?' · 等待收款方确认':''}</p>${p.status==='pending'?`<div class="row-actions"><button data-payment-confirm="${p.id}">模拟收款方确认</button><button data-payment-reject="${p.id}">拒绝</button></div>`:''}</article>`).join('')||empty('还没有收付款记录。')}`;
  }
  function settingsHtml() {
    return `<form id="book-settings"><label>账本名称<input name="name" value="${esc(model().name)}" required maxlength="30"></label><label>总预算 · 元<input name="budget" value="${state.prefs.budget}" type="number" min="1" max="1000000" required></label><button class="primary-button">保存设置</button></form><details class="ledger-details"><summary>分类管理</summary><p>${model().categories.map(esc).join(' / ')}</p><form id="category-form"><label>新增分类<input name="name" required maxlength="12"></label><button class="primary-button">添加分类</button></form></details><details class="ledger-details"><summary>导出记录 · ${model().exports.length}</summary>${model().exports.map(x=>`<p class="history-line">${esc(x.at)} · ${x.scope==='personal'?'个人明细':'账本汇总'}预览</p>`).join('')||empty('尚未预览导出。')}</details><details class="ledger-details"><summary>操作记录 · ${model().logs.length}</summary>${model().logs.slice(0,20).map(x=>`<p class="history-line">${esc(x.text)}<small>${esc(x.at)}</small></p>`).join('')||empty('暂无操作。')}</details><button class="add-card" data-ledger-action="archive">${model().archived?'恢复账本':'归档账本'}</button><p class="form-note">样稿仅维护当前共享账本。多账本切换、封面上传、转让/退出等保留在原小程序中，迁移接入见功能对照文档。</p>`;
  }
  ledgerHtml = function() {
    const m=model();const ratio=Math.round(total()/state.prefs.budget);
    return `<div class="budget-strip"><div class="budget-label"><span>${esc(m.name)} · ${m.archived?'已归档':'共享账本'}</span><span>预算使用 ${ratio}%</span></div><div class="budget-numbers"><strong>¥${money(total())}</strong><span>/ ¥${Number(state.prefs.budget).toLocaleString('zh-CN')}</span></div><div class="progress-track"><i style="width:${Math.min(100,ratio)}%"></i></div></div><nav class="ledger-tabs" aria-label="账本功能">${Object.entries(labels).map(([key,label])=>`<button data-ledger-section="${key}" class="${section===key?'active':''}" aria-current="${section===key?'page':'false'}">${label}${key==='requests'&&m.requests.some(x=>x.status==='pending')?' · '+m.requests.filter(x=>x.status==='pending').length:''}</button>`).join('')}</nav>${({bills:billsHtml,statistics:statisticsHtml,members:membersHtml,requests:requestsHtml,settlement:settlementView,settings:settingsHtml})[section]()}`;
  };
  documentHtml = function() {
    if(view!=='ledger') return oldDocument();
    const bills=exportScope==='personal'?state.bills.filter(b=>split(b).includes('我')):state.bills;
    return `<div class="paper-brand">SHARED LEDGER</div><div class="paper-title">${esc(model().name)} · ${exportScope==='personal'?'个人明细':'账本汇总'}</div><p class="paper-meta">9 月 26 — 27 日 · ${bills.length} 笔 · 本地演示</p><section class="paper-day"><b>${exportScope==='personal'?'我的承担金额':'账本总支出'} ¥${money(exportScope==='personal'?bills.reduce((s,b)=>s+(shares(b).find(x=>x.name==='我')?.cents||0),0):total())}</b>${bills.map(b=>`<div class="paper-row"><span>09.${26+b.day}</span><div>${esc(b.title)} · ¥${money(b.cents)}<small>${esc(b.payer)}先付 / ${shares(b).map(x=>`${esc(x.name)} ¥${money(x.cents)}`).join('、')}</small></div></div>`).join('')}</section><section class="paper-day"><b>待结算建议</b>${transfers().map(t=>`<div class="paper-row"><span>转账</span><div>${esc(t.from)} → ${esc(t.to)} ¥${money(t.cents)}</div></div>`).join('')||'<p class="paper-meta">账目已平。</p>'}</section><p class="paper-meta" style="margin-top:18px">不包含私密凭据。结算建议不代表实际付款。</p>`;
  };
  render = function() {
    oldRender();const ledger=view==='ledger', gallery=view==='media';
    $('.export-stage').setAttribute('aria-label',ledger?'账本导出预览':'行程导出预览');$('#primary').setAttribute('aria-label',ledger?'账本内容':gallery?'画廊内容':'旅行内容');$('.export-stage').hidden=gallery;$('.companion').hidden=gallery;$('.agent-entry').hidden=gallery;
    $('.top-actions .quiet').hidden=gallery;
    $('.top-actions .quiet').textContent=ledger?'↗ 导出账本':'↗ 导出行程';
    $('.export-heading h2').textContent=ledger?'账本导出':'行程单';
    $('.export-heading p').textContent=ledger?'账单明细 · 分摊与结算':'完整行程 · PDF / 打印';
    $('.export-link').innerHTML=(ledger?'预览与导出账本':'预览与导出')+' <span>↗</span>';
    $('.preview-paper').setAttribute('aria-label',ledger?'展开账本导出预览':'展开正视行程导出预览');
    $('.companion-head strong').textContent=ledger?'帮我算账':'行程助手';
    $('.agent-entry span').textContent=ledger?'帮我算账':'行程助手';
    $('.wide-link').innerHTML=(ledger?'查看分摊与结算':'规划偏好')+' <span>↗</span>';
    $('#export-title').textContent=ledger?'账本导出':'行程单';
    $('#export-scope').hidden=!ledger;
    if(ledger) $('#page-subtitle').textContent=model().name+' · '+members.length+' 位账本成员';
    if(gallery) $('#page-title').textContent='Gallery';
  };
  openAgent = function() {
    if(view!=='ledger') {oldAgent();return;}
    $('#calculation-body').innerHTML=`<p class="form-note">基于当前账单确定性计算，未调用大模型。</p><p>共 ${state.bills.length} 笔，支出 ¥${money(total())}。</p><h3>成员余额</h3>${Object.entries(balances()).map(([name,n])=>`<div class="management-row"><span>${esc(name)}</span><strong>${n>=0?'应收':'应付'} ¥${money(Math.abs(n))}</strong></div>`).join('')}<p class="form-note">每笔按所选成员均分，余数按成员顺序分配；已确认付款计入余额，待确认付款不抵扣。</p><button class="primary-button" data-ledger-action="go-settlement">查看结算记录</button>`;
    $('#calculation-dialog').showModal();
  };
  openBill=function(activity=null) {
    if(actionsLocked()){toast('已归档或已确认结算，当前样稿不再改动账单');return;}
    editing=null;oldOpenBill(activity);const form=$('#bill-form');
    form.elements.category.innerHTML=model().categories.map(c=>`<option>${esc(c)}</option>`).join('');
    $('#bill-title').textContent='记一笔';
    $('#split-options').innerHTML=members.map(name=>`<label><input type="checkbox" name="participant" value="${esc(name)}" checked>${esc(name)}</label>`).join('');
    $('#bill-form .form-note').textContent='选择参与分摊的成员，按分均分；只选自己即为个人支出。账单修改和删除需审批。';
  };
  window.saveLedgerBill = function(form) {
    if(actionsLocked()) return;
    const data=new FormData(form),title=String(data.get('title')).trim(),cents=Math.round(Number(data.get('amount'))*100),participants=data.getAll('participant');
    if(!title||!Number.isSafeInteger(cents)||cents<=0||!participants.length){toast('请填写金额并选择至少一位分摊成员');return;}
    const next={id:editing||crypto.randomUUID(),title,cents,participants,payer:data.get('payer'),category:data.get('category'),day:Number(data.get('day')),activityId:linkedActivity};
    if(editing){if(model().requests.some(r=>r.billId===editing&&r.status==='pending')){toast('这笔账单已有待审批申请');return;}const b=state.bills.find(x=>x.id===editing);model().requests.unshift({id:crypto.randomUUID(),billId:b.id,title:b.title,kind:'edit',next,before:JSON.stringify(b),beforeCents:b.cents,status:'pending'});log('发起修改申请：'+b.title);section='requests';}
    else {state.bills.push(next);model().payments.filter(x=>x.status==='pending').forEach(x=>x.status='cancelled');log('新增账单：'+title);section='bills';}
    $('#bill-dialog').close();view='ledger';history.replaceState(null,'','#ledger');render();toast(editing?'已提交本地修改申请':'已保存本地账单');editing=null;
  };
  document.body.insertAdjacentHTML('beforeend','<dialog id="calculation-dialog" aria-labelledby="calculation-title"><header class="dialog-head"><h2 id="calculation-title">帮我算账</h2><button class="icon-button" data-close="calculation-dialog" aria-label="关闭算账">×</button></header><div id="calculation-body"></div></dialog>');
  $('#bill-form .form-note').insertAdjacentHTML('beforebegin','<fieldset class="split-field"><legend>分摊成员</legend><div id="split-options"></div></fieldset>');
  $('#export-document').insertAdjacentHTML('beforebegin','<label id="export-scope">导出范围<select><option value="book">账本汇总</option><option value="personal">我的个人明细</option></select></label>');
  document.addEventListener('click',event=>{
    const b=event.target.closest('button');if(!b)return;
    if(b.dataset.action==='export'&&view==='ledger'){model().exports.unshift({at:new Date().toLocaleString('zh-CN'),scope:exportScope});save();}
    if(b.dataset.ledgerSection){section=b.dataset.ledgerSection;render();return;}
    if(b.dataset.ledgerEdit){const item=state.bills.find(x=>x.id===b.dataset.ledgerEdit);openBill();if(!$('#bill-dialog').open)return;editing=item.id;linkedActivity=item.activityId||null;const form=$('#bill-form');['title','payer','category','day'].forEach(k=>form.elements[k].value=item[k]);form.elements.amount.value=(item.cents/100).toFixed(2);form.querySelectorAll('[name=participant]').forEach(x=>x.checked=split(item).includes(x.value));$('#bill-title').textContent='申请修改账单';return;}
    if(b.dataset.ledgerDelete&&!actionsLocked()){const item=state.bills.find(x=>x.id===b.dataset.ledgerDelete);if(model().requests.some(r=>r.billId===item.id&&r.status==='pending')){toast('这笔账单已有待审批申请');return;}model().requests.unshift({id:crypto.randomUUID(),billId:item.id,title:item.title,before:JSON.stringify(item),beforeCents:item.cents,kind:'delete',status:'pending'});log('发起删除申请：'+item.title);section='requests';render();return;}
    if(b.dataset.ledgerReceipt){view='media';mediaType='receipt';history.replaceState(null,'','#media');render();toast('进入私密凭据，本地预览尚未关联账单');return;}
    for(const [key,status] of [['requestApprove','approved'],['requestReject','rejected'],['requestCancel','cancelled']]) if(b.dataset[key]){
      const r=model().requests.find(x=>x.id===b.dataset[key]);if(!r||r.status!=='pending')return;
      if(status==='approved') {if(actionsLocked()){toast('账本已锁定，不能审批变更');return;}const current=state.bills.find(x=>x.id===r.billId);if(!current||(r.before?JSON.stringify(current)!==r.before:current.cents!==r.beforeCents)){toast('账单已变化，请撤回后重新申请');return;}if(r.kind==='delete')state.bills=state.bills.filter(x=>x.id!==r.billId);else state.bills=state.bills.map(x=>x.id===r.billId?r.next:x);model().payments.filter(x=>x.status==='pending').forEach(x=>x.status='cancelled');}
      r.status=status;log(statusText[status]+'申请：'+r.title);render();return;
    }
    if(b.dataset.inviteCancel){model().invitations.find(x=>x.id===b.dataset.inviteCancel).status='cancelled';log('撤回邀请草稿');render();return;}
    if(b.dataset.tempToggle){const t=model().temps.find(x=>x.id===b.dataset.tempToggle);t.active=!t.active;log((t.active?'启用':'停用')+'临时成员 '+t.name);render();return;}
    if(b.dataset.paymentCreate!==undefined){if(model().payments.some(x=>x.status==='pending'))return;const t=transfers()[Number(b.dataset.paymentCreate)];if(!t)return;model().payments.unshift({...t,id:crypto.randomUUID(),status:'pending'});log('记录付款，待收款方确认');render();return;}
    for(const [key,status] of [['paymentConfirm','confirmed'],['paymentReject','rejected']])if(b.dataset[key]){const p=model().payments.find(x=>x.id===b.dataset[key]);if(p.status!=='pending')return;p.status=status;log(statusText[status]+'收付款记录');render();return;}
    if(b.dataset.ledgerAction==='go-settlement'){$('#calculation-dialog').close();section='settlement';render();}
    if(b.dataset.ledgerAction==='archive'){model().archived=!model().archived;log(model().archived?'归档账本':'恢复账本');render();}
  });
  document.addEventListener('change',event=>{
    const el=event.target;
    if(el.id==='ledger-filter'){filter=el.value;render();}
    if(el.dataset.memberRole){model().roles[el.dataset.memberRole]=el.value;log('调整 '+el.dataset.memberRole+' 的角色为 '+el.value);}
    if(el.matches('#export-scope select')){exportScope=el.value;$('#export-document').innerHTML=documentHtml();$('#paper-preview').innerHTML=documentHtml();}
  });
  document.addEventListener('submit',event=>{
    const id=event.target.id;if(!['invite-form','temp-form','category-form','book-settings'].includes(id))return;
    event.preventDefault();const data=new FormData(event.target);const name=String(data.get('name')).trim();if(!name)return;
    if(id==='invite-form'){model().invitations.unshift({id:crypto.randomUUID(),name,status:'pending'});log('创建邀请草稿：'+name);}
    if(id==='temp-form'){model().temps.push({id:crypto.randomUUID(),name,attached:data.get('attached'),active:true});log('添加临时成员：'+name);}
    if(id==='category-form'){if(model().categories.includes(name)){toast('分类已存在');return;}model().categories.push(name);log('添加分类：'+name);}
    if(id==='book-settings'){model().name=name;state.prefs.budget=Number(data.get('budget'));log('更新账本设置');}
    render();toast('已保存到本机演示');
  });
  render();
})();
