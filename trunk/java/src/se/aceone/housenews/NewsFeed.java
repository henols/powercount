package se.aceone.housenews;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import se.aceone.housenews.heatpump.HeatPump;

public class NewsFeed {

	static Logger logger = Logger.getLogger(NewsFeed.class);
	private int updateRate = 1;

	private boolean running = true;
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
		try {
			if (powerMeterBluetoothAddress != null) {
				logger.debug("Adding power meter.");
				news.add(new PowerMeter(new SerialPortConnectin(powerMeterBluetoothAddress)));
			}
			if (heatPumpBluetoothAddress != null) {
				logger.debug("Adding heat pump.");
				news.add(new HeatPump(new SerialPortConnectin(heatPumpBluetoothAddress)));
			}
		} catch (Exception e) {
			logger.error("Failed to connect", e);
		}

		Util.setEmonApiKey(Util.EMON_API_KEY);
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
				newsItem.tick();
			}
			try {
				Thread.sleep(updateRate * 1000);
			} catch (InterruptedException e) {
			}

		}
	}

	// 00:19:5d:ee:23:07 heat pump
	// 00:19:5d:ee:23:0b power meter
	// -hpb 00195dee2307
	// -pmb 00195dee230b
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
