---
title: "{{ replace .File.ContentBaseName "-" " " | title }}"
date: {{ .Date }}
draft: true
description: ""
tags: []
# Required: how much AI was involved. One of: none, ai-assisted, ai-generated (the build fails without it).
ai_disclosure: ""
# Put the scripts and raw data for this post under benchmarks/<slug>/ and link them.
# Every source the post links to or builds on (scripts/check_posts.py fails without it). One entry per source:
#   - author: "OpenJDK"
#     title: "JEP 341: Default CDS Archives"
#     venue: "Delivered in JDK 12"   # optional
#     year: 2019                     # optional
#     url: "https://openjdk.org/jeps/341"
#     checked: "by hand"             # optional: you compared it yourself because scripts can't read it
references: []
---
