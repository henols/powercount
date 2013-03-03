package se.aceone.housenews;

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

public class DailyConsumtionTweet {

	final static String POWER_TOPIC = "mulbetet49/powermeter/power";
	final static String DAILY_CONSUMPTION_TOPIC = "mulbetet49/powermeter/kwh/dailyconsumption";

	final static String[] TOPICS = { POWER_TOPIC, DAILY_CONSUMPTION_TOPIC, };

	private static final String PORT = "port";
	private static final String ADDRESS = "address";

	private static Logger logger = Logger.getLogger(DailyConsumtionTweet.class);
	private MqttClient client;

	public DailyConsumtionTweet(CommandLine cmd) throws MqttException, TwitterException {
		String address = cmd.getOptionValue(ADDRESS);
		String port = "1883";
		if (cmd.hasOption(PORT)) {
			port = cmd.getOptionValue(PORT);
		}
		String serverURI = "tcp://" + address + ":" + port;

		String tmpDir = System.getProperty("java.io.tmpdir");
		MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir + "/mqtt");

		client = new MqttClient(serverURI, "DailyConsumtionTweet", dataStore);
		client.setCallback(new Callback());
		
		connectMqtt();

		logger.info("MQTT Client ID: " + client.getClientId());
		logger.info("MQTT Server URI: " + client.getServerURI());
		logger.info("MQTT Is connected: " + client.isConnected());

		Util.initTwitter();

	}

	class Callback implements MqttCallback {

		private String power;

		@Override
		public void messageArrived(MqttTopic topic, MqttMessage message) throws Exception {
			String topicName = topic.getName();
			logger.debug("Got topic: " + topicName);
			String payLoad = new String(message.getPayload());
			if (topicName.equals(POWER_TOPIC)) {
				String sensorName = "power";
				power = payLoad;
				logger.info(sensorName + " " + payLoad);
			} else if (topicName.equals(DAILY_CONSUMPTION_TOPIC)) {
				String sensorName = "kwh";
				logger.info(sensorName + " " + payLoad);
				String status = "Last day's power consumption for the house were " + payLoad + "kWh. Using " + power
						+ "w right now. #tweetawatt";
				logger.debug(status);

				try {
					Util.post2Twitter(status);
				} catch (TwitterException e) {
					logger.error("Failed to post Tweet maessage.", e);
				}
			}
		}

		@Override
		public void deliveryComplete(MqttDeliveryToken token) {

		}

		@Override
		public void connectionLost(Throwable cause) {
			logger.error("Connection lost", cause);
			connectMqtt();
		}
	}

	private void connectMqtt() {
		while (!client.isConnected()) {
			logger.info("Trying to reconnect to MQTT server");
			try {
				client.connect();
				client.subscribe(TOPICS);
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

	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {
		Options options = new Options();

		Option address = OptionBuilder.withLongOpt(ADDRESS).hasArg().withDescription("Mqtt server address")
				.isRequired().create('a');
		options.addOption(address);
		options.addOption("p", PORT, true, "Mqtt server port, default 1883");

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

		new DailyConsumtionTweet(cmd);
	}

	private static void printUsage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("TemperatureTweet", options, true);
		System.exit(1);
	}

}
