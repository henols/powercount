package se.aceone.housenews;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.log4j.Logger;

import twitter4j.Twitter;
import twitter4j.TwitterException;

public class PowerMeter extends BlueToothNews {
	private static final boolean DAYS = true;
	private static final boolean CLEAR_COUNT = DAYS;
	private static final SimpleDateFormat sdf = new SimpleDateFormat();
	private static final long PING_TIME = 60000;

	private static Logger logger = Logger.getLogger(PowerMeter.class);
	private String total;

	private long pingTime;

	private Calendar nextTweet;

	public PowerMeter(String bluetoothAddress) {
		super(bluetoothAddress);
	}

	@Override
	public void init() throws Exception {
		super.init();
		nextTweet = getNextTweetTime();
		pingTime = System.currentTimeMillis() + PING_TIME;
	}

	@Override
	public void tweet(Twitter twitter) {
		Calendar now = Calendar.getInstance();
		// logger.debug(sdf.format(now.getTime()) + " before " + sdf.format(nextTweet.getTime()));
		if (now.before(nextTweet)) {
			if (System.currentTimeMillis() > pingTime) {
				try { 
					StringBuilder sb = new StringBuilder();
					os.write((byte) '4');
					os.flush();
					char c;
					while ((c = (char) is.read()) != '\n') {
						sb.append(c);
						if (sb.length() > 15) {
							String message = "To mutch to read... '" + sb + "' (ping)";
							logger.error(message);
//							tweetError(twitter, message);
							break;
						}
					}
					// logger.debug("ping : " + sb);
					pingTime += PING_TIME;
				} catch (IOException e) {
					logger.error(e);
//					tweetError(twitter, e);
					reconnect(twitter);
				}
			}
			return;
		}
		logger.debug("Read power counter.");
		try {
			StringBuilder sb = new StringBuilder();
			os.write((byte) '4');
			os.flush();
			char c;
			while ((c = (char) is.read()) != '\n') {
				sb.append(c);
				if (sb.length() > 15) {
					String message = "To mutch to read... '" + sb + "'";
					logger.error(message);
//					tweetError(twitter, message);
					return;
				}
			}
			String power = sb.substring(sb.indexOf(":") + 1).trim();
			int p = Integer.parseInt(power);
			BigDecimal b = new BigDecimal(power);
			BigDecimal divide = b.divide(new BigDecimal(1000), 3, BigDecimal.ROUND_HALF_UP);
			total += divide.doubleValue();
			String status = "Last day's power consumption for the house were " + divide + "kWh. #tweetawatt #smarthome";
			logger.debug(status);
			try {
				twitter.updateStatus(status);
			} catch (TwitterException e) {
				logger.error("Failed to post Twitter maessage.", e);
			}
			if (CLEAR_COUNT) {
				os.write('c');
				for (int i = 0; i < power.length(); i++) {
					byte charAt = (byte) power.charAt(i);
					os.write(charAt);
				}
				os.write('\n');
			}
		} catch (IOException e) {
			logger.error(e);
//			tweetError(twitter, e);
			reconnect(twitter);
			return;
		}
		nextTweet = getNextTweetTime();
	}

	private void reconnect(Twitter twitter) {
		try {
			logger.debug("Reconecting.");
			init();
		} catch (Exception ex) {
			logger.error(ex);
			tweetError(twitter, ex);
		}
	}

	private void tweetError(Twitter twitter, Exception ex) {
		tweetError(twitter, ex.getMessage());
	}

	private void tweetError(Twitter twitter, String message) {
		try {
			twitter.updateStatus("PowerMeter: " + message + " #error #smarthome");
		} catch (TwitterException e2) {
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

}
