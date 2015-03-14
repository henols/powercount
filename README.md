Project summary
This is a power and temperature monitoring system.

It is using an Arduino to count pules from my power meter and read temperatures over 1wire from sensors.

It is using an Mqtt server to publish and subscribe to the data (I'm using Mosquitto).

Arduino
Checkout PowerMeter and open in Arduino IDE The PowerMeter.ino uses 2 liberays <OneWire.h> and <DallasTemperature.h>, make sure that they is installed in the IDE.

Pin 2 and 3 are connected to the power meter(s), either with a IR sensor or to a switch on the meter.

Pin 13, 9 is connected to LEDs and is used to indicate pulses on pin 2 and 3 (switches level on every pulse). Pin 10 is used as 1wire bus and there can you connect 1-n number of temperature sensors (DS18B20).



Java
Checkout and compile the java code found in java directory.

Launch se.aceone.housenews.SensorPublisher with arguments -a MQTT_SERVER_ADDRESS -c ARDUINO_COMPORT it will start to post temeratures and pulses to the Mqtt server. You must download and install rxtxSerial to make the serial connection to the Arduino to work see http://rxtx.qbang.org/wiki/index.php/Main_Page.

se.aceone.housenews.EmonPoster posts to your EmonCMS.
se.aceone.housenews.TemperatureTweet tweets temperatures 4 times a day and a summay 8 o'clock in the morning.
se.aceone.housenews.DailyConsumtionTweet tweets the daily power consumption at midnight.
There are some windows cmd scripts to launch it all to.

I have probably forgotten a bunch of info ether just dig in to it or ask me a question.

Install on Raspberry PI
sudo apt-get update

Install Pi4J
(http://pi4j.com/install.html)

curl -s get.pi4j.com | sudo bash

Disable serial console
(https://github.com/lurch/rpi-serial-console)

sudo wget https://raw.github.com/lurch/rpi-serial-console/master/rpi-serial-console -O /usr/bin/rpi-serial-console
sudo chmod +x /usr/bin/rpi-serial-console

sudo rpi-serial-console disable

Install Sensor publisher
Copy housenews to home dir
wget https://powercount.googlecode.com/svn/trunk/java/install/housenews.zip

unzip housenews.zip

cd housenews/
chmod +x ./SensorPublisher
sudo mv sensor_publisher /etc/init.d
sudo chmod +x /etc/init.d/sensor_publisher
sudo update-rc.d sensor_publisher defaults
Modify arguments in SensorPublisher?

nano SensorPublisher

Install Mosquitto
(http://mosquitto.org/2013/01/mosquitto-debian-repository/) sudo apt-get install mosquitto

Install Node-RED
http://learn.adafruit.com/raspberry-pi-hosting-node-red/what-is-node-red

Install Emoncms node see https://github.com/node-red/node-red-nodes

https://github.com/node-red/node-red-nodes/tree/master/io/emoncms

Node-RED flow publish to Emoncms and Twitter https://gist.github.com/henols/9267730

Install Emoncms
http://emoncms.org/site/docs/installlinux
