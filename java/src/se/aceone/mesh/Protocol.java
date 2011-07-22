package se.aceone.mesh;

public interface Protocol {
	public final static int REGISTER = 1;
	public final static int OK = 2;
	public final static int PING = 3;
	public final static int SHUTDOWN = 4;
	public final static int CLOSED = 5;
	public final static int LOG = 6;
	public final static int INFO = 7;
	public final static int WARN = 8;
	public final static int ERROR = 9;
	public final static int DEBUG = 10;
}
