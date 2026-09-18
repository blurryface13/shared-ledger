const $ = (selector) => document.querySelector(selector);
const members = ['我', '小林', '小陈', '小余'];
const storageKey = 'shared-ledger-travel-prototype-v1';
const initial = {
  prefs: { people: 4, budget: 4000, pace: 'relaxed', style: 'culture', stay: 'lake' },
  days: [
    [
      { id: 'a1', time: '09:30', duration: 90, title: '西湖，沿着湖边走走', english: 'A morning by West Lake', note: '从断桥出发，今天不用赶路。', color: 'blue', locked: false, done: false },
      { id: 'a2', time: '12:00', duration: 60, title: '湖边的午餐', english: 'Lunch, with a view', note: '找一家顺眼的小店，慢慢吃。', color: 'rose', locked: false, done: false },
      { id: 'a3', time: '14:00', duration: 120, title: '中国茶叶博物馆', english: 'China National Tea Museum', note: '把下午留给茶香。开放与预约信息待确认。', color: 'sage', locked: false, done: false },
      { id: 'a4', time: '17:00', duration: 45, title: '入住 · 湖畔小住', english: 'A place to slow down', note: '已锁定的住宿安排 · 示例订单', color: 'rose', locked: true, done: false }
    ],
    [
      { id: 'b1', time: '09:00', duration: 90, title: '九溪烟树', english: 'A walk through the green', note: '带一瓶水，穿一双好走的鞋。', color: 'sage', locked: false, done: false },
      { id: 'b2', time: '12:00', duration: 60, title: '山间午餐', english: 'A table in the hills', note: '今天的午餐，留给沿途的发现。', color: 'rose', locked: false, done: false },
      { id: 'b3', time: '14:00', duration: 90, title: '河坊街', english: 'One last little wander', note: '给朋友带一点小礼物，再慢慢回家。', color: 'blue', locked: false, done: false }
    ]
  ],
  bills: [
    { id: 'e1', title: '湖畔小住 · 两间房', cents: 128000, payer: '我', category: '住宿', day: 0 },
    { id: 'e2', title: '湖边的午餐', cents: 18600, payer: '小林', category: '餐饮', day: 0 },
    { id: 'e3', title: '车站到西湖', cents: 4800, payer: '小陈', category: '交通', day: 0 },
    { id: 'e4', title: '四杯桂花拿铁', cents: 6000, payer: '小余', category: '餐饮', day: 0 }
  ]
};
let state = structuredClone(initial);
try {
  const saved = JSON.parse(localStorage.getItem(storageKey));
  if (saved && Array.isArray(saved.days) && saved.days.length === 2 && saved.days.every(Array.isArray) && Array.isArray(saved.bills) && saved.prefs) state = saved;
} catch { /* A private window or corrupt storage must not prevent preview. */ }
let view = ['itinerary', 'ledger', 'media'].includes(location.hash.slice(1)) ? location.hash.slice(1) : 'itinerary';
let day = 0, expanded = null, editingId = null, linkedActivity = null, showRoute = false, proposal = null, mediaType = 'gallery';
let photos = [], toastTimer;
const esc = (value) => String(value ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = (cents) => (cents / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const minutes = (time) => Number(time.split(':')[0]) * 60 + Number(time.split(':')[1]);
const duration = (value) => value >= 60 ? `${Math.floor(value / 60)} 小时${value % 60 ? ` ${value % 60} 分` : ''}` : `${value} 分钟`;
const total = () => state.bills.reduce((sum, bill) => sum + bill.cents, 0);
function save() {
  try { localStorage.setItem(storageKey, JSON.stringify(state)); }
  catch { toast('浏览器未允许保存，本次修改仅在当前页面保留'); }
}
function toast(message) {
  $('#toast').textContent = message;
  $('#toast').classList.add('visible');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => $('#toast').classList.remove('visible'), 3200);
}
function documentHtml() {
  return `<div class="paper-brand">SHARED LEDGER</div><div class="paper-title">杭州两日 · 随行清单</div><p class="paper-meta">2026.09.26 — 09.27 · ${esc(state.prefs.people)} 人 · 规划总预算 ¥${Number(state.prefs.budget).toLocaleString('zh-CN')}</p>${state.days.map((items, index) => `<section class="paper-day"><b>第 ${index + 1} 天 / 9 月 ${26 + index} 日</b>${items.map(item => `<div class="paper-row"><span>${esc(item.time)}</span><div>${esc(item.title)}${item.done ? ' ✓' : ''}<small>${duration(item.duration)}${item.locked ? ' · 已锁定' : ''} · ${esc(item.note)}</small></div></div>`).join('')}</section>`).join('')}<p class="paper-meta" style="margin-top:20px">本地演示行程 · 地点、开放时间与交通尚待核实<br>不含私密账单凭据。</p>`;
}
function routeHtml() {
  return `<section class="route-diagram"><header><strong>今日路线 · 示意</strong><button data-action="play-route">播放 / 暂停</button></header><svg viewBox="0 0 400 165" role="img" aria-label="行程顺序示意，不代表真实道路"><path d="M 28 125 C 85 125 63 40 134 45 S 206 130 265 92 S 328 38 370 45" stroke="#93a8a5" stroke-width="2" stroke-dasharray="5 5" fill="none"/><g fill="#f9f8f2" stroke="#6f8580" stroke-width="2"><circle cx="28" cy="125" r="8"/><circle cx="134" cy="45" r="8"/><circle cx="265" cy="92" r="8"/><circle cx="370" cy="45" r="8"/></g><g fill="#374641" font-size="10" text-anchor="middle"><text x="28" y="149">第一站</text><text x="134" y="24">第二站</text><text x="265" y="120">第三站</text><text x="366" y="23">终点</text></g><circle class="route-dot" cx="0" cy="0" r="7" fill="#c29a40" transform="translate(28 125)"/></svg><p>仅展示路线动画方向，真实道路与耗时将在地图接入后提供。</p></section>`;
}
function itineraryHtml() {
  const items = state.days[day];
  return `<div class="day-tabs"><button class="day-tab ${day === 0 ? 'active' : ''}" data-day="0">第 1 天 · 09.26</button><button class="day-tab ${day === 1 ? 'active' : ''}" data-day="1">第 2 天 · 09.27</button><button class="map-toggle" data-action="route">${showRoute ? '收起' : '⌁ 查看'}路线</button></div><div class="day-title"><div><h2>第 ${day + 1} 天</h2><p>9 月 ${26 + day} 日 ${day ? '周日' : '周六'} · Hangzhou UTC+8</p></div><small>${items.filter(x => x.done).length} / ${items.length} 已完成</small></div><div class="base-line"><span>⌂</span><b>Base</b><span>湖畔小住 · 西湖周边（示例）</span></div>${showRoute ? routeHtml() : ''}${items.map((item, index) => `${index ? `<div class="connection"><span class="arrow">⇢</span><span>前往下一站</span><span class="route-note">交通方式与耗时待查询</span></div>${minutes(item.time) - minutes(items[index - 1].time) - items[index - 1].duration >= 45 ? `<div class="free-time">间隔 ${duration(minutes(item.time) - minutes(items[index - 1].time) - items[index - 1].duration)} · 含待确认交通时间</div>` : ''}` : ''}<article class="activity ${esc(item.color)}"><button class="card-open" data-expand="${esc(item.id)}" aria-expanded="${expanded === item.id}"><div class="activity-top"><span class="activity-time">${esc(item.time)}</span><span class="pill">${item.done ? '✓ 已打卡' : item.locked ? '⌑ 已锁定' : '○ 待出发'}</span></div><div class="activity-title"><h3>${esc(item.title)}</h3><span>${duration(item.duration)}</span></div><p class="english">${esc(item.english)}</p><p class="activity-note">${esc(item.note)}</p></button>${expanded === item.id ? `<div class="activity-actions"><button data-complete="${esc(item.id)}">${item.done ? '撤销打卡' : '✓ 手动打卡'}</button><button data-bill="${esc(item.id)}">＋ 记一笔</button><button data-lock="${esc(item.id)}">${item.locked ? '解锁' : '锁定'}</button><button data-edit="${esc(item.id)}" ${item.locked ? 'disabled' : ''}>编辑</button><button data-delete="${esc(item.id)}" ${item.locked || item.done ? 'disabled' : ''}>移除</button></div>` : ''}</article>`).join('')}<button class="add-card" data-action="add-activity">＋ 加一张行程卡</button>`;
}
function settlementHtml() {
  const paid = Object.fromEntries(members.map(name => [name, 0]));
  const owed = Object.fromEntries(members.map(name => [name, 0]));
  for (const bill of state.bills) {
    paid[bill.payer] += bill.cents;
    members.forEach((name, index) => owed[name] += Math.floor(bill.cents / 4) + (index < bill.cents % 4 ? 1 : 0));
  }
  const creditors = members.map(name => ({ name, cents: paid[name] - owed[name] })).filter(x => x.cents > 0);
  const debtors = members.map(name => ({ name, cents: owed[name] - paid[name] })).filter(x => x.cents > 0);
  const transfers = [];
  for (const debtor of debtors) for (const creditor of creditors) {
    const cents = Math.min(debtor.cents, creditor.cents);
    if (cents > 0) transfers.push(`${esc(debtor.name)} → ${esc(creditor.name)} <strong>¥${money(cents)}</strong>`);
    debtor.cents -= cents; creditor.cents -= cents;
  }
  return `<section class="settlement"><h3>结算试算</h3><p>按 4 位账本成员均分 · 以下为本地试算</p>${transfers.map(line => `<p>${line}</p>`).join('') || '<p>当前无需转账。</p>'}<p>尚未确认结算，不代表已经付款。</p></section>`;
}
function ledgerHtml() {
  const percent = Math.round(total() / (state.prefs.budget * 100) * 100);
  const icons = { '住宿': '⌂', '餐饮': '♧', '交通': '⇢', '门票': '▣', '其他': '＋' };
  return `<div class="budget-strip"><div class="budget-label"><span>旅途实际支出</span><span>${percent > 100 ? '已超预算' : '预算使用'} ${percent}%</span></div><div class="budget-numbers"><strong>¥${money(total())}</strong><span>/ 总预算 ¥${Number(state.prefs.budget).toLocaleString('zh-CN')}</span></div><div class="progress-track"><i style="width:${Math.min(100, percent)}%"></i></div></div><div class="section-title"><h2>账单明细</h2><button class="primary-button" data-action="new-bill">＋ 记一笔</button></div>${[0, 1].map(index => { const bills = state.bills.filter(x => x.day === index); return bills.length ? `<div class="section-title"><span>9 月 ${26 + index} 日 · ${index ? '周日' : '周六'}</span><span>${bills.length} 笔</span></div>${bills.map(bill => `<div class="bill-row"><span class="bill-icon">${icons[bill.category] || '＋'}</span><div class="bill-info"><strong>${esc(bill.title)}</strong><p>${esc(bill.category)} · ${esc(bill.payer)}先付 · 4 人均分</p></div><div class="bill-amount">¥${money(bill.cents)}<small>已记录 · 示例账本</small></div></div>`).join('')}` : ''; }).join('')}${settlementHtml()}`;
}
function mediaHtml() {
  const shown = photos.filter(photo => photo.type === mediaType);
  return `<p class="section-description">相册与私密凭据分开管理。</p><div class="media-toolbar"><button class="chip ${mediaType === 'gallery' ? 'active' : ''}" data-media="gallery">旅行相册</button><button class="chip ${mediaType === 'receipt' ? 'active' : ''}" data-media="receipt">私密凭据</button></div><label class="upload-zone"><span>＋</span><strong>${mediaType === 'gallery' ? '添一张旅途中的照片' : '添一张账单凭据'}</strong><small>JPG / PNG / WebP · 每张最多 10 MB</small><small>只在本机预览，刷新后清除，不上传服务器</small><input id="photo-upload" type="file" accept="image/jpeg,image/png,image/webp" multiple aria-label="选择本地图片"></label>${shown.length ? `<div class="media-grid">${shown.map(photo => `<figure class="photo"><img src="${photo.url}" alt="${esc(photo.name)}"><figcaption>${esc(photo.name)}<small>${mediaType === 'receipt' ? '私密凭据 · 未分享' : '本地预览 · 尚未发布'}<br>水印待接入</small><button data-remove-photo="${photo.id}">移除</button></figcaption></figure>`).join('')}</div>` : ''}<section class="watermark-info"><div class="section-title"><h3>水印处理</h3><span class="pill">算法待接入</span></div><p>未来将为分享副本嵌入隐形水印，保留原件，并支持提取验证。当前样稿只展示图片与处理流程，不执行嵌入或提取。</p><div class="process-steps"><span>保留原件</span>→<span class="pending">嵌入水印</span>→<span class="pending">验证结果</span>→<span class="pending">分享副本</span></div><p>${mediaType === 'receipt' ? '凭据默认仅账本成员可见，公开导出不包含这些图片。' : '公开分享时只选择相册中的照片，不带入私密凭据。'}</p></section>`;
}
function render() {
  document.querySelectorAll('[data-view]').forEach(button => {
    button.classList.toggle('active', button.dataset.view === view);
    button.setAttribute('aria-current', button.dataset.view === view ? 'page' : 'false');
  });
  const titles = { itinerary: ['09.26 — 09.27', '杭州两日', `${state.prefs.people} 人 · 2 天`], ledger: ['09.26 — 09.27', '共享账本', '杭州两日 · 4 位账本成员'], media: ['09.26 — 09.27', '相册与水印', '杭州两日'] };
  ['page-kicker', 'page-title', 'page-subtitle'].forEach((id, i) => $(`#${id}`).textContent = titles[view][i]);
  $('#primary').innerHTML = view === 'itinerary' ? itineraryHtml() : view === 'ledger' ? ledgerHtml() : mediaHtml();
  $('#paper-preview').innerHTML = documentHtml();
  $('#export-document').innerHTML = documentHtml();
}
function openAgent() {
  const form = $('#preferences-form');
  Object.entries(state.prefs).forEach(([name, value]) => form.elements[name].value = value);
  $('#proposal').hidden = true; proposal = null;
  $('#agent-dialog').showModal();
}
function openBill(activity = null) {
  const form = $('#bill-form'); form.reset();
  linkedActivity = activity?.id || null;
  form.elements.title.value = activity?.title || '';
  form.elements.day.value = day;
  $('#bill-dialog').showModal();
}
function openActivity(item = null) {
  const form = $('#activity-form'); form.reset(); editingId = item?.id || null;
  $('#activity-title').textContent = item ? '编辑这张行程卡' : '加一张行程卡';
  if (item) ['title', 'time', 'duration', 'note'].forEach(name => form.elements[name].value = item[name]);
  $('#activity-error').textContent = '';
  $('#activity-dialog').showModal();
}
document.addEventListener('click', (event) => {
  const button = event.target.closest('button');
  if (!button) return;
  if (button.dataset.view) { view = button.dataset.view; history.replaceState(null, '', `#${view}`); render(); return; }
  if (button.dataset.close) { $(`#${button.dataset.close}`).close(); return; }
  if (button.dataset.day !== undefined) { day = Number(button.dataset.day); expanded = null; render(); return; }
  if (button.dataset.expand) { expanded = expanded === button.dataset.expand ? null : button.dataset.expand; render(); return; }
  if (button.dataset.media) { mediaType = button.dataset.media; render(); return; }
  if (button.dataset.removePhoto) { const photo = photos.find(x => x.id === button.dataset.removePhoto); URL.revokeObjectURL(photo.url); photos = photos.filter(x => x !== photo); render(); return; }
  for (const action of ['complete', 'lock', 'edit', 'delete', 'bill']) {
    if (!button.dataset[action]) continue;
    const item = state.days[day].find(x => x.id === button.dataset[action]); if (!item) return;
    if (action === 'bill') { openBill(item); return; }
    if (action === 'edit') { if (!item.locked) openActivity(item); return; }
    if (action === 'complete') item.done = !item.done;
    if (action === 'lock') item.locked = !item.locked;
    if (action === 'delete' && !item.locked && !item.done) state.days[day] = state.days[day].filter(x => x.id !== item.id);
    save(); render(); return;
  }
  switch (button.dataset.action) {
    case 'export': $('#export-document').innerHTML = documentHtml(); $('#export-dialog').showModal(); break;
    case 'print': window.print(); break;
    case 'agent': openAgent(); break;
    case 'new-bill': openBill(); break;
    case 'add-activity': openActivity(); break;
    case 'route': showRoute = !showRoute; render(); break;
    case 'play-route': { const dot = $('.route-dot'); if (matchMedia('(prefers-reduced-motion: reduce)').matches) { toast('已启用减少动态效果，保留静态路线'); break; } dot.removeAttribute('transform'); dot.classList.toggle('playing'); if (!dot.classList.contains('playing')) dot.setAttribute('transform', 'translate(28 125)'); break; }
    case 'accept-proposal': if (proposal) { state.prefs = proposal.prefs; state.days = proposal.days; save(); $('#agent-dialog').close(); day = 0; view = 'itinerary'; history.replaceState(null, '', '#itinerary'); render(); toast('已采纳演示草案，锁定安排已保留'); proposal = null; } break;
    case 'reset': if (confirm('重置本机演示行程和账单？本地图片预览也会清除。')) { state = structuredClone(initial); photos.forEach(photo => URL.revokeObjectURL(photo.url)); photos = []; save(); render(); toast('演示已重置'); } break;
  }
});
$('#bill-form').addEventListener('submit', (event) => {
  event.preventDefault(); if (window.saveLedgerBill) { window.saveLedgerBill(event.target); return; } const data = new FormData(event.target);
  const cents = Math.round(Number(data.get('amount')) * 100);
  const title = String(data.get('title')).trim();
  if (!title || !Number.isSafeInteger(cents) || cents <= 0) return;
  state.bills.push({ id: crypto.randomUUID(), title, cents, payer: data.get('payer'), category: data.get('category'), day: Number(data.get('day')), activityId: linkedActivity });
  save(); $('#bill-dialog').close(); view = 'ledger'; history.replaceState(null, '', '#ledger'); render(); toast('已记录在本机演示账本中');
});
$('#activity-form').addEventListener('submit', (event) => {
  event.preventDefault(); const data = new FormData(event.target);
  const title = String(data.get('title')).trim(), time = data.get('time'), length = Number(data.get('duration'));
  if (!title || !time || length < 15) return;
  const start = minutes(time), end = start + length;
  if (end > 1440 || state.days[day].some(item => item.id !== editingId && start < minutes(item.time) + item.duration && end > minutes(item.time))) {
    $('#activity-error').textContent = end > 1440 ? '这张卡跨过了午夜，请拆分为两天。' : '这段时间已有安排，请调整开始时间或停留时长。'; return;
  }
  const existing = state.days[day].find(item => item.id === editingId);
  const item = { id: editingId || crypto.randomUUID(), title, time, duration: length, note: String(data.get('note')).trim(), english: existing?.english || 'A moment of your own', color: existing?.color || 'blue', locked: existing?.locked || false, done: existing?.done || false };
  state.days[day] = [...state.days[day].filter(x => x.id !== editingId), item].sort((a, b) => minutes(a.time) - minutes(b.time));
  save(); $('#activity-dialog').close(); expanded = item.id; render(); toast('行程已保存到本机');
});
$('#preferences-form').addEventListener('submit', (event) => {
  event.preventDefault(); const data = new FormData(event.target);
  const prefs = { people: Number(data.get('people')), budget: Number(data.get('budget')), pace: data.get('pace'), style: data.get('style'), stay: data.get('stay') };
  const days = structuredClone(state.days);
  const alternatives = { culture: ['中国茶叶博物馆', 'China National Tea Museum', '文化体验：茶文化与展览，开放信息待确认。'], classic: ['雷峰塔', 'Leifeng Pagoda', '经典必去：湖光与塔影，门票另行确认。'], nature: ['茅家埠', 'A quiet corner of West Lake', '自然漫游：在树荫与湖边散散步。'] };
  const candidate = days[0].find(item => item.id === 'a3' && !item.locked && !item.done);
  const changes = [];
  if (candidate) {
    const choice = alternatives[prefs.style]; candidate.title = choice[0]; candidate.english = choice[1]; candidate.note = choice[2];
    const nextStart = Math.min(1440, ...days[0].filter(x => minutes(x.time) > minutes(candidate.time)).map(x => minutes(x.time)));
    candidate.duration = Math.min(prefs.pace === 'relaxed' ? 120 : prefs.pace === 'balanced' ? 90 : 60, nextStart - minutes(candidate.time));
    changes.push(`第 1 天 14:00 安排为「${candidate.title}」，停留 ${duration(candidate.duration)}。`);
  } else changes.push('下午安排已锁定、已完成或已删除，本次保持不变。');
  changes.push(`规划人数 ${prefs.people} 人，旅行总预算 ¥${prefs.budget.toLocaleString('zh-CN')}。账本仍由现有 4 位成员分摊。`);
  changes.push(`住宿偏好：${{ lake: '西湖周边', metro: '地铁附近', quiet: '安静街区' }[prefs.stay]}。已锁定酒店不会被替换。`);
  proposal = { prefs, days };
  $('#proposal').hidden = false;
  $('#proposal').innerHTML = `<h3>行程调整预览</h3><ul>${changes.map(text => `<li>${esc(text)}</li>`).join('')}</ul><p class="form-note">这是本地规则演示，不含实时搜索、价格校验或真实 Agent 调用。预算与住宿偏好已记录，暂未据此查找酒店和核算票价。</p><button class="primary-button" data-action="accept-proposal">采纳这份草案</button>`;
  $('#proposal').scrollIntoView({ block: 'nearest', behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth' });
});
document.addEventListener('change', async (event) => {
  if (event.target.id !== 'photo-upload') return;
  const selectedType = mediaType;
  let rejected = 0;
  for (const file of event.target.files) {
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 10 * 1024 * 1024) { rejected++; continue; }
    const url = URL.createObjectURL(file);
    const valid = await new Promise(resolve => { const image = new Image(); image.onload = () => resolve(image.naturalWidth * image.naturalHeight <= 40000000); image.onerror = () => resolve(false); image.src = url; });
    if (!valid) { URL.revokeObjectURL(url); rejected++; continue; }
    photos.push({ id: crypto.randomUUID(), url, name: file.name, type: selectedType });
  }
  render(); toast(rejected ? `${rejected} 张图片因格式、大小或像素限制未加入` : '已添加本地预览，未上传或执行水印');
});
window.addEventListener('hashchange', () => { const next = location.hash.slice(1); if (['itinerary', 'ledger', 'media'].includes(next)) { view = next; render(); } });
render();
