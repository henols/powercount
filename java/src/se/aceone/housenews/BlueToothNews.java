package se.aceone.housenews;

import java.io.InputStream;
import java.io.OutputStream;

import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import org.apache.log4j.Logger;

public abstract class BlueToothNews extends News {
	private static Logger logger = Logger.getLogger(BlueToothNews.class);
	private final String bluetoothAddress;
	protected InputStream is = null;
	protected OutputStream os = null;

	public BlueToothNews(String bluetoothAddress) {
		this.bluetoothAddress = bluetoothAddress;
	}
	
	@Override
	public void init() throws Exception {
		UUID uuid = new UUID(bluetoothAddress, false);
		String connectionURL = "btspp://" + uuid.toString() + ":1;master=false;encrypt=false;authenticate=false";
		logger.info("Connecting to Blue Tooth device: " + uuid.toString());
		StreamConnection streamConnection = (StreamConnection) Connector.open(connectionURL);
		os = streamConnection.openOutputStream();
		is = streamConnection.openInputStream();
	}
	
}
