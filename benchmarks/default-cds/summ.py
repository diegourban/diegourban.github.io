import json, sys, statistics as s, collections
d = collections.defaultdict(list)
for l in open(sys.argv[1]):
    if l.startswith("{"):
        r = json.loads(l); d[r["variant"]].append(r)
print("variant             n  process  ready  cpu_s  anon  file  metasp")
for v, rs in sorted(d.items()):
    m = lambda k: s.median(x[k] for x in rs)
    print(f"{v:18} {len(rs):2} {m('process'):7.2f} {m('ready_wall'):6.2f} {m('cpu_s'):6.2f} {m('rss_anon'):5.0f} {m('rss_file'):5.0f} {m('metaspace'):6.1f}")
