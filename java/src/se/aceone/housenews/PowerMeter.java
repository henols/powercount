package se.aceone.housenews;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import twitter4j.TwitterException;

public class PowerMeter extends News {

	private static final String TEMP_SENSOR = "1053C65B02080047:";
	private static final byte[] READ_METER_1 = { '4', '0' };
	private static final byte[] CONFIRM_METER_1 = { 'c', '0' };
	@SuppressWarnings("unused")
	private static final byte[] READ_METER_2 = { '4', '1' };
	@SuppressWarnings("unused")
	private static final byte[] CONFIRM_METER_2 = { 'c', '1' };
	private static final byte[] TEMPERATURE = { 't', 't' };

	private static final boolean DAYS = true;
	private static final boolean CLEAR_COUNT = true;

	private static final SimpleDateFormat sdf = new SimpleDateFormat();
	private static final SimpleDateFormat hhMM = new SimpleDateFormat("HH:mm");

	private static final long POWER_PING_TIME = 10000;
	private static final long TEMPERATURE_PING_TIME = 300000;

	private static Logger logger = Logger.getLogger(PowerMeter.class);
//	private int oldWh = Integer.MIN_VALUE;
	private double oldKWh = Double.NaN;

	private long powerPingTime;
	private long temperaturePingTime;

	private Calendar nextPowerTweet;
	private Calendar nextTemperatureTweet;

	private double max = -10000;
	private long maxStamp;
	private double min = 10000;
	private long minStamp;
	private List<Double> values = new ArrayList<Double>();

	private Calendar tempAt_00_00_c;
	private Calendar tempAt_06_00_c;
	private Calendar tempAt_12_00_c;
	private Calendar tempAt_18_00_c;

	private Connection connection;

	public PowerMeter(Connection connection) {
		this.connection = connection;
	}

	@Override
	public void init() throws Exception {
		nextPowerTweet = getNextPowerTweetTime();
		nextTemperatureTweet = getNextTemperatureTweetTime();
		tempAt_00_00_c = getNextTime(0);
		tempAt_06_00_c = getNextTime(6);
		tempAt_12_00_c = getNextTime(12);
		tempAt_18_00_c = getNextTime(18);

		powerPingTime = System.currentTimeMillis() + POWER_PING_TIME;
		temperaturePingTime = System.currentTimeMillis() + TEMPERATURE_PING_TIME;
	}

	@Override
	public void tick() {
		Calendar now = Calendar.getInstance();
		// logger.debug(sdf.format(now.getTime()) + " before " +
		// sdf.format(nextTweet.getTime()));

		readPowerMeter(now);
		readTemperature(now);
	}

	private void readTemperature(Calendar now) {
		if (now.getTimeInMillis() > temperaturePingTime) {
			temperaturePingTime += TEMPERATURE_PING_TIME;
			logTemperature(now);
		}
		if (!now.before(nextTemperatureTweet)) {
			tweetTemperature();
			nextTemperatureTweet = getNextTemperatureTweetTime();
		}
	}

	private void readPowerMeter(Calendar now) {

		if (now.getTimeInMillis() > powerPingTime) {
			powerPingTime += POWER_PING_TIME;
			logPower();
		}
		if (!now.before(nextPowerTweet)) {
			tweetPower();
			nextPowerTweet = getNextPowerTweetTime();
		}
	}

	
	private void tweetTemperatureNow(double temp, Calendar now){
		String status = "The temperature are " + temp+ "°C at " + hhMM.format(now.getTime()) + ". #temperature";
		logger.debug(status + " l:" + status.length());
		try {
			Util.post2Twitter(status);
		} catch (TwitterException e) {
			logger.error("Failed to post Twitter message.", e);
		}
	}
	
	private void tweetTemperature() {
		double average = getAverage();
		String status = "Last day's temperatures: average:" + average + "°C, max:" + max + "°C at " + hhMM.format(new Date(maxStamp)) + ", min:" + min + "°C at "
				+ hhMM.format(new Date(minStamp)) + ". #temperature";
		logger.debug(status + " l:" + status.length());
		try {
			Util.post2Twitter(status);
		} catch (TwitterException e) {
			logger.error("Failed to post Twitter message.", e);
		}
		max = -10000;
		min = 10000;
		values.clear();
	}

//Last day's temperatures: average:1.11°C, max:1.56°C @13:54, min:-0.56°C @10:04, 0.88°C @12:00, 1.31°C @18:00, 1.31°C @00:00, 1.06°C @06:00. #temperature l:152
	
