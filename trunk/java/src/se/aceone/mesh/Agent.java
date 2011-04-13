package se.aceone.mesh;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class Agent extends Thread implements Protocol {

	String[] cmds = { "se.aceone.mesh.BlueToothNode -b 00195DEE230B",
	// "se.aceone.mesh.Node -bt 00195DEE230X"
	};

	private boolean alive = true;
	private Map<String, NodeWrapper> nodes = new HashMap<String, NodeWrapper>();

	private String classPath;

	private ObjectInputStream is;

	private ObjectOutputStream os;

	public Agent() {
		Properties prop = System.getProperties();
		classPath = prop.getProperty("java.class.path", null);

		for (String cmd : cmds) {
			NodeWrapper node = new NodeWrapper(cmd);
			nodes.put(node.getKey(), node);
		}

	}

	private void work() throws IOException {
		ServerSocket serverSocket = new ServerSocket(34200);
		while (alive) {
			try {
				System.out.println("Server socket created");
				Socket socket = serverSocket.accept();
				is = new ObjectInputStream(socket.getInputStream());
				os = new ObjectOutputStream(socket.getOutputStream());

				String key = NodeWrapper.connect(is, os);
				NodeWrapper node = nodes.get(key);
				node.setConnection(socket, is, os);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		while (alive) {
			try {
				for (NodeWrapper node : nodes.values()) {
					if (!node.isAlive()) {
						startNode(node);
					}

				}
				sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void startNode(NodeWrapper node) {
		try {
			String command = node.getCommand();
			String key = node.getKey();
			String cmd = "javaw -cp \"" + classPath + "\" " + command + " -h " + key;
			System.out.println(cmd);
			Process process = Runtime.getRuntime().exec(cmd);
			System.out.println(process.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		Options options = new Options();
		options.addOption("h", "help", false, "Print this message");
		options.addOption("p", true, "Filename of property file");
		CommandLineParser parser = new PosixParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println("Command line parsing failed. Reason: " + e.getMessage() + ". Exiting.");
			System.exit(1);
		}
		@SuppressWarnings("unused")
		String optionValue = cmd.getOptionValue('p');
		Agent agent = new Agent();
		agent.start();
		agent.work();
	}

}