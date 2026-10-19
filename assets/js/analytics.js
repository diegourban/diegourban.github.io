// Click events for GoatCounter: one event per click on a link that leaves the site.
// "source" = the post's Source link, "benchmarks" = links into a benchmarks/ folder,
// "outbound" = any other external link. Page views are counted by count.js itself.
(function () {
  const kind = (a, url) => {
    if (url.hostname === location.hostname) return null;
    if (a.closest(".post-meta") && a.textContent.trim() === "Source") return "source";
    if (url.pathname.includes("/benchmarks/")) return "benchmarks";
    return "outbound";
  };

  document.addEventListener("click", (event) => {
    const a = event.target.closest("a[href]");
    if (!a || !window.goatcounter || !window.goatcounter.count) return;
    let url;
    try { url = new URL(a.href, location.href); } catch (e) { return; }
    const k = kind(a, url);
    if (!k) return;
    window.goatcounter.count({
      path: `${k}: ${url.hostname}${url.pathname}`,
      title: (a.textContent.trim() || url.href).slice(0, 80),
      event: true,
    });
  });
})();
