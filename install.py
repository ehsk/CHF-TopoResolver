import argparse
import urllib.request as url_request
from urllib.error import URLError, HTTPError
import zipfile
import tarfile
import os
import shutil
import re
import subprocess
import timeit

parser = argparse.ArgumentParser()
parser.add_argument('--geonames_url', type=str,
                    default="http://download.geonames.org/export/dump/allCountries.zip",
                    help="GeoNames data URL to download")
parser.add_argument('--no_redis', action='store_true',
                    help="In case, no Redis installation is need (not recommended)")
parser.add_argument('--redis_port', type=int, default=6384, help="Redis port")
parser.add_argument('--redis_host', type=str, default='localhost', help="Redis host")
parser.add_argument('--redis_url', type=str,
                    default="http://download.redis.io/releases/redis-4.0.9.tar.gz",
                    help="Redis URL to download")
parser.add_argument('--maven_url', type=str,
                    default="http://apache.mirror.rafal.ca/maven/maven-3/3.5.3/binaries/apache-maven-3.5.3-bin.tar.gz",
                    help="Apache Maven URL to download")
args = parser.parse_args()

java_version = subprocess.check_output(["java", "-version"], stderr=subprocess.STDOUT)
if java_version:
    java_version = java_version.decode('utf-8')
    version_check = re.match(r'java version "1\.8\..*"', java_version, re.I)
    if not version_check:
        version_check = re.match(r'java version "(\d+)\.\d+.\d+(_\d+)?"', java_version, re.I)
        if version_check:
            if int(version_check.group(1)) < 8:
                print("Java version must be at least 8")
                exit(1)
        else:
            print("Please install Java 8 to proceed")
            exit(1)
else:
    print("Please install Java 8 to proceed")
    exit(1)


geonames_dir = os.path.join('data', 'gazetteer')
if not os.path.exists(geonames_dir):
    os.mkdir(geonames_dir)

tools_dir = 'tools'
if not os.path.exists(tools_dir):
    os.mkdir(tools_dir)


def prepare_geonames():
    geonames_zipfile = os.path.join(geonames_dir, 'allCountries.zip')
    print('Downloading GeoNames...')
    try:
        with url_request.urlopen(args.geonames_url) as resp, \
                open(geonames_zipfile, 'wb') as out_file:
            data = resp.read()  # a `bytes` object
            out_file.write(data)
    except HTTPError as e:
        if e.code == 404:
            print(
                'The provided URL seems to be broken. Please find a URL for GeoNames data file, named allCountries.zip')
        else:
            print('Error code: ', e.code)
    except URLError as e:
        print('URL error: ', e.reason)
    print('Extracting GeoNames...')
    with zipfile.ZipFile(geonames_zipfile, 'r') as zip_ref:
        zip_ref.extractall(geonames_dir)

    os.remove(geonames_zipfile)
    print('GeoNames is ready!')


def prepare_redis():
    redis_tgzfile = os.path.join(tools_dir, 'redis.tar.gz')
    print('Downloading Redis...')
    try:
        with url_request.urlopen(args.redis_url) as resp, \
                open(redis_tgzfile, 'wb') as out_file:
            data = resp.read()  # a `bytes` object
            out_file.write(data)
    except HTTPError as e:
        if e.code == 404:
            print(
                'The provided URL seems to be broken. Please find a URL for Redis')
        else:
            print('Error code: ', e.code)
        raise ValueError()
    except URLError as e:
        print('URL error: ', e.reason)
        raise ValueError()

    print('Extracting Redis...')
    with tarfile.open(redis_tgzfile, 'r:gz') as tgz_ref:
        tgz_ref.extractall(tools_dir)

    os.remove(redis_tgzfile)

    redis_dir = None
    for f in os.listdir(tools_dir):
        if f.lower().startswith('redis'):
            redis_dir = os.path.join(tools_dir, f)
            break

    if not redis_dir:
        raise ValueError

    if not os.path.exists(os.path.join(tools_dir, 'redis')):
        os.rename(redis_dir, os.path.join(tools_dir, 'redis'))

    redis_dir = os.path.join(tools_dir, 'redis')

    print('Installing Redis...')
    subprocess.call(
        ['sed', '-i', 's/tcp-backlog [0-9]\+$/tcp-backlog 3000/g', os.path.join(redis_dir, 'redis.conf')])
    subprocess.call(
        ['sed', '-i', 's/daemonize no$/daemonize yes/g', os.path.join(redis_dir, 'redis.conf')])
    subprocess.call(
        ['sed', '-i', 's/pidfile .*\.pid$/pidfile redis_6384.pid/g', os.path.join(redis_dir, 'redis.conf')])
    subprocess.call(
        ['sed', '-i', 's/port 6379/port 6384/g', os.path.join(redis_dir, 'redis.conf')])
    subprocess.call(['make'], cwd=redis_dir)

    print('Running Redis on port 6384...')
    subprocess.call(['src/redis-server', 'redis.conf'], cwd=redis_dir)


def prepare_maven():
    maven_tgzfile = os.path.join(tools_dir, 'apache-maven.tar.gz')
    print('Downloading Apache Maven...')
    try:
        with url_request.urlopen(args.maven_url) as resp, \
                open(maven_tgzfile, 'wb') as out_file:
            data = resp.read()  # a `bytes` object
            out_file.write(data)
    except HTTPError as e:
        if e.code == 404:
            print(
                'The provided URL seems to be broken. Please find a URL for Apache Maven')
        else:
            print('Error code: ', e.code)
        raise ValueError()
    except URLError as e:
        print('URL error: ', e.reason)
        raise ValueError()

    print('Extracting Apache Maven...')
    with tarfile.open(maven_tgzfile, 'r:gz') as tgz_ref:
        tgz_ref.extractall(tools_dir)

    os.remove(maven_tgzfile)

    maven_dir = None
    for f in os.listdir(tools_dir):
        if f.lower().startswith('apache-maven'):
            maven_dir = os.path.join(tools_dir, f)
            break

    if not maven_dir:
        raise ValueError

    if os.path.exists(os.path.join(tools_dir, 'maven')):
        shutil.rmtree(os.path.join(tools_dir, 'maven'))

    os.rename(maven_dir, os.path.join(tools_dir, 'maven'))


def run_importer():
    mvn = os.path.join(tools_dir, 'maven', 'bin', 'mvn')

    print('Building the source...')
    subprocess.call([mvn, 'clean', 'compile'])

    print('Running importer...')
    geonames_file = os.path.join(geonames_dir, 'allCountries.txt')
    subprocess.call(
        [mvn, 'exec:exec', '-Pimport',
         '-Darg.geonames.path={}'.format(geonames_file),
         '-Darg.redis.host={}'.format(args.redis_host),
         '-Darg.redis.port={}'.format(args.redis_port)])


start = timeit.default_timer()
geonames_start = timeit.default_timer()

prepare_geonames()
geonames_done = timeit.default_timer()

redis_start = timeit.default_timer()
if not args.no_redis:
    prepare_redis()
redis_done = timeit.default_timer()

maven_start = timeit.default_timer()
prepare_maven()
maven_done = timeit.default_timer()

importer_start = timeit.default_timer()
run_importer()
importer_done = timeit.default_timer()

done = timeit.default_timer()

print('GeoNames done in {:.1f}s'.format(geonames_done - geonames_start))
print('Redis done in {:.1f}s'.format(redis_done - redis_start))
print('Maven done in {:.1f}s'.format(maven_done - maven_start))
print('Importer done in {:.1f}s'.format(importer_done - importer_start))
print('The whole process took {:.1f}s'.format(done - start))
print('Installation complete.')
