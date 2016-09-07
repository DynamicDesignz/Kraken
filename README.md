<center><img src="https://github.com/arcaneiceman/Kraken/blob/master/misc/repoimages/kraken-logo.png" width="200" style="display: block;margin: 0 auto;"></center>

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

You can download Java 1.8 for your platform from the [Oracle Java Website]

#### ***Aircrack-ng 1.1 or above***  
###### Linux
On Ubuntu, you can use the following commands to download the following commands in terminal:
```sh
$ sudo apt-get update
$ sudo apt-get install aircrack-ng
```
###### Windows
Aircrack has limited portability on Windows.
To install aircrack 1.2 on Windows, use its precompiled binary. For this you should:
- Download the win32 zip
- Navigate to the /bin/32bit folder containing aircrack-ng-avx.exe.
- Extract aircrack-ng-avx.exe and all dll files in the directory and place them in the same directory as kraken.jar.
- Rename aircrack-ng-avx.exe to aircrack-ng.exe

*Note* : This binary was compiled on an AVX enabled windows CPU. We are working to include the non-AVX compiled binary with Kraken in the next release.

###### Mac
On Mac, you can install aircrack using the following commands in termnial:
```sh
sudo port update
sudo port install aircrack-ng
```

#### Testing
To test if Kraken will work properly, first test aircrack by entering the following command:
Next, test aircrack from the same folder as kraken.jar by typing
```sh
aircrack-ng
```
You should see the following on the first line.
```sh
Aircrack-ng 1.1 - (C) 2006, 2007, 2008, 2009 Thomas d'Otreppe
```
*Note* : The exact version may vary but as long as it is above version 1.1, you should be fine.

### Getting Started
*Step 1* : Download this repository's .zip file and extract it to a folder. 

*Step 2A* : To start kraken as a *Server and Worker*, rename 'server+worker.config.yml' to 'config.yml'.

*Step 2B* : To start kraken as only a *Server*, rename 'server.config.yml' to 'config.yml'.

*Step 2C* : To start kraken as only a *Worker*, rename 'worker.config.yml' to 'config.yml' and change the value of <your-server-ip-here> with your server ip. For example:
```yaml
#Gearman Server-to-connect-to IP
JobServerIP : 198.162.52.98
```

At the end of Step 2, ensure that only one file the folder is named 'config.yml'.

*Step 3* : Navigate to folder or terminal (or cmd prompt) and type :
```sh
java -jar kraken.jar
```

##### PasswordLists
In Kraken, only a single default wpa password list is provided. You can add more lists by downloading them of the internet and adding them through the web ui. We are working to make WPA password lists available. Stay tuned!

### Screenshots
![screenshot](https://github.com/arcaneiceman/Kraken/blob/master/misc/repoimages/kraken-server-screenshot.png)

![screenshot](https://github.com/arcaneiceman/Kraken/blob/master/misc/repoimages/kraken-worker-screenshot.png)

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
