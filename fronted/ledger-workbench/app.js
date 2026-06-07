const storageKeys = {
  baseUrl: "ledger-workbench.base-url",
  token: "ledger-workbench.access-token",
  refreshToken: "ledger-workbench.refresh-token",
  context: "ledger-workbench.context",
  history: "ledger-workbench.history",
};

const state = {
  query: "",
  method: "ALL",
  history: [],
};

const contextSchema = [
  ["bookId", "账本 ID"],
  ["billId", "账单 ID"],
  ["requestId", "申请 ID"],
  ["invitationId", "邀请 ID"],
  ["memberId", "成员 ID"],
  ["categoryId", "分类 ID"],
  ["tempParticipantId", "临时参与人 ID"],
  ["settlementBatchId", "结算批次 ID"],
  ["paymentConfirmId", "确认单 ID"],
  ["fileName", "文件名"],
];

const sections = [
  {
    id: "system",
    title: "System",
    description: "系统可用性检查。学习阶段先用它确认后端、CORS 和本地环境是否正常。",
    endpoints: [
      { title: "Ping", method: "GET", path: "/api/v1/ping", note: "最轻量的活性探针。" },
      { title: "Health", method: "GET", path: "/api/v1/health", note: "查看后端健康状态。" },
    ],
  },
  {
    id: "auth",
    title: "Auth",
    description: "认证主线。建议按 `mock users -> wechat login -> users/me -> refresh token` 的顺序学。",
    endpoints: [
      { title: "List Mock Users", method: "GET", path: "/api/v1/auth/dev/mock-users", note: "开发环境拉取 mock 登录候选用户。" },
      {
        title: "Wechat Login",
        method: "POST",
        path: "/api/v1/auth/wechat-login",
        note: "成功后会自动写入 access token 和 refresh token。",
        autoStoreAuth: true,
        body: { code: "mock-user-001", confirmReRegister: false },
      },
      {
        title: "Bind Wechat Mobile",
        method: "POST",
        path: "/api/v1/auth/bind-wechat-mobile",
        note: "开发环境可直接走 manualMobile。",
        autoStoreAuth: true,
        body: { nickname: "测试用户", avatarUrl: "", phoneCode: "", manualMobile: "13800138000" },
      },
      {
        title: "Refresh Token",
        method: "POST",
        path: "/api/v1/auth/refresh-token",
        note: "使用 refresh token 静默续签。",
        useStoredRefreshToken: true,
        autoStoreAuth: true,
        body: { refreshToken: "" },
      },
    ],
  },
  {
    id: "user",
    title: "User",
    description: "当前用户信息与自我管理。",
    endpoints: [
      { title: "Current User", method: "GET", path: "/api/v1/users/me", note: "查看当前登录用户资料。" },
      {
        title: "Update Current User",
        method: "PUT",
        path: "/api/v1/users/me",
        note: "开发态推荐只改昵称和 manualMobile。",
        body: { nickname: "新的昵称", avatarUrl: "", phoneCode: "", manualMobile: "13800138000" },
      },
      { title: "Cancel Current User", method: "POST", path: "/api/v1/users/me/cancel", note: "注销当前用户，学习时谨慎执行。" },
    ],
  },
  {
    id: "books",
    title: "Books",
    description: "账本主域。后续大部分业务都从 bookId 出发。",
    endpoints: [
      {
        title: "Create Book",
        method: "POST",
        path: "/api/v1/books",
        note: "建议先创建一个最小账本拿到 bookId。",
        body: { name: "学习账本", coverUrl: "", description: "用于代码走读和接口调试" },
      },
      { title: "My Books", method: "GET", path: "/api/v1/books/my", note: "拉取我参与的账本列表。" },
      { title: "Book Detail", method: "GET", path: "/api/v1/books/:bookId", note: "需要先填写左侧 bookId。" },
      {
        title: "Update Book",
        method: "PUT",
        path: "/api/v1/books/:bookId",
        note: "更新账本元信息。",
        body: { name: "学习账本-更新", coverUrl: "", description: "更新后的描述" },
      },
      { title: "Delete Book", method: "DELETE", path: "/api/v1/books/:bookId", note: "删除账本。" },
    ],
  },
  {
    id: "members",
    title: "Members",
    description: "账本成员治理，包括退出、移除、设管理员和转移所有者。",
    endpoints: [
      { title: "List Members", method: "GET", path: "/api/v1/books/:bookId/members", note: "查看账本成员列表。" },
      { title: "Quit Book", method: "POST", path: "/api/v1/books/:bookId/quit", note: "当前用户退出账本。" },
      { title: "Remove Member", method: "POST", path: "/api/v1/books/:bookId/members/:memberId/remove", note: "移除成员。" },
      { title: "Set Admin", method: "POST", path: "/api/v1/books/:bookId/members/:memberId/set-admin", note: "设为管理员。" },
      { title: "Cancel Admin", method: "POST", path: "/api/v1/books/:bookId/members/:memberId/cancel-admin", note: "取消管理员。" },
      {
        title: "Transfer Owner",
        method: "POST",
        path: "/api/v1/books/:bookId/transfer-owner",
        note: "转移账本所有权。",
        body: { targetMemberId: 0 },
      },
    ],
  },
  {
    id: "invitations",
    title: "Invitations",
    description: "账本邀请链路，覆盖创建、收发、接受、拒绝、撤销。",
    endpoints: [
      {
        title: "Create Invitation",
        method: "POST",
        path: "/api/v1/books/:bookId/invitations",
        note: "邀请新成员进入账本。",
        body: { targetUserId: 0, remark: "欢迎加入" },
      },
      { title: "Invitation Candidates", method: "GET", path: "/api/v1/books/:bookId/invitation-candidates", note: "查看可邀请用户候选列表。" },
      { title: "Pending Invitations", method: "GET", path: "/api/v1/invitations/pending", note: "我待处理的邀请。" },
      { title: "Received Invitations", method: "GET", path: "/api/v1/invitations/received", note: "我收到的邀请。" },
      { title: "Sent Invitations", method: "GET", path: "/api/v1/invitations/sent", note: "我发出的邀请。" },
      { title: "Accept Invitation", method: "POST", path: "/api/v1/invitations/:invitationId/accept", note: "接受邀请。" },
      { title: "Reject Invitation", method: "POST", path: "/api/v1/invitations/:invitationId/reject", note: "拒绝邀请。" },
      { title: "Revoke Invitation", method: "POST", path: "/api/v1/invitations/:invitationId/revoke", note: "撤销邀请。" },
    ],
  },
  {
    id: "categories",
    title: "Categories",
    description: "账本分类管理。账单、统计都会依赖它。",
    endpoints: [
      { title: "List Categories", method: "GET", path: "/api/v1/books/:bookId/categories", note: "查看分类列表。" },
      {
        title: "Create Category",
        method: "POST",
        path: "/api/v1/books/:bookId/categories",
        note: "创建分类。",
        body: { name: "交通", icon: "car", color: "#3b82f6", budget: 0 },
      },
      {
        title: "Update Category",
        method: "PUT",
        path: "/api/v1/books/:bookId/categories/:categoryId",
        note: "更新分类。",
        body: { name: "交通-更新", icon: "car", color: "#2563eb", budget: 0 },
      },
      { title: "Disable Category", method: "POST", path: "/api/v1/books/:bookId/categories/:categoryId/disable", note: "禁用分类。" },
    ],
  },
  {
    id: "temp-participants",
    title: "Temp Participants",
    description: "临时参与人是这个项目里很有业务味的一块，值得重点学。",
    endpoints: [
      { title: "List Temp Participants", method: "GET", path: "/api/v1/books/:bookId/temp-participants", note: "查看临时参与人列表。" },
      {
        title: "Create Temp Participant",
        method: "POST",
        path: "/api/v1/books/:bookId/temp-participants",
        note: "创建临时参与人。",
        body: { nickname: "路人甲", attachedMemberId: null },
      },
      {
        title: "Update Temp Nickname",
        method: "PUT",
        path: "/api/v1/books/:bookId/temp-participants/:tempParticipantId/nickname",
        note: "修改昵称。",
        body: { nickname: "临时参与人-更新" },
      },
      {
        title: "Change Attached Member",
        method: "PUT",
        path: "/api/v1/books/:bookId/temp-participants/:tempParticipantId/attached-member",
        note: "更换挂靠正式成员。",
        body: { attachedMemberId: 0 },
      },
      { title: "Delete Temp Participant", method: "DELETE", path: "/api/v1/books/:bookId/temp-participants/:tempParticipantId", note: "删除临时参与人。" },
      { title: "Enable Temp Participant", method: "POST", path: "/api/v1/books/:bookId/temp-participants/:tempParticipantId/enable", note: "重新启用临时参与人。" },
    ],
  },
  {
    id: "bills",
    title: "Bills",
    description: "账单核心域，包括个人账单、共享支出、更新、删除和单人结清。",
    endpoints: [
      { title: "List Bills", method: "GET", path: "/api/v1/books/:bookId/bills", note: "拉取账单列表。" },
      { title: "Bill Detail", method: "GET", path: "/api/v1/books/:bookId/bills/:billId", note: "查看账单详情。" },
      {
        title: "Create Personal Bill",
        method: "POST",
        path: "/api/v1/books/:bookId/bills/personal-bill",
        note: "创建个人账单。",
        body: { title: "个人消费", amount: 88.5, categoryId: 0, billDate: "2026-05-18", remark: "" },
      },
      {
        title: "Create Shared Expense",
        method: "POST",
        path: "/api/v1/books/:bookId/bills/shared-expense",
        note: "创建共享支出。",
        body: { title: "聚餐", amount: 300, categoryId: 0, billDate: "2026-05-18", payerMemberId: 0, shareItems: [] },
      },
      {
        title: "Update Bill",
        method: "PUT",
        path: "/api/v1/books/:bookId/bills/:billId",
        note: "更新账单。",
        body: { title: "账单更新", amount: 120, categoryId: 0, billDate: "2026-05-18", remark: "调整后" },
      },
      { title: "Delete Bill", method: "DELETE", path: "/api/v1/books/:bookId/bills/:billId", note: "删除账单。" },
      { title: "Settle Bill Participant", method: "POST", path: "/api/v1/books/:bookId/bills/:billId/participants/:memberId/settle", note: "结清某个参与成员的账单份额。" },
    ],
  },
  {
    id: "bill-requests",
    title: "Bill Requests",
    description: "账单变更申请流，适合用来理解 application service 如何编排复杂流程。",
    endpoints: [
      {
        title: "Create Modify Request",
        method: "POST",
        path: "/api/v1/books/:bookId/bills/:billId/modify-requests",
        note: "发起修改申请。",
        body: { title: "改价申请", amount: 199, remark: "申请修改" },
      },
      {
        title: "Create Delete Request",
        method: "POST",
        path: "/api/v1/books/:bookId/bills/:billId/delete-requests",
        note: "发起删除申请。",
        body: { reason: "误记账单" },
      },
      { title: "My Bill Requests", method: "GET", path: "/api/v1/books/:bookId/bill-requests/my", note: "我发起的申请。" },
      { title: "Pending Approve Requests", method: "GET", path: "/api/v1/books/:bookId/bill-requests/pending-approve", note: "待我审批的申请。" },
      { title: "Bill Change Requests", method: "GET", path: "/api/v1/books/:bookId/bills/:billId/change-requests", note: "某账单的申请历史。" },
      { title: "Bill Request Detail", method: "GET", path: "/api/v1/books/:bookId/bill-requests/:requestId", note: "申请详情。" },
      { title: "Approve Request", method: "POST", path: "/api/v1/books/:bookId/bill-requests/:requestId/approve", note: "通过申请。" },
      {
        title: "Reject Request",
        method: "POST",
        path: "/api/v1/books/:bookId/bill-requests/:requestId/reject",
        note: "拒绝申请。",
        body: { reason: "驳回原因" },
      },
      { title: "Cancel Request", method: "POST", path: "/api/v1/books/:bookId/bill-requests/:requestId/cancel", note: "撤销申请。" },
    ],
  },
  {
    id: "settlements",
    title: "Settlements",
    description: "结算批次管理，帮助你理解账本维度的债务汇总。",
    endpoints: [
      {
        title: "Create Settlement",
        method: "POST",
        path: "/api/v1/books/:bookId/settlements",
        note: "创建结算批次。",
        body: { remark: "阶段结算" },
      },
      { title: "Settlement Detail", method: "GET", path: "/api/v1/books/:bookId/settlements/:settlementBatchId", note: "查看结算批次详情。" },
      { title: "List Settlements", method: "GET", path: "/api/v1/books/:bookId/settlements", note: "查看结算批次列表。" },
    ],
  },
  {
    id: "payment-confirms",
    title: "Payment Confirms",
    description: "付款确认流。它是结算后续动作的一部分。",
    endpoints: [
      {
        title: "Create Payment Confirm",
        method: "POST",
        path: "/api/v1/books/:bookId/payment-confirms",
        note: "创建付款确认单。",
        body: { settlementBatchId: 0, payeeMemberId: 0, amount: 100, remark: "" },
      },
      { title: "Pending Receive", method: "GET", path: "/api/v1/books/:bookId/payment-confirms/pending-receive", note: "待我确认收款。" },
      { title: "Confirm Receive", method: "POST", path: "/api/v1/books/:bookId/payment-confirms/:paymentConfirmId/confirm", note: "确认收款。" },
      {
        title: "Direct Receive",
        method: "POST",
        path: "/api/v1/books/:bookId/payment-confirms/direct-receive",
        note: "直接登记已收款。",
        body: { payerMemberId: 0, payeeMemberId: 0, amount: 100, remark: "" },
      },
      { title: "Pending Pay", method: "GET", path: "/api/v1/books/:bookId/payment-confirms/pending-pay", note: "待我付款。" },
      {
        title: "Reject Receive",
        method: "POST",
        path: "/api/v1/books/:bookId/payment-confirms/:paymentConfirmId/reject",
        note: "拒绝确认。",
        body: { reason: "金额不一致" },
      },
      { title: "Payment History", method: "GET", path: "/api/v1/books/:bookId/payment-confirms/history", note: "历史记录。" },
    ],
  },
  {
    id: "temp-recoveries",
    title: "Temp Recoveries",
    description: "临时参与人回收链路。",
    endpoints: [
      {
        title: "Create Temp Recovery",
        method: "POST",
        path: "/api/v1/books/:bookId/temp-recoveries",
        note: "创建临时参与人回收记录。",
        body: { tempParticipantId: 0, attachedMemberId: 0, remark: "" },
      },
      { title: "List Temp Recoveries", method: "GET", path: "/api/v1/books/:bookId/temp-recoveries", note: "查看回收记录列表。" },
    ],
  },
  {
    id: "statistics",
    title: "Statistics",
    description: "统计视图。读这组接口时建议同时看 billing 和 settlement 模块。",
    endpoints: [
      { title: "Statistics Overview", method: "GET", path: "/api/v1/books/:bookId/statistics/overview", note: "统计总览。" },
      {
        title: "Update Budget",
        method: "PUT",
        path: "/api/v1/books/:bookId/statistics/budget",
        note: "更新预算。",
        body: { budgetAmount: 5000 },
      },
      { title: "Category Consumption", method: "GET", path: "/api/v1/books/:bookId/statistics/category-consumption", note: "分类消费统计。" },
      { title: "Member Relations", method: "GET", path: "/api/v1/books/:bookId/statistics/member-relations", note: "成员关系统计。" },
      { title: "Member Relations Details", method: "GET", path: "/api/v1/books/:bookId/statistics/member-relations/details", note: "成员关系明细。" },
      { title: "Attached Temp Details", method: "GET", path: "/api/v1/books/:bookId/statistics/attached-temp-details", note: "挂靠临时参与人统计。" },
    ],
  },
  {
    id: "exports",
    title: "Exports",
    description: "导出记录。适合后面补做导出历史和文件下载页。",
    endpoints: [
      {
        title: "Export Personal Detail",
        method: "POST",
        path: "/api/v1/books/:bookId/exports/personal-detail",
        note: "生成个人明细导出。",
        body: { memberId: 0 },
      },
      {
        title: "Export Book Summary",
        method: "POST",
        path: "/api/v1/books/:bookId/exports/book-summary",
        note: "生成账本汇总导出。",
        body: {},
      },
      { title: "List Exports", method: "GET", path: "/api/v1/books/:bookId/exports", note: "查看导出历史。" },
    ],
  },
  {
    id: "files",
    title: "Files",
    description: "文件上传和回显。当前工作台先保留接口位，不在纯静态页里做 multipart。",
    endpoints: [
      { title: "Upload File", method: "POST", path: "/api/v1/files/upload", note: "multipart 上传，后续单独补做。", disabled: true },
      { title: "Delete File", method: "DELETE", path: "/api/v1/files/billing/:fileName", note: "删除账单附件。" },
      { title: "Preview Billing File", method: "GET", path: "/api/v1/files/billing/:fileName", note: "预览附件。" },
    ],
  },
];

