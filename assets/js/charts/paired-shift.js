// Chart A: per-setup paired startup differences, Hodges-Lehmann tick and 95% CI bar.
// The axis is cut at XMIN/XMAX; points beyond it are drawn at the edge with their value.
CPCharts.register("paired-shift", (el, data, narrow) => {
  const { tooltip, svgFor, text } = CPCharts;
  const rows = data.rows;
  const XMIN = -200, XMAX = 250;
  const W = el.clientWidth, ROW = narrow ? 64 : 56, TOP = 70;
  const LEFT = narrow ? 112 : 150, RIGHT = narrow ? 56 : 212;
  const H = TOP + ROW * rows.length + 34;
  const x = d3.scaleLinear([XMIN, XMAX], [LEFT, W - RIGHT]);
  const svg = svgFor(el, H);
  const tip = tooltip(el);
  const JIT = [-7, 5, -2, 8, -5, 2, -8, 6, -3, 4];

  text(svg, 0, 18, "Startup change after removing -Xshare:off", "cp-t-title");
  text(svg, 0, 40, "Each dot is one round. Left of 0 is faster.", "cp-t-sub");
  text(svg, 0, 58, "Bar: 95% CI · tick: Hodges-Lehmann estimate", "cp-t-sub");

  const axisY = TOP + ROW * rows.length;
  d3.range(XMIN, XMAX + 1, 100).forEach((t) => {
    svg.append("line").attr("class", t === 0 ? "cp-zero" : "cp-grid")
      .attr("x1", x(t)).attr("x2", x(t)).attr("y1", TOP - 4).attr("y2", axisY);
    text(svg, x(t), axisY + 18, t === 0 ? "0 ms" : `${t > 0 ? "+" : "−"}${Math.abs(t)}`, "cp-t-tick")
      .attr("text-anchor", "middle");
  });

  rows.forEach((r, i) => {
    const cy = TOP + ROW * i + ROW / 2;
    text(svg, 0, narrow ? cy - 2 : cy + 5, r.setup, "cp-t-lab");
    if (narrow) text(svg, 0, cy + 15, `${r.shift.label} ms · ${r.faster}/${r.n}`, "cp-t-val2");

    svg.append("line").attr("class", "cp-ci")
      .attr("x1", x(Math.max(r.ci.lo, XMIN))).attr("x2", x(Math.min(r.ci.hi, XMAX)))
      .attr("y1", cy).attr("y2", cy);

    r.diffs.forEach((d, j) => {
      const over = d.v > XMAX, under = d.v < XMIN;
      // Out-of-range points sit just outside the plot edge, so they never cover in-range dots.
      const px = over ? x(XMAX) + 14 : under ? x(XMIN) - 14 : x(d.v), py = cy + JIT[j];
      const say = (ev) => tip.show(ev, `${r.setup}, round ${j + 1}: ${d.label} ms`);
      if (over || under) {
        const s = over ? 1 : -1;
        svg.append("path").attr("class", "cp-dot")
          .attr("d", `M${px - 5 * s},${py - 5}L${px + 5 * s},${py}L${px - 5 * s},${py + 5}Z`)
          .on("mousemove", say).on("mouseleave", tip.hide);
        text(svg, px + 8 * s, py + 4, `${d.label}`, "cp-t-val2").attr("text-anchor", over ? "start" : "end");
      } else {
        svg.append("circle").attr("class", "cp-dot").attr("cx", px).attr("cy", py).attr("r", 5)
          .on("mousemove", say).on("mouseleave", tip.hide);
      }
    });

    svg.append("line").attr("class", "cp-hl")
      .attr("x1", x(r.shift.v)).attr("x2", x(r.shift.v)).attr("y1", cy - 13).attr("y2", cy + 13)
      .on("mousemove", (ev) => tip.show(ev, `${r.setup}: ${r.shift.label} ms, 95% CI ${r.ci.label} ms, ${r.faster}/${r.n} faster`))
      .on("mouseleave", tip.hide);

    if (!narrow) {
      const vx = W - RIGHT + 64;
      text(svg, vx, cy, `${r.shift.label} ms`, "cp-t-val");
      text(svg, vx, cy + 16, `CI ${r.ci.label} · ${r.faster}/${r.n} faster`, "cp-t-val2");
    }
  });
});
