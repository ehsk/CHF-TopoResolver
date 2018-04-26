# CHF-TopoResolver
This repository is the implemenation of the paper: "A Coherent Unsupervised Model for Toponym Resolution".

- [Installation](#installation)
- [Getting Started](#getting-started)
- [Reference](#reference)

## Installation
You need Java 8 (or higher) to use the toponym resolver. 
All library dependencies are specified in a Maven [pom](pom.xml) file.
 
The GeoNames data are stored in SQLite and [Redis](https://redis.io/).

We provided a [script](install.py), written in Python 3, for Linux machines to lay the groundwork. 
For a quick start, just run
```
   python3 install.py
``` 
The script downloads GeoNames [data](http://download.geonames.org/export/dump/), Redis and Apache Maven. 
After extracting the downloaded files, it prepares a Redis instance by compiling and running it on port `6384` by default.

Once done, the installer runs an importer using Maven to build the SQLite database and initiate the Redis keys.
The whole process takes less than 5 minutes to install the requirements and roughly 30 minutes to import the databases
 (Tested on Ubuntu 14.04 with 4 CPU-cores and 8GB memory).
 
If you do not need a new Redis instance, specify only the host and port of the instance using the following command:
```
   python3 install.py --no_redis --redis_port <PORT> --redis_host <HOST> 
```
This will bypass the Redis installation part.

Here is more details about the arguments for the install script:
```
    --no_redis        In case no Redis installation is need (not recommended)
    --redis_port      Redis port (default: 6384)
    --redis_host      Redis host (default: localhost)
    --redis_url       Redis URL to download
    --geonames_url    GeoNames data URL to download
    --maven_url       Apache Maven URL to download
``` 
The above URLs are provided by default. If the links were broken, you can pass new URLs using the above arguments.

## Getting Started
You can create a `GeoTagger` instance for toponym recognition and resolution. 
Here is the simplest way to extract toponyms:

```java
GeoTagger geoTagger = new GeoTagger();
List<Toponym> toponyms = geoTagger.extractToponyms("");
for (Toponym toponym : toponyms)
	System.out.printf("%s located at (%.2f, %.2f)\n", toponym.getPhrase(), );
```
By default, the Context Hierarchy Fusion (CHF) method is employed to resolve toponyms (Refer to the paper for more details).


## Reference
If you found the code useful, please cite the following paper:

```
@inproceedings{Kamalloo2018,
 author = {Kamalloo, Ehsan and Rafiei, Davood},
 title = {A Coherent Unsupervised Model for Toponym Resolution},
 booktitle = {Proceedings of the 2018 World Wide Web Conference},
 series = {WWW '18},
 year = {2018},
 isbn = {978-1-4503-5639-8},
 location = {Lyon, France},
 pages = {1287--1296},
 numpages = {10},
 url = {https://doi.org/10.1145/3178876.3186027},
 doi = {10.1145/3178876.3186027},
 acmid = {3186027},
 publisher = {International World Wide Web Conferences Steering Committee},
 address = {Republic and Canton of Geneva, Switzerland},
 keywords = {context-bound hypotheses, geolocation extraction, spatial hierarchies, toponym resolution, unsupervised disambiguation},
}
```
