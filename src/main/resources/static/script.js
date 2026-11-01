// ================= Config =================

// Same server as the page, so this works locally and after deployment
const BASE_URL = '/api';
const AUTH_URL = `${BASE_URL}/auth`;
const ME_URL = `${BASE_URL}/users/me`;
const API_URL = `${BASE_URL}/applications`;
const COMPANY_API_URL = `${BASE_URL}/companies`;
const ROUND_API_URL = `${BASE_URL}/interview-rounds`;
const DOC_API_URL = `${BASE_URL}/documents`;

const TOKEN_KEY = 'jobTracker.token';
const USER_KEY = 'jobTracker.user';

// ================= State =================

let applications = [];
let companies = [];
let editingId = null;
let editingCompanyId = null;
let currentUser = null;

// ================= Elements =================

const authScreen = document.getElementById('authScreen');
const appScreen = document.getElementById('appScreen');
const authError = document.getElementById('authError');
const loginForm = document.getElementById('loginForm');
const registerForm = document.getElementById('registerForm');
const loginTab = document.getElementById('loginTab');
const registerTab = document.getElementById('registerTab');

const formSection = document.getElementById('addForm');
const form = document.getElementById('applicationForm');
const formTitle = document.getElementById('formTitle');
const formError = document.getElementById('formError');
const submitBtn = document.getElementById('submitBtn');
const list = document.getElementById('applicationsList');
const companySelect = document.getElementById('companySelect');
const newCompanyField = document.getElementById('newCompanyField');
const newCompanyName = document.getElementById('newCompanyName');
const searchInput = document.getElementById('searchInput');
const statusFilter = document.getElementById('statusFilter');
const sortBy = document.getElementById('sortBy');
const listViewBtn = document.getElementById('listViewBtn');
const boardViewBtn = document.getElementById('boardViewBtn');
const boardSection = document.getElementById('boardView');
const toast = document.getElementById('toast');

const accountPanel = document.getElementById('accountPanel');
const companiesPanel = document.getElementById('companiesPanel');
const companyForm = document.getElementById('companyForm');
const companyList = document.getElementById('companyList');
const companyError = document.getElementById('companyError');
const companySubmitBtn = document.getElementById('companySubmitBtn');
const companyCancelBtn = document.getElementById('companyCancelBtn');

// ================= Storage (token) =================

function getToken() {
  try { return localStorage.getItem(TOKEN_KEY); } catch (e) { return null; }
}

function saveSession(token, user) {
  try {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  } catch (e) { /* storage blocked: the session lasts until the page is closed */ }
  memoryToken = token;
  currentUser = user;
}

function clearSession() {
  try {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  } catch (e) { /* ignore */ }
  memoryToken = null;
  currentUser = null;
}

// Fallback when localStorage is not available
let memoryToken = null;
function currentToken() {
  return getToken() || memoryToken;
}

/** Reads the expiry time from the JWT (no secret needed to read it). */
function isTokenExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return !payload.exp || payload.exp * 1000 <= Date.now();
  } catch (e) {
    return true;
  }
}

// ================= Helpers =================

/**
 * Calls the backend and returns the parsed JSON.
 * Adds the login token. On 401 (token missing, wrong or expired) the user is logged out.
 * On other errors it throws an Error with the backend's message.
 */
async function api(url, options = {}) {
  const headers = {};
  if (options.body) headers['Content-Type'] = 'application/json';
  const token = currentToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(url, { ...options, headers });
  } catch (err) {
    throw new Error('Cannot reach the server. Is the app running?');
  }

  if (res.status === 204) return null;

  const data = await res.json().catch(() => null);
  if (!res.ok) {
    if (res.status === 401 && !url.startsWith(AUTH_URL)) {
      logout('Your session has expired. Please log in again.');
      throw new Error('Session expired');
    }
    let message = (data && data.message) || `Request failed (${res.status})`;
    if (data && data.errors) {
      message = Object.values(data.errors).join(' · ');
    }
    throw new Error(message);
  }
  return data;
}

/** Escapes text before putting it into innerHTML (prevents broken layout / script injection). */
function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/** Only allow http(s) links in href attributes. */
function safeUrl(url) {
  return /^https?:\/\//i.test(url || '') ? escapeHtml(url) : '#';
}