const workflow = [
  ["System", "Ping"],
  ["Auth", "List Mock Users"],
  ["Auth", "Wechat Login"],
  ["User", "Current User"],
  ["Books", "Create Book"],
  ["Books", "My Books"],
  ["Books", "Book Detail"],
];

const els = {};

document.addEventListener("DOMContentLoaded", () => {
  collectElements();
  renderContextFields();
  loadSession();
  loadHistory();
  renderOverview();
  renderWorkflow();
  renderSidebar();
  renderSections();
  renderHistory();
  updateContextHealth();
  bindEvents();
  applyFilters();
});

function collectElements() {
  els.baseUrl = document.getElementById("base-url");
  els.accessToken = document.getElementById("access-token");
  els.refreshToken = document.getElementById("refresh-token");
  els.loginCode = document.getElementById("login-code");
  els.loginPayload = document.getElementById("login-payload");
  els.mockUsers = document.getElementById("mock-users");
  els.responseOutput = document.getElementById("response-output");
  els.responseMeta = document.getElementById("response-meta");
  els.sections = document.getElementById("sections");
  els.sidebarNav = document.getElementById("sidebar-nav");
  els.contextFields = document.getElementById("context-fields");
  els.search = document.getElementById("endpoint-search");
  els.resultCount = document.getElementById("result-count");
  els.metricEndpoints = document.getElementById("metric-endpoints");
  els.metricSections = document.getElementById("metric-sections");
  els.metricContext = document.getElementById("metric-context");
  els.metricStatus = document.getElementById("metric-status");
  els.workflowSteps = document.getElementById("workflow-steps");
  els.contextHealth = document.getElementById("context-health");
  els.copyResponse = document.getElementById("copy-response");
  els.clearHistory = document.getElementById("clear-history");
  els.requestHistory = document.getElementById("request-history");
}

