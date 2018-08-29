package se.aceone.housenews;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialFactory;

public class Pi4JSerialConnection implements Connection {

	private OutputStream os;
	private InputStream is;
	private Serial serial;
	private String device;

	public Pi4JSerialConnection() {
	}

	@Override
	public void init(String device) {
		this.device = device;
	}

	@Override
	public void open() throws Exception {
		serial = SerialFactory.createInstance();
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
			byte[] r = serial.read(1);
			return r != null && r.length > 0 ? (int) r[0] : -1;
		}

		@Override
		public void close() throws IOException {
			serial.close();
		}

		@Override
		public int available() throws IOException {
			return serial.available();
		}

	}

	@Override
	public void close() {
		if (serial != null) {
			try {
				serial.close();
			} catch (IllegalStateException e) {
			} catch (IOException e) {
			}
		}
	}

}
