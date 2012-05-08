package se.aceone.housenews;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import twitter4j.Twitter;
import twitter4j.TwitterException;

public abstract class News {
	private static final String API_KEY = "afdd5eecb848d9bb758bd3f6dc91a1a9";
	private Twitter twitter;

	public abstract void init() throws Exception;

	public abstract void tick();

	public void post2Twitter(String status) throws TwitterException {
		status += " #smarthome";
		twitter.updateStatus(status);
	}

	public final int post2Emon(String msg) throws MalformedURLException, IOException {
		String url = "http://192.168.1.223/emon/api/post?json={" + msg + "}&apikey=" + API_KEY;
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		int respCode = connection.getResponseCode();
		connection.disconnect();
		return respCode;

	}

	final public void setTwitter(Twitter twitter) {
		this.twitter = twitter;

	}
}
