// PricePath - shared frontend helper functions
async function api(method, url, fields) {
  var options = { method: method, credentials: 'same-origin', headers: {} };
  var token = sessionStorage.getItem('pricepathToken');
  if (token) options.headers['X-Session-Token'] = token;
  if (method === 'POST') {
    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
    options.body = new URLSearchParams(fields || {}).toString();
  }
  var response = await fetch(url, options);
  var data = null;
  try { data = await response.json(); } catch (e) { data = null; }
  if (response.status === 401 && !location.pathname.endsWith('login.html')
      && !location.pathname.endsWith('register.html')) {
    sessionStorage.removeItem('pricepathToken');
    location.href = 'login.html';
    throw new Error('Please log in');
  }
  if (!response.ok) {
    throw new Error(data && data.error ? data.error : 'Request failed (' + response.status + ')');
  }
  return data;
}

function esc(text) {
  if (text === null || text === undefined) return '';
  return String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;')
    .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function money(value) {
  if (value === null || value === undefined) return '-';
  return '₹' + Number(value).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function showMsg(id, text, type) {
  var box = document.getElementById(id);
  if (!box) return;
  box.textContent = text;
  box.className = 'msg ' + type;
}

function getParam(name) {
  return new URLSearchParams(location.search).get(name);
}

async function startPage() {
  var user = await api('GET', '/api/auth/me');
  var html = '<span class="brand">PricePath</span>'
    + '<a href="products.html">Products</a>'
    + '<a href="lists.html">My Lists</a>'
    + '<a href="orders.html">Orders &amp; Saved Plans</a>'
    + '<a href="price-history.html">Price History</a>';
  if (user.role === 'admin') html += '<a href="admin.html">Admin</a>';
  html += '<span class="spacer"></span><span>' + esc(user.name) + '</span>'
    + '<a href="#" id="logoutLink">Log out</a>';
  var nav = document.getElementById('navbar');
  nav.className = 'navbar';
  nav.innerHTML = html;
  document.getElementById('logoutLink').onclick = async function (event) {
    event.preventDefault();
    await api('POST', '/api/auth/logout');
    sessionStorage.removeItem('pricepathToken');
    location.href = 'login.html';
  };
  return user;
}