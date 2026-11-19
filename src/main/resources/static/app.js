const API = "/api/applications";
const STATUSES = ["WISHLIST", "APPLIED", "INTERVIEWING", "OFFER", "REJECTED"];
const LABELS = { WISHLIST: "Wishlist", APPLIED: "Applied", INTERVIEWING: "Interviewing", OFFER: "Offer", REJECTED: "Rejected" };

const $ = (sel) => document.querySelector(sel);
const board = $("#board"), stats = $("#stats"), dialog = $("#dialog"), form = $("#form");
let editingId = null;
let searchTimer;

function el(tag, attrs = {}, ...children) {
  const node = document.createElement(tag);
  Object.entries(attrs).forEach(([k, v]) => (k === "class" ? (node.className = v) : node.setAttribute(k, v)));
  children.flat().forEach((c) => c != null && node.append(c)); // append() treats strings as text: no XSS
  return node;
}

async function api(path = "", options = {}) {
  const res = await fetch(API + path, { headers: { "Content-Type": "application/json" }, ...options });
  if (res.status === 204) return null;
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw body;
  return body;
}

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
      await api(`/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) });
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
    await api(editingId ? `/${editingId}` : "", { method: editingId ? "PUT" : "POST", body: JSON.stringify(data) });
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

refresh();
