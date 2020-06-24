# EGA FUSE Client
This is a Java Native Runtime (JNR) based Filesystem in Userspace (FUSE) client to access the EGA Data REST API. This client will allow access 
to authorized EGA Archive files by presenting them in a vitual directory, where they can be used like regular files, 
without first having to download them.

## Prerequisite dependencies
1. Maven
2. Java

#### Linux

[`libfuse`](https://github.com/libfuse/libfuse) needs to be installed.

#### Ubuntu
```
sudo apt-get install libfuse-dev
``` 

#### MacOS

[`osxfuse`](https://osxfuse.github.io) needs to be installed.

```
brew cask install osxfuse
```


## Build the project

To build the project run the command below. It will produce the executable jar file in the /target directory.
```
mvn install
```

## Run the project

Use the command below to run the jar
```
java -jar target/ega-fuse-0.0.1-SNAPSHOT.jar --u=USERNAME --p=PASSWORD
```
or
```
java -jar target/ega-fuse-0.0.1-SNAPSHOT.jar --cf=CREDENTIAL_FILE_PATH
```
or
```
java -jar target/ega-fuse-0.0.1-SNAPSHOT.jar --u=USERNAME --p=PASSWORD --m=MOUNTPOINT_PATH --cache=CACHE_SIZE --c=CONNECTION
```
or

In Linux the fuse layer can also be started, stopped and restarted using shell script ./fuseclient.sh as:

```
./fuseclient.sh start "--u=USERNAME --p=PASSWORD"
```
 
```
./fuseclient.sh restart "--u=USERNAME --p=PASSWORD"
```

``` 
./fuseclient.sh stop
```

Optional arguments:
* u : username
* p : password
* m : mount point path, default value: /tmp/mnt `Note: Ensure that the mount point path exists`
* cache : the maximum size of the cache, default value: 100 `Means 100 * 10 MB = 1000 MB`
* c : connections, download using specified number of connections, default value: 4
* h : help, show this help message and exit
* cf : credential file below format
```
username:ega-test-data@ebi.ac.uk
password:egarocks
```

### Troubleshoot fuseclient.sh
Check the log file fuse-client-logs.log. If you see any error as `fuse: bad mount point /tmp/mnt: Transport endpoint is not connected.`, try running the command below

```
umount -l /tmp/mnt
```

## Supported platforms
* Linux                                                         
* MacOS (via [osxfuse](https://osxfuse.github.io/))