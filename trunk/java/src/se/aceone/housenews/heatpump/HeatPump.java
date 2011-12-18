package se.aceone.housenews.heatpump;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.log4j.Logger;

import se.aceone.housenews.BlueToothNews;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class HeatPump extends BlueToothNews {
	private static final boolean DAYS = true;
	private static final boolean CLEAR_COUNT = DAYS;
	private static final SimpleDateFormat sdf = new SimpleDateFormat();
	private static final long PING_TIME = 60000;

	private static Logger logger = Logger.getLogger(HeatPump.class);
	private String total;

	private long pingTime;

	private Calendar nextTweet;
	private Rego600 rego600;

	public HeatPump(String bluetoothAddress) {
		super(bluetoothAddress);
	}

	@Override
	public void init() throws Exception {
		super.init();
		nextTweet = getNextTweetTime();
		pingTime = System.currentTimeMillis() + PING_TIME;
		rego600 = new Rego600(os, is);
	}

	@Override
	public void tweet(Twitter twitter) {
		try {
			double temp = rego600.getRegisterValueTemperature(Rego600.OUTDOOR_TEMP_GT2);
			System.out.println("Radiator return 0x020B=" + temp);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (DataException e) {
			logger.error(e.getMessage(), e);
		}
		//
		// char c;
		// try {
		// while ((c = (char) is.read()) != '\n') {
		// System.out.println(c);
		// }
		// } catch (IOException e) {
		// logger.error("Fuck", e);
		// }
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
		String bluetoothAddress = "00195dee2307";
		HeatPump heatPump = new HeatPump(bluetoothAddress);
		heatPump.init();
		heatPump.tweet(null);
	}
}
