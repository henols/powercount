package se.aceone.mesh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class BlueToothNode extends Node {

	private int updateRate = 10;

	public BlueToothNode(String[] args) {
		super(args);
	}

	void addOptions(Options options) {
		options.addOption("b", true, "bluetooth address");
	}

	@Override
	void work(CommandLine cmd) {
		System.out.println("Doing the work");
		process(cmd.getOptionValue('b'));
		System.exit(0);
	}

	public static void main(String[] args) {
		new BlueToothNode(args);
	}

	public void process(String blueToothAddress) {

		UUID uuid = new UUID(blueToothAddress, false);
		String connectionURL = "btspp://" + uuid.toString() + ":1;master=false;encrypt=false;authenticate=false";
		System.out.println("Connecting to " + uuid.toString());
		StreamConnection streamConnection = null;

		InputStream is = null;
		OutputStream os = null;

		double total = 0;
		while (!isShutingdown()) {
			try {
				if (streamConnection == null) {
					streamConnection = (StreamConnection) Connector.open(connectionURL);
					os = streamConnection.openOutputStream();
					is = streamConnection.openInputStream();
				}
			} catch (IOException e) {
				System.out.println("Faild to connect: " + e.getMessage());
				try {
					Thread.sleep(updateRate * 1000);
				} catch (InterruptedException e1) {
				}
				continue;
			}

			StringBuilder sb = new StringBuilder();
			try {
				os.write((byte) '4');
				os.flush();
				char c;
				while ((c = (char) is.read()) != '\n') {
					sb.append(c);
				}
				String power = sb.substring(sb.indexOf(":") + 1).trim();
				int p = Integer.parseInt(power);
				BigDecimal b = new BigDecimal(power);
				BigDecimal divide = b.divide(new BigDecimal(1000), 3, BigDecimal.ROUND_HALF_UP);
				total += ((double) p) / 1000;
				System.out.println("Meter value:" + (total) + "kW Total:" + divide + "kW Last:" + p + "W");

				// http://yoursite/api/api.php?json={testA:200,testB:400}
				String url = "http://aceone.se/emoncms/api/api.php?json={MainPower:" + divide + "}";
				HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
				System.out.println(url +" "+ connection.getResponseCode());
				if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					os.write('c');
					for (int i = 0; i < power.length(); i++) {
						byte charAt = (byte) power.charAt(i);
						os.write(charAt);
					}
					os.write('\n');
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// Send some data here
			try {
				Thread.sleep(updateRate * 1000);
			} catch (InterruptedException e) {
			}

		}
	}

}
