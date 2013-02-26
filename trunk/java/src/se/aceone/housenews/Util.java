package se.aceone.housenews;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.log4j.Logger;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class Util {
	public static final String EMON_API_KEY = "afdd5eecb848d9bb758bd3f6dc91a1a9";

	private static final String CONSUMER_KEY = "EKfZBbOwyqbDSKIeeDKVBA";
	private static final String CONSUMER_SECRET = "Mn1tTkXvZmSgQ5pPWx8OSHZJXSyuWnrdeaiqO2fqco";

	private static Logger logger = Logger.getLogger(Util.class);

	private static Twitter twitter;
	private static String emonUri;
	private static String emonApiKey;

	public static void post2Twitter(String status) throws TwitterException {
		if (twitter == null) {
			twitter = new TwitterFactory().getInstance();
			twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);

			try {
				Util.handleAccessToken(twitter);
			} catch (ClassNotFoundException | IOException | URISyntaxException e) {
				new TwitterException(e);
			}
		}
		status = status + " #smarthome";
		twitter.updateStatus(status);
		logger.debug("Tweeting: " + status);
	}

	public static void setEmonUri(String emonUri) {
		Util.emonUri = emonUri;
	}

	public static void setEmonApiKey(String emonApiKey) {
		Util.emonApiKey = emonApiKey;
	}

	public static int post2Emon(String msg) throws MalformedURLException, IOException {
		String url = emonUri + "/api/post?json={" + msg + "}&apikey=" + emonApiKey;
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		int respCode = connection.getResponseCode();
		connection.disconnect();
		logger.debug("Posting to emon: " + respCode + " url: " + url);
		return respCode;
	}

	private static void handleAccessToken(Twitter twitter) throws IOException, FileNotFoundException,
			ClassNotFoundException, TwitterException, URISyntaxException {
		AccessToken accessToken = null;
		File settingsDir = new File(System.getenv("HOMEPATH"), ".housenews");
		File accessTokenFile = new File(settingsDir, "accessToken");
		if (settingsDir.isDirectory() && accessTokenFile.isFile()) {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(accessTokenFile));
			accessToken = (AccessToken) ois.readObject();
			ois.close();
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
			oos.close();
		}
		logger.debug("Got access token.");
		logger.debug("Access token: " + accessToken.getToken());
		logger.debug("Access token secret: " + accessToken.getTokenSecret());
		twitter.setOAuthAccessToken(accessToken);
	}

}