function bindEvents() {
  document.getElementById("save-session").addEventListener("click", saveSession);
  document.getElementById("clear-session").addEventListener("click", clearSession);
  document.getElementById("quick-ping").addEventListener("click", () => runQuick({ method: "GET", path: "/api/v1/ping" }));
  document.getElementById("quick-mock-users").addEventListener("click", loadMockUsers);
  document.getElementById("quick-me").addEventListener("click", () => runQuick({ method: "GET", path: "/api/v1/users/me" }));
  document.getElementById("quick-login").addEventListener("click", quickLogin);
  document.getElementById("quick-refresh").addEventListener("click", quickRefresh);
  els.copyResponse.addEventListener("click", copyResponse);
  els.clearHistory.addEventListener("click", clearHistory);
  [els.baseUrl, els.accessToken, els.refreshToken].forEach((input) => {
    input.addEventListener("change", updateContextHealth);
  });
  els.search.addEventListener("input", (event) => {
    state.query = event.target.value.trim().toLowerCase();
    applyFilters();
  });

  document.querySelectorAll("[data-method-filter]").forEach((button) => {
    button.addEventListener("click", (event) => {
      state.method = event.currentTarget.getAttribute("data-method-filter") || "ALL";
      document.querySelectorAll("[data-method-filter]").forEach((item) => item.classList.remove("active"));
      event.currentTarget.classList.add("active");
      applyFilters();
    });
  });

  window.addEventListener("scroll", syncActiveNav);
}

