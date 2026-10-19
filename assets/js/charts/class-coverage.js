// Chart C: share of the classes loaded during startup that come from the default CDS archive.
CPCharts.register("class-coverage", (el, data, narrow) => {
  const { tooltip, svgFor, text, barPath } = CPCharts;
  const rows = data.coverage;
  const BAR = 16, ROW = narrow ? 58 : 44, TOP = 62;
  const W = el.clientWidth, LEFT = narrow ? 0 : 110, RIGHT = narrow ? 0 : 230;
  const H = TOP + ROW * rows.length + 4;
  const svg = svgFor(el, H);
  const tip = tooltip(el);

  text(svg, 0, 18, "Classes served by the default CDS archive", "cp-t-title");
  text(svg, 0, 40, "Blue: from the archive · gray: loaded and parsed at startup", "cp-t-sub");

  rows.forEach((r, i) => {
    const y = TOP + ROW * i + (narrow ? 20 : 0);
    const x = d3.scaleLinear([0, r.loaded], [LEFT, W - RIGHT]);
    text(svg, 0, narrow ? y - 6 : y + BAR - 3, r.app, "cp-t-lab");
    const split = x(r.shared);
    svg.append("path").attr("class", "cp-bar-1").attr("d", barPath(LEFT, split, y, BAR))
      .on("mousemove", (ev) => tip.show(ev, `${r.app}: ${r.shared_label} classes from the archive`))
      .on("mouseleave", tip.hide);
    svg.append("path").attr("class", "cp-bar-rest").attr("d", barPath(split + 2, x(r.loaded), y, BAR))
      .on("mousemove", (ev) => tip.show(ev, `${r.app}: ${r.loaded_label} classes loaded in total`))
      .on("mouseleave", tip.hide);
    const label = `${r.shared_label} of ${r.loaded_label} (${r.pct_label})`;
    if (narrow) text(svg, W, y - 6, label, "cp-t-val2").attr("text-anchor", "end");
    else text(svg, W - RIGHT + 12, y + BAR - 3, label, "cp-t-val");
  });
});
