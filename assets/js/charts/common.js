// Shared helpers for the D3 charts. Each chart registers a draw(el, data) function; all numbers
// arrive pre-rounded and pre-formatted from charts.json, so charts never format values themselves.
(function () {
  const registry = {};

  const tooltip = (el) => {
    const tip = d3.select(el).append("div").attr("class", "cp-tip");
    return {
      show(event, text) {
        const [x, y] = d3.pointer(event, el);
        const left = Math.min(x + 12, el.clientWidth - tip.node().offsetWidth - 4);
        tip.text(text).style("left", `${Math.max(0, left)}px`).style("top", `${y - 34}px`).style("opacity", 1);
      },
      hide() { tip.style("opacity", 0); },
    };
  };

  // A bar with a 4px rounded data-end and a square end at the baseline.
  const barPath = (x0, x1, y, h, r = 4) => {
    const w = Math.abs(x1 - x0), dir = x1 >= x0 ? 1 : -1, rr = Math.min(r, w, h / 2);
    if (w < 0.5) return "";
    const end = x1 - dir * rr;
    return `M${x0},${y}H${end}` +
      `A${rr},${rr} 0 0 ${dir > 0 ? 1 : 0} ${x1},${y + rr}` +
      `V${y + h - rr}` +
      `A${rr},${rr} 0 0 ${dir > 0 ? 1 : 0} ${end},${y + h}` +
      `H${x0}Z`;
  };

  const svgFor = (el, height) => {
    const width = el.clientWidth;
    return d3.select(el).append("svg")
      .attr("viewBox", `0 0 ${width} ${height}`)
      .attr("width", width).attr("height", height)
      .attr("aria-hidden", "true");
  };

  const text = (sel, x, y, s, cls) => sel.append("text").attr("x", x).attr("y", y).text(s).attr("class", cls);

  window.CPCharts = {
    register(name, draw) { registry[name] = draw; },
    tooltip, barPath, svgFor, text,
    renderAll(data) {
      document.querySelectorAll(".cp-plot[data-chart]").forEach((el) => {
        const draw = registry[el.dataset.chart];
        if (!draw) return;
        let width = 0;
        const render = () => {
          if (el.clientWidth === width) return;
          width = el.clientWidth;
          el.replaceChildren();
          draw(el, data, width < 560);
        };
        render();
        new ResizeObserver(render).observe(el);
      });
    },
  };
})();
