# Platform Notes (platformnotes.dev)

A static site built with [Hugo](https://gohugo.io/) and the [PaperMod](https://github.com/adityatelange/hugo-PaperMod) theme, published to <https://platformnotes.dev/> by GitHub Pages on every push to `master`.

Every post with numbers ships its scripts and raw data in `benchmarks/<slug>/`, so any reader can reproduce them.

## Layout

| Path | What it is |
|---|---|
| `hugo.toml` | Site configuration: title, menu, social links, theme options |
| `content/posts/<slug>/index.md` | A post (page bundle: the post's images live in the same folder) |
| `content/about.md` | The About page |
| `benchmarks/<slug>/` | Scripts and raw data for each post; kept in the repo, not on the site |
| `archetypes/default.md` | Front matter template for new posts |
| `themes/PaperMod/` | The theme, as a git submodule pinned to a commit |
| `.github/workflows/hugo.yml` | Build and deploy to GitHub Pages |

## Day to day

```bash
git clone --recurse-submodules git@github.com:diegourban/diegourban.github.io.git
hugo server --buildDrafts                 # preview at http://localhost:1313, reloads on save
hugo new content posts/my-post/index.md   # new post, starts as draft: true
hugo --gc --minify                        # production build into public/ (drafts are left out)
```

A post is published only when its front matter changes to `draft: false` and the commit reaches `master`.

In a public repository, a draft's text is visible on GitHub even with `draft: true`. Drafts that aren't ready to be read stay on a local branch until they are.

Push over SSH: the `gh` HTTPS token rejects pushes that touch `.github/workflows/` unless it has the `workflow` scope.

## Charts

Posts draw charts with [D3](https://d3js.org/) 7.9.0, self-hosted in `assets/js/vendor/` and loaded only on pages that use the `chart` shortcode.

- **Data:** each post's generator (e.g. `benchmarks/default-cds/make_charts.py`) computes every displayed number from the raw results, rounds it half away from zero, and writes `content/posts/<slug>/charts.json`.
- **Charts:** `{{</* chart "name" */>}}Caption{{</* /chart */>}}` places a chart; its code lives in `assets/js/charts/<name>.js` and registers with `CPCharts.register`. Add new chart files to the list in `layouts/_partials/extend_footer.html`.
- **Tables:** `{{</* results-table */>}}` renders the default-CDS results table from the same `charts.json`, so the table and the charts never disagree.
- **Without JavaScript:** each chart shows its `aria` sentence from `charts.json` instead.
- **Social card:** `benchmarks/default-cds/make_cover.sh` renders `cover.png` (1200×630) with headless Chrome.

To refresh the default-CDS post after changing its data:

```bash
cd benchmarks/default-cds
python3 make_charts.py   # charts.json
./make_cover.sh          # cover.png
```

## SEO and analytics

- **Structured data:** `layouts/_partials/templates/schema_json.html` overrides PaperMod's. The home page is a `WebSite` + `Person` (from `[params.schema]` in `hugo.toml`), posts are `BlogPosting`s whose author and publisher point at that `Person`, and a page with `schemaType: "ProfilePage"` (About) is a `ProfilePage`.
- **Kept out of search and the sitemap:** `/search/` and the tag pages (`content/tags/_index.md`). Once tags group several posts each, delete the `cascade` there.
- **Social images:** posts set `cover` (with `relative: true`); everything else falls back to `static/site-card.png`, rendered by `scripts/make_site_card.sh`.
- **Favicons:** `scripts/favicon.svg` is the source; `scripts/make_favicons.sh` renders the five files PaperMod links into `static/` (`favicon.ico` is 48×48, the size Google uses in search results).
- **Page views:** [GoatCounter](https://www.goatcounter.com/), cookieless, dashboard at <https://diegourban.goatcounter.com>. `count.v5.js` is self-hosted and loaded in production builds only, so `hugo server` never counts. `assets/js/analytics.js` adds click events: `source`, `benchmarks` and `outbound`.
- **Where readers come from:** tag every link you share with `?ref=`, so sources stay separate even when the browser hides the referrer:

  | Where | Link |
  |---|---|
  | LinkedIn | `https://platformnotes.dev/posts/<slug>/?ref=linkedin` |
  | dev.to | `?ref=devto` |
  | Java Annotated Monthly | `?ref=jam` |
  | JVM Weekly | `?ref=jvmweekly` |
  | foojay.io | `?ref=foojay` |

## AI disclosure and training opt-out

- **Every post must declare `ai_disclosure`** in its front matter: `none`, `ai-assisted` or `ai-generated` (values from the [AI Content Disclosure for HTML](https://github.com/dweekly/ai-content-disclosure) proposal). The build fails without it. `layouts/_partials/ai_disclosure.html` holds the levels and their note text.
- The level shows as a note after the post (`extend_post_content.html`), as `<meta name="ai-disclosure">` (`extend_head.html`), and at the end of each item's summary in the RSS feed (`layouts/rss.xml`, an override of PaperMod's).
- **Training opt-out:** `layouts/robots.txt` sends `Content-Signal: search=yes, ai-input=yes, ai-train=no` and blocks the training crawlers by name; `static/.well-known/tdmrep.json` and `<meta name="tdm-reservation" content="1">` reserve text and data mining rights (W3C TDMRep). Search and AI-answer crawlers stay allowed.
- The About page explains the levels and the opt-out to readers.

## Publishing

GitHub Pages uses **Settings → Pages → Source = GitHub Actions**. The workflow builds with Hugo and publishes on every push to `master`, or manually from **Actions → Deploy Hugo site to Pages → Run workflow**.

**Custom domain:** `platformnotes.dev`, registered at Cloudflare.
- **Ownership:** verified on the GitHub account (profile → Settings → Pages → verified domains) with a TXT record, so no other account can claim it.
- **DNS, all DNS only (grey cloud):** the apex has 4 `A` records (`185.199.108.153` to `185.199.111.153`) and 4 `AAAA` records (`2606:50c0:8000::153` to `2606:50c0:8003::153`); `www` is a `CNAME` to `diegourban.github.io`. Cloudflare's proxy (orange cloud) stops GitHub from issuing the HTTPS certificate.
- **Repo setting:** Settings → Pages → Custom domain = `platformnotes.dev`, with Enforce HTTPS on. `diegourban.github.io` redirects to it.
- **Base URL:** the workflow takes it from Pages (`steps.pages.outputs.base_url`), so canonical URLs, the sitemap and `og:url` follow the custom domain without code changes.

The previous site (a 2017 Bootstrap CV page) is still in the history of `master`.

## Updating the theme

PaperMod is pinned to commit `d376885` (2026-08-02). Its latest tag, v8.0 (2024), doesn't work with Hugo 0.168. To update:

```bash
git -C themes/PaperMod fetch && git -C themes/PaperMod checkout <commit>
hugo server --buildDrafts   # check nothing broke
git add themes/PaperMod && git commit -m "Update PaperMod"
```

Keep the Hugo version in the workflow (`HUGO_VERSION`) the same as on your machine (`hugo version`).
