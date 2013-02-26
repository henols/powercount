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
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

public class EmonPoster {

	final static String POWER_TOPIC = "mulbetet49/powermeter/power";
	final static String KWH_TOPIC = "mulbetet49/powermeter/kwh";
	final static String TEMPERATURE_TOPIC = "mulbetet49/temperature/";

	final static String[] TOPICS = { POWER_TOPIC, KWH_TOPIC, TEMPERATURE_TOPIC+"#"};

	private static final String URI = "uri";
	private static final String API_KEY = "apikey";
	private static final String PORT = "port";
	private static final String ADDRESS = "address";
	
	private String address;
	private String port = "1883";

	private static Logger logger = Logger.getLogger(EmonPoster.class);

	public EmonPoster(CommandLine cmd) throws MqttException {
		address = cmd.getOptionValue(ADDRESS);
		String uri = cmd.getOptionValue(URI);
		String apikey = cmd.getOptionValue(API_KEY);
		if (cmd.hasOption(PORT)) {
			port = cmd.getOptionValue(PORT);
		}
		String serverURI = "tcp://" + address + ":"+port;
		MqttClient client = new MqttClient(serverURI, "EmonPoster");
		client.connect();
		client.subscribe(TOPICS);
		client.setCallback(new Callback());

		logger.info("MQTT Client ID: "+client.getClientId());
		logger.info("MQTT Server URI: "+client.getServerURI());
		logger.info("MQTT Is connected: " + client.isConnected());
		logger.info("EmonCMS uri " + uri);
		
		Util.setEmonUri(uri);
		Util.setEmonApiKey(apikey);
	}

	class Callback implements MqttCallback {

		@Override
		public void messageArrived(MqttTopic topic, MqttMessage message) throws Exception {
			String topicName = topic.getName();
			logger.info("Got topic: " + topicName);
			String result = null;
			String payLoad = new String(message.getPayload());
			if(topicName.startsWith(TEMPERATURE_TOPIC)){
				String sensorName = topicName.substring(TEMPERATURE_TOPIC.length());
				result = sensorName+":"+payLoad;
				logger.info(sensorName+" "+payLoad);
			}else if (topicName.equals(POWER_TOPIC)) {
				String sensorName = "power";
				result = sensorName+":"+payLoad;
				logger.info(sensorName+" "+payLoad);
			}else if (topicName.equals(KWH_TOPIC)) {
				String sensorName = "kwh";
				result = sensorName+":"+payLoad;
				logger.info(sensorName+" "+payLoad);
				
			}
			if(result != null){
//				Util.post2Emon(result);
			}
		}

		@Override
		public void deliveryComplete(MqttDeliveryToken token) {

		}

		@Override
		public void connectionLost(Throwable cause) {
			logger.error("Connection lost");
			System.exit(0);
		}
	}

	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {
		Options options = new Options();

		options.addOption("p", PORT, true, "Mqtt server port, default 1883");
		Option address = OptionBuilder.withLongOpt(ADDRESS).hasArg(true).withDescription("Mqtt server address")
				.isRequired().create('a');
		options.addOption(address);

		Option uri = OptionBuilder.withLongOpt(URI).hasArg(true).withDescription("EmonCMS server URI")
				.isRequired().create('u');
		options.addOption(uri);

		Option apiKey = OptionBuilder.withLongOpt(API_KEY).hasArg(true).withDescription("EmonCMS api key")
				.isRequired().create('k');
		options.addOption(apiKey);



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

		EmonPoster powerMeter = new EmonPoster(cmd);
	}

	private static void printUsage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("EmonPoster", options,true);
		System.exit(1);
	}

}
