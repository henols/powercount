## Project summary ##
This is a power and temperature monitoring system.

It is using an Arduino to count pules from my power meter and read temperatures over 1wire from sensors.

It is using an Mqtt server to publish and subscribe to the data (I'm using Mosquitto).


## Arduino ##
Checkout `PowerMeter` and open in Arduino IDE
The `PowerMeter.ino` uses 2 liberays `<OneWire.h>` and `<DallasTemperature.h>`, make sure that they is installed in the IDE.

Pin 2 and 3 are connected to the power meter(s), either with a IR sensor or to a switch on the meter.

Pin 13, 9 is connected to LEDs and is used to indicate pulses on pin 2 and 3 (switches level on every pulse).
Pin 10 is used as 1wire bus and there can you connect 1-n number of temperature sensors (DS18B20).

![http://powercount.googlecode.com/files/PulseCounter_schem.png](http://powercount.googlecode.com/files/PulseCounter_schem.png)

## Java ##
Checkout and compile the java code found in `java` directory.

Launch `se.aceone.housenews.SensorPublisher` with arguments -a MQTT\_SERVER\_ADDRESS -c ARDUINO\_COMPORT it will start to post temeratures and pulses to the Mqtt server.
You must download and install rxtxSerial to make the serial connection to the Arduino to work see http://rxtx.qbang.org/wiki/index.php/Main_Page.


`se.aceone.housenews.EmonPoster` posts to your EmonCMS.<br>
<code>se.aceone.housenews.TemperatureTweet</code> tweets temperatures 4 times a day and a summay 8 o'clock in the morning.<br>
<code>se.aceone.housenews.DailyConsumtionTweet</code> tweets the daily power consumption at midnight.<br>

There are some windows cmd scripts to launch it all to.<br>
<br>
I have probably forgotten a bunch of info ether just dig in to it or ask me a question.<br>
<br>
<h1>Install on Raspberry PI</h1>
<code>sudo apt-get update</code>

<h3>Install Pi4J</h3>
(<a href='http://pi4j.com/install.html'>http://pi4j.com/install.html</a>)<br>
<br>
<code>curl -s get.pi4j.com | sudo bash</code>



<h3>Disable serial console</h3>
(<a href='https://github.com/lurch/rpi-serial-console'>https://github.com/lurch/rpi-serial-console</a>)<br>
<br>
<code>sudo wget https://raw.github.com/lurch/rpi-serial-console/master/rpi-serial-console -O /usr/bin/rpi-serial-console</code><br>
<code>sudo chmod +x /usr/bin/rpi-serial-console</code>

<code>sudo rpi-serial-console disable</code>

<h3>Install Sensor publisher</h3>

Copy housenews to home dir<br>
<code>wget https://powercount.googlecode.com/svn/trunk/java/install/housenews.zip</code>

<code>unzip housenews.zip</code>

<code>cd housenews/</code><br>
<code>chmod +x ./SensorPublisher</code><br>
<code>sudo mv sensor_publisher /etc/init.d</code><br>
<code>sudo chmod +x /etc/init.d/sensor_publisher</code><br>
<code>sudo update-rc.d sensor_publisher defaults</code><br>

Modify arguments in SensorPublisher<br>
<br>
<code>nano SensorPublisher</code>

<h3>Install Mosquitto</h3>
(<a href='http://mosquitto.org/2013/01/mosquitto-debian-repository/'>http://mosquitto.org/2013/01/mosquitto-debian-repository/</a>)<br>
<code>sudo apt-get install mosquitto</code>

<h3>Install Node-RED</h3>
<a href='http://learn.adafruit.com/raspberry-pi-hosting-node-red/what-is-node-red'>http://learn.adafruit.com/raspberry-pi-hosting-node-red/what-is-node-red</a>

Install Emoncms node see <a href='https://github.com/node-red/node-red-nodes'>https://github.com/node-red/node-red-nodes</a>

<a href='https://github.com/node-red/node-red-nodes/tree/master/io/emoncms'>https://github.com/node-red/node-red-nodes/tree/master/io/emoncms</a>


Node-RED flow publish to Emoncms and Twitter<br>
<a href='https://gist.github.com/henols/9267730'>https://gist.github.com/henols/9267730</a>

<h3>Install Emoncms</h3>
<a href='http://emoncms.org/site/docs/installlinux'>http://emoncms.org/site/docs/installlinux</a>