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

	String[] cmds = { 
			"se.aceone.mesh.BlueToothNode -b 00195DEE230B",
			//"se.aceone.mesh.ComportNode -p COM8",
	// "se.aceone.mesh.Node -bt 00195DEE230X"
	};

	private boolean alive = true;
	private Map<String, NodeWrapper> nodes = new HashMap<String, NodeWrapper>();

	private String classPath;

	private ObjectInputStream is;

	private ObjectOutputStream os;

	private static Logger log;

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
		log.info("Server socket created");
		while (alive) {
			try {
				Socket socket = serverSocket.accept();
				log.info("Got socket accept");
				is = new ObjectInputStream(socket.getInputStream());
				os = new ObjectOutputStream(socket.getOutputStream());

				connect(socket, is, os);
			} catch (IOException e) {
				log.error(e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public void connect(Socket socket, ObjectInputStream is,
			ObjectOutputStream os) throws IOException {
		log.info("Connecting.");
		os.writeInt(REGISTER);
		os.flush();
		@SuppressWarnings("unused")
		int readInt = is.readInt();
		String key = is.readUTF();
		String name = is.readUTF();
		os.writeInt(OK);
		os.flush();
		log.info("Got connection from " +name +"("+ key+")");
		NodeWrapper node = nodes.get(key);
		node.setConnection(socket, is, os, name	);
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
			}
		}
	}

	private void startNode(NodeWrapper node) {
		try {
			String command = node.getCommand();
			String key = node.getKey();
			String cmd = "javaw -cp \"" + classPath + "\" " + command + " -h "
					+ key;
			log.info(cmd);
			Process process = Runtime.getRuntime().exec(cmd);
			log.info(process.toString());
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	public static void main(String[] args) throws IOException {
		log = new Logger("Agent");
		Options options = new Options();
		options.addOption("h", "help", false, "Print this message");
		options.addOption("p", true, "Filename of property file");
		CommandLineParser parser = new PosixParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			log.error("Command line parsing failed. Reason: "
					+ e.getMessage() + ". Exiting.");
			System.exit(1);
		}
		@SuppressWarnings("unused")
		String optionValue = cmd.getOptionValue('p');
		Agent agent = new Agent();
		agent.start();
		agent.work();
	}

}