function renderOverview() {
  const endpointCount = sections.reduce((sum, section) => sum + section.endpoints.length, 0);
  const contextKeys = new Set();

  sections.forEach((section) => {
    section.endpoints.forEach((endpoint) => {
      getPathVariables(endpoint.path).forEach((key) => contextKeys.add(key));
    });
  });

  els.metricEndpoints.textContent = String(endpointCount);
  els.metricSections.textContent = String(sections.length);
  els.metricContext.textContent = String(contextKeys.size);
}

function renderWorkflow() {
  els.workflowSteps.innerHTML = workflow
    .map(
      ([domain, endpoint], index) => `
        <li>
          <span>${String(index + 1).padStart(2, "0")} · ${escapeHtml(domain)}</span>
          <strong>${escapeHtml(endpoint)}</strong>
        </li>
      `
    )
    .join("");
}

function renderContextFields() {
  const saved = getStoredContext();
  els.contextFields.innerHTML = contextSchema
    .map(([key, label]) => {
      const value = saved[key] || "";
      return `
        <label class="field">
          <span>${label}</span>
          <input data-context-key="${key}" type="text" value="${escapeHtml(value)}" />
        </label>
      `;
    })
    .join("");

  els.contextFields.querySelectorAll("input").forEach((input) => {
    input.addEventListener("change", () => {
      saveSession();
      updateContextHealth();
    });
  });
}

