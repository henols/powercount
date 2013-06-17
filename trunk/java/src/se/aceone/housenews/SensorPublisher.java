package se.aceone.housenews;

import java.io.IOException;
import java.net.SocketException;
import java.util.Calendar;
import java.util.StringTokenizer;

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
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttDefaultFilePersistence;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;

public class SensorPublisher {

	private static final String BLUETOOTH = "bluetooth";
	private static final String COMPORT = "comport";
	private static final String PORT = "port";
	private static final String ADDRESS = "address";

	private static final byte[] READ_METER_1 = { '4', '0' };
	@SuppressWarnings("unused")
	private static final byte[] READ_METER_2 = { '4', '1' };
	private static final byte[] CONFIRM_METER_1 = { 'c', '0' };
	@SuppressWarnings("unused")
	private static final byte[] CONFIRM_METER_2 = { 'c', '1' };
	private static final byte[] TEMPERATURE = { 't', 't' };

	private static final boolean DAYS = true;
	private static final boolean CLEAR_COUNT = true;

	private static final long POWER_PING_TIME = 10000;
	 private static final long TEMPERATURE_PING_TIME = 300000;

	final static String POWER_TOPIC = "mulbetet49/powermeter/power";
	final static String KWH_TOPIC = "mulbetet49/powermeter/kwh";
	final static String TEMPERATURE_TOPIC = "mulbetet49/temperature/";

	private static Logger logger = Logger.getLogger(SensorPublisher.class);
	private static SensorPublisher powerMeter;
	// private int oldWh = Integer.MIN_VALUE;
	private double oldKWh = Double.NaN;

	private long powerPingTime;
	private long temperaturePingTime;

	private Calendar nextDailyConsumtion;
	private MqttClient client;
	private Connection connection;
	private String address;
	private String port = "1883";

	public SensorPublisher(CommandLine cmd) throws Exception {
		address = cmd.getOptionValue(ADDRESS);
		if (cmd.hasOption(BLUETOOTH)) {
			String bluetooth = cmd.getOptionValue(BLUETOOTH);
			logger.info("Using Bluetooth connection: " + bluetooth);
			connection = new BlueToothConnection(bluetooth);
		} else if (cmd.hasOption(COMPORT)) {
			String comport = cmd.getOptionValue(COMPORT);
			logger.info("Using Serial connection: " + comport);
			connection = new SerialPortConnection(comport);
		}
		if (cmd.hasOption(PORT)) {
			port = cmd.getOptionValue(PORT);
		}
	}

	public void init() throws Exception {
		nextDailyConsumtion = getDailyConsumtionTime();
		long currentTimeMillis = System.currentTimeMillis();
		powerPingTime = currentTimeMillis + POWER_PING_TIME;
		temperaturePingTime = currentTimeMillis;
		String serverURI = "tcp://" + address + ":" + port;
		logger.info("Connecting to MQTT server : " + serverURI);

		String tmpDir = System.getProperty("java.io.tmpdir");
		MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir + "/mqtt");

		client = new MqttClient(serverURI, "SensorPublisher", dataStore);
		client.setCallback(new Callback());
		client.connect();
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

	public void tick() {
		Calendar now = Calendar.getInstance();
		readPowerMeter(now);
		readTemperature(now);
	}

	private void readTemperature(Calendar now) {
		if (now.getTimeInMillis() > temperaturePingTime) {
			if (publishTemperature(now)) {
				temperaturePingTime += TEMPERATURE_PING_TIME;
			}
		}
	}

	private void readPowerMeter(Calendar now) {
		if (now.getTimeInMillis() > powerPingTime) {
			if (publishPower()) {
				powerPingTime += POWER_PING_TIME;
			}
		}
		if (now.after(nextDailyConsumtion)) {
			if (publishDailyConsumtion()) {
				nextDailyConsumtion = getDailyConsumtionTime();
			}
		}
	}

