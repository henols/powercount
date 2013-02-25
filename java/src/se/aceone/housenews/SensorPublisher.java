package se.aceone.housenews;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.util.Calendar;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import twitter4j.TwitterException;

public class SensorPublisher extends SerialPortNews {

	private static final byte[] READ_METER_1 = { '4', '0' };
	private static final byte[] READ_METER_2 = { '4', '1' };
	private static final byte[] CONFIRM_METER_1 = { 'c', '0' };
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
	private int oldWh = Integer.MIN_VALUE;
	private double oldKWh = Double.NaN;

	private long powerPingTime;
	private long temperaturePingTime;

	private Calendar nextDailyConsumtion;
	private MqttClient client;

	public SensorPublisher(String port) {
		super(port);
	}

	@Override
	public void init() throws Exception {
		super.init();
		nextDailyConsumtion = getDailyConsumtionTime();
		powerPingTime = System.currentTimeMillis() + POWER_PING_TIME;
		temperaturePingTime = System.currentTimeMillis() + TEMPERATURE_PING_TIME;

		client = new MqttClient("tcp://192.168.1.121:1889", "Publisher_" + MqttClient.generateClientId());

	}

	@Override
	public void tick() {
		Calendar now = Calendar.getInstance();
		readPowerMeter(now);
		readTemperature(now);
	}

	private void readTemperature(Calendar now) {
		if (now.getTimeInMillis() > temperaturePingTime) {
			temperaturePingTime += TEMPERATURE_PING_TIME;
			publishTemperature(now);
		}
	}

	private void readPowerMeter(Calendar now) {

		if (now.getTimeInMillis() > powerPingTime) {
			powerPingTime += POWER_PING_TIME;
			publishPower();
		}
		if (now.after(nextDailyConsumtion)) {
			publishDailyConsumtion();
			nextDailyConsumtion = getDailyConsumtionTime();
		}
	}

	private void publishTemperature(Calendar now) {
		String result;
		try {
			result = readProtocol(TEMPERATURE);
			if (result == null) {
				return;
			}
		} catch (SocketException e) {
			logger.error("SocketException", e);
			reconnect();
			return;
		} catch (IOException e) {
			logger.error("IOException", e);
			reconnect();
			return;
		}

		// Split result
		// power:252.4,temperature:15.4
		
		MqttMessage message = new MqttMessage();
		message.setQos(2);
		
		String[] strings = result.split(",");
		for (String string : strings) {
			int indexOf = string.indexOf(':');
			MqttTopic topic = client.getTopic(TEMPERATURE_TOPIC+string.substring(0,indexOf));
			message.setPayload(string.substring(indexOf+1).getBytes());
			try {
				topic.publish(message);
			} catch (MqttPersistenceException e) {
				logger.error("Failed to persist: "+message,e);
			} catch (MqttException e) {
				logger.error("Failed to publish: "+message,e);
			}
		}
	}

	private void publishPower() {
		String result;
		try {
			result = readProtocol(READ_METER_1);
			if (result == null) {
				return;
			}
		} catch (SocketException e) {
			logger.error("SocketException", e);
			reconnect();
			return;
		} catch (IOException e) {
			logger.error("IOException", e);
			reconnect();
			return;
		}

		String[] r = splitPowerResult(result);
		String counter = r[0];
		String pulses = r[1];
		String power = r[2];
		// logger.debug("pulses:"+pulses+" power:"+power);
		if (Long.parseLong(pulses) < 0 || Long.parseLong(power) < 0) {
			logger.error("We seem to have a negative value: pulses:" + pulses + " power:" + power);
			return;
		}
		double kWh = toKWh(pulses);
		int Wh = Integer.parseInt(pulses);
	
		if (!Double.isNaN(oldKWh)) {
			double nKWh = kWh - oldKWh;

			MqttMessage message = new MqttMessage();
			message.setQos(2);
			
			MqttTopic topic = client.getTopic(KWH_TOPIC);
			message.setPayload(String.valueOf(nKWh).getBytes());
			try {
				topic.publish(message);
			} catch (MqttPersistenceException e) {
				logger.error("Failed to persist: "+message,e);
			} catch (MqttException e) {
				logger.error("Failed to publish: "+message,e);
			}
			topic = client.getTopic(POWER_TOPIC);
			message.setPayload(power.getBytes());
			try {
				topic.publish(message);
			} catch (MqttPersistenceException e) {
				logger.error("Failed to persist: "+message,e);
			} catch (MqttException e) {
				logger.error("Failed to publish: "+message,e);
			}
		}
		oldWh = Wh;
		oldKWh = kWh;
		// logger.debug("ping : " + sb.toString().trim());
		return;
	}

	private void publishDailyConsumtion() {
		logger.debug("Read power counter.");
		try {
			String result = readProtocol(READ_METER_1);
			if (result == null) {
				return;
			}
			String[] r = splitPowerResult(result.toString());
			String counter = r[0];
			String pulses = r[1];
			String power = r[2];
			double kWh = toKWh(pulses);
			oldWh = 0;
			oldKWh = 0;
			MqttMessage message = new MqttMessage();
			message.setQos(2);
			
			MqttTopic topic = client.getTopic(KWH_TOPIC);
			message.setPayload(String.valueOf(kWh).getBytes());
			try {
				topic.publish(message);
			} catch (MqttPersistenceException e) {
				logger.error("Failed to persist: "+message,e);
			} catch (MqttException e) {
				logger.error("Failed to publish: "+message,e);
			}
			if (CLEAR_COUNT) {
				os.write(CONFIRM_METER_1);
				for (int i = 0; i < pulses.length(); i++) {
					byte charAt = (byte) pulses.charAt(i);
					os.write(charAt);
				}
				os.write('\n');
			}
		} catch (Exception e) {
			logger.error("Faild to tweet", e);
			return;
		}
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
		os.write(protocol);
		os.flush();
		char c;
		while ((c = (char) is.read()) != '\n') {
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
		return nextTime;
	}

	public static void main(String[] args) throws Exception {
		SensorPublisher powerMeter = new SensorPublisher("COM3");
		powerMeter.init();
	}
}
