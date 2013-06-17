@title Emon Poster
@set LOCAL_CLASS_PATH=bin
@set LOCAL_CLASS_PATH=%LOCAL_CLASS_PATH%;HouseNews.jar
@set LOCAL_CLASS_PATH=%LOCAL_CLASS_PATH%;resources
@set LOCAL_CLASS_PATH=%LOCAL_CLASS_PATH%;lib/commons-cli.jar
@set LOCAL_CLASS_PATH=%LOCAL_CLASS_PATH%;lib/twitter4j-core-3.0.3.jar
@set LOCAL_CLASS_PATH=%LOCAL_CLASS_PATH%;lib/log4j-1.2.16.jar
@set LOCAL_CLASS_PATH=%LOCAL_CLASS_PATH%;lib/org.eclipse.paho.client.mqttv3.jar
@
java -classpath %LOCAL_CLASS_PATH% se.aceone.housenews.EmonPoster -u http://192.168.1.223/emon -k afdd5eecb848d9bb758bd3f6dc91a1a9 -a 192.168.1.121
@pause
@exit