function renderSidebar() {
  els.sidebarNav.innerHTML = sections
    .map(
      (section) => `
        <a class="nav-item" href="#${section.id}">
          <span>${section.title}</span>
          <span class="nav-item-count">${section.endpoints.length}</span>
        </a>
      `
    )
    .join("");
}

function renderSections() {
  els.sections.innerHTML = sections
    .map(
      (section) => `
        <section id="${section.id}" class="section-card">
          <h2>${section.title}</h2>
          <p class="section-desc">${section.description}</p>
          <div class="endpoint-grid">
            ${section.endpoints.map((endpoint, index) => renderEndpointCard(section.id, endpoint, index)).join("")}
          </div>
        </section>
      `
    )
    .join("");

  els.sections.querySelectorAll("[data-endpoint-run]").forEach((button) => {
    button.addEventListener("click", async (event) => {
      const key = event.currentTarget.getAttribute("data-endpoint-run");
      const endpoint = findEndpointByKey(key);
      if (!endpoint) {
        return;
      }

      await runEndpoint(endpoint, key);
    });
  });

  els.sections.querySelectorAll("[data-endpoint-copy]").forEach((button) => {
    button.addEventListener("click", async (event) => {
      const key = event.currentTarget.getAttribute("data-endpoint-copy");
      const endpoint = findEndpointByKey(key);
      if (!endpoint) {
        return;
      }

      await copyCurl(endpoint, key);
    });
  });
}

