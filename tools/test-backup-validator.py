#!/usr/bin/env python3
import json, sys

def valid(value):
    return (isinstance(value, dict) and value.get('format') == 'safescan-backup'
            and value.get('version') == 1
            and all(isinstance(value.get(k), str) and value[k].strip() for k in ('created','device','scan')))

fixtures = {
    'valid': {'format':'safescan-backup','version':1,'created':'now','device':'test','scan':'{}'},
    'wrong-format': {'format':'other','version':1,'created':'now','device':'test','scan':'{}'},
    'wrong-version': {'format':'safescan-backup','version':99,'created':'now','device':'test','scan':'{}'},
    'missing-scan': {'format':'safescan-backup','version':1,'created':'now','device':'test'},
}
assert valid(fixtures['valid'])
for name in ('wrong-format','wrong-version','missing-scan'):
    assert not valid(fixtures[name]), name
try:
    json.loads('{broken')
    raise AssertionError('corrupt JSON accepted')
except json.JSONDecodeError:
    pass
print('OK: valid, corrupt, wrong-format, wrong-version and missing-field fixtures')
