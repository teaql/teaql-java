#!/usr/bin/env python3
"""Export diagnostics and observations, excluding credentials and deployed binaries."""
from pathlib import Path
import tarfile
base = Path(__file__).resolve().parent
allowed = {'.csv', '.json', '.log', '.txt', '.args'}
skip = {'toolchain', 'lib', 'aot', 'current-spring', 'current-quarkus', 'current-micronaut'}
with tarfile.open('/tmp/teaql-jdk25-27-evidence.tgz', 'w:gz') as archive:
    for p in sorted(base.rglob('*')):
        rel = p.relative_to(base)
        if any(part in skip for part in rel.parts) or p.is_symlink() or not p.is_file():
            continue
        if p.suffix in allowed:
            archive.add(p, arcname=str(rel), recursive=False)
print('/tmp/teaql-jdk25-27-evidence.tgz')
