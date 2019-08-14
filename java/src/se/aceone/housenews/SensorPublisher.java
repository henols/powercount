package se.aceone.housenews;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.net.SocketException;
import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

public class SensorPublisher {

	private static final String BLUETOOTH = "bluetooth";
	private static final String COMPORT = "comport";
	private static final String PORT = "port";
	private static final String ADDRESS = "address";
	private final static String LOCATION = "location";

	private static final byte METER_1 = 0;
	private static final byte METER_2 = 1;

	private static final byte[] READ_METER_1 = { '4', '0' };
	private static final byte[] READ_METER_2 = { '4', '1' };
	private static final byte[][] READ_METER = { READ_METER_1, READ_METER_2 };

	private static final byte[] CONFIRM_METER_1 = { 'c', '0' };
	private static final byte[] CONFIRM_METER_2 = { 'c', '1' };
	private static final byte[][] CONFIRM_METER = { CONFIRM_METER_1, CONFIRM_METER_2 };

	private static final byte[] TEMPERATURE = { 't', 't' };

	private static final boolean DAYS = true;
	private static final boolean CLEAR_COUNT = true;

	private static final long POWER_PING_TIME = 10;
	private static final long TEMPERATURE_PING_TIME = 60;

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	final String POWER_TOPIC;
	final String KWH_TOPIC;
	final String TEMPERATURE_TOPIC;

	private static Logger logger = Logger.getLogger(SensorPublisher.class);

	private double oldKWh[] = { Double.MIN_VALUE, Double.MIN_VALUE };

	private Calendar nextDailyConsumtion;
	private MqttClient client;
	private Connection connection;
	private String address;
	private String port = "1883";
	private String location;

	private Object lock = new Object();

	public SensorPublisher(CommandLine cmd) throws Exception {
		address = cmd.getOptionValue(ADDRESS);
		logger.info("Using address: " + address);
		location = cmd.getOptionValue(LOCATION);
		logger.info("Using location name: " + location);

		String connectionClass = null;
		String connectionName = null;
		if (cmd.hasOption(BLUETOOTH)) {
			connectionName = cmd.getOptionValue(BLUETOOTH);
			logger.info("Using Bluetooth connection: " + connectionName);
			connectionClass = "se.aceone.housenews.BlueToothConnection";
		} else if (cmd.hasOption(COMPORT)) {
			connectionName = cmd.getOptionValue(COMPORT);
			logger.info("Using Serial connection: " + connectionName);
			if (connectionName.startsWith("/")) {
				connectionClass = "se.aceone.housenews.Pi4JSerialConnection";
			} else {
				connectionClass = "se.aceone.housenews.RXTXSerialConnection";
			}
		}

		logger.info("Using connection type: " + connectionClass);

		Class<?> clazz = getClass().getClassLoader().loadClass(connectionClass);
		connection = (Connection) clazz.newInstance();

		connection.init(connectionName);
		connection.open();

		if (cmd.hasOption(PORT)) {
			port = cmd.getOptionValue(PORT);
		}
		POWER_TOPIC = location + "/powermeter/power";
		KWH_TOPIC = location + "/powermeter/kwh";
		TEMPERATURE_TOPIC = location + "/temperature/";
	}

	public void init() throws MqttException {
		nextDailyConsumtion = getDailyConsumtionTime();
		String serverURI = "tcp://" + address + ":" + port;
		logger.info("Connecting to MQTT server : " + serverURI);

		String tmpDir = System.getProperty("java.io.tmpdir");
		MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir + "/mqtt");

		client = new MqttClient(serverURI, "SensPub" + location, dataStore);
		client.setCallback(new Callback());
		client.connect();
		logger.info("Connected to MQTT server : " + serverURI);
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

	private void readTemperature() {
		synchronized (lock) {
			if (!publishTemperature()) {
				connection.close();
				try {
					connection.open();
				} catch (Exception e) {
				}
			}
		}
	}

	private void readPowerMeter() {
		synchronized (lock) {
			if (!publishPower()) {
				connection.close();
				try {
					connection.open();
				} catch (Exception e) {
				}
			}
			Calendar now = Calendar.getInstance();
			if (now.after(nextDailyConsumtion)) {
				if (publishDailyConsumtion()) {
					nextDailyConsumtion = getDailyConsumtionTime();
				} else {
					connection.close();
					try {
						connection.open();
					} catch (Exception e) {
					}
				}
			}
		}
	}

