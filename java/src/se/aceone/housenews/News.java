package se.aceone.housenews;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

import se.aceone.housenews.heatpump.HeatPump;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public abstract class News {

	public abstract void init() throws Exception;

	public abstract void tick();

}
