// Chart B: change in private (RssAnon) and file-backed (RssFile) memory per setup, in MB.
CPCharts.register("memory-change", (el, data, narrow) => {
  const { tooltip, svgFor, text, barPath } = CPCharts;
  const rows = data.rows;
  const series = [
    { key: "anon", name: "Private (RssAnon)", cls: "cp-bar-1", sw: "var(--c-s1)" },
    { key: "file", name: "File-backed (RssFile)", cls: "cp-bar-2", sw: "var(--c-s2)" },
  ];
  const BAR = 14, GAP = 2, ROW = 2 * BAR + GAP + 20, TOP = 82;
  const W = el.clientWidth, LEFT = narrow ? 112 : 150, RIGHT = 56;
  const H = TOP + ROW * rows.length + 30;
  const x = d3.scaleLinear([-8, 4], [LEFT, W - RIGHT]);
  const svg = svgFor(el, H);
  const tip = tooltip(el);

  text(svg, 0, 18, narrow ? "Memory change per JVM" : "Memory change per JVM after removing -Xshare:off", "cp-t-title");
  text(svg, 0, 40, "Paired change in MB. Left of 0 is less memory.", "cp-t-sub");
  let lx = 0;
  series.forEach((s) => {
    svg.append("rect").attr("x", lx).attr("y", 52).attr("width", 12).attr("height", 12).attr("rx", 3).style("fill", s.sw);
    const t = text(svg, lx + 18, 62, s.name, "cp-t-sub");
    lx += 18 + t.node().getComputedTextLength() + 20;
  });

  const axisY = TOP + ROW * rows.length - 10;
  d3.range(-8, 5, 2).forEach((t) => {
    svg.append("line").attr("class", t === 0 ? "cp-zero" : "cp-grid")
      .attr("x1", x(t)).attr("x2", x(t)).attr("y1", TOP - 6).attr("y2", axisY);
    text(svg, x(t), axisY + 18, t === 0 ? "0 MB" : `${t > 0 ? "+" : "−"}${Math.abs(t)}`, "cp-t-tick")
      .attr("text-anchor", "middle");
  });

  rows.forEach((r, i) => {
    const y0 = TOP + ROW * i;
    text(svg, 0, y0 + BAR + 5, r.setup, "cp-t-lab");
    series.forEach((s, k) => {
      const m = r.memory[s.key], y = y0 + k * (BAR + GAP), x1 = x(m.shift);
      svg.append("path").attr("class", s.cls).attr("d", barPath(x(0), x1, y, BAR))
        .on("mousemove", (ev) => tip.show(ev, `${r.setup}, ${s.name}: ${m.label} MB`))
        .on("mouseleave", tip.hide);
      const after = m.shift >= 0;
      text(svg, x1 + (after ? 6 : -6), y + BAR - 3, `${m.label}`, "cp-t-val2")
        .attr("text-anchor", after ? "start" : "end");
    });
  });
});
