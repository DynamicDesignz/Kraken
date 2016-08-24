![logo](https://github.com/arcaneiceman/Kraken/misc/repo-images/kraken-logo.png) 

# Kraken

Kraken is a cross-platform distributed brute-force WPA/WPA2 WiFi password cracking tool. It allows you to parallelize password-list based cracking across multiple machines to create a cluster of crackers, and can run on Unix, Mac, Windows or any operating system that supports Java. Kraken nearly eliminates lengthy setup time by being easy to install, is fault tolerant and can support dynamic resizing of the network. 

Compared to the usual method of cracking WiFi Passwords on a single machine, Kraken has the following advantages.
    
- **Parallel** - The more machines you add to your cracking cluster, the faster you can crack larger password lists.
- **Flexible** - Allows you to add or remove machines to cracking cluster on the go.
- **Fault-Tolerant** - If a machine in your cracking cluster goes down, no worries! Kraken can recover. 
- **Easy to Setup** - Requires a sum total of 2 dependencies. All configurations are in a single YAML file!
- **Cross Platform** - Can run on Windows, Unix and Mac.

# How does Kraken Work?
Kraken is built on top of the [Gearman] library, a generic distributed job management framework that allows the distribution of jobs across multiple machines. To crack passwords, it uses the [Aircrack-ng] suite for WiFi security which allows Kraken to take advantage of the speed and multi CPU core support that aircrack-ng provides, while allowing its own core to be packaged in a neat, clean single jar file. 

Kraken has a web interface where you can submit a packet capture (.cap) file and specify the SSID of the network you want to crack. You also have to specify the various password lists that you want to run against the packet capture file. Kraken takes care of the rest, automatically breaking up large files into smaller chunks and distributing jobs across multiple machines. You can monitor progress using the interface which by default runs on port 8080.

Kraken is organised in the client server architecture where the server acts as the main hub connecting other worker clients which do the actual cracking. You can have both a client and server on a single machine as well. The server must stay online during the cracking process but worker clients may be added or removed at any time. The system is fault tolerant so that if a worker crashes, the lost job is recovered and re-sent to a different worker

# Installation
### Dependencies

To run Kraken you need two dependecies:
- Java 1.8
- Aircrack-ng 1.1 or above
##### ***Java 1.8*** 
You can download Java 1.8 for your platform from the [Oracle Java Website]. Otherwise:
###### Linux
On Ubuntu, you can use the following commands to download the following commands
```sh
$ sudo add-apt-repository ppa:webupd8team/java
$ sudo apt-get update
$ sudo apt-get install oracle-java8-installer
```
###### Windows
On Windows you need to download and install the Java 1.8 exe file from the link above.

###### Mac
On Mac, you need to download and install the Java 1.8 dmg file from the link above.

#### ***Aircrack-ng 1.1 or above***  
You can download Aircrack-ng from its [download] site.
###### Linux
On Ubuntu, you can use the following commands to download the following commands:
```sh
$ sudo apt-get install aircrack-ng
```
You may also choose to compile aircrack from source. For this, you should follow the instructions given on Aircrack's installation page.
###### Windows
To install aircrack 1.2 on Windows, use its precompiled binary. For this you should:
- Download the win32 zip
- Navigate to the /bin/32bit folder containing aircrack-ng-avx.exe.
- Extract aircrack-ng-avx.exe and all dll files in the directory and place them in the same directory as kraken.jar.
- Rename aircrack-ng-avx.exe to aircrack-ng.exe

###### Mac
On Mac, you can install aircrack using the following commands:
```sh
sudo port install aircrack-ng
```

#### ***Gearman Native Server (Optional, only for Linux)***
-Instructions coming soon -

#### Testing
To test if Kraken will work properly, first run:
```sh
java -version
```
Your result should include
```sh
Java(TM) SE Runtime Environment (build 1.8.0_91-b14)
```
Next, test aircrack from the same folder as kraken.jar by typing
```sh
aircrack-ng
```
You should see the following on the first line.
```sh
Aircrack-ng 1.1 - (C) 2006, 2007, 2008, 2009 Thomas d'Otreppe
```

### Getting Started
1. Download this repository's  .zip file and navigate to the Kraken/jar folder.
2. Run java -jar kraken.jar
3. This will start an instance of a server and worker client (on the same local machine). A gearman server will be created at port 4730 and a Web UI will be instantiated on port 8080.

If you want to use more advanced features, see the configuration section below.

### Screenshots
![screenshot](https://github.com/arcaneiceman/Kraken/misc/repo-images/kraken-screenshot.png)

# Configuration

Configuration of Kraken is peformed in the config.yml file. This section explains simple configuration setup to create a cracking cluster.

##### Startup Mode
Kraken has three running modes: 
- **Server** : Runs the Kraken Server which all other worker clients connect to. It also creates a web ui running on port 8080. To start kraken in Server Mode ensure that:
```sh
StartupMode = Server
```
in the config.yml.
- **Worker** : Runs a Kraken Worker Client which receives jobs and cracks passwords. To run kraken in Worker Mode ensure that:
```sh
StartupMode = Worker
```
and specify the IP or hostname of the Kraken Server this Worker is connecting to
```sh
GearmanServer = <Your IP/Hostname here>
```
- Server+Worker : Runs an instance of both Server and Worker on the local machine.
Ensure that:
```sh
StartupMode = Server+Worker
```

##### PasswordLists
In Kraken, you can add password lists individually or by folder. Look at config.yml for more details.

# Version
0.1.1

# Licence
GPL 3.0

# Development
Want to contribute? Great! Please open an issue on Github or contact me :)

[//]: # (These are reference links used in the body of this note and get stripped out when the markdown processor does its job. There is no need to format nicely because it shouldn't be seen. Thanks SO - http://stackoverflow.com/questions/4823468/store-comments-in-markdown-syntax)


[Aircrack-ng]: <http://aircrack-ng.org>
[Gearman]: <http://gearman.org>
[Oracle Java Website]: <http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html>
[download]: <http://aircrack-ng.org>
