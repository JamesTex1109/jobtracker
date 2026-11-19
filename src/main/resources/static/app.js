const API = "/api/applications";
const STATUSES = ["WISHLIST", "APPLIED", "INTERVIEWING", "OFFER", "REJECTED"];
const LABELS = { WISHLIST: "Wishlist", APPLIED: "Applied", INTERVIEWING: "Interviewing", OFFER: "Offer", REJECTED: "Rejected" };

const $ = (sel) => document.querySelector(sel);
const board = $("#board"), stats = $("#stats"), dialog = $("#dialog"), form = $("#form");
let editingId = null;
let searchTimer;
let registering = false;

function el(tag, attrs = {}, ...children) {
  const node = document.createElement(tag);
  Object.entries(attrs).forEach(([k, v]) => (k === "class" ? (node.className = v) : node.setAttribute(k, v)));
  children.flat().forEach((c) => c != null && node.append(c)); // append() treats strings as text: no XSS
  return node;
}

// --- HTTP + auth ----------------------------------------------------------

/** Spring Security puts a CSRF token in this cookie; state-changing requests must echo it in a header. */
function csrfToken() {
  return document.cookie.split("; ").find((c) => c.startsWith("XSRF-TOKEN="))?.split("=")[1];
}

async function ensureCsrf() {
  if (!csrfToken()) await fetch("/api/auth/csrf");
}

