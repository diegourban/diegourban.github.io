#!/usr/bin/env python3
"""Searches the web for the post's wording: a free stand-in for a plagiarism checker. Warns, never fails.

  TAVILY_API_KEY=... python3 scripts/check_web_copies.py [--report report.md] content/posts/<slug>/index.md ...

Picks up to SAMPLES distinctive sentences per post, searches a 12-word stretch of each as an exact phrase
on Tavily (free tier: 1,000 searches a month), and lists results on sites other than this one, its repo and
the post's own cited sources. Without TAVILY_API_KEY it reports that it skipped.
"""
import argparse, json, os, re, sys, urllib.request
from urllib.parse import urlparse

sys.path.insert(0, os.path.dirname(__file__))
from check_posts import is_own, parse_references, split_post  # noqa: E402

SAMPLES = 12
PHRASE_WORDS = 12
API = "https://api.tavily.com/search"


def sentences(body):
    """Long prose sentences, spread evenly over the post. Sentences with inline code or numbers are left out:
    stripping those leaves gaps that would never match the published page word for word."""
    text = re.sub(r"```.*?```|\{\{[<%].*?[>%]\}\}", "\n", body, flags=re.S)
    text = re.sub(r"!?\[([^\]]*)\]\([^)]*\)", r"\1", text)
    text = re.sub(r"`[^`]*`", "§", text)
    found = []
    for line in text.splitlines():  # list items and paragraphs end at a line break
        line = re.sub(r"^\s*(#+|[-*]|\d+\.)\s+", "", line)
        found += [s.strip() for s in re.split(r"(?<=[.!?])\s+", line)]
    found = [s for s in found if len(s.split()) >= PHRASE_WORDS + 2 and not re.search(r"[\d§*_<>]", s)]
    step = max(1, len(found) // SAMPLES)
    return found[::step][:SAMPLES]


def phrase(sentence):
    """A 12-word stretch from the middle, without punctuation that would break an exact match."""
    w = re.sub(r"[^\w\s'’-]", " ", sentence).split()
    start = max(0, (len(w) - PHRASE_WORDS) // 2)
    return " ".join(w[start:start + PHRASE_WORDS])


def search(key, text):
    body = json.dumps({"query": f'"{text}"', "exact_match": True, "max_results": 5, "search_depth": "basic"}).encode()
    req = urllib.request.Request(API, data=body, headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return [x["url"] for x in json.load(r).get("results", [])]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("files", nargs="+")
    ap.add_argument("--report")
    args = ap.parse_args()
    key = os.environ.get("TAVILY_API_KEY")
    out = ["## Web search for copies (warning only)", ""]
    if not key:
        out.append("Skipped: no `TAVILY_API_KEY` secret set.")
    for path in args.files if key else []:
        fm, body = split_post(path)
        cited = {urlparse(r["url"]).netloc for r in parse_references(fm) or [] if r.get("url")}
        hits, errors, picked = [], [], sentences(body)
        for s in picked:
            p = phrase(s)
            try:
                urls = search(key, p)
            except Exception as e:
                errors.append(f"{type(e).__name__}: {e}"[:120])
                continue
            hits += [f'"{p}" → {u}' for u in urls if not is_own(u) and urlparse(u).netloc not in cited]
        out.append(f"### `{path}`")
        out.append(f"- Searched {len(picked) - len(errors)} of {len(picked)} phrases: "
                   + ("**found elsewhere, check these**" if hits else "nothing found elsewhere"))
        out += [f"  - {h}" for h in hits]
        out += [f"  - search error: {e}" for e in errors[:3]]
        out.append("")
    report = "\n".join(out)
    print(report)
    if args.report:
        open(args.report, "w", encoding="utf-8").write(report + "\n")


if __name__ == "__main__":
    main()