function renderEndpointCard(sectionId, endpoint, index) {
  const key = `${sectionId}-${index}`;
  const body = endpoint.body ? JSON.stringify(endpoint.body, null, 2) : "";

  return `
    <article class="endpoint-card" data-endpoint-card data-method="${endpoint.method}" data-search="${escapeHtml(
      `${sectionId} ${endpoint.title} ${endpoint.path} ${endpoint.note}`.toLowerCase()
    )}">
      <div class="endpoint-head">
        <div>
          <h3>${endpoint.title}</h3>
          <div class="endpoint-path">${escapeHtml(endpoint.path)}</div>
        </div>
        <span class="endpoint-method ${endpoint.method}">${endpoint.method}</span>
      </div>
      <p class="endpoint-note">${endpoint.note}</p>
      ${
        endpoint.method === "GET" || endpoint.method === "DELETE"
          ? ""
          : `
            <div class="endpoint-body">
              <label class="field">
                <span>请求体</span>
                <textarea id="body-${key}" rows="8">${escapeHtml(body)}</textarea>
              </label>
            </div>
          `
      }
      <div class="endpoint-actions">
        <span class="context-hint">${renderContextHint(endpoint.path)}</span>
        <div class="button-row">
          <button class="ghost" data-endpoint-copy="${key}" ${endpoint.disabled ? "disabled" : ""}>⧉ cURL</button>
          <button data-endpoint-run="${key}" ${endpoint.disabled ? "disabled" : ""}>→ 运行</button>
        </div>
      </div>
    </article>
  `;
}

function findEndpointByKey(key) {
  const separatorIndex = key.lastIndexOf("-");
  const sectionId = key.slice(0, separatorIndex);
  const rawIndex = key.slice(separatorIndex + 1);
  const section = sections.find((item) => item.id === sectionId);
  if (!section) {
    return null;
  }

  return section.endpoints[Number(rawIndex)];
}

async function runEndpoint(endpoint, key) {
  try {
    let body;
    if (endpoint.method !== "GET" && endpoint.method !== "DELETE") {
      const textarea = document.getElementById(`body-${key}`);
      body = textarea?.value?.trim() ? JSON.parse(textarea.value) : undefined;
    }

    if (endpoint.useStoredRefreshToken) {
      body = body || {};
      body.refreshToken = els.refreshToken.value.trim();
    }

    const response = await request({
      method: endpoint.method,
      path: endpoint.path,
      body,
    });

    if (endpoint.autoStoreAuth) {
      tryStoreAuth(response);
    }
  } catch (error) {
    recordHistory({
      status: "ERR",
      method: endpoint.method,
      path: applyContext(endpoint.path),
      ok: false,
    });
    writeResponse({
      meta: `运行失败 · ${endpoint.method} ${endpoint.path}`,
      payload: { error: error.message },
    });
  }
}

async function runQuick(endpoint) {
  try {
    await request(endpoint);
  } catch (error) {
    recordHistory({
      status: "ERR",
      method: endpoint.method,
      path: applyContext(endpoint.path),
      ok: false,
    });
    writeResponse({
      meta: `运行失败 · ${endpoint.method} ${endpoint.path}`,
      payload: { error: error.message },
    });
  }
}

async function quickLogin() {
  try {
    const payload = JSON.parse(els.loginPayload.value);
    payload.code = els.loginCode.value.trim() || payload.code;
    const response = await request({
      method: "POST",
      path: "/api/v1/auth/wechat-login",
      body: payload,
    });
    tryStoreAuth(response);
  } catch (error) {
    recordHistory({
      status: "ERR",
      method: "POST",
      path: "/api/v1/auth/wechat-login",
      ok: false,
    });
    writeResponse({
      meta: "快速登录失败",
      payload: { error: error.message },
    });
  }
}

async function quickRefresh() {
  try {
    const response = await request({
      method: "POST",
      path: "/api/v1/auth/refresh-token",
      body: { refreshToken: els.refreshToken.value.trim() },
    });
    tryStoreAuth(response);
  } catch (error) {
    recordHistory({
      status: "ERR",
      method: "POST",
      path: "/api/v1/auth/refresh-token",
      ok: false,
    });
    writeResponse({
      meta: "刷新 Token 失败",
      payload: { error: error.message },
    });
  }
}