	private boolean publishTemperature() {
		String result;
		try {
			result = readProtocol(TEMPERATURE);
			if (result == null) {
				return false;
			}
		} catch (SocketException e) {
			logger.error("SocketException", e);
			reconnect();
			return false;
		} catch (IOException e) {
			logger.error("IOException", e);
			reconnect();
			return false;
		}

		// Split result
		// power:22.44,temperature:15.50

		MqttMessage message = new MqttMessage();
		message.setQos(1);
		message.setRetained(true);

		long timestamp = System.currentTimeMillis();

		String[] strings = result.split(",");
		for (String string : strings) {
			int indexOf = string.indexOf(':');
			if (indexOf < 0) {
				continue;
			}
			MqttTopic topic = client.getTopic(TEMPERATURE_TOPIC + string.substring(0, indexOf));
			message.setPayload(buildJson(string.substring(indexOf + 1), timestamp, string.substring(0, indexOf),  null).getBytes());
			try {
				logger.debug("Publishing to broker: " + topic + " : " + message);
				topic.publish(message);
			} catch (MqttPersistenceException e) {
				logger.error("Failed to persist: " + message, e);
			} catch (MqttException e) {
				logger.error("Failed to publish: " + message, e);
			}
		}
		return true;
	}

	private boolean publishPower() {
		long timestamp = System.currentTimeMillis();
		return publishPower(METER_1, timestamp) && publishPower(METER_2, timestamp);
	}

	private boolean publishPower(byte meter, long timestamp) {
		String result;
		try {
			result = readProtocol(READ_METER[meter]);
			if (result == null) {
				return false;
			}
		} catch (SocketException e) {
			logger.error("SocketException", e);
			reconnect();
			return false;
		} catch (IOException e) {
			logger.error("IOException", e);
			reconnect();
			return false;
		}

		String[] r;
		try {
			r = splitPowerResult(result);
		} catch (NoSuchElementException e) {
			logger.error("Got some crapp from reader: " + result, e);
			return false;
		}
		@SuppressWarnings("unused")
		String counter = r[0];
		String pulses = r[1];
		String power = r[2];
		// logger.debug("pulses:"+pulses+" power:"+power)
		double kWh;
		try {
			if (Double.parseDouble(pulses) < 0 || Double.parseDouble(power) < 0) {
				logger.error("We seem to have a negative value: pulses:" + pulses + " power:" + power);
				return false;
			}
			kWh = Double.parseDouble(pulses);
		} catch (NumberFormatException e) {
			logger.error("We seem to have a negative value: pulses:" + pulses + " power:" + power, e);
			return false;
		}

		if (Double.MIN_VALUE != oldKWh[meter]) {
			double nKWh = kWh - oldKWh[meter];

			MqttMessage message = new MqttMessage();
			message.setQos(1);

			MqttTopic topic = client.getTopic(KWH_TOPIC + meter);
			message.setPayload(buildJson(nKWh, timestamp, "kwh" + meter, meter == 0 ? "Main" : "Heantpump").getBytes());
			try {
				logger.debug("Publishing to broker: " + topic + " : " + message);
				topic.publish(message);
			} catch (MqttPersistenceException e) {
				logger.error("Failed to persist: " + message, e);
			} catch (MqttException e) {
				logger.error("Failed to publish: " + message, e);
			}
			message.setPayload(buildJson(power, timestamp, "power" + meter,  meter == 0 ? "Main" : "Heantpump").getBytes());
			topic = client.getTopic(POWER_TOPIC + meter);
			try {
				logger.debug("Publishing to broker: " + topic + " : " + message);
				topic.publish(message);
			} catch (MqttPersistenceException e) {
				logger.error("Failed to persist: " + message, e);
			} catch (MqttException e) {
				logger.error("Failed to publish: " + message, e);
			}
		}
		oldKWh[meter] = kWh;
		// logger.debug("ping : " + sb.toString().trim());
		return true;
	}

	private String buildJson(double value, long timestamp, String name, String alias) {
		return buildJson(String.valueOf(value), timestamp, name, alias);
	}

	private String buildJson(String value, long timestamp, String name, String alias) {
		StringBuffer sb = new StringBuffer();
		sb.append("{\"value\":");
		sb.append(value);
		sb.append(",\"timestamp\":");
		sb.append(timestamp);
		if (name != null) {
			sb.append(",\"name\":\"");
			sb.append(name);
			sb.append("\"");
		}
		if (alias != null) {
			sb.append(",\"alias\":\"");
			sb.append(alias);
			sb.append("\"");
		}
		sb.append("}");
		return sb.toString();
	}

	private boolean publishDailyConsumtion() {
		return publishDailyConsumtion(METER_1) && publishDailyConsumtion(METER_2);
	}

