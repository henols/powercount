/*
 * Copyright 2007 Yusuke Yamamoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import java.net.URI;
import java.net.URISyntaxException;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * Example application that uses OAuth method to acquire access to your account.<br>
 * This application illustrates how to use OAuth method with Twitter4J.<br>
 * 
 * @author Yusuke Yamamoto - yusuke at mac.com
 */
public final class UpdateStatus {
	private static final String CONSUMER_KEY = "EKfZBbOwyqbDSKIeeDKVBA";
	private static final String CONSUMER_SECRET = "Mn1tTkXvZmSgQ5pPWx8OSHZJXSyuWnrdeaiqO2fqco";

	/**
	 * Usage: java twitter4j.examples.tweets.UpdateStatus [text]
	 * 
	 * @param args
	 *            message
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: java twitter4j.examples.tweets.UpdateStatus [text]");
			System.exit(-1);
		}
		try {
			Twitter twitter = new TwitterFactory().getInstance();
			// get request token.
			// this will throw IllegalStateException if access token is already available
			twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);

			handleAccessToken(twitter);
			Status status = twitter.updateStatus(args[0] + " : " + System.currentTimeMillis());
			System.out.println("Successfully updated the status to [" + status.getText() + "].");
			System.exit(0);
		} catch (IllegalStateException ie) {
			ie.printStackTrace();
			// access token is already available, or consumer key/secret is not set.
		} catch (TwitterException te) {
			te.printStackTrace();
			System.out.println("Failed to get timeline: " + te.getMessage());
			System.exit(-1);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.out.println("Failed to read the system input.");
			System.exit(-1);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
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
			ois.close();
		} else {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Open the following URL and grant access to your account:");

			RequestToken requestToken = twitter.getOAuthRequestToken();
			System.out.println("Got request token.");
			System.out.println("Request token: " + requestToken.getToken());
			System.out.println("Request token secret: " + requestToken.getTokenSecret());

			String authorizationURL = requestToken.getAuthorizationURL();
			System.out.println(authorizationURL);

			java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
			if (!desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
				System.err.println("Desktop doesn't support the browse action (fatal)");
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
		System.out.println("Got access token.");
		System.out.println("Access token: " + accessToken.getToken());
		System.out.println("Access token secret: " + accessToken.getTokenSecret());
		twitter.setOAuthAccessToken(accessToken);
	}
}
