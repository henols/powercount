package se.aceone.housenews;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import org.apache.log4j.Logger;

public class SerialPortConnectin implements Connection {
	private static Logger logger = Logger.getLogger(SerialPortConnectin.class);
	private final String portName;
	protected InputStream is = null;
	protected OutputStream os = null;
	private final int baudRate;

	public SerialPortConnectin(String port) throws Exception {
		this(port, BAUD_RATE);
	}

	public SerialPortConnectin(String port, int baudRate) throws Exception {
		this.portName = port;
		this.baudRate = baudRate;
		init();
	}

	static void listPorts() {
		@SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier portIdentifier = portEnum.nextElement();
			logger.info(portIdentifier.getName() + " - " + getPortTypeName(portIdentifier.getPortType()));
		}
	}

	static String getPortTypeName(int portType) {
		switch (portType) {
		case CommPortIdentifier.PORT_I2C:
			return "I2C";
		case CommPortIdentifier.PORT_PARALLEL:
			return "Parallel";
		case CommPortIdentifier.PORT_RAW:
			return "Raw";
		case CommPortIdentifier.PORT_RS485:
			return "RS485";
		case CommPortIdentifier.PORT_SERIAL:
			return "Serial";
		default:
			return "unknown type";
		}
	}

	private void init() throws Exception {
		listPorts();
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		if (portIdentifier.isCurrentlyOwned()) {
			logger.error("Error: Port is currently in use");
		} else {
			CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);

			if (commPort instanceof SerialPort) {
				SerialPort serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

				is = serialPort.getInputStream();
				os = serialPort.getOutputStream();

			} else {
				logger.error("Not an serial port");
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see se.aceone.housenews.Connection#getInputStream()
	 */
	@Override
	public InputStream getInputStream(){
		return is;
	}
	
	/* (non-Javadoc)
	 * @see se.aceone.housenews.Connection#getOutputStream()
	 */
	@Override
	public OutputStream getOutputStream() {
		return os;
	}
}
