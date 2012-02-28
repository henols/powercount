package se.aceone.housenews;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import twitter4j.TwitterException;

public class PowerMeter extends SerialPortNews {

	private static final byte[] READ_METER_1 = { '4', '0' };
	private static final byte[] READ_METER_2 = { '4', '1' };
	private static final byte[] CONFIRM_METER_1 = { 'c', '0' };
	private static final byte[] CONFIRM_METER_2 = { 'c', '1' };

	private static final boolean DAYS = true;
	private static final boolean CLEAR_COUNT = true;
	private static final SimpleDateFormat sdf = new SimpleDateFormat();
	private static final long PING_TIME = 10000;

	private static Logger logger = Logger.getLogger(PowerMeter.class);
	private double oldKWh = Double.NaN;

	private long pingTime;

	private Calendar nextTweet;

	public PowerMeter(String port) {
		super(port);
	}

	@Override
	public void init() throws Exception {
		super.init();
		nextTweet = getNextTweetTime();
		pingTime = System.currentTimeMillis() + PING_TIME;
	}

	@Override
	public void tick() {
		Calendar now = Calendar.getInstance();
		// logger.debug(sdf.format(now.getTime()) + " before " +
		// sdf.format(nextTweet.getTime()));
		if (now.before(nextTweet)) {
			if (System.currentTimeMillis() > pingTime) {
				StringBuilder sb = new StringBuilder();
				try {
					os.write(READ_METER_1);
					os.flush();
					char c;
					while ((c = (char)is.read()) != '\n') {
						sb.append(c);
						if (sb.length() > 35) {
							String message = "To mutch to read... '" + sb + "' (ping)";
							logger.error(message);
							// tweetError(twitter, message);
							break;
						}
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

				logger.debug(sb.toString().trim());
				String[] result = splitResult(sb.toString());
				String counter = result[0];
				String pulses = result[1];
				String power = result[2];
				// logger.debug("pulses:"+pulses+" power:"+power);
				if (Long.parseLong(pulses) < 0 || Long.parseLong(power) < 0) {
					logger.error("We seem to have a negative value: pulses:" + pulses + " power:" + power);
					return;
				}
				double kWh = toKWh(pulses);
				if (!Double.isNaN(oldKWh)) {
					double nKWh = kWh - oldKWh;
					String values = "kWh:" + nKWh + ",power:" + power;
					try {
						int resp = post2Emon(values);
						logger.debug(resp + " " + values);
					} catch (MalformedURLException e) {
						logger.error("MalformedURLException", e);
					} catch (IOException e) {
						logger.error("IOException", e);
					}
				}
				oldKWh = kWh;
				// logger.debug("ping : " + sb.toString().trim());
				pingTime += PING_TIME;
			}
			return;
		}
		logger.debug("Read power counter.");
		try {
			StringBuilder sb = new StringBuilder();
			os.write(READ_METER_1);
			os.flush();
			char c;
			while ((c = (char)is.read()) != '\n') {
				sb.append(c);
				if (sb.length() > 35) {
					String message = "To mutch to read... '" + sb + "'";
					logger.error(message);
					return;
				}
			}

			logger.debug(sb.toString());
			String[] result = splitResult(sb.toString());
			String counter = result[0];
			String pulses = result[1];
			String power = result[2];

			double kWh = toKWh(pulses);
			oldKWh = 0;
			String status = "Last day's power consumption for the house were " + kWh + "kWh. Using " + power + "w right now. #tweetawatt";
			logger.debug(status);
			try {
				post2Twitter(status);
			} catch (TwitterException e) {
				logger.error("Failed to post Twitter maessage.", e);
			}
			if (CLEAR_COUNT) {
				os.write(CONFIRM_METER_1);
				for(int i = 0; i < power.length(); i++) {
					byte charAt = (byte)power.charAt(i);
					os.write(charAt);
				}
				os.write('\n');
			}
		} catch (Exception e) {
			logger.error("Faild to tweet", e);
			return;
		}
		nextTweet = getNextTweetTime();
	}

	private String[] splitResult(String result) {
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

	private Calendar getNextTweetTime() {
		Calendar nextTime = Calendar.getInstance();

		// Zero out the hour, minute, second, and millisecond
		if (DAYS) {
			nextTime.set(Calendar.HOUR_OF_DAY, 0);
			nextTime.set(Calendar.MINUTE, 0);
		}
		nextTime.set(Calendar.SECOND, 0);
		nextTime.set(Calendar.MILLISECOND, 0);

		if (DAYS) {
			nextTime.add(Calendar.DAY_OF_YEAR, 1);
		} else {
			nextTime.add(Calendar.MINUTE, 1);
		}
		logger.debug("Creating new next time: " + sdf.format(nextTime.getTime()));
		return nextTime;
	}

	public static void main(String[] args) throws Exception {
		PowerMeter powerMeter = new PowerMeter("COM3");
		powerMeter.init();
	}

}