function formatDate(isoDate) {
  if (!isoDate) return 'No date';
  const [y, m, d] = isoDate.split('-');
  return new Date(y, m - 1, d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

function todayIso() {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 10);
}

let toastTimer;
function showToast(message, type = 'success') {
  toast.textContent = message;
  toast.className = `toast show ${type}`;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 3500);
}

function showError(el, message) {
  el.textContent = message;
  el.hidden = false;
}

function debounce(fn, delay) {
  let timer;
  return function (...args) {
    clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), delay);
  };
}

/** Builds the request body the backend expects (JobApplicationRequest). */
function toRequest(app) {
  return {
    companyId: app.companyId,
    role: app.role,
    dateApplied: app.dateApplied,
    status: app.status,
    jobLink: app.jobLink || null,
    location: app.location || null
  };
}

// ================= Login / sign up / logout =================

function showAuthTab(tab) {
  const isLogin = tab === 'login';
  loginForm.hidden = !isLogin;
  registerForm.hidden = isLogin;
  loginTab.classList.toggle('active', isLogin);
  registerTab.classList.toggle('active', !isLogin);
  authError.hidden = true;
  (isLogin ? document.getElementById('loginEmail') : document.getElementById('registerName')).focus();
}

loginTab.addEventListener('click', () => showAuthTab('login'));
registerTab.addEventListener('click', () => showAuthTab('register'));

function showAuthScreen(message) {
  appScreen.hidden = true;
  authScreen.hidden = false;
  loginForm.reset();
  registerForm.reset();
  showAuthTab('login');
  if (message) showError(authError, message);
}

function showApp() {
  authScreen.hidden = true;
  appScreen.hidden = false;
  document.getElementById('userName').textContent = currentUser ? currentUser.name : 'Account';
  loadCompanies();
  loadApplications();
}

async function submitAuth(url, body, button) {
  authError.hidden = true;
  button.disabled = true;
  try {
    const result = await api(url, { method: 'POST', body: JSON.stringify(body) });
    saveSession(result.token, result.user);
    showApp();
    showToast(`Welcome, ${result.user.name}!`);
  } catch (err) {
    showError(authError, err.message);
  } finally {
    button.disabled = false;
  }
}

loginForm.addEventListener('submit', function (e) {
  e.preventDefault();
  const email = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value;
  if (!email || !password) return showError(authError, 'Please enter your email and password.');
  submitAuth(`${AUTH_URL}/login`, { email, password }, loginForm.querySelector('button'));
});

registerForm.addEventListener('submit', function (e) {
  e.preventDefault();
  const name = document.getElementById('registerName').value.trim();
  const email = document.getElementById('registerEmail').value.trim();
  const password = document.getElementById('registerPassword').value;
  if (!name || !email || !password) return showError(authError, 'Please fill in all fields.');
  if (password.length < 8) return showError(authError, 'Password must be at least 8 characters.');
  submitAuth(`${AUTH_URL}/register`, { name, email, password }, registerForm.querySelector('button'));
});

function logout(message) {
  clearSession();
  applications = [];
  companies = [];
  editingId = null;
  closeForm();
  accountPanel.hidden = true;
  companiesPanel.hidden = true;
  list.innerHTML = '';
  showAuthScreen(message);
}

document.getElementById('logoutBtn').addEventListener('click', function () {
  logout();
  showToast('You have been logged out');
});

// ================= Panels (account, companies, form) =================

document.querySelectorAll('.close-btn').forEach(function (btn) {
  btn.addEventListener('click', function () {
    if (btn.dataset.close === 'addForm') closeForm();
    else if (btn.dataset.close === 'companiesPanel') { resetCompanyForm(); companiesPanel.hidden = true; }
    else document.getElementById(btn.dataset.close).hidden = true;
  });
});