	private void logTemperature(Calendar now) {
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
		try {
			 Util.post2Emon(result);
		} catch (MalformedURLException e) {
			logger.error("MalformedURLException", e);
		} catch (IOException e) {
			logger.error("IOException", e);
		}
		int ind1 = result.indexOf(TEMP_SENSOR) + TEMP_SENSOR.length();
		if (ind1 >= 0) {
			int ind2 = result.indexOf(',', ind1);
			if (ind2 >= 0) {
				double outDoorTemp = Double.parseDouble(result.substring(ind1, ind2));

				if (outDoorTemp > max) {
					max = outDoorTemp;
					maxStamp = System.currentTimeMillis();
				}
				if (outDoorTemp < min) {
					min = outDoorTemp;
					minStamp = System.currentTimeMillis();
				}

				values.add(outDoorTemp);

				double average = getAverage();
				logger.debug("average:" + average + "°C max:" + max + "°C at " + hhMM.format(new Date(maxStamp)) + " min:" + min + "°C at "
						+ hhMM.format(new Date(minStamp)) + " now:" + outDoorTemp + "°C");
				if (now.after(tempAt_00_00_c)) {
					tweetTemperatureNow(outDoorTemp, tempAt_00_00_c);
					tempAt_00_00_c = getNextTime(0);
				}
				if (now.after(tempAt_06_00_c)) {
					tweetTemperatureNow(outDoorTemp, tempAt_06_00_c);
					tempAt_06_00_c = getNextTime(6);
				}
				if (now.after(tempAt_12_00_c)) {
					tweetTemperatureNow(outDoorTemp, tempAt_12_00_c);
					tempAt_12_00_c = getNextTime(12);
				}
				if (now.after(tempAt_18_00_c)) {
					tweetTemperatureNow(outDoorTemp, tempAt_18_00_c);
					tempAt_18_00_c = getNextTime(18);
				}
			}
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

	private void tweetPower() {
		logger.debug("Read power counter.");
		try {
			String result = readProtocol(READ_METER_1);
			if (result == null) {
				return;
			}
			String[] r = splitPowerResult(result.toString());
			@SuppressWarnings("unused")
			String counter = r[0];
			String pulses = r[1];
			String power = r[2];
			double kWh = toKWh(pulses);
//			oldWh = 0;
			oldKWh = 0;
			String status = "Last day's power consumption for the house were " + kWh + "kWh. Using " + power + "w right now. #tweetawatt";

			logger.debug(status);
			try {
				Util.post2Twitter(status);
			} catch (TwitterException e) {
				logger.error("Failed to post Twitter maessage.", e);
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
			return;
		}
	}

	private void logPower() {
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
		@SuppressWarnings("unused")
		String counter = r[0];
		String pulses = r[1];
		String power = r[2];
		// logger.debug("pulses:"+pulses+" power:"+power);
		if (Long.parseLong(pulses) < 0 || Long.parseLong(power) < 0) {
			logger.error("We seem to have a negative value: pulses:" + pulses + " power:" + power);
			return;
		}
		double kWh = toKWh(pulses);
//		int Wh = Integer.parseInt(pulses);
		if (!Double.isNaN(oldKWh)) {
			double nKWh = kWh - oldKWh;
			String values = "kWh:" + nKWh + ",power:" + power;
			// if (oldWh!= Integer.MIN_VALUE) {
			// int nWh = Wh - oldWh;
			// String values = "Wh:" + nWh + ",power:" + power;
			try {
				 Util.post2Emon(values);
			} catch (MalformedURLException e) {
				logger.error("MalformedURLException", e);
			} catch (IOException e) {
				logger.error("IOException", e);
			}
		}
//		oldWh = Wh;
		oldKWh = kWh;
		// logger.debug("ping : " + sb.toString().trim());
		return;
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

	private Calendar getNextPowerTweetTime() {
		Calendar nextTime = getNextTime(0);
		logger.debug("Creating new next power tweet time: " + sdf.format(nextTime.getTime()));
		return nextTime;
	}

	private Calendar getNextTemperatureTweetTime() {
		Calendar nextTime = getNextTime(8);
		logger.debug("Creating new next temperature tweet time: " + sdf.format(nextTime.getTime()));
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

	private String readProtocol(byte[] protocol) throws IOException {
		StringBuilder sb = new StringBuilder();
		connection.getOutputStream().write(protocol);
		connection.getOutputStream().flush();
		char c;
		while ((c = (char) connection.getInputStream().read()) != '\n') {
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

	public static void main(String[] args) throws Exception {
		PowerMeter powerMeter = new PowerMeter(new SerialPortConnectin("COM3"));
		powerMeter.init();
	}

}