async function request(url, { method = "GET", body, form } = {}) {
  const headers = {};
  if (method !== "GET") {
    await ensureCsrf();
    headers["X-XSRF-TOKEN"] = csrfToken();
  }
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const res = await fetch(url, {
    method, headers,
    body: form ? new URLSearchParams(form) : body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (res.status === 401 && !url.startsWith("/api/auth/")) { showAuth(); throw { detail: "Please sign in" }; }
  if (res.status === 204) return null;
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw { status: res.status, ...data };
  return data;
}

const api = (path = "", { method, body } = {}) => request(API + path, { method, body });

function showAuth() {
  $("#app-view").hidden = true;
  $("#auth-view").hidden = false;
  $("#auth-form [name=email]").focus();
}

function showApp(user) {
  $("#auth-view").hidden = true;
  $("#app-view").hidden = false;
  $("#user-name").textContent = user.displayName;
  refresh();
}

function setAuthMode(isRegister) {
  registering = isRegister;
  $("#name-field").hidden = !isRegister;
  $("#auth-form [name=displayName]").required = isRegister;
  $("#auth-form [name=password]").autocomplete = isRegister ? "new-password" : "current-password";
  $("#auth-subtitle").textContent = isRegister ? "Create your account" : "Sign in to your pipeline";
  $("#auth-submit").textContent = isRegister ? "Create account" : "Sign in";
  $("#auth-switch-text").textContent = isRegister ? "Already have an account?" : "New here?";
  $("#auth-switch").textContent = isRegister ? "Sign in" : "Create an account";
  $("#auth-error").textContent = "";
}

$("#auth-switch").addEventListener("click", () => setAuthMode(!registering));

$("#auth-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const { email, password, displayName } = Object.fromEntries(new FormData(e.target));
  try {
    if (registering) {
      await request("/api/auth/register", { method: "POST", body: { email, password, displayName } });
    }
    await request("/api/auth/login", { method: "POST", form: { email, password } });
    await fetch("/api/auth/csrf"); // the token rotates on login; fetch the new one
    e.target.reset();
    setAuthMode(false);
    showApp(await request("/api/auth/me"));
  } catch (err) {
    const fields = err.errors ? Object.entries(err.errors).map(([f, m]) => `${f} ${m}`).join("; ") : "";
    $("#auth-error").textContent =
      fields || (err.status === 401 ? "Wrong email or password" : err.detail) || "Something went wrong";
  }
});

$("#logout-btn").addEventListener("click", async () => {
  await request("/api/auth/logout", { method: "POST" });
  showAuth();
});

// --- Board -----------------------------------------------------------------

async function refresh() {
  const q = $("#search").value.trim();
  const [page, s] = await Promise.all([api(`?size=100${q ? "&q=" + encodeURIComponent(q) : ""}`), api("/stats")]);
  renderBoard(page.content);
  renderStats(s);
}

function renderStats(s) {
  const tile = (value, label) => el("div", { class: "stat" }, el("b", {}, String(value)), el("span", {}, label));
  stats.replaceChildren(
    tile(s.total, "Total"),
    tile(s.total - s.byStatus.WISHLIST, "Submitted"),
    tile(s.byStatus.INTERVIEWING, "Interviewing"),
    tile(s.byStatus.OFFER, "Offers"),
    tile(Math.round(s.responseRate * 100) + "%", "Response rate"),
  );
}

function renderBoard(apps) {
  board.replaceChildren(...STATUSES.map((status) => {
    const items = apps.filter((a) => a.status === status);
    const col = el("section", { class: "column", "data-status": status, style: `--c: var(--${status.toLowerCase()})` },
      el("h2", {}, LABELS[status], el("span", { class: "count" }, String(items.length))),
      items.length ? items.map(card) : el("p", { class: "empty" }, "Nothing here yet"));
    col.addEventListener("dragover", (e) => { e.preventDefault(); col.classList.add("drop"); });
    col.addEventListener("dragleave", () => col.classList.remove("drop"));
    col.addEventListener("drop", async (e) => {
      e.preventDefault();
      col.classList.remove("drop");
      const id = e.dataTransfer.getData("text/plain");
      await api(`/${id}/status`, { method: "PATCH", body: { status } });
      refresh();
    });
    return col;
  }));
}

function card(a) {
  const meta = [a.role, a.location].filter(Boolean).join(" · ");
  const node = el("article", { class: "card", draggable: "true", tabindex: "0" },
    el("div", { class: "company" }, a.company),
    el("div", { class: "meta" }, meta),
    a.appliedDate ? el("div", { class: "meta" }, "Applied " + a.appliedDate) : null,
    a.jobUrl ? el("a", { href: a.jobUrl, target: "_blank", rel: "noopener" }, "Posting ↗") : null);
  node.addEventListener("dragstart", (e) => e.dataTransfer.setData("text/plain", a.id));
  node.addEventListener("click", (e) => e.target.tagName !== "A" && openForm(a));
  node.addEventListener("keydown", (e) => e.key === "Enter" && openForm(a));
  return node;
}

function openForm(app = null) {
  editingId = app?.id ?? null;
  form.reset();
  $("#form-error").textContent = "";
  $("#form-title").textContent = app ? "Edit application" : "New application";
  $("#delete-btn").hidden = !app;
  if (app) for (const [k, v] of Object.entries(app)) if (form.elements[k]) form.elements[k].value = v ?? "";
  dialog.showModal();
}

form.elements.status.append(...STATUSES.map((s) => el("option", { value: s }, LABELS[s])));

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  const data = Object.fromEntries(new FormData(form));
  for (const k in data) if (data[k] === "") data[k] = null; // blank optional fields -> null
  try {
    await api(editingId ? `/${editingId}` : "", { method: editingId ? "PUT" : "POST", body: data });
    dialog.close();
    refresh();
  } catch (err) {
    const fields = err.errors ? Object.entries(err.errors).map(([f, m]) => `${f} ${m}`).join("; ") : "";
    $("#form-error").textContent = fields || err.detail || "Something went wrong";
  }
});

$("#delete-btn").addEventListener("click", async () => {
  if (!confirm("Delete this application?")) return;
  await api(`/${editingId}`, { method: "DELETE" });
  dialog.close();
  refresh();
});
$("#cancel-btn").addEventListener("click", () => dialog.close());
$("#new-btn").addEventListener("click", () => openForm());
$("#search").addEventListener("input", () => { clearTimeout(searchTimer); searchTimer = setTimeout(refresh, 250); });

request("/api/auth/me").then(showApp, showAuth);
