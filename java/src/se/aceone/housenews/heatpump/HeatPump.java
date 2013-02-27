package se.aceone.housenews.heatpump;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import se.aceone.housenews.Connection;
import se.aceone.housenews.News;
import se.aceone.housenews.SerialPortConnection;
import se.aceone.housenews.Util;
import twitter4j.TwitterException;

public class HeatPump extends News {

	public static final int[] TEMPS = new int[] { //
	Rego600.RADIATOR_RETURN_GT1, // Radiator return[GT1]
			// Rego600.OUTDOOR_TEMP_GT2, // Outdoor [GT2]
			Rego600.HOT_WATER_GT3, // Hot water [GT3]
			Rego600.FORWARD_GT4, // Forward [GT4]
			Rego600.ROOM_GT5, // Room [GT5]
			Rego600.COMPRESSOR_GT6, // Compressor [GT6]
			Rego600.HEAT_FLUID_OUT_GT8, // Heat fluid out [GT8]
			Rego600.HEAT_FLUID_IN_GT9, // Heat fluid in [GT9]
			Rego600.TRANSFER_FLUID_IN_GT10, // Cold fluid in [GT10]
			Rego600.TRANSFER_FLUID_OUT_GT11, // Cold fluid out [GT11]
			Rego600.HOT_WATER_EXTERNAL_GT3X, // External hot water [GT3x]
	};
	public static final int[] SENSORS = new int[] { //
	Rego600.GROUND_LOOP_PUMP_P3, // Ground loop pump [P3]
			Rego600.COMPRESSOR, // Compressor
			Rego600.ADDITIONAL_HEAT_STEP_1, // Additional heat 3kW
			Rego600.ADDITIONAL_HEAT_STEP_2, // Additional heat 6kW
			Rego600.RADIATOR_PUMP_P1, // Radiator pump [P1]
			Rego600.HEAT_CARRIER_PUMP_P2, // Heat carrier pump [P2]
			Rego600.THREE_WAY_VALVE, // Tree-way valve [VXV]
			Rego600.ALARM, // Alarm
	};

	private static final boolean DAYS = true;
	private static final SimpleDateFormat sdf = new SimpleDateFormat();
	private static final SimpleDateFormat hhMM = new SimpleDateFormat("HH:mm");
	private static final long PING_TIME = 60000;

	private static Logger logger = Logger.getLogger(HeatPump.class);

	private long pingTime;

	private Calendar nextTweet;
	private Rego600 rego600;

	private double max = -10000;
	private long maxStamp;
	private double min = 10000;
	private long minStamp;

	private List<Double> values = new ArrayList<Double>();
	private Connection connection;

	public HeatPump(Connection connection) {
		this.connection = connection;
	}

	@Override
	public void init() throws Exception {
		nextTweet = getNextTweetTime();
		pingTime = System.currentTimeMillis() + PING_TIME;
		rego600 = new Rego600(connection);
	}

	static String toCamelCase(String s) {
		StringTokenizer st = new StringTokenizer(s, " -_[]");
		String camelCaseString = "";
		while (st.hasMoreTokens()) {
			String part = st.nextToken();
			camelCaseString += part.substring(0, 1).toUpperCase();
			if (part.length() > 1) {
				camelCaseString += part.substring(1).toLowerCase();
			}
		}
		return camelCaseString;
	}

	@Override
	public void tick() {
		Calendar now = Calendar.getInstance();
		// logger.debug(sdf.format(now.getTime()) + " before " +
		// sdf.format(nextTweet.getTime()));
		if (now.before(nextTweet)) {
			if (System.currentTimeMillis() > pingTime) {
				double outDoorTemp;
				StringBuffer post = new StringBuffer();
				try {
					outDoorTemp = rego600.getRegisterValueTemperature(Rego600.OUTDOOR_TEMP_GT2);
					for (int reg : TEMPS) {
						post.append(toCamelCase(Rego600.translateRegister(reg)));
						post.append(':');
						post.append(rego600.getRegisterValueTemperature(reg));
						post.append(',');
					}
					for (int reg : SENSORS) {
						post.append(toCamelCase(Rego600.translateRegister(reg)));
						post.append(':');
						post.append(rego600.getRegisterValue(reg));
						post.append(',');
					}
					post.append("OutDoorTemp:");
					post.append(outDoorTemp);
				} catch (IOException e) {
					logger.error(e);
					reconnect();
					return;
				} catch (DataException e) {
					logger.error(e);
					reconnect();
					return;
				}

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
				logger.debug("average:" + average + " max:" + max + " at " + hhMM.format(new Date(maxStamp)) + " min:"
						+ min + " at " + hhMM.format(new Date(minStamp)) + " now:" + outDoorTemp);

				try {
					logger.debug(post.toString());
					Util.post2Emon(post.toString());
				} catch (MalformedURLException e) {
					logger.error(e);
				} catch (IOException e) {
					logger.error(e);
				}

				pingTime += PING_TIME;
			}
			return;
		}

		double average = getAverage();
		String status = "Last day's temperatures, average:" + average + "°C max:" + max + "°C at "
				+ hhMM.format(new Date(maxStamp)) + "  min:" + min + "°C at " + hhMM.format(new Date(minStamp))
				+ ". #temperature";
		logger.debug(status + " l:" + status.length());
		try {
			Util.post2Twitter(status);
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
		int ix = (int) (average * 10.0); // scale it
		return ((double) ix) / 10.0;
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

	private void reconnect() {
		try {
			logger.debug("Reconecting.");
			init();
		} catch (Exception ex) {
			logger.error(ex);
		}
	}

	public static void main(String[] args) throws Exception {

		String comPort = "COM8";
		HeatPump heatPump = new HeatPump(new SerialPortConnection(comPort));
		heatPump.init();
		System.out.print(toCamelCase(Rego600.translateRegister(Rego600.OUTDOOR_TEMP_GT2)));
		try {
			System.out.println(" " + heatPump.rego600.getRegisterValueTemperature(Rego600.OUTDOOR_TEMP_GT2));
		} catch (DataException e) {
			System.out.println(" " + e.getMessage());
		}

		for (int reg : TEMPS) {
			System.out.print(toCamelCase(Rego600.translateRegister(reg)));
			try {
				System.out.println(" " + heatPump.rego600.getRegisterValueTemperature(reg));
			} catch (DataException e) {
				System.out.println(" " + e.getMessage());
			}
		}
		System.out.println();
		for (int reg : SENSORS) {
			System.out.print(toCamelCase(Rego600.translateRegister(reg)));
			try {
				System.out.println(" " + heatPump.rego600.getRegisterValue(reg));
			} catch (DataException e) {
				System.out.println(" " + e.getMessage());
			}
		}

		// try {
		// double temp =
		// heatPump.rego600.getRegisterValueTemperature(Rego600.HEAT_CURVE);
		// System.out.println("Heat curve 0x0000=" + temp);
		// } catch (DataException e) {
		// System.out.println(e.getMessage());
		// }
		// try {
		// double temp =
		// heatPump.rego600.getRegisterValueTemperature(Rego600.RADIATOR_RETURN_GT1);
		// System.out.println("Radiator return 0x020B=" + temp);
		// } catch (DataException e) {
		// System.out.println(e.getMessage());
		// }

		// while (true) {
		// heatPump.post2Twitter(null);
		// Thread.sleep(10000);
		// }
		System.exit(0);
	}
}
