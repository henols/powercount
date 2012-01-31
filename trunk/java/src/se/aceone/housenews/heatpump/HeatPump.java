package se.aceone.housenews.heatpump;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import se.aceone.housenews.BlueToothNews;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class HeatPump extends BlueToothNews {
	private static final boolean DAYS = true;
	private static final boolean CLEAR_COUNT = DAYS;
	private static final SimpleDateFormat sdf = new SimpleDateFormat();
	private static final SimpleDateFormat hhMM = new SimpleDateFormat("HH:mm");
	private static final long PING_TIME = 60000;

	private static Logger logger = Logger.getLogger(HeatPump.class);
	private String total;

	private long pingTime;

	private Calendar nextTweet;
	private Rego600 rego600;

	private double max = -10000;
	private long maxStamp;
	private double min = 10000;
	private long minStamp;

	private List<Double> values = new ArrayList<Double>();

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
		Calendar now = Calendar.getInstance();
		// logger.debug(sdf.format(now.getTime()) + " before " +
		// sdf.format(nextTweet.getTime()));
		if (now.before(nextTweet)) {
			if (System.currentTimeMillis() > pingTime) {
				try {
					double outDoorTemp = rego600.getRegisterValueTemperature(Rego600.OUTDOOR_TEMP_GT2);

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
					logger.debug("average:" + average + " max:" + max + " at " + hhMM.format(new Date(maxStamp))
							+ " min:" + min + " at " + hhMM.format(new Date(minStamp)) + " now:" + outDoorTemp);
					pingTime += PING_TIME;
				} catch (IOException e) {
					logger.error(e);
					reconnect(twitter);
				} catch (DataException e) {
					logger.error(e);
					reconnect(twitter);
				}
			}
			return;
		}

		double average = getAverage();
		String status = "Last day's temperatures, average:" + average + " max:" + max + " at "
				+ hhMM.format(new Date(maxStamp)) + "  min:" + min + " at " + hhMM.format(new Date(minStamp))
				+ ". #temperature #smarthome";
		logger.debug(status + " l:" + status.length());
		try {
			twitter.updateStatus(status);
		} catch (TwitterException e) {
			logger.error("Failed to post Twitter maessage.", e);
		}
		max = -10000;
		min = 10000;
		values.clear();
		nextTweet = getNextTweetTime();
	}

	private double getAverage() {
		double total = 0;
		for (double value : values) {
			total += value;
		}
		double average = total / values.size();
		int ix = (int)(average * 10.0); // scale it 
		return ((double)ix)/10.0;
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

	private void reconnect(Twitter twitter) {
		try {
			logger.debug("Reconecting.");
			init();
		} catch (Exception ex) {
			logger.error(ex);
		}
	}

	public static void main(String[] args) throws Exception {
		String bluetoothAddress = "00195dee2307";
		HeatPump heatPump = new HeatPump(bluetoothAddress);
		heatPump.init();
		while (true) {
			heatPump.tweet(null);
			Thread.sleep(10000);
		}
	}
}