async function loadMockUsers() {
  try {
    const response = await request({
      method: "GET",
      path: "/api/v1/auth/dev/mock-users",
    });

    const users = response?.data || [];
    if (!Array.isArray(users) || users.length === 0) {
      els.mockUsers.innerHTML = '<div class="empty">当前没有可用 mock 用户。</div>';
      return;
    }

    els.mockUsers.innerHTML = users
      .map(
        (user) => `
          <div class="mock-user">
            <strong>${escapeHtml(user.nickname || "未命名用户")}</strong>
            <div class="mock-user-meta">
              code: ${escapeHtml(user.suggestedCode || "")}<br />
              userId: ${escapeHtml(String(user.userId ?? ""))}<br />
              mobile: ${escapeHtml(user.mobileMasked || "未绑定")}
            </div>
            <div class="button-row">
              <button class="secondary" data-mock-code="${escapeHtml(user.suggestedCode || "")}">填入登录</button>
            </div>
          </div>
        `
      )
      .join("");

    els.mockUsers.querySelectorAll("[data-mock-code]").forEach((button) => {
      button.addEventListener("click", (event) => {
        const code = event.currentTarget.getAttribute("data-mock-code");
        els.loginCode.value = code || "";

        try {
          const payload = JSON.parse(els.loginPayload.value);
          payload.code = code || "";
          els.loginPayload.value = JSON.stringify(payload, null, 2);
        } catch (_error) {
          els.loginPayload.value = JSON.stringify({ code: code || "", confirmReRegister: false }, null, 2);
        }
      });
    });
  } catch (error) {
    recordHistory({
      status: "ERR",
      method: "GET",
      path: "/api/v1/auth/dev/mock-users",
      ok: false,
    });
    writeResponse({
      meta: "拉取 mock 用户失败",
      payload: { error: error.message },
    });
  }
}

async function request({ method, path, body }) {
  const finalPath = applyContext(path);
  const url = `${els.baseUrl.value.trim()}${finalPath}`;
  const headers = {};
  const token = els.accessToken.value.trim();

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let payloadBody;
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    payloadBody = JSON.stringify(body);
  }

  const response = await fetch(url, {
    method,
    headers,
    body: payloadBody,
  });

  const text = await response.text();
  let json;
  try {
    json = text ? JSON.parse(text) : null;
  } catch (_error) {
    json = text;
  }

  writeResponse({
    meta: `${response.status} ${method} ${finalPath}`,
    payload: json,
  });
  recordHistory({
    status: response.status,
    method,
    path: finalPath,
    ok: response.ok,
  });

  return json;
}

function applyContext(path) {
  const context = collectContext();
  return path.replace(/:([A-Za-z0-9_]+)/g, (_, key) => {
    const value = context[key];
    return value ? encodeURIComponent(value) : `:${key}`;
  });
}

function getPathVariables(path) {
  return Array.from(path.matchAll(/:([A-Za-z0-9_]+)/g)).map((match) => match[1]);
}

function renderContextHint(path) {
  const variables = getPathVariables(path);
  if (variables.length === 0) {
    return "无需路径变量";
  }

  return `需要 ${variables.join(", ")}`;
}

async function copyCurl(endpoint, key) {
  let body;
  if (endpoint.method !== "GET" && endpoint.method !== "DELETE") {
    const textarea = document.getElementById(`body-${key}`);
    body = textarea?.value?.trim();
  }

  const finalPath = applyContext(endpoint.path);
  const url = `${els.baseUrl.value.trim()}${finalPath}`;
  const parts = [`curl -X ${endpoint.method} '${url}'`];
  const token = els.accessToken.value.trim();

  if (token) {
    parts.push(`-H 'Authorization: Bearer ${token}'`);
  }

  if (body) {
    parts.push("-H 'Content-Type: application/json'");
    parts.push(`-d '${body.replaceAll("'", "'\\''")}'`);
  }

  await navigator.clipboard.writeText(parts.join(" \\\n  "));
  writeResponse({
    meta: `已复制 cURL · ${endpoint.method} ${finalPath}`,
    payload: { copied: true, url },
  });
}

function collectContext() {
  const context = {};
  els.contextFields.querySelectorAll("[data-context-key]").forEach((input) => {
    const key = input.getAttribute("data-context-key");
    context[key] = input.value.trim();
  });
  return context;
}

function saveSession() {
  localStorage.setItem(storageKeys.baseUrl, els.baseUrl.value.trim());
  localStorage.setItem(storageKeys.token, els.accessToken.value.trim());
  localStorage.setItem(storageKeys.refreshToken, els.refreshToken.value.trim());
  localStorage.setItem(storageKeys.context, JSON.stringify(collectContext()));
  writeResponse({
    meta: "本地会话已保存",
    payload: {
      baseUrl: els.baseUrl.value.trim(),
      context: collectContext(),
    },
  });
  updateContextHealth();
}

function clearSession() {
  localStorage.removeItem(storageKeys.baseUrl);
  localStorage.removeItem(storageKeys.token);
  localStorage.removeItem(storageKeys.refreshToken);
  localStorage.removeItem(storageKeys.context);

  els.baseUrl.value = "http://127.0.0.1:8080";
  els.accessToken.value = "";
  els.refreshToken.value = "";
  els.contextFields.querySelectorAll("[data-context-key]").forEach((input) => {
    input.value = "";
  });

  writeResponse({
    meta: "本地会话已清空",
    payload: {},
  });
  updateContextHealth();
}

function loadSession() {
  els.baseUrl.value = localStorage.getItem(storageKeys.baseUrl) || "http://127.0.0.1:8080";
  els.accessToken.value = localStorage.getItem(storageKeys.token) || "";
  els.refreshToken.value = localStorage.getItem(storageKeys.refreshToken) || "";
}

