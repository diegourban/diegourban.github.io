// Line chart for change over a run: {{< chart "lines" "key" >}} draws charts.json data[key]:
// {title, sub[], x: {label, scale: "linear"|"log", min, max, ticks[]}, y: {label, min, max, ticks[], unit},
//  series: [{name, short (end label and tooltip), cls: "s1"|"s2"|"s3"|"rest", dash?, points: [[x, y], ...]}]}.
// One y-axis only; values above y.max are clipped to the top edge. Each line is labelled at its end and in
// the legend, so identity never rests on color alone, and a crosshair tooltip lists every series at a point.
CPCharts.register("lines", (el, data, narrow) => {
  const { tooltip, svgFor, text } = CPCharts;
  const d = data[el.dataset.key];
  const W = el.clientWidth, LEFT = 44, RIGHT = narrow ? 12 : 150;
  const legendRows = narrow ? d.series.length : Math.ceil(d.series.length / 2);
  const TOP = 48 + 18 * d.sub.length + 20 * legendRows, PLOT = narrow ? 200 : 240, H = TOP + PLOT + 44;
  const x = (d.x.scale === "log" ? d3.scaleLog() : d3.scaleLinear()).domain([d.x.min, d.x.max]).range([LEFT, W - RIGHT]);
  const y = d3.scaleLinear([d.y.min, d.y.max], [TOP + PLOT, TOP]);
  const clip = (v) => Math.min(Math.max(v, d.y.min), d.y.max);
  const svg = svgFor(el, H);
  const tip = tooltip(el);
  const fmt = (v) => v.toLocaleString("en-US");

  text(svg, 0, 18, d.title, "cp-t-title");
  d.sub.forEach((s, i) => text(svg, 0, 40 + 18 * i, s, "cp-t-sub"));

  // Legend: a swatch line in the series' own style, then its name in ink.
  const legendTop = 40 + 18 * d.sub.length;
  d.series.forEach((s, i) => {
    const col = narrow ? 0 : i % 2, row = narrow ? i : Math.floor(i / 2);
    const lx = col * Math.max(260, (W - RIGHT) / 2), ly = legendTop + 20 * row;
    svg.append("line").attr("class", `cp-line cp-line-${s.cls}${s.dash ? " cp-dash" : ""}`)
      .attr("x1", lx).attr("x2", lx + 22).attr("y1", ly - 4).attr("y2", ly - 4);
    text(svg, lx + 30, ly, s.name, "cp-t-val2");
  });

  d.y.ticks.forEach((t) => {
    svg.append("line").attr("class", t === d.y.min ? "cp-zero" : "cp-grid")
      .attr("x1", LEFT).attr("x2", W - RIGHT).attr("y1", y(t)).attr("y2", y(t));
    text(svg, LEFT - 6, y(t) + 4, `${fmt(t)}${t === d.y.max && d.y.unit ? " " + d.y.unit : ""}`, "cp-t-tick").attr("text-anchor", "end");
  });
  d.x.ticks.forEach((t) => {
    text(svg, x(t), TOP + PLOT + 18, fmt(t), "cp-t-tick").attr("text-anchor", "middle");
  });
  text(svg, LEFT, TOP + PLOT + 38, d.x.label, "cp-t-tick");

  const line = d3.line().x((p) => x(p[0])).y((p) => y(clip(p[1])));
  // Draw the context series first, so colored lines sit on top of it.
  [...d.series].sort((a, b) => (a.cls === "rest" ? -1 : 0) - (b.cls === "rest" ? -1 : 0)).forEach((s) => {
    svg.append("path").attr("class", `cp-line cp-line-${s.cls}${s.dash ? " cp-dash" : ""}`).attr("d", line(s.points));
  });
  if (!narrow) {
    // End labels, nudged apart upwards (from the lowest line end) so they never overlap or drop below the plot.
    const ends = d.series.map((s) => ({ s, y: y(clip(s.points[s.points.length - 1][1])) })).sort((a, b) => b.y - a.y);
    ends.forEach((e, i) => { if (i > 0 && ends[i - 1].y - e.y < 15) e.y = ends[i - 1].y - 15; });
    ends.forEach((e) => text(svg, W - RIGHT + 8, e.y + 4, e.s.short || e.s.name, "cp-t-val2"));
  }

  // Crosshair: snap to the nearest x of the first series and list every series' value there.
  const xs = d.series[0].points.map((p) => p[0]);
  const cross = svg.append("line").attr("class", "cp-zero").attr("y1", TOP).attr("y2", TOP + PLOT).style("opacity", 0);
  svg.append("rect").attr("x", LEFT).attr("y", TOP).attr("width", W - RIGHT - LEFT).attr("height", PLOT)
    .attr("fill", "transparent")
    .on("mousemove", (ev) => {
      const mx = x.invert(d3.pointer(ev)[0]);
      const i = d3.minIndex(xs, (v) => Math.abs(v - mx));
      cross.attr("x1", x(xs[i])).attr("x2", x(xs[i])).style("opacity", 1);
      tip.show(ev, `${d.x.label} ${fmt(xs[i])}: ` + d.series.map((s) => `${s.short || s.name} ${fmt(s.points[i][1])}${d.y.unit ? " " + d.y.unit : ""}`).join(" · "));
    })
    .on("mouseleave", () => { cross.style("opacity", 0); tip.hide(); });
});
