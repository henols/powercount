@title Sensor Publisher
@set LOCAL_CLASS_PATH=bin
@set LOCAL_CLASS_PATH=%LOCAL_CLASS_PATH%;HouseNews.jar
@set LOCAL_CLASS_PATH=%LOCAL_CLASS_PATH%;resources
@set LOCAL_CLASS_PATH=%LOCAL_CLASS_PATH%;lib/commons-cli.jar
@rem set LOCAL_CLASS_PATH=%LOCAL_CLASS_PATH%;lib/twitter4j-core-2.2.4.jar
@set LOCAL_CLASS_PATH=%LOCAL_CLASS_PATH%;lib/log4j-1.2.16.jar
@set LOCAL_CLASS_PATH=%LOCAL_CLASS_PATH%;lib/bluecove-2.1.1-SNAPSHOT.jar
@set LOCAL_CLASS_PATH=%LOCAL_CLASS_PATH%;lib/RXTXcomm.jar
@set LOCAL_CLASS_PATH=%LOCAL_CLASS_PATH%;lib/org.eclipse.paho.client.mqttv3.jar
@
java -classpath %LOCAL_CLASS_PATH% se.aceone.housenews.SensorPublisher -a 192.168.1.121 -c COM1
@pause
@exit

