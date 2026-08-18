#!/usr/bin/env python3
"""Validate that every shipped locale has the same non-empty key set."""
import json, pathlib, sys
root = pathlib.Path(__file__).parents[1] / 'app/src/main/assets/site/i18n'
files = sorted(root.glob('*.json'))
required = {'nl','en','de','fr','es','it','pt','pl'}
found = {p.stem for p in files}
if found != required:
    print(f'locale set mismatch: expected {sorted(required)}, got {sorted(found)}'); sys.exit(1)
data = {p.stem: json.loads(p.read_text(encoding='utf-8')) for p in files}
keys = set(data['nl'])
errors = []
for lang, values in data.items():
    missing = keys - set(values)
    extra = set(values) - keys
    empty = [k for k,v in values.items() if not isinstance(v,str) or not v.strip()]
    if missing: errors.append(f'{lang}: missing {sorted(missing)}')
    if extra: errors.append(f'{lang}: extra {sorted(extra)}')
    if empty: errors.append(f'{lang}: empty {sorted(empty)}')
if errors:
    print('\n'.join(errors)); sys.exit(1)
print(f'OK: {len(keys)} keys across {len(data)} locales')