function getStoredContext() {
  try {
    return JSON.parse(localStorage.getItem(storageKeys.context) || "{}");
  } catch (_error) {
    return {};
  }
}

function tryStoreAuth(response) {
  const data = response?.data;
  if (!data) {
    return;
  }

  if (data.token) {
    els.accessToken.value = data.token;
    localStorage.setItem(storageKeys.token, data.token);
  } else if (data.bindToken) {
    els.accessToken.value = data.bindToken;
    localStorage.setItem(storageKeys.token, data.bindToken);
  }

  if (data.refreshToken) {
    els.refreshToken.value = data.refreshToken;
    localStorage.setItem(storageKeys.refreshToken, data.refreshToken);
  }

  updateContextHealth();
}

function writeResponse({ meta, payload }) {
  els.responseMeta.textContent = meta;
  els.responseOutput.textContent = typeof payload === "string" ? payload : JSON.stringify(payload, null, 2);
}

function updateContextHealth() {
  const hasBaseUrl = Boolean(els.baseUrl.value.trim());
  const hasToken = Boolean(els.accessToken.value.trim());
  const filledContextCount = Object.values(collectContext()).filter(Boolean).length;

  els.contextHealth.classList.remove("ready", "warn");
  if (hasBaseUrl && hasToken && filledContextCount > 0) {
    els.contextHealth.textContent = "可运行";
    els.contextHealth.classList.add("ready");
    return;
  }

  if (hasBaseUrl && (hasToken || filledContextCount > 0)) {
    els.contextHealth.textContent = "部分就绪";
    els.contextHealth.classList.add("warn");
    return;
  }

  els.contextHealth.textContent = "未配置";
}

function applyFilters() {
  let visibleCount = 0;

  document.querySelectorAll("[data-endpoint-card]").forEach((card) => {
    const methodMatches = state.method === "ALL" || card.getAttribute("data-method") === state.method;
    const searchText = card.getAttribute("data-search") || "";
    const queryMatches = !state.query || searchText.includes(state.query);
    const visible = methodMatches && queryMatches;

    card.classList.toggle("is-hidden", !visible);
    if (visible) {
      visibleCount += 1;
    }
  });

  document.querySelectorAll(".section-card").forEach((section) => {
    const hasVisibleEndpoint = Boolean(section.querySelector("[data-endpoint-card]:not(.is-hidden)"));
    section.classList.toggle("is-hidden", !hasVisibleEndpoint);
  });

  els.resultCount.textContent = state.query || state.method !== "ALL" ? `显示 ${visibleCount} 个匹配接口` : "显示全部接口";
}

function recordHistory(entry) {
  const item = {
    ...entry,
    time: new Date().toLocaleTimeString("zh-CN", { hour12: false }),
  };

  state.history = [item, ...state.history].slice(0, 10);
  localStorage.setItem(storageKeys.history, JSON.stringify(state.history));
  renderHistory();
  els.metricStatus.textContent = `${entry.status}`;
  els.metricStatus.classList.toggle("status-ok", entry.ok);
}

function loadHistory() {
  try {
    state.history = JSON.parse(localStorage.getItem(storageKeys.history) || "[]");
  } catch (_error) {
    state.history = [];
  }
}

function renderHistory() {
  if (state.history.length === 0) {
    els.requestHistory.innerHTML = '<div class="empty">还没有请求记录。</div>';
    return;
  }

  els.requestHistory.innerHTML = state.history
    .map(
      (item) => `
        <div class="history-item">
          <strong>${escapeHtml(item.status)} ${escapeHtml(item.method)} ${escapeHtml(item.path)}</strong>
          <span>${escapeHtml(item.time)} · ${item.ok ? "请求成功" : "需要检查"}</span>
        </div>
      `
    )
    .join("");
}

function clearHistory() {
  state.history = [];
  localStorage.removeItem(storageKeys.history);
  renderHistory();
}

async function copyResponse() {
  await navigator.clipboard.writeText(els.responseOutput.textContent || "");
  const previous = els.responseMeta.textContent;
  els.responseMeta.textContent = "响应已复制";
  window.setTimeout(() => {
    els.responseMeta.textContent = previous;
  }, 1200);
}

function syncActiveNav() {
  const anchors = Array.from(document.querySelectorAll(".section-card"));
  const active = anchors.find((section) => {
    const rect = section.getBoundingClientRect();
    return rect.top <= 140 && rect.bottom >= 140;
  });

  document.querySelectorAll(".nav-item").forEach((item) => {
    item.classList.remove("active");
  });

  if (!active) {
    return;
  }

  const current = document.querySelector(`.nav-item[href="#${active.id}"]`);
  current?.classList.add("active");
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}
