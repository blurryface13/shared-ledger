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
  return { request, setSession, get session(){return session;}, get:path=>request(path), post:(path,body={})=>request(path,{method:'POST',body}), put:(path,body)=>request(path,{method:'PUT',body}), delete:path=>request(path,{method:'DELETE'}),
    async image(url){ const parsed = new URL(url,location.origin); if(!parsed.pathname.startsWith('/api/v1/files/billing/')) throw new Error('凭据地址无效'); return URL.createObjectURL(await request(parsed.pathname,{blob:true})); }
  };
})();