function openPanel(panel) {
  panel.hidden = false;
  panel.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// ---------- Account ----------

document.getElementById('accountBtn').addEventListener('click', async function () {
  if (!accountPanel.hidden) { accountPanel.hidden = true; return; }
  document.getElementById('profileError').hidden = true;
  document.getElementById('passwordError').hidden = true;
  document.getElementById('passwordForm').reset();
  openPanel(accountPanel);
  try {
    const me = await api(ME_URL);
    document.getElementById('profileName').value = me.name;
    document.getElementById('profileEmail').value = me.email;
  } catch (err) {
    showError(document.getElementById('profileError'), err.message);
  }
});

document.getElementById('profileForm').addEventListener('submit', async function (e) {
  e.preventDefault();
  const errorEl = document.getElementById('profileError');
  errorEl.hidden = true;
  const name = document.getElementById('profileName').value.trim();
  const email = document.getElementById('profileEmail').value.trim();
  if (!name || !email) return showError(errorEl, 'Please enter your name and email.');

  try {
    const me = await api(ME_URL, { method: 'PUT', body: JSON.stringify({ name, email }) });
    saveSession(currentToken(), me);
    document.getElementById('userName').textContent = me.name;
    showToast('Profile saved');
  } catch (err) {
    showError(errorEl, err.message);
  }
});

document.getElementById('passwordForm').addEventListener('submit', async function (e) {
  e.preventDefault();
  const errorEl = document.getElementById('passwordError');
  errorEl.hidden = true;
  const currentPassword = document.getElementById('currentPassword').value;
  const newPassword = document.getElementById('newPassword').value;
  const confirmPassword = document.getElementById('confirmPassword').value;

  if (!currentPassword || !newPassword) return showError(errorEl, 'Please fill in all password fields.');
  if (newPassword.length < 8) return showError(errorEl, 'New password must be at least 8 characters.');
  if (newPassword !== confirmPassword) return showError(errorEl, 'New passwords do not match.');

  try {
    await api(`${ME_URL}/password`, { method: 'PUT', body: JSON.stringify({ currentPassword, newPassword }) });
    e.target.reset();
    showToast('Password changed');
  } catch (err) {
    showError(errorEl, err.message);
  }
});

// ---------- Companies ----------

document.getElementById('companiesBtn').addEventListener('click', function () {
  if (!companiesPanel.hidden) { resetCompanyForm(); companiesPanel.hidden = true; return; }
  resetCompanyForm();
  renderCompanyList();
  openPanel(companiesPanel);
});

function resetCompanyForm() {
  companyForm.reset();
  editingCompanyId = null;
  companySubmitBtn.textContent = 'Add company';
  companyCancelBtn.hidden = true;
  companyError.hidden = true;
}

companyCancelBtn.addEventListener('click', resetCompanyForm);

function renderCompanyList() {
  if (companies.length === 0) {
    companyList.innerHTML = '<p class="muted">No companies yet. Add one above or while adding an application.</p>';
    return;
  }
  companyList.innerHTML = companies.map(function (c) {
    const details = [
      c.industry ? escapeHtml(c.industry) : '',
      c.website ? `<a href="${safeUrl(c.website)}" target="_blank" rel="noopener noreferrer">${escapeHtml(c.website)}</a>` : ''
    ].filter(Boolean).join(' · ');
    return `<div class="company-row">
      <div>
        <strong>${escapeHtml(c.name)}</strong>
        ${details ? `<div class="muted">${details}</div>` : ''}
      </div>
      <span class="item-actions">
        <button type="button" class="company-edit-btn" data-id="${c.id}">Edit</button>
        <button type="button" class="company-delete-btn" data-id="${c.id}">Delete</button>
      </span>
    </div>`;
  }).join('');
}

companyList.addEventListener('click', async function (e) {
  const btn = e.target.closest('button');
  if (!btn) return;
  const id = Number(btn.dataset.id);
  const company = companies.find(function (c) { return c.id === id; });
  if (!company) return;

  if (btn.classList.contains('company-edit-btn')) {
    editingCompanyId = id;
    document.getElementById('companyName').value = company.name;
    document.getElementById('companyIndustry').value = company.industry || '';
    document.getElementById('companyWebsite').value = company.website || '';
    companySubmitBtn.textContent = 'Save changes';
    companyCancelBtn.hidden = false;
    companyError.hidden = true;
    document.getElementById('companyName').focus();
  }

  if (btn.classList.contains('company-delete-btn')) {
    if (!confirm(`Delete ${company.name}?`)) return;
    try {
      await api(`${COMPANY_API_URL}/${id}`, { method: 'DELETE' });
      if (editingCompanyId === id) resetCompanyForm();
      showToast('Company deleted');
      await loadCompanies();
    } catch (err) {
      showError(companyError, err.message);
    }
  }
});

companyForm.addEventListener('submit', async function (e) {
  e.preventDefault();
  companyError.hidden = true;
  const name = document.getElementById('companyName').value.trim();
  if (!name) return showError(companyError, 'Please enter the company name.');

  const body = {
    name: name,
    industry: document.getElementById('companyIndustry').value.trim() || null,
    website: document.getElementById('companyWebsite').value.trim() || null
  };

  try {
    if (editingCompanyId) {
      await api(`${COMPANY_API_URL}/${editingCompanyId}`, { method: 'PUT', body: JSON.stringify(body) });
      showToast('Company updated');
    } else {
      await api(COMPANY_API_URL, { method: 'POST', body: JSON.stringify(body) });
      showToast('Company added');
    }
    resetCompanyForm();
    await loadCompanies();
    loadApplications(); // company names on cards may have changed
  } catch (err) {
    showError(companyError, err.message);
  }
});

// ================= Loading data =================

async function loadApplications() {
  const params = new URLSearchParams();
  if (statusFilter.value !== 'All') params.set('status', statusFilter.value);
  if (searchInput.value.trim()) params.set('search', searchInput.value.trim());
  params.set('sort', sortBy.value);

  try {
    applications = await api(`${API_URL}?${params}`);
    renderApplications();
    renderBoard();
    loadStats();
  } catch (err) {
    if (err.message !== 'Session expired') {
      list.innerHTML = `<div class="empty-state"><p>Could not load applications</p><p>${escapeHtml(err.message)}</p></div>`;
    }
  }
}

async function loadStats() {
  try {
    const stats = await api(`${API_URL}/stats`);
    document.getElementById('statTotal').textContent = stats.total;
    document.getElementById('statInterview').textContent = stats.interviews;
    document.getElementById('statOffer').textContent = stats.offers;
    document.getElementById('statResponseRate').textContent = stats.responseRate + '%';
  } catch (err) {
    // Stats are not critical
  }
}

async function loadCompanies(selectedId) {
  try {
    companies = await api(COMPANY_API_URL);
  } catch (err) {
    if (err.message !== 'Session expired') showToast(err.message, 'error');
    return;
  }

  const previous = selectedId ?? companySelect.value;
  companySelect.innerHTML = '<option value="">Select company</option>';
  companies.forEach(function (c) {
    const opt = document.createElement('option');
    opt.value = c.id;
    opt.textContent = c.name;
    companySelect.appendChild(opt);
  });
  const newOpt = document.createElement('option');
  newOpt.value = '__new__';
  newOpt.textContent = '+ Add new company';
  companySelect.appendChild(newOpt);
  if (previous) companySelect.value = previous;

  if (!companiesPanel.hidden) renderCompanyList();
}

// ================= Application form (create + update) =================

function openForm() {
  formSection.hidden = false;
  document.getElementById('dateApplied').max = todayIso();
  formSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function resetForm() {
  form.reset();
  editingId = null;
  formTitle.textContent = 'Add application';
  submitBtn.textContent = 'Add application';
  newCompanyField.hidden = true;
  formError.hidden = true;
}

function closeForm() {
  resetForm();
  formSection.hidden = true;
}

document.getElementById('addBtn').addEventListener('click', function () {
  resetForm();
  document.getElementById('dateApplied').value = todayIso();
  openForm();
  companySelect.focus();
});

document.getElementById('cancelBtn').addEventListener('click', closeForm);

companySelect.addEventListener('change', function () {
  newCompanyField.hidden = companySelect.value !== '__new__';
  if (!newCompanyField.hidden) newCompanyName.focus();
});

form.addEventListener('submit', async function (e) {
  e.preventDefault();
  formError.hidden = true;

  const role = document.getElementById('role').value.trim();
  const dateApplied = document.getElementById('dateApplied').value;

  if (!companySelect.value) return showError(formError, 'Please select a company.');
  if (companySelect.value === '__new__' && !newCompanyName.value.trim()) {
    return showError(formError, 'Please enter the new company name.');
  }
  if (!role) return showError(formError, 'Please enter the role title.');
  if (!dateApplied) return showError(formError, 'Please choose the date you applied.');

  submitBtn.disabled = true;
  try {
    let companyId = companySelect.value;
    if (companyId === '__new__') {
      const newCompany = await api(COMPANY_API_URL, {
        method: 'POST',
        body: JSON.stringify({ name: newCompanyName.value.trim() })
      });
      companyId = newCompany.id;
      // Select the new company now, so a retry after an error does not create it twice
      await loadCompanies(companyId);
      newCompanyField.hidden = true;
    }

    const appData = {
      companyId: Number(companyId),
      role: role,
      dateApplied: dateApplied,
      status: document.getElementById('status').value,
      jobLink: document.getElementById('jobLink').value.trim() || null,
      location: document.getElementById('location').value.trim() || null
    };

    if (editingId) {
      await api(`${API_URL}/${editingId}`, { method: 'PUT', body: JSON.stringify(appData) });
      showToast('Application updated');
    } else {
      await api(API_URL, { method: 'POST', body: JSON.stringify(appData) });
      showToast('Application added');
    }

    closeForm();
    await loadCompanies();
    await loadApplications();
  } catch (err) {
    if (err.message !== 'Session expired') showError(formError, err.message);
  } finally {
    submitBtn.disabled = false;
  }
});

function startEdit(id) {
  const app = applications.find(function (a) { return a.id === id; });
  if (!app) return;

  resetForm();
  editingId = id;
  companySelect.value = app.companyId ?? '';
  document.getElementById('role').value = app.role;
  document.getElementById('dateApplied').value = app.dateApplied || '';
  document.getElementById('status').value = app.status;
  document.getElementById('jobLink').value = app.jobLink || '';
  document.getElementById('location').value = app.location || '';

  formTitle.textContent = 'Edit application';
  submitBtn.textContent = 'Update application';
  openForm();
  if (!app.companyId) showError(formError, 'This application has no company linked. Please select one.');
}

async function deleteApplication(id) {
  const app = applications.find(function (a) { return a.id === id; });
  const label = app ? `${app.role} at ${app.company || 'this company'}` : 'this application';
  if (!confirm(`Delete ${label}? Its interview rounds and documents will also be deleted.`)) return;

  try {
    await api(`${API_URL}/${id}`, { method: 'DELETE' });
    if (editingId === id) closeForm();
    showToast('Application deleted');
    loadApplications();
  } catch (err) {
    if (err.message !== 'Session expired') showToast(err.message, 'error');
  }
}

// ================= List view =================

function renderApplications() {
  if (applications.length === 0) {
    const filtering = statusFilter.value !== 'All' || searchInput.value.trim();
    list.innerHTML = filtering
      ? `<div class="empty-state"><p>No matching applications</p><p>Try a different search or status.</p></div>`
      : `<div class="empty-state"><p>No applications yet</p><p>Click "+ Add application" to add your first one.</p></div>`;
    return;
  }

  list.innerHTML = applications.map(function (app) {
    const id = app.id;
    const status = escapeHtml(app.status);
    return `
      <article class="app-card">
        <div class="card-head">
          <h3>${escapeHtml(app.company || 'No company')}</h3>
          <span class="badge badge-${status.toLowerCase()}">${status}</span>
        </div>
        <p class="card-role">${escapeHtml(app.role)}</p>
        <p>Applied: ${formatDate(app.dateApplied)}</p>
        ${app.location ? `<p>Location: ${escapeHtml(app.location)}</p>` : ''}
        ${app.jobLink ? `<p><a href="${safeUrl(app.jobLink)}" target="_blank" rel="noopener noreferrer">View job posting</a></p>` : ''}

        <div class="card-actions">
          <button type="button" class="edit-btn" data-id="${id}">Edit</button>
          <button type="button" class="delete-btn" data-id="${id}">Delete</button>
          <button type="button" class="round-toggle-btn" data-id="${id}">+ Round</button>
          <button type="button" class="doc-toggle-btn" data-id="${id}">+ Document</button>
        </div>

        <div class="inline-form" id="round-form-${id}">
          <input type="text" id="roundName-${id}" placeholder="Round name (e.g. HR Round)" />
          <input type="date" id="roundDate-${id}" aria-label="Round date" />
          <select id="roundResult-${id}" aria-label="Round result">
            <option value="Pending">Pending</option>
            <option value="Passed">Passed</option>
            <option value="Failed">Failed</option>
          </select>
          <textarea id="roundMistakes-${id}" rows="2" placeholder="Mistakes I made (optional)"></textarea>
          <textarea id="roundImprovements-${id}" rows="2" placeholder="What to improve next time (optional)"></textarea>
          <div class="inline-actions">
            <button type="button" class="inline-cancel-btn" data-form="round-form-${id}">Cancel</button>
            <button type="button" class="round-save-btn" data-id="${id}">Save round</button>
          </div>
        </div>

        <div class="inline-form" id="doc-form-${id}">
          <input type="text" id="docName-${id}" placeholder="File name (e.g. Resume.pdf)" />
          <select id="docType-${id}" aria-label="Document type">
            <option value="Resume">Resume</option>
            <option value="Cover Letter">Cover Letter</option>
            <option value="Other">Other</option>
          </select>
          <input type="url" id="docUrl-${id}" placeholder="File link (https://...)" />
          <div class="inline-actions">
            <button type="button" class="inline-cancel-btn" data-form="doc-form-${id}">Cancel</button>
            <button type="button" class="doc-save-btn" data-id="${id}">Save document</button>
          </div>
        </div>

        <div class="rounds-list" id="rounds-${id}"></div>
        <div class="rounds-list" id="docs-${id}"></div>
      </article>`;
  }).join('');

  applications.forEach(function (app) {
    loadRoundsForApp(app.id);
    loadDocsForApp(app.id);
  });
}

// ================= Interview rounds =================

async function loadRoundsForApp(appId) {
  const container = document.getElementById(`rounds-${appId}`);
  if (!container) return;
  try {
    const rounds = await api(`${API_URL}/${appId}/interview-rounds`);
    container.innerHTML = rounds.map(function (r) {
      const notes = [
        r.mistakes ? `<span><strong>Mistakes:</strong> ${escapeHtml(r.mistakes)}</span>` : '',
        r.improvements ? `<span><strong>Improve:</strong> ${escapeHtml(r.improvements)}</span>` : ''
      ].join('');
      return `<div class="round-item">
        <div class="item-main">
          <span>📝 ${escapeHtml(r.roundName)} · ${formatDate(r.roundDate)} · <strong>${escapeHtml(r.result || 'Pending')}</strong></span>
          <span class="item-actions">
            <button type="button" class="round-edit-btn" data-id="${r.id}" data-app="${appId}">Edit</button>
            <button type="button" class="round-delete-btn" data-id="${r.id}" data-app="${appId}">Delete</button>
          </span>
        </div>
        ${notes ? `<div class="item-notes">${notes}</div>` : ''}
      </div>`;
    }).join('');
  } catch (err) {
    container.innerHTML = '';
  }
}

async function saveRound(appId) {
  const roundName = document.getElementById(`roundName-${appId}`).value.trim();
  if (!roundName) return showToast('Please enter a round name.', 'error');

  const saveBtn = document.querySelector(`.round-save-btn[data-id="${appId}"]`);
  const editId = saveBtn.dataset.editId;

  const body = {
    applicationId: appId,
    roundName: roundName,
    roundDate: document.getElementById(`roundDate-${appId}`).value || null,
    result: document.getElementById(`roundResult-${appId}`).value,
    mistakes: document.getElementById(`roundMistakes-${appId}`).value.trim() || null,
    improvements: document.getElementById(`roundImprovements-${appId}`).value.trim() || null
  };

  try {
    if (editId) {
      await api(`${ROUND_API_URL}/${editId}`, { method: 'PUT', body: JSON.stringify(body) });
    } else {
      await api(ROUND_API_URL, { method: 'POST', body: JSON.stringify(body) });
    }
    closeInlineForm(`round-form-${appId}`);
    showToast(editId ? 'Round updated' : 'Round added');
    loadRoundsForApp(appId);
  } catch (err) {
    if (err.message !== 'Session expired') showToast(err.message, 'error');
  }
}

async function editRound(roundId, appId) {
  try {
    const round = await api(`${ROUND_API_URL}/${roundId}`);
    document.getElementById(`round-form-${appId}`).classList.add('open');
    document.getElementById(`roundName-${appId}`).value = round.roundName;
    document.getElementById(`roundDate-${appId}`).value = round.roundDate || '';
    document.getElementById(`roundResult-${appId}`).value = round.result || 'Pending';
    document.getElementById(`roundMistakes-${appId}`).value = round.mistakes || '';
    document.getElementById(`roundImprovements-${appId}`).value = round.improvements || '';

    const saveBtn = document.querySelector(`.round-save-btn[data-id="${appId}"]`);
    saveBtn.dataset.editId = roundId;
    saveBtn.textContent = 'Update round';
  } catch (err) {
    if (err.message !== 'Session expired') showToast(err.message, 'error');
  }
}

async function deleteRound(roundId, appId) {
  if (!confirm('Delete this interview round?')) return;
  try {
    await api(`${ROUND_API_URL}/${roundId}`, { method: 'DELETE' });
    showToast('Round deleted');
    loadRoundsForApp(appId);
  } catch (err) {
    if (err.message !== 'Session expired') showToast(err.message, 'error');
  }
}

// ================= Documents =================

async function loadDocsForApp(appId) {
  const container = document.getElementById(`docs-${appId}`);
  if (!container) return;
  try {
    const docs = await api(`${API_URL}/${appId}/documents`);
    container.innerHTML = docs.map(function (d) {
      return `<div class="round-item">
        <div class="item-main">
          <span>📎 ${escapeHtml(d.fileType || 'File')}: <a href="${safeUrl(d.fileUrl)}" target="_blank" rel="noopener noreferrer">${escapeHtml(d.fileName)}</a></span>
          <span class="item-actions">
            <button type="button" class="doc-edit-btn" data-id="${d.id}" data-app="${appId}">Edit</button>
            <button type="button" class="doc-delete-btn" data-id="${d.id}" data-app="${appId}">Delete</button>
          </span>
        </div>
      </div>`;
    }).join('');
  } catch (err) {
    container.innerHTML = '';
  }
}

async function saveDocument(appId) {
  const fileName = document.getElementById(`docName-${appId}`).value.trim();
  const fileUrl = document.getElementById(`docUrl-${appId}`).value.trim();
  if (!fileName || !fileUrl) return showToast('Please enter the file name and link.', 'error');

  const saveBtn = document.querySelector(`.doc-save-btn[data-id="${appId}"]`);
  const editId = saveBtn.dataset.editId;

  const body = {
    applicationId: appId,
    fileName: fileName,
    fileType: document.getElementById(`docType-${appId}`).value,
    fileUrl: fileUrl
  };

  try {
    if (editId) {
      await api(`${DOC_API_URL}/${editId}`, { method: 'PUT', body: JSON.stringify(body) });
    } else {
      await api(DOC_API_URL, { method: 'POST', body: JSON.stringify(body) });
    }
    closeInlineForm(`doc-form-${appId}`);
    showToast(editId ? 'Document updated' : 'Document added');
    loadDocsForApp(appId);
  } catch (err) {
    if (err.message !== 'Session expired') showToast(err.message, 'error');
  }
}

async function editDocument(docId, appId) {
  try {
    const doc = await api(`${DOC_API_URL}/${docId}`);
    document.getElementById(`doc-form-${appId}`).classList.add('open');
    document.getElementById(`docName-${appId}`).value = doc.fileName;
    document.getElementById(`docType-${appId}`).value = doc.fileType || 'Other';
    document.getElementById(`docUrl-${appId}`).value = doc.fileUrl;

    const saveBtn = document.querySelector(`.doc-save-btn[data-id="${appId}"]`);
    saveBtn.dataset.editId = docId;
    saveBtn.textContent = 'Update document';
  } catch (err) {
    if (err.message !== 'Session expired') showToast(err.message, 'error');
  }
}

async function deleteDocument(docId, appId) {
  if (!confirm('Delete this document?')) return;
  try {
    await api(`${DOC_API_URL}/${docId}`, { method: 'DELETE' });
    showToast('Document deleted');
    loadDocsForApp(appId);
  } catch (err) {
    if (err.message !== 'Session expired') showToast(err.message, 'error');
  }
}

/** Closes an inline form, clears its fields and resets its save button. */
function closeInlineForm(formId) {
  const box = document.getElementById(formId);
  if (!box) return;
  box.classList.remove('open');
  box.querySelectorAll('input, textarea').forEach(function (el) { el.value = ''; });
  box.querySelectorAll('select').forEach(function (el) { el.selectedIndex = 0; });

  const saveBtn = box.querySelector('.round-save-btn, .doc-save-btn');
  delete saveBtn.dataset.editId;
  saveBtn.textContent = saveBtn.classList.contains('round-save-btn') ? 'Save round' : 'Save document';
}

function toggleInlineForm(formId) {
  const box = document.getElementById(formId);
  if (box.classList.contains('open')) {
    closeInlineForm(formId);
  } else {
    box.classList.add('open');
    const first = box.querySelector('input');
    if (first) first.focus();
  }
}

// ================= Click handling (list) =================

list.addEventListener('click', function (e) {
  const btn = e.target.closest('button');
  if (!btn) return;
  const id = Number(btn.dataset.id);
  const appId = Number(btn.dataset.app);

  if (btn.classList.contains('edit-btn')) startEdit(id);
  else if (btn.classList.contains('delete-btn')) deleteApplication(id);
  else if (btn.classList.contains('round-toggle-btn')) toggleInlineForm(`round-form-${id}`);
  else if (btn.classList.contains('doc-toggle-btn')) toggleInlineForm(`doc-form-${id}`);
  else if (btn.classList.contains('inline-cancel-btn')) closeInlineForm(btn.dataset.form);
  else if (btn.classList.contains('round-save-btn')) saveRound(id);
  else if (btn.classList.contains('doc-save-btn')) saveDocument(id);
  else if (btn.classList.contains('round-edit-btn')) editRound(id, appId);
  else if (btn.classList.contains('round-delete-btn')) deleteRound(id, appId);
  else if (btn.classList.contains('doc-edit-btn')) editDocument(id, appId);
  else if (btn.classList.contains('doc-delete-btn')) deleteDocument(id, appId);
});

// ================= Search, filter, sort =================

searchInput.addEventListener('input', debounce(loadApplications, 300));
statusFilter.addEventListener('change', loadApplications);
sortBy.addEventListener('change', loadApplications);

// ================= View toggle =================

listViewBtn.addEventListener('click', function () {
  list.hidden = false;
  boardSection.hidden = true;
  listViewBtn.classList.add('active');
  boardViewBtn.classList.remove('active');
});

boardViewBtn.addEventListener('click', function () {
  list.hidden = true;
  boardSection.hidden = false;
  listViewBtn.classList.remove('active');
  boardViewBtn.classList.add('active');
  renderBoard();
});

// ================= Board view (drag and drop) =================

function renderBoard() {
  document.querySelectorAll('.column-cards').forEach(function (col) {
    col.innerHTML = '';
  });

  applications.forEach(function (app) {
    const card = document.createElement('div');
    card.className = 'board-card';
    card.draggable = true;
    card.dataset.id = app.id;
    card.innerHTML = `<strong>${escapeHtml(app.company || 'No company')}</strong><span>${escapeHtml(app.role)}</span>`;

    card.addEventListener('dragstart', function (e) {
      card.classList.add('dragging');
      e.dataTransfer.setData('text/plain', String(app.id));
    });
    card.addEventListener('dragend', function () {
      card.classList.remove('dragging');
    });

    const targetColumn = document.querySelector(`.column-cards[data-status="${app.status}"]`);
    if (targetColumn) targetColumn.appendChild(card);
  });

  document.querySelectorAll('.column-cards').forEach(function (col) {
    if (!col.children.length) col.innerHTML = '<p class="column-empty">Drop here</p>';
  });
}

document.querySelectorAll('.column-cards').forEach(function (column) {
  column.addEventListener('dragover', function (e) {
    e.preventDefault();
    column.classList.add('drag-over');
  });

  column.addEventListener('dragleave', function () {
    column.classList.remove('drag-over');
  });

  column.addEventListener('drop', async function (e) {
    e.preventDefault();
    column.classList.remove('drag-over');

    const id = Number(e.dataTransfer.getData('text/plain'));
    const app = applications.find(function (a) { return a.id === id; });
    const newStatus = column.dataset.status;
    if (!app || app.status === newStatus) return;

    try {
      await api(`${API_URL}/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ ...toRequest(app), status: newStatus })
      });
      showToast(`Moved to ${newStatus}`);
    } catch (err) {
      if (err.message !== 'Session expired') showToast(err.message, 'error');
      return;
    }
    loadApplications();
  });
});

// ================= Start =================

(function start() {
  const token = currentToken();
  if (!token || isTokenExpired(token)) {
    clearSession();
    showAuthScreen(token ? 'Your session has expired. Please log in again.' : null);
    return;
  }
  try { currentUser = JSON.parse(localStorage.getItem(USER_KEY)); } catch (e) { currentUser = null; }
  showApp();
})();
