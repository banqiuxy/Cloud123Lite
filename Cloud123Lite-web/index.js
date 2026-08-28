const byId = (id) => document.getElementById(id);

function applyVersion(version) {
    const label = version.version ? `v${version.version}` : "开发版";
    const downloadUrl = version.downloadUrl || "#";

    byId("hero-version").textContent = label;
    byId("download-version").textContent = label;
    byId("panel-version").textContent = label;
    byId("build-code").textContent = version.versionCode || "--";
    byId("min-android").textContent = version.minAndroid || "Android 10.0+";

    const downloadLink = byId("download-link");
    downloadLink.href = downloadUrl;
    if (/^https?:\/\//i.test(downloadUrl)) {
        downloadLink.removeAttribute("download");
    } else {
        downloadLink.setAttribute("download", "");
    }

    byId("source-link").href = version.sourceUrl || "#";
}

async function loadVersion() {
    if (window.location.protocol === "file:") return;

    try {
        const response = await fetch("version.json", { cache: "no-store" });
        if (!response.ok) throw new Error(`version.json returned ${response.status}`);
        applyVersion(await response.json());
    } catch (error) {
        console.info("Version config unavailable.", error);
    }
}

function setupReveal() {
    const revealItems = document.querySelectorAll("[data-reveal]");
    document.body.classList.add("reveal-ready");
    if (!("IntersectionObserver" in window)) {
        revealItems.forEach((item) => item.classList.add("is-visible"));
        return;
    }

    const observer = new IntersectionObserver((entries, currentObserver) => {
        entries.forEach((entry) => {
            if (!entry.isIntersecting) return;
            const item = entry.target;
            const delay = item.dataset.delay || 0;
            item.style.transitionDelay = `${delay}ms`;
            item.classList.add("is-visible");
            currentObserver.unobserve(item);
        });
    }, { threshold: 0.12 });

    revealItems.forEach((item) => observer.observe(item));
}

function setupActiveNavigation() {
    const sections = [...document.querySelectorAll("main section[id]")];
    const links = [...document.querySelectorAll(".site-nav a")];
    if (!("IntersectionObserver" in window)) return;

    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (!entry.isIntersecting) return;
            links.forEach((link) => {
                link.toggleAttribute("aria-current", link.getAttribute("href") === `#${entry.target.id}`);
            });
        });
    }, { rootMargin: "-35% 0px -55%", threshold: 0 });

    sections.forEach((section) => observer.observe(section));
}

document.addEventListener("DOMContentLoaded", () => {
    setupReveal();
    setupActiveNavigation();
    loadVersion();
});