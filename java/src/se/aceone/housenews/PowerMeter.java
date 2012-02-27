package se.aceone.housenews;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import twitter4j.TwitterException;

public class PowerMeter extends SerialPortNews {
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
		// logger.debug(sdf.format(now.getTime()) + " before " + sdf.format(nextTweet.getTime()));
		if (now.before(nextTweet)) {
			if (System.currentTimeMillis() > pingTime) {
				StringBuilder sb = new StringBuilder();
				try { 
					os.write((byte) '4');
					os.write((byte) '0');
					os.flush();
					char c;
					while ((c = (char) is.read()) != '\n') {
						sb.append(c);
						if (sb.length() > 35) {
							String message = "To mutch to read... '" + sb + "' (ping)";
							logger.error(message);
//							tweetError(twitter, message);
							break;
						}
					}
				} catch (SocketException e) {
					logger.error("SocketException",e);
					reconnect();
					return;
				} catch (IOException e) {
					logger.error("IOException",e);
					reconnect();
					return;
				}
				
					logger.debug(sb.toString().trim());
					StringTokenizer st = new StringTokenizer(sb.toString(),",");
					String counter = st.nextToken();
					String pulses = st.nextToken();
					pulses = pulses.substring(pulses.indexOf(":") + 1);
					String power = st.nextToken();
					power = power.substring(power.indexOf(":") + 1).trim();
					logger.debug("pulses:"+pulses+" power:"+power);
					
					double kWh = toKWh(pulses);
					if(!Double.isNaN(oldKWh)){
						double nKWh = kWh - oldKWh;
						//																								  3b8b6b6204d4a6b34868ebb36fe14886
//						String url = "http://192.168.1.223/emon/api/post?json={kWh:" +nKWh + ",power:" +power + "}&apikey=3b8b6b6204d4a6b34868ebb36fe14886";
						String values ="kWh:" +nKWh + ",power:" +power ; 
						try {
							int result = post(values);
							logger.debug(result + " " +values);
						} catch (MalformedURLException e) {
							logger.error("MalformedURLException",e);
						} catch (IOException e) {
							logger.error("IOException",e);
						}
					}
					oldKWh = kWh;
//					logger.debug("ping : " + sb.toString().trim());
					pingTime += PING_TIME;
			}
			return;
		}
		logger.debug("Read power counter.");
		try {
			StringBuilder sb = new StringBuilder();
			os.write((byte) '4');
			os.write((byte) '0');
			os.flush();
			char c;
			while ((c = (char) is.read()) != '\n') {
				sb.append(c);
				if (sb.length() > 35) {
					String message = "To mutch to read... '" + sb + "'";
					logger.error(message);
//					tweetError(twitter, message);
					return;
				}
			}
			
			logger.debug(sb.toString());
			String power = sb.substring(sb.indexOf(":") + 1).trim();

			double kWh = toKWh(power);
			oldKWh = 0;
			String status = "Last day's power consumption for the house were " + kWh + "kWh. #tweetawatt";
			logger.debug(status);
			try {
				tweet(status);
			} catch (TwitterException e) {
				logger.error("Failed to post Twitter maessage.", e);
			}
			if (CLEAR_COUNT) {
				os.write('c');
				os.write('0');
				for (int i = 0; i < power.length(); i++) {
					byte charAt = (byte) power.charAt(i);
					os.write(charAt);
				}
				os.write('\n');
			}
		} catch (Exception e) {
			logger.error(e);
			reconnect();
			return;
		}
		nextTweet = getNextTweetTime();
	}

//	private double toKWh(String power) throws NumberFormatException {
//		BigDecimal b = new BigDecimal(power);
//		BigDecimal divide = b.divide(new BigDecimal(1000), 3, BigDecimal.ROUND_HALF_UP);
//		double kWh = divide.doubleValue();
//		return kWh;
//		return ((double)Integer.parseInt(power))/1000;
//	}
	
	private static double toKWh(String power) {
		if(power.length()==1){
			power = "0.00"+power;
		}else  if(power.length()==2){
			power = "0.0"+power;
		}else  if(power.length()==3){
			power = "0."+power;
		} else {
			int l = power.length();
			power = power.substring(0,l-3)+"."+power.substring(l-3);
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
