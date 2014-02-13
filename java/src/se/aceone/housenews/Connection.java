package se.aceone.housenews;

import java.io.InputStream;
import java.io.OutputStream;

public interface Connection {

	public final static int BAUD_RATE = 19200;

	public abstract void init(String port);

	public abstract InputStream getInputStream();

	public abstract OutputStream getOutputStream();

	public abstract void close();

	public abstract void open() throws Exception;
	
	

}