	private boolean publishTemperature(Calendar now) {
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

		String[] strings = result.split(",");
		for (String string : strings) {
			int indexOf = string.indexOf(':');
			if (indexOf < 0) {
				continue;
			}
			MqttTopic topic = client.getTopic(TEMPERATURE_TOPIC + string.substring(0, indexOf));
			message.setPayload(string.substring(indexOf + 1).getBytes());
			try {
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
		String result;
		try {
			result = readProtocol(READ_METER_1);
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

		String[] r = splitPowerResult(result);
		@SuppressWarnings("unused")
		String counter = r[0];
		String pulses = r[1];
		String power = r[2];
		// logger.debug("pulses:"+pulses+" power:"+power);
		if (Long.parseLong(pulses) < 0 || Long.parseLong(power) < 0) {
			logger.error("We seem to have a negative value: pulses:" + pulses + " power:" + power);
			return false;
		}
		double kWh = toKWh(pulses);
		// int Wh = Integer.parseInt(pulses);

		if (!Double.isNaN(oldKWh)) {
			double nKWh = kWh - oldKWh;

			MqttMessage message = new MqttMessage();
			message.setQos(1);

			MqttTopic topic = client.getTopic(KWH_TOPIC);
			message.setPayload(String.valueOf(nKWh).getBytes());
			try {
				topic.publish(message);
			} catch (MqttPersistenceException e) {
				logger.error("Failed to persist: " + message, e);
			} catch (MqttException e) {
				logger.error("Failed to publish: " + message, e);
			}
			topic = client.getTopic(POWER_TOPIC);
			message.setPayload(power.getBytes());
			try {
				topic.publish(message);
			} catch (MqttPersistenceException e) {
				logger.error("Failed to persist: " + message, e);
			} catch (MqttException e) {
				logger.error("Failed to publish: " + message, e);
			}
		}
		// oldWh = Wh;
		oldKWh = kWh;
		// logger.debug("ping : " + sb.toString().trim());
		return true;
	}

	private boolean publishDailyConsumtion() {
		logger.debug("Read power counter.");
		try {
			String result = readProtocol(READ_METER_1);
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
			oldKWh = 0;
			MqttMessage message = new MqttMessage();
			message.setQos(1);

			MqttTopic topic = client.getTopic(KWH_TOPIC + "/dailyconsumption");
			message.setPayload(String.valueOf(kWh).getBytes());
			try {
				topic.publish(message);
			} catch (MqttPersistenceException e) {
				logger.error("Failed to persist: " + message, e);
			} catch (MqttException e) {
				logger.error("Failed to publish: " + message, e);
			}
			if (CLEAR_COUNT) {
				connection.getOutputStream().write(CONFIRM_METER_1);
				for (int i = 0; i < pulses.length(); i++) {
					byte charAt = (byte) pulses.charAt(i);
					connection.getOutputStream().write(charAt);
				}
				connection.getOutputStream().write('\n');
			}
		} catch (Exception e) {
			logger.error("Faild to tweet", e);
			return false;
		}
		return true;

	}

	private String[] splitPowerResult(String result) {
		String[] r = new String[3];
		StringTokenizer st = new StringTokenizer(result, ",");
		r[0] = st.nextToken();
		String tmp = st.nextToken();
		r[1] = tmp.substring(tmp.indexOf(":") + 1);
		tmp = st.nextToken();
		r[2] = tmp.substring(tmp.indexOf(":") + 1).trim();
		return r;
	}

	// private double toKWh(String power) throws NumberFormatException {
	// BigDecimal b = new BigDecimal(power);
	// BigDecimal divide = b.divide(new BigDecimal(1000), 3,
	// BigDecimal.ROUND_HALF_UP);
	// double kWh = divide.doubleValue();
	// return kWh;
	// return ((double)Integer.parseInt(power))/1000;
	// }

	private static double toKWh(String power) {
		if (power.length() == 1) {
			power = "0.00" + power;
		} else if (power.length() == 2) {
			power = "0.0" + power;
		} else if (power.length() == 3) {
			power = "0." + power;
		} else {
			int l = power.length();
			power = power.substring(0, l - 3) + "." + power.substring(l - 3);
		}
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
		logger.debug(result);
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
		while (true) {
			tick();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
	}

	@SuppressWarnings("static-access")
	public static void main(String[] args) throws Exception {
		Options options = new Options();

		options.addOption("p", PORT, true, "Mqtt server port, default 1883");
		Option address = OptionBuilder.withLongOpt(ADDRESS).hasArg(true).withDescription("Mqtt server address").isRequired().create('a');

		options.addOption(address);

		OptionGroup optionGroup = new OptionGroup();
		optionGroup.isRequired();
		Option bluetooth = OptionBuilder.withLongOpt(BLUETOOTH).hasArg().withDescription("Buletooth address").create('b');
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

		powerMeter = new SensorPublisher(cmd);
		powerMeter.init();
		powerMeter.process();
	}

	private static void printUsage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("SensorPublisher", options, true);
		System.exit(1);
	}

	class Callback implements MqttCallback {

		@Override
		public void messageArrived(MqttTopic topic, MqttMessage message) throws Exception {
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
}