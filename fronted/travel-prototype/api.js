/* Same-origin transport. Business failures never fall back to demo data. */
window.ledgerApi = (() => {
  const key = 'trip-ledger-session-v1';
  let session = null, refreshPromise = null;
  try { session = JSON.parse(sessionStorage.getItem(key)); } catch {}
  function setSession(value) { session = value; if(value) sessionStorage.setItem(key, JSON.stringify(value)); else sessionStorage.removeItem(key); }
  async function request(path, { method = 'GET', body, retry = true, blob = false, idempotencyKey } = {}) {
    if (!path.startsWith('/api/v1/')) throw new Error('不支持的请求地址');
    const headers = {};
    if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey;
    if (session?.token) headers.Authorization = `Bearer ${session.token}`;
    if (body !== undefined && !(body instanceof FormData)) headers['Content-Type'] = 'application/json';
    const response = await fetch(path, { method, headers, body: body === undefined ? undefined : body instanceof FormData ? body : JSON.stringify(body), signal: AbortSignal.timeout(20000), cache: 'no-store' });
    if (blob && response.ok && !response.headers.get('content-type')?.includes('json')) return response.blob();
    let result; try { result = await response.json(); } catch { throw new Error('服务暂时不可用，请稍后重试'); }
    if ((response.status === 401 || result.code === 4002) && retry && session?.refreshToken) {
      refreshPromise ||= request('/api/v1/auth/refresh-token', { method:'POST', body:{refreshToken:session.refreshToken}, retry:false }).then(value=>setSession({...session,...value})).catch(error=>{setSession(null);throw error;}).finally(()=>{refreshPromise=null;});
      await refreshPromise; return request(path,{method,body,retry:false,blob,idempotencyKey});
    }
    if (!response.ok || result.code !== 0) { const error=new Error(result.message || `请求失败 (${response.status})`);error.code=result.code;throw error; }
    return result.data;
  }
  function pendingSlot(ownerId, bookId) {
    if (!Number.isSafeInteger(Number(ownerId)) || Number(ownerId)<=0 || !Number.isSafeInteger(Number(bookId)) || Number(bookId)<=0) throw new Error('无法确认待提交账单所属账户');
    return `trip-ledger-pending-v1:${ownerId}:${bookId}`;
  }
  function recoverBill(ownerId, bookId) {
    const raw = sessionStorage.getItem(pendingSlot(ownerId,bookId));
    if (!raw) return null;
    const pending = JSON.parse(raw);
    if (String(pending.ownerId)!==String(ownerId) || String(pending.bookId)!==String(bookId) ||
        ![`/api/v1/books/${bookId}/bills/personal-bill`,`/api/v1/books/${bookId}/bills/shared-expense`].includes(pending.path) ||
        !/^[A-Za-z0-9_-]{16,128}$/.test(pending.key) || !pending.body || typeof pending.body!=='object') throw new Error('待提交记录无效，请先核对账本');
    return pending;
  }
  function storePending(pending) {
    if (pending.ownerId === undefined) return; // Non-persistent API callers.
    const slot=pendingSlot(pending.ownerId,pending.bookId);
    const previous=recoverBill(pending.ownerId,pending.bookId);
    if(previous && (previous.key!==pending.key || JSON.stringify(previous.body)!==JSON.stringify(pending.body))) throw new Error('请先处理上次未确认的账单');
    sessionStorage.setItem(slot,JSON.stringify(pending)); // Fail closed before network when storage is unavailable.
  }
  function clearPending(pending) {
    if(pending.ownerId !== undefined) sessionStorage.removeItem(pendingSlot(pending.ownerId,pending.bookId));
  }
  // Only an explicit parameter rejection permits changing this submission.
  // Conflicts, authentication failures and transport failures may follow a committed request.
  async function submitBill(holder, pending = holder.pendingBill) {
    storePending(pending);
    holder.pendingBill = pending;
    try {
      const result = await request(pending.path, {method:'POST', body:pending.body, idempotencyKey:pending.key});
      clearPending(pending);
      holder.pendingBill = null;
      return result;
    } catch (error) {
      if (error.code === 4001) {
        clearPending(pending);
        holder.pendingBill = null;
        throw new Error('账单未被接受，请修改后重新提交。' + error.message);
      }
      throw new Error('提交结果未确认，请保留原内容重试或先核对账本。' + error.message);
    }
  }
  return { request, submitBill, recoverBill, setSession, get session(){return session;}, get:path=>request(path), post:(path,body={})=>request(path,{method:'POST',body}), put:(path,body)=>request(path,{method:'PUT',body}), delete:path=>request(path,{method:'DELETE'}),
    async image(url){ const parsed = new URL(url,location.origin); if(!parsed.pathname.startsWith('/api/v1/files/billing/')) throw new Error('凭据地址无效'); return URL.createObjectURL(await request(parsed.pathname,{blob:true})); }
  };
})();
