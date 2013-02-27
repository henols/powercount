package se.aceone.housenews;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttDefaultFilePersistence;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import twitter4j.TwitterException;

public class TemperatureTweet {

	private static final boolean DAYS = true;

	final static String TEMPERATURE_TOPIC = "mulbetet49/temperature/";

	private static final String PORT = "port";
	private static final String ADDRESS = "address";
	private static final String SENSOR = "sensor";

	private static final SimpleDateFormat sdf = new SimpleDateFormat();
	private static final SimpleDateFormat hhMM = new SimpleDateFormat("HH:mm");

	private static Logger logger = Logger.getLogger(TemperatureTweet.class);

	private static TemperatureTweet temperatureTwitter;

	private double max = -10000;
	private long maxStamp;
	private double min = 10000;
	private long minStamp;
	private List<Double> values = new ArrayList<Double>();

	private Calendar nextTemperatureTweet;

	private Calendar tempAt_00_00_c;
	private Calendar tempAt_06_00_c;
	private Calendar tempAt_12_00_c;
	private Calendar tempAt_18_00_c;

	private MqttClient client;

	public TemperatureTweet(CommandLine cmd) throws MqttException, TwitterException {
		String address = cmd.getOptionValue(ADDRESS);
		String port = "1883";
		if (cmd.hasOption(PORT)) {
			port = cmd.getOptionValue(PORT);
		}
		String serverURI = "tcp://" + address + ":" + port;

		String tmpDir = System.getProperty("java.io.tmpdir");
		MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir + "/mqtt");

		client = new MqttClient(serverURI, "TemperatureTweet", dataStore);
		client.setCallback(new Callback());
		client.connect();

		String sensor = cmd.getOptionValue(SENSOR);

		client.subscribe(TEMPERATURE_TOPIC + sensor);

		logger.info("MQTT Client ID: " + client.getClientId());
		logger.info("MQTT Server URI: " + client.getServerURI());
		logger.info("MQTT Is connected: " + client.isConnected());
		logger.info("Sensor name: " + sensor);

		Util.initTwitter();

		nextTemperatureTweet = getNextTemperatureTweetTime();
		tempAt_00_00_c = getNextTime(0);
		tempAt_06_00_c = getNextTime(6);
		tempAt_12_00_c = getNextTime(12);
		tempAt_18_00_c = getNextTime(18);

	}

	class Callback implements MqttCallback {

		@Override
		public void messageArrived(MqttTopic topic, MqttMessage message) throws Exception {
			String topicName = topic.getName();
			logger.debug("Got topic: " + topicName);
			String payLoad = new String(message.getPayload());
			processTemperature(Double.parseDouble(payLoad));
		}

		@Override
		public void deliveryComplete(MqttDeliveryToken token) {

		}

		@Override
		public void connectionLost(Throwable cause) {
			logger.error("Connection lost", cause);
			reconnectMqtt();
		}
	}

	private void processTemperature(double teperature) {

		if (teperature > max) {
			max = teperature;
			maxStamp = System.currentTimeMillis();
		}
		if (teperature < min) {
			min = teperature;
			minStamp = System.currentTimeMillis();
		}

		values.add(teperature);

		double average = getAverage();
		logger.info("average:" + average + "°C max:" + max + "°C at " + hhMM.format(new Date(maxStamp)) + " min:" + min
				+ "°C at " + hhMM.format(new Date(minStamp)) + " now:" + teperature + "°C");

		Calendar now = Calendar.getInstance();
		if (now.after(tempAt_00_00_c)) {
			tweetTemperatureNow(teperature, tempAt_00_00_c);
			tempAt_00_00_c = getNextTime(0);
		}
		if (now.after(tempAt_06_00_c)) {
			tweetTemperatureNow(teperature, tempAt_06_00_c);
			tempAt_06_00_c = getNextTime(6);
		}
		if (now.after(tempAt_12_00_c)) {
			tweetTemperatureNow(teperature, tempAt_12_00_c);
			tempAt_12_00_c = getNextTime(12);
		}
		if (now.after(tempAt_18_00_c)) {
			tweetTemperatureNow(teperature, tempAt_18_00_c);
			tempAt_18_00_c = getNextTime(18);
		}
		if (now.after(nextTemperatureTweet)) {
			String status = "Last day's temperatures: average:" + average + "°C, max:" + max + "°C at "
					+ hhMM.format(new Date(maxStamp)) + ", min:" + min + "°C at " + hhMM.format(new Date(minStamp))
					+ ". #temperature";
			logger.info(status + " l:" + status.length());
			try {
				Util.post2Twitter(status);
			} catch (TwitterException e) {
				logger.error("Failed to post Twitter message.", e);
			}
			max = -10000;
			min = 10000;
			values.clear();

			nextTemperatureTweet = getNextTemperatureTweetTime();
		}
	}

	private void tweetTemperatureNow(double temp, Calendar now) {
		String status = "The temperature are " + temp + "°C at " + hhMM.format(now.getTime()) + ". #temperature";
		logger.info(status + " l:" + status.length());
		try {
			Util.post2Twitter(status);
		} catch (TwitterException e) {
			logger.error("Failed to post Twitter message.", e);
		}
	}

	private double getAverage() {
		double total = 0;
		for (double value : values) {
			total += value;
		}
		double average = total / values.size();
		int ix = (int) (average * 100.0); // scale it
		return ((double) ix) / 100.0;
	}

	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {
		Options options = new Options();

		Option address = OptionBuilder.withLongOpt(ADDRESS).hasArg().withDescription("Mqtt server address")
				.isRequired().create('a');
		options.addOption(address);
		options.addOption("p", PORT, true, "Mqtt server port, default 1883");

		Option sensor = OptionBuilder.withLongOpt(SENSOR).hasArg().withDescription("Temperature sensor name")
				.isRequired().create('s');
		options.addOption(sensor);

		if (args.length == 0) {
			printUsage(options);
		}

		CommandLineParser parser = new PosixParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			logger.error(e.getMessage());
			printUsage(options);
		}

		new TemperatureTweet(cmd);
	}

	private static void printUsage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("TemperatureTweet", options, true);
		System.exit(1);
	}

	private Calendar getNextTemperatureTweetTime() {
		Calendar nextTime = getNextTime(8);
		return nextTime;
	}

	private Calendar getNextTime(int houres) {
		Calendar nextTime = Calendar.getInstance();

		// Zero out the hour, minute, second, and millisecond
		if (DAYS) {
			nextTime.set(Calendar.HOUR_OF_DAY, houres);
			nextTime.set(Calendar.MINUTE, 0);
		}
		nextTime.set(Calendar.SECOND, 0);
		nextTime.set(Calendar.MILLISECOND, 0);

		if (DAYS) {
			if (Calendar.getInstance().after(nextTime)) {
				nextTime.add(Calendar.DAY_OF_YEAR, 1);
			}
		} else {
			nextTime.add(Calendar.MINUTE, 1);
		}
		logger.debug("Creating new next temperature tweet time: " + sdf.format(nextTime.getTime()));
		return nextTime;
	}

	private void reconnectMqtt() {
		while (!client.isConnected()) {
			logger.info("Trying to reconnect to MQTT server");
			try {
				client.connect();
			} catch (MqttException e) {
			}
			if (client.isConnected()) {
				break;
			}
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
			}
		}
		logger.info("Reconnected to MQTT server");
	}

}
