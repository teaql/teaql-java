#!/usr/bin/env python3
"""Promote previously qualified Maven files; no package build or lifecycle deploy."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import traceback
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

REPOSITORY = 'https://maven.teaql.io/repository/maven-releases'
FINGERPRINT = 'D195CDD10288573D650193DA6B9F1F8BE00BD595'


def digest(data):
    return hashlib.sha256(data).hexdigest()


def save(path, value):
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + '\n')


def verify(source, tag, expected_sha, manifest_path, packages):
    git = ['git', '-C', str(source)]
    assert subprocess.check_output(git + ['cat-file', '-t', tag], text=True).strip() == 'tag'
    signature = subprocess.run(git + ['verify-tag', '--raw', tag], capture_output=True, text=True, check=True)
    assert '[GNUPG:] VALIDSIG ' + FINGERPRINT + ' ' in signature.stderr
    commit = subprocess.check_output(git + ['rev-parse', tag + '^{commit}'], text=True).strip()
    assert subprocess.check_output(git + ['rev-parse', 'HEAD'], text=True).strip() == commit
    subprocess.run(git + ['merge-base', '--is-ancestor', commit, 'refs/remotes/origin/main'], check=True)
    tag_body = subprocess.check_output(git + ['cat-file', 'tag', tag], text=True)
    assert 'Qualification manifest SHA256: ' + expected_sha in tag_body
    raw = manifest_path.read_bytes()
    assert digest(raw) == expected_sha, 'Qualification manifest checksum differs'
    manifest = json.loads(raw)
    assert manifest['schema'] == 'teaql.logging-release.qualified-packages/v1'
    assert manifest['repository'] == 'teaql/teaql-java' and manifest['language'] == 'java'
    assert manifest['sourceCommit'] == commit and tag == 'v' + manifest['version']
    assert re.fullmatch(r'\d+\.\d+\.\d+', manifest['version'])
    internal = manifest['internalQualification']
    assert internal['passed'] is True and internal['rounds'] == 2
    assert re.fullmatch(r'[a-f0-9]{40}', internal['generatorCommit'])
    assert re.fullmatch(r'[a-f0-9]{64}', internal['modelSHA256'])
    assert re.fullmatch(r'[a-f0-9]{64}', internal['receiptSHA256'])
    pom = ET.fromstring(subprocess.check_output(git + ['show', commit + ':pom.xml']))
    modules = [node.text for node in pom.findall('{*}modules/{*}module') if not node.text.startswith('examples/')]
    assert len(modules) == 34 and all('/' not in name for name in modules)
    version = manifest['version']
    names = {'teaql-java-parent-' + version + '.pom'}
    names.update(module + '-' + version + suffix for module in modules for suffix in ['.pom', '.jar', '-sources.jar'])
    artifacts = {row['file']: row for row in manifest['artifacts']}
    assert len(manifest['artifacts']) == len(artifacts) == len(names) == 103 and set(artifacts) == names
    assert {file.name for file in packages.iterdir()} == names
    for name, item in artifacts.items():
        path = packages / name
        assert path.is_file() and not path.is_symlink() and path.stat().st_size == item['bytes']
        assert digest(path.read_bytes()) == item['sha256'], 'Qualified artifact checksum differs: ' + name
    for module in ['teaql-java-parent', *modules]:
        metadata = ET.parse(packages / (module + '-' + version + '.pom')).getroot()
        assert metadata.findtext('{*}artifactId') == module
        assert (metadata.findtext('{*}groupId') or metadata.findtext('{*}parent/{*}groupId')) == 'io.teaql'
        assert (metadata.findtext('{*}version') or metadata.findtext('{*}parent/{*}version')) == version
    return manifest, modules


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--tag', required=True)
    parser.add_argument('--manifest-sha256', required=True)
    parser.add_argument('--qualification', type=Path)
    parser.add_argument('--packages', type=Path)
    parser.add_argument('--settings', type=Path)
    parser.add_argument('--deploy', action='store_true')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    assert re.fullmatch(r'v\d+\.\d+\.\d+', args.tag)
    assert re.fullmatch(r'[a-f0-9]{64}', args.manifest_sha256)
    assert bool(args.qualification) == bool(args.packages)
    assert not args.deploy or args.settings is not None
    output = args.output.resolve(); output.mkdir()
    receipt = dict(schema='teaql.logging-release.qualified-maven-promotion/v1', passed=False,
        qualificationSHA256=args.manifest_sha256, repository=REPOSITORY, tag=args.tag,
        sourceBuilt=False, credentialValuesRecorded=False, deployRequested=args.deploy, commands=[], artifacts=[],
        startedAt=datetime.datetime.now(datetime.timezone.utc).isoformat())
    scratch_name = None
    try:
        with tempfile.TemporaryDirectory(prefix='teaql-qualified-maven-promotion-') as temporary:
            scratch_name = temporary; scratch = Path(temporary)
            def run(command, label, cwd=scratch, timeout=600):
                result = subprocess.run(command, cwd=cwd, capture_output=True, timeout=timeout)
                log = output / (label + '.log'); log.write_bytes(result.stdout + result.stderr)
                receipt['commands'].append(dict(command=[str(value) for value in command], exitCode=result.returncode,
                    log=log.name, logSHA256=digest(log.read_bytes())))
                assert result.returncode == 0, 'Native command failed; inspect ' + log.name
                return result.stdout
            if args.qualification:
                qualification, packages = args.qualification.resolve(), args.packages.resolve()
            else:
                metadata = scratch / 'metadata'; metadata.mkdir()
                packages = scratch / 'packages'; packages.mkdir()
                run(['gh', 'release', 'download', args.tag, '--repo', 'teaql/teaql-java', '--pattern',
                    'logging-qualification.json', '--dir', str(metadata)], 'download-qualification')
                qualification = metadata / 'logging-qualification.json'
                # Verify the manifest identity before using any of its names.
                assert digest(qualification.read_bytes()) == args.manifest_sha256
                run(['gh', 'release', 'download', args.tag, '--repo', 'teaql/teaql-java', '--pattern', '*.pom',
                    '--pattern', '*.jar', '--dir', str(packages)], 'download-qualified-files')
            manifest, modules = verify(args.source.resolve(), args.tag, args.manifest_sha256, qualification, packages)
            receipt.update(sourceCommit=manifest['sourceCommit'], version=manifest['version'],
                verifiedNativeFiles=103, signedManifestBindingVerified=True, modules=modules)
            if args.deploy:
                assert args.settings.is_file()
                receipt['credentialsOrigin'] = 'authorized GitHub Maven environment via setup-java server TEAQL'
                base = scratch / 'standalone-pom.xml'
                base.write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>'
                    '<groupId>release.promotion</groupId><artifactId>qualified-maven-files</artifactId><version>0.0.1</version></project>\n')
                opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
                def fetch(url):
                    assert url.startswith(REPOSITORY + '/io/teaql/')
                    try:
                        with opener.open(url, timeout=45) as response:
                            assert response.geturl().startswith(REPOSITORY + '/io/teaql/')
                            return response.read()
                    except urllib.error.HTTPError as error:
                        if error.code == 404: return None
                        raise
                version = manifest['version']
                artifacts = {item['file']: item for item in manifest['artifacts']}
                groups = []
                # Read every destination before any deployment. Conflicting
                # or partial pre-existing groups fail without any write.
                for module in ['teaql-java-parent', *modules]:
                    names = [module + '-' + version + '.pom']
                    if module != 'teaql-java-parent':
                        names += [module + '-' + version + '.jar', module + '-' + version + '-sources.jar']
                    existing = []
                    for name in names:
                        url = REPOSITORY + '/io/teaql/' + module + '/' + version + '/' + name
                        data = fetch(url); existing.append(data is not None)
                        if data is not None:
                            assert digest(data) == artifacts[name]['sha256'], 'Existing artifact differs; overwrite refused: ' + name
                    assert all(existing) or not any(existing), 'Partial existing native group requires inspection: ' + module
                    groups.append((module, names, all(existing)))
                for module, names, reused in groups:
                    if not reused:
                        pom_file = packages / (module + '-' + version + '.pom')
                        primary = pom_file if module == 'teaql-java-parent' else packages / (module + '-' + version + '.jar')
                        command = ['mvn', '-B', '-ntp', '-N', '-f', str(base), '-s', str(args.settings),
                            '-Dmaven.repo.local=' + str(scratch / 'maven-repository'),
                            'org.apache.maven.plugins:maven-deploy-plugin:3.1.4:deploy-file',
                            '-DrepositoryId=TEAQL', '-Durl=' + REPOSITORY, '-Dfile=' + str(primary),
                            '-DpomFile=' + str(pom_file), '-DgeneratePom=false', '-DretryFailedDeploymentCount=1']
                        if module != 'teaql-java-parent':
                            command += ['-Dfiles=' + str(packages / (module + '-' + version + '-sources.jar')),
                                '-Dclassifiers=sources', '-Dtypes=jar']
                        run(command, 'deploy-' + module)
                    for name in names:
                        url = REPOSITORY + '/io/teaql/' + module + '/' + version + '/' + name
                        data = fetch(url)
                        assert data is not None and digest(data) == artifacts[name]['sha256']
                        receipt['artifacts'].append(dict(**artifacts[name], url=url, readbackSHA256=digest(data),
                            qualifiedPayloadIdentical=True, reusedExactExisting=reused))
                receipt['nativeGroupsVerified'] = len(groups)
            receipt['passed'] = True
    except Exception as error:
        receipt['error'] = type(error).__name__ + ': ' + str(error)
        (output / 'failure.txt').write_text(traceback.format_exc())
    finally:
        receipt['scratchRemoved'] = scratch_name is None or not Path(scratch_name).exists()
        receipt['passed'] = receipt['passed'] and receipt['scratchRemoved']
        save(output / 'receipt.json', receipt)
    print(json.dumps({key: receipt.get(key) for key in ['passed', 'error', 'sourceCommit', 'verifiedNativeFiles', 'nativeGroupsVerified', 'scratchRemoved']}))
    return 0 if receipt['passed'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
