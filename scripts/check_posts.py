#!/usr/bin/env python3
"""Pre-publish checks for posts: the references lint and the overlap with cited sources.

  python3 scripts/check_posts.py [--report report.md] [content/posts/<slug>/index.md ...]

With no files it checks every post. Exit code 1 if any check fails, 0 otherwise.

References lint (fails):
  - front matter has a `references` list, each entry with author, title and an http(s) url;
  - every external link in the text is listed there (links to this site and its repo are exempt);
  - a post marked `ai_disclosure: ai-generated` cites at least one source.
Overlap (fails on a match, never on a fetch error):
  - downloads every cited source and reports any run of 8 or more words the post shares with it.
    Code, inline code, shortcodes, link targets and text in "quotation marks" are left out.
  - a reference with `checked: "by hand"` is skipped: use it for sources scripts can't read (journals behind
    bot protection, scanned PDFs) once you've compared them yourself.
Stdlib only, so it runs on a bare CI runner.
"""
import argparse, glob, html, re, sys, urllib.request

RUN = 8  # words in a row that count as copied wording
MIN_SOURCE_WORDS = 200  # below this the page is a bot challenge or JS shell (e.g. JSTOR), not the source
OWN = ("platformnotes.dev", "diegourban.github.io", "github.com/diegourban/diegourban.github.io")
UA = {"User-Agent": "Mozilla/5.0 (platformnotes.dev post checks)"}


def split_post(path):
    """Returns (front matter text, body text)."""
    text = open(path, encoding="utf-8").read()
    _, fm, body = text.split("---\n", 2)
    return fm, body


def parse_references(fm):
    """Reads the `references:` list from front matter (the flat key: "value" shape the posts use)."""
    refs, cur, inside = [], None, False
    for line in fm.splitlines():
        if line.startswith("references:"):
            inside = True
            continue
        if inside and line and not line.startswith(" "):
            break
        if not inside:
            continue
        m = re.match(r"\s*(-\s+)?(\w+):\s*(.*)$", line)
        if not m:
            continue
        if m.group(1):
            cur = {}
            refs.append(cur)
        cur[m.group(2)] = m.group(3).strip().strip('"')
    return refs if inside else None


def scalar(fm, key):
    m = re.search(rf"^{key}:\s*(.+)$", fm, re.M)
    return m.group(1).strip().strip('"') if m else None


def strip_fragment(url):
    return url.split("#", 1)[0].rstrip("/")


def is_own(url):
    return any(o in url for o in OWN)


def lint(path, fm, body):
    problems = []
    refs = parse_references(fm)
    if refs is None:
        return ["no `references` list in the front matter"], []
    for i, r in enumerate(refs, 1):
        for k in ("author", "title", "url"):
            if not r.get(k):
                problems.append(f"reference {i} has no `{k}`")
        if r.get("url") and not re.match(r"https?://", r["url"]):
            problems.append(f"reference {i} url is not http(s): {r['url']}")
        if r.get("checked") not in (None, "by hand"):
            problems.append(f'reference {i} has `checked: {r["checked"]}`; the only value is "by hand"')
    listed = {strip_fragment(r["url"]) for r in refs if r.get("url")}
    links = re.findall(r"\]\((https?://[^)\s]+)\)", body) + re.findall(r"<(https?://[^>\s]+)>", body)
    for url in sorted(set(links)):
        if not is_own(url) and strip_fragment(url) not in listed:
            problems.append(f"link not in references: {url}")
    if scalar(fm, "ai_disclosure") == "ai-generated" and not refs:
        problems.append("`ai_disclosure: ai-generated` but no sources cited")
    return problems, refs


def prose(body):
    """The post's own wording: no code, shortcodes, link targets, HTML or quoted text."""
    t = re.sub(r"```.*?```", " ", body, flags=re.S)
    t = re.sub(r"\{\{[<%].*?[>%]\}\}", " ", t, flags=re.S)
    t = re.sub(r"`[^`]*`", " ", t)
    t = re.sub(r"!?\[([^\]]*)\]\([^)]*\)", r"\1", t)
    t = re.sub(r"<[^>]+>", " ", t)
    t = re.sub(r"[\"“][^\"”\n]{1,400}[\"”]", " ", t)  # quoted on purpose, with a citation next to it
    return t


def words(text):
    return re.findall(r"[a-z0-9]+(?:'[a-z]+)?", text.lower().replace("’", "'"))


def page_text(url):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=30) as r:
        raw = r.read(5_000_000).decode("utf-8", "replace")
    raw = re.sub(r"<(script|style)\b.*?</\1>", " ", raw, flags=re.S | re.I)
    return html.unescape(re.sub(r"<[^>]+>", " ", raw))


def overlaps(post_words, source_words):
    """Maximal runs of at least RUN words that appear in both, as strings."""
    grams = {tuple(source_words[i:i + RUN]) for i in range(len(source_words) - RUN + 1)}
    hit = [tuple(post_words[i:i + RUN]) in grams for i in range(len(post_words) - RUN + 1)]
    runs, i = [], 0
    while i < len(hit):
        if hit[i]:
            j = i
            while j + 1 < len(hit) and hit[j + 1]:
                j += 1
            runs.append(" ".join(post_words[i:j + RUN]))
            i = j + 1
        else:
            i += 1
    return runs


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("files", nargs="*")
    ap.add_argument("--report", help="also write the Markdown report to this file")
    args = ap.parse_args()
    files = args.files or sorted(glob.glob("content/posts/*/index.md"))
    out, failed = ["## References and overlap", ""], False
    for path in files:
        fm, body = split_post(path)
        problems, refs = lint(path, fm, body)
        out.append(f"### `{path}`")
        out.append(f"- References lint: {'**failed**' if problems else 'passed'}")
        out += [f"  - {p}" for p in problems]
        failed |= bool(problems)
        post_words = words(prose(body))
        matches, unreachable = [], []
        by_hand = {strip_fragment(r["url"]) for r in refs or [] if r.get("url") and r.get("checked") == "by hand"}
        for url in sorted({strip_fragment(r["url"]) for r in refs or [] if r.get("url")} - by_hand):
            try:
                source_words = words(page_text(url))
            except Exception as e:  # a source being down never blocks the PR
                unreachable.append(f"{url} ({type(e).__name__}: {e})"[:160])
                continue
            if len(source_words) < MIN_SOURCE_WORDS:
                unreachable.append(f"{url} (only {len(source_words)} words came back, compare it by hand)")
                continue
            matches += [f'"{r}" → {url}' for r in overlaps(post_words, source_words)]
        out.append(f"- Overlap with cited sources ({RUN}+ words in a row): {'**failed**' if matches else 'passed'}")
        out += [f"  - {m}" for m in matches]
        if matches:
            out.append("  - Rewrite these in your own words, or quote them with the citation next to them.")
        out += [f"  - couldn't check {u}" for u in unreachable]
        if by_hand:
            out.append(f"  - {len(by_hand)} source{'s' if len(by_hand) > 1 else ''} marked as compared by hand")
        failed |= bool(matches)
        out.append("")
    report = "\n".join(out)
    print(report)
    if args.report:
        open(args.report, "w", encoding="utf-8").write(report + "\n")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
