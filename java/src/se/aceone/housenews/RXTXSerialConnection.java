package se.aceone.housenews;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import org.apache.log4j.Logger;

public class RXTXSerialConnection implements Connection {
	private static Logger logger = Logger.getLogger(RXTXSerialConnection.class);
	private String portName;
	protected InputStream is = null;
	protected OutputStream os = null;
	private int baudRate;
	private CommPort commPort;

	public RXTXSerialConnection() {
	}

	public void init(String port)  {
		
		this.portName = port;
	}
	@Override
	public void open() throws Exception {
		this.baudRate = BAUD_RATE;
		listPorts();
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		if (portIdentifier.isCurrentlyOwned()) {
			logger.error("Error: Port is currently in use");
		} else {
			 commPort = portIdentifier.open(this.getClass().getName(), 2000);

			if (commPort instanceof SerialPort) {
				SerialPort serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);

				is = serialPort.getInputStream();
				os = serialPort.getOutputStream();

			} else {
				logger.error("Not an serial port");
			}
		}
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.aceone.housenews.Connection#getInputStream()
	 */
	@Override
	public InputStream getInputStream() {
		return is;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.aceone.housenews.Connection#getOutputStream()
	 */
	@Override
	public OutputStream getOutputStream() {
		return os;
	}
	
	
	@Override
	public void close() {
		if(commPort!=null){
			commPort.close();
		}
	}
}
