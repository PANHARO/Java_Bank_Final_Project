const state = {
    username: sessionStorage.getItem("bankflowUser") || "",
    balance: 0,
    history: []
};

if (!state.username) {
    window.location.href = "auth.html";
}

const welcome = document.getElementById("welcome");
const balance = document.getElementById("balance");
const activeUser = document.getElementById("active-user");
const historyCount = document.getElementById("history-count");
const balanceNote = document.getElementById("balance-note");
const userNote = document.getElementById("user-note");
const historyNote = document.getElementById("history-note");
const recentType = document.getElementById("recent-type");
const lastAction = document.getElementById("last-action");
const historyList = document.getElementById("history-list");
const dashboardStatus = document.getElementById("dashboard-status");
const sidebarUser = document.getElementById("sidebar-user");
const sidebarBalance = document.getElementById("sidebar-balance");
const historyPanel = document.getElementById("activity");
const historyButton = document.getElementById("open-history-button");
const activityLink = document.getElementById("activity-link");

document.getElementById("deposit-form").addEventListener("submit", (event) => {
    event.preventDefault();
    submitAction("/api/deposit", new FormData(event.currentTarget), event.currentTarget);
});

document.getElementById("withdraw-form").addEventListener("submit", (event) => {
    event.preventDefault();
    submitAction("/api/withdraw", new FormData(event.currentTarget), event.currentTarget);
});

document.getElementById("transfer-form").addEventListener("submit", (event) => {
    event.preventDefault();
    submitAction("/api/transfer", new FormData(event.currentTarget), event.currentTarget);
});

document.getElementById("refresh-button").addEventListener("click", async () => {
    await loadAccount();
});

document.getElementById("logout-button").addEventListener("click", () => {
    sessionStorage.removeItem("bankflowUser");
    window.location.href = "auth.html";
});

historyButton.addEventListener("click", () => {
    showHistoryPanel();
});

if (activityLink) {
    activityLink.addEventListener("click", () => {
        showHistoryPanel();
    });
}

loadAccount();

async function submitAction(url, formData, form) {
    setDashboardStatus("Processing request...", false);
    formData.append("username", state.username);

    const response = await fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
        },
        body: new URLSearchParams(formData).toString()
    });

    const data = await response.json();
    if (!data.ok) {
        setDashboardStatus(data.message || "Something went wrong.", true);
        return;
    }

    updateState(data);
    form.reset();
}

async function loadAccount() {
    setDashboardStatus("Loading account...", false);
    const response = await fetch(`/api/account?username=${encodeURIComponent(state.username)}`);
    const data = await response.json();

    if (!data.ok) {
        setDashboardStatus(data.message || "Unable to load account.", true);
        return;
    }

    updateState(data);
}

function updateState(data) {
    const previousBalance = state.balance;

    state.username = data.username;
    state.balance = Number(data.balance);
    state.history = Array.isArray(data.history) ? data.history : [];
    sessionStorage.setItem("bankflowUser", state.username);

    welcome.textContent = `Welcome, ${state.username}.`;
    activeUser.textContent = state.username;
    sidebarUser.textContent = state.username;
    animateMoney(previousBalance, state.balance);

    historyCount.textContent = String(state.history.length);
    balanceNote.textContent = state.balance > 0 ? "Funds available for your next move." : "Deposit to activate this account.";
    userNote.textContent = "Verified session active.";
    historyNote.textContent = state.history.length ? `${state.history.length} transaction${state.history.length === 1 ? "" : "s"} recorded.` : "No transactions yet.";

    updateInsights();
    renderHistory();
    setDashboardStatus(data.message || "Updated.", false);
}

