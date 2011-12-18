package se.aceone.housenews;

import twitter4j.Twitter;

public interface News {
	public void init() throws Exception;
	public void tweet(Twitter twitter);
}