	private boolean publishDailyConsumtion(byte meter) {
		logger.debug("Read power counter.");
		try {
			String result = readProtocol(READ_METER[meter]);
			if (result == null) {
				return false;
			}
			String[] r = splitPowerResult(result.toString());
			@SuppressWarnings("unused")
			String counter = r[0];
			String pulses = r[1];
			@SuppressWarnings("unused")
			String power = r[2];
			double kWh = toKWh(pulses);
			// oldWh = 0;
			oldKWh[meter] = 0;
			MqttMessage message = new MqttMessage();
			message.setQos(1);

			MqttTopic topic = client.getTopic(KWH_TOPIC + "/dailyconsumption" + meter);
			message.setPayload(String.valueOf(kWh).getBytes());
			try {
				topic.publish(message);
			} catch (MqttPersistenceException e) {
				logger.error("Failed to persist: " + message, e);
			} catch (MqttException e) {
				logger.error("Failed to publish: " + message, e);
			}
			if (CLEAR_COUNT) {
				connection.getOutputStream().write(CONFIRM_METER[meter]);
				for (int i = 0; i < pulses.length(); i++) {
					byte charAt = (byte) pulses.charAt(i);
					connection.getOutputStream().write(charAt);
				}
				connection.getOutputStream().write('\n');
			}
		} catch (IOException e) {
			logger.error("Failed to tweet", e);
			return false;
		}
		return true;

	}

	private String[] splitPowerResult(String result) throws NoSuchElementException {
		String[] r = new String[3];
		StringTokenizer st = new StringTokenizer(result, ",");
		r[0] = st.nextToken();
		String tmp = st.nextToken();
		r[1] = tmp.substring(tmp.indexOf(":") + 1);
		tmp = st.nextToken();
		r[2] = tmp.substring(tmp.indexOf(":") + 1).trim();
		return r;
	}

	private static double toKWh(String power) {
		return Double.parseDouble(power);
	}

	private void reconnect() {
		try {
			logger.debug("Reconecting.");
			init();
		} catch (Exception ex) {
			logger.error(ex);
		}
	}

	private String readProtocol(byte[] protocol) throws IOException {
		StringBuilder sb = new StringBuilder();
		logger.debug("Read protocol:" + new String(protocol));
		connection.getOutputStream().write(protocol);
		connection.getOutputStream().flush();
		char c;
		while (true) {
			int sleeps = 0;
			while (connection.getInputStream().available() <= 0) {
				sleeps++;
				if (sleeps > 20) {
					logger.error("Slept to long.");
					return null;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			if ((c = (char) connection.getInputStream().read()) == '\n') {
				break;
			}
			sb.append(c);
			if (sb.length() > 135) {
				String message = "To mutch to read... '" + sb + "'";
				logger.error(message);
				return null;
			}
		}

		String result = sb.toString().trim();
		logger.debug("Response: " + result);
		return result;
	}

	private Calendar getDailyConsumtionTime() {
		Calendar nextTime = getNextTime(0);
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
		return nextTime;
	}

	public void process() {

		logger.info("Setting power readings to every: " + POWER_PING_TIME + " sec");
		scheduler.scheduleAtFixedRate(this::readPowerMeter, 0, POWER_PING_TIME, SECONDS);

		logger.info("Setting temperature readings to every: " + TEMPERATURE_PING_TIME + " sec");
		scheduler.scheduleAtFixedRate(this::readTemperature, 0, TEMPERATURE_PING_TIME, SECONDS);

	}

	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {
		Options options = new Options();

		options.addOption("p", PORT, true, "Mqtt server port, default 1883");
		Option address = OptionBuilder.withLongOpt(ADDRESS).hasArg(true).withDescription("Mqtt server address")
				.isRequired().create('a');
		options.addOption(address);

		Option location = OptionBuilder.withLongOpt(LOCATION).hasArg(true).withDescription("Location name").isRequired()
				.create('l');
		options.addOption(location);

		OptionGroup optionGroup = new OptionGroup();
		optionGroup.isRequired();
		Option bluetooth = OptionBuilder.withLongOpt(BLUETOOTH).hasArg().withDescription("Buletooth address")
				.create('b');
		optionGroup.addOption(bluetooth);
		Option comport = OptionBuilder.withLongOpt(COMPORT).hasArg().withDescription("Serial com port").create('c');
		optionGroup.addOption(comport);

		options.addOptionGroup(optionGroup);

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
		SensorPublisher sensorPublisher = new SensorPublisher(cmd);
		while (true) {
			try {
				sensorPublisher.init();
			} catch (MqttException e) {
				logger.error("Failed to init.", e);
				Thread.sleep(10000);
				continue;
			}
			break;
		}
		sensorPublisher.process();
	}

	private static void printUsage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("SensorPublisher", options, true);
		System.exit(1);
	}

	class Callback implements MqttCallback {

		@Override
		public void connectionLost(Throwable cause) {
			logger.error("Connection lost", cause);
			reconnectMqtt();
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken arg0) {

		}

		@Override
		public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
		}
	}
}
