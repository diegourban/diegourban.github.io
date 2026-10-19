# diegourban.github.io

A static site built with [Hugo](https://gohugo.io/) and the [PaperMod](https://github.com/adityatelange/hugo-PaperMod) theme, published to <https://diegourban.github.io/> by GitHub Pages on every push to `master`.

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

## Publishing

GitHub Pages uses **Settings → Pages → Source = GitHub Actions**. The workflow builds with Hugo and publishes on every push to `master`, or manually from **Actions → Deploy Hugo site to Pages → Run workflow**.

Custom domain (optional): **Settings → Pages → Custom domain**, plus a `CNAME` record in your DNS pointing to `diegourban.github.io`.

The previous site (a 2017 Bootstrap CV page) is still in the history of `master`.

## Updating the theme

PaperMod is pinned to commit `d376885` (2026-08-02). Its latest tag, v8.0 (2024), doesn't work with Hugo 0.168. To update:

```bash
git -C themes/PaperMod fetch && git -C themes/PaperMod checkout <commit>
hugo server --buildDrafts   # check nothing broke
git add themes/PaperMod && git commit -m "Update PaperMod"
```

Keep the Hugo version in the workflow (`HUGO_VERSION`) the same as on your machine (`hugo version`).