function renderHistory() {
    historyList.innerHTML = "";

    if (!state.history.length) {
        historyList.innerHTML = '<div class="activity-empty">No transactions yet.</div>';
        return;
    }

    state.history
        .slice()
        .reverse()
        .forEach((entry, index) => {
            const parsed = parseHistoryEntry(entry);
            const item = document.createElement("article");
            item.className = "timeline-item";
            item.style.animationDelay = `${Math.min(index * 70, 420)}ms`;

            const row = document.createElement("div");
            row.className = "timeline-item__row";

            const title = document.createElement("strong");
            title.textContent = parsed.title;

            const badge = document.createElement("span");
            badge.className = `badge ${badgeClass(parsed.typeKey)}`;
            badge.textContent = parsed.badge;

            const meta = document.createElement("div");
            meta.className = "timeline-item__meta";
            meta.textContent = parsed.meta;

            row.appendChild(title);
            row.appendChild(badge);
            item.appendChild(row);
            item.appendChild(meta);
            historyList.appendChild(item);
        });
}

function parseHistoryEntry(entry) {
    const parts = entry.split(" | ");
    const datePart = parts[0] || "";
    const type = parts[1] || "Activity";
    const rest = parts.slice(2).join(" | ");
    const typeKey = type.toLowerCase();

    return {
        title: friendlyType(typeKey),
        badge: badgeText(typeKey),
        typeKey,
        meta: [datePart, rest].filter(Boolean).join(" | ")
    };
}

function friendlyType(typeKey) {
    switch (typeKey) {
        case "deposit":
            return "Deposit received";
        case "withdraw":
            return "Cash withdrawn";
        case "transfer_out":
            return "Transfer sent";
        case "transfer_in":
            return "Transfer received";
        default:
            return "Account activity";
    }
}

function badgeText(typeKey) {
    switch (typeKey) {
        case "deposit":
            return "Deposit";
        case "withdraw":
            return "Withdraw";
        case "transfer_out":
            return "Sent";
        case "transfer_in":
            return "Received";
        default:
            return "Update";
    }
}

function badgeClass(typeKey) {
    switch (typeKey) {
        case "deposit":
            return "badge--deposit";
        case "withdraw":
            return "badge--withdraw";
        case "transfer_out":
            return "badge--transfer_out";
        case "transfer_in":
            return "badge--transfer_in";
        default:
            return "badge--deposit";
    }
}

function updateInsights() {
    const latest = state.history[state.history.length - 1] || "";
    const latestParts = latest.split(" | ");
    const latestType = latestParts[1] || "Waiting";

    if (recentType) {
        recentType.textContent = latest ? friendlyType(latestType.toLowerCase()) : "Waiting";
    }

    if (lastAction) {
        lastAction.textContent = latest ? latestParts[0] : "No activity yet";
    }
}

function showHistoryPanel() {
    historyPanel.classList.remove("is-hidden");
    historyPanel.setAttribute("aria-hidden", "false");
    requestAnimationFrame(() => {
        historyPanel.scrollIntoView({ behavior: "smooth", block: "start" });
    });
}

function animateMoney(from, to) {
    const start = Number.isFinite(from) ? from : 0;
    const end = Number.isFinite(to) ? to : 0;
    const duration = 650;
    const startedAt = performance.now();
    const targets = [balance, sidebarBalance].filter(Boolean);

    pulseBalanceChange(start, end, targets);

    function frame(now) {
        const progress = Math.min((now - startedAt) / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);
        const value = start + (end - start) * eased;
        targets.forEach((target) => {
            target.textContent = formatMoney(value);
        });

        if (progress < 1) {
            requestAnimationFrame(frame);
        } else {
            targets.forEach((target) => {
                target.textContent = formatMoney(end);
            });
        }
    }

    requestAnimationFrame(frame);
}

function pulseBalanceChange(from, to, targets) {
    const direction = to > from ? "balance--up" : to < from ? "balance--down" : "";
    if (!direction) {
        return;
    }

    targets.forEach((target) => {
        target.classList.remove("balance--up", "balance--down");
        void target.offsetWidth;
        target.classList.add(direction);
    });

    window.setTimeout(() => {
        targets.forEach((target) => {
            target.classList.remove(direction);
        });
    }, 760);
}

function setDashboardStatus(message, isError) {
    dashboardStatus.textContent = message;
    dashboardStatus.className = isError ? "status-box status-box--error" : "status-box status-box--success";
}

function formatMoney(value) {
    return new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD"
    }).format(value || 0);
}
