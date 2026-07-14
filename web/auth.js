const authStatus = document.getElementById("auth-status");

const storedUser = sessionStorage.getItem("bankflowUser");
if (storedUser) {
    window.location.href = "dashboard.html";
}

document.getElementById("register-form").addEventListener("submit", (event) => {
    event.preventDefault();
    submitAuth("/api/register", new FormData(event.currentTarget), event.currentTarget);
});

document.getElementById("login-form").addEventListener("submit", (event) => {
    event.preventDefault();
    submitAuth("/api/login", new FormData(event.currentTarget), event.currentTarget);
});

async function submitAuth(url, formData, form) {
    setAuthStatus("Checking details...", false);

    const response = await fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
        },
        body: new URLSearchParams(formData).toString()
    });

    const data = await response.json();
    if (!data.ok) {
        setAuthStatus(data.message || "Something went wrong.", true);
        return;
    }

    sessionStorage.setItem("bankflowUser", data.username);
    form.reset();
    setAuthStatus(data.message || "Success.", false);
    window.location.href = "dashboard.html";
}

function setAuthStatus(message, isError) {
    authStatus.textContent = message;
    authStatus.className = isError ? "status-box status-box--error" : "status-box status-box--success";
}
