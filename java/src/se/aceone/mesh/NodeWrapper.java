package se.aceone.mesh;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class NodeWrapper implements Runnable, Protocol {

	boolean alive = false;
	private Socket socket;
	private ObjectInputStream is;
	private ObjectOutputStream os;
	private final String key;
	private int status = OK;
	private final String cmd;
	int i = 0;
	private Logger nodeLog;
	private Logger log;

	public NodeWrapper(String cmd) {
		this.cmd = cmd;
		this.key = cmd.hashCode() + "";
	}

	public String getCommand() {
		return cmd;
	}

	public String getKey() {
		return key;
	}

	public void close() {
		status = SHUTDOWN;
	}

	@Override
	public void run() {
		try {
			while (alive) {
				int v = is.readInt();
				if (v == PING) {
					// System.out.println("Got ping");
					// if (i % 5 == 0) {
					// System.out.println("Sending shutdown");
					// status = SHUTDOWN;
					// }
					if (status == SHUTDOWN) {
						alive = false;
					}
					os.writeInt(status);
					os.flush();
				} else if (v == LOG) {
					int logLevel = is.readInt();
					String message = is.readUTF();
					nodeLog.log(logLevel, message);
				} else {
					os.writeInt(SHUTDOWN);
					os.flush();
					break;
				}
				i++;
			}
		} catch (IOException e) {
			log.error(e.getMessage());
		} finally {
			alive = false;
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	public boolean isAlive() {
		return alive;
	}

	public void setConnection(Socket socket, ObjectInputStream is,
			ObjectOutputStream os, String name) throws IOException {

		log = new Logger("Wrapper",name, key);
		nodeLog = new Logger("Node",name, key);

		this.socket = socket;
		this.is = is;
		this.os = os;
		socket.setSoTimeout(7000);
		alive = true;
		new Thread(this).start();
	}

}
