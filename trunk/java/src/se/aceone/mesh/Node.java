package se.aceone.mesh;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public abstract class Node extends Thread implements Protocol {
	private boolean shutdown = false;
	private Timer timer;
	private ObjectOutputStream outputStream;
	private ObjectInputStream inputStream;
	private CommandLine cmd;

	public Node(String[] args) {
		Options options = new Options();
		options.addOption("h", true, "hash key");
		addOptions(options);
		CommandLineParser parser = new PosixParser();

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println("Command line parsing failed. Reason: " + e.getMessage() + ". Exiting.");
			System.exit(1);
		}
		String hash = cmd.getOptionValue('h');
		try {
			if (startShouter(hash)) {
				start();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	void addOptions(Options options) {
	}

	boolean startShouter(String hash) throws UnknownHostException, IOException {
		Socket s = new Socket("localhost", 34200);
		outputStream = new ObjectOutputStream(s.getOutputStream());
		inputStream = new ObjectInputStream(s.getInputStream());
		
		
		// register
		int op = inputStream.readInt();
		if (op != REGISTER) {
			return false;
		}
		outputStream.writeInt(REGISTER);
		outputStream.writeUTF(hash);
		outputStream.flush();
		op = inputStream.readInt();
		if (op != OK) {
			return false;
		}
		System.out.println("Connected ok");
		timer = new Timer("Ping timer", true);
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				try {
					outputStream.writeInt(PING);
					outputStream.flush();
					int op = inputStream.readInt();
					if (op == OK) {
//						System.out.println("Ping OK");
					} else if (op == SHUTDOWN) {
						System.out.println("Setting shutdown");
						shutdown = true;
						timer.cancel();
					}
				} catch (IOException e) {
					e.printStackTrace();
					shutdown = true;
					timer.cancel();
				}

			}
		};
		timer.schedule(task, 3000, 5000);
		return true;
	}

	@Override
	final public void run() {
		try {
			work(cmd);
		} finally {
			close();
		}
	}

	abstract void work(CommandLine cmd2);

	boolean isShutingdown(){
		return shutdown;
	}
	
	private void close() {
		try {
			outputStream.writeInt(CLOSED);
			outputStream.flush();
			outputStream.close();
			inputStream.close();
		} catch (IOException e) {
		}
		System.exit(0);
	}
}
