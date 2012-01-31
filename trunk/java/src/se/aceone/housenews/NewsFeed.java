package se.aceone.housenews;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.naming.InitialContext;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import se.aceone.housenews.heatpump.HeatPump;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class NewsFeed {
	private static final String CONSUMER_KEY = "EKfZBbOwyqbDSKIeeDKVBA";
	private static final String CONSUMER_SECRET = "Mn1tTkXvZmSgQ5pPWx8OSHZJXSyuWnrdeaiqO2fqco";

	static Logger logger = Logger.getLogger(NewsFeed.class);
	private int updateRate = 1;

	private boolean running = true;
	private Twitter twitter;
	private List<News> news = new ArrayList<News>();
	private String powerMeterBluetoothAddress;
	private final String heatPumpBluetoothAddress;

		public NewsFeed(String powerMeterBluetoothAddress, String heatPumpBluetoothAddress) {
		this.powerMeterBluetoothAddress = powerMeterBluetoothAddress;
		this.heatPumpBluetoothAddress = heatPumpBluetoothAddress;
		init();
		process();
	}

	private void init() {
		twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);

		try {
			handleAccessToken(twitter);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.debug("Adding power meter.");
		news.add(new PowerMeter(powerMeterBluetoothAddress));
		logger.debug("Adding heat pump.");
		news.add(new HeatPump(heatPumpBluetoothAddress));
		for (News newsItem : news) {
			try {
				newsItem.init();
			} catch (Exception e) {
				logger.error("Cant init newsItem '" + newsItem.getClass().getSimpleName() + "'", e);
			}
		}

	}

	public void process() {
		while (running) {
			for (News newsItem : news) {
				newsItem.tweet(twitter);
			}
			try {
				Thread.sleep(updateRate * 1000);
			} catch (InterruptedException e) {
			}

		}
	}

	private static void handleAccessToken(Twitter twitter) throws IOException, FileNotFoundException,
			ClassNotFoundException, TwitterException, URISyntaxException {
		AccessToken accessToken = null;
		File settingsDir = new File(System.getenv("HOMEPATH"), ".housenews");
		File accessTokenFile = new File(settingsDir, "accessToken");
		if (settingsDir.isDirectory() && accessTokenFile.isFile()) {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(accessTokenFile));
			accessToken = (AccessToken) ois.readObject();
		} else {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			logger.debug("Open the following URL and grant access to your account:");

			RequestToken requestToken = twitter.getOAuthRequestToken();
			logger.debug("Got request token.");
			logger.debug("Request token: " + requestToken.getToken());
			logger.debug("Request token secret: " + requestToken.getTokenSecret());

			String authorizationURL = requestToken.getAuthorizationURL();
			logger.debug(authorizationURL);

			java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
			if (!desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
				logger.error("Desktop doesn't support the browse action (fatal)");
				System.exit(1);
			}
			URI uri = new URI(authorizationURL);
			desktop.browse(uri);

			System.out.print("Enter the PIN(if available) and hit enter after you granted access.[PIN]:");
			String pin = br.readLine();
			if (pin.length() > 0) {
				accessToken = twitter.getOAuthAccessToken(requestToken, pin);
			} else {
				accessToken = twitter.getOAuthAccessToken(requestToken);
			}
			if (!settingsDir.isDirectory()) {
				settingsDir.mkdirs();
			}
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(accessTokenFile));
			oos.writeObject(accessToken);
		}
		logger.debug("Got access token.");
		logger.debug("Access token: " + accessToken.getToken());
		logger.debug("Access token secret: " + accessToken.getTokenSecret());
		twitter.setOAuthAccessToken(accessToken);
	}

	public static void main(String[] args) {
		// BasicConfigurator.configure();
		Options options = new Options();
		options.addOption("pmb", true, "Power Meter bluetooth address");
		options.addOption("hpb", true, "Heat Pump bluetooth address");
		CommandLineParser parser = new PosixParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			logger.error("Command line parsing failed.", e);
			System.exit(1);
		}
		String powerMeterBluetoothAddress = cmd.getOptionValue("pmb");
		String heatPumpBluetoothAddress = cmd.getOptionValue("hpb");
		// twitter.
		new NewsFeed(powerMeterBluetoothAddress, heatPumpBluetoothAddress);
	}

}
