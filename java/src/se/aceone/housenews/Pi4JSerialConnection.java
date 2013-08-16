package se.aceone.housenews;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialFactory;


public class Pi4JSerialConnection implements Connection {

	private OutputStream os;
	private InputStream is;

	public Pi4JSerialConnection() {
	}

	@Override
	public void init(String device) throws Exception {
		final Serial serial = SerialFactory.createInstance();
		serial.open(device, BAUD_RATE);
		os = new LocalOutputStream(serial);
		is = new LocalInputStream(serial);
	}

	@Override
	public InputStream getInputStream() {
		return is;
	}

	@Override
	public OutputStream getOutputStream() {
		return os;
	}

	private class LocalOutputStream extends OutputStream {
		private Serial serial;

		LocalOutputStream(Serial serial) {
			this.serial = serial;
		}

		@Override
		public void write(int b) throws IOException {
			serial.write((byte) b);
		}

		@Override
		public void flush() throws IOException {
			serial.flush();
		}

		@Override
		public void close() throws IOException {
			serial.close();
		}
	}

	private class LocalInputStream extends InputStream {

		private Serial serial;

		LocalInputStream(Serial serial) {
			this.serial = serial;

		}

		@Override
		public int read() throws IOException {
			return serial.read();
		}

		@Override
		public void close() throws IOException {
			serial.close();
		}

		@Override
		public int available() throws IOException {
			return serial.availableBytes();
		}

	}
}
