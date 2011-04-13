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

	public NodeWrapper(String cmd) {
		this.cmd = cmd;
		this.key = cmd.hashCode() + "";
	}

	public static String connect(ObjectInputStream is, ObjectOutputStream os) throws IOException {
		System.out.println("Connecting.");
		os.writeInt(REGISTER);
		os.flush();
		@SuppressWarnings("unused")
		int readInt = is.readInt();
		String key = is.readUTF();
		os.writeInt(OK);
		os.flush();
		System.out.println("Got connection from " + key);
		return key;
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

	int i = 0;

	@Override
	public void run() {
		try {
			while (alive) {
				int v = is.readInt();
				if (v == PING) {
//					System.out.println("Got ping");
//					if (i % 5 == 0) {
//						System.out.println("Sending shutdown");
//						status = SHUTDOWN;
//					}
					if (status == SHUTDOWN) {
						alive = false;
					}
					os.writeInt(status);
					os.flush();
				} else {
					os.writeInt(SHUTDOWN);
					os.flush();
					break;
				}
				i++;
			}
		} catch (IOException e) {
			e.printStackTrace();
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

	public void setConnection(Socket socket, ObjectInputStream is, ObjectOutputStream os) throws IOException {
		this.socket = socket;
		this.is = is;
		this.os = os;
		socket.setSoTimeout(7000);
		alive = true;
		new Thread(this).start();
	}

}